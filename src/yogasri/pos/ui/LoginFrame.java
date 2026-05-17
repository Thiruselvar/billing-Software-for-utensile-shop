package yogasri.pos.ui;

import yogasri.pos.dao.UserDAO;
import yogasri.pos.model.User;
import yogasri.pos.util.AppSession;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final JTextField txtUser = new JTextField(15);
    private final JPasswordField txtPass = new JPasswordField(15);
    private final JButton btnLogin = new JButton("Login");

    public LoginFrame() {
        setTitle("Yogasri POS - Login");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(420, 220);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setContentPane(root);

        JLabel title = new JLabel("Yogasri POS System", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.EAST;
        form.add(new JLabel("Username:"), c);
        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        form.add(txtUser, c);

        c.gridx = 0; c.gridy = 1; c.anchor = GridBagConstraints.EAST;
        form.add(new JLabel("Password:"), c);
        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        form.add(txtPass, c);

        root.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnLogin.setPreferredSize(new Dimension(120, 35));
        actions.add(btnLogin);
        root.add(actions, BorderLayout.SOUTH);

        btnLogin.addActionListener(e -> doLogin());
        txtPass.addActionListener(e -> doLogin());
        getRootPane().setDefaultButton(btnLogin);
    }

    private void doLogin() {
        btnLogin.setEnabled(false);
        try {
            String u = txtUser.getText().trim();
            String p = new String(txtPass.getPassword());
            if (u.isEmpty() || p.isEmpty()) {
                SwingUtil.info(this, "Enter username and password.");
                return;
            }
            User user = new UserDAO().authenticate(u, p);
            if (user == null) {
                SwingUtil.info(this, "Invalid login.");
                return;
            }
            AppSession.setCurrentUser(user);
            new DashboardFrame().setVisible(true);
            dispose();
        } catch (Exception ex) {
            String msg = "Login failed. Check database connection/settings.";
            String em = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (em.contains("access denied")) {
                msg = "Database login failed. Update db.user/db.password in app.properties.";
            } else if (em.contains("communications link failure") || em.contains("connection refused")) {
                msg = "Cannot connect to MySQL. Start MySQL server and verify db.url in app.properties.";
            } else if (em.contains("table") && em.contains("users")) {
                msg = "Users table not found. Run SQL files in the sql folder first.";
            }
            SwingUtil.error(this, msg, ex);
        } finally {
            btnLogin.setEnabled(true);
        }
    }
}

