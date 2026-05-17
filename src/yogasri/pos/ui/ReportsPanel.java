package yogasri.pos.ui;

import yogasri.pos.dao.ReportDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportsPanel extends JPanel {
    private final JTextField txtDate = new JTextField(10);
    private final JLabel lblDailyTotal = new JLabel("0.00");

    private final JTextField txtMonth = new JTextField(7); // yyyy-MM
    private final DefaultTableModel monthModel = new DefaultTableModel(new Object[]{"Date", "Bills", "Total"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable monthTable = new JTable(monthModel);

    public ReportsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new GridLayout(1, 2, 10, 10));

        JPanel daily = new JPanel(new FlowLayout(FlowLayout.LEFT));
        daily.setBorder(BorderFactory.createTitledBorder("Daily Sales"));
        txtDate.setText(LocalDate.now().toString());
        JButton btnDaily = new JButton("Get Daily Total");
        btnDaily.setPreferredSize(new Dimension(150, 32));
        lblDailyTotal.setFont(lblDailyTotal.getFont().deriveFont(Font.BOLD, 18f));
        daily.add(new JLabel("Date (yyyy-MM-dd):"));
        daily.add(txtDate);
        daily.add(btnDaily);
        daily.add(new JLabel("Total:"));
        daily.add(lblDailyTotal);
        top.add(daily);

        JPanel monthly = new JPanel(new FlowLayout(FlowLayout.LEFT));
        monthly.setBorder(BorderFactory.createTitledBorder("Monthly Summary"));
        txtMonth.setText(YearMonth.now().toString()); // yyyy-MM
        JButton btnMonth = new JButton("Load Month");
        btnMonth.setPreferredSize(new Dimension(130, 32));
        monthly.add(new JLabel("Month (yyyy-MM):"));
        monthly.add(txtMonth);
        monthly.add(btnMonth);
        top.add(monthly);

        add(top, BorderLayout.NORTH);

        monthTable.setRowHeight(24);
        add(new JScrollPane(monthTable), BorderLayout.CENTER);

        btnDaily.addActionListener(e -> loadDaily());
        btnMonth.addActionListener(e -> loadMonthly());

        loadDaily();
        loadMonthly();
    }

    private void loadDaily() {
        try {
            LocalDate d = LocalDate.parse(txtDate.getText().trim());
            BigDecimal total = new ReportDAO().dailyTotal(d);
            lblDailyTotal.setText(total.toPlainString());
        } catch (Exception ex) {
            SwingUtil.error(this, "Failed to load daily report.", ex);
        }
    }

    private void loadMonthly() {
        try {
            YearMonth ym = YearMonth.parse(txtMonth.getText().trim());
            List<String[]> rows = new ReportDAO().monthlySummary(ym);
            monthModel.setRowCount(0);
            for (String[] r : rows) monthModel.addRow(r);
            if (rows.isEmpty()) SwingUtil.info(this, "No sales for this month.");
        } catch (Exception ex) {
            SwingUtil.error(this, "Failed to load monthly summary.", ex);
        }
    }
}

