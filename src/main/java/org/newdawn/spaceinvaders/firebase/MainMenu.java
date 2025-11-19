package org.newdawn.spaceinvaders.firebase;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.settings.SettingsDialog;
import org.newdawn.spaceinvaders.ui.StoreDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;

import java.awt.event.MouseEvent;

/** 메인 메뉴: 로그인/회원가입/랭킹/게임 시작 */
public class MainMenu extends JFrame {
    private static final String FONT_ARIAL = "Arial";

    // 2. 불필요한 필드를 제거하고 필요한 것만 남겼습니다.
    private JButton startGameButton;
    private JButton loginButton;
    private JButton rankingButton;
    private JButton storeButton;
    private JButton settingsButton;
    private JButton exitButton;
    private JLabel userStatusLabel;

    public MainMenu() {
        initializeUI();
        setupEventHandlers();
        updateUserStatus();
    }

    private void initializeUI() {
        setTitle("Space Invaders - Main Menu");
        // 3. WindowConstants.EXIT_ON_CLOSE 사용 (SonarQube 권장)
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(800, 700);
        setLocationRelativeTo(null);
        setResizable(false);

        // 2. mainPanel과 titleLabel을 지역 변수로 변경
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.BLACK);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 90, 40, 90));

        JLabel titleLabel = new JLabel("SPACE INVADERS");
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
        storeButton     = createMenuButton("Store");
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

        // 1. @Override 어노테이션 추가 (MouseAdapter 사용)
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(Color.LIGHT_GRAY);
            }
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
            handleLoginAction();
        } else if (src == rankingButton) {
            new RankingDialog(MainMenu.this).setVisible(true);
        } else if (src == storeButton) {
            showStore();
        } else if (src == settingsButton) {
            new SettingsDialog(this).setVisible(true);
        } else if (src == exitButton) {
            System.exit(0);
        }
    }

    private void handleLoginAction() {
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
    }

    private void startGame() {
        // 메뉴 숨기고 게임 시작 (게임 루프는 별도 스레드 권장)
        this.setVisible(false);
        // 2. 불필요한 currentGame 필드 제거하고 지역 변수로 사용
        new Thread(() -> {
            try {
                Game game = new Game();
                SwingUtilities.invokeLater(game::requestFocusInWindow);
                game.gameLoop();
            } finally {
                // 게임 종료 후 메뉴 복귀 (EDT에서)
                SwingUtilities.invokeLater(() -> {
                    // 기존 MainMenu 인스턴스를 재사용하거나 새로 생성할 수 있음.
                    // 여기서는 간단히 현재 창을 다시 보이게 함.
                    this.setVisible(true);
                });
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

    private void showStore() {
        StoreDialog store = new StoreDialog(this);
        store.setVisible(true);
    }

    public static void main(String[] args) {
        // macOS에서 생기는 UI 문제 해결 - 수영
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignore) {
            // 5. 예외 무시 이유 명시 (SonarQube 권장)
            // UI 룩앤필 설정 실패 시 기본값 사용을 위해 예외를 무시합니다.
        }

        // Firebase 초기화
        FirebaseManager.getInstance().initialize();

        // MainMenu UI 실행 (EDT)
        SwingUtilities.invokeLater(() -> new MainMenu().setVisible(true));
    }
}