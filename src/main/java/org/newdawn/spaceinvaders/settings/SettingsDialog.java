package org.newdawn.spaceinvaders.settings; // 본인 패키지 선언


import javax.swing.*;
import java.awt.*;

// 설정 '창'이므로 JDialog를 상속받습니다.
public class SettingsDialog extends JDialog {

    private JRadioButton onePlayerRadioButton;
    private JRadioButton twoPlayerRadioButton;
    private JButton saveButton;
    private JButton cancelButton;

    /**
     * 설정 창 생성자
     * @param parent 이 창을 띄운 부모 프레임 (예: 메인 메뉴 창)
     */
    public SettingsDialog(JFrame parent) {
        // JDialog의 생성자 호출: (부모 창, 창 제목, 창이 켜있을 때 다른 창 클릭 막기 여부)
        super(parent, "Settings", true);

        initializeUI();         //UI컴포넌트 생성 및 배치
        loadCurrentSettings(); // 현재 값을 불러와 ui에 반영
        setupEventHandlers();   //버튼 클릭 이벤트 처리
    }


    /*
    ui컴포넌트 생성하고 화면에 배치하는 메소드
     */

    private void initializeUI() {

        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());

        // --- 플레이어 선택 패널 ---
        JPanel playerSelectionPanel = new JPanel();
        playerSelectionPanel.setBorder(BorderFactory.createTitledBorder("Player Mode"));


        playerSelectionPanel.setLayout(new BoxLayout(playerSelectionPanel, BoxLayout.Y_AXIS));

        onePlayerRadioButton = new JRadioButton("1 Player");
        twoPlayerRadioButton = new JRadioButton("2 Player");

        ButtonGroup playerGroup = new ButtonGroup();
        playerGroup.add(onePlayerRadioButton);
        playerGroup.add(twoPlayerRadioButton);

        JLabel keyInfoLabel = new JLabel("   (2P Keys: W, A, S, D)");
        keyInfoLabel.setForeground(Color.GRAY);

        // 왼쪽 정렬 맞추기 (BoxLayout 특성상 필요할 수 있음)
        onePlayerRadioButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        twoPlayerRadioButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        keyInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        playerSelectionPanel.add(onePlayerRadioButton);
        playerSelectionPanel.add(Box.createVerticalStrut(5)); // 간격 5px 추가
        playerSelectionPanel.add(twoPlayerRadioButton);
        playerSelectionPanel.add(Box.createVerticalStrut(2)); // 간격 2px 추가
        playerSelectionPanel.add(keyInfoLabel);               // 설명글 추가

        // --- 저장/취소 버튼 패널 ---
        JPanel buttonPanel = new JPanel();
        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(playerSelectionPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        add(playerSelectionPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setSize(400, 350); // 가로 400, 세로 350


        setLocationRelativeTo(getParent());
    }

    private void loadCurrentSettings(){
        boolean isTwoPlayer = SettingsManager.isTwoPlayerEnabled();
        if(isTwoPlayer){
            twoPlayerRadioButton.setSelected(true);
        } else {
            onePlayerRadioButton.setSelected(true);
        }

    }

    /**
     * 버튼 클릭에 대한 동작을 설정합니다.
     */
    private void setupEventHandlers() {
        // 저장 버튼 클릭 시
        saveButton.addActionListener(e -> {
            // 2P 라디오 버튼이 선택되었는지 확인합니다.
            boolean selectedTwoPlayer = twoPlayerRadioButton.isSelected();

            // SettingsManager를 통해 설정을 저장합니다.
            SettingsManager.setTwoPlayerEnabled(selectedTwoPlayer);

            // 사용자에게 저장 완료 메시지를 보여줍니다.
            JOptionPane.showMessageDialog(this, "Settings saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

            // 창을 닫습니다.
            dispose();
        });

        // 취소 버튼 클릭 시
        cancelButton.addActionListener(e -> {
            // 아무것도 저장하지 않고 그냥 창을 닫습니다.
            dispose();
        });
    }


}