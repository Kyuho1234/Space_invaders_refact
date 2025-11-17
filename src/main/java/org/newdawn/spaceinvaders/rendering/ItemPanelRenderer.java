package org.newdawn.spaceinvaders.rendering;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;

/**
 * Renderer for the left-side item panel UI.
 * Displays item icons with count badges.
 */
public class ItemPanelRenderer {

    private final java.util.List<String> itemUIList;
    private final int[] itemUICounts;
    private final java.util.List<Image> itemUIIcons;
    private final boolean isLoggedIn;

    /**
     * Create item panel renderer
     * @param itemUIList List of item filenames to display
     * @param itemUICounts Array of item counts
     * @param itemUIIcons List of loaded item icons
     * @param isLoggedIn Whether user is logged in
     */
    public ItemPanelRenderer(java.util.List<String> itemUIList, int[] itemUICounts,
                             java.util.List<Image> itemUIIcons, boolean isLoggedIn) {
        this.itemUIList = itemUIList;
        this.itemUICounts = itemUICounts;
        this.itemUIIcons = itemUIIcons;
        this.isLoggedIn = isLoggedIn;
    }

    /**
     * Draw the item panel on the left side of the screen
     * @param g2 Graphics context
     * @param canvasWidth Canvas width
     * @param canvasHeight Canvas height
     */
    public void drawLeftItemsPanel(Graphics2D g2, int canvasWidth, int canvasHeight) {
        int rows = (itemUIList != null) ? itemUIList.size() : 0;
        if (rows <= 0) return;

        ItemPanelLayout layout = calculateItemPanelLayout(rows, canvasHeight);
        drawItemPanelBackground(g2, layout);
        drawItemSlots(g2, layout, canvasHeight);
    }

    /**
     * Calculate layout metrics for item panel
     */
    private ItemPanelLayout calculateItemPanelLayout(int rows, int canvasH) {
        int pad = 8, gap = 6, startY = 70, innerPad = 6;

        int[] drawWArr = new int[rows];
        int[] drawHArr = new int[rows];
        int[] slotWArr = new int[rows];
        int[] slotHArr = new int[rows];

        int maxPanelW = 0;
        int y = startY;

        for (int i = 0; i < rows; i++) {
            Image icon = (i < itemUIIcons.size()) ? itemUIIcons.get(i) : null;
            int[] sizes = calculateIconSize(icon, innerPad);

            drawWArr[i] = sizes[0];
            drawHArr[i] = sizes[1];
            slotWArr[i] = sizes[2];
            slotHArr[i] = sizes[3];

            maxPanelW = Math.max(maxPanelW, sizes[2]);
            y += sizes[3] + gap;
        }

        int totalPanelH = y - startY - gap + pad;
        if (startY + totalPanelH > canvasH - pad) {
            totalPanelH = Math.max(0, (canvasH - pad) - startY);
        }

        return new ItemPanelLayout(pad, gap, startY, innerPad, maxPanelW, totalPanelH,
                                   drawWArr, drawHArr, slotWArr, slotHArr);
    }

    /**
     * Calculate icon and slot sizes
     * @return [drawW, drawH, slotW, slotH]
     */
    private int[] calculateIconSize(Image icon, int innerPad) {
        int baseMaxW = 48, baseMaxH = 48;
        int imgW = (icon != null) ? icon.getWidth(null) : baseMaxW;
        int imgH = (icon != null) ? icon.getHeight(null) : baseMaxH;
        if (imgW <= 0 || imgH <= 0) { imgW = baseMaxW; imgH = baseMaxH; }

        double fitScale = Math.min((double) baseMaxW / imgW, (double) baseMaxH / imgH);
        int fitW = (int) Math.max(1, Math.round(imgW * fitScale));
        int fitH = (int) Math.max(1, Math.round(imgH * fitScale));
        int drawW = (int) Math.max(1, Math.round(fitW * (2.0 / 3.0)));
        int drawH = (int) Math.max(1, Math.round(fitH * (2.0 / 3.0)));

        int slotW = Math.max(drawW + innerPad * 2, 28);
        int slotH = Math.max(drawH + innerPad * 2, 28);

        return new int[]{drawW, drawH, slotW, slotH};
    }

