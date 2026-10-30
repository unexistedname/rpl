package bobob.model;

public class Cashflow {

    public static final String PEMASUKAN = "PEMASUKAN";
    public static final String PENGELUARAN = "PENGELUARAN";

    public final int id;
    public final String jenis;
    public final String tanggal;
    public final String nama;
    public final double biaya;
    public final String kategori;

    public Cashflow(int id, String jenis, String tanggal,
                    String nama, double biaya, String kategori) {
        this.id = id;
        this.jenis = jenis;
        this.tanggal = tanggal;
        this.nama = nama;
        this.biaya = biaya;
        this.kategori = kategori;
    }

    public boolean isPemasukan() {
        return PEMASUKAN.equals(jenis);
    }
}
