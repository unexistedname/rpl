package bobob.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    public static final String URL = "jdbc:sqlite:transaksi.db";

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initSchema() throws SQLException {
        try (Connection c = getConnection();
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

            s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS transaksi (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "jenis TEXT NOT NULL, " +
                "tanggal TEXT NOT NULL, " +
                "nama_barang TEXT NOT NULL, " +
                "harga REAL NOT NULL, " +
                "jumlah INTEGER NOT NULL)"
            );

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

            addColumnIfMissing(s, "transaksi", "barang_id", "INTEGER");
            addColumnIfMissing(s, "cashflow", "transaksi_id", "INTEGER");
            addColumnIfMissing(s, "cashflow", "pengadaan_id", "INTEGER");

            // Penjualan lama yang belum tercatat di cashflow ikut dimasukkan
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

    private static void addColumnIfMissing(
        Statement s, String table, String column, String type
    ) throws SQLException {

        boolean exists = false;

        try (ResultSet rs = s.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equals(rs.getString("name"))) {
                    exists = true;
                }
            }
        }

        if (!exists) {
            s.executeUpdate(
                "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type
            );
        }
    }
}
