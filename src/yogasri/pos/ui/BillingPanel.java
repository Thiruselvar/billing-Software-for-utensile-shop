package yogasri.pos.ui;

import yogasri.pos.dao.CustomerDAO;
import yogasri.pos.dao.ProductDAO;
import yogasri.pos.model.CartItem;
import yogasri.pos.model.Customer;
import yogasri.pos.model.Product;
import yogasri.pos.service.BillingService;
import yogasri.pos.util.AppConfig;
import yogasri.pos.util.AppSession;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BillingPanel extends JPanel {
    private final JTextField txtProductSearch = new JTextField(25);
    private final DefaultTableModel prodModel = new DefaultTableModel(new Object[]{"ID", "Name", "Category", "Price", "GST%", "Stock"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable prodTable = new JTable(prodModel);

    private final DefaultTableModel cartModel = new DefaultTableModel(new Object[]{"ID", "Name", "Qty", "Unit", "GST%", "SubTotal", "GST", "Total"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable cartTable = new JTable(cartModel);
    private final List<CartItem> cart = new ArrayList<>();

    private final JTextField txtCustPhone = new JTextField(12);
    private final JTextField txtCustName = new JTextField(16);
    private Customer selectedCustomer;

    private final JComboBox<String> cmbPayment = new JComboBox<>(new String[]{"CASH", "UPI"});
    private final JCheckBox chkCredit = new JCheckBox("Khata (Credit)");

    private final JTextField txtDiscount = new JTextField("0.00", 8);
    private final JLabel lblSubtotal = new JLabel("0.00");
    private final JLabel lblGst = new JLabel("0.00");
    private final JLabel lblFinal = new JLabel("0.00");

    private final JButton btnAddToCart = new JButton("Add to Cart");
    private final JButton btnRemove = new JButton("Remove Item");
    private final JButton btnClearCart = new JButton("Clear Cart");
    private final JButton btnGenerate = new JButton("Generate Bill");

    public BillingPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.48);
        add(split, BorderLayout.CENTER);

        split.setLeftComponent(buildProductsPane());
        split.setRightComponent(buildCartPane());

        installShortcuts();
        loadProducts("");
        recalcTotals();
    }

    private Component buildProductsPane() {
        JPanel left = new JPanel(new BorderLayout(10, 10));
        left.setBorder(BorderFactory.createTitledBorder("Products"));

        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel hint = new JLabel("Search (Name/Barcode/ID):");
        hint.setFont(hint.getFont().deriveFont(Font.BOLD));
        search.add(hint);
        search.add(txtProductSearch);
        JButton btnRefresh = new JButton("Refresh");
        search.add(btnRefresh);
        left.add(search, BorderLayout.NORTH);

        prodTable.setRowHeight(26);
        left.add(new JScrollPane(prodTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        btnAddToCart.setPreferredSize(new Dimension(150, 40));
        btnAddToCart.setFont(btnAddToCart.getFont().deriveFont(Font.BOLD, 15f));
        actions.add(btnAddToCart);
        left.add(actions, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadProducts(txtProductSearch.getText().trim()));
        btnAddToCart.addActionListener(e -> addSelectedProduct());

        txtProductSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { loadProducts(txtProductSearch.getText().trim()); }
            @Override public void removeUpdate(DocumentEvent e) { loadProducts(txtProductSearch.getText().trim()); }
            @Override public void changedUpdate(DocumentEvent e) { loadProducts(txtProductSearch.getText().trim()); }
        });

        prodTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) addSelectedProduct();
            }
        });

        return left;
    }

    private Component buildCartPane() {
        JPanel right = new JPanel(new BorderLayout(10, 10));
        right.setBorder(BorderFactory.createTitledBorder("Cart / Billing"));

        JPanel customer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        customer.setBorder(BorderFactory.createTitledBorder("Customer (optional)"));
        customer.add(new JLabel("Phone:"));
        customer.add(txtCustPhone);
        JButton btnFind = new JButton("Find");
        customer.add(btnFind);
        customer.add(new JLabel("Name:"));
        txtCustName.setEditable(false);
        customer.add(txtCustName);
        JButton btnAddCust = new JButton("Add New");
        customer.add(btnAddCust);
        right.add(customer, BorderLayout.NORTH);

        btnFind.addActionListener(e -> findCustomer());
        btnAddCust.addActionListener(e -> quickAddCustomer());

        cartTable.setRowHeight(26);
        right.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(10, 10));

        JPanel cartActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        btnRemove.setPreferredSize(new Dimension(140, 38));
        btnClearCart.setPreferredSize(new Dimension(120, 38));
        cartActions.add(btnRemove);
        cartActions.add(btnClearCart);
        bottom.add(cartActions, BorderLayout.WEST);

        JPanel totals = new JPanel(new GridBagLayout());
        totals.setBorder(BorderFactory.createTitledBorder("Totals"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 8, 4, 8);
        c.anchor = GridBagConstraints.WEST;

        int y = 0;
        addTotalRow(totals, c, y++, "Subtotal:", lblSubtotal);
        addTotalRow(totals, c, y++, "GST:", lblGst);
        c.gridx = 0; c.gridy = y; totals.add(new JLabel("Discount:"), c);
        c.gridx = 1; totals.add(txtDiscount, c);
        y++;
        JLabel lblFinalText = new JLabel("Final Amount:");
        lblFinalText.setFont(lblFinalText.getFont().deriveFont(Font.BOLD, 16f));
        lblFinal.setFont(lblFinal.getFont().deriveFont(Font.BOLD, 18f));
        c.gridx = 0; c.gridy = y; totals.add(lblFinalText, c);
        c.gridx = 1; totals.add(lblFinal, c);

        bottom.add(totals, BorderLayout.CENTER);

        JPanel pay = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        pay.setBorder(BorderFactory.createTitledBorder("Payment"));
        cmbPayment.setPreferredSize(new Dimension(120, 30));
        pay.add(new JLabel("Method:"));
        pay.add(cmbPayment);
        pay.add(chkCredit);
        btnGenerate.setPreferredSize(new Dimension(170, 45));
        btnGenerate.setFont(btnGenerate.getFont().deriveFont(Font.BOLD, 16f));
        pay.add(btnGenerate);
        bottom.add(pay, BorderLayout.EAST);

        right.add(bottom, BorderLayout.SOUTH);

        btnRemove.addActionListener(e -> removeSelectedCartItem());
        btnClearCart.addActionListener(e -> clearCart());
        btnGenerate.addActionListener(e -> generateBill());

        txtDiscount.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalcTotals(); }
            @Override public void removeUpdate(DocumentEvent e) { recalcTotals(); }
            @Override public void changedUpdate(DocumentEvent e) { recalcTotals(); }
        });

        cartTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) changeCartQty();
            }
        });

        return right;
    }

    private void addTotalRow(JPanel p, GridBagConstraints c, int y, String label, JLabel value) {
        c.gridx = 0; c.gridy = y;
        p.add(new JLabel(label), c);
        c.gridx = 1;
        p.add(value, c);
    }

    private void loadProducts(String q) {
        try {
            ProductDAO dao = new ProductDAO();
            List<Product> products;
            if (q == null || q.isBlank()) {
                products = dao.listAll();
            } else if (q.matches("\\d+")) {
                // allow quick ID search
                Product p = dao.getById(Integer.parseInt(q));
                products = new ArrayList<>();
                if (p != null) products.add(p);
                products.addAll(dao.searchByNameOrBarcode(q));
            } else {
                products = dao.searchByNameOrBarcode(q);
            }
            prodModel.setRowCount(0);
            for (Product p : products) {
                prodModel.addRow(new Object[]{p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getGstPercent(), p.getStock()});
            }
            if (prodModel.getRowCount() > 0) prodTable.setRowSelectionInterval(0, 0);
        } catch (Exception ex) {
            SwingUtil.error(this, "Failed to load products.", ex);
        }
    }

    private void addSelectedProduct() {
        int row = prodTable.getSelectedRow();
        if (row < 0) {
            SwingUtil.info(this, "Select a product.");
            return;
        }
        int id = Integer.parseInt(prodModel.getValueAt(row, 0).toString());
        try {
            Product p = new ProductDAO().getById(id);
            if (p == null) {
                SwingUtil.info(this, "Product not found.");
                return;
            }
            if (p.getStock() <= 0) {
                SwingUtil.info(this, "Out of stock.");
                return;
            }

            int qty = askQty(1, p.getStock());
            if (qty <= 0) return;

            CartItem existing = findInCart(p.getId());
            if (existing != null) {
                int newQty = existing.getQuantity() + qty;
                if (newQty > p.getStock()) {
                    SwingUtil.info(this, "Stock available: " + p.getStock());
                    return;
                }
                existing.setQuantity(newQty);
            } else {
                cart.add(new CartItem(p, qty));
            }
            refreshCartTable();
            recalcTotals();
        } catch (Exception ex) {
            SwingUtil.error(this, "Failed to add to cart.", ex);
        }
    }

    private int askQty(int defaultQty, int maxQty) {
        String s = JOptionPane.showInputDialog(this, "Enter quantity (max " + maxQty + "):", defaultQty);
        if (s == null) return 0;
        try {
            int q = Integer.parseInt(s.trim());
            if (q <= 0) return 0;
            if (q > maxQty) q = maxQty;
            return q;
        } catch (Exception e) {
            SwingUtil.info(this, "Invalid quantity.");
            return 0;
        }
    }

    private CartItem findInCart(int productId) {
        for (CartItem it : cart) if (it.getProduct().getId() == productId) return it;
        return null;
    }

    private void refreshCartTable() {
        cartModel.setRowCount(0);
        for (CartItem it : cart) {
            Product p = it.getProduct();
            cartModel.addRow(new Object[]{
                    p.getId(),
                    p.getName(),
                    it.getQuantity(),
                    p.getPrice(),
                    p.getGstPercent(),
                    it.lineSubtotal(),
                    it.lineGst(),
                    it.lineTotal()
            });
        }
    }

    private void recalcTotals() {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal gst = BigDecimal.ZERO;
        for (CartItem it : cart) {
            subtotal = subtotal.add(it.lineSubtotal());
            gst = gst.add(it.lineGst());
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        gst = gst.setScale(2, RoundingMode.HALF_UP);

        BigDecimal discount = parseMoney(txtDiscount.getText());
        BigDecimal finalAmt = subtotal.add(gst).subtract(discount).setScale(2, RoundingMode.HALF_UP);
        if (finalAmt.compareTo(BigDecimal.ZERO) < 0) finalAmt = BigDecimal.ZERO;

        lblSubtotal.setText(subtotal.toPlainString());
        lblGst.setText(gst.toPlainString());
        lblFinal.setText(finalAmt.toPlainString());
    }

    private BigDecimal parseMoney(String s) {
        try {
            if (s == null || s.isBlank()) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return new BigDecimal(s.trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    private void removeSelectedCartItem() {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            SwingUtil.info(this, "Select an item in cart.");
            return;
        }
        int pid = Integer.parseInt(cartModel.getValueAt(row, 0).toString());
        cart.removeIf(it -> it.getProduct().getId() == pid);
        refreshCartTable();
        recalcTotals();
    }

    private void changeCartQty() {
        int row = cartTable.getSelectedRow();
        if (row < 0) return;
        int pid = Integer.parseInt(cartModel.getValueAt(row, 0).toString());
        CartItem it = findInCart(pid);
        if (it == null) return;
        try {
            Product fresh = new ProductDAO().getById(pid);
            int max = fresh == null ? it.getQuantity() : Math.max(fresh.getStock(), it.getQuantity());
            int qty = askQty(it.getQuantity(), max);
            if (qty <= 0) return;
            it.setQuantity(qty);
            refreshCartTable();
            recalcTotals();
        } catch (Exception ex) {
            SwingUtil.error(this, "Failed to update quantity.", ex);
        }
    }

    private void clearCart() {
        if (!cart.isEmpty() && !SwingUtil.confirm(this, "Clear cart?")) return;
        cart.clear();
        refreshCartTable();
        recalcTotals();
    }

    private void findCustomer() {
        try {
            String phone = txtCustPhone.getText().trim();
            if (phone.isBlank()) {
                selectedCustomer = null;
                txtCustName.setText("");
                return;
            }
            Customer c = new CustomerDAO().findByPhone(phone);
            selectedCustomer = c;
            txtCustName.setText(c == null ? "" : c.getName());
            if (c == null) SwingUtil.info(this, "Customer not found. Use 'Add New' if needed.");
        } catch (Exception ex) {
            SwingUtil.error(this, "Failed to find customer.", ex);
        }
    }

    private void quickAddCustomer() {
        try {
            String phone = txtCustPhone.getText().trim();
            if (phone.isBlank()) {
                SwingUtil.info(this, "Enter phone first.");
                return;
            }
            CustomerDAO dao = new CustomerDAO();
            Customer existing = dao.findByPhone(phone);
            if (existing != null) {
                selectedCustomer = existing;
                txtCustName.setText(existing.getName());
                return;
            }
            String name = JOptionPane.showInputDialog(this, "Customer name:", "");
            if (name == null || name.trim().isEmpty()) return;
            Customer c = new Customer();
            c.setName(name.trim());
            c.setPhone(phone);
            int id = dao.add(c);
            c.setId(id);
            selectedCustomer = c;
            txtCustName.setText(c.getName());
            SwingUtil.info(this, "Customer added.");
        } catch (Exception ex) {
            SwingUtil.error(this, "Failed to add customer.", ex);
        }
    }

    private void generateBill() {
        if (cart.isEmpty()) {
            SwingUtil.info(this, "Cart is empty.");
            return;
        }
        btnGenerate.setEnabled(false);
        try {
            BigDecimal discount = parseMoney(txtDiscount.getText());
            String method = (String) cmbPayment.getSelectedItem();
            boolean credit = chkCredit.isSelected();

            BillingService.SaleResult res = new BillingService().createSale(
                    selectedCustomer == null ? null : selectedCustomer.getId(),
                    AppSession.getCurrentUser().getId(),
                    method,
                    credit,
                    discount,
                    cart
            );

            String bill = buildBillText(res.invoiceNo, method, credit, discount);
            showBillDialog(bill);

            // Reset
            cart.clear();
            refreshCartTable();
            txtDiscount.setText("0.00");
            recalcTotals();
            loadProducts(txtProductSearch.getText().trim());
        } catch (Exception ex) {
            SwingUtil.error(this, "Failed to generate bill.", ex);
        } finally {
            btnGenerate.setEnabled(true);
        }
    }

    private String buildBillText(String invoiceNo, String paymentMethod, boolean credit, BigDecimal discount) {
        AppConfig cfg = AppConfig.getInstance();
        String shop = cfg.get("shop.name", "Yogasri Utensils");
        String phone = cfg.get("shop.phone", "");
        String addr = cfg.get("shop.address", "");

        StringBuilder sb = new StringBuilder();
        sb.append(shop).append("\n");
        if (!addr.isBlank()) sb.append(addr).append("\n");
        if (!phone.isBlank()) sb.append("Phone: ").append(phone).append("\n");
        sb.append("----------------------------------------\n");
        sb.append("Invoice: ").append(invoiceNo).append("\n");
        sb.append("Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        sb.append("Cashier: ").append(AppSession.getCurrentUser().getUsername()).append("\n");
        if (selectedCustomer != null) {
            sb.append("Customer: ").append(selectedCustomer.getName()).append(" (").append(selectedCustomer.getPhone()).append(")\n");
        }
        sb.append("Pay: ").append(paymentMethod);
        if (credit) sb.append("  [CREDIT]");
        sb.append("\n");
        sb.append("----------------------------------------\n");
        sb.append(String.format("%-18s %3s %8s\n", "Item", "Qty", "Total"));
        sb.append("----------------------------------------\n");
        for (CartItem it : cart) {
            String name = it.getProduct().getName();
            if (name.length() > 18) name = name.substring(0, 18);
            sb.append(String.format("%-18s %3d %8.2f\n", name, it.getQuantity(), it.lineTotal().doubleValue()));
        }
        sb.append("----------------------------------------\n");
        BigDecimal subtotal = new BigDecimal(lblSubtotal.getText());
        BigDecimal gst = new BigDecimal(lblGst.getText());
        BigDecimal finalAmt = new BigDecimal(lblFinal.getText());
        sb.append(String.format("%-22s %10.2f\n", "Subtotal:", subtotal.doubleValue()));
        sb.append(String.format("%-22s %10.2f\n", "GST:", gst.doubleValue()));
        sb.append(String.format("%-22s %10.2f\n", "Discount:", discount.doubleValue()));
        sb.append(String.format("%-22s %10.2f\n", "Final Amount:", finalAmt.doubleValue()));
        sb.append("----------------------------------------\n");
        sb.append("Thank you! Visit again.\n");
        return sb.toString();
    }

    private void showBillDialog(String billText) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Printable Bill", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(520, 650);
        dlg.setLocationRelativeTo(this);
        JTextArea area = new JTextArea(billText);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setEditable(false);
        dlg.setLayout(new BorderLayout(10, 10));
        dlg.add(new JScrollPane(area), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnPrint = new JButton("Print");
        JButton btnClose = new JButton("Close");
        actions.add(btnPrint);
        actions.add(btnClose);
        dlg.add(actions, BorderLayout.SOUTH);

        btnPrint.addActionListener(e -> {
            try {
                area.print();
            } catch (Exception ex) {
                SwingUtil.error(dlg, "Print failed.", ex);
            }
        });
        btnClose.addActionListener(e -> dlg.dispose());

        dlg.setVisible(true);
    }

    private void installShortcuts() {
        // F2: focus product search
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "focusSearch");
        getActionMap().put("focusSearch", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { txtProductSearch.requestFocusInWindow(); }
        });

        // Ctrl+G: generate bill
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK), "genBill");
        getActionMap().put("genBill", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { generateBill(); }
        });

        // Enter on product table: add to cart
        prodTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "addToCart");
        prodTable.getActionMap().put("addToCart", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { addSelectedProduct(); }
        });
    }
}

