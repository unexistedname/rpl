package bobob.model;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PengadaanDAO {

    public List<Pengadaan> findByPeriod(LocalDate start, LocalDate end) {
        try (Session session = HibernateUtil.open()) {
            List<Pengadaan> purchases = session.createQuery(
                "from Pengadaan where tanggal between :start and :end " +
                "order by tanggal, id", Pengadaan.class
            ).setParameter("start", start.toString())
             .setParameter("end", end.toString())
             .getResultList();

            fillNames(session, purchases);
            return purchases;
        }
    }

    public Pengadaan findById(int id) {
        try (Session session = HibernateUtil.open()) {
            Pengadaan purchase = session.get(Pengadaan.class, id);

            if (purchase != null) {
                fillNames(session, List.of(purchase));
            }

            return purchase;
        }
    }

    private void fillNames(Session session, List<Pengadaan> purchases) {
        Map<Integer, String> barangNames = new HashMap<>();
        Map<Integer, String> distributorNames = new HashMap<>();

        for (Barang item : session.createQuery(
            "from Barang", Barang.class
        ).getResultList()) {
            barangNames.put(item.id, item.nama);
        }

        for (Distributor distributor : session.createQuery(
            "from Distributor", Distributor.class
        ).getResultList()) {
            distributorNames.put(distributor.id, distributor.nama);
        }

        for (Pengadaan purchase : purchases) {
            purchase.namaBarang = barangNames.get(purchase.barangId);

            if (purchase.distributorId != null) {
                purchase.distributorNama =
                    distributorNames.get(purchase.distributorId);
            }
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
                "select count(p) from Pengadaan p where " + condition,
                Long.class
            ).setParameter("date", value).uniqueResult();

            return count != null && count > 0;
        }
    }

    public void save(Integer id, LocalDate date, int barangId,
                     int jumlah, double harga, Integer distributorId) {

        try (Session session = HibernateUtil.open()) {
            Transaction tx = session.beginTransaction();

            try {
                String namaBarang = BarangDAO.findName(session, barangId);
                double total = jumlah * harga;

                if (id == null) {
                    Pengadaan pengadaan = new Pengadaan(
                        date.toString(), barangId, jumlah, harga,
                        distributorId);

                    session.persist(pengadaan);
                    session.flush();

                    BarangDAO.adjustStock(session, barangId, jumlah);
                    CashflowDAO.insertPengadaan(
                        session, pengadaan.id, date.toString(),
                        namaBarang, total
                    );
                } else {
                    Pengadaan pengadaan =
                        session.get(Pengadaan.class, id);

                    if (pengadaan == null) {
                        throw new HibernateException(
                            "Pengadaan tidak ditemukan.");
                    }

                    int oldBarangId = pengadaan.barangId;
                    int oldJumlah = pengadaan.jumlah;

                    pengadaan.tanggal = date.toString();
                    pengadaan.barangId = barangId;
                    pengadaan.jumlah = jumlah;
                    pengadaan.harga = harga;
                    pengadaan.distributorId = distributorId;

                    if (oldBarangId == barangId) {
                        BarangDAO.adjustStock(
                            session, barangId, jumlah - oldJumlah);
                    } else {
                        BarangDAO.adjustStock(
                            session, oldBarangId, -oldJumlah);
                        BarangDAO.adjustStock(
                            session, barangId, jumlah);
                    }

                    CashflowDAO.updatePengadaan(
                        session, id, date.toString(), namaBarang, total
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
                Pengadaan pengadaan = session.get(Pengadaan.class, id);

                if (pengadaan == null) {
                    tx.commit();
                    return;
                }

                Integer barangId = pengadaan.barangId;
                int qty = pengadaan.jumlah;

                CashflowDAO.deletePengadaan(session, id);
                session.remove(pengadaan);

                if (barangId != null && barangId > 0) {
                    BarangDAO.adjustStock(session, barangId, -qty);
                }

                tx.commit();
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }
}
