package bobob.model;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BarangDAO {

    public List<Barang> findAll() {
        try (Session session = HibernateUtil.open()) {
            List<Barang> items = session.createQuery(
                "from Barang order by nama", Barang.class
            ).getResultList();

            Map<Integer, String> categories = new HashMap<>();

            for (Kategori category : session.createQuery(
                "from Kategori", Kategori.class
            ).getResultList()) {
                categories.put(category.id, category.nama);
            }

            for (Barang item : items) {
                if (item.kategoriId != null) {
                    item.kategori = categories.get(item.kategoriId);
                }
            }

            return items;
        }
    }

    public void insert(String nama, int kategoriId, int stok,
                       String keterangan) {
        try (Session session = HibernateUtil.open()) {
            Transaction tx = session.beginTransaction();

            try {
                session.persist(
                    new Barang(nama, kategoriId, stok, keterangan));
                tx.commit();
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }

    public void updateStock(int id, int stok) {
        try (Session session = HibernateUtil.open()) {
            Transaction tx = session.beginTransaction();

            try {
                Barang item = session.get(Barang.class, id);

                if (item == null) {
                    throw new HibernateException("Barang tidak ditemukan.");
                }

                item.stok = stok;
                tx.commit();
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }

    public static String findName(Session session, int barangId) {
        Barang item = session.get(Barang.class, barangId);

        if (item == null) {
            throw new HibernateException("Barang tidak ditemukan.");
        }

        return item.nama;
    }

    public static void adjustStock(Session session, int barangId, int delta) {
        if (delta == 0) return;

        Barang item = session.get(Barang.class, barangId);

        if (item == null) {
            throw new HibernateException("Barang tidak ditemukan.");
        }

        if (item.stok + delta < 0) {
            throw new HibernateException("Stok barang tidak mencukupi.");
        }

        item.stok += delta;
    }
}
