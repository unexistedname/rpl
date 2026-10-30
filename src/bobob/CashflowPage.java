package bobob;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

public class CashflowPage extends JPanel {

    private static final String DB = "jdbc:sqlite:transaksi.db";

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter MONTH_FMT =
        DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("id", "ID"));

    private static final String[] CATEGORIES = {
        "penjualan", "modal", "gaji", "operasional", "pembelian", "lainnya"
    };

    private LocalDate anchor = LocalDate.now();
    private String mode = "Harian";

    private JLabel periodLabel;
    private JButton prevButton;
    private JButton nextButton;

    private DefaultTableModel incomeModel;
    private DefaultTableModel expenseModel;
    private RatioPanel ratioPanel;
    private JLabel incomeTotalLabel;
    private JLabel expenseTotalLabel;

    public CashflowPage() {
        setLayout(new BorderLayout(0, 12));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initDatabase();
        buildUI();
        refreshData();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB);
    }

    private void initDatabase() {
        try (Connection c = connect()) {
            initSchema(c);
        } catch (SQLException e) {
            error(e);
        }
    }

    // Dipakai juga oleh TransaksiPage agar penjualan otomatis masuk pemasukan
    static void initSchema(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS cashflow (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "jenis TEXT NOT NULL, " +
                "tanggal TEXT NOT NULL, " +
                "nama TEXT NOT NULL, " +
                "biaya REAL NOT NULL, " +
                "kategori TEXT NOT NULL, " +
                "transaksi_id INTEGER, " +
                "pengadaan_id INTEGER)"
            );

            boolean hasTransaksiId = false;
            boolean hasPengadaanId = false;
            try (ResultSet rs = s.executeQuery("PRAGMA table_info(cashflow)")) {
                while (rs.next()) {
                    if ("transaksi_id".equals(rs.getString("name"))) {
                        hasTransaksiId = true;
                    }
                    if ("pengadaan_id".equals(rs.getString("name"))) {
                        hasPengadaanId = true;
                    }
                }
            }

            if (!hasTransaksiId) {
                s.executeUpdate(
                    "ALTER TABLE cashflow ADD COLUMN transaksi_id INTEGER"
                );
            }

            if (!hasPengadaanId) {
                s.executeUpdate(
                    "ALTER TABLE cashflow ADD COLUMN pengadaan_id INTEGER"
                );
            }

            boolean hasTransaksiTable = false;
            try (ResultSet rs = s.executeQuery(
                "SELECT name FROM sqlite_master " +
                "WHERE type='table' AND name='transaksi'")) {
                hasTransaksiTable = rs.next();
            }

            // Masukkan penjualan lama yang belum tercatat di cashflow
            if (hasTransaksiTable) {
                s.executeUpdate(
                    "INSERT INTO cashflow" +
                    "(jenis,tanggal,nama,biaya,kategori,transaksi_id) " +
                    "SELECT 'PEMASUKAN', t.tanggal, t.nama_barang, " +
                    "t.harga*t.jumlah, 'penjualan', t.id " +
                    "FROM transaksi t WHERE t.jenis='PENJUALAN' " +
                    "AND NOT EXISTS (SELECT 1 FROM cashflow c " +
                    "WHERE c.transaksi_id=t.id)"
                );
            }
        }
    }

    static void saveSale(Connection c, int transaksiId, String tanggal,
                         String nama, double total) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
            "INSERT INTO cashflow" +
            "(jenis,tanggal,nama,biaya,kategori,transaksi_id) " +
            "VALUES('PEMASUKAN',?,?,?,'penjualan',?)")) {
            ps.setString(1, tanggal);
            ps.setString(2, nama);
            ps.setDouble(3, total);
            ps.setInt(4, transaksiId);
            ps.executeUpdate();
        }
    }

    static void updateSale(Connection c, int transaksiId, String tanggal,
                           String nama, double total) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
            "UPDATE cashflow SET tanggal=?,nama=?,biaya=? " +
            "WHERE transaksi_id=?")) {
            ps.setString(1, tanggal);
            ps.setString(2, nama);
            ps.setDouble(3, total);
            ps.setInt(4, transaksiId);

            if (ps.executeUpdate() == 0) {
                saveSale(c, transaksiId, tanggal, nama, total);
            }
        }
    }

    static void deleteSale(Connection c, int transaksiId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
            "DELETE FROM cashflow WHERE transaksi_id=?")) {
            ps.setInt(1, transaksiId);
            ps.executeUpdate();
        }
    }

    private void buildUI() {
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel title = new JLabel("Cashflow");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JButton addButton = new JButton("+ Tambah Transaksi");
        addButton.addActionListener(e -> transactionDialog(null));

        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(addButton, BorderLayout.EAST);

        JPanel periodBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        periodBar.setOpaque(false);

        JToggleButton daily = new JToggleButton("Harian");
        JToggleButton weekly = new JToggleButton("Mingguan");
        JToggleButton monthly = new JToggleButton("Bulanan");

        ButtonGroup group = new ButtonGroup();
        group.add(daily);
        group.add(weekly);
        group.add(monthly);
        daily.setSelected(true);

        daily.addActionListener(e -> setMode("Harian"));
        weekly.addActionListener(e -> setMode("Mingguan"));
        monthly.addActionListener(e -> setMode("Bulanan"));

        prevButton = new JButton("<");
        nextButton = new JButton(">");
        periodLabel = new JLabel();
        periodLabel.setFont(new Font("Arial", Font.BOLD, 14));

        prevButton.addActionListener(e -> {
            anchor = periodStart().minusDays(1);
            refreshData();
        });

        nextButton.addActionListener(e -> {
            anchor = periodEnd().plusDays(1);
            refreshData();
        });

        periodBar.add(daily);
        periodBar.add(weekly);
        periodBar.add(monthly);
        periodBar.add(Box.createHorizontalStrut(12));
        periodBar.add(prevButton);
        periodBar.add(periodLabel);
        periodBar.add(nextButton);

        top.add(titleRow, BorderLayout.NORTH);
        top.add(periodBar, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        ratioPanel = new RatioPanel();

        incomeTotalLabel = new JLabel();
        expenseTotalLabel = new JLabel();

        incomeModel = createModel();
        expenseModel = createModel();

        JPanel tables = new JPanel(new GridLayout(2, 1, 0, 12));
        tables.setOpaque(false);
        tables.add(tablePanel("Pemasukan", incomeTotalLabel,
            createTable(incomeModel)));
        tables.add(tablePanel("Pengeluaran", expenseTotalLabel,
            createTable(expenseModel)));

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(ratioPanel, BorderLayout.NORTH);
        center.add(tables, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private DefaultTableModel createModel() {
        return new DefaultTableModel(
            new Object[]{"No. Transaksi", "Tanggal", "Nama Barang",
                "Biaya", "Kategori", "Edit", "Hapus"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 5 || col == 6;
            }
        };
    }

    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(220);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);
        table.getColumnModel().getColumn(5).setPreferredWidth(70);
        table.getColumnModel().getColumn(6).setPreferredWidth(70);

        table.getColumnModel().getColumn(5).setCellRenderer(
            (t, value, selected, focus, row, col) -> new JButton("Edit")
        );

        table.getColumnModel().getColumn(6).setCellRenderer(
            (t, value, selected, focus, row, col) -> new JButton("Hapus")
        );

        table.getColumnModel().getColumn(5).setCellEditor(
            new ActionEditor(new JCheckBox(), row -> transactionDialog(
                (int) model.getValueAt(row, 0)))
        );

        table.getColumnModel().getColumn(6).setCellEditor(
            new ActionEditor(new JCheckBox(), row -> deleteTransaction(
                (int) model.getValueAt(row, 0)))
        );

        return table;
    }

    private JPanel tablePanel(String title, JLabel totalLabel, JTable table) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel label = new JLabel(title);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        totalLabel.setFont(new Font("Arial", Font.BOLD, 14));

        header.add(label, BorderLayout.WEST);
        header.add(totalLabel, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    void refreshData() {
        if (incomeModel == null) return;

        periodLabel.setText(periodText());
        prevButton.setEnabled(hasData("tanggal < ?", periodStart().toString()));
        nextButton.setEnabled(hasData("tanggal > ?", periodEnd().toString()));

        incomeModel.setRowCount(0);
        expenseModel.setRowCount(0);

        double income = 0;
        double expense = 0;

        String sql = "SELECT id,jenis,tanggal,nama,biaya,kategori " +
            "FROM cashflow WHERE tanggal BETWEEN ? AND ? " +
            "ORDER BY tanggal,id";

        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, periodStart().toString());
            ps.setString(2, periodEnd().toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double biaya = rs.getDouble("biaya");
                    boolean isIncome =
                        "PEMASUKAN".equals(rs.getString("jenis"));

                    Object[] row = {
                        rs.getInt("id"),
                        DATE_FMT.format(
                            LocalDate.parse(rs.getString("tanggal"))),
                        rs.getString("nama"),
                        money(biaya),
                        rs.getString("kategori"),
                        "Edit",
                        "Hapus"
                    };

                    if (isIncome) {
                        incomeModel.addRow(row);
                        income += biaya;
                    } else {
                        expenseModel.addRow(row);
                        expense += biaya;
                    }
                }
            }

        } catch (SQLException e) {
            error(e);
        }

        incomeTotalLabel.setText("Total: " + money(income));
        expenseTotalLabel.setText("Total: " + money(expense));
        ratioPanel.setData(income, expense);
    }

    private boolean hasData(String condition, String value) {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT COUNT(*) FROM cashflow WHERE " + condition)) {

            ps.setString(1, value);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            error(e);
            return false;
        }
    }

    private void setMode(String mode) {
        this.mode = mode;
        anchor = LocalDate.now();
        refreshData();
    }

    private LocalDate periodStart() {
        if ("Mingguan".equals(mode)) {
            return anchor.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        if ("Bulanan".equals(mode)) {
            return anchor.withDayOfMonth(1);
        }
        return anchor;
    }

    private LocalDate periodEnd() {
        if ("Mingguan".equals(mode)) {
            return periodStart().plusDays(6);
        }
        if ("Bulanan".equals(mode)) {
            LocalDate start = periodStart();
            return start.withDayOfMonth(start.lengthOfMonth());
        }
        return anchor;
    }

    private String periodText() {
        LocalDate start = periodStart();
        LocalDate end = periodEnd();

        if ("Bulanan".equals(mode)) {
            return start.format(MONTH_FMT);
        }
        if ("Mingguan".equals(mode)) {
            return DATE_FMT.format(start) + " - " + DATE_FMT.format(end);
        }
        return DATE_FMT.format(start);
    }

    private void transactionDialog(Integer id) {
        String jenis = "PEMASUKAN";
        String tanggal = LocalDate.now().toString();
        String nama = "";
        String biaya = "";
        String kategori = "lainnya";

        if (id != null) {
            try (Connection c = connect();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT jenis,tanggal,nama,biaya,kategori " +
                     "FROM cashflow WHERE id=?")) {

                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return;

                    jenis = rs.getString("jenis");
                    tanggal = rs.getString("tanggal");
                    nama = rs.getString("nama");
                    biaya = String.valueOf(rs.getDouble("biaya"));
                    kategori = rs.getString("kategori");
                }

            } catch (SQLException e) {
                error(e);
                return;
            }
        }

        JComboBox<String> typeBox = new JComboBox<>(
            new String[]{"Pemasukan", "Pengeluaran"});
        typeBox.setSelectedItem(
            "PEMASUKAN".equals(jenis) ? "Pemasukan" : "Pengeluaran");

        JTextField dateField = new JTextField(tanggal);
        JTextField nameField = new JTextField(nama);
        JTextField amountField = new JTextField(biaya);

        JComboBox<String> categoryBox = new JComboBox<>(CATEGORIES);
        categoryBox.setEditable(true);
        categoryBox.setSelectedItem(kategori);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Jenis:"));
        form.add(typeBox);
        form.add(new JLabel("Tanggal (yyyy-mm-dd):"));
        form.add(dateField);
        form.add(new JLabel("Nama / Barang:"));
        form.add(nameField);
        form.add(new JLabel("Biaya Total:"));
        form.add(amountField);
        form.add(new JLabel("Kategori:"));
        form.add(categoryBox);

        int result = JOptionPane.showConfirmDialog(
            this, form,
            id == null ? "Tambah Transaksi" : "Edit Transaksi",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) return;

        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            String name = nameField.getText().trim();
            double amount = Double.parseDouble(amountField.getText().trim());

            Object selected = categoryBox.getEditor().getItem();
            String category =
                selected == null ? "" : selected.toString().trim();

            if (name.isEmpty() || category.isEmpty() || amount < 0) {
                throw new IllegalArgumentException();
            }

            String newJenis = "Pemasukan".equals(typeBox.getSelectedItem())
                ? "PEMASUKAN" : "PENGELUARAN";

            try (Connection c = connect()) {
                if (id == null) {
                    try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO cashflow" +
                        "(jenis,tanggal,nama,biaya,kategori) " +
                        "VALUES(?,?,?,?,?)")) {
                        ps.setString(1, newJenis);
                        ps.setString(2, date.toString());
                        ps.setString(3, name);
                        ps.setDouble(4, amount);
                        ps.setString(5, category);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE cashflow SET jenis=?,tanggal=?,nama=?," +
                        "biaya=?,kategori=? WHERE id=?")) {
                        ps.setString(1, newJenis);
                        ps.setString(2, date.toString());
                        ps.setString(3, name);
                        ps.setDouble(4, amount);
                        ps.setString(5, category);
                        ps.setInt(6, id);
                        ps.executeUpdate();
                    }
                }
            }

            // shortcut: edit di sini hanya mengubah catatan cashflow,
            // transaksi penjualan di halaman Transaksi tidak ikut berubah
            anchor = date;
            refreshData();

        } catch (java.time.format.DateTimeParseException e) {
            JOptionPane.showMessageDialog(
                this, "Format tanggal harus yyyy-mm-dd.");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                this, "Biaya harus berupa angka yang valid.");
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(
                this, "Nama dan kategori wajib diisi, " +
                "biaya tidak boleh negatif.");
        } catch (SQLException e) {
            error(e);
        }
    }

    private void deleteTransaction(int id) {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Hapus transaksi #" + id + "? Jika ini penjualan, " +
            "riwayat transaksi ikut terhapus dan stok barang dikembalikan.",
            "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            deleteCashflowEntry(id);
            refreshData();
        } catch (SQLException e) {
            error(e);
        }
    }

    void deleteCashflowEntry(int id) throws SQLException {
        try (Connection c = connect()) {
            c.setAutoCommit(false);

            try {
                Integer transaksiId = null;
                Integer pengadaanId = null;

                try (PreparedStatement ps = c.prepareStatement(
                    "SELECT transaksi_id,pengadaan_id FROM cashflow WHERE id=?")) {
                    ps.setInt(1, id);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            transaksiId = rs.getInt("transaksi_id");
                            if (rs.wasNull()) transaksiId = null;

                            pengadaanId = rs.getInt("pengadaan_id");
                            if (rs.wasNull()) pengadaanId = null;
                        }
                    }
                }

                if (transaksiId != null) {
                    int barangId = 0;
                    int qty = 0;

                    try (PreparedStatement ps = c.prepareStatement(
                        "SELECT barang_id,jumlah FROM transaksi WHERE id=?")) {
                        ps.setInt(1, transaksiId);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                barangId = rs.getInt("barang_id");
                                qty = rs.getInt("jumlah");
                            }
                        }
                    }

                    try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM transaksi WHERE id=?")) {
                        ps.setInt(1, transaksiId);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM cashflow WHERE transaksi_id=?")) {
                        ps.setInt(1, transaksiId);
                        ps.executeUpdate();
                    }

                    if (barangId > 0) {
                        try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE barang SET stok=stok+? WHERE id=?")) {
                            ps.setInt(1, qty);
                            ps.setInt(2, barangId);
                            ps.executeUpdate();
                        }
                    }
                } else if (pengadaanId != null) {
                    int barangId = 0;
                    int qty = 0;

                    try (PreparedStatement ps = c.prepareStatement(
                        "SELECT barang_id,jumlah FROM pengadaan WHERE id=?")) {
                        ps.setInt(1, pengadaanId);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                barangId = rs.getInt("barang_id");
                                qty = rs.getInt("jumlah");
                            }
                        }
                    }

                    try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM pengadaan WHERE id=?")) {
                        ps.setInt(1, pengadaanId);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM cashflow WHERE pengadaan_id=?")) {
                        ps.setInt(1, pengadaanId);
                        ps.executeUpdate();
                    }

                    if (barangId > 0) {
                        try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE barang SET stok=stok-? " +
                            "WHERE id=? AND stok>=?")) {
                            ps.setInt(1, qty);
                            ps.setInt(2, barangId);
                            ps.setInt(3, qty);

                            if (ps.executeUpdate() != 1) {
                                throw new SQLException(
                                    "Stok barang tidak mencukupi untuk " +
                                    "membatalkan pengadaan."
                                );
                            }
                        }
                    }
                } else {
                    try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM cashflow WHERE id=?")) {
                        ps.setInt(1, id);
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
        }
    }

    private static String money(double value) {
        return "Rp " + String.format(Locale.GERMANY, "%,.0f", value);
    }

    private void error(Exception e) {
        JOptionPane.showMessageDialog(
            this, "Terjadi kesalahan: " + e.getMessage(),
            "Error", JOptionPane.ERROR_MESSAGE
        );
    }

    private static class RatioPanel extends JPanel {
        private double income;
        private double expense;

        RatioPanel() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
            ));
            setPreferredSize(new Dimension(0, 130));
        }

        void setData(double income, double expense) {
            this.income = income;
            this.expense = expense;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(new Color(30, 41, 59));
            g2.drawString("Rasio Pemasukan vs Pengeluaran", 16, 22);

            double total = income + expense;

            if (total <= 0) {
                g2.setFont(new Font("Arial", Font.PLAIN, 13));
                g2.setColor(new Color(100, 116, 139));
                g2.drawString("Belum ada data pada periode ini.", 16, 50);
                g2.dispose();
                return;
            }

            int x = 16;
            int y = 38;
            int width = getWidth() - 32;
            int height = 24;
            int incomeWidth =
                (int) Math.round(width * income / total);

            g2.setColor(new Color(34, 197, 94));
            g2.fillRoundRect(x, y, incomeWidth, height, 8, 8);

            g2.setColor(new Color(239, 68, 68));
            g2.fillRoundRect(x + incomeWidth, y,
                width - incomeWidth, height, 8, 8);

            g2.setFont(new Font("Arial", Font.PLAIN, 12));

            g2.setColor(new Color(34, 197, 94));
            g2.drawString("Pemasukan: " + money(income) +
                " (" + Math.round(income / total * 100) + "%)",
                x, y + height + 22);

            g2.setColor(new Color(239, 68, 68));
            g2.drawString("Pengeluaran: " + money(expense) +
                " (" + Math.round(expense / total * 100) + "%)",
                x, y + height + 42);

            g2.dispose();
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
