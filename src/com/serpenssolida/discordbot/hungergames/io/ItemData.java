package com.serpenssolida.discordbot.hungergames.io;

import com.serpenssolida.discordbot.hungergames.inventory.Food;
import com.serpenssolida.discordbot.hungergames.inventory.Weapon;

import java.util.ArrayList;

public class ItemData
{
	private ArrayList<Food> foods = new ArrayList<>();
	private ArrayList<Weapon> weapons = new ArrayList<>();
	
	public ArrayList<Food> getFoods()
	{
		return this.foods;
	}
	
	public void setFoods(ArrayList<Food> foods)
	{
		this.foods = foods;
	}
	
	public ArrayList<Weapon> getWeapons()
	{
		return this.weapons;
	}
	
	public void setWeapons(ArrayList<Weapon> weapons)
	{
		this.weapons = weapons;
	}
}
