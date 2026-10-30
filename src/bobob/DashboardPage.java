package bobob;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardPage extends JFrame {

    private JPanel sidebar;
    private JPanel contentPanel;
    private JPanel pageHeader;
    private TransaksiPage transaksiPage;
    private InventoryPage inventoryPage;
    private CashflowPage cashflowPage;
    private DistributorPage distributorPage;

    private CardLayout cardLayout;
    private JButton toggleButton;

    private boolean collapsed = false;

    private final Color SIDEBAR_COLOR = new Color(30, 41, 59);
    private final Color ACTIVE_COLOR = new Color(37, 99, 235);
    private final Color CONTENT_COLOR = new Color(245, 247, 250);
    private final Color TEXT_COLOR = new Color(226, 232, 240);

    private final String[] menuNames = {
        "Hak Akses",
        "Transaksi",
        "Inventory",
        "Cashflow",
        "Distributor"
    };

    private final String[] menuIcons = {
        "HA", "TR", "IN", "CF", "DI"
    };

    private final Map<String, JButton> menuButtons
            = new LinkedHashMap<>();

    private String activeMenu = "Hak Akses";

    public DashboardPage() {
        setTitle("Dashboard");
        setSize(1100, 700);
        setMinimumSize(new Dimension(800, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        createSidebar();
        createContent();

        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        showPage("Hak Akses");
    }

    // ================= SIDEBAR =================
    private void createSidebar() {
        sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR_COLOR);
        sidebar.setPreferredSize(new Dimension(230, 0));

        // Brand / logo
        JLabel brand = new JLabel("  MY APP");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("Arial", Font.BOLD, 22));
        brand.setBorder(
                BorderFactory.createEmptyBorder(25, 10, 25, 10)
        );
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(brand);

        // Garis pemisah
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(71, 85, 105));
        sidebar.add(separator);

        sidebar.add(Box.createVerticalStrut(15));

        // Menu navigasi
        for (int i = 0; i < menuNames.length; i++) {
            final String menuName = menuNames[i];
            final String icon = menuIcons[i];

            JButton button = new JButton(
                    icon + "     " + menuName
            );

            button.setFont(new Font("Arial", Font.PLAIN, 14));
            button.setForeground(TEXT_COLOR);
            button.setBackground(SIDEBAR_COLOR);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setOpaque(true);
            button.setMaximumSize(
                    new Dimension(Integer.MAX_VALUE, 48)
            );
            button.setPreferredSize(new Dimension(230, 48));
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setBorder(
                    BorderFactory.createEmptyBorder(0, 18, 0, 8)
            );

            button.addActionListener(e -> showPage(menuName));

            menuButtons.put(menuName, button);
            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(5));
        }

        // Ruang kosong di bawah menu
        sidebar.add(Box.createVerticalGlue());
    }

    // ================= CONTENT =================
    private void createContent() {
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(CONTENT_COLOR);

        // Header bagian atas
        pageHeader = new JPanel(new BorderLayout());
        pageHeader.setBackground(Color.WHITE);
        pageHeader.setBorder(
                BorderFactory.createEmptyBorder(12, 20, 12, 20)
        );

        toggleButton = new JButton("\u2630");

        toggleButton.setFont(new Font("Arial", Font.PLAIN, 24));
        toggleButton.setForeground(new Color(51, 65, 85));
        toggleButton.setBackground(Color.WHITE);

        toggleButton.setFocusPainted(false);
        toggleButton.setBorderPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setOpaque(false);

        toggleButton.setPreferredSize(new Dimension(45, 40));
        toggleButton.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        toggleButton.addActionListener(e -> toggleSidebar());

        JLabel pageTitle = new JLabel("Dashboard");
        pageTitle.setFont(new Font("Arial", Font.BOLD, 20));
        pageTitle.setForeground(new Color(30, 41, 59));

        pageHeader.add(toggleButton, BorderLayout.WEST);
        pageHeader.add(pageTitle, BorderLayout.CENTER);

        contentPanel.add(pageHeader, BorderLayout.NORTH);

        // Area pergantian halaman
        cardLayout = new CardLayout();

        JPanel pages = new JPanel(cardLayout);
        pages.setBackground(CONTENT_COLOR);

        for (String menuName : menuNames) {
            if (menuName.equals("Transaksi")) {
                transaksiPage = new TransaksiPage();
                pages.add(transaksiPage, menuName);
            } else if (menuName.equals("Inventory")) {
                inventoryPage = new InventoryPage();
                pages.add(inventoryPage, menuName);
            } else if (menuName.equals("Cashflow")) {
                cashflowPage = new CashflowPage();
                pages.add(cashflowPage, menuName);
            } else if (menuName.equals("Distributor")) {
                distributorPage = new DistributorPage();
                pages.add(distributorPage, menuName);
            } else {
                JPanel blankPage = new JPanel();
                blankPage.setBackground(CONTENT_COLOR);
                pages.add(blankPage, menuName);
            }
        }

        contentPanel.add(pages, BorderLayout.CENTER);

        // Simpan panel halaman agar bisa diakses saat navigasi
        contentPanel.putClientProperty("pages", pages);
    }

    // ================= NAVIGATION =================
    private void showPage(String menuName) {
        activeMenu = menuName;

        JPanel pages = (JPanel) contentPanel.getClientProperty("pages");

        cardLayout.show(pages, menuName);

        // Muat ulang data agar perubahan dari halaman lain langsung terlihat
        if ("Transaksi".equals(menuName) && transaksiPage != null) {
            transaksiPage.refreshData();
        } else if ("Inventory".equals(menuName) && inventoryPage != null) {
            inventoryPage.refreshData();
        } else if ("Cashflow".equals(menuName) && cashflowPage != null) {
            cashflowPage.refreshData();
        } else if ("Distributor".equals(menuName) && distributorPage != null) {
            distributorPage.refreshData();
        }

        for (String name : menuNames) {
            JButton button = menuButtons.get(name);

            if (name.equals(menuName)) {
                button.setBackground(ACTIVE_COLOR);
                button.setForeground(Color.WHITE);
            } else {
                button.setBackground(SIDEBAR_COLOR);
                button.setForeground(TEXT_COLOR);
            }
        }
    }

    // ================= COLLAPSE SIDEBAR =================
    private void toggleSidebar() {
        collapsed = !collapsed;

        if (collapsed) {
            sidebar.setPreferredSize(new Dimension(75, 0));

            for (int i = 0; i < menuNames.length; i++) {
                JButton button = menuButtons.get(menuNames[i]);

                button.setText(menuIcons[i]);
                button.setHorizontalAlignment(SwingConstants.CENTER);
                button.setBorder(
                        BorderFactory.createEmptyBorder(0, 5, 0, 5)
                );
            }
        } else {
            sidebar.setPreferredSize(new Dimension(230, 0));

            for (int i = 0; i < menuNames.length; i++) {
                JButton button = menuButtons.get(menuNames[i]);

                button.setText(
                        menuIcons[i] + "     " + menuNames[i]
                );
                button.setHorizontalAlignment(SwingConstants.LEFT);
                button.setBorder(
                        BorderFactory.createEmptyBorder(0, 18, 0, 8)
                );
            }
        }

        sidebar.revalidate();
        sidebar.repaint();
        getContentPane().revalidate();
        getContentPane().repaint();
    }
}
