package org.newdawn.spaceinvaders.settings; // 본인 패키지 선언

// --- 아래 import 목록을 모두 추가하세요 ---
import org.newdawn.spaceinvaders.settings.SettingsManager; // SettingsManager import
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;

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

    private void initializeUI(){
    setSize(300,200);
    setLocationRelativeTo(getParent());
    setLayout(new BorderLayout());

    //플레이어 선택 패널
        JPanel playerSelectionPanel= new JPanel();
        playerSelectionPanel.setBorder(BorderFactory.createTitledBorder("Player Mode"));

        onePlayerRadioButton = new JRadioButton("1 Player");
        twoPlayerRadioButton = new JRadioButton("2 Player");
        // 라디오버튼중 하나만 선택되도록 묶는 역할
        ButtonGroup playerGroup = new ButtonGroup();
        playerGroup.add(onePlayerRadioButton);
        playerGroup.add(twoPlayerRadioButton);

        playerSelectionPanel.add(onePlayerRadioButton);
        playerSelectionPanel.add(twoPlayerRadioButton);

// --- 저장/취소 버튼 패널 ---
        JPanel buttonPanel = new JPanel();
        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        //패널들을 다이얼 로그에 추가
        add(playerSelectionPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);


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