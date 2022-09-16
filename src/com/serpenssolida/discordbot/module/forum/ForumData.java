package com.serpenssolida.discordbot.module.forum;

import java.util.Map;

public class ForumData
{
	private Map<String, Forum> forums;
	
	public ForumData(Map<String, Forum> forums)
	{
	    this.forums = forums;
	}
	
	public Map<String, Forum> getForums()
	{
		return this.forums;
	}
	
	public void setForums(Map<String, Forum> forums)
	{
		this.forums = forums;
	}
}
