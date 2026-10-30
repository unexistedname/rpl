package bobob.controller;

import bobob.view.DashboardView;

import javax.swing.JPanel;

public class DashboardController {

    private DashboardView view;

    private TransaksiController transaksiController;
    private InventoryController inventoryController;
    private CashflowController cashflowController;
    private DistributorController distributorController;

    public void start() {
        view = new DashboardView(this);

        transaksiController = new TransaksiController();
        inventoryController = new InventoryController();
        cashflowController = new CashflowController();
        distributorController = new DistributorController();

        view.addPage("Hak Akses", new JPanel());
        view.addPage("Transaksi", transaksiController.getView());
        view.addPage("Inventory", inventoryController.getView());
        view.addPage("Cashflow", cashflowController.getView());
        view.addPage("Distributor", distributorController.getView());

        view.setVisible(true);
        showPage("Hak Akses");
    }

    public void showPage(String name) {
        view.showPage(name);

        if ("Transaksi".equals(name)) {
            transaksiController.refresh();
        } else if ("Inventory".equals(name)) {
            inventoryController.refresh();
        } else if ("Cashflow".equals(name)) {
            cashflowController.refresh();
        } else if ("Distributor".equals(name)) {
            distributorController.refresh();
        }
    }
}
