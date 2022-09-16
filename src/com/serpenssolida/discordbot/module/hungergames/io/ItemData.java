package com.serpenssolida.discordbot.module.hungergames.io;

import com.serpenssolida.discordbot.module.hungergames.inventory.Food;
import com.serpenssolida.discordbot.module.hungergames.inventory.Weapon;

import java.util.ArrayList;
import java.util.List;

public class ItemData
{
	private List<Food> foods = new ArrayList<>();
	private List<Weapon> weapons = new ArrayList<>();
	
	public List<Food> getFoods()
	{
		return this.foods;
	}
	
	public void setFoods(List<Food> foods)
	{
		this.foods = foods;
	}
	
	public List<Weapon> getWeapons()
	{
		return this.weapons;
	}
	
	public void setWeapons(List<Weapon> weapons)
	{
		this.weapons = weapons;
	}
}
