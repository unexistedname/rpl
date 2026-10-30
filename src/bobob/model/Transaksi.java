package bobob.model;

public class Transaksi {

    public final int id;
    public final String tanggal;
    public final int barangId;
    public final String namaBarang;
    public final double harga;
    public final int jumlah;

    public Transaksi(int id, String tanggal, int barangId,
                     String namaBarang, double harga, int jumlah) {
        this.id = id;
        this.tanggal = tanggal;
        this.barangId = barangId;
        this.namaBarang = namaBarang;
        this.harga = harga;
        this.jumlah = jumlah;
    }

    public double total() {
        return harga * jumlah;
    }
}
