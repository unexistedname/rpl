package bobob.model;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDate;
import java.util.List;

public class CashflowDAO {

    public List<Cashflow> findByPeriod(LocalDate start, LocalDate end) {
        try (Session session = HibernateUtil.open()) {
            return session.createQuery(
                "from Cashflow where tanggal between :start and :end " +
                "order by tanggal, id", Cashflow.class
            ).setParameter("start", start.toString())
             .setParameter("end", end.toString())
             .getResultList();
        }
    }

    public Cashflow findById(int id) {
        try (Session session = HibernateUtil.open()) {
            return session.get(Cashflow.class, id);
        }
    }

    public boolean hasBefore(LocalDate date) {
        return hasData("tanggal < :date", date.toString());
    }

    public boolean hasAfter(LocalDate date) {
        return hasData("tanggal > :date", date.toString());
    }

    private boolean hasData(String condition, String value) {
        try (Session session = HibernateUtil.open()) {
            Long count = session.createQuery(
                "select count(c) from Cashflow c where " + condition,
                Long.class
            ).setParameter("date", value).uniqueResult();

            return count != null && count > 0;
        }
    }

    public void save(Integer id, String jenis, LocalDate tanggal,
                     String nama, double biaya, String kategori) {
        try (Session session = HibernateUtil.open()) {
            Transaction tx = session.beginTransaction();

            try {
                if (id == null) {
                    session.persist(new Cashflow(
                        jenis, tanggal.toString(), nama, biaya, kategori,
                        null, null));
                } else {
                    Cashflow entry = session.get(Cashflow.class, id);

                    if (entry == null) {
                        throw new HibernateException(
                            "Transaksi cashflow tidak ditemukan.");
                    }

                    entry.jenis = jenis;
                    entry.tanggal = tanggal.toString();
                    entry.nama = nama;
                    entry.biaya = biaya;
                    entry.kategori = kategori;
                }

                tx.commit();
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }

    public void deleteEntry(int id) {
        try (Session session = HibernateUtil.open()) {
            Transaction tx = session.beginTransaction();

            try {
                Cashflow entry = session.get(Cashflow.class, id);

                if (entry == null) {
                    tx.commit();
                    return;
                }

                if (entry.transaksiId != null) {
                    Transaksi transaksi =
                        session.get(Transaksi.class, entry.transaksiId);

                    Integer barangId = null;
                    int qty = 0;

                    if (transaksi != null) {
                        barangId = transaksi.barangId;
                        qty = transaksi.jumlah;
                    }

                    session.createMutationQuery(
                        "delete from Cashflow where transaksiId = :id"
                    ).setParameter("id", entry.transaksiId)
                     .executeUpdate();

                    if (transaksi != null) {
                        session.remove(transaksi);
                    }

                    if (barangId != null && barangId > 0) {
                        BarangDAO.adjustStock(session, barangId, qty);
                    }

                } else if (entry.pengadaanId != null) {
                    Pengadaan pengadaan =
                        session.get(Pengadaan.class, entry.pengadaanId);

                    Integer barangId = null;
                    int qty = 0;

                    if (pengadaan != null) {
                        barangId = pengadaan.barangId;
                        qty = pengadaan.jumlah;
                    }

                    session.createMutationQuery(
                        "delete from Cashflow where pengadaanId = :id"
                    ).setParameter("id", entry.pengadaanId)
                     .executeUpdate();

                    if (pengadaan != null) {
                        session.remove(pengadaan);
                    }

                    if (barangId != null && barangId > 0) {
                        BarangDAO.adjustStock(session, barangId, -qty);
                    }

                } else {
                    session.remove(entry);
                }

                tx.commit();
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }

    public static void insertSale(Session session, int transaksiId,
                                  String tanggal, String nama,
                                  double total) {
        session.persist(new Cashflow(
            Cashflow.PEMASUKAN, tanggal, nama, total, "penjualan",
            transaksiId, null));
    }

    public static void updateSale(Session session, int transaksiId,
                                  String tanggal, String nama,
                                  double total) {
        List<Cashflow> entries = session.createQuery(
            "from Cashflow where transaksiId = :id", Cashflow.class
        ).setParameter("id", transaksiId).getResultList();

        if (entries.isEmpty()) {
            insertSale(session, transaksiId, tanggal, nama, total);
            return;
        }

        Cashflow entry = entries.get(0);
        entry.jenis = Cashflow.PEMASUKAN;
        entry.tanggal = tanggal;
        entry.nama = nama;
        entry.biaya = total;
        entry.kategori = "penjualan";
    }

    public static void deleteSale(Session session, int transaksiId) {
        session.createMutationQuery(
            "delete from Cashflow where transaksiId = :id"
        ).setParameter("id", transaksiId).executeUpdate();
    }

    public static void insertPengadaan(Session session, int pengadaanId,
                                       String tanggal, String nama,
                                       double total) {
        session.persist(new Cashflow(
            Cashflow.PENGELUARAN, tanggal, nama, total, "pengadaan",
            null, pengadaanId));
    }

    public static void updatePengadaan(Session session, int pengadaanId,
                                       String tanggal, String nama,
                                       double total) {
        List<Cashflow> entries = session.createQuery(
            "from Cashflow where pengadaanId = :id", Cashflow.class
        ).setParameter("id", pengadaanId).getResultList();

        if (entries.isEmpty()) {
            insertPengadaan(session, pengadaanId, tanggal, nama, total);
            return;
        }

        Cashflow entry = entries.get(0);
        entry.jenis = Cashflow.PENGELUARAN;
        entry.tanggal = tanggal;
        entry.nama = nama;
        entry.biaya = total;
        entry.kategori = "pengadaan";
    }

    public static void deletePengadaan(Session session, int pengadaanId) {
        session.createMutationQuery(
            "delete from Cashflow where pengadaanId = :id"
        ).setParameter("id", pengadaanId).executeUpdate();
    }
}
