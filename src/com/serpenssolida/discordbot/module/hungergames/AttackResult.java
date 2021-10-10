package com.serpenssolida.discordbot.module.hungergames;

import com.serpenssolida.discordbot.module.hungergames.inventory.Weapon;

/**
 * Data of the result of an attack a player did to another.
 */
public class AttackResult
{
	private final Player user;
	private final Player receiver;
	private float inflictedDamage;
	private Weapon weapon;
	
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
		attackString = attackString.replace("user", "**" + this.user + "**");
		attackString = attackString.replace("receiver", "**" + this.receiver + "**");
		attackString = attackString.replace("weapon", "*" + this.weapon.getName() + "*");
		attackString = attackString.replace("damage", String.valueOf((int) this.inflictedDamage));
		return attackString;
	}
}
