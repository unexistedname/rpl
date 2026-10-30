package bobob.model;

public class Barang {

    public final int id;
    public final String nama;
    public final int stok;
    public final String kategori;
    public final String keterangan;

    public Barang(int id, String nama, int stok,
                  String kategori, String keterangan) {
        this.id = id;
        this.nama = nama;
        this.stok = stok;
        this.kategori = kategori;
        this.keterangan = keterangan;
    }

    @Override
    public String toString() {
        return id + " - " + nama + " (Stok: " + stok + ")";
    }
}
