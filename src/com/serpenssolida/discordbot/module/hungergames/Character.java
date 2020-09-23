package com.serpenssolida.discordbot.module.hungergames;

import java.io.Serializable;

public class Character implements Serializable
{
	//Characteristic of the character.
	private int vitality;
	private int strength;
	private int ability;
	private int special;
	private int speed;
	private int endurance;
	private int taste;
	
	private String name; //Name of the character.
	private String ownerID; //ID of the user that own this character.
	private int wins = 0; //Number of times the character won the HungerGames.
	private int kills = 0; //Number of kills
	private boolean enabled = true; //If the player participate to the HungerGames or no.
	
	public Character(String ownerID)
	{
		this.ownerID = ownerID;
	}
	
	public void setStats(int[] stats)
	{
		this.setVitality(stats[0]);
		this.setStrength(stats[1]);
		this.setAbility(stats[2]);
		this.setSpecial(stats[3]);
		this.setSpeed(stats[4]);
		this.setEndurance(stats[5]);
		this.setTaste(stats[6]);
	}
	
	public int getVitality()
	{
		return this.vitality;
	}
	
	public void setVitality(int vitality)
	{
		this.vitality = vitality;
	}
	
	public int getStrength()
	{
		return this.strength;
	}
	
	public void setStrength(int strength)
	{
		this.strength = strength;
	}
	
	public int getAbility()
	{
		return this.ability;
	}
	
	public void setAbility(int ability)
	{
		this.ability = ability;
	}
	
	public int getSpecial()
	{
		return this.special;
	}
	
	public void setSpecial(int special)
	{
		this.special = special;
	}
	
	public int getSpeed()
	{
		return this.speed;
	}
	
	public void setSpeed(int speed)
	{
		this.speed = speed;
	}
	
	public int getEndurance()
	{
		return this.endurance;
	}
	
	public void setEndurance(int endurance)
	{
		this.endurance = endurance;
	}
	
	public int getTaste()
	{
		return this.taste;
	}
	
	public void setTaste(int taste)
	{
		this.taste = taste;
	}
	
	public String getDisplayName()
	{
		return this.name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return this.name.toLowerCase();
	}
	
	public String getOwnerID()
	{
		return this.ownerID;
	}
	
	public void setOwnerID(String ownerID)
	{
		this.ownerID = ownerID;
	}
	
	public int getWins()
	{
		return this.wins;
	}
	
	public void setWins(int wins)
	{
		this.wins = wins;
	}
	
	public void incrementWins()
	{
		this.wins++;
	}
	
	public int getKills()
	{
		return this.kills;
	}
	
	public void setKills(int kills)
	{
		this.kills = kills;
	}
	
	public void incrementKills()
	{
		this.kills++;
	}
	
	public boolean isEnabled()
	{
		return this.enabled;
	}
	
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}
}
