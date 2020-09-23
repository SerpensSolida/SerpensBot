package com.serpenssolida.discordbot.module.hungergames.io;

import java.util.ArrayList;

public class HungerGamesData
{
	private int hgCount;
	private long messageSpeed;
	private ArrayList<String> winners;
	
	public ArrayList<String> getWinners()
	{
		return this.winners;
	}
	
	public void setWinners(ArrayList<String> winners)
	{
		this.winners = winners;
	}
	
	public int getHgCount()
	{
		return this.hgCount;
	}
	
	public void setHgCount(int hgCount)
	{
		this.hgCount = hgCount;
	}
	
	public long getMessageSpeed()
	{
		return this.messageSpeed;
	}
	
	public void setMessageSpeed(long messageSpeed)
	{
		this.messageSpeed = messageSpeed;
	}
}
