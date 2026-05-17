package yogasri.pos.dao;

import yogasri.pos.db.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {
    public BigDecimal dailyTotal(LocalDate date) throws Exception {
        String sql = "SELECT COALESCE(SUM(final_amount),0) AS total FROM sales WHERE DATE(sale_date)=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("total");
            }
        }
    }

    public List<String[]> monthlySummary(YearMonth month) throws Exception {
        String sql = """
            SELECT DATE(sale_date) AS d, COUNT(*) AS bills, SUM(final_amount) AS total
            FROM sales
            WHERE YEAR(sale_date)=? AND MONTH(sale_date)=?
            GROUP BY DATE(sale_date)
            ORDER BY d
            """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, month.getYear());
            ps.setInt(2, month.getMonthValue());
            try (ResultSet rs = ps.executeQuery()) {
                List<String[]> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new String[]{
                            rs.getDate("d").toString(),
                            rs.getString("bills"),
                            rs.getBigDecimal("total").toPlainString()
                    });
                }
                return out;
            }
        }
    }
}

