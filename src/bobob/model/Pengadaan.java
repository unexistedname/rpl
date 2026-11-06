package bobob.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "pengadaan")
public class Pengadaan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    @Column(nullable = false)
    public String tanggal;

    @Column(name = "barang_id", nullable = false)
    public int barangId;

    @Column(nullable = false)
    public int jumlah;

    @Column(nullable = false)
    public double harga;

    @Column(name = "distributor_id")
    public Integer distributorId;

    @Transient
    public String namaBarang;

    @Transient
    public String distributorNama;

    protected Pengadaan() {
    }

    public Pengadaan(int id, String tanggal, int barangId,
                     String namaBarang, int jumlah, double harga,
                     Integer distributorId, String distributorNama) {
        this.id = id;
        this.tanggal = tanggal;
        this.barangId = barangId;
        this.namaBarang = namaBarang;
        this.jumlah = jumlah;
        this.harga = harga;
        this.distributorId = distributorId;
        this.distributorNama = distributorNama;
    }

    public Pengadaan(String tanggal, int barangId, int jumlah,
                     double harga, Integer distributorId) {
        this.tanggal = tanggal;
        this.barangId = barangId;
        this.jumlah = jumlah;
        this.harga = harga;
        this.distributorId = distributorId;
    }

    public double total() {
        return harga * jumlah;
    }
}
