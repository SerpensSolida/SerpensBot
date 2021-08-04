package com.serpenssolida.discordbot.module.connect4;

import com.serpenssolida.discordbot.image.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

import static com.serpenssolida.discordbot.module.connect4.Connect4Game.FIELD_HEIGHT;
import static com.serpenssolida.discordbot.module.connect4.Connect4Game.FIELD_WIDTH;

public class Connect4GameDrawer
{
	private static final Color[] pawnColor = {new Color(0xE43613), new Color(0xEFCC1C)};
	private static final Color[] darkPawnColor = {new Color(0x952F15), new Color(0x95771A)};
	
	public static byte[] generateFieldImage(Connect4Game game)
	{
		//Calculate image dimensions.
		int fieldWidth = 512;
		int fieldHeight = (fieldWidth * FIELD_HEIGHT / FIELD_WIDTH);
		
		int imageWidth = fieldWidth;
		int imageHeight = fieldHeight + 64;
		
		int cellWidth = fieldWidth / FIELD_WIDTH;
		int cellHeight = fieldHeight / FIELD_HEIGHT;
		
		//Create the image and get its Graphics.
		BufferedImage fieldImage = new BufferedImage(imageWidth, imageHeight, 6);
		Graphics2D g = (Graphics2D) fieldImage.getGraphics();
		
		//Draw field.
		Connect4GameDrawer.drawField(g, fieldWidth, fieldHeight, cellWidth, cellHeight);
		
		//Draw pawns.
		Connect4GameDrawer.drawPawns(game, g, fieldHeight, cellWidth, cellHeight);
		
		//Draw number guide.
		Connect4GameDrawer.drawNumberGuide(g, fieldHeight, cellWidth, cellHeight);
		
		return ImageUtils.toByteArray(fieldImage);
	}
	
	private static void drawNumberGuide(Graphics2D g, int fieldHeight, int cellWidth, int cellHeight)
	{
		for (int i = 0; i < FIELD_WIDTH; i++)
		{
			g.setFont(g.getFont().deriveFont(32f));
			g.setColor(Color.WHITE);
			int w = g.getFontMetrics().stringWidth("" + (i + 1));
			g.drawString("" + (i + 1), cellWidth / 2 + i * cellWidth - w / 2, fieldHeight + cellHeight / 2);
		}
	}
	
	private static void drawPawns(Connect4Game game, Graphics2D g, int fieldHeight, int cellWidth, int cellHeight)
	{
		for (int i = 0; i < FIELD_WIDTH; i++)
		{
			for (int j = 0; j < FIELD_HEIGHT; j++)
			{
				if (game.isCellEmpty(i, j))
					continue;
				
				//Get the value of the cell.
				int value = game.getCell(i, j);
				
				int x = i * cellWidth;
				int y = fieldHeight - cellHeight - (j * cellHeight);
				
				//Draw the pawn.
				Connect4GameDrawer.drawPawn(g, x, y, cellWidth, cellHeight, value);
				
				//Draw last move.
				int h = game.getHeight(game.getLastMove()) - 1;
				if (i == game.getLastMove() && j == h)
				{
					g.setColor(new Color(255, 255, 255, 100));
					g.fillOval(x + 25,   y + 25, cellWidth - 50, cellHeight - 50);
				}
			}
		}
	}
	
	private static void drawField(Graphics2D g, int fieldWidth, int fieldHeight, int cellWidth, int cellHeight)
	{
		//Draw the field.
		g.setColor(new Color(0x2475C9));
		g.fillRect(0, 0, fieldWidth, fieldHeight);
		g.setColor(new Color(0, 0, 0, 0));
		g.setComposite(AlphaComposite.Clear);
		for (int i = 0; i < FIELD_WIDTH; i++)
		{
			for (int j = 0; j < FIELD_HEIGHT; j++)
			{
				int x = i * cellWidth;
				int y = fieldHeight - cellHeight - (j * cellHeight);
				g.fillOval( x + 2,   y + 2, cellWidth - 4, cellHeight - 4);
			}
		}
		g.setComposite(AlphaComposite.SrcOver);
	}
	
	private static void drawPawn(Graphics2D g, int x, int y, int cellWidth, int cellHeight, int value)
	{
		//Draw the pawn.
		g.setColor(Connect4GameDrawer.pawnColor[value]);
		g.fillOval( x + 2,   y + 2, cellWidth - 4, cellHeight - 4);
		g.setColor(Connect4GameDrawer.darkPawnColor[value]);
		g.fillOval(x + 10,   y + 10, cellWidth - 20, cellHeight - 20);
	}
}
