package com.serpenssolida.discordbot.module.channelfilter;

import java.util.HashMap;

public class GuildFilterData
{
	private final HashMap<String, FilterData> filters = new HashMap<>();
	
	public void setFilter(String channelID, FilterData filter)
	{
		this.filters.put(channelID, filter);
	}
	
	public FilterData getFilter(String channelID)
	{
		return this.filters.get(channelID);
	}
	
	public void removeFilter(String channelID)
	{
		this.filters.remove(channelID);
	}
}
