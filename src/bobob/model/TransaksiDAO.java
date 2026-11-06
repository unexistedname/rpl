package bobob.model;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDate;
import java.util.List;

public class TransaksiDAO {

    public List<Transaksi> findByDate(LocalDate date) {
        try (Session session = HibernateUtil.open()) {
            return session.createQuery(
                "from Transaksi where jenis = 'PENJUALAN' " +
                "and tanggal = :date and barangId is not null order by id",
                Transaksi.class
            ).setParameter("date", date.toString()).getResultList();
        }
    }

    public Transaksi findById(int id) {
        try (Session session = HibernateUtil.open()) {
            return session.get(Transaksi.class, id);
        }
    }

    public void save(Integer id, LocalDate date, int barangId,
                     double price, int qty) {

        try (Session session = HibernateUtil.open()) {
            Transaction tx = session.beginTransaction();

            try {
                String namaBarang = BarangDAO.findName(session, barangId);

                Integer oldBarangId = null;
                int oldQty = 0;
                Transaksi old = null;

                if (id != null) {
                    old = session.get(Transaksi.class, id);

                    if (old == null) {
                        throw new HibernateException(
                            "Transaksi tidak ditemukan.");
                    }

                    oldBarangId = old.barangId;
                    oldQty = old.jumlah;
                }

                if (oldBarangId != null && oldBarangId > 0) {
                    BarangDAO.adjustStock(session, oldBarangId, oldQty);
                }

                BarangDAO.adjustStock(session, barangId, -qty);

                if (id == null) {
                    Transaksi transaksi = new Transaksi(
                        date.toString(), barangId, namaBarang, price, qty);

                    session.persist(transaksi);
                    session.flush();

                    CashflowDAO.insertSale(
                        session, transaksi.id, date.toString(),
                        namaBarang, price * qty
                    );
                } else {
                    old.namaBarang = namaBarang;
                    old.harga = price;
                    old.jumlah = qty;
                    old.barangId = barangId;

                    CashflowDAO.updateSale(
                        session, id, date.toString(),
                        namaBarang, price * qty
                    );
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
                Transaksi transaksi = session.get(Transaksi.class, id);

                if (transaksi == null) {
                    throw new HibernateException("Transaksi tidak ditemukan.");
                }

                Integer barangId = transaksi.barangId;
                int qty = transaksi.jumlah;

                session.remove(transaksi);
                CashflowDAO.deleteSale(session, id);

                if (barangId != null && barangId > 0) {
                    BarangDAO.adjustStock(session, barangId, qty);
                }

                tx.commit();
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }
}
