package bobob.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "barang")
public class Barang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    @Column(nullable = false)
    public String nama;

    @Column(name = "kategori_id")
    public Integer kategoriId;

    @Column(nullable = false)
    public int stok;

    @Column(name = "keterangan")
    public String keterangan;

    @Transient
    public String kategori;

    protected Barang() {
    }

    public Barang(int id, String nama, int stok,
                  String kategori, String keterangan) {
        this.id = id;
        this.nama = nama;
        this.stok = stok;
        this.kategori = kategori;
        this.keterangan = keterangan;
    }

    public Barang(String nama, Integer kategoriId, int stok,
                  String keterangan) {
        this.nama = nama;
        this.kategoriId = kategoriId;
        this.stok = stok;
        this.keterangan = keterangan;
    }

    @Override
    public String toString() {
        return id + " - " + nama + " (Stok: " + stok + ")";
    }
}
