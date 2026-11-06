package bobob.controller;

import bobob.model.BarangDAO;
import bobob.model.KategoriDAO;
import bobob.view.InventoryView;

import javax.swing.JPanel;
import org.hibernate.HibernateException;

public class InventoryController {

    private final InventoryView view;
    private final KategoriDAO kategoriDAO = new KategoriDAO();
    private final BarangDAO barangDAO = new BarangDAO();

    public InventoryController() {
        view = new InventoryView(this);
        refresh();
    }

    public JPanel getView() {
        return view;
    }

    public void refresh() {
        try {
            view.setCategories(kategoriDAO.findAll());
            view.setItems(barangDAO.findAll());
        } catch (HibernateException e) {
            view.showError(e.getMessage());
        }
    }

    public void addCategory(String nama) {
        try {
            kategoriDAO.insert(nama);
            refresh();
            view.selectCategory(nama);
        } catch (HibernateException e) {
            view.showInfo(
                "Kategori gagal ditambahkan. Pastikan nama kategori unik."
            );
        }
    }

    public void addItem(String nama, int kategoriId, int stok,
                        String keterangan) {
        try {
            barangDAO.insert(nama, kategoriId, stok, keterangan);
            refresh();
        } catch (HibernateException e) {
            view.showError(e.getMessage());
        }
    }

    public void adjustStock(int id, int stok) {
        try {
            barangDAO.updateStock(id, stok);
            refresh();
        } catch (HibernateException e) {
            view.showError(e.getMessage());
        }
    }
}
