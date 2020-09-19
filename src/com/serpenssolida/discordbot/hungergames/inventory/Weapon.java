package com.serpenssolida.discordbot.hungergames.inventory;

import java.io.Serializable;

public class Weapon extends Item
{
	private int damage;
	private int chance;
	private WeaponType type;
	
	public int getDamage()
	{
		return this.damage;
	}
	
	public void setDamage(int damage)
	{
		this.damage = damage;
	}
	
	public int getChance()
	{
		return this.chance;
	}
	
	public void setChance(int chance)
	{
		this.chance = chance;
	}
	
	public WeaponType getType()
	{
		return this.type;
	}
	
	public void setType(WeaponType type)
	{
		this.type = type;
	}
	
	public enum WeaponType implements Serializable
	{
		Strength, Ability, Special
	}
	
	public Weapon(String name, int damage, int chance, WeaponType type)
	{
		this.name = name;
		this.damage = damage;
		this.setChance(chance);
		this.setType(type);
		this.consumable = false;
	}
}
