package bobob.view;

import bobob.controller.DistributorController;
import bobob.model.Barang;
import bobob.model.Distributor;
import bobob.model.Pengadaan;
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
import javax.swing.JSplitPane;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DistributorView extends JPanel {

    private final DistributorController controller;

    private final JLabel periodLabel = new JLabel();
    private final JButton prevButton = new JButton("<");
    private final JButton nextButton = new JButton(">");

    private final DefaultTableModel purchaseModel = createPurchaseModel();
    private final DefaultTableModel distributorModel = createDistributorModel();
    private final DistributorChart chart = new DistributorChart();
    private final JLabel purchaseTotalLabel = new JLabel();
    private final List<Integer> purchaseIds = new ArrayList<>();

    public DistributorView(DistributorController controller) {
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

        JLabel title = new JLabel("Distributor");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JButton addButton = new JButton("+ Tambah Pengadaan");
        addButton.addActionListener(e -> controller.addPurchase());

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

        JScrollPane chartScroll = new JScrollPane(chart);
        chartScroll.setPreferredSize(new Dimension(0, 170));
        chartScroll.setBorder(null);
        chartScroll.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chartScroll.getViewport().setBackground(Color.WHITE);

        JTable purchaseTable = createPurchaseTable();
        JTable distributorTable = createDistributorTable();

        JPanel purchasePanel = tablePanel(
            "Pengadaan Barang", purchaseTotalLabel, purchaseTable);
        JPanel distributorPanel = distributorPanel(distributorTable);

        JSplitPane split = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT, purchasePanel, distributorPanel);
        split.setResizeWeight(0.72);
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(8);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(chartScroll, BorderLayout.NORTH);
        center.add(split, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private DefaultTableModel createPurchaseModel() {
        return new DefaultTableModel(
            new Object[]{"ID Barang", "Nama Barang", "Jumlah", "Harga",
                "Distributor", "Total", "Edit", "Hapus"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 6 || col == 7;
            }
        };
    }

    private DefaultTableModel createDistributorModel() {
        return new DefaultTableModel(
            new Object[]{"ID", "Nama Distributor", "Edit", "Hapus"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 2 || col == 3;
            }
        };
    }

    private JTable createPurchaseTable() {
        JTable table = new JTable(purchaseModel);
        table.setRowHeight(30);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(140);
        table.getColumnModel().getColumn(5).setPreferredWidth(110);
        table.getColumnModel().getColumn(6).setPreferredWidth(70);
        table.getColumnModel().getColumn(7).setPreferredWidth(70);

        table.getColumnModel().getColumn(6).setCellRenderer(
            (t, value, selected, focus, row, col) -> new JButton("Edit")
        );

        table.getColumnModel().getColumn(7).setCellRenderer(
            (t, value, selected, focus, row, col) -> new JButton("Hapus")
        );

        table.getColumnModel().getColumn(6).setCellEditor(
            new ActionEditor(new JCheckBox(), row ->
                controller.editPurchase(purchaseIds.get(row)))
        );

        table.getColumnModel().getColumn(7).setCellEditor(
            new ActionEditor(new JCheckBox(), row ->
                controller.deletePurchase(purchaseIds.get(row)))
        );

        return table;
    }

    private JTable createDistributorTable() {
        JTable table = new JTable(distributorModel);
        table.setRowHeight(28);

        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);

        table.getColumnModel().getColumn(2).setCellRenderer(
            (t, value, selected, focus, row, col) -> new JButton("Edit")
        );

        table.getColumnModel().getColumn(3).setCellRenderer(
            (t, value, selected, focus, row, col) -> new JButton("Hapus")
        );

        table.getColumnModel().getColumn(2).setCellEditor(
            new ActionEditor(new JCheckBox(), row ->
                controller.editDistributor(
                    (int) distributorModel.getValueAt(row, 0)))
        );

        table.getColumnModel().getColumn(3).setCellEditor(
            new ActionEditor(new JCheckBox(), row ->
                controller.deleteDistributor(
                    (int) distributorModel.getValueAt(row, 0)))
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

    private JPanel distributorPanel(JTable table) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel label = new JLabel("Daftar Distributor");
        label.setFont(new Font("Arial", Font.BOLD, 16));

        JButton addButton = new JButton("+ Distributor");
        addButton.addActionListener(e -> controller.addDistributor());

        header.add(label, BorderLayout.WEST);
        header.add(addButton, BorderLayout.EAST);

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

    public void setData(List<Pengadaan> purchases,
                        List<Distributor> distributors) {
        purchaseModel.setRowCount(0);
        purchaseIds.clear();
        distributorModel.setRowCount(0);

        double total = 0;
        Map<String, Double> byDistributor = new LinkedHashMap<>();

        for (Pengadaan purchase : purchases) {
            String distributor = purchase.distributorNama;
            if (distributor == null) {
                distributor = "Tanpa distributor";
            }

            purchaseModel.addRow(new Object[]{
                purchase.barangId,
                purchase.namaBarang,
                purchase.jumlah,
                Format.money(purchase.harga),
                distributor,
                Format.money(purchase.total()),
                "Edit",
                "Hapus"
            });

            purchaseIds.add(purchase.id);
            total += purchase.total();

            Double current = byDistributor.get(distributor);
            byDistributor.put(distributor,
                current == null ? purchase.total() : current + purchase.total());
        }

        for (Distributor distributor : distributors) {
            distributorModel.addRow(new Object[]{
                distributor.id,
                distributor.nama,
                "Edit",
                "Hapus"
            });
        }

        purchaseTotalLabel.setText("Total: " + Format.money(total));
        chart.setData(byDistributor);
    }

    public void showPurchaseDialog(Pengadaan existing, List<Barang> items,
                                   List<Distributor> distributors) {
        if (items.isEmpty()) {
            showInfo("Tambahkan barang di Inventory terlebih dahulu.");
            return;
        }

        if (distributors.isEmpty()) {
            showInfo("Tambahkan distributor terlebih dahulu.");
            return;
        }

        JComboBox<Barang> itemBox = new JComboBox<>();
        for (Barang item : items) {
            itemBox.addItem(item);
        }

        JComboBox<Distributor> distributorBox = new JComboBox<>();
        for (Distributor distributor : distributors) {
            distributorBox.addItem(distributor);
        }

        if (existing != null) {
            for (int i = 0; i < itemBox.getItemCount(); i++) {
                if (itemBox.getItemAt(i).id == existing.barangId) {
                    itemBox.setSelectedIndex(i);
                    break;
                }
            }

            if (existing.distributorId != null) {
                for (int i = 0; i < distributorBox.getItemCount(); i++) {
                    if (distributorBox.getItemAt(i).id
                        == existing.distributorId) {
                        distributorBox.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }

        JTextField dateField = new JTextField(
            existing == null ? LocalDate.now().toString() : existing.tanggal
        );
        JTextField qtyField = new JTextField(
            existing == null ? "1" : String.valueOf(existing.jumlah)
        );
        JTextField priceField = new JTextField(
            existing == null ? "" : String.valueOf(existing.harga)
        );

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Barang dari Inventory:"));
        form.add(itemBox);
        form.add(new JLabel("Distributor:"));
        form.add(distributorBox);
        form.add(new JLabel("Tanggal (yyyy-mm-dd):"));
        form.add(dateField);
        form.add(new JLabel("Jumlah Barang:"));
        form.add(qtyField);
        form.add(new JLabel("Harga Satuan:"));
        form.add(priceField);

        int result = JOptionPane.showConfirmDialog(
            this, form,
            existing == null ? "Tambah Pengadaan" : "Edit Pengadaan",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) return;

        Barang item = (Barang) itemBox.getSelectedItem();
        Distributor distributor =
            (Distributor) distributorBox.getSelectedItem();

        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            int qty = Integer.parseInt(qtyField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());

            if (item == null || distributor == null ||
                qty <= 0 || price < 0) {
                throw new IllegalArgumentException();
            }

            controller.savePurchase(
                existing == null ? null : existing.id,
                date, item.id, qty, price, distributor.id
            );

        } catch (java.time.format.DateTimeParseException e) {
            showInfo("Format tanggal harus yyyy-mm-dd.");
        } catch (NumberFormatException e) {
            showInfo("Jumlah dan harga harus berupa angka yang valid.");
        } catch (IllegalArgumentException e) {
            showInfo(
                "Jumlah harus lebih dari nol dan harga tidak " +
                "boleh negatif."
            );
        }
    }

    public void showDistributorDialog(Distributor existing) {
        String value = JOptionPane.showInputDialog(
            this, "Nama distributor:", existing == null ? "" : existing.nama
        );

        if (value == null) return;
        value = value.trim();

        if (value.isEmpty()) {
            showInfo("Nama distributor wajib diisi.");
            return;
        }

        controller.saveDistributor(
            existing == null ? null : existing.id, value);
    }

    public boolean confirmDeletePurchase(int id) {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Hapus pengadaan #" + id + "? Stok barang akan dikurangi " +
            "dan pengeluaran cashflow ikut terhapus.",
            "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION
        );

        return confirm == JOptionPane.YES_OPTION;
    }

    public boolean confirmDeleteDistributor(int id) {
        int confirm = JOptionPane.showConfirmDialog(
            this, "Hapus distributor #" + id + "?",
            "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION
        );

        return confirm == JOptionPane.YES_OPTION;
    }

    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public void showWarning(String message) {
        JOptionPane.showMessageDialog(
            this, message, "Tidak Bisa Menghapus",
            JOptionPane.WARNING_MESSAGE
        );
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(
            this, "Terjadi kesalahan: " + message,
            "Error", JOptionPane.ERROR_MESSAGE
        );
    }

    private static class DistributorChart extends JPanel {
        private final List<Map.Entry<String, Double>> rows =
            new ArrayList<>();

        DistributorChart() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
            ));
            setPreferredSize(new Dimension(0, 120));
        }

        void setData(Map<String, Double> data) {
            rows.clear();

            List<Map.Entry<String, Double>> sorted =
                new ArrayList<>(data.entrySet());
            sorted.sort((a, b) ->
                Double.compare(b.getValue(), a.getValue()));

            rows.addAll(sorted);

            setPreferredSize(new Dimension(
                0, Math.max(120, 56 + rows.size() * 28)));
            revalidate();
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
            g2.drawString("Pembelian per Distributor", 16, 22);

            if (rows.isEmpty()) {
                g2.setFont(new Font("Arial", Font.PLAIN, 13));
                g2.setColor(new Color(100, 116, 139));
                g2.drawString(
                    "Belum ada data pada periode ini.", 16, 50);
                g2.dispose();
                return;
            }

            double max = rows.get(0).getValue();
            int y = 40;

            for (Map.Entry<String, Double> row : rows) {
                g2.setFont(new Font("Arial", Font.PLAIN, 12));
                g2.setColor(new Color(51, 65, 85));

                String name = row.getKey();
                if (name.length() > 24) {
                    name = name.substring(0, 23) + "...";
                }
                g2.drawString(name, 16, y + 13);

                int barX = 180;
                int barW = Math.max(10, getWidth() - barX - 130);
                int width = max <= 0 ? 0 :
                    (int) Math.round(barW * row.getValue() / max);

                g2.setColor(new Color(37, 99, 235));
                g2.fillRoundRect(barX, y, width, 16, 6, 6);

                g2.setColor(new Color(30, 41, 59));
                g2.drawString(
                    Format.money(row.getValue()), barX + barW + 8, y + 13);

                y += 28;
            }

            g2.dispose();
        }
    }
}
