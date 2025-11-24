package org.newdawn.spaceinvaders.firebase;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;


/**
 * 랭킹 표시 다이얼로그
 * Firebase Firestore와 연동하여 서버 내 최고 점수 랭킹을 표시합니다.
 */
public class RankingDialog extends JDialog {
    private static final String FONT_ARIAL = "Arial";
    private static final String ERROR_TEXT = "Error";

    private JTable rankingTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private JButton closeButton;

    // 🚀 FirebaseManager 인스턴스를 가져옵니다.
    private final transient FirebaseManager firebaseManager = FirebaseManager.getInstance();
    private final int RANKING_LIMIT = 20; // 상위 20명만 가져오도록 제한

    public RankingDialog(Frame parent) {
        super(parent, "Ranking", true);
        initializeUI();
        setupEventHandlers();
        loadRankingData();
    }

    private void initializeUI() {
        setSize(600, 400);
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 타이틀
        JLabel titleLabel = new JLabel("Top Players Ranking (High Score)", SwingConstants.CENTER);
        titleLabel.setFont(new Font(FONT_ARIAL, Font.BOLD, 20));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // 테이블 설정
        // [주의] Level, Date 필드는 FirebaseManager.getTopScores()에서 현재 0, N/A로 반환됨
        String[] columnNames = {"Rank", "Player (Email)", "Highest Score", "Level", "Date"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 테이블 편집 불가
            }
        };

        rankingTable = new JTable(tableModel);
        rankingTable.setFont(new Font(FONT_ARIAL, Font.PLAIN, 14));
        rankingTable.getTableHeader().setFont(new Font(FONT_ARIAL, Font.BOLD, 14));
        rankingTable.setRowHeight(25);

        // 컬럼 너비 설정
        rankingTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // Rank
        rankingTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Player (Email)
        rankingTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Highest Score
        rankingTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Level
        rankingTable.getColumnModel().getColumn(4).setPreferredWidth(120); // Date

        JScrollPane scrollPane = new JScrollPane(rankingTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout());
        refreshButton = new JButton("Refresh");
        closeButton = new JButton("Close");

        refreshButton.setFont(new Font(FONT_ARIAL, Font.PLAIN, 14));
        closeButton.setFont(new Font(FONT_ARIAL, Font.PLAIN, 14));

        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void setupEventHandlers() {
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadRankingData();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private void loadRankingData() {
        // 로딩 메시지 표시
        refreshButton.setEnabled(false);
        refreshButton.setText("Loading...");
        tableModel.setRowCount(0); // 로딩 시작 시 테이블 초기화
        
        // 🚀 SwingWorker의 반환 타입을 List<Map<String, Object>>로 변경합니다.
        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<List<Map<String, Object>>, Void>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                // 🚀 FirebaseManager를 통해 실제 랭킹 데이터를 가져옵니다.
                // FirebaseManager.getTopScores가 모든 유저의 최고 점수를 반환합니다.
                return firebaseManager.getTopScores(RANKING_LIMIT);
            }

            @Override
            protected void done() {
                try {
                    // 결과를 가져옵니다.
                    List<Map<String, Object>> rankingList = get(); 
                    
                    tableModel.setRowCount(0); // 결과 반영 전 다시 초기화
                    int rank = 1;

                    if (rankingList == null || rankingList.isEmpty()) {
                         tableModel.addRow(new Object[]{ "-", "No ranking data available.", "-", "-", "-" });
                    } else {
                        // 가져온 데이터를 JTable에 추가
                        for (Map<String, Object> data : rankingList) {
                            // FirebaseManager에서 가져온 필드를 사용하여 행 추가
                            tableModel.addRow(new Object[]{
                                rank++,
                                data.getOrDefault("player", "Unknown").toString(),
                                data.getOrDefault("score", 0),
                                data.getOrDefault("level", 0), // 현재는 0
                                data.getOrDefault("date", "N/A").toString() // 현재는 N/A
                            });
                        }
                    }

                } catch (Exception e) {
                    tableModel.addRow(new Object[]{ "-", "Failed to load ranking data.", ERROR_TEXT, ERROR_TEXT, ERROR_TEXT });
                    e.printStackTrace();
                } finally {
                    refreshButton.setEnabled(true);
                    refreshButton.setText("Refresh");
                }
            }
        };

        worker.execute();
    }
    
    // 이 메서드들은 이제 loadRankingData()가 FirebaseManager를 직접 사용하므로 더 이상 필요 없습니다.
    /*
    private void loadDummyData() { ... }
    private void loadFirebaseRankingData() { ... }
    public static void saveScore(String playerName, int score, int level) { ... }
    */
}