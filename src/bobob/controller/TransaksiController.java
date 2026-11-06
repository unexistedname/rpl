package bobob.controller;

import bobob.model.Barang;
import bobob.model.BarangDAO;
import bobob.model.Transaksi;
import bobob.model.TransaksiDAO;
import bobob.util.Format;
import bobob.view.TransaksiView;

import javax.swing.JPanel;
import org.hibernate.HibernateException;
import java.time.LocalDate;
import java.util.List;

public class TransaksiController {

    private final TransaksiView view;
    private final TransaksiDAO transaksiDAO = new TransaksiDAO();
    private final BarangDAO barangDAO = new BarangDAO();

    private LocalDate tanggal = LocalDate.now();

    public TransaksiController() {
        view = new TransaksiView(this);
        refresh();
    }

    public JPanel getView() {
        return view;
    }

    public void refresh() {
        view.setDate(Format.date(tanggal));

        try {
            view.setTransactions(transaksiDAO.findByDate(tanggal));
        } catch (HibernateException e) {
            view.showError(e.getMessage());
        }
    }

    public void previousDay() {
        tanggal = tanggal.minusDays(1);
        refresh();
    }

    public void nextDay() {
        tanggal = tanggal.plusDays(1);
        refresh();
    }

    public void addTransaction() {
        try {
            List<Barang> items = barangDAO.findAll();
            view.showTransactionDialog(null, items);
        } catch (HibernateException e) {
            view.showError(e.getMessage());
        }
    }

    public void editTransaction(int id) {
        try {
            Transaksi transaction = transaksiDAO.findById(id);
            if (transaction == null) return;

            List<Barang> items = barangDAO.findAll();
            view.showTransactionDialog(transaction, items);
        } catch (HibernateException e) {
            view.showError(e.getMessage());
        }
    }

    public void saveTransaction(
        Integer id, int barangId, double price, int qty
    ) {
        try {
            transaksiDAO.save(id, tanggal, barangId, price, qty);
            refresh();
        } catch (HibernateException e) {
            view.showError(e.getMessage());
        }
    }

    public void deleteTransaction(int id) {
        if (!view.confirmDelete(id)) return;

        try {
            transaksiDAO.delete(id);
            refresh();
        } catch (HibernateException e) {
            view.showError(e.getMessage());
        }
    }
}
