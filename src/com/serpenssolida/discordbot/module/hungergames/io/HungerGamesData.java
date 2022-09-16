package com.serpenssolida.discordbot.module.hungergames.io;

import java.util.List;

public class HungerGamesData
{
	private int hgCount;
	private long messageSpeed;
	private List<String> winners;
	
	public List<String> getWinners()
	{
		return this.winners;
	}
	
	public void setWinners(List<String> winners)
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
