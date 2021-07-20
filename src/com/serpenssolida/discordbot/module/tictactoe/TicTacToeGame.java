package com.serpenssolida.discordbot.module.tictactoe;

import net.dv8tion.jda.api.entities.User;

import java.util.Arrays;
import java.util.HashSet;

public class TicTacToeGame
{
	private String messageId;
	private int[][] field = new int[3][3];
	private User[] players = new User[2];
	private int currentTurn = 0;
	private boolean interrupted = false;

	public static int FIELD_WIDHT = 3;
	public static int FIELD_HEIGHT = 3;
	
	public TicTacToeGame(User player1, User player2)
	{
		this.players[0] = player1;
		this.players[1] = player2;
		
		//Fill all values with -1.
		for (int[] row : this.field)
		{
			Arrays.fill(row, -1);
		}
	}
	
	public void setCell(int playerIndex, int x, int y)
	{
		if (this.isCellEmpty(x, y))
			this.field[x][y] = playerIndex;
	}
	
	public boolean isCellEmpty(int x, int y)
	{
		return this.field[x][y] == -1;
	}
	
	public User getCurrentUser()
	{
		return this.players[this.currentTurn];
	}
	
	public boolean isFinished()
	{
		//Rows
		for (int i = 0; i < FIELD_HEIGHT; i++)
		{
			if (this.field[0][i] == this.field[1][i] && this.field[1][i] == this.field[2][i] && this.field[0][i] != -1)
				return true;
		}
		
		//Column
		for (int i = 0; i < FIELD_WIDHT; i++)
		{
			if (this.field[i][0] == this.field[i][1] && this.field[i][1] == this.field[i][2] && this.field[i][0] != -1)
				return true;
		}
		
		//Main diagonal
		if (this.field[0][0] == this.field[1][1] && this.field[1][1] == this.field[2][2] && this.field[0][0] != -1)
			return true;
		
		//Secondary diagonal
		if (this.field[0][2] == this.field[1][1] && this.field[1][1] == this.field[2][0] && this.field[0][2] != -1)
			return true;
		
		return this.isFieldFull();
	}
	
	public User getWinner()
	{
		int index = this.getWinnerIndex();
		
		if (index == -1)
			return null;
		
		return this.getPlayer(index);
	}
	
	public int getWinnerIndex()
	{
		//Rows
		for (int i = 0; i < FIELD_HEIGHT; i++)
		{
			if (this.field[0][i] == this.field[1][i] && this.field[1][i] == this.field[2][i] && this.field[0][i] != -1)
				return this.field[0][i];
		}
		
		//Column
		for (int i = 0; i < FIELD_WIDHT; i++)
		{
			if (this.field[i][0] == this.field[i][1] && this.field[i][1] == this.field[i][2] && this.field[i][0] != -1)
				return this.field[i][0];
		}
		
		//Main diagonal
		if (this.field[0][0] == this.field[1][1] && this.field[1][1] == this.field[2][2] && this.field[0][0] != -1)
			return this.field[0][0];
		
		//Secondary diagonal
		if (this.field[0][2] == this.field[1][1] && this.field[1][1] == this.field[2][0] && this.field[0][2] != -1)
			return this.field[0][2];
		
		return -1;
	}
	
	public User getPlayer(int playerIndex)
	{
		if (playerIndex > this.players.length || playerIndex < 0)
			return null;
		
		return this.players[playerIndex];
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
	
	public int[][] getField()
	{
		return this.field;
	}
	
	public HashSet<User> getPlayers()
	{
		return new HashSet<>(Arrays.asList(this.players));
	}
	
	public int getCurrentTurn()
	{
		return this.currentTurn;
	}
	
	public void setCurrentTurn(int currentTurn)
	{
		this.currentTurn = currentTurn;
	}
	
	public String getMessageId()
	{
		return this.messageId;
	}
	
	public void setMessageId(String messageId)
	{
		this.messageId = messageId;
	}
	
	public int getCell(int i, int j)
	{
		return this.field[i][j];
	}
	
	public void incrementTurn()
	{
		if (this.currentTurn == 0)
			this.currentTurn = 1;
		else
			this.currentTurn = 0;
	}
	
	public boolean isInterrupted()
	{
		return this.interrupted;
	}
	
	public void setInterrupted(boolean interrupted)
	{
		this.interrupted = interrupted;
	}
}
