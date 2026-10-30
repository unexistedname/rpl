
package bobob;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class InventoryPage extends JPanel {

    private static final String DB = "jdbc:sqlite:transaksi.db";

    private JTable table;
    private DefaultTableModel model;
    private JComboBox<String> categoryFilter;

    public InventoryPage() {
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
                "CREATE TABLE IF NOT EXISTS kategori (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nama TEXT NOT NULL UNIQUE)"
            );

            s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS barang (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nama TEXT NOT NULL, " +
                "kategori_id INTEGER, " +
                "stok INTEGER NOT NULL DEFAULT 0, " +
                "keterangan TEXT DEFAULT '', " +
                "FOREIGN KEY(kategori_id) REFERENCES kategori(id))"
            );

            // Menambahkan relasi barang ke transaksi lama jika belum ada
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

        } catch (SQLException e) {
            error(e);
        }
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Inventory");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);

        categoryFilter = new JComboBox<>();
        categoryFilter.addActionListener(e -> refreshTable());

        JButton categoryButton = new JButton("+ Kategori");
        categoryButton.addActionListener(e -> addCategory());

        JButton addButton = new JButton("+ Tambah Barang");
        addButton.addActionListener(e -> addItem());

        actions.add(new JLabel("Kategori:"));
        actions.add(categoryFilter);
        actions.add(categoryButton);
        actions.add(addButton);

        header.add(title, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        model = new DefaultTableModel(
            new Object[]{"ID Barang", "Nama Barang", "Kategori",
                "Jumlah Stok", "Keterangan", "Atur Stok"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 5;
            }
        };

        table = new JTable(model);
        table.setRowHeight(32);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getColumnModel().getColumn(5).setCellRenderer(
            (t, value, selected, focus, row, col) -> new JButton("Atur Stok")
        );

        table.getColumnModel().getColumn(5).setCellEditor(
            new StockEditor(new JCheckBox(), row -> {
                adjustStock(row);
            })
        );

        add(new JScrollPane(table), BorderLayout.CENTER);
        loadCategories();
    }

    private void loadCategories() {
        if (categoryFilter == null) return;

        String selected = (String) categoryFilter.getSelectedItem();

        categoryFilter.removeAllItems();
        categoryFilter.addItem("Semua kategori");

        try (Connection c = connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                 "SELECT nama FROM kategori ORDER BY nama")) {

            while (rs.next()) {
                categoryFilter.addItem(rs.getString("nama"));
            }

        } catch (SQLException e) {
            error(e);
        }

        if (selected != null) {
            categoryFilter.setSelectedItem(selected);
        }

        if (categoryFilter.getSelectedIndex() < 0) {
            categoryFilter.setSelectedIndex(0);
        }
    }

    private void addCategory() {
        String nama = JOptionPane.showInputDialog(
            this, "Nama kategori baru:"
        );

        if (nama == null) return;
        nama = nama.trim();

        if (nama.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama kategori wajib diisi.");
            return;
        }

        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO kategori(nama) VALUES(?)")) {

            ps.setString(1, nama);
            ps.executeUpdate();

            loadCategories();
            categoryFilter.setSelectedItem(nama);
            refreshTable();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                this, "Kategori gagal ditambahkan. Pastikan nama kategori unik."
            );
        }
    }

    private void addItem() {
        JComboBox<Category> categoryBox = new JComboBox<>();
        loadCategoryOptions(categoryBox);

        JTextField nameField = new JTextField();
        JTextField stockField = new JTextField("0");
        JTextField noteField = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Nama Barang:"));
        form.add(nameField);
        form.add(new JLabel("Kategori:"));
        form.add(categoryBox);
        form.add(new JLabel("Stok Awal:"));
        form.add(stockField);
        form.add(new JLabel("Keterangan:"));
        form.add(noteField);

        if (categoryBox.getItemCount() == 0) {
            JOptionPane.showMessageDialog(
                this, "Tambahkan kategori terlebih dahulu."
            );
            return;
        }

        int result = JOptionPane.showConfirmDialog(
            this, form, "Tambah Barang",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        Category category = (Category) categoryBox.getSelectedItem();

        try {
            int stock = Integer.parseInt(stockField.getText().trim());

            if (name.isEmpty() || stock < 0 || category == null) {
                throw new IllegalArgumentException();
            }

            try (Connection c = connect();
                 PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO barang(nama,kategori_id,stok,keterangan) " +
                     "VALUES(?,?,?,?)")) {

                ps.setString(1, name);
                ps.setInt(2, category.id);
                ps.setInt(3, stock);
                ps.setString(4, noteField.getText().trim());
                ps.executeUpdate();
            }

            refreshTable();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                this, "Stok harus berupa bilangan bulat."
            );
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(
                this, "Nama wajib diisi dan stok tidak boleh negatif."
            );
        } catch (SQLException e) {
            error(e);
        }
    }

    private void loadCategoryOptions(JComboBox<Category> box) {
        box.removeAllItems();

        try (Connection c = connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                 "SELECT id,nama FROM kategori ORDER BY nama")) {

            while (rs.next()) {
                box.addItem(new Category(
                    rs.getInt("id"), rs.getString("nama")
                ));
            }

        } catch (SQLException e) {
            error(e);
        }
    }

    
private void adjustStock(int row) {
    int id = (int) model.getValueAt(row, 0);
    int currentStock = (int) model.getValueAt(row, 3);

    String value = JOptionPane.showInputDialog(
        this, "Masukkan jumlah stok baru:", currentStock
    );

    if (value == null) return;

    try {
        int newStock = Integer.parseInt(value.trim());

        if (newStock < 0) {
            JOptionPane.showMessageDialog(
                this, "Stok tidak boleh negatif."
            );
            return;
        }

        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE barang SET stok=? WHERE id=?")) {

            ps.setInt(1, newStock);
            ps.setInt(2, id);
            ps.executeUpdate();
        }

        refreshTable();

    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(
            this, "Masukkan bilangan bulat yang valid."
        );
    } catch (SQLException e) {
        error(e);
    }
}

    void refreshData() {
        loadCategories();
        refreshTable();
    }

    private void refreshTable() {
        if (model == null) return;

        model.setRowCount(0);

        String selected = categoryFilter == null
            ? "Semua kategori"
            : (String) categoryFilter.getSelectedItem();

        String sql =
            "SELECT b.id,b.nama,k.nama AS kategori,b.stok,b.keterangan " +
            "FROM barang b LEFT JOIN kategori k ON b.kategori_id=k.id ";

        if (selected != null && !"Semua kategori".equals(selected)) {
            sql += "WHERE k.nama=? ";
        }

        sql += "ORDER BY b.id";

        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (selected != null && !"Semua kategori".equals(selected)) {
                ps.setString(1, selected);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("nama"),
                        rs.getString("kategori"),
                        rs.getInt("stok"),
                        rs.getString("keterangan"),
                        "Atur Stok"
                    });
                }
            }

        } catch (SQLException e) {
            error(e);
        }
    }

    private void error(Exception e) {
        JOptionPane.showMessageDialog(
            this, "Terjadi kesalahan: " + e.getMessage(),
            "Error", JOptionPane.ERROR_MESSAGE
        );
    }

    private static class Category {
        int id;
        String name;

        Category(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class StockEditor extends DefaultCellEditor {
        private final JButton button = new JButton("Atur Stok");
        private final java.util.function.IntConsumer action;
        private int row;

        StockEditor(JCheckBox checkBox,
                    java.util.function.IntConsumer action) {
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
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "Atur Stok";
        }
    }
}
