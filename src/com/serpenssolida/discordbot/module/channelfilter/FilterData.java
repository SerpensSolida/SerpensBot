package com.serpenssolida.discordbot.module.channelfilter;

public class FilterData
{
	private boolean requiresImages = false;
	private boolean requiresLinks = false;
	
	public boolean getRequiresImages()
	{
		return this.requiresImages;
	}
	
	public void setRequiresImages(boolean requiresImages)
	{
		this.requiresImages = requiresImages;
	}
	
	public boolean getRequiresLinks()
	{
		return this.requiresLinks;
	}
	
	public void setRequiresLinks(boolean requiresLinks)
	{
		this.requiresLinks = requiresLinks;
	}
}
