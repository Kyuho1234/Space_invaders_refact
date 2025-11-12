package org.newdawn.spaceinvaders.ui;

import org.newdawn.spaceinvaders.items.GameItem;
import org.newdawn.spaceinvaders.items.ItemRegistry;
import org.newdawn.spaceinvaders.firebase.FirebaseManager;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.net.URL;
import java.awt.event.ActionEvent;

/**
 * 상점 UI를 표시하고 아이템을 그리드(격자) 형태로 진열하는 다이얼로그 클래스입니다.
 * 소모품 탭과 영구 업그레이드 탭으로 구성됩니다.
 */
public class StoreDialog extends JDialog {

    private static final String FONT_ARIAL = "Arial";
    private static final String ERROR_TEXT = "Error";

    private final transient FirebaseManager firebaseManager;
    private JLabel pointsLabel;

    public StoreDialog(JFrame parent) {
        super(parent, "Game Store", true);

        this.firebaseManager = FirebaseManager.getInstance();

        setSize(650, 550);
        setLocationRelativeTo(parent);
        setResizable(false);

        initializeStoreUI();
    }

    private void initializeStoreUI() {
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 헤더: 타이틀 + 포인트
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("=== GAME STORE ===");
        title.setFont(new Font(FONT_ARIAL, Font.BOLD, 24));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        int currentPoints = firebaseManager.isLoggedIn() ? firebaseManager.getUserPoints() : 0;
        pointsLabel = new JLabel("Your Points: " + currentPoints);
        pointsLabel.setFont(new Font(FONT_ARIAL, Font.BOLD, 16));
        pointsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(title);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        headerPanel.add(pointsLabel);

        // 탭 패널 생성
        JTabbedPane tabbedPane = new JTabbedPane();

        // 탭 1: 소모품 (기존 아이템)
        JPanel itemsTab = createItemsTab();
        tabbedPane.addTab("🛒 Consumables", itemsTab);

        // 탭 2: 영구 업그레이드
        JPanel upgradesTab = createUpgradesTab();
        tabbedPane.addTab("⭐ Permanent Upgrades", upgradesTab);

        // 하단 안내
        JLabel footer = new JLabel("Hint: Press Ctrl+Enter for +500 test points");
        footer.setFont(new Font(FONT_ARIAL, Font.ITALIC, 11));
        footer.setHorizontalAlignment(SwingConstants.CENTER);

        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(tabbedPane, BorderLayout.CENTER);
        contentPanel.add(footer, BorderLayout.SOUTH);

        add(contentPanel);

        // Ctrl+Enter: 테스트용 포인트 +500 지급
        contentPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ctrl ENTER"), "addTestPoints");
        contentPanel.getActionMap().put("addTestPoints", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (firebaseManager.isLoggedIn()) {
                    int current = firebaseManager.getUserPoints();
                    int newPoints = current + 500;
                    boolean ok = firebaseManager.updateUserPoints(newPoints);
                    if (ok) {
                        JOptionPane.showMessageDialog(StoreDialog.this, "+500 points added! (Test Mode)");
                        refreshPoints();
                    } else {
                        JOptionPane.showMessageDialog(StoreDialog.this, "Failed to add points.", ERROR_TEXT, JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(StoreDialog.this, "You must be logged in to use this cheat.", "Not logged in", JOptionPane.WARNING_MESSAGE);
                }
            }
        });
    }

