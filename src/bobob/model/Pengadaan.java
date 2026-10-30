package bobob.model;

public class Pengadaan {

    public final int id;
    public final String tanggal;
    public final int barangId;
    public final String namaBarang;
    public final int jumlah;
    public final double harga;
    public final Integer distributorId;
    public final String distributorNama;

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

    public double total() {
        return harga * jumlah;
    }
}
