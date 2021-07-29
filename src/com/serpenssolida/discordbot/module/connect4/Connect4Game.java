package com.serpenssolida.discordbot.module.connect4;

import net.dv8tion.jda.api.entities.User;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

public class Connect4Game
{
	private int[][] field = new int[FIELD_WIDTH][FIELD_HEIGHT];
	private String messageId;
	private User[] players = new User[2];
	private User winner;
	private int currentTurn = 0;
	private boolean interrupted = false;
	private boolean finished = false;
	
	public static int FIELD_WIDTH = 7;
	public static int FIELD_HEIGHT = 6;
	
	public Connect4Game(User player1, User player2)
	{
		this.players[0] = player1;
		this.players[1] = player2;
		
		//Fill all values with -1.
		for (int[] row : this.field)
		{
			Arrays.fill(row, -1);
		}
	}
	
	public boolean checkMove(int turn, int x, int y)
	{
		//Direction for the check.
		int[][] directions =
				{
						{1, 0}, {1, 1}, {0, 1}, {-1, 1}
				};
		
		for (int direction = 0; direction < directions.length; direction++) //Foreach direction
		{
			for (int offset = 0; offset < 4; offset++) //4 checks for each direction
			{
				int count = 0;
				for (int i = 0; i < 4; i++) //Check if there is 4 in a row.
				{
					int posX = (i - offset) * directions[direction][0];
					int posY = (i - offset) * directions[direction][1];
					
					int value;
					
					//Get the value from the field.
					try
					{
						value = this.field[x + posX][y + posY];
					}
					catch (IndexOutOfBoundsException e)
					{
						break;
					}
					
					if (value == turn)
						count++;
					else
						break;
				}
				
				//Check if there was 4 in a row.
				if (count == 4)
					return true;
			}
		}
		
		return false;
	}
	
	public ByteArrayOutputStream generateFieldImage()
	{
		//BufferedImage tombImage = this.getTombImage(); //Tombstone image.
		
		//Calculate image dimensions.
		int fieldWidth = 512;
		int fieldHeight = (fieldWidth * FIELD_HEIGHT / FIELD_WIDTH);
		
		int imageWidth = fieldWidth;
		int imageHeight = fieldHeight + 64;
		
		int cellWidth = fieldWidth / FIELD_WIDTH;
		int cellHeight = fieldHeight / FIELD_HEIGHT;
		
		//Create the image and get its Graphics.
		BufferedImage statusImage = new BufferedImage(imageWidth, imageHeight, 6);
		Graphics2D g = (Graphics2D) statusImage.getGraphics();

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
		
		//Draw pawns.
		Color[] pawnColor = {new Color(0xE43613), new Color(0xEFCC1C)};
		Color[] darkPawnColor = {new Color(0x952F15), new Color(0x95771A)};
		for (int i = 0; i < FIELD_WIDTH; i++)
		{
			for (int j = 0; j < FIELD_HEIGHT; j++)
			{
				if (this.isCellEmpty(i, j))
					continue;
				
				//Get the value of the cell.
				int value = this.getCell(i, j);
				
				//Draw the pawn.
				int x = i * cellWidth;
				int y = fieldHeight - cellHeight - (j * cellHeight);
				g.setColor(pawnColor[value]);
				g.fillOval( x + 2,   y + 2, cellWidth - 4, cellHeight - 4);
				g.setColor(darkPawnColor[value]);
				g.fillOval(x + 10,   y + 10, cellWidth - 20, cellHeight - 20);
			}
		}
		
		//Draw number guide.
		for (int i = 0; i < FIELD_WIDTH; i++)
		{
			g.setFont(g.getFont().deriveFont(32f));
			g.setColor(Color.WHITE);
			int w = g.getFontMetrics().stringWidth("" + (i + 1));
			g.drawString("" + (i + 1), cellWidth / 2 + i * cellWidth - w / 2, fieldHeight + cellHeight / 2);
		}
		
		ByteArrayOutputStream outputStream;
		try
		{
			outputStream = new ByteArrayOutputStream();
			ImageIO.write(statusImage, "png", outputStream);
		}
		catch (IOException e)
		{
			outputStream = null;
			e.printStackTrace();
		}
		
		return outputStream;
	}
	
	public void setCell(int playerIndex, int x, int y)
	{
		if (!this.isCellEmpty(x, y))
			return;
		
		this.field[x][y] = playerIndex;
		
		return;
	}
	
	public int getHeight(int x)
	{
		int height = -1;
		
		for (int i = 0; i < FIELD_HEIGHT; i++)
		{
			height++;
			
			if (this.isCellEmpty(x, i))
				break;
		}
		
		return height;
	}
	
	public int getCell(int x, int y)
	{
		if (x < 0 && x >= Connect4Game.FIELD_WIDTH && y < 0 && y >= Connect4Game.FIELD_HEIGHT)
			return -1;
		
		return this.field[x][y];
	}
	
	public User getPlayer(int playerIndex)
	{
		if (playerIndex > this.players.length || playerIndex < 0)
			return null;
		
		return this.players[playerIndex];
	}
	
	public HashSet<User> getPlayers()
	{
		return new HashSet<>(Arrays.asList(this.players));
	}
	
	public boolean isCellEmpty(int x, int y)
	{
		return this.field[x][y] == -1;
	}
	
	public User getCurrentUser()
	{
		return this.players[this.currentTurn];
	}
	
	public boolean isFieldFull()
	{
		for (int[] row : this.field)
		{
			for (int cell : row)
			{
				if (cell == -1)
					return false;
			}
		}
		
		return true;
	}
	
	public void incrementTurn()
	{
		if (this.currentTurn == 0)
			this.currentTurn = 1;
		else
			this.currentTurn = 0;
	}
	
	public int[][] getField()
	{
		return this.field;
	}
	
	public String getMessageId()
	{
		return this.messageId;
	}
	
	public void setMessageId(String messageId)
	{
		this.messageId = messageId;
	}
	
	public int getCurrentTurn()
	{
		return this.currentTurn;
	}
	
	public void setCurrentTurn(int currentTurn)
	{
		this.currentTurn = currentTurn;
	}
	
	public boolean isInterrupted()
	{
		return this.interrupted;
	}
	
	public void setInterrupted(boolean interrupted)
	{
		this.interrupted = interrupted;
	}
	
	public boolean isFinished()
	{
		return this.finished;
	}
	
	public void setFinished(boolean finished)
	{
		this.finished = finished;
	}
	
	public User getWinner()
	{
		return this.winner;
	}
	
	public void setWinner(User winner)
	{
		this.winner = winner;
	}
}