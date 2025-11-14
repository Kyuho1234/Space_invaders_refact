package org.newdawn.spaceinvaders.rendering;

import org.newdawn.spaceinvaders.GameConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Renderer for player HP bars.
 * Supports single-player (centered) and two-player (left/right) modes.
 */
public class HPBarRenderer {

    private static final String FONT_ARIAL = "ARIAL";

    /**
     * Draw HP bars for all active players.
     * @param g2 Graphics context
     * @param canvasWidth Canvas width
     * @param canvasHeight Canvas height
     * @param isTwoPlayerMode Whether two-player mode is active
     * @param player1Health Player 1 current health
     * @param player1MaxHealth Player 1 max health
     * @param player2Health Player 2 current health (used only in 2P mode)
     * @param player2MaxHealth Player 2 max health (used only in 2P mode)
     */
    public void drawPlayerHPBars(Graphics2D g2, int canvasWidth, int canvasHeight,
                                 boolean isTwoPlayerMode,
                                 int player1Health, int player1MaxHealth,
                                 int player2Health, int player2MaxHealth) {
        if (isTwoPlayerMode) {
            // Draw P1's HP Bar on the bottom-left
            drawSingleHPBar(g2, "P1", player1Health, player1MaxHealth, "left", canvasWidth, canvasHeight);
            // Draw P2's HP Bar on the bottom-right
            drawSingleHPBar(g2, "P2", player2Health, player2MaxHealth, "right", canvasWidth, canvasHeight);
        } else {
            // Default 1P behavior: a single bar in the center
            drawSingleHPBar(g2, null, player1Health, player1MaxHealth, "center", canvasWidth, canvasHeight);
        }
    }

    /**
     * Helper method to draw a single segmented HP bar.
     * @param g2 The graphics context
     * @param label The label for the bar (e.g., "P1") or null for none
     * @param currentHP The current health points
     * @param maxHP The maximum health points
     * @param position Where to draw the bar ("left", "right", or "center")
     * @param canvasW Canvas width
     * @param canvasH Canvas height
     */
    private void drawSingleHPBar(Graphics2D g2, String label, int currentHP, int maxHP,
                                 String position, int canvasW, int canvasH) {
        int segments = Math.max(1, maxHP);
        int segWidth = GameConstants.HP_SEGMENT_WIDTH;
        int segHeight = GameConstants.HP_SEGMENT_HEIGHT;
        int gap = GameConstants.HP_SEGMENT_GAP;

        int totalW = segments * segWidth + (segments - 1) * gap;
        int y0 = canvasH - 28; // Position from the bottom edge
        int x0;

        // Determine horizontal position based on the 'position' parameter
        switch (position) {
            case "left":
                x0 = 40; // Margin from the left edge
                break;
            case "right":
                x0 = canvasW - totalW - 40; // Margin from the right edge
                break;
            default: // "center"
                x0 = (canvasW - totalW) / 2;
                break;
        }

        // Draw the player label (e.g., "P1") above the bar if provided
        if (label != null) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font(FONT_ARIAL, Font.BOLD, 14));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, x0, y0 - fm.getHeight() / 2);
        }

        // Draw each segment of the HP bar
        for (int i = 0; i < segments; i++) {
            int x = x0 + i * (segWidth + gap);
            int y = y0;

            g2.setColor(new Color(20, 20, 20, 180));
            g2.fillRect(x - 2, y - 2, segWidth + 4, segHeight + 4);

            g2.setColor(Color.DARK_GRAY); // Background for an empty segment
            g2.fillRect(x, y, segWidth, segHeight);

            if (i < currentHP) {
                g2.setColor(Color.GREEN); // Fill for a full health segment
                g2.fillRect(x, y, segWidth, segHeight);
            }

            g2.setColor(Color.WHITE); // Border for the segment
            g2.drawRect(x, y, segWidth, segHeight);
        }
    }
}
