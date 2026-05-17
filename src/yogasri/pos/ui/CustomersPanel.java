package yogasri.pos.ui;

import yogasri.pos.dao.CustomerDAO;
import yogasri.pos.model.Customer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CustomersPanel extends JPanel {
    private final JTextField txtSearch = new JTextField(25);

    private final JTextField txtName = new JTextField(18);
    private final JTextField txtPhone = new JTextField(14);
    private final JButton btnAdd = new JButton("Add Customer");

    private final DefaultTableModel custModel = new DefaultTableModel(new Object[]{"ID", "Name", "Phone"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable custTable = new JTable(custModel);

    private final DefaultTableModel historyModel = new DefaultTableModel(new Object[]{"Invoice", "Date/Time", "Amount", "Pay", "Credit"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable historyTable = new JTable(historyModel);

    public CustomersPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search:"));
        top.add(txtSearch);
        add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.45);
        add(split, BorderLayout.CENTER);

        JPanel left = new JPanel(new BorderLayout(10, 10));
        JPanel addBox = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addBox.setBorder(BorderFactory.createTitledBorder("Add Customer"));
        addBox.add(new JLabel("Name:"));
        addBox.add(txtName);
        addBox.add(new JLabel("Phone:"));
        addBox.add(txtPhone);
        btnAdd.setPreferredSize(new Dimension(140, 32));
        addBox.add(btnAdd);
        left.add(addBox, BorderLayout.NORTH);

        custTable.setRowHeight(24);
        left.add(new JScrollPane(custTable), BorderLayout.CENTER);
        split.setLeftComponent(left);

        JPanel right = new JPanel(new BorderLayout(10, 10));
        right.setBorder(BorderFactory.createTitledBorder("Customer History (last 200 bills)"));
        historyTable.setRowHeight(24);
        right.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        split.setRightComponent(right);

        btnAdd.addActionListener(e -> addCustomer());

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { loadCustomers(); }
            @Override public void removeUpdate(DocumentEvent e) { loadCustomers(); }
            @Override public void changedUpdate(DocumentEvent e) { loadCustomers(); }
        });

        custTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = custTable.getSelectedRow();
            if (row < 0) return;
            int id = Integer.parseInt(custModel.getValueAt(row, 0).toString());
            loadHistory(id);
        });

        loadCustomers();
    }

    private void loadCustomers() {
        try {
            String q = txtSearch.getText().trim();
            List<Customer> list = q.isBlank() ? new CustomerDAO().search("") : new CustomerDAO().search(q);
            custModel.setRowCount(0);
            for (Customer c : list) {
                custModel.addRow(new Object[]{c.getId(), c.getName(), c.getPhone()});
            }
            historyModel.setRowCount(0);
        } catch (Exception ex) {
            SwingUtil.error(this, "Failed to load customers.", ex);
        }
    }

    private void loadHistory(int customerId) {
        try {
            List<String[]> rows = new CustomerDAO().customerHistory(customerId);
            historyModel.setRowCount(0);
            for (String[] r : rows) historyModel.addRow(r);
        } catch (Exception ex) {
            SwingUtil.error(this, "Failed to load customer history.", ex);
        }
    }

    private void addCustomer() {
        try {
            String name = txtName.getText().trim();
            String phone = txtPhone.getText().trim();
            if (name.isBlank() || phone.isBlank()) {
                SwingUtil.info(this, "Enter name and phone.");
                return;
            }
            CustomerDAO dao = new CustomerDAO();
            Customer existing = dao.findByPhone(phone);
            if (existing != null) {
                SwingUtil.info(this, "Customer already exists: " + existing.getName());
                return;
            }
            Customer c = new Customer();
            c.setName(name);
            c.setPhone(phone);
            dao.add(c);
            SwingUtil.info(this, "Customer added.");
            txtName.setText("");
            txtPhone.setText("");
            loadCustomers();
        } catch (Exception ex) {
            SwingUtil.error(this, "Failed to add customer.", ex);
        }
    }
}

