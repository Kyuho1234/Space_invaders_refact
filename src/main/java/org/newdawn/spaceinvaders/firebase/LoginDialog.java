package org.newdawn.spaceinvaders.firebase;

import javax.swing.*;
import java.awt.*;

/** 이메일/비밀번호 로그인/회원가입 다이얼로그 */
public class LoginDialog extends JDialog {
    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private boolean loginSuccessful = false;

    public LoginDialog(JFrame parent) {
        super(parent, "Login / Register", true);
        setSize(420, 220);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(12, 12));

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        form.add(new JLabel("Email:"));
        form.add(emailField);
        form.add(new JLabel("Password:"));
        form.add(passwordField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");
        JButton cancelBtn = new JButton("Cancel");
        buttons.add(loginBtn);
        buttons.add(registerBtn);
        buttons.add(cancelBtn);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        // 로그인
        loginBtn.addActionListener(e -> doLogin());
        // 회원가입
        registerBtn.addActionListener(e -> doRegister());
        // 취소
        cancelBtn.addActionListener(e -> dispose());
    }

    private void doLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Email/Password를 입력하세요.");
            return;
        }
        boolean ok = FirebaseManager.getInstance().signInWithEmailPassword(email, password);
        if (ok) {
            JOptionPane.showMessageDialog(this, "Login successful!");
            loginSuccessful = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Login failed. 이메일/비밀번호를 확인하세요.");
        }
    }

    private void doRegister() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Email/Password를 입력하세요.");
            return;
        }
        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this, "비밀번호는 최소 6자 이상이어야 합니다.");
            return;
        }
        boolean ok = FirebaseManager.getInstance().signUpWithEmailPassword(email, password);
        if (ok) {
            JOptionPane.showMessageDialog(this, "Registered & logged in!");
            loginSuccessful = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Sign-up failed. 이미 사용 중인 이메일인지 확인하세요.");
        }
    }

    public boolean isLoginSuccessful() {
        return loginSuccessful;
    }
}
