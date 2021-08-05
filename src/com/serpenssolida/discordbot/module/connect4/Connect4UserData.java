package com.serpenssolida.discordbot.module.connect4;

public class Connect4UserData
{
	private int wins;
	private int loses;
	private int draws;
	
	public int getWins()
	{
		return this.wins;
	}
	
	public void setWins(int wins)
	{
		this.wins = wins;
	}
	
	public int getLoses()
	{
		return this.loses;
	}
	
	public void setLoses(int loses)
	{
		this.loses = loses;
	}
	
	public int getDraws()
	{
		return this.draws;
	}
	
	public void setDraws(int draws)
	{
		this.draws = draws;
	}
}