    /**
     * Draw panel background
     */
    private void drawItemPanelBackground(Graphics2D g2, ItemPanelLayout layout) {
        g2.setColor(new Color(20, 20, 20, 150));
        g2.fillRect(layout.pad - 2, layout.startY - 2, layout.maxPanelW + 4, layout.totalPanelH + 4);
    }

    /**
     * Draw all item slots
     */
    private void drawItemSlots(Graphics2D g2, ItemPanelLayout layout, int canvasH) {
        int rowY = layout.startY;
        int rows = layout.slotWArr.length;

        for (int i = 0; i < rows; i++) {
            if (rowY + layout.slotHArr[i] > canvasH - layout.pad) break;

            drawSingleItemSlot(g2, layout, i, rowY);
            rowY += layout.slotHArr[i] + layout.gap;
        }
    }

    /**
     * Draw a single item slot
     */
    private void drawSingleItemSlot(Graphics2D g2, ItemPanelLayout layout, int index, int rowY) {
        int slotW = layout.slotWArr[index];
        int slotH = layout.slotHArr[index];
        int drawW = layout.drawWArr[index];
        int drawH = layout.drawHArr[index];

        // Slot background
        g2.setColor(new Color(45, 45, 45));
        g2.fillRect(layout.pad, rowY, slotW, slotH);
        g2.setColor(Color.WHITE);
        g2.drawRect(layout.pad, rowY, slotW, slotH);

        // Draw icon or placeholder
        Image icon = (index < itemUIIcons.size()) ? itemUIIcons.get(index) : null;
        int dx = layout.pad + (slotW - drawW) / 2;
        int dy = rowY + (slotH - drawH) / 2;

        if (icon != null) {
            g2.drawImage(icon, dx, dy, drawW, drawH, null);
        } else {
            drawIconPlaceholder(g2, layout, slotW, slotH, rowY);
        }

        // Draw count badge
        drawItemCountBadge(g2, index, dx, dy, drawW, drawH);
    }

    /**
     * Draw placeholder when icon is missing
     */
    private void drawIconPlaceholder(Graphics2D g2, ItemPanelLayout layout, int slotW, int slotH, int rowY) {
        g2.setColor(new Color(80, 80, 80));
        g2.fillRect(layout.pad + layout.innerPad, rowY + layout.innerPad,
                    slotW - layout.innerPad * 2, slotH - layout.innerPad * 2);
        g2.setColor(Color.WHITE);
        g2.drawString("?", layout.pad + slotW / 2 - 3, rowY + slotH / 2 + 4);
    }

    /**
     * Draw item count badge
     */
    private void drawItemCountBadge(Graphics2D g2, int index, int dx, int dy, int drawW, int drawH) {
        int count = (itemUICounts != null && index < itemUICounts.length) ? itemUICounts[index] : 0;
        if (!isLoggedIn) {
            count = 0;
        }

        String label = "x" + count;
        FontMetrics fm = g2.getFontMetrics();
        int bw = fm.stringWidth(label) + 10;
        int bh = fm.getAscent() + fm.getDescent();
        int bx = dx + drawW - bw - 2;
        int by = dy + drawH - bh - 2;

        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRoundRect(bx, by, bw, bh, 8, 8);
        g2.setColor(Color.WHITE);
        g2.drawString(label, bx + 5, by + fm.getAscent());
    }

    /**
     * Layout data for item panel
     */
    private static class ItemPanelLayout {
        final int pad, gap, startY, innerPad, maxPanelW, totalPanelH;
        final int[] drawWArr, drawHArr, slotWArr, slotHArr;

        ItemPanelLayout(int pad, int gap, int startY, int innerPad, int maxPanelW, int totalPanelH,
                        int[] drawWArr, int[] drawHArr, int[] slotWArr, int[] slotHArr) {
            this.pad = pad;
            this.gap = gap;
            this.startY = startY;
            this.innerPad = innerPad;
            this.maxPanelW = maxPanelW;
            this.totalPanelH = totalPanelH;
            this.drawWArr = drawWArr;
            this.drawHArr = drawHArr;
            this.slotWArr = slotWArr;
            this.slotHArr = slotHArr;
        }
    }
}
