package com.serpenssolida.discordbot.hungergames.inventory;

public class Item
{
	protected String name;
	private String useMessage;
	protected boolean consumable;
	
	public String getName()
	{
		return this.name;
	}
	
	public String getUseMessage()
	{
		return this.useMessage;
	}
	
	public void setUseMessage(String useMessage)
	{
		this.useMessage = useMessage;
	}
	
	public String toString()
	{
		return this.getName();
	}
}
