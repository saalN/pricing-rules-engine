package com.priceforge.audit.application.service.support;

import com.priceforge.audit.application.port.out.PriceReplayPort;
import com.priceforge.audit.domain.model.PriceCalculationSnapshot;
import java.math.BigDecimal;

/** Replays to a fixed price, regardless of the snapshot. */
public final class FakePriceReplayPort implements PriceReplayPort {

    private BigDecimal replayedPrice = BigDecimal.ZERO;
    private PriceCalculationSnapshot lastSnapshot;

    public void replayTo(BigDecimal price) {
        this.replayedPrice = price;
    }

    public PriceCalculationSnapshot lastSnapshot() {
        return lastSnapshot;
    }

    @Override
    public BigDecimal replayFinalPrice(PriceCalculationSnapshot snapshot) {
        this.lastSnapshot = snapshot;
        return replayedPrice;
    }
}
