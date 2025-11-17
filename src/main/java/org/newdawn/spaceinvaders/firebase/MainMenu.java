package org.newdawn.spaceinvaders.firebase;

import org.newdawn.spaceinvaders.Game;

import org.newdawn.spaceinvaders.ui.StoreDialog;
import javax.swing.*;
import java.awt.*;
import org.newdawn.spaceinvaders.items.GameItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.newdawn.spaceinvaders.settings.SettingsDialog;
import org.newdawn.spaceinvaders.firebase.RankingDialog;
import org.newdawn.spaceinvaders.firebase.RankingDialog;

/** 메인 메뉴: 로그인/회원가입/랭킹/게임 시작 */
public class MainMenu extends JFrame {
    private static final String FONT_ARIAL = "Arial";

    private JPanel mainPanel;
    private JLabel titleLabel;
    private JButton startGameButton;
    private JButton loginButton;
    private JButton rankingButton;
    private JButton storeButton;
    private JButton settingsButton;
    private JButton exitButton;
    private JLabel userStatusLabel;
    private Game currentGame;      // 멤버 변수 선언

    public MainMenu() {

        initializeUI();
        setupEventHandlers();
        updateUserStatus();
    }

        private void initializeUI() {
        setTitle("Space Invaders - Main Menu");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(800, 700);
        setLocationRelativeTo(null);
        setResizable(false);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.BLACK);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 90, 40, 90));

        titleLabel = new JLabel("SPACE INVADERS");
        titleLabel.setFont(new Font(FONT_ARIAL, Font.BOLD, 48));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        userStatusLabel = new JLabel("Guest User");
        userStatusLabel.setFont(new Font(FONT_ARIAL, Font.PLAIN, 16));
        userStatusLabel.setForeground(Color.YELLOW);
        userStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        startGameButton = createMenuButton("Start Game");
        loginButton     = createMenuButton("Login / Register");
        rankingButton   = createMenuButton("Ranking");
        storeButton = createMenuButton("Store");
        settingsButton  = createMenuButton("Settings");
        exitButton      = createMenuButton("Exit");

        mainPanel.add(Box.createVerticalGlue());
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(userStatusLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 40)));
        mainPanel.add(startGameButton);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(loginButton);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(rankingButton);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        mainPanel.add(storeButton);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(settingsButton);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(exitButton);
        mainPanel.add(Box.createVerticalGlue());

        // ESC 키로 프로그램 종료
        mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "exitApp");
        mainPanel.getActionMap().put("exitApp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        add(mainPanel);
    }

    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font(FONT_ARIAL, Font.BOLD, 20));
        button.setForeground(Color.BLACK);
        button.setBackground(Color.LIGHT_GRAY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(300, 50));
        button.setPreferredSize(new Dimension(300, 50));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { button.setBackground(Color.WHITE); }
            public void mouseExited (java.awt.event.MouseEvent evt) { button.setBackground(Color.LIGHT_GRAY); }
        });
        return button;
    }

    private void setupEventHandlers() {
        ActionListener menuActionListener = this::handleMenuAction;

        startGameButton.addActionListener(menuActionListener);
        loginButton.addActionListener(menuActionListener);
        rankingButton.addActionListener(menuActionListener);
        storeButton.addActionListener(menuActionListener);
        settingsButton.addActionListener(menuActionListener);
        exitButton.addActionListener(menuActionListener);
    }

    private void handleMenuAction(ActionEvent e) {
        Object src = e.getSource();

        if (src == startGameButton) {
            startGame();
        } else if (src == loginButton) {
            if (FirebaseManager.getInstance().isLoggedIn()) {
                int result = JOptionPane.showConfirmDialog(
                        this,
                        "Logged in as: " + FirebaseManager.getInstance().getCurrentUserEmail() + "\nLogout?",
                        "Logout",
                        JOptionPane.YES_NO_OPTION
                );
                if (result == JOptionPane.YES_OPTION) {
                    FirebaseManager.getInstance().signOut();
                    updateUserStatus();
                }
            } else {
                LoginDialog dlg = new LoginDialog(this);
                dlg.setVisible(true);
                if (dlg.isLoginSuccessful()) {
                    updateUserStatus();
                }
            }
        } else if (src == rankingButton) {
            RankingDialog rankingDialog = new RankingDialog(MainMenu.this);
            rankingDialog.setVisible(true);
        } else if (src == storeButton) {
            showStore();
        } else if (src == settingsButton) {
            new SettingsDialog(this).setVisible(true);
        } else if (src == exitButton) {
            System.exit(0);
        }
    }

    private void startGame() {
        // 메뉴 숨기고 게임 시작 (게임 루프는 별도 스레드 권장)
        this.setVisible(false);
        new Thread(() -> {
            try {
                Game game = new Game();
                SwingUtilities.invokeLater(game::requestFocusInWindow);
                game.gameLoop();
            } finally {
                // 게임 종료 후 메뉴 복귀 (EDT에서)
                SwingUtilities.invokeLater(() -> MainMenu.this.setVisible(true));
            }
        }, "GameLoop-Thread").start();
    }

    private void updateUserStatus() {
        if (FirebaseManager.getInstance().isLoggedIn()) {
            String email = FirebaseManager.getInstance().getCurrentUserEmail();
            userStatusLabel.setText("Welcome, " + email + "!");
            loginButton.setText("Logout");
        } else {
            userStatusLabel.setText("Guest User");
            loginButton.setText("Login / Register");
        }
    }

    private void showStore(){
        // StoreDialog를 메인 메뉴 창(this)을 부모로 하여 생성하고 표시합니다.
        // this는 현재 JFrame 인스턴스(SimpleMainMenu)를 나타냅니다.
        StoreDialog store = new StoreDialog(this);

        // 다이얼로그를 보이게 합니다.
        store.setVisible(true);
    }



    public static void main(String[] args) {

        // macOS에서 생기는 UI 문제 해결 - 수영
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignore) {}

        // Firebase 초기화
        FirebaseManager.getInstance().initialize();

        // MainMenu UI 실행 (EDT)
        SwingUtilities.invokeLater(() -> new MainMenu().setVisible(true));
    }
}
