package bobob.view;

import bobob.controller.InventoryController;
import bobob.model.Barang;
import bobob.model.Kategori;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

public class InventoryView extends JPanel {

    private final InventoryController controller;
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private final DefaultTableModel model;
    private final JTable table;

    private List<Barang> allItems = new ArrayList<>();
    private List<Kategori> categories = new ArrayList<>();

    public InventoryView(InventoryController controller) {
        this.controller = controller;

        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        model = new DefaultTableModel(
            new Object[]{"ID Barang", "Nama Barang", "Kategori",
                "Jumlah Stok", "Keterangan", "Atur Stok"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 5;
            }
        };

        table = new JTable(model);
        buildUI();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Inventory");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);

        categoryFilter.addActionListener(e -> applyFilter());

        JButton categoryButton = new JButton("+ Kategori");
        categoryButton.addActionListener(e -> addCategory());

        JButton addButton = new JButton("+ Tambah Barang");
        addButton.addActionListener(e -> addItem());

        actions.add(new JLabel("Kategori:"));
        actions.add(categoryFilter);
        actions.add(categoryButton);
        actions.add(addButton);

        header.add(title, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        table.setRowHeight(32);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getColumnModel().getColumn(5).setCellRenderer(
            (t, value, selected, focus, row, col) -> new JButton("Atur Stok")
        );

        table.getColumnModel().getColumn(5).setCellEditor(
            new ActionEditor(new JCheckBox(), row -> adjustStock(row))
        );

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void setCategories(List<Kategori> categories) {
        this.categories = categories;

        String selected = (String) categoryFilter.getSelectedItem();

        categoryFilter.removeAllItems();
        categoryFilter.addItem("Semua kategori");

        for (Kategori category : categories) {
            categoryFilter.addItem(category.nama);
        }

        if (selected != null) {
            categoryFilter.setSelectedItem(selected);
        }

        if (categoryFilter.getSelectedIndex() < 0) {
            categoryFilter.setSelectedIndex(0);
        }
    }

    public void setItems(List<Barang> items) {
        this.allItems = items;
        applyFilter();
    }

    public void selectCategory(String name) {
        categoryFilter.setSelectedItem(name);
    }

    private void applyFilter() {
        if (model == null) return;

        model.setRowCount(0);

        String selected = (String) categoryFilter.getSelectedItem();

        for (Barang item : allItems) {
            if (selected != null
                && !"Semua kategori".equals(selected)
                && !selected.equals(item.kategori)) {
                continue;
            }

            model.addRow(new Object[]{
                item.id,
                item.nama,
                item.kategori,
                item.stok,
                item.keterangan,
                "Atur Stok"
            });
        }
    }

    private void addCategory() {
        String nama = JOptionPane.showInputDialog(
            this, "Nama kategori baru:"
        );

        if (nama == null) return;
        nama = nama.trim();

        if (nama.isEmpty()) {
            showInfo("Nama kategori wajib diisi.");
            return;
        }

        controller.addCategory(nama);
    }

    private void addItem() {
        JComboBox<Kategori> categoryBox = new JComboBox<>();

        for (Kategori category : categories) {
            categoryBox.addItem(category);
        }

        JTextField nameField = new JTextField();
        JTextField stockField = new JTextField("0");
        JTextField noteField = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Nama Barang:"));
        form.add(nameField);
        form.add(new JLabel("Kategori:"));
        form.add(categoryBox);
        form.add(new JLabel("Stok Awal:"));
        form.add(stockField);
        form.add(new JLabel("Keterangan:"));
        form.add(noteField);

        if (categoryBox.getItemCount() == 0) {
            showInfo("Tambahkan kategori terlebih dahulu.");
            return;
        }

        int result = JOptionPane.showConfirmDialog(
            this, form, "Tambah Barang",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        Kategori category = (Kategori) categoryBox.getSelectedItem();

        try {
            int stock = Integer.parseInt(stockField.getText().trim());

            if (name.isEmpty() || stock < 0 || category == null) {
                throw new IllegalArgumentException();
            }

            controller.addItem(
                name, category.id, stock, noteField.getText().trim()
            );

        } catch (NumberFormatException e) {
            showInfo("Stok harus berupa bilangan bulat.");
        } catch (IllegalArgumentException e) {
            showInfo("Nama wajib diisi dan stok tidak boleh negatif.");
        }
    }

    private void adjustStock(int row) {
        int id = (int) model.getValueAt(row, 0);
        int currentStock = (int) model.getValueAt(row, 3);

        String value = JOptionPane.showInputDialog(
            this, "Masukkan jumlah stok baru:", currentStock
        );

        if (value == null) return;

        try {
            int newStock = Integer.parseInt(value.trim());

            if (newStock < 0) {
                showInfo("Stok tidak boleh negatif.");
                return;
            }

            controller.adjustStock(id, newStock);

        } catch (NumberFormatException e) {
            showInfo("Masukkan bilangan bulat yang valid.");
        }
    }

    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(
            this, "Terjadi kesalahan: " + message,
            "Error", JOptionPane.ERROR_MESSAGE
        );
    }
}
