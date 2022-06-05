package com.serpenssolida.discordbot.module.forum;

import java.util.HashMap;

public class ForumData
{
	private HashMap<String, Forum> forums;
	
	public ForumData(HashMap<String, Forum> forums)
	{
	    this.forums = forums;
	}
	
	public HashMap<String, Forum> getForums()
	{
		return this.forums;
	}
	
	public void setForums(HashMap<String, Forum> forums)
	{
		this.forums = forums;
	}
}
