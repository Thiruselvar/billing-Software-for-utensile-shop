package yogasri.pos.ui;

import yogasri.pos.dao.ProductDAO;
import yogasri.pos.model.Product;
import yogasri.pos.util.AppConfig;
import yogasri.pos.util.AppSession;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DashboardFrame extends JFrame {
    public DashboardFrame() {
        setTitle("Yogasri POS - Dashboard (" + AppSession.getCurrentUser().getRole() + ")");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Billing", new BillingPanel());
        tabs.addTab("Products", new ProductsPanel());
        tabs.addTab("Customers", new CustomersPanel());
        tabs.addTab("Inventory", new InventoryPanel());
        tabs.addTab("Reports", new ReportsPanel());
        setContentPane(tabs);

        SwingUtilities.invokeLater(this::showLowStockAlert);
    }

    private void showLowStockAlert() {
        try {
            int threshold = AppConfig.getInstance().getInt("low_stock_threshold", 5);
            List<Product> low = new ProductDAO().lowStock(threshold);
            if (low.isEmpty()) return;
            StringBuilder sb = new StringBuilder("Low stock items (<= " + threshold + "):\n\n");
            int count = 0;
            for (Product p : low) {
                sb.append("- ").append(p.getName()).append(" (Stock: ").append(p.getStock()).append(")\n");
                if (++count >= 15) {
                    sb.append("... and ").append(low.size() - count).append(" more\n");
                    break;
                }
            }
            SwingUtil.info(this, sb.toString());
        } catch (Exception ignored) {
        }
    }
}

