package yogasri.pos.ui;

import yogasri.pos.dao.ProductDAO;
import yogasri.pos.model.Product;
import yogasri.pos.util.AppConfig;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class InventoryPanel extends JPanel {
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Name", "Category", "Stock", "Price", "GST%"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);

    public InventoryPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAll = new JButton("View All Stock");
        JButton btnLow = new JButton("View Low Stock");
        top.add(btnAll);
        top.add(btnLow);
        add(top, BorderLayout.NORTH);

        table.setRowHeight(24);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnAll.addActionListener(e -> loadAll());
        btnLow.addActionListener(e -> loadLow());

        loadAll();
    }

    private void loadAll() {
        try {
            List<Product> products = new ProductDAO().listAll();
            setRows(products);
        } catch (Exception ex) {
            SwingUtil.error(this, "Failed to load stock.", ex);
        }
    }

    private void loadLow() {
        try {
            int threshold = AppConfig.getInstance().getInt("low_stock_threshold", 5);
            List<Product> products = new ProductDAO().lowStock(threshold);
            setRows(products);
            if (products.isEmpty()) SwingUtil.info(this, "No low-stock items.");
        } catch (Exception ex) {
            SwingUtil.error(this, "Failed to load low-stock list.", ex);
        }
    }

    private void setRows(List<Product> products) {
        model.setRowCount(0);
        for (Product p : products) {
            model.addRow(new Object[]{
                    p.getId(), p.getName(), p.getCategory(), p.getStock(), p.getPrice(), p.getGstPercent()
            });
        }
    }
}

