package com.serpenssolida.discordbot.module.hungergames;

import com.serpenssolida.discordbot.RandomChoice;
import com.serpenssolida.discordbot.SerpensBot;
import com.serpenssolida.discordbot.module.hungergames.inventory.Inventory;
import com.serpenssolida.discordbot.module.hungergames.inventory.Weapon;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.HashSet;

public class Player
{
	private final Character character; //Character of the player.
	private final User owner; //User that own this player.
	private final Inventory inventory = new Inventory(); //Inventory of the player
	private final HashSet<Player> friends = new HashSet<>(); //List of friends of the player.
	private final HashSet<Player> enemies = new HashSet<>(); //List of enemies of the player.
	private float health; //Current health of the player.
	
	public Player(Character character)
	{
		this.character = character;
		this.health = this.getMaxHealth();
		this.owner = SerpensBot.api.retrieveUserById(this.character.getOwnerID()).complete();
	}
	
	public int getMaxHealth()
	{
		return this.character.getVitality() * 5 + 100;
	}
	
	public boolean isDead()
	{
		return (Math.round(this.health) <= 0);
	}
	
	public AttackResult attack(Player player2)
	{
		//Chose the weapon with the highest damage.
		Weapon weapon = this.getEquippedWeapon();
		
		//Get the weapon damage.
		float damage = this.getDamageWithWeapon(weapon);
		damage += damage * (RandomChoice.random.nextFloat() - 0.5f) * 0.2f;
		
		//Attack the player
		float trueDamage = player2.hit(damage);
		
		return new AttackResult(this, player2, trueDamage, weapon);
	}
	
	public AttackResult attack(Player player2, float multiplier)
	{
		//Chose the weapon with the highest damage.
		Weapon weapon = this.getEquippedWeapon();
		
		//Get the weapon damage.
		float baseDamage = this.getDamageWithWeapon(weapon);
		float damage = baseDamage * multiplier;
		damage += baseDamage * (RandomChoice.random.nextFloat() - 0.5F) * 0.2F;
		
		//Attack the player
		float trueDamage = player2.hit(damage);
		
		return new AttackResult(this, player2, trueDamage, weapon);
	}
	
	private float hit(float damage)
	{
		int endurance = this.getCharacter().getEndurance();
		float trueDamage = damage - damage * endurance / 30.0F;
		this.health -= trueDamage;
		
		return trueDamage;
	}
	
	/**
	 * Get the weapon which deal the greatest damage when wielded by the player.
	 *
	 * @return The weapon with the greatest dmg.
	 */
	public Weapon getEquippedWeapon()
	{
		ArrayList<Weapon> weapons = this.inventory.getWeapons();
		
		//If the player has no weapon return "fists"
		if (weapons.isEmpty())
		{
			Weapon weapon = new Weapon("Pugni", 15, 20, Weapon.WeaponType.Strength);
			weapon.setUseMessage("user tira un pugno a receiver togliendogli damageHP.");
			return weapon;
		}
		
		Player player = this;
		
		weapons.sort((weapon1, weapon2) ->
		{
			int dmg1 = Math.round(player.getDamageWithWeapon(weapon1));
			int dmg2 = Math.round(player.getDamageWithWeapon(weapon2));
			return dmg2 - dmg1;
		});
		
		return weapons.get(0);
	}
	
	/**
	 * Get the damage with the given weapon.
	 *
	 * @param weapon
	 * 		The weapon used to calculate the damage.
	 *
	 * @return The damgae dealt by the player when he wields the given weapon.
	 */
	private float getDamageWithWeapon(Weapon weapon)
	{
		int statValue = 0;
		
		switch (weapon.getType())
		{
			case Strength:
				statValue = this.character.getStrength();
				break;
			case Ability:
				statValue = this.character.getAbility();
				break;
			case Special:
				statValue = this.character.getSpecial();
				break;
		}
		
		float baseDamage = weapon.getDamage();
		return baseDamage + baseDamage * statValue / 30.0F;
	}
	
	/**
	 * Break all relationship of the player.
	 */
	public void removeRelationships()
	{
		for (Player enemy : this.getEnemies())
		{
			enemy.getEnemies().remove(this);
			enemy.getFriends().remove(this);
		}
		for (Player friend : this.getFriends())
		{
			friend.getEnemies().remove(this);
			friend.getFriends().remove(this);
		}
	}
	
	public User getOwner()
	{
		return this.owner;
	}
	
	public Character getCharacter()
	{
		return this.character;
	}
	
	public Inventory getInventory()
	{
		return this.inventory;
	}
	
	public float getHealth()
	{
		return this.health;
	}
	
	public void setHealth(float health)
	{
		this.health = health;
	}
	
	public HashSet<Player> getFriends()
	{
		return this.friends;
	}
	
	public HashSet<Player> getEnemies()
	{
		return this.enemies;
	}
	
	public String toString()
	{
		return this.getCharacter().getDisplayName();
	}
}
