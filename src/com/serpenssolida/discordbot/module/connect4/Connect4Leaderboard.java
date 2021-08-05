package com.serpenssolida.discordbot.module.connect4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Connect4Leaderboard
{
	HashMap<String, Connect4UserData> userData;
	
	public HashMap<String, Connect4UserData> getUserData()
	{
		if (this.userData == null)
			this.userData = new HashMap<>();
		
		return this.userData;
	}
	
	public void setUserData(HashMap<String, Connect4UserData> userData)
	{
		this.userData = userData;
	}
	
	public ArrayList<Map.Entry<String, Connect4UserData>> getDataAsList()
	{
		return new ArrayList<>(this.getUserData().entrySet());
	}
	
}
