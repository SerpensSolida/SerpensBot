package com.serpenssolida.discordbot.hungergames.inventory;

public class Food extends Item
{
	private float hpRestored;
	
	public Food(float hpRestored, String name)
	{
		this.hpRestored = hpRestored;
		this.name = name;
	}
	
	public float getHpRestored()
	{
		return this.hpRestored;
	}
}
