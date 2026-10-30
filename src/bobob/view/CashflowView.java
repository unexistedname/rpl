package bobob.view;

import bobob.controller.CashflowController;
import bobob.model.Cashflow;
import bobob.util.Format;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.util.List;

public class CashflowView extends JPanel {

    private static final String[] CATEGORIES = {
        "penjualan", "modal", "gaji", "operasional", "pembelian", "lainnya"
    };

    private final CashflowController controller;

    private final JLabel periodLabel = new JLabel();
    private final JButton prevButton = new JButton("<");
    private final JButton nextButton = new JButton(">");

    private final DefaultTableModel incomeModel = createModel();
    private final DefaultTableModel expenseModel = createModel();
    private final RatioPanel ratioPanel = new RatioPanel();
    private final JLabel incomeTotalLabel = new JLabel();
    private final JLabel expenseTotalLabel = new JLabel();

    public CashflowView(CashflowController controller) {
        this.controller = controller;

        setLayout(new BorderLayout(0, 12));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        buildUI();
    }

    private void buildUI() {
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel title = new JLabel("Cashflow");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JButton addButton = new JButton("+ Tambah Transaksi");
        addButton.addActionListener(e -> controller.addTransaction());

        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(addButton, BorderLayout.EAST);

        JPanel periodBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        periodBar.setOpaque(false);

        JToggleButton daily = new JToggleButton("Harian");
        JToggleButton weekly = new JToggleButton("Mingguan");
        JToggleButton monthly = new JToggleButton("Bulanan");

        ButtonGroup group = new ButtonGroup();
        group.add(daily);
        group.add(weekly);
        group.add(monthly);
        daily.setSelected(true);

        daily.addActionListener(e -> controller.setMode("Harian"));
        weekly.addActionListener(e -> controller.setMode("Mingguan"));
        monthly.addActionListener(e -> controller.setMode("Bulanan"));

        periodLabel.setFont(new Font("Arial", Font.BOLD, 14));

        prevButton.addActionListener(e -> controller.previousPeriod());
        nextButton.addActionListener(e -> controller.nextPeriod());

        periodBar.add(daily);
        periodBar.add(weekly);
        periodBar.add(monthly);
        periodBar.add(Box.createHorizontalStrut(12));
        periodBar.add(prevButton);
        periodBar.add(periodLabel);
        periodBar.add(nextButton);

        top.add(titleRow, BorderLayout.NORTH);
        top.add(periodBar, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel tables = new JPanel(new GridLayout(2, 1, 0, 12));
        tables.setOpaque(false);
        tables.add(tablePanel(
            "Pemasukan", incomeTotalLabel, createTable(incomeModel)));
        tables.add(tablePanel(
            "Pengeluaran", expenseTotalLabel, createTable(expenseModel)));

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(ratioPanel, BorderLayout.NORTH);
        center.add(tables, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private DefaultTableModel createModel() {
        return new DefaultTableModel(
            new Object[]{"No. Transaksi", "Tanggal", "Nama Barang",
                "Biaya", "Kategori", "Edit", "Hapus"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 5 || col == 6;
            }
        };
    }

    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(220);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);
        table.getColumnModel().getColumn(5).setPreferredWidth(70);
        table.getColumnModel().getColumn(6).setPreferredWidth(70);

        table.getColumnModel().getColumn(5).setCellRenderer(
            (t, value, selected, focus, row, col) -> new JButton("Edit")
        );

        table.getColumnModel().getColumn(6).setCellRenderer(
            (t, value, selected, focus, row, col) -> new JButton("Hapus")
        );

        table.getColumnModel().getColumn(5).setCellEditor(
            new ActionEditor(new JCheckBox(), row -> controller.editTransaction(
                (int) model.getValueAt(row, 0)))
        );

        table.getColumnModel().getColumn(6).setCellEditor(
            new ActionEditor(new JCheckBox(), row ->
                controller.deleteTransaction(
                    (int) model.getValueAt(row, 0)))
        );

        return table;
    }

    private JPanel tablePanel(String title, JLabel totalLabel, JTable table) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel label = new JLabel(title);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        totalLabel.setFont(new Font("Arial", Font.BOLD, 14));

        header.add(label, BorderLayout.WEST);
        header.add(totalLabel, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    public void setPeriodLabel(String label) {
        periodLabel.setText(label);
    }

    public void setPrevEnabled(boolean enabled) {
        prevButton.setEnabled(enabled);
    }

    public void setNextEnabled(boolean enabled) {
        nextButton.setEnabled(enabled);
    }

    public void setData(List<Cashflow> income, List<Cashflow> expense,
                        double incomeTotal, double expenseTotal) {
        incomeModel.setRowCount(0);
        expenseModel.setRowCount(0);

        for (Cashflow entry : income) {
            incomeModel.addRow(row(entry));
        }

        for (Cashflow entry : expense) {
            expenseModel.addRow(row(entry));
        }

        incomeTotalLabel.setText("Total: " + Format.money(incomeTotal));
        expenseTotalLabel.setText("Total: " + Format.money(expenseTotal));
        ratioPanel.setData(incomeTotal, expenseTotal);
    }

    private Object[] row(Cashflow entry) {
        return new Object[]{
            entry.id,
            Format.date(entry.tanggal),
            entry.nama,
            Format.money(entry.biaya),
            entry.kategori,
            "Edit",
            "Hapus"
        };
    }

    public void showTransactionDialog(Cashflow existing) {
        String jenis = existing == null ? "PEMASUKAN" : existing.jenis;
        String tanggal = existing == null
            ? LocalDate.now().toString()
            : existing.tanggal;
        String nama = existing == null ? "" : existing.nama;
        String biaya = existing == null ? "" : String.valueOf(existing.biaya);
        String kategori = existing == null ? "lainnya" : existing.kategori;

        JComboBox<String> typeBox = new JComboBox<>(
            new String[]{"Pemasukan", "Pengeluaran"});
        typeBox.setSelectedItem(
            "PEMASUKAN".equals(jenis) ? "Pemasukan" : "Pengeluaran");

        JTextField dateField = new JTextField(tanggal);
        JTextField nameField = new JTextField(nama);
        JTextField amountField = new JTextField(biaya);

        JComboBox<String> categoryBox = new JComboBox<>(CATEGORIES);
        categoryBox.setEditable(true);
        categoryBox.setSelectedItem(kategori);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Jenis:"));
        form.add(typeBox);
        form.add(new JLabel("Tanggal (yyyy-mm-dd):"));
        form.add(dateField);
        form.add(new JLabel("Nama / Barang:"));
        form.add(nameField);
        form.add(new JLabel("Biaya Total:"));
        form.add(amountField);
        form.add(new JLabel("Kategori:"));
        form.add(categoryBox);

        int result = JOptionPane.showConfirmDialog(
            this, form,
            existing == null ? "Tambah Transaksi" : "Edit Transaksi",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) return;

        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            String name = nameField.getText().trim();
            double amount = Double.parseDouble(amountField.getText().trim());

            Object selected = categoryBox.getEditor().getItem();
            String category =
                selected == null ? "" : selected.toString().trim();

            if (name.isEmpty() || category.isEmpty() || amount < 0) {
                throw new IllegalArgumentException();
            }

            String newJenis = "Pemasukan".equals(typeBox.getSelectedItem())
                ? "PEMASUKAN" : "PENGELUARAN";

            controller.saveTransaction(
                existing == null ? null : existing.id,
                newJenis, date, name, amount, category
            );

        } catch (java.time.format.DateTimeParseException e) {
            showInfo("Format tanggal harus yyyy-mm-dd.");
        } catch (NumberFormatException e) {
            showInfo("Biaya harus berupa angka yang valid.");
        } catch (IllegalArgumentException e) {
            showInfo(
                "Nama dan kategori wajib diisi, biaya tidak boleh negatif."
            );
        }
    }

    public boolean confirmDelete(int id) {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Hapus transaksi #" + id + "? Jika ini penjualan, " +
            "riwayat transaksi ikut terhapus dan stok barang dikembalikan.",
            "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION
        );

        return confirm == JOptionPane.YES_OPTION;
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

    private static class RatioPanel extends JPanel {
        private double income;
        private double expense;

        RatioPanel() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
            ));
            setPreferredSize(new Dimension(0, 130));
        }

        void setData(double income, double expense) {
            this.income = income;
            this.expense = expense;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(new Color(30, 41, 59));
            g2.drawString("Rasio Pemasukan vs Pengeluaran", 16, 22);

            double total = income + expense;

            if (total <= 0) {
                g2.setFont(new Font("Arial", Font.PLAIN, 13));
                g2.setColor(new Color(100, 116, 139));
                g2.drawString("Belum ada data pada periode ini.", 16, 50);
                g2.dispose();
                return;
            }

            int x = 16;
            int y = 38;
            int width = getWidth() - 32;
            int height = 24;
            int incomeWidth =
                (int) Math.round(width * income / total);

            g2.setColor(new Color(34, 197, 94));
            g2.fillRoundRect(x, y, incomeWidth, height, 8, 8);

            g2.setColor(new Color(239, 68, 68));
            g2.fillRoundRect(x + incomeWidth, y,
                width - incomeWidth, height, 8, 8);

            g2.setFont(new Font("Arial", Font.PLAIN, 12));

            g2.setColor(new Color(34, 197, 94));
            g2.drawString("Pemasukan: " + Format.money(income) +
                " (" + Math.round(income / total * 100) + "%)",
                x, y + height + 22);

            g2.setColor(new Color(239, 68, 68));
            g2.drawString("Pengeluaran: " + Format.money(expense) +
                " (" + Math.round(expense / total * 100) + "%)",
                x, y + height + 42);

            g2.dispose();
        }
    }
}
