package bobob.model;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class KategoriDAO {

    public List<Kategori> findAll() {
        try (Session session = HibernateUtil.open()) {
            return session.createQuery(
                "from Kategori order by nama", Kategori.class
            ).getResultList();
        }
    }

    public void insert(String nama) {
        try (Session session = HibernateUtil.open()) {
            Transaction tx = session.beginTransaction();

            try {
                session.persist(new Kategori(nama));
                tx.commit();
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }
}
