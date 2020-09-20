package com.serpenssolida.discordbot;

import java.util.HashMap;

public class SettingsData
{
	private String commandSymbol;
	private HashMap<String, String> modulePrefixes = new HashMap<>();
	
	public HashMap<String, String> getModulePrefixes()
	{
		return this.modulePrefixes;
	}
	
	public void setModulePrefixes(HashMap<String, String> modulePrefixes)
	{
		this.modulePrefixes = modulePrefixes;
	}
	
	public String getCommandSymbol()
	{
		return this.commandSymbol;
	}
	
	public void setCommandSymbol(String commandSymbol)
	{
		this.commandSymbol = commandSymbol;
	}
}
