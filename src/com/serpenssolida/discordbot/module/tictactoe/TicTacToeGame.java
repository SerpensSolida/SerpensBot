package com.serpenssolida.discordbot.module.tictactoe;

import net.dv8tion.jda.api.entities.User;

import java.util.Arrays;
import java.util.HashSet;

public class TicTacToeGame
{
	private final int[][] field = new int[FIELD_SIZE][FIELD_SIZE];
	private final User[] players = new User[2];
	private String messageId;
	private User winner;
	private int currentTurn = 0;
	private boolean interrupted = false;
	private boolean finished = false;
	
	//Variable for win condition checking.
	private final int[] rowSum = new int[FIELD_SIZE];
	private final int[] columnSum = new int[FIELD_SIZE];
	private int diagonalSum = 0;
	private int antiDiagonalSum = 0;

	public static final int FIELD_SIZE = 3;
	
	public TicTacToeGame(User player1, User player2)
	{
		this.players[0] = player1;
		this.players[1] = player2;
		
		//Fill all values with -1.
		for (int[] row : this.field)
		{
			Arrays.fill(row, -1);
		}
		
		Arrays.fill(this.rowSum, 0);
		Arrays.fill(this.columnSum, 0);
	}
	
	public void setCell(int playerIndex, int x, int y)
	{
		if (this.isCellFull(x, y))
			return;
		
		this.field[x][y] = playerIndex;
		
		this.columnSum[x] += playerIndex;
		this.rowSum[y] += playerIndex;
		
		if (x == y)
			this.diagonalSum += playerIndex;
		
		if  (x == FIELD_SIZE - 1 - y)
			this.antiDiagonalSum += playerIndex;
	}
	
	public boolean isCellFull(int x, int y)
	{
		return this.field[x][y] != -1;
	}
	
	public User getCurrentUser()
	{
		return this.players[this.currentTurn];
	}
	
	public boolean checkMove(int turn, int x, int y)
	{
		if ((this.columnSum[x] == turn * 3 && this.isColumnFull(x)) || (this.rowSum[y] == turn * 3 && this.isRowFull(y)))
			return true;
		
		if (x == y && this.isDiagonalFull() && this.diagonalSum == turn * 3)
			return true;
		
		return x == FIELD_SIZE - 1 - y && this.isAntiDiagonalFull() && this.antiDiagonalSum == turn * 3;
	}
	
	public boolean isRowFull(int y)
	{
		for (int i = 0; i < FIELD_SIZE; i++)
		{
			if (this.field[i][y] == -1)
				return false;
		}
		
		return true;
	}
	
	public boolean isColumnFull(int x)
	{
		for (int i = 0; i < FIELD_SIZE; i++)
		{
			if (this.field[x][i] == -1)
				return false;
		}
		
		return true;
	}
	
	public boolean isDiagonalFull()
	{
		for (int i = 0; i < FIELD_SIZE; i++)
		{
			if (this.field[i][i] == -1)
				return false;
		}
		
		return true;
	}
	
	public boolean isAntiDiagonalFull()
	{
		for (int i = 0; i < FIELD_SIZE; i++)
		{
			if (this.field[i][FIELD_SIZE - 1 - i] == -1)
				return false;
		}
		
		return true;
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
