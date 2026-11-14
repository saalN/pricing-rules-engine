#!/usr/bin/env bash
#
# Guided tour of PriceForge against a running instance (docker compose up -d && ./gradlew bootRun).
# Creates its own products, rules and promotions through the public API, then walks through the
# pricing, conflict-resolution, promotion and audit scenarios, printing what happens and why.
#
# Safe to run repeatedly: everything is created under a fresh, per-run category and SKU suffix.
# Requires: curl, jq.   Usage: ./scripts/demo.sh [base-url]   (default http://localhost:8080)

set -euo pipefail

BASE_URL="${1:-${BASE_URL:-http://localhost:8080}}"
RUN="$(date +%s)"

if [ -t 1 ]; then BOLD=$'\033[1m'; DIM=$'\033[2m'; GREEN=$'\033[32m'; CYAN=$'\033[36m'; RESET=$'\033[0m'
else BOLD=""; DIM=""; GREEN=""; CYAN=""; RESET=""; fi

for tool in curl jq; do
  command -v "$tool" >/dev/null || { echo "error: '$tool' is required" >&2; exit 1; }
done
curl -fsS -o /dev/null "$BASE_URL/v3/api-docs" 2>/dev/null \
  || { echo "error: no PriceForge instance at $BASE_URL (start it with: docker compose up -d && ./gradlew bootRun)" >&2; exit 1; }

# ---------- helpers -------------------------------------------------------------------------

title()   { printf '\n%s== %s ==%s\n' "$BOLD$CYAN" "$1" "$RESET"; }
say()     { printf '%s%s%s\n' "$DIM" "$1" "$RESET"; }
ok()      { printf '%s  ✓ %s%s\n' "$GREEN" "$1" "$RESET"; }
new_uuid() { (uuidgen 2>/dev/null || cat /proc/sys/kernel/random/uuid) | tr 'A-Z' 'a-z'; }
ago()     { jq -rn "now - $1 | todate"; }
ahead()   { jq -rn "now + $1 | todate"; }

# call METHOD PATH TOKEN [JSON_BODY]  ->  sets $STATUS and $BODY
call() {
  local method="$1" path="$2" token="$3" data="${4:-}" out
  local args=(-sS -X "$method" "$BASE_URL$path" -H "Authorization: Bearer $token" -w $'\n%{http_code}')
  [ -n "$data" ] && args+=(-H 'Content-Type: application/json' -d "$data")
  out="$(curl "${args[@]}")"
  STATUS="${out##*$'\n'}"
  BODY="${out%$'\n'*}"
}

# expect_status CODE  -> aborts with the response if the last call did not return CODE
expect_status() {
  if [ "$STATUS" != "$1" ]; then
    echo "unexpected HTTP $STATUS (wanted $1): $BODY" >&2
    exit 1
  fi
}

token() { curl -fsS -X POST "$BASE_URL/auth/token" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$1\",\"role\":\"$2\"}" | jq -r .accessToken; }

create_product() { # sku name base-price category
  call POST /products "$ADMIN" "$(jq -n --arg sku "$1-$RUN" --arg name "$2" --argjson price "$3" --arg cat "$4" \
    '{sku:$sku, name:$name, description:$name, basePriceAmount:$price, currencyCode:"EUR", category:$cat}')"
  expect_status 201
  jq -r .productId <<<"$BODY"
}

create_rule() { # type segment discount-type value priority mode category   (segment may be "")
  call POST /pricing-rules "$ADMIN" "$(jq -n --arg type "$1" --arg seg "$2" --arg dt "$3" --argjson v "$4" \
    --argjson prio "$5" --arg mode "$6" --arg cat "$7" \
    '{type:$type, segment:(if $seg=="" then null else $seg end), discountType:$dt, discountValue:$v,
      priority:$prio, combinationMode:$mode, scopeCategory:$cat}')"
  expect_status 201
  jq -r .ruleId <<<"$BODY"
}

create_promotion() { # name discount-type value priority mode category global-limit per-user-limit
  call POST /promotions "$MARKETING" "$(jq -n --arg name "$1" --arg dt "$2" --argjson v "$3" --argjson prio "$4" \
    --arg mode "$5" --arg cat "$6" --argjson gl "$7" --argjson ul "$8" --arg from "$(ago 86400)" --arg to "$(ahead 86400)" \
    '{name:$name, description:$name, validFrom:$from, validUntil:$to, globalUsageLimit:$gl, perUserUsageLimit:$ul,
      category:$cat, discountType:$dt, discountValue:$v, currency:"EUR", priority:$prio, combinationMode:$mode}')"
  [ "$STATUS" = "201" ] || return 1
  jq -r .promotionId <<<"$BODY"
}

calculate() { # product-id user-id segment  -> prints a readable breakdown
  call POST /price-calculations "$ADMIN" "$(jq -n --arg p "$1" --arg u "$2" --arg s "$3" \
    '{productId:$p, userId:$u, userSegment:$s, quantity:1, location:"URBAN", currency:"EUR"}')"
  expect_status 200
  breakdown <<<"$BODY"
}

