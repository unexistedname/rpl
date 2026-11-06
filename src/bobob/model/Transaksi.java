package bobob.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transaksi")
public class Transaksi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    @Column(nullable = false)
    public String jenis;

    @Column(nullable = false)
    public String tanggal;

    @Column(name = "barang_id")
    public Integer barangId;

    @Column(name = "nama_barang", nullable = false)
    public String namaBarang;

    @Column(nullable = false)
    public double harga;

    @Column(nullable = false)
    public int jumlah;

    protected Transaksi() {
    }

    public Transaksi(int id, String tanggal, Integer barangId,
                     String namaBarang, double harga, int jumlah) {
        this.id = id;
        this.jenis = "PENJUALAN";
        this.tanggal = tanggal;
        this.barangId = barangId;
        this.namaBarang = namaBarang;
        this.harga = harga;
        this.jumlah = jumlah;
    }

    public Transaksi(String tanggal, Integer barangId,
                     String namaBarang, double harga, int jumlah) {
        this(0, tanggal, barangId, namaBarang, harga, jumlah);
    }

    public double total() {
        return harga * jumlah;
    }
}
