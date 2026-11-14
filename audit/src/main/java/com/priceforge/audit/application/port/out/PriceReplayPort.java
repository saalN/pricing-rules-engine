package com.priceforge.audit.application.port.out;

import com.priceforge.audit.domain.model.PriceCalculationSnapshot;
import java.math.BigDecimal;

/** Re-runs a stored calculation against the rules active now, via the Pricing context. */
public interface PriceReplayPort {

    /** @return the final price the current rules produce for that snapshot's context */
    BigDecimal replayFinalPrice(PriceCalculationSnapshot snapshot);
}
