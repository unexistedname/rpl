package bobob.model;

public class Kategori {

    public final int id;
    public final String nama;

    public Kategori(int id, String nama) {
        this.id = id;
        this.nama = nama;
    }

    @Override
    public String toString() {
        return nama;
    }
}
