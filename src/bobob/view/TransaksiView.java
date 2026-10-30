package bobob.view;

import bobob.controller.TransaksiController;
import bobob.model.Barang;
import bobob.model.Transaksi;
import bobob.util.Format;

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
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

public class TransaksiView extends JPanel {

    private final TransaksiController controller;
    private final JLabel tanggalLabel = new JLabel();
    private final DefaultTableModel model;
    private final JTable table;

    public TransaksiView(TransaksiController controller) {
        this.controller = controller;

        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        model = new DefaultTableModel(
            new Object[]{"No. Transaksi", "Tanggal", "ID Barang",
                "Nama Barang", "Harga", "Jumlah", "Total", "Edit", "Hapus"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 7 || col == 8;
            }
        };

        table = new JTable(model);
        buildUI();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Transaksi Penjualan");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        datePanel.setOpaque(false);

        JButton prev = new JButton("<");
        JButton next = new JButton(">");

        tanggalLabel.setFont(new Font("Arial", Font.BOLD, 14));

        prev.addActionListener(e -> controller.previousDay());
        next.addActionListener(e -> controller.nextDay());

        datePanel.add(prev);
        datePanel.add(tanggalLabel);
        datePanel.add(next);

        header.add(title, BorderLayout.WEST);
        header.add(datePanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        table.setRowHeight(30);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        table.getColumnModel().getColumn(7).setCellRenderer(
            (t, value, selected, focus, row, col) -> new JButton("Edit")
        );

        table.getColumnModel().getColumn(8).setCellRenderer(
            (t, value, selected, focus, row, col) -> new JButton("Hapus")
        );

        table.getColumnModel().getColumn(7).setCellEditor(
            new ActionEditor(new JCheckBox(), row -> editTransaction(row))
        );

        table.getColumnModel().getColumn(8).setCellEditor(
            new ActionEditor(new JCheckBox(), row -> deleteTransaction(row))
        );

        JButton addButton = new JButton("+ Tambah Penjualan");
        addButton.addActionListener(e -> controller.addTransaction());

        JPanel body = new JPanel(new BorderLayout(8, 8));
        body.setOpaque(false);
        body.add(addButton, BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);
    }

    public void setDate(String date) {
        tanggalLabel.setText(date);
    }

    public void setTransactions(List<Transaksi> transactions) {
        model.setRowCount(0);

        for (Transaksi t : transactions) {
            model.addRow(new Object[]{
                t.id,
                Format.date(t.tanggal),
                t.barangId,
                t.namaBarang,
                t.harga,
                t.jumlah,
                t.total(),
                "Edit",
                "Hapus"
            });
        }
    }

    public void showTransactionDialog(Transaksi existing, List<Barang> items) {
        int oldItemId = existing == null ? -1 : existing.barangId;

        JComboBox<Barang> itemBox = new JComboBox<>();
        JTextField priceField = new JTextField(
            existing == null ? "" : String.valueOf(existing.harga)
        );
        JTextField qtyField = new JTextField(
            existing == null ? "1" : String.valueOf(existing.jumlah)
        );

        for (Barang item : items) {
            // Barang stok nol tetap disediakan saat mengedit
            if (item.stok > 0 || item.id == oldItemId) {
                itemBox.addItem(item);
            }
        }

        if (existing != null) {
            for (int i = 0; i < itemBox.getItemCount(); i++) {
                if (itemBox.getItemAt(i).id == oldItemId) {
                    itemBox.setSelectedIndex(i);
                    break;
                }
            }
        }

        if (itemBox.getItemCount() == 0) {
            showInfo(
                "Belum ada barang dengan stok tersedia. " +
                "Tambahkan barang di Inventory terlebih dahulu."
            );
            return;
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Barang dari Inventory:"));
        form.add(itemBox);
        form.add(new JLabel("Harga Jual Satuan:"));
        form.add(priceField);
        form.add(new JLabel("Jumlah:"));
        form.add(qtyField);

        int result = JOptionPane.showConfirmDialog(
            this, form,
            existing == null ? "Tambah Penjualan" : "Edit Penjualan",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) return;

        Barang selected = (Barang) itemBox.getSelectedItem();
        if (selected == null) return;

        try {
            double price = Double.parseDouble(priceField.getText().trim());
            int qty = Integer.parseInt(qtyField.getText().trim());

            if (price < 0 || qty <= 0) {
                throw new IllegalArgumentException();
            }

            controller.saveTransaction(
                existing == null ? null : existing.id,
                selected.id, price, qty
            );

        } catch (NumberFormatException e) {
            showInfo("Harga dan jumlah harus berupa angka yang valid.");
        } catch (IllegalArgumentException e) {
            showInfo(
                "Harga tidak boleh negatif dan jumlah harus lebih dari nol."
            );
        }
    }

    public boolean confirmDelete(int id) {
        int confirm = JOptionPane.showConfirmDialog(
            this, "Hapus transaksi #" + id + "? Stok akan dikembalikan.",
            "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION
        );

        return confirm == JOptionPane.YES_OPTION;
    }

    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(
            this, message, "Kesalahan", JOptionPane.ERROR_MESSAGE
        );
    }


    private void editTransaction(int row) {
        controller.editTransaction((int) model.getValueAt(row, 0));
    }

    private void deleteTransaction(int row) {
        controller.deleteTransaction((int) model.getValueAt(row, 0));
    }
}
