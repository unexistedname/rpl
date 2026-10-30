package bobob;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DistributorPage extends JPanel {

    private static final String DB = "jdbc:sqlite:transaksi.db";

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter MONTH_FMT =
        DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("id", "ID"));

    private LocalDate anchor = LocalDate.now();
    private String mode = "Harian";

    private JLabel periodLabel;
    private JButton prevButton;
    private JButton nextButton;

    private DefaultTableModel purchaseModel;
    private DefaultTableModel distributorModel;
    private DistributorChart chart;
    private JLabel purchaseTotalLabel;
    private final List<Integer> purchaseIds = new ArrayList<>();

    public DistributorPage() {
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
            CashflowPage.initSchema(c);

            try (Statement s = c.createStatement()) {
                s.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS distributor (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nama TEXT NOT NULL UNIQUE)"
                );

                s.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS pengadaan (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "tanggal TEXT NOT NULL, " +
                    "barang_id INTEGER NOT NULL, " +
                    "jumlah INTEGER NOT NULL, " +
                    "harga REAL NOT NULL, " +
                    "distributor_id INTEGER, " +
                    "FOREIGN KEY(barang_id) REFERENCES barang(id), " +
                    "FOREIGN KEY(distributor_id) REFERENCES distributor(id))"
                );
            }

        } catch (SQLException e) {
            error(e);
        }
    }

    private void buildUI() {
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel title = new JLabel("Distributor");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JButton addButton = new JButton("+ Tambah Pengadaan");
        addButton.addActionListener(e -> purchaseDialog(null));

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

        chart = new DistributorChart();

        JScrollPane chartScroll = new JScrollPane(chart);
        chartScroll.setPreferredSize(new Dimension(0, 170));
        chartScroll.setBorder(null);
        chartScroll.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chartScroll.getViewport().setBackground(Color.WHITE);

        purchaseModel = createPurchaseModel();
        distributorModel = createDistributorModel();

        purchaseTotalLabel = new JLabel();

        JTable purchaseTable = createPurchaseTable();
        JTable distributorTable = createDistributorTable();

        JPanel purchasePanel = tablePanel(
            "Pengadaan Barang", purchaseTotalLabel, purchaseTable);
        JPanel distributorPanel = distributorPanel(distributorTable);

        JSplitPane split = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT, purchasePanel, distributorPanel);
        split.setResizeWeight(0.72);
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(8);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(chartScroll, BorderLayout.NORTH);
        center.add(split, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private DefaultTableModel createPurchaseModel() {
        return new DefaultTableModel(
            new Object[]{"ID Barang", "Nama Barang", "Jumlah", "Harga",
                "Distributor", "Total", "Edit", "Hapus"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 6 || col == 7;
            }
        };
    }

    private DefaultTableModel createDistributorModel() {
        return new DefaultTableModel(
            new Object[]{"ID", "Nama Distributor", "Edit", "Hapus"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 2 || col == 3;
            }
        };
    }

    private JTable createPurchaseTable() {
        JTable table = new JTable(purchaseModel);
        table.setRowHeight(30);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(140);
        table.getColumnModel().getColumn(5).setPreferredWidth(110);
        table.getColumnModel().getColumn(6).setPreferredWidth(70);
        table.getColumnModel().getColumn(7).setPreferredWidth(70);

        table.getColumnModel().getColumn(6).setCellRenderer(
            (t, value, selected, focus, row, col) -> new JButton("Edit")
        );

        table.getColumnModel().getColumn(7).setCellRenderer(
            (t, value, selected, focus, row, col) -> new JButton("Hapus")
        );

        table.getColumnModel().getColumn(6).setCellEditor(
            new ActionEditor(new JCheckBox(), row -> purchaseDialog(
                purchaseIds.get(row)))
        );

        table.getColumnModel().getColumn(7).setCellEditor(
            new ActionEditor(new JCheckBox(), row -> deletePurchaseAction(
                purchaseIds.get(row)))
        );

        return table;
    }

    private JTable createDistributorTable() {
        JTable table = new JTable(distributorModel);
        table.setRowHeight(28);

        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);

        table.getColumnModel().getColumn(2).setCellRenderer(
            (t, value, selected, focus, row, col) -> new JButton("Edit")
        );

        table.getColumnModel().getColumn(3).setCellRenderer(
            (t, value, selected, focus, row, col) -> new JButton("Hapus")
        );

        table.getColumnModel().getColumn(2).setCellEditor(
            new ActionEditor(new JCheckBox(), row -> distributorDialog(
                (int) distributorModel.getValueAt(row, 0)))
        );

        table.getColumnModel().getColumn(3).setCellEditor(
            new ActionEditor(new JCheckBox(), row -> deleteDistributorAction(
                (int) distributorModel.getValueAt(row, 0)))
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

    private JPanel distributorPanel(JTable table) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel label = new JLabel("Daftar Distributor");
        label.setFont(new Font("Arial", Font.BOLD, 16));

        JButton addButton = new JButton("+ Distributor");
        addButton.addActionListener(e -> distributorDialog(null));

        header.add(label, BorderLayout.WEST);
        header.add(addButton, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    void refreshData() {
        if (purchaseModel == null) return;

        periodLabel.setText(periodText());
        prevButton.setEnabled(hasData("tanggal < ?", periodStart().toString()));
        nextButton.setEnabled(hasData("tanggal > ?", periodEnd().toString()));

        purchaseModel.setRowCount(0);
        purchaseIds.clear();
        distributorModel.setRowCount(0);

        double total = 0;
        Map<String, Double> byDistributor = new LinkedHashMap<>();

        String sql =
            "SELECT p.id,p.barang_id,b.nama,p.jumlah,p.harga," +
            "d.nama AS distributor FROM pengadaan p " +
            "LEFT JOIN barang b ON p.barang_id=b.id " +
            "LEFT JOIN distributor d ON p.distributor_id=d.id " +
            "WHERE p.tanggal BETWEEN ? AND ? " +
            "ORDER BY p.tanggal,p.id";

        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, periodStart().toString());
            ps.setString(2, periodEnd().toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int jumlah = rs.getInt("jumlah");
                    double harga = rs.getDouble("harga");
                    double rowTotal = harga * jumlah;

                    String distributor = rs.getString("distributor");
                    if (distributor == null) {
                        distributor = "Tanpa distributor";
                    }

                    purchaseModel.addRow(new Object[]{
                        rs.getInt("barang_id"),
                        rs.getString("nama"),
                        jumlah,
                        money(harga),
                        distributor,
                        money(rowTotal),
                        "Edit",
                        "Hapus"
                    });

                    purchaseIds.add(rs.getInt("id"));
                    total += rowTotal;

                    Double current = byDistributor.get(distributor);
                    byDistributor.put(distributor,
                        current == null ? rowTotal : current + rowTotal);
                }
            }

        } catch (SQLException e) {
            error(e);
        }

        purchaseTotalLabel.setText("Total: " + money(total));
        chart.setData(byDistributor);

        try (Connection c = connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                 "SELECT id,nama FROM distributor ORDER BY nama")) {

            while (rs.next()) {
                distributorModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nama"),
                    "Edit",
                    "Hapus"
                });
            }

        } catch (SQLException e) {
            error(e);
        }
    }

    private boolean hasData(String condition, String value) {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT COUNT(*) FROM pengadaan WHERE " + condition)) {

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

    private void purchaseDialog(Integer id) {
        String tanggal = LocalDate.now().toString();
        int oldBarangId = -1;
        int oldJumlah = 1;
        double oldHarga = 0;
        Integer oldDistributorId = null;

        if (id != null) {
            try (Connection c = connect();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT tanggal,barang_id,jumlah,harga,distributor_id " +
                     "FROM pengadaan WHERE id=?")) {

                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return;

                    tanggal = rs.getString("tanggal");
                    oldBarangId = rs.getInt("barang_id");
                    oldJumlah = rs.getInt("jumlah");
                    oldHarga = rs.getDouble("harga");

                    oldDistributorId = rs.getInt("distributor_id");
                    if (rs.wasNull()) oldDistributorId = null;
                }

            } catch (SQLException e) {
                error(e);
                return;
            }
        }

        List<Item> items;
        List<Distributor> distributors;

        try {
            items = loadItems();
            distributors = loadDistributors();
        } catch (SQLException e) {
            error(e);
            return;
        }

        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(
                this, "Tambahkan barang di Inventory terlebih dahulu.");
            return;
        }

        if (distributors.isEmpty()) {
            JOptionPane.showMessageDialog(
                this, "Tambahkan distributor terlebih dahulu.");
            return;
        }

        JComboBox<Item> itemBox = new JComboBox<>();
        for (Item item : items) {
            itemBox.addItem(item);
        }

        JComboBox<Distributor> distributorBox = new JComboBox<>();
        for (Distributor distributor : distributors) {
            distributorBox.addItem(distributor);
        }

        if (oldBarangId > 0) {
            for (int i = 0; i < itemBox.getItemCount(); i++) {
                if (itemBox.getItemAt(i).id == oldBarangId) {
                    itemBox.setSelectedIndex(i);
                    break;
                }
            }
        }

        if (oldDistributorId != null) {
            for (int i = 0; i < distributorBox.getItemCount(); i++) {
                if (distributorBox.getItemAt(i).id == oldDistributorId) {
                    distributorBox.setSelectedIndex(i);
                    break;
                }
            }
        }

        JTextField dateField = new JTextField(tanggal);
        JTextField qtyField = new JTextField(
            id == null ? "1" : String.valueOf(oldJumlah));
        JTextField priceField = new JTextField(
            id == null ? "" : String.valueOf(oldHarga));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Barang dari Inventory:"));
        form.add(itemBox);
        form.add(new JLabel("Distributor:"));
        form.add(distributorBox);
        form.add(new JLabel("Tanggal (yyyy-mm-dd):"));
        form.add(dateField);
        form.add(new JLabel("Jumlah Barang:"));
        form.add(qtyField);
        form.add(new JLabel("Harga Satuan:"));
        form.add(priceField);

        int result = JOptionPane.showConfirmDialog(
            this, form,
            id == null ? "Tambah Pengadaan" : "Edit Pengadaan",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) return;

        Item item = (Item) itemBox.getSelectedItem();
        Distributor distributor =
            (Distributor) distributorBox.getSelectedItem();

        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            int qty = Integer.parseInt(qtyField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());

            if (item == null || distributor == null ||
                qty <= 0 || price < 0) {
                throw new IllegalArgumentException();
            }

            savePurchase(id, date.toString(), item.id, qty, price,
                distributor.id);

            anchor = date;
            refreshData();

        } catch (java.time.format.DateTimeParseException e) {
            JOptionPane.showMessageDialog(
                this, "Format tanggal harus yyyy-mm-dd.");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                this, "Jumlah dan harga harus berupa angka yang valid.");
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(
                this, "Jumlah harus lebih dari nol dan harga tidak " +
                "boleh negatif.");
        } catch (SQLException e) {
            error(e);
        }
    }

    private void deletePurchaseAction(int id) {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Hapus pengadaan #" + id + "? Stok barang akan dikurangi " +
            "dan pengeluaran cashflow ikut terhapus.",
            "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            deletePurchase(id);
            refreshData();
        } catch (SQLException e) {
            error(e);
        }
    }

    private void distributorDialog(Integer id) {
        String nama = "";

        if (id != null) {
            try (Connection c = connect();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT nama FROM distributor WHERE id=?")) {

                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return;
                    nama = rs.getString("nama");
                }

            } catch (SQLException e) {
                error(e);
                return;
            }
        }

        String value = JOptionPane.showInputDialog(
            this, "Nama distributor:", nama);

        if (value == null) return;
        value = value.trim();

        if (value.isEmpty()) {
            JOptionPane.showMessageDialog(
                this, "Nama distributor wajib diisi.");
            return;
        }

        try {
            saveDistributor(id, value);
            refreshData();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                this, "Gagal menyimpan distributor. " +
                "Pastikan nama distributor unik.");
        }
    }

    private void deleteDistributorAction(int id) {
        int confirm = JOptionPane.showConfirmDialog(
            this, "Hapus distributor #" + id + "?",
            "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            deleteDistributor(id);
            refreshData();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                this, e.getMessage(), "Tidak Bisa Menghapus",
                JOptionPane.WARNING_MESSAGE);
        }
    }

    void savePurchase(Integer id, String tanggal, int barangId, int jumlah,
                      double harga, Integer distributorId)
                      throws SQLException {

        try (Connection c = connect()) {
            c.setAutoCommit(false);

            try {
                String namaBarang = itemName(c, barangId);
                double total = jumlah * harga;

                if (id == null) {
                    int pengadaanId;

                    try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO pengadaan" +
                        "(tanggal,barang_id,jumlah,harga,distributor_id) " +
                        "VALUES(?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {

                        ps.setString(1, tanggal);
                        ps.setInt(2, barangId);
                        ps.setInt(3, jumlah);
                        ps.setDouble(4, harga);

                        if (distributorId == null) {
                            ps.setNull(5, Types.INTEGER);
                        } else {
                            ps.setInt(5, distributorId);
                        }

                        ps.executeUpdate();

                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new SQLException(
                                    "Gagal menyimpan pengadaan.");
                            }
                            pengadaanId = keys.getInt(1);
                        }
                    }

                    adjustStock(c, barangId, jumlah);
                    insertCashflow(c, pengadaanId, tanggal,
                        namaBarang, total);

                } else {
                    int oldBarangId = 0;
                    int oldJumlah = 0;

                    try (PreparedStatement ps = c.prepareStatement(
                        "SELECT barang_id,jumlah FROM pengadaan WHERE id=?")) {
                        ps.setInt(1, id);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new SQLException(
                                    "Pengadaan tidak ditemukan.");
                            }
                            oldBarangId = rs.getInt("barang_id");
                            oldJumlah = rs.getInt("jumlah");
                        }
                    }

                    try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE pengadaan SET tanggal=?,barang_id=?," +
                        "jumlah=?,harga=?,distributor_id=? WHERE id=?")) {

                        ps.setString(1, tanggal);
                        ps.setInt(2, barangId);
                        ps.setInt(3, jumlah);
                        ps.setDouble(4, harga);

                        if (distributorId == null) {
                            ps.setNull(5, Types.INTEGER);
                        } else {
                            ps.setInt(5, distributorId);
                        }

                        ps.setInt(6, id);
                        ps.executeUpdate();
                    }

                    if (oldBarangId == barangId) {
                        adjustStock(c, barangId, jumlah - oldJumlah);
                    } else {
                        adjustStock(c, oldBarangId, -oldJumlah);
                        adjustStock(c, barangId, jumlah);
                    }

                    updateCashflow(c, id, tanggal, namaBarang, total);
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

    void deletePurchase(int id) throws SQLException {
        try (Connection c = connect()) {
            c.setAutoCommit(false);

            try {
                int barangId = 0;
                int qty = 0;

                try (PreparedStatement ps = c.prepareStatement(
                    "SELECT barang_id,jumlah FROM pengadaan WHERE id=?")) {
                    ps.setInt(1, id);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            barangId = rs.getInt("barang_id");
                            qty = rs.getInt("jumlah");
                        }
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM cashflow WHERE pengadaan_id=?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM pengadaan WHERE id=?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

                if (barangId > 0) {
                    adjustStock(c, barangId, -qty);
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

    void saveDistributor(Integer id, String nama) throws SQLException {
        try (Connection c = connect()) {
            if (id == null) {
                try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO distributor(nama) VALUES(?)")) {
                    ps.setString(1, nama);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE distributor SET nama=? WHERE id=?")) {
                    ps.setString(1, nama);
                    ps.setInt(2, id);
                    ps.executeUpdate();
                }
            }
        }
    }

    void deleteDistributor(int id) throws SQLException {
        try (Connection c = connect()) {
            try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM pengadaan WHERE distributor_id=?")) {
                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new SQLException(
                            "Distributor masih dipakai pada transaksi " +
                            "pengadaan.");
                    }
                }
            }

            try (PreparedStatement ps = c.prepareStatement(
                "DELETE FROM distributor WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        }
    }

    private List<Item> loadItems() throws SQLException {
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

    private List<Distributor> loadDistributors() throws SQLException {
        List<Distributor> distributors = new ArrayList<>();

        try (Connection c = connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                 "SELECT id,nama FROM distributor ORDER BY nama")) {

            while (rs.next()) {
                distributors.add(new Distributor(
                    rs.getInt("id"),
                    rs.getString("nama")
                ));
            }
        }

        return distributors;
    }

    private static String itemName(Connection c, int barangId)
                                   throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
            "SELECT nama FROM barang WHERE id=?")) {

            ps.setInt(1, barangId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Barang tidak ditemukan.");
                }
                return rs.getString(1);
            }
        }
    }

    private static void adjustStock(Connection c, int barangId, int delta)
                                    throws SQLException {
        if (delta == 0) return;

        if (delta > 0) {
            try (PreparedStatement ps = c.prepareStatement(
                "UPDATE barang SET stok=stok+? WHERE id=?")) {
                ps.setInt(1, delta);
                ps.setInt(2, barangId);

                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Barang tidak ditemukan.");
                }
            }
        } else {
            int qty = -delta;

            try (PreparedStatement ps = c.prepareStatement(
                "UPDATE barang SET stok=stok-? WHERE id=? AND stok>=?")) {
                ps.setInt(1, qty);
                ps.setInt(2, barangId);
                ps.setInt(3, qty);

                if (ps.executeUpdate() != 1) {
                    throw new SQLException(
                        "Stok barang tidak mencukupi untuk mengurangi " +
                        "atau membatalkan pengadaan.");
                }
            }
        }
    }

    private static void insertCashflow(Connection c, int pengadaanId,
                                       String tanggal, String nama,
                                       double total) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
            "INSERT INTO cashflow" +
            "(jenis,tanggal,nama,biaya,kategori,pengadaan_id) " +
            "VALUES('PENGELUARAN',?,?,?,'pengadaan',?)")) {

            ps.setString(1, tanggal);
            ps.setString(2, nama);
            ps.setDouble(3, total);
            ps.setInt(4, pengadaanId);
            ps.executeUpdate();
        }
    }

    private static void updateCashflow(Connection c, int pengadaanId,
                                       String tanggal, String nama,
                                       double total) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
            "UPDATE cashflow SET tanggal=?,nama=?,biaya=?," +
            "kategori='pengadaan' WHERE pengadaan_id=?")) {

            ps.setString(1, tanggal);
            ps.setString(2, nama);
            ps.setDouble(3, total);
            ps.setInt(4, pengadaanId);

            if (ps.executeUpdate() == 0) {
                insertCashflow(c, pengadaanId, tanggal, nama, total);
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

    private static class Distributor {
        int id;
        String name;

        Distributor(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class DistributorChart extends JPanel {
        private final List<Map.Entry<String, Double>> rows =
            new ArrayList<>();

        DistributorChart() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
            ));
            setPreferredSize(new Dimension(0, 120));
        }

        void setData(Map<String, Double> data) {
            rows.clear();

            List<Map.Entry<String, Double>> sorted =
                new ArrayList<>(data.entrySet());
            sorted.sort((a, b) ->
                Double.compare(b.getValue(), a.getValue()));

            rows.addAll(sorted);

            setPreferredSize(new Dimension(
                0, Math.max(120, 56 + rows.size() * 28)));
            revalidate();
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
            g2.drawString("Pembelian per Distributor", 16, 22);

            if (rows.isEmpty()) {
                g2.setFont(new Font("Arial", Font.PLAIN, 13));
                g2.setColor(new Color(100, 116, 139));
                g2.drawString(
                    "Belum ada data pada periode ini.", 16, 50);
                g2.dispose();
                return;
            }

            double max = rows.get(0).getValue();
            int y = 40;

            for (Map.Entry<String, Double> row : rows) {
                g2.setFont(new Font("Arial", Font.PLAIN, 12));
                g2.setColor(new Color(51, 65, 85));

                String name = row.getKey();
                if (name.length() > 24) {
                    name = name.substring(0, 23) + "...";
                }
                g2.drawString(name, 16, y + 13);

                int barX = 180;
                int barW = Math.max(10, getWidth() - barX - 130);
                int width = max <= 0 ? 0 :
                    (int) Math.round(barW * row.getValue() / max);

                g2.setColor(new Color(37, 99, 235));
                g2.fillRoundRect(barX, y, width, 16, 6, 6);

                g2.setColor(new Color(30, 41, 59));
                g2.drawString(
                    money(row.getValue()), barX + barW + 8, y + 13);

                y += 28;
            }

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
