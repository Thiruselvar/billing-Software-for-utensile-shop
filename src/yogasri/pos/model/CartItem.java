package yogasri.pos.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CartItem {
    private final Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal lineSubtotal() {
        return product.getPrice().multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal lineGst() {
        BigDecimal gst = product.getGstPercent() == null ? BigDecimal.ZERO : product.getGstPercent();
        return lineSubtotal().multiply(gst).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal lineTotal() {
        return lineSubtotal().add(lineGst()).setScale(2, RoundingMode.HALF_UP);
    }
}

