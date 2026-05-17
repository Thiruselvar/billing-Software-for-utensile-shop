package yogasri.pos.dao;

import yogasri.pos.db.DBConnection;
import yogasri.pos.model.User;
import yogasri.pos.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDAO {
    public User authenticate(String username, String passwordPlain) throws Exception {
        String sql = "SELECT id, username, role, password_hash FROM users WHERE username = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String stored = rs.getString("password_hash");
                String incoming = PasswordUtil.sha256Hex(passwordPlain);
                if (!incoming.equalsIgnoreCase(stored)) return null;
                return new User(rs.getInt("id"), rs.getString("username"), rs.getString("role"));
            }
        }
    }
}

