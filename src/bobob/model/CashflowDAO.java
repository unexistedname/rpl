package bobob.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CashflowDAO {

    public List<Cashflow> findByPeriod(LocalDate start, LocalDate end)
                                      throws SQLException {
        List<Cashflow> entries = new ArrayList<>();

        String sql =
            "SELECT id,jenis,tanggal,nama,biaya,kategori FROM cashflow " +
            "WHERE tanggal BETWEEN ? AND ? ORDER BY tanggal,id";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, start.toString());
            ps.setString(2, end.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new Cashflow(
                        rs.getInt("id"),
                        rs.getString("jenis"),
                        rs.getString("tanggal"),
                        rs.getString("nama"),
                        rs.getDouble("biaya"),
                        rs.getString("kategori")
                    ));
                }
            }
        }

        return entries;
    }

    public Cashflow findById(int id) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT id,jenis,tanggal,nama,biaya,kategori " +
                 "FROM cashflow WHERE id=?")) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                return new Cashflow(
                    rs.getInt("id"),
                    rs.getString("jenis"),
                    rs.getString("tanggal"),
                    rs.getString("nama"),
                    rs.getDouble("biaya"),
                    rs.getString("kategori")
                );
            }
        }
    }

    public boolean hasBefore(LocalDate date) throws SQLException {
        return hasData("tanggal < ?", date.toString());
    }

    public boolean hasAfter(LocalDate date) throws SQLException {
        return hasData("tanggal > ?", date.toString());
    }

    private boolean hasData(String condition, String value)
                            throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT COUNT(*) FROM cashflow WHERE " + condition)) {

            ps.setString(1, value);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void save(Integer id, String jenis, LocalDate tanggal,
                     String nama, double biaya, String kategori)
                     throws SQLException {

        try (Connection c = Database.getConnection()) {
            if (id == null) {
                try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO cashflow" +
                    "(jenis,tanggal,nama,biaya,kategori) VALUES(?,?,?,?,?)")) {

                    ps.setString(1, jenis);
                    ps.setString(2, tanggal.toString());
                    ps.setString(3, nama);
                    ps.setDouble(4, biaya);
                    ps.setString(5, kategori);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE cashflow SET jenis=?,tanggal=?,nama=?," +
                    "biaya=?,kategori=? WHERE id=?")) {

                    ps.setString(1, jenis);
                    ps.setString(2, tanggal.toString());
                    ps.setString(3, nama);
                    ps.setDouble(4, biaya);
                    ps.setString(5, kategori);
                    ps.setInt(6, id);
                    ps.executeUpdate();
                }
            }
        }
    }

    public void deleteEntry(int id) throws SQLException {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);

            try {
                Integer transaksiId = null;
                Integer pengadaanId = null;

                try (PreparedStatement ps = c.prepareStatement(
                    "SELECT transaksi_id,pengadaan_id FROM cashflow " +
                    "WHERE id=?")) {

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
                        BarangDAO.adjustStock(c, barangId, qty);
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
                        BarangDAO.adjustStock(c, barangId, -qty);
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

    public static void insertSale(Connection c, int transaksiId,
                                  String tanggal, String nama,
                                  double total) throws SQLException {
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

    public static void updateSale(Connection c, int transaksiId,
                                  String tanggal, String nama,
                                  double total) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
            "UPDATE cashflow SET tanggal=?,nama=?,biaya=? " +
            "WHERE transaksi_id=?")) {

            ps.setString(1, tanggal);
            ps.setString(2, nama);
            ps.setDouble(3, total);
            ps.setInt(4, transaksiId);

            if (ps.executeUpdate() == 0) {
                insertSale(c, transaksiId, tanggal, nama, total);
            }
        }
    }

    public static void deleteSale(Connection c, int transaksiId)
                                  throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
            "DELETE FROM cashflow WHERE transaksi_id=?")) {
            ps.setInt(1, transaksiId);
            ps.executeUpdate();
        }
    }

    public static void insertPengadaan(Connection c, int pengadaanId,
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

    public static void updatePengadaan(Connection c, int pengadaanId,
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
                insertPengadaan(c, pengadaanId, tanggal, nama, total);
            }
        }
    }

    public static void deletePengadaan(Connection c, int pengadaanId)
                                       throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
            "DELETE FROM cashflow WHERE pengadaan_id=?")) {
            ps.setInt(1, pengadaanId);
            ps.executeUpdate();
        }
    }
}
