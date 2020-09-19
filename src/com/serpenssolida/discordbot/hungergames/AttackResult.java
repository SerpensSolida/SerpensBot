package com.serpenssolida.discordbot.hungergames;

import com.serpenssolida.discordbot.hungergames.inventory.Weapon;

/**
 * Data of the result of an attack a player did to another.
 */
public class AttackResult
{
	private float inflictedDamage;
	private Weapon weapon;
	private Player user;
	private Player receiver;
	
	public AttackResult(Player user, Player receiver, float inflictedDamage, Weapon weapon)
	{
		this.user = user;
		this.receiver = receiver;
		this.inflictedDamage = inflictedDamage;
		this.weapon = weapon;
	}
	
	public float getInflictedDamage()
	{
		return this.inflictedDamage;
	}
	
	public void setInflictedDamage(float inflictedDamage)
	{
		this.inflictedDamage = inflictedDamage;
	}
	
	public Weapon getWeapon()
	{
		return this.weapon;
	}
	
	public void setWeapon(Weapon weapon)
	{
		this.weapon = weapon;
	}
	
	public String toString()
	{
		String attackString = this.weapon.getUseMessage();
		attackString = attackString.replaceAll("user", "**" + this.user + "**");
		attackString = attackString.replaceAll("receiver", "**" + this.receiver + "**");
		attackString = attackString.replaceAll("weapon", "*" + this.weapon.getName() + "*");
		attackString = attackString.replaceAll("damage", String.valueOf((int) this.inflictedDamage));
		return attackString;
	}
}
