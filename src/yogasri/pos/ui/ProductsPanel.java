package yogasri.pos.ui;

import yogasri.pos.dao.ProductDAO;
import yogasri.pos.model.Product;
import yogasri.pos.util.AppSession;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class ProductsPanel extends JPanel {
    private final JTextField txtSearch = new JTextField(25);
    private final JTable table;
    private final DefaultTableModel model;

    private final JTextField txtId = new JTextField(6);
    private final JTextField txtName = new JTextField(20);
    private final JComboBox<String> cmbCategory = new JComboBox<>(new String[]{"Steel", "Aluminium", "Plastic"});
    private final JTextField txtPrice = new JTextField(8);
    private final JTextField txtGst = new JTextField(5);
    private final JTextField txtStock = new JTextField(5);
    private final JTextField txtBarcode = new JTextField(12);

    private final JButton btnAdd = new JButton("Add");
    private final JButton btnUpdate = new JButton("Update");
    private final JButton btnDelete = new JButton("Delete");
    private final JButton btnClear = new JButton("Clear");

    public ProductsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search:"));
        top.add(txtSearch);
        JButton btnRefresh = new JButton("Refresh");
        top.add(btnRefresh);
        add(top, BorderLayout.NORTH);

        model = new DefaultTableModel(new Object[]{"ID", "Name", "Category", "Price", "GST%", "Stock", "Barcode"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(24);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel right = new JPanel(new GridBagLayout());
        right.setBorder(BorderFactory.createTitledBorder("Product Form"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;

        txtId.setEditable(false);

        int y = 0;
        addRow(right, c, y++, "ID", txtId);
        addRow(right, c, y++, "Name", txtName);
        addRow(right, c, y++, "Category", cmbCategory);
        addRow(right, c, y++, "Price", txtPrice);
        addRow(right, c, y++, "GST %", txtGst);
        addRow(right, c, y++, "Stock", txtStock);
        addRow(right, c, y++, "Barcode (opt.)", txtBarcode);

        JPanel actions = new JPanel(new GridLayout(2, 2, 8, 8));
        btnAdd.setFont(btnAdd.getFont().deriveFont(Font.BOLD, 14f));
        btnUpdate.setFont(btnUpdate.getFont().deriveFont(Font.BOLD, 14f));
        btnDelete.setFont(btnDelete.getFont().deriveFont(Font.BOLD, 14f));
        btnClear.setFont(btnClear.getFont().deriveFont(Font.BOLD, 14f));
        actions.add(btnAdd);
        actions.add(btnUpdate);
        actions.add(btnDelete);
        actions.add(btnClear);

        c.gridx = 0; c.gridy = y; c.gridwidth = 2;
        right.add(actions, c);

        add(right, BorderLayout.EAST);

        btnRefresh.addActionListener(e -> loadTableSafely(""));
        btnClear.addActionListener(e -> clearForm());
        btnAdd.addActionListener(e -> addProduct());
        btnUpdate.addActionListener(e -> updateProduct());
        btnDelete.addActionListener(e -> deleteProduct());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row < 0) return;
            txtId.setText(model.getValueAt(row, 0).toString());
            txtName.setText(model.getValueAt(row, 1).toString());
            cmbCategory.setSelectedItem(model.getValueAt(row, 2).toString());
            txtPrice.setText(model.getValueAt(row, 3).toString());
            txtGst.setText(model.getValueAt(row, 4).toString());
            txtStock.setText(model.getValueAt(row, 5).toString());
            Object bc = model.getValueAt(row, 6);
            txtBarcode.setText(bc == null ? "" : bc.toString());
        });

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { loadTableSafely(txtSearch.getText().trim()); }
            @Override public void removeUpdate(DocumentEvent e) { loadTableSafely(txtSearch.getText().trim()); }
            @Override public void changedUpdate(DocumentEvent e) { loadTableSafely(txtSearch.getText().trim()); }
        });

        // Basic role guard (staff can view/search; admin can edit)
        boolean isAdmin = "ADMIN".equalsIgnoreCase(AppSession.getCurrentUser().getRole());
        btnAdd.setEnabled(isAdmin);
        btnUpdate.setEnabled(isAdmin);
        btnDelete.setEnabled(isAdmin);

        clearForm();
        loadTableSafely("");
    }

    private void addRow(JPanel p, GridBagConstraints c, int y, String label, Component field) {
        c.gridx = 0; c.gridy = y; c.weightx = 0;
        p.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 1;
        p.add(field, c);
    }

    private void loadTableSafely(String q) {
        try {
            ProductDAO dao = new ProductDAO();
            List<Product> products = q == null || q.isBlank() ? dao.listAll() : dao.searchByNameOrBarcode(q);
            model.setRowCount(0);
            for (Product p : products) {
                model.addRow(new Object[]{
                        p.getId(),
                        p.getName(),
                        p.getCategory(),
                        p.getPrice(),
                        p.getGstPercent(),
                        p.getStock(),
                        p.getBarcode()
                });
            }
        } catch (Exception ex) {
            SwingUtil.error(this, "Failed to load products.", ex);
        }
    }

    private Product readForm() {
        Product p = new Product();
        if (!txtId.getText().isBlank()) p.setId(Integer.parseInt(txtId.getText().trim()));
        p.setName(txtName.getText().trim());
        p.setCategory((String) cmbCategory.getSelectedItem());
        p.setPrice(new BigDecimal(txtPrice.getText().trim()));
        p.setGstPercent(new BigDecimal(txtGst.getText().trim()));
        p.setStock(Integer.parseInt(txtStock.getText().trim()));
        String bc = txtBarcode.getText().trim();
        p.setBarcode(bc.isEmpty() ? null : bc);
        return p;
    }

    private void addProduct() {
        try {
            Product p = readForm();
            if (p.getName().isEmpty()) {
                SwingUtil.info(this, "Enter product name.");
                return;
            }
            int id = new ProductDAO().add(p);
            SwingUtil.info(this, "Product added (ID: " + id + ").");
            clearForm();
            loadTableSafely(txtSearch.getText().trim());
        } catch (Exception ex) {
            SwingUtil.error(this, "Failed to add product.", ex);
        }
    }

    private void updateProduct() {
        try {
            if (txtId.getText().isBlank()) {
                SwingUtil.info(this, "Select a product from table.");
                return;
            }
            Product p = readForm();
            new ProductDAO().update(p);
            SwingUtil.info(this, "Updated.");
            loadTableSafely(txtSearch.getText().trim());
        } catch (Exception ex) {
            SwingUtil.error(this, "Failed to update product.", ex);
        }
    }

    private void deleteProduct() {
        try {
            if (txtId.getText().isBlank()) {
                SwingUtil.info(this, "Select a product from table.");
                return;
            }
            if (!SwingUtil.confirm(this, "Delete this product?")) return;
            new ProductDAO().delete(Integer.parseInt(txtId.getText().trim()));
            SwingUtil.info(this, "Deleted.");
            clearForm();
            loadTableSafely(txtSearch.getText().trim());
        } catch (Exception ex) {
            SwingUtil.error(this, "Failed to delete product.", ex);
        }
    }

    private void clearForm() {
        txtId.setText("");
        txtName.setText("");
        cmbCategory.setSelectedIndex(0);
        txtPrice.setText("");
        txtGst.setText("0");
        txtStock.setText("0");
        txtBarcode.setText("");
        table.clearSelection();
        txtName.requestFocusInWindow();
    }
}

