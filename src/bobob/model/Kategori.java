package bobob.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "kategori")
public class Kategori {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    @Column(nullable = false, unique = true)
    public String nama;

    protected Kategori() {
    }

    public Kategori(int id, String nama) {
        this.id = id;
        this.nama = nama;
    }

    public Kategori(String nama) {
        this.nama = nama;
    }

    @Override
    public String toString() {
        return nama;
    }
}
