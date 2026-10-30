package bobob.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class KategoriDAO {

    public List<Kategori> findAll() throws SQLException {
        List<Kategori> categories = new ArrayList<>();

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT id,nama FROM kategori ORDER BY nama");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                categories.add(new Kategori(
                    rs.getInt("id"), rs.getString("nama")));
            }
        }

        return categories;
    }

    public void insert(String nama) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO kategori(nama) VALUES(?)")) {

            ps.setString(1, nama);
            ps.executeUpdate();
        }
    }
}
