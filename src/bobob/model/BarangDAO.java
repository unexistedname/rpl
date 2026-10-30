package bobob.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BarangDAO {

    public List<Barang> findAll() throws SQLException {
        List<Barang> items = new ArrayList<>();

        String sql =
            "SELECT b.id,b.nama,b.stok,b.keterangan,k.nama AS kategori " +
            "FROM barang b LEFT JOIN kategori k ON b.kategori_id=k.id " +
            "ORDER BY b.nama";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                items.add(new Barang(
                    rs.getInt("id"),
                    rs.getString("nama"),
                    rs.getInt("stok"),
                    rs.getString("kategori"),
                    rs.getString("keterangan")
                ));
            }
        }

        return items;
    }

    public void insert(String nama, int kategoriId, int stok,
                       String keterangan) throws SQLException {

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO barang(nama,kategori_id,stok,keterangan) " +
                 "VALUES(?,?,?,?)")) {

            ps.setString(1, nama);
            ps.setInt(2, kategoriId);
            ps.setInt(3, stok);
            ps.setString(4, keterangan);
            ps.executeUpdate();
        }
    }

    public void updateStock(int id, int stok) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE barang SET stok=? WHERE id=?")) {

            ps.setInt(1, stok);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public static String findName(Connection c, int barangId)
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

    public static void adjustStock(Connection c, int barangId, int delta)
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
                        "Stok barang tidak mencukupi."
                    );
                }
            }
        }
    }
}
