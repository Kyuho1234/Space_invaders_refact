package org.newdawn.spaceinvaders;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Color;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;

/**
 * A sprite to be displayed on the screen. Note that a sprite
 * contains no state information, i.e. its just the image and 
 * not the location. This allows us to use a single sprite in
 * lots of different places without having to store multiple 
 * copies of the image.
 * 
 * @author Kevin Glass
 */
public class Sprite {
	/** The image to be drawn for this sprite */
	private Image image;
	/** Color tint for the sprite */
	private Color tintColor = null;

	/**
	 * Create a new sprite based on an image
	 *
	 * @param image The image that is this sprite
	 */
	public Sprite(Image image) {
		this.image = image;
	}

	/**
	 * Create a new sprite with color tinting
	 *
	 * @param image The image that is this sprite
	 * @param tintColor The color to tint this sprite
	 */
	public Sprite(Image image, Color tintColor) {
		this.image = image;
		this.tintColor = tintColor;
	}

	/**
	 * Get the width of the drawn sprite
	 * 
	 * @return The width in pixels of this sprite
	 */
	public int getWidth() {
		return image.getWidth(null);
	}

	/**
	 * Get the height of the drawn sprite
	 * 
	 * @return The height in pixels of this sprite
	 */
	public int getHeight() {
		return image.getHeight(null);
	}
	
	/**
	 * Draw the sprite onto the graphics context provided
	 *
	 * @param g The graphics context on which to draw the sprite
	 * @param x The x location at which to draw the sprite
	 * @param y The y location at which to draw the sprite
	 */
	public void draw(Graphics g,int x,int y) {
		if (tintColor == null) {
			// Draw normally without tint
			g.drawImage(image,x,y,null);
		} else {
			// Draw with color tint
			Graphics2D g2d = (Graphics2D) g.create();
			g2d.drawImage(image,x,y,null);

			// Apply color tint with multiply blend
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.7f));
			g2d.setColor(tintColor);
			g2d.fillRect(x, y, getWidth(), getHeight());

			g2d.dispose();
		}
	}

	/**
	 * Create a tinted version of this sprite
	 *
	 * @param tintColor The color to tint the sprite
	 * @return A new sprite with the specified tint
	 */
	public Sprite createTintedSprite(Color tintColor) {
		return new Sprite(this.image, tintColor);
	}
}