package bobob.model;

public class Distributor {

    public final int id;
    public final String nama;

    public Distributor(int id, String nama) {
        this.id = id;
        this.nama = nama;
    }

    @Override
    public String toString() {
        return nama;
    }
}
