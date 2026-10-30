package bobob.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PengadaanDAO {

    public List<Pengadaan> findByPeriod(LocalDate start, LocalDate end)
                                        throws SQLException {
        List<Pengadaan> purchases = new ArrayList<>();

        String sql =
            "SELECT p.id,p.tanggal,p.barang_id,b.nama,p.jumlah,p.harga," +
            "p.distributor_id,d.nama AS distributor " +
            "FROM pengadaan p " +
            "LEFT JOIN barang b ON p.barang_id=b.id " +
            "LEFT JOIN distributor d ON p.distributor_id=d.id " +
            "WHERE p.tanggal BETWEEN ? AND ? " +
            "ORDER BY p.tanggal,p.id";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, start.toString());
            ps.setString(2, end.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer distributorId = rs.getInt("distributor_id");
                    if (rs.wasNull()) distributorId = null;

                    purchases.add(new Pengadaan(
                        rs.getInt("id"),
                        rs.getString("tanggal"),
                        rs.getInt("barang_id"),
                        rs.getString("nama"),
                        rs.getInt("jumlah"),
                        rs.getDouble("harga"),
                        distributorId,
                        rs.getString("distributor")
                    ));
                }
            }
        }

        return purchases;
    }

    public Pengadaan findById(int id) throws SQLException {
        String sql =
            "SELECT p.id,p.tanggal,p.barang_id,b.nama,p.jumlah,p.harga," +
            "p.distributor_id,d.nama AS distributor " +
            "FROM pengadaan p " +
            "LEFT JOIN barang b ON p.barang_id=b.id " +
            "LEFT JOIN distributor d ON p.distributor_id=d.id " +
            "WHERE p.id=?";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                Integer distributorId = rs.getInt("distributor_id");
                if (rs.wasNull()) distributorId = null;

                return new Pengadaan(
                    rs.getInt("id"),
                    rs.getString("tanggal"),
                    rs.getInt("barang_id"),
                    rs.getString("nama"),
                    rs.getInt("jumlah"),
                    rs.getDouble("harga"),
                    distributorId,
                    rs.getString("distributor")
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
                 "SELECT COUNT(*) FROM pengadaan WHERE " + condition)) {

            ps.setString(1, value);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void save(Integer id, LocalDate date, int barangId,
                     int jumlah, double harga, Integer distributorId)
                     throws SQLException {

        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);

            try {
                String namaBarang = BarangDAO.findName(c, barangId);
                double total = jumlah * harga;

                if (id == null) {
                    int pengadaanId;

                    try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO pengadaan" +
                        "(tanggal,barang_id,jumlah,harga,distributor_id) " +
                        "VALUES(?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {

                        ps.setString(1, date.toString());
                        ps.setInt(2, barangId);
                        ps.setInt(3, jumlah);
                        ps.setDouble(4, harga);

                        if (distributorId == null) {
                            ps.setNull(5, java.sql.Types.INTEGER);
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

                    BarangDAO.adjustStock(c, barangId, jumlah);
                    CashflowDAO.insertPengadaan(
                        c, pengadaanId, date.toString(),
                        namaBarang, total
                    );

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

                        ps.setString(1, date.toString());
                        ps.setInt(2, barangId);
                        ps.setInt(3, jumlah);
                        ps.setDouble(4, harga);

                        if (distributorId == null) {
                            ps.setNull(5, java.sql.Types.INTEGER);
                        } else {
                            ps.setInt(5, distributorId);
                        }

                        ps.setInt(6, id);
                        ps.executeUpdate();
                    }

                    if (oldBarangId == barangId) {
                        BarangDAO.adjustStock(
                            c, barangId, jumlah - oldJumlah);
                    } else {
                        BarangDAO.adjustStock(c, oldBarangId, -oldJumlah);
                        BarangDAO.adjustStock(c, barangId, jumlah);
                    }

                    CashflowDAO.updatePengadaan(
                        c, id, date.toString(), namaBarang, total
                    );
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

    public void delete(int id) throws SQLException {
        try (Connection c = Database.getConnection()) {
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

                CashflowDAO.deletePengadaan(c, id);

                try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM pengadaan WHERE id=?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

                if (barangId > 0) {
                    BarangDAO.adjustStock(c, barangId, -qty);
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
}