breakdown() {
  jq -r '
    "  price: \(.basePriceAmount) -> \(.finalPriceAmount) EUR",
    (.appliedRules[]?        | "    + rule      \(.description) (-\(.discountApplied))"),
    (.excludedRules[]?       | "    - rule      \(.ruleId[0:8])…  \(.reason)"),
    (.appliedPromotions[]?   | "    + promotion \(.description) (-\(.discountApplied))"),
    (.excludedPromotions[]?  | "    - promotion \(.sourceId[0:8])…  \(.reason)")'
}

final_price() { jq -r .finalPriceAmount <<<"$BODY"; }

# ---------- tour ----------------------------------------------------------------------------

title "Tokens"
say "POST /auth/token is a demo utility (disabled by the 'prod' profile): it issues a JWT for any role."
ADMIN="$(token admin@priceforge.com ADMIN)"
MARKETING="$(token marketing@priceforge.com MARKETING)"
AUDITOR="$(token auditor@priceforge.com AUDITOR)"
ok "ADMIN, MARKETING and AUDITOR tokens issued"

VIP_USER="$(new_uuid)"; STANDARD_USER="$(new_uuid)"

title "1. UC-06 — an explainable price (segment rule)"
CAT_A="DEMO-A-$RUN"
LAPTOP="$(create_product LAPTOP "Laptop" 100 "$CAT_A")"
create_rule SEGMENT_BASED VIP PERCENTAGE 20 10 STACKABLE "$CAT_A" >/dev/null
say "Product 100 EUR, rule: VIP customers get 20% off."
say "VIP customer:";      calculate "$LAPTOP" "$VIP_USER" VIP
say "STANDARD customer:"; calculate "$LAPTOP" "$STANDARD_USER" STANDARD

title "2. UC-08 — what-if simulator (persists nothing)"
call POST /price-calculations/simulate "$ADMIN" "$(jq -n --arg p "$LAPTOP" --arg u "$VIP_USER" --arg cat "$CAT_A" \
  '{productId:$p, userId:$u, userSegment:"VIP", quantity:1, location:"URBAN", currency:"EUR",
    hypotheticalRule:{type:"SEGMENT_BASED", segment:"VIP", discountType:"PERCENTAGE", discountValue:5,
                      priority:5, combinationMode:"STACKABLE", scopeCategory:$cat}}')"
expect_status 200
say "What would a VIP pay if we also added a 5% rule? (simulated: $(jq -r .simulated <<<"$BODY"))"
breakdown <<<"$BODY"

title "3. UC-07 — conflicts between rules"
CAT_B="DEMO-B-$RUN"
SERVER="$(create_product SERVER "Server" 200 "$CAT_B")"
create_rule SEGMENT_BASED VIP PERCENTAGE 10 30 EXCLUSIVE "$CAT_B" >/dev/null   # wins its tie (created first)
LOSER_RULE="$(create_rule SEGMENT_BASED VIP FIXED_AMOUNT 5 30 EXCLUSIVE "$CAT_B")" # same priority, also EXCLUSIVE
create_rule SEGMENT_BASED VIP PERCENTAGE 50 10 STACKABLE "$CAT_B" >/dev/null   # outranked by the EXCLUSIVE winner
say "Product 200 EUR. Rules: A = 10% EXCLUSIVE p30, B = 5 EUR EXCLUSIVE p30, C = 50% STACKABLE p10."
say "A and B tie and both are EXCLUSIVE, so the earlier-created A wins and B is a genuine conflict;"
say "C is simply outranked by an EXCLUSIVE rule of higher priority:"
calculate "$SERVER" "$VIP_USER" VIP

CAT_C="DEMO-C-$RUN"
SWITCH="$(create_product SWITCH "Switch" 100 "$CAT_C")"
create_rule SEGMENT_BASED VIP PERCENTAGE 10 10 STACKABLE "$CAT_C" >/dev/null
create_rule SEGMENT_BASED VIP FIXED_AMOUNT 5 10 STACKABLE "$CAT_C" >/dev/null
say "Product 100 EUR. Two STACKABLE rules tie at p10 (10% and 5 EUR): both apply, computed on the same starting price:"
calculate "$SWITCH" "$VIP_USER" VIP

title "4. UC-10/UC-11 — promotions on top of the pricing rules"
say "Promotions apply after the rules, with the same conflict algorithm, on the price the rules produced."
create_promotion "P1 20 EUR off" FIXED_AMOUNT 20 20 STACKABLE "$CAT_B" 1000 10 >/dev/null
create_promotion "P2 10% off" PERCENTAGE 10 20 STACKABLE "$CAT_B" 1000 10 >/dev/null
create_promotion "P3 5 EUR off (EXCLUSIVE)" FIXED_AMOUNT 5 10 EXCLUSIVE "$CAT_B" 1000 10 >/dev/null
create_promotion "P4 7 EUR off (EXCLUSIVE)" FIXED_AMOUNT 7 10 EXCLUSIVE "$CAT_B" 1000 10 >/dev/null
create_promotion "P5 3 EUR off" FIXED_AMOUNT 3 5 STACKABLE "$CAT_B" 1000 10 >/dev/null
say "Server again: P1 and P2 tie and stack, P3/P4 tie as EXCLUSIVE (earlier wins), P5 is outranked."
say "180 after the rules; P1 (20) + P2 (10% of 180 = 18) -> 142; P3 (5) -> 137:"
calculate "$SERVER" "$VIP_USER" VIP

