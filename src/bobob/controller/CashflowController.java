package bobob.controller;

import bobob.model.Cashflow;
import bobob.model.CashflowDAO;
import bobob.util.Period;
import bobob.view.CashflowView;

import javax.swing.JPanel;
import org.hibernate.HibernateException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CashflowController {

    private final CashflowView view;
    private final CashflowDAO cashflowDAO = new CashflowDAO();

    private LocalDate anchor = LocalDate.now();
    private String mode = Period.HARIAN;

    public CashflowController() {
        view = new CashflowView(this);
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
            view.setPrevEnabled(cashflowDAO.hasBefore(start));
            view.setNextEnabled(cashflowDAO.hasAfter(end));

            List<Cashflow> income = new ArrayList<>();
            List<Cashflow> expense = new ArrayList<>();
            double incomeTotal = 0;
            double expenseTotal = 0;

            for (Cashflow entry : cashflowDAO.findByPeriod(start, end)) {
                if (entry.isPemasukan()) {
                    income.add(entry);
                    incomeTotal += entry.biaya;
                } else {
                    expense.add(entry);
                    expenseTotal += entry.biaya;
                }
            }

            view.setData(income, expense, incomeTotal, expenseTotal);

        } catch (HibernateException e) {
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

    public void addTransaction() {
        view.showTransactionDialog(null);
    }

    public void editTransaction(int id) {
        try {
            Cashflow entry = cashflowDAO.findById(id);
            if (entry == null) return;
            view.showTransactionDialog(entry);
        } catch (HibernateException e) {
            view.showError(e.getMessage());
        }
    }

    public void saveTransaction(Integer id, String jenis, LocalDate tanggal,
                                String nama, double biaya, String kategori) {
        try {
            cashflowDAO.save(id, jenis, tanggal, nama, biaya, kategori);
            // shortcut: edit di sini hanya mengubah catatan cashflow,
            // transaksi penjualan di halaman Transaksi tidak ikut berubah
            anchor = tanggal;
            refresh();
        } catch (HibernateException e) {
            view.showError(e.getMessage());
        }
    }

    public void deleteTransaction(int id) {
        if (!view.confirmDelete(id)) return;

        try {
            cashflowDAO.deleteEntry(id);
            refresh();
        } catch (HibernateException e) {
            view.showError(e.getMessage());
        }
    }
}
