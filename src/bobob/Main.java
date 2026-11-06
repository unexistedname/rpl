package bobob;

import bobob.controller.LoginController;
import bobob.model.Database;
import bobob.model.HibernateUtil;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Database.initSchema();
                HibernateUtil.init();
            } catch (SQLException | RuntimeException e) {
                JOptionPane.showMessageDialog(
                    null,
                    "Gagal menyiapkan database: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            Runtime.getRuntime().addShutdownHook(
                new Thread(HibernateUtil::shutdown));

            new LoginController().start();
        });
    }
}
