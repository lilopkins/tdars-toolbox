package uk.org.tdars.toolbox.surplus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurplusSaleItem implements Serializable {
    private String lotNumber;
    private String sellerCallsign;
    private String itemDescription;
    private BigDecimal reservePrice;
    /**
     * Check if `hammerPrice` is null instead.
     */
    @Deprecated
    private boolean itemSold;
    private String buyerCallsign;
    private BigDecimal hammerPrice;
    private boolean reconciledSeller;
    private boolean reconciledBuyer;

    public void setReservePrice(BigDecimal reservePrice) {
        this.reservePrice = reservePrice.setScale(2, RoundingMode.HALF_EVEN);
    }

    public void setHammerPrice(BigDecimal hammerPrice) {
        this.hammerPrice = hammerPrice.setScale(2, RoundingMode.HALF_EVEN);
    }
}
