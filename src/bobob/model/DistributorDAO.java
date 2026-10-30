package bobob.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DistributorDAO {

    public List<Distributor> findAll() throws SQLException {
        List<Distributor> distributors = new ArrayList<>();

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT id,nama FROM distributor ORDER BY nama");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                distributors.add(new Distributor(
                    rs.getInt("id"), rs.getString("nama")));
            }
        }

        return distributors;
    }

    public Distributor findById(int id) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT id,nama FROM distributor WHERE id=?")) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Distributor(
                    rs.getInt("id"), rs.getString("nama"));
            }
        }
    }

    public void save(Integer id, String nama) throws SQLException {
        try (Connection c = Database.getConnection()) {
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

    public void delete(int id) throws SQLException {
        try (Connection c = Database.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM pengadaan WHERE distributor_id=?")) {
                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new SQLException(
                            "Distributor masih dipakai pada transaksi " +
                            "pengadaan."
                        );
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
}
