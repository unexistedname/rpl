package bobob.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cashflow")
public class Cashflow {

    public static final String PEMASUKAN = "PEMASUKAN";
    public static final String PENGELUARAN = "PENGELUARAN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    @Column(nullable = false)
    public String jenis;

    @Column(nullable = false)
    public String tanggal;

    @Column(nullable = false)
    public String nama;

    @Column(nullable = false)
    public double biaya;

    @Column(nullable = false)
    public String kategori;

    @Column(name = "transaksi_id")
    public Integer transaksiId;

    @Column(name = "pengadaan_id")
    public Integer pengadaanId;

    protected Cashflow() {
    }

    public Cashflow(int id, String jenis, String tanggal,
                    String nama, double biaya, String kategori) {
        this.id = id;
        this.jenis = jenis;
        this.tanggal = tanggal;
        this.nama = nama;
        this.biaya = biaya;
        this.kategori = kategori;
    }

    public Cashflow(String jenis, String tanggal, String nama,
                    double biaya, String kategori,
                    Integer transaksiId, Integer pengadaanId) {
        this.jenis = jenis;
        this.tanggal = tanggal;
        this.nama = nama;
        this.biaya = biaya;
        this.kategori = kategori;
        this.transaksiId = transaksiId;
        this.pengadaanId = pengadaanId;
    }

    public boolean isPemasukan() {
        return PEMASUKAN.equals(jenis);
    }
}