title "5. UC-13 — a promotion that could drive the price to zero is rejected"
CAT_D="DEMO-D-$RUN"
PHONE="$(create_product PHONE "Phone" 100 "$CAT_D")"
create_rule SEGMENT_BASED VIP PERCENTAGE 60 10 STACKABLE "$CAT_D" >/dev/null
if create_promotion "Too generous" FIXED_AMOUNT 50 10 STACKABLE "$CAT_D" 1000 10 >/dev/null; then
  echo "expected the promotion to be rejected" >&2; exit 1
fi
say "Product 100 EUR with a 60% rule: a 50 EUR promotion could stack to 110, so it is rejected (HTTP $STATUS):"
say "  $(jq -r .detail <<<"$BODY")"

title "6. UC-11 — usage limits (global 2, one per user)"
FAIR="$(create_promotion "Fair 30 EUR off" FIXED_AMOUNT 30 10 STACKABLE "$CAT_D" 2 1)"
U1="$(new_uuid)"; U2="$(new_uuid)"; U3="$(new_uuid)"
say "User 1, first purchase (rule 60% -> 40, promotion -30 -> 10):"; calculate "$PHONE" "$U1" VIP
say "User 1 again: per-user limit reached, so the promotion is excluded:"; calculate "$PHONE" "$U1" VIP
say "User 2 takes the last global use (the promotion becomes EXHAUSTED):"; calculate "$PHONE" "$U2" VIP
say "User 3: no longer a candidate at all:"; calculate "$PHONE" "$U3" VIP

title "7. UC-14/UC-15 — what the audit trail recorded"
say "Events travel through each context's outbox to Audit; the relay runs every few seconds."
for _ in $(seq 1 30); do
  call GET "/audit/price-calculations?productId=$SERVER" "$AUDITOR"
  [ "$(jq -r .totalElements <<<"$BODY")" -ge 1 ] && break
  sleep 1
done
expect_status 200
say "Price calculations stored for the Server product: $(jq -r .totalElements <<<"$BODY") (newest first, each with its full contextSnapshot)"
CALC_ID="$(jq -r '.items[0].eventId' <<<"$BODY")"

say "Replaying that calculation against today's pricing rules (promotions are not re-applied):"
call GET "/audit/price-calculations/$CALC_ID/replay" "$AUDITOR"; expect_status 200
jq -r '"  stored \(.originalFinalPriceAmount) EUR, replayed \(.replayedFinalPriceAmount) EUR, matches: \(.matches)"' <<<"$BODY"

say "Rule conflicts recorded for the Server product (RuleConflictDetected, visible to auditors):"
for _ in $(seq 1 30); do
  call GET "/audit/export?format=JSON&productId=$SERVER" "$AUDITOR"
  jq -e 'any(.[]; .eventType=="RuleConflictDetected")' <<<"$BODY" >/dev/null 2>&1 && break
  sleep 1
done
jq -r '.[] | select(.eventType=="RuleConflictDetected") | "  \(.occurredAt)  reason=\(.reason)  \(.newState)"' <<<"$BODY" | head -1

say "Change history of the loser rule (UC-15):"
for _ in $(seq 1 30); do
  call GET "/audit/history/PRICING_RULE/$LOSER_RULE" "$AUDITOR"
  [ "$(jq 'length' <<<"$BODY" 2>/dev/null || echo 0)" -ge 1 ] && break
  sleep 1
done
jq -r '.[] | "  \(.occurredAt)  \(.eventType)  \(.newState)"' <<<"$BODY"

say "CSV export of the Phone promotion's history (first lines):"
call GET "/audit/export?format=CSV&entityType=PROMOTION&entityId=$FAIR" "$AUDITOR"
head -c 600 <<<"$BODY" | cut -c1-150 | sed 's/^/  /'

title "8. RBAC — the Auditor can read everything and write nothing"
call POST /products "$AUDITOR" '{"sku":"AUDITOR-TRY","name":"x","description":"x","basePriceAmount":1,"currencyCode":"EUR","category":"X"}'
say "AUDITOR creating a product -> HTTP $STATUS"; [ "$STATUS" = "403" ] && ok "forbidden, as specified"
call POST /price-calculations "$AUDITOR" "$(jq -n --arg p "$LAPTOP" --arg u "$VIP_USER" \
  '{productId:$p, userId:$u, userSegment:"VIP", quantity:1, location:"URBAN", currency:"EUR"}')"
say "AUDITOR calculating a price (it would write events and consume promotion usage) -> HTTP $STATUS"
[ "$STATUS" = "403" ] && ok "forbidden, as specified"

printf '\n%sDone.%s Explore further in Swagger UI: %s/swagger-ui/index.html\n' "$BOLD" "$RESET" "$BASE_URL"
