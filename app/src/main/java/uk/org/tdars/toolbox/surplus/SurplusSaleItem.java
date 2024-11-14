package uk.org.tdars.toolbox.surplus;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurplusSaleItem implements Serializable {
    private String lotNumber;
    private String sellerCallsign;
    private String itemDescription;
    private BigDecimal reservePrice;
    private boolean itemSold;
    private String buyerCallsign;
    private BigDecimal hammerPrice;
}
