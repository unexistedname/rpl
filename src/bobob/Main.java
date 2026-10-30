package bobob;

import bobob.controller.LoginController;
import bobob.model.Database;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Database.initSchema();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(
                    null,
                    "Gagal menyiapkan database: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            new LoginController().start();
        });
    }
}