    /**
     * 소모품 탭 생성 (기존 아이템 상점)
     */
    private JPanel createItemsTab() {
        JPanel tabPanel = new JPanel(new BorderLayout());

        JPanel itemsPanel = new JPanel(new GridLayout(0, 3, 10, 10));
        Map<String, GameItem> items = ItemRegistry.getAllItems();

        if (items.isEmpty()) {
            itemsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            itemsPanel.add(new JLabel("No items available in the store yet!"));
        } else {
            for (GameItem item : items.values()) {
                itemsPanel.add(createItemPanel(item));
            }
        }

        JScrollPane scrollPane = new JScrollPane(itemsPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tabPanel.add(scrollPane, BorderLayout.CENTER);

        return tabPanel;
    }

    /**
     * 영구 업그레이드 탭 생성
     */
    private JPanel createUpgradesTab() {
        JPanel tabPanel = new JPanel(new BorderLayout(10, 10));
        tabPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel infoLabel = new JLabel("<html><center>Permanent upgrades enhance your stats for all future games!<br>Effects apply immediately after purchase.</center></html>");
        infoLabel.setFont(new Font(FONT_ARIAL, Font.ITALIC, 12));
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel upgradesPanel = new JPanel();
        upgradesPanel.setLayout(new BoxLayout(upgradesPanel, BoxLayout.Y_AXIS));

        // 공격력 업그레이드
        upgradesPanel.add(createUpgradePanel("attack", "⚡ Attack Speed",
            "Increases firing rate (reduces cooldown)", Color.RED));
        upgradesPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 체력 업그레이드
        upgradesPanel.add(createUpgradePanel("health", "❤️ Max Health",
            "Increases maximum HP at game start", Color.GREEN));
        upgradesPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 속도 업그레이드
        upgradesPanel.add(createUpgradePanel("speed", "🚀 Movement Speed",
            "Increases ship movement speed", Color.BLUE));

        tabPanel.add(infoLabel, BorderLayout.NORTH);
        tabPanel.add(upgradesPanel, BorderLayout.CENTER);

        return tabPanel;
    }

    /**
     * 개별 업그레이드 패널 생성
     */
    private JPanel createUpgradePanel(String upgradeType, String title, String description, Color accentColor) {
        JPanel panel = createUpgradePanelContainer(accentColor);
        JPanel infoPanel = createUpgradeInfoPanel(title, description, accentColor);
        JPanel controlPanel = createUpgradeControlPanel(upgradeType, title);

        panel.add(infoPanel, BorderLayout.CENTER);
        panel.add(controlPanel, BorderLayout.EAST);
        return panel;
    }

    private JPanel createUpgradePanelContainer(Color accentColor) {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setBackground(new Color(245, 245, 245));
        return panel;
    }

    private JPanel createUpgradeInfoPanel(String title, String description, Color accentColor) {
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(FONT_ARIAL, Font.BOLD, 16));
        titleLabel.setForeground(accentColor);

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font(FONT_ARIAL, Font.PLAIN, 12));

        infoPanel.add(titleLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(descLabel);
        return infoPanel;
    }

    private JPanel createUpgradeControlPanel(String upgradeType, String title) {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setOpaque(false);

        int currentLevel = firebaseManager.isLoggedIn() ? firebaseManager.getUpgradeLevel(upgradeType) : 0;
        int nextCost = firebaseManager.getUpgradeCost(upgradeType);

        JLabel levelLabel = createUpgradeLevelLabel(currentLevel);
        JButton buyButton = createUpgradeBuyButton(upgradeType, title, currentLevel, nextCost);

        controlPanel.add(levelLabel);
        controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        controlPanel.add(buyButton);
        return controlPanel;
    }

    private JLabel createUpgradeLevelLabel(int currentLevel) {
        JLabel levelLabel = new JLabel("Level: " + currentLevel + " / 5");
        levelLabel.setFont(new Font(FONT_ARIAL, Font.BOLD, 14));
        levelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return levelLabel;
    }

    private JButton createUpgradeBuyButton(String upgradeType, String title, int currentLevel, int nextCost) {
        JButton buyButton = new JButton();
        buyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buyButton.setPreferredSize(new Dimension(150, 30));
        buyButton.setMaximumSize(new Dimension(150, 30));

        if (nextCost == -1) {
            buyButton.setText("MAX LEVEL");
            buyButton.setEnabled(false);
        } else {
            buyButton.setText("Upgrade (" + nextCost + " pts)");
            buyButton.addActionListener(e -> handleUpgradePurchase(upgradeType, title, currentLevel, nextCost));
        }
        return buyButton;
    }

