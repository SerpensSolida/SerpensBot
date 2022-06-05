package com.serpenssolida.discordbot.module.forum;

public final class Forum
{
	private final String title;
	private final String description;
	private final String buttonLabel;
	private final String guildID;
	private final String channelID;
	private String messageID;
	
	public Forum(String title, String description, String buttonLabel, String guildID, String channelID)
	{
		this.title = title;
		this.description = description;
		this.guildID = guildID;
		this.buttonLabel = buttonLabel;
		this.channelID = channelID;
	}
	
	public String getTitle()
	{
		return this.title;
	}
	
	public String getDescription()
	{
		return this.description;
	}
	
	public String getButtonLabel()
	{
		return this.buttonLabel;
	}
	
	public String getGuildID()
	{
		return this.guildID;
	}
	
	public String getChannelID()
	{
		return this.channelID;
	}
	
	public String getMessageID()
	{
		return this.messageID;
	}
	
	public void setMessageID(String messageID)
	{
		this.messageID = messageID;
	}
}
