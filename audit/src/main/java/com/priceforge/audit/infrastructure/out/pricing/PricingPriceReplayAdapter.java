package com.priceforge.audit.infrastructure.out.pricing;

import com.priceforge.audit.application.port.out.PriceReplayPort;
import com.priceforge.audit.domain.model.PriceCalculationSnapshot;
import com.priceforge.pricing.application.dto.ReplayPriceCalculationCommand;
import com.priceforge.pricing.application.port.in.ReplayPriceCalculationUseCase;
import com.priceforge.sharedkernel.ProductId;
import com.priceforge.sharedkernel.UserId;
import java.math.BigDecimal;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/** Driven adapter onto the Pricing context's published replay port — the only place Audit talks to Pricing. */
@Component
public class PricingPriceReplayAdapter implements PriceReplayPort {

    private final ReplayPriceCalculationUseCase replayPriceCalculationUseCase;

    public PricingPriceReplayAdapter(ReplayPriceCalculationUseCase replayPriceCalculationUseCase) {
        this.replayPriceCalculationUseCase = replayPriceCalculationUseCase;
    }

    @Override
    public BigDecimal replayFinalPrice(PriceCalculationSnapshot snapshot) {
        return replayPriceCalculationUseCase.execute(new ReplayPriceCalculationCommand(
                ProductId.of(snapshot.productId()),
                snapshot.userId() == null ? null : UserId.of(snapshot.userId()),
                snapshot.userSegment(),
                snapshot.quantity(),
                snapshot.requestedAt(),
                snapshot.location(),
                snapshot.currency(),
                ZoneId.of(snapshot.zoneId())))
            .finalPriceAmount();
    }
}
