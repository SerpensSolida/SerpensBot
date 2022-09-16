package com.serpenssolida.discordbot.module.connect4;

import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Connect4Game
{
	private final int[][] field = new int[FIELD_WIDTH][FIELD_HEIGHT];
	private final User[] players = new User[2];
	private String messageId;
	private User winner;
	private int currentTurn = 0;
	private int lastMove;
	private boolean interrupted = false;
	private boolean finished = false;
	
	public static final int FIELD_WIDTH = 7;
	public static final int FIELD_HEIGHT = 6;
	
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
	
	/**
	 * Checks if the move in the given position is a winning move.
	 *
	 * @param turn
	 * 		The turn the game is at.
	 * @param x
	 * 		The x position of the move.
	 * @param y
	 * 		The y position of the move.
	 *
	 * @return
	 * 		True if the move is a winning move, false otherwise.
	 */
	public boolean checkMove(int turn, int x, int y)
	{
		//Direction for the check.
		Point[] directions =
				{
						new Point(1, 0), //Horizontal.
						new Point(1, 1), //Diagonal.
						new Point(0, 1), //Vertical.
						new Point(-1, 1) //Anti diagonal.
				};
		
		//Foreach direction
		for (Point direction : directions)
		{
			for (int offset = 0; offset < 4; offset++) //4 checks for each direction
			{
				int count = 0;
				for (int i = 0; i < 4; i++) //Check if there is 4 in a row.
				{
					int posX = (i - offset) * direction.x;
					int posY = (i - offset) * direction.y;
					
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
				
				//Check if there is 4 in a row.
				if (count == 4)
					return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Places the given player pawn in the given cell.
	 *
	 * @param playerIndex
	 * 		The index of the player that made the move.
	 * @param x
	 * 		The x position of the cell.
	 * @param y
	 * 		The y position of the cell.
	 */
	public void setCell(int playerIndex, int x, int y)
	{
		if (!this.isCellEmpty(x, y))
			return;
		
		this.field[x][y] = playerIndex;
	}
	
	/**
	 * Gets the height of the last empty space in the column.
	 *
	 * @param x
	 * 		The x position of the column.
	 *
	 * @return
	 * 		The height of the last empty space in the column.
	 */
	public int getHeight(int x)
	{
		int height = -1;
		
		for (int i = 0; i < FIELD_HEIGHT; i++)
		{
			height++;
			
			if (this.isCellEmpty(x, i))
				return height;
		}
		
		return FIELD_HEIGHT;
	}
	
	/**
	 * Gets the value inside the given cell.
	 *
	 * @param x
	 * 		The x position of the cell.
	 * @param y
	 * 		The y position of the cell.
	 *
	 * @return
	 * 		The value inside the given cell.
	 */
	public int getCell(int x, int y)
	{
		if (x < 0 || x >= Connect4Game.FIELD_WIDTH || y < 0 || y >= Connect4Game.FIELD_HEIGHT)
			return -1;
		
		return this.field[x][y];
	}
	
	/**
	 * Gets the User with the given index in the game.
	 *
	 * @param playerIndex
	 *		The index of the user.
	 *
	 * @return
	 * 		The Discord User with the given index in the game.
	 */
	public User getPlayer(int playerIndex)
	{
		if (playerIndex > this.players.length || playerIndex < 0)
			return null;
		
		return this.players[playerIndex];
	}
	
	/**
	 * Gets the Users that are playing the game.
	 *
	 * @return
	 * 		An HashSet of User containing the users that are playing the game.
	 */
	public Set<User> getPlayers()
	{
		return new HashSet<>(Arrays.asList(this.players));
	}
	
	/**
	 * Checks if the given cell is empty.
	 *
	 * @param x
	 * 		The x position of the cell.
	 * @param y
	 * 		The y position of the cell.
	 *
	 * @return
	 * 		True if the cell is empty, false otherwise.
	 */
	public boolean isCellEmpty(int x, int y)
	{
		return this.field[x][y] == -1;
	}
	
	/**
	 * Gets the user that is currently playing.
	 *
	 * @return
	 * 		The Discord User that is currently playing.
	 */
	public User getCurrentUser()
	{
		return this.players[this.currentTurn];
	}
	
	/**
	 * Checks if the field is completely full.
	 *
	 * @return
	 * 		True if the field is full, false otherwise.
	 */
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
	
	/**
	 * Increment the game turn counter.
	 */
	public void incrementTurn()
	{
		if (this.currentTurn == 0)
			this.currentTurn = 1;
		else
			this.currentTurn = 0;
	}
	
	public String getMessageId()
	{
		return this.messageId;
	}
	
	void setMessageId(String messageId)
	{
		this.messageId = messageId;
	}
	
	public int getCurrentTurn()
	{
		return this.currentTurn;
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
	
	public int getLastMove()
	{
		return this.lastMove;
	}
	
	public void setLastMove(int lastMove)
	{
		this.lastMove = lastMove;
	}
}
