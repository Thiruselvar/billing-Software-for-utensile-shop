package yogasri.pos.service;

import yogasri.pos.db.DBConnection;
import yogasri.pos.model.CartItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BillingService {
    public static class SaleResult {
        public int saleId;
        public String invoiceNo;
    }

    public SaleResult createSale(
            Integer customerId,
            int createdByUserId,
            String paymentMethod, // CASH/UPI
            boolean isCredit,
            BigDecimal discountAmount,
            List<CartItem> items
    ) throws Exception {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Cart is empty");

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal gstTotal = BigDecimal.ZERO;
        for (CartItem it : items) {
            subtotal = subtotal.add(it.lineSubtotal());
            gstTotal = gstTotal.add(it.lineGst());
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        gstTotal = gstTotal.setScale(2, RoundingMode.HALF_UP);
        discountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount.setScale(2, RoundingMode.HALF_UP);
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) discountAmount = BigDecimal.ZERO;
        BigDecimal finalAmount = subtotal.add(gstTotal).subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;

        String invoiceNo = generateInvoiceNo();

        Connection con = DBConnection.getConnection();
        boolean oldAuto = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            int saleId;
            String saleSql = "INSERT INTO sales(invoice_no, customer_id, payment_method, subtotal, gst_amount, discount_amount, final_amount, is_credit, created_by) VALUES(?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, invoiceNo);
                if (customerId == null) ps.setNull(2, java.sql.Types.INTEGER);
                else ps.setInt(2, customerId);
                ps.setString(3, paymentMethod);
                ps.setBigDecimal(4, subtotal);
                ps.setBigDecimal(5, gstTotal);
                ps.setBigDecimal(6, discountAmount);
                ps.setBigDecimal(7, finalAmount);
                ps.setInt(8, isCredit ? 1 : 0);
                ps.setInt(9, createdByUserId);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new RuntimeException("Failed to create sale");
                    saleId = keys.getInt(1);
                }
            }

            String itemSql = "INSERT INTO sale_items(sale_id, product_id, quantity, unit_price, gst_percent, line_subtotal, line_gst, line_total) VALUES(?,?,?,?,?,?,?,?)";
            String stockSql = "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?";

            try (PreparedStatement psItem = con.prepareStatement(itemSql);
                 PreparedStatement psStock = con.prepareStatement(stockSql)) {
                for (CartItem it : items) {
                    int pid = it.getProduct().getId();
                    int qty = it.getQuantity();
                    BigDecimal unit = it.getProduct().getPrice();
                    BigDecimal gstPct = it.getProduct().getGstPercent() == null ? BigDecimal.ZERO : it.getProduct().getGstPercent();

                    BigDecimal lineSubtotal = it.lineSubtotal();
                    BigDecimal lineGst = it.lineGst();
                    BigDecimal lineTotal = it.lineTotal();

                    psItem.setInt(1, saleId);
                    psItem.setInt(2, pid);
                    psItem.setInt(3, qty);
                    psItem.setBigDecimal(4, unit);
                    psItem.setBigDecimal(5, gstPct);
                    psItem.setBigDecimal(6, lineSubtotal);
                    psItem.setBigDecimal(7, lineGst);
                    psItem.setBigDecimal(8, lineTotal);
                    psItem.addBatch();

                    psStock.setInt(1, qty);
                    psStock.setInt(2, pid);
                    psStock.setInt(3, qty);
                    psStock.addBatch();
                }
                psItem.executeBatch();
                int[] stockUpdated = psStock.executeBatch();
                for (int u : stockUpdated) {
                    if (u == 0) throw new RuntimeException("Insufficient stock for one or more items");
                }
            }

            con.commit();
            SaleResult res = new SaleResult();
            res.saleId = saleId;
            res.invoiceNo = invoiceNo;
            return res;
        } catch (Exception e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(oldAuto);
        }
    }

    private String generateInvoiceNo() {
        String d = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        long n = System.currentTimeMillis() % 1000000;
        return "INV-" + d + "-" + String.format("%06d", n);
    }
}

