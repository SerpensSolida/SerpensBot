package com.serpenssolida.discordbot.module.hungergames.inventory;

public class Food extends Item
{
	private final float hpRestored;
	
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
