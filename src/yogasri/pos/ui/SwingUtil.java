package yogasri.pos.ui;

import javax.swing.*;
import java.awt.*;

public class SwingUtil {
    private SwingUtil() {}

    public static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String msg, Exception e) {
        String full = msg + (e == null ? "" : ("\n\n" + e.getMessage()));
        JOptionPane.showMessageDialog(parent, full, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String msg) {
        return JOptionPane.showConfirmDialog(parent, msg, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}