    private void handleUpgradePurchase(String upgradeType, String title, int currentLevel, int nextCost) {
        if (!firebaseManager.isLoggedIn()) {
            JOptionPane.showMessageDialog(this, "Please login first!", ERROR_TEXT, JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!confirmUpgradePurchase(title, currentLevel, nextCost)) {
            return;
        }

        boolean success = firebaseManager.purchaseUpgrade(upgradeType, nextCost);
        if (success) {
            showUpgradeSuccess(title, currentLevel);
            refreshStoreDialog();
        } else {
            showUpgradeFailure(nextCost);
        }
    }

    private boolean confirmUpgradePurchase(String title, int currentLevel, int nextCost) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Purchase " + title + " upgrade for " + nextCost + " points?\n" +
            "Current level: " + currentLevel + " → " + (currentLevel + 1),
            "Confirm Upgrade", JOptionPane.YES_NO_OPTION);
        return confirm == JOptionPane.YES_OPTION;
    }

    private void showUpgradeSuccess(String title, int currentLevel) {
        JOptionPane.showMessageDialog(this,
            title + " upgraded to level " + (currentLevel + 1) + "!",
            "Upgrade Successful", JOptionPane.INFORMATION_MESSAGE);
        refreshPoints();
    }

    private void showUpgradeFailure(int nextCost) {
        int userPoints = firebaseManager.getUserPoints();
        JOptionPane.showMessageDialog(this,
            "Upgrade failed!\nYour points: " + userPoints + "\nRequired: " + nextCost,
            "Insufficient Points", JOptionPane.ERROR_MESSAGE);
    }

    private void refreshStoreDialog() {
        dispose();
        new StoreDialog((JFrame) getOwner()).setVisible(true);
    }

    /**
     * 개별 아이템의 정보를 담는 패널을 생성합니다.
     */
    private JPanel createItemPanel(GameItem item) {
        JPanel itemPanel = createItemPanelContainer();

        JLabel imageLabel = createItemImageLabel(item);
        JLabel nameLabel = createItemNameLabel(item);
        JLabel priceLabel = createItemPriceLabel(item);
        JButton detailButton = createItemDetailButton(item);
        JButton buyButton = createItemBuyButton(item);

        addComponentsToItemPanel(itemPanel, imageLabel, nameLabel, priceLabel, detailButton, buyButton);
        return itemPanel;
    }

    private JPanel createItemPanelContainer() {
        JPanel itemPanel = new JPanel();
        itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.Y_AXIS));
        itemPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        itemPanel.setBackground(Color.WHITE);
        return itemPanel;
    }

    private JLabel createItemImageLabel(GameItem item) {
        String imageFile = resolveImageFilename(item);
        ImageIcon icon = loadIconFromSprites(imageFile);

        JLabel imageLabel;
        if (icon != null && icon.getImage() != null) {
            Image scaledImage = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            imageLabel = new JLabel(new ImageIcon(scaledImage));
        } else {
            imageLabel = new JLabel("No Image", SwingConstants.CENTER);
            imageLabel.setPreferredSize(new Dimension(80, 80));
            imageLabel.setForeground(Color.RED);
        }
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return imageLabel;
    }

    private JLabel createItemNameLabel(GameItem item) {
        JLabel nameLabel = new JLabel("<html><b>" + item.getName() + "</b></html>", SwingConstants.CENTER);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return nameLabel;
    }

    private JLabel createItemPriceLabel(GameItem item) {
        JLabel priceLabel = new JLabel("Price: " + item.getPrice() + " pts", SwingConstants.CENTER);
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return priceLabel;
    }

    private JButton createItemDetailButton(GameItem item) {
        JButton detailButton = new JButton("Details");
        detailButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailButton.setMaximumSize(new Dimension(150, 25));
        detailButton.addActionListener(e -> showItemDetails(item));
        return detailButton;
    }

    private void showItemDetails(GameItem item) {
        JOptionPane.showMessageDialog(
            this,
            item.getDescription(),
            "Item Details: " + item.getName(),
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private JButton createItemBuyButton(GameItem item) {
        JButton buyButton = new JButton("Buy (" + item.getPrice() + " pts)");
        buyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buyButton.setMaximumSize(new Dimension(150, 30));
        buyButton.addActionListener(e -> handleItemPurchase(item));
        return buyButton;
    }

    private void handleItemPurchase(GameItem item) {
        if (!firebaseManager.isLoggedIn()) {
            JOptionPane.showMessageDialog(this, "Please login first!", ERROR_TEXT, JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!hasEnoughPointsForItem(item)) {
            return;
        }

        if (!confirmItemPurchase(item)) {
            return;
        }

        executePurchase(item);
    }

    private boolean hasEnoughPointsForItem(GameItem item) {
        int currentPoints = firebaseManager.getUserPoints();
        if (currentPoints < item.getPrice()) {
            JOptionPane.showMessageDialog(this,
                "Not enough points!\nCurrent: " + currentPoints + " pts\nRequired: " + item.getPrice() + " pts",
                "Purchase Failed", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private boolean confirmItemPurchase(GameItem item) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Purchase " + item.getName() + " for " + item.getPrice() + " points?",
            "Confirm Purchase", JOptionPane.YES_NO_OPTION);
        return confirm == JOptionPane.YES_OPTION;
    }

    private void executePurchase(GameItem item) {
        boolean success = firebaseManager.purchaseItem(item.getId(), item.getName(), item.getPrice());
        if (success) {
            showPurchaseSuccess(item);
        } else {
            showPurchaseFailure();
        }
    }

    private void showPurchaseSuccess(GameItem item) {
        JOptionPane.showMessageDialog(this,
            "Successfully purchased " + item.getName() + "!",
            "Purchase Successful", JOptionPane.INFORMATION_MESSAGE);
        refreshPoints();
    }

    private void showPurchaseFailure() {
        JOptionPane.showMessageDialog(this,
            "Purchase failed. Please try again.",
            ERROR_TEXT, JOptionPane.ERROR_MESSAGE);
    }

    private void addComponentsToItemPanel(JPanel itemPanel, JLabel imageLabel, JLabel nameLabel,
                                          JLabel priceLabel, JButton detailButton, JButton buyButton) {
        itemPanel.add(Box.createVerticalStrut(5));
        itemPanel.add(imageLabel);
        itemPanel.add(Box.createVerticalStrut(5));
        itemPanel.add(nameLabel);
        itemPanel.add(priceLabel);
        itemPanel.add(Box.createVerticalStrut(5));
        itemPanel.add(detailButton);
        itemPanel.add(Box.createVerticalStrut(5));
        itemPanel.add(buyButton);
        itemPanel.add(Box.createVerticalStrut(5));
    }

    /**
     * 포인트 표시 업데이트
     */
    private void refreshPoints() {
        int newPoints = firebaseManager.isLoggedIn() ? firebaseManager.getUserPoints() : 0;
        pointsLabel.setText("Your Points: " + newPoints);
    }

    private String resolveImageFilename(GameItem item) {
        String explicitFilename = getExplicitImageFilename(item);
        if (explicitFilename != null) {
            return explicitFilename;
        }
        return resolveImageFromItemName(item);
    }

    private String getExplicitImageFilename(GameItem item) {
        if (item == null || item.getImageFileName() == null) {
            return null;
        }
        String raw = item.getImageFileName().trim();
        return raw.isEmpty() ? null : raw;
    }

    private String resolveImageFromItemName(GameItem item) {
        String name = (item != null && item.getName() != null) ? item.getName().toLowerCase() : "";
        String key = name.replaceAll("\\s+", "");
        return matchImageByKeyword(key);
    }

    private String matchImageByKeyword(String key) {
        if (key.contains("ammo")) return "item_ammo_boost.png";
        if (key.contains("double") && key.contains("score")) return "item_double_score.png";
        if (key.contains("invinc") || key.contains("shield")) return "item_invincibility.png";
        if (key.contains("pluslife") || key.contains("extralife") || key.contains("life")) return "item_plusLife.png";
        return "item_plusLife.png"; // default fallback
    }

    private ImageIcon loadIconFromSprites(String filename) {
        if (filename == null || filename.isEmpty()) return null;
        String[] candidates = new String[] {
            "sprites/" + filename,
            "/sprites/" + filename,
            "resources/sprites/" + filename,
            "/resources/sprites/" + filename
        };
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (String c : candidates) {
            URL url = getClass().getResource(c);
            if (url == null) {
                url = cl.getResource(c.startsWith("/") ? c.substring(1) : c);
            }
            if (url != null) {
                return new ImageIcon(url);
            }
        }
        System.err.println("[WARN] StoreDialog: image not found for " + filename);
        return null;
    }
}
