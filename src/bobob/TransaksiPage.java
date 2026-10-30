
package bobob;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TransaksiPage extends JPanel {

    private static final String DB = "jdbc:sqlite:transaksi.db";

    private LocalDate tanggal = LocalDate.now();
    private JLabel tanggalLabel;
    private JTable table;
    private DefaultTableModel model;

    private final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public TransaksiPage() {
        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initDatabase();
        buildUI();
        refreshTable();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB);
    }

    private void initDatabase() {
        try (Connection c = connect();
             Statement s = c.createStatement()) {

            s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS transaksi (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "jenis TEXT NOT NULL, " +
                "tanggal TEXT NOT NULL, " +
                "nama_barang TEXT NOT NULL, " +
                "harga REAL NOT NULL, " +
                "jumlah INTEGER NOT NULL)"
            );

            boolean hasBarangId = false;
            try (ResultSet rs = s.executeQuery("PRAGMA table_info(transaksi)")) {
                while (rs.next()) {
                    if ("barang_id".equals(rs.getString("name"))) {
                        hasBarangId = true;
                    }
                }
            }

            if (!hasBarangId) {
                s.executeUpdate(
                    "ALTER TABLE transaksi ADD COLUMN barang_id INTEGER"
                );
            }

            CashflowPage.initSchema(c);

        } catch (SQLException e) {
            error(e);
        }
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Transaksi Penjualan");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        datePanel.setOpaque(false);

        JButton prev = new JButton("<");
        JButton next = new JButton(">");

        tanggalLabel = new JLabel();
        tanggalLabel.setFont(new Font("Arial", Font.BOLD, 14));

        prev.addActionListener(e -> {
            tanggal = tanggal.minusDays(1);
            refreshTable();
        });

        next.addActionListener(e -> {
            tanggal = tanggal.plusDays(1);
            refreshTable();
        });

        datePanel.add(prev);
        datePanel.add(tanggalLabel);
        datePanel.add(next);

        header.add(title, BorderLayout.WEST);
        header.add(datePanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        model = new DefaultTableModel(
            new Object[]{"No. Transaksi", "Tanggal", "ID Barang",
                "Nama Barang", "Harga", "Jumlah", "Total", "Edit", "Hapus"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 7 || col == 8;
            }
        };

        table = new JTable(model);
        table.setRowHeight(30);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        table.getColumnModel().getColumn(7).setCellRenderer(
            (t, value, selected, focus, row, col) -> new JButton("Edit")
        );

        table.getColumnModel().getColumn(8).setCellRenderer(
            (t, value, selected, focus, row, col) -> new JButton("Hapus")
        );

        table.getColumnModel().getColumn(7).setCellEditor(
            new ActionEditor(new JCheckBox(), row -> editTransaction(row))
        );

        table.getColumnModel().getColumn(8).setCellEditor(
            new ActionEditor(new JCheckBox(), row -> deleteTransaction(row))
        );

        JButton addButton = new JButton("+ Tambah Penjualan");
        addButton.addActionListener(e -> transactionDialog(null));

        JPanel body = new JPanel(new BorderLayout(8, 8));
        body.setOpaque(false);
        body.add(addButton, BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);
    }

    private List<Item> getItems() throws SQLException {
        List<Item> items = new ArrayList<>();

        try (Connection c = connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                 "SELECT id,nama,stok FROM barang ORDER BY nama")) {

            while (rs.next()) {
                items.add(new Item(
                    rs.getInt("id"),
                    rs.getString("nama"),
                    rs.getInt("stok")
                ));
            }
        }

        return items;
    }

    private void transactionDialog(Integer transactionId) {
        int oldItemId = -1;
        int oldQty = 0;
        String oldName = "";
        double oldPrice = 0;

        if (transactionId != null) {
            try (Connection c = connect();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM transaksi WHERE id=?")) {

                ps.setInt(1, transactionId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return;

                    oldItemId = rs.getInt("barang_id");
                    oldQty = rs.getInt("jumlah");
                    oldName = rs.getString("nama_barang");
                    oldPrice = rs.getDouble("harga");
                }
            } catch (SQLException e) {
                error(e);
                return;
            }
        }

        JComboBox<Item> itemBox = new JComboBox<>();
        JTextField priceField = new JTextField(String.valueOf(oldPrice));
        JTextField qtyField = new JTextField(
            transactionId == null ? "1" : String.valueOf(oldQty)
        );

        try {
            for (Item item : getItems()) {
                // Barang stok nol tetap disediakan saat mengedit
                if (item.stock > 0 || item.id == oldItemId) {
                    itemBox.addItem(item);
                }
            }
        } catch (SQLException e) {
            error(e);
            return;
        }

        if (transactionId != null) {
            for (int i = 0; i < itemBox.getItemCount(); i++) {
                if (itemBox.getItemAt(i).id == oldItemId) {
                    itemBox.setSelectedIndex(i);
                    break;
                }
            }
        }

        if (itemBox.getItemCount() == 0) {
            JOptionPane.showMessageDialog(
                this, "Belum ada barang dengan stok tersedia. " +
                "Tambahkan barang di Inventory terlebih dahulu."
            );
            return;
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Barang dari Inventory:"));
        form.add(itemBox);
        form.add(new JLabel("Harga Jual Satuan:"));
        form.add(priceField);
        form.add(new JLabel("Jumlah:"));
        form.add(qtyField);

        int result = JOptionPane.showConfirmDialog(
            this, form,
            transactionId == null ? "Tambah Penjualan" : "Edit Penjualan",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) return;

        Item selected = (Item) itemBox.getSelectedItem();
        if (selected == null) return;

        try {
            double price = Double.parseDouble(priceField.getText().trim());
            int qty = Integer.parseInt(qtyField.getText().trim());

            if (price < 0 || qty <= 0) {
                throw new IllegalArgumentException();
            }

            saveTransaction(
                transactionId, selected, price, qty,
                oldItemId, oldQty
            );

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                this, "Harga dan jumlah harus berupa angka yang valid."
            );
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(
                this, "Harga tidak boleh negatif dan jumlah harus lebih dari nol."
            );
        } catch (SQLException e) {
            error(e);
        }
    }

    private void saveTransaction(
        Integer transactionId, Item item, double price, int qty,
        int oldItemId, int oldQty
    ) throws SQLException {

        try (Connection c = connect()) {
            c.setAutoCommit(false);

            try {
                // Kembalikan stok transaksi lama ketika sedang mengedit
                if (transactionId != null && oldItemId > 0) {
                    try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE barang SET stok=stok+? WHERE id=?")) {
                        ps.setInt(1, oldQty);
                        ps.setInt(2, oldItemId);
                        ps.executeUpdate();
                    }
                }

                // Kurangi stok barang yang dipilih, hanya jika cukup
                try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE barang SET stok=stok-? " +
                    "WHERE id=? AND stok>=?")) {
                    ps.setInt(1, qty);
                    ps.setInt(2, item.id);
                    ps.setInt(3, qty);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                            "Stok barang tidak mencukupi."
                        );
                    }
                }

                if (transactionId == null) {
                    try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO transaksi " +
                        "(jenis,tanggal,nama_barang,harga,jumlah,barang_id) " +
                        "VALUES('PENJUALAN',?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, tanggal.toString());
                        ps.setString(2, item.name);
                        ps.setDouble(3, price);
                        ps.setInt(4, qty);
                        ps.setInt(5, item.id);
                        ps.executeUpdate();

                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (keys.next()) {
                                CashflowPage.saveSale(
                                    c, keys.getInt(1), tanggal.toString(),
                                    item.name, price * qty
                                );
                            }
                        }
                    }
                } else {
                    try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE transaksi SET nama_barang=?,harga=?," +
                        "jumlah=?,barang_id=? WHERE id=?")) {
                        ps.setString(1, item.name);
                        ps.setDouble(2, price);
                        ps.setInt(3, qty);
                        ps.setInt(4, item.id);
                        ps.setInt(5, transactionId);
                        ps.executeUpdate();

                        CashflowPage.updateSale(
                            c, transactionId, tanggal.toString(),
                            item.name, price * qty
                        );
                    }
                }

                c.commit();

            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }

        refreshTable();
    }

    private void editTransaction(int row) {
        int id = (int) model.getValueAt(row, 0);
        transactionDialog(id);
    }

    private void deleteTransaction(int row) {
        int id = (int) model.getValueAt(row, 0);

        int confirm = JOptionPane.showConfirmDialog(
            this, "Hapus transaksi #" + id + "? Stok akan dikembalikan.",
            "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection c = connect()) {
            c.setAutoCommit(false);

            try {
                int barangId;
                int qty;

                try (PreparedStatement ps = c.prepareStatement(
                    "SELECT barang_id,jumlah FROM transaksi WHERE id=?")) {
                    ps.setInt(1, id);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return;
                        }

                        barangId = rs.getInt("barang_id");
                        qty = rs.getInt("jumlah");
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM transaksi WHERE id=?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

                CashflowPage.deleteSale(c, id);

                if (barangId > 0) {
                    try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE barang SET stok=stok+? WHERE id=?")) {
                        ps.setInt(1, qty);
                        ps.setInt(2, barangId);
                        ps.executeUpdate();
                    }
                }

                c.commit();

            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }

            refreshTable();

        } catch (SQLException e) {
            error(e);
        }
    }

    void refreshData() {
        refreshTable();
    }

    private void refreshTable() {
        if (model == null) return;

        tanggalLabel.setText(tanggal.format(formatter));
        model.setRowCount(0);

        String sql =
            "SELECT id,tanggal,barang_id,nama_barang,harga,jumlah " +
            "FROM transaksi WHERE jenis='PENJUALAN' AND tanggal=? " +
            "AND barang_id IS NOT NULL ORDER BY id";

        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, tanggal.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double price = rs.getDouble("harga");
                    int qty = rs.getInt("jumlah");

                    model.addRow(new Object[]{
                        rs.getInt("id"),
                        tanggal.format(formatter),
                        rs.getInt("barang_id"),
                        rs.getString("nama_barang"),
                        price,
                        qty,
                        price * qty,
                        "Edit",
                        "Hapus"
                    });
                }
            }

        } catch (SQLException e) {
            error(e);
        }
    }

    private void error(Exception e) {
        JOptionPane.showMessageDialog(
            this, e.getMessage(),
            "Kesalahan", JOptionPane.ERROR_MESSAGE
        );
    }

    private static class Item {
        int id;
        String name;
        int stock;

        Item(int id, String name, int stock) {
            this.id = id;
            this.name = name;
            this.stock = stock;
        }

        @Override
        public String toString() {
            return id + " - " + name + " (Stok: " + stock + ")";
        }
    }

    private static class ActionEditor extends DefaultCellEditor {
        private final JButton button = new JButton();
        private final java.util.function.IntConsumer action;
        private int row;

        ActionEditor(
            JCheckBox checkBox,
            java.util.function.IntConsumer action
        ) {
            super(checkBox);
            this.action = action;

            button.addActionListener(e -> {
                fireEditingStopped();
                action.accept(row);
            });
        }

        @Override
        public Component getTableCellEditorComponent(
            JTable table, Object value, boolean selected,
            int row, int column
        ) {
            this.row = table.convertRowIndexToModel(row);
            button.setText(value == null ? "" : value.toString());
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }
    }
}
