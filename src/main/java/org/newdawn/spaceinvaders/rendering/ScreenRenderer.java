package org.newdawn.spaceinvaders.rendering;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Renderer for overlay screens (stage select, pause, game over).
 * Handles all screen-sized overlays and dialogs.
 */
public class ScreenRenderer {

    private static final String FONT_ARIAL = "ARIAL";
    private static final String PRESS_ANY_KEY_MESSAGE = "PRESS_ANY_KEY_MESSAGE";

    /**
     * Draw stage selection screen
     * @param g2 Graphics context
     * @param selectedStage Currently selected stage
     * @param maxClearedStage Maximum cleared stage
     */
    public void drawStageSelectScreen(Graphics2D g2, int selectedStage, int maxClearedStage) {
        // 1. 배경 어둡게 처리
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, 1200, 900);

        // 2. 제목 그리기
        String title = "SELECT NEXT STAGE";
        g2.setColor(Color.WHITE);
        g2.setFont(new Font(FONT_ARIAL, Font.BOLD, 36));
        FontMetrics fmTitle = g2.getFontMetrics();
        g2.drawString(title, (1200 - fmTitle.stringWidth(title)) / 2, 100);

        // 3. 스테이지 버튼 그리기 (1단계 ~ 5단계)
        int btnSize = 60;
        int gap = 20;
        int totalStages = 5;
        int totalW = totalStages * btnSize + (totalStages - 1) * gap;
        int startX = (1200 - totalW) / 2;
        int startY = 200;

        for (int stage = 1; stage <= totalStages; stage++) {
            int x = startX + (stage - 1) * (btnSize + gap);

            // 선택된 스테이지에 따라 색상 변경
            if (stage == selectedStage) {
                // 1. 선택된 스테이지: 노란색
                g2.setColor(Color.YELLOW);
            } else if (stage <= maxClearedStage || stage == maxClearedStage + 1) {
                // 2. 클리어했거나, 현재 선택 가능한 스테이지
                g2.setColor(Color.GREEN);
            } else {
                // 3. 잠긴 스테이지: 회색
                g2.setColor(Color.LIGHT_GRAY);
            }

            // 버튼 사각형
            g2.fillRect(x, startY, btnSize, btnSize);
            g2.setColor(Color.BLACK);
            g2.drawRect(x, startY, btnSize, btnSize);

            // 버튼 텍스트 (스테이지 번호)
            String stageNum = String.valueOf(stage);
            g2.setColor(Color.BLACK);
            g2.setFont(new Font(FONT_ARIAL, Font.BOLD, 24));
            FontMetrics fmBtn = g2.getFontMetrics();
            g2.drawString(stageNum, x + (btnSize - fmBtn.stringWidth(stageNum)) / 2, startY + fmBtn.getAscent() + 10);

            // 잠금 상태 표시
            if (stage > maxClearedStage + 1) {
                g2.setColor(new Color(0, 0, 0, 150));
                g2.fillRect(x, startY, btnSize, btnSize);
                g2.setColor(Color.RED);
                g2.drawString("LOCK", x + 5, startY + 40);
            }
        }

        // 안내 메시지
        String info = "Use Left/Right Arrows to select, Enter to start.";
        g2.setColor(Color.WHITE);
        g2.setFont(new Font(FONT_ARIAL, Font.PLAIN, 18));
        FontMetrics fmInfo = g2.getFontMetrics();
        g2.drawString(info, (1200 - fmInfo.stringWidth(info)) / 2, 500);

        // 폰트와 색상 복구 (안전성)
        g2.setColor(Color.white);
        g2.setFont(new Font(FONT_ARIAL, Font.PLAIN, 12));
    }

    /**
     * Draw pause prompt overlay
     * @param g2 Graphics context
     * @param currentScore Current score
     */
    public void drawPausePrompt(Graphics2D g2, int currentScore) {
        // dim background
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, 1200, 900);
        g2.setColor(Color.white);
        String pts = String.format("%03d", Math.max(0, currentScore));
        String l1 = "여기서 멈춘다면 " + pts + " 포인트를 얻습니다.";
        String l2 = "메인메뉴로 나가려면 ESC, 계속 플레이하려면 SPACE를 누르십시오.";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(l1, (1200 - fm.stringWidth(l1)) / 2, 260);
        g2.drawString(l2, (1200 - fm.stringWidth(l2)) / 2, 300);
    }

    /**
     * Draw game over screen
     * @param g2 Graphics context
     * @param message Main message to display
     * @param newHighScoreAchieved Whether new high score was achieved
     * @param finalScore Final score (only used if newHighScoreAchieved is true)
     */
    public void drawGameOverScreen(Graphics2D g2, String message, boolean newHighScoreAchieved, int finalScore) {
        g2.setColor(Color.white);
        String mainMessage = message;
        FontMetrics fm = g2.getFontMetrics();

        // 1. 주 메시지 출력
        g2.drawString(mainMessage, (1200 - fm.stringWidth(mainMessage)) / 2, 250);
        g2.drawString(PRESS_ANY_KEY_MESSAGE, (1200 - fm.stringWidth(PRESS_ANY_KEY_MESSAGE)) / 2, 300);

        // 2. 최고 점수 안내문 표시
        if (newHighScoreAchieved) {
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font(FONT_ARIAL, Font.BOLD, 30));

            String highMsg = "🎉 New High Score! (" + finalScore + ")";

            FontMetrics fm30 = g2.getFontMetrics();
            g2.drawString(highMsg, (1200 - fm30.stringWidth(highMsg)) / 2, 400);
        }

        // 폰트와 색상 복구
        g2.setColor(Color.white);
        g2.setFont(new Font(FONT_ARIAL, Font.PLAIN, 12));
    }

    /**
     * Draw HUD (score, stage)
     * @param g2 Graphics context
     * @param currentStage Current stage number
     * @param score Current score
     */
    public void drawHUD(Graphics2D g2, int currentStage, int score) {
        g2.setColor(Color.white);
        g2.drawString("Stage: " + currentStage, 10, 30);
        g2.drawString("Score: " + score, 10, 50);
    }
}
