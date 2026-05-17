package yogasri.pos;

import yogasri.pos.ui.LoginFrame;
import yogasri.pos.util.AppConfig;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            AppConfig.getInstance().load();
            new LoginFrame().setVisible(true);
        });
    }
}

