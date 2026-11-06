package bobob.model;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class DistributorDAO {

    public List<Distributor> findAll() {
        try (Session session = HibernateUtil.open()) {
            return session.createQuery(
                "from Distributor order by nama", Distributor.class
            ).getResultList();
        }
    }

    public Distributor findById(int id) {
        try (Session session = HibernateUtil.open()) {
            return session.get(Distributor.class, id);
        }
    }

    public void save(Integer id, String nama) {
        try (Session session = HibernateUtil.open()) {
            Transaction tx = session.beginTransaction();

            try {
                if (id == null) {
                    session.persist(new Distributor(nama));
                } else {
                    Distributor distributor =
                        session.get(Distributor.class, id);

                    if (distributor == null) {
                        throw new HibernateException(
                            "Distributor tidak ditemukan.");
                    }

                    distributor.nama = nama;
                }

                tx.commit();
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }

    public void delete(int id) {
        try (Session session = HibernateUtil.open()) {
            Transaction tx = session.beginTransaction();

            try {
                Long used = session.createQuery(
                    "select count(p) from Pengadaan p " +
                    "where p.distributorId = :id", Long.class
                ).setParameter("id", id).uniqueResult();

                if (used != null && used > 0) {
                    throw new HibernateException(
                        "Distributor masih dipakai pada transaksi pengadaan.");
                }

                Distributor distributor =
                    session.get(Distributor.class, id);

                if (distributor != null) {
                    session.remove(distributor);
                }

                tx.commit();
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }
}
