package bobob.controller;

import bobob.model.BarangDAO;
import bobob.model.Distributor;
import bobob.model.DistributorDAO;
import bobob.model.Pengadaan;
import bobob.model.PengadaanDAO;
import bobob.util.Period;
import bobob.view.DistributorView;

import javax.swing.JPanel;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class DistributorController {

    private final DistributorView view;
    private final PengadaanDAO pengadaanDAO = new PengadaanDAO();
    private final DistributorDAO distributorDAO = new DistributorDAO();
    private final BarangDAO barangDAO = new BarangDAO();

    private LocalDate anchor = LocalDate.now();
    private String mode = Period.HARIAN;

    public DistributorController() {
        view = new DistributorView(this);
        refresh();
    }

    public JPanel getView() {
        return view;
    }

    public void refresh() {
        LocalDate start = Period.start(anchor, mode);
        LocalDate end = Period.end(anchor, mode);

        view.setPeriodLabel(Period.label(anchor, mode));

        try {
            view.setPrevEnabled(pengadaanDAO.hasBefore(start));
            view.setNextEnabled(pengadaanDAO.hasAfter(end));
            view.setData(
                pengadaanDAO.findByPeriod(start, end),
                distributorDAO.findAll()
            );
        } catch (SQLException e) {
            view.showError(e.getMessage());
        }
    }

    public void previousPeriod() {
        anchor = Period.start(anchor, mode).minusDays(1);
        refresh();
    }

    public void nextPeriod() {
        anchor = Period.end(anchor, mode).plusDays(1);
        refresh();
    }

    public void setMode(String mode) {
        this.mode = mode;
        anchor = LocalDate.now();
        refresh();
    }

    public void addPurchase() {
        try {
            view.showPurchaseDialog(
                null,
                barangDAO.findAll(),
                distributorDAO.findAll()
            );
        } catch (SQLException e) {
            view.showError(e.getMessage());
        }
    }

    public void editPurchase(int id) {
        try {
            Pengadaan purchase = pengadaanDAO.findById(id);
            if (purchase == null) return;

            view.showPurchaseDialog(
                purchase,
                barangDAO.findAll(),
                distributorDAO.findAll()
            );
        } catch (SQLException e) {
            view.showError(e.getMessage());
        }
    }

    public void savePurchase(Integer id, LocalDate date, int barangId,
                             int jumlah, double harga, Integer distributorId) {
        try {
            pengadaanDAO.save(
                id, date, barangId, jumlah, harga, distributorId);
            anchor = date;
            refresh();
        } catch (SQLException e) {
            view.showError(e.getMessage());
        }
    }

    public void deletePurchase(int id) {
        if (!view.confirmDeletePurchase(id)) return;

        try {
            pengadaanDAO.delete(id);
            refresh();
        } catch (SQLException e) {
            view.showError(e.getMessage());
        }
    }

    public void addDistributor() {
        view.showDistributorDialog(null);
    }

    public void editDistributor(int id) {
        try {
            Distributor distributor = distributorDAO.findById(id);
            if (distributor == null) return;
            view.showDistributorDialog(distributor);
        } catch (SQLException e) {
            view.showError(e.getMessage());
        }
    }

    public void saveDistributor(Integer id, String nama) {
        try {
            distributorDAO.save(id, nama);
            refresh();
        } catch (SQLException e) {
            view.showInfo(
                "Gagal menyimpan distributor. " +
                "Pastikan nama distributor unik."
            );
        }
    }

    public void deleteDistributor(int id) {
        if (!view.confirmDeleteDistributor(id)) return;

        try {
            distributorDAO.delete(id);
            refresh();
        } catch (SQLException e) {
            view.showWarning(e.getMessage());
        }
    }
}
