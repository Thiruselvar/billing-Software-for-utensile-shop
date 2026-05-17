package yogasri.pos.dao;

import yogasri.pos.db.DBConnection;
import yogasri.pos.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {
    private Customer map(ResultSet rs) throws Exception {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setPhone(rs.getString("phone"));
        return c;
    }

    public Customer findByPhone(String phone) throws Exception {
        String sql = "SELECT * FROM customers WHERE phone = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    public int add(Customer c) throws Exception {
        String sql = "INSERT INTO customers(name, phone) VALUES(?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getPhone());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                return 0;
            }
        }
    }

    public List<Customer> search(String query) throws Exception {
        String sql = "SELECT * FROM customers WHERE name LIKE ? OR phone LIKE ? ORDER BY name LIMIT 200";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + query + "%");
            ps.setString(2, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<Customer> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public List<String[]> customerHistory(int customerId) throws Exception {
        String sql = """
            SELECT s.invoice_no, DATE_FORMAT(s.sale_date, '%Y-%m-%d %H:%i') AS dt, s.final_amount, s.payment_method, s.is_credit
            FROM sales s
            WHERE s.customer_id = ?
            ORDER BY s.sale_date DESC
            LIMIT 200
            """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                List<String[]> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new String[]{
                            rs.getString("invoice_no"),
                            rs.getString("dt"),
                            rs.getBigDecimal("final_amount").toPlainString(),
                            rs.getString("payment_method"),
                            rs.getInt("is_credit") == 1 ? "YES" : "NO"
                    });
                }
                return out;
            }
        }
    }
}

