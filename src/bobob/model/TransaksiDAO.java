package bobob.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransaksiDAO {

    public List<Transaksi> findByDate(LocalDate date) throws SQLException {
        List<Transaksi> transactions = new ArrayList<>();

        String sql =
            "SELECT id,tanggal,barang_id,nama_barang,harga,jumlah " +
            "FROM transaksi WHERE jenis='PENJUALAN' AND tanggal=? " +
            "AND barang_id IS NOT NULL ORDER BY id";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, date.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transactions.add(new Transaksi(
                        rs.getInt("id"),
                        rs.getString("tanggal"),
                        rs.getInt("barang_id"),
                        rs.getString("nama_barang"),
                        rs.getDouble("harga"),
                        rs.getInt("jumlah")
                    ));
                }
            }
        }

        return transactions;
    }

    public Transaksi findById(int id) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT id,tanggal,barang_id,nama_barang,harga,jumlah " +
                 "FROM transaksi WHERE id=?")) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                return new Transaksi(
                    rs.getInt("id"),
                    rs.getString("tanggal"),
                    rs.getInt("barang_id"),
                    rs.getString("nama_barang"),
                    rs.getDouble("harga"),
                    rs.getInt("jumlah")
                );
            }
        }
    }

    public void save(Integer id, LocalDate date, int barangId,
                     double price, int qty) throws SQLException {

        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);

            try {
                String namaBarang = BarangDAO.findName(c, barangId);

                int oldBarangId = 0;
                int oldQty = 0;

                if (id != null) {
                    try (PreparedStatement ps = c.prepareStatement(
                        "SELECT barang_id,jumlah FROM transaksi WHERE id=?")) {
                        ps.setInt(1, id);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new SQLException(
                                    "Transaksi tidak ditemukan.");
                            }
                            oldBarangId = rs.getInt("barang_id");
                            oldQty = rs.getInt("jumlah");
                        }
                    }
                }

                if (oldBarangId > 0) {
                    BarangDAO.adjustStock(c, oldBarangId, oldQty);
                }

                BarangDAO.adjustStock(c, barangId, -qty);

                if (id == null) {
                    try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO transaksi" +
                        "(jenis,tanggal,nama_barang,harga,jumlah,barang_id) " +
                        "VALUES('PENJUALAN',?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {

                        ps.setString(1, date.toString());
                        ps.setString(2, namaBarang);
                        ps.setDouble(3, price);
                        ps.setInt(4, qty);
                        ps.setInt(5, barangId);
                        ps.executeUpdate();

                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new SQLException(
                                    "Gagal menyimpan transaksi.");
                            }
                            CashflowDAO.insertSale(
                                c, keys.getInt(1), date.toString(),
                                namaBarang, price * qty
                            );
                        }
                    }
                } else {
                    try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE transaksi SET nama_barang=?,harga=?," +
                        "jumlah=?,barang_id=? WHERE id=?")) {

                        ps.setString(1, namaBarang);
                        ps.setDouble(2, price);
                        ps.setInt(3, qty);
                        ps.setInt(4, barangId);
                        ps.setInt(5, id);
                        ps.executeUpdate();

                        CashflowDAO.updateSale(
                            c, id, date.toString(),
                            namaBarang, price * qty
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
    }

    public void delete(int id) throws SQLException {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);

            try {
                int barangId = 0;
                int qty = 0;

                try (PreparedStatement ps = c.prepareStatement(
                    "SELECT barang_id,jumlah FROM transaksi WHERE id=?")) {
                    ps.setInt(1, id);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                "Transaksi tidak ditemukan.");
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

                CashflowDAO.deleteSale(c, id);

                if (barangId > 0) {
                    BarangDAO.adjustStock(c, barangId, qty);
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
