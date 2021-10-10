package com.serpenssolida.discordbot.module.hungergames;

import com.serpenssolida.discordbot.RandomChoice;
import com.serpenssolida.discordbot.module.hungergames.inventory.Food;
import com.serpenssolida.discordbot.module.hungergames.inventory.Weapon;
import com.serpenssolida.discordbot.module.hungergames.io.ItemData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

public class HungerGames
{
	private final String guildID;
	private ItemData itemData; //Collection of items of the HungerGames.
	private HashSet<Weapon> weaponPool; //Pool of weapons that can be found during the HungerGames.
	private int day; //Current day of the HungerGames.
	private HashSet<Player> alivePlayers; //List of alive players.
	private HashSet<Player> deadPlayers = new HashSet<>(); //List of dead players.
	private HashSet<Player> involvedPlayers = new HashSet<>(); //List of player active during the turn.
	private HashSet<Player> combatPlayers = new HashSet<>(); //List of players that fought during the turn.
	private HashSet<Player> healedPlayers = new HashSet<>(); //List of players that healed during the turn.
	private HashSet<Player> foundItemPlayers = new HashSet<>(); //List of players that found an item during the turn.
	private HashSet<Player> incidentPlayers = new HashSet<>(); //List of players that had an incident during the turm.
	private HashSet<Player> sleepPlayers = new HashSet<>(); //List of players that slept during the turn.
	
	public HungerGames(String guildID, ItemData itemData)
	{
		this.guildID = guildID;
		this.itemData = itemData;
		this.weaponPool = new HashSet<>(this.itemData.getWeapons());
		this.alivePlayers = this.generatePlayers();
		this.day = 1;
	}
	
	/**
	 * Generate a list of players with starting inventory that will partecipate to the HungerGames.
	 *
	 * @return The list of players.
	 */
	public HashSet<Player> generatePlayers()
	{
		HashSet<Player> players = new HashSet<>();
		
		for (Map.Entry<String, Character> characterEntry : HungerGamesController.getCharacters(this.guildID).entrySet())
		{
			Character character = characterEntry.getValue();
			
			if (character.isEnabled())
			{
				Player player = new Player(character);
				players.add(player);
				
				this.generateInventory(player);
			}
		}
		
		return players;
	}
	
	/**
	 * Generate the inventory of the given player.
	 *
	 * @param player
	 * 		The player whose inventory is to be generated.
	 */
	public void generateInventory(Player player)
	{
		int foodNum = RandomChoice.random.nextInt(3);
		int weaponNum = RandomChoice.random.nextInt(3);
		
		for (int i = 0; i < foodNum; i++)
		{
			player.getInventory().addItem(this.getRandomFood(), 1);
		}
		
		for (int i = 0; i < weaponNum; i++)
		{
			player.getInventory().addItem(this.getRandomWeapon(), 1);
		}
	}
	
	/**
	 * @return A random food from the food list.
	 */
	public Food getRandomFood()
	{
		ArrayList<Food> foods = this.getItemData().getFoods();
		float[] probabilities = new float[foods.size()];
		
		for (int i = 0; i < foods.size(); i++)
		{
			probabilities[i] = 100.0F - foods.get(i).getHpRestored();
		}
		
		return (Food) RandomChoice.getRandomWithProbability(foods.toArray(), probabilities);
	}
	
	/**
	 * @return A random weapon from the item pool.
	 */
	public Weapon getRandomWeapon()
	{
		HashSet<Weapon> pool = this.getWeaponPool();
		
		float[] probabilities = new float[pool.size()];
		int i = 0;
		
		for (Weapon weapon : pool)
		{
			probabilities[i] = (100 - weapon.getDamage());
			i++;
		}
		
		Weapon choosenWeapon = (Weapon) RandomChoice.getRandomWithProbability(pool.toArray(), probabilities);
		pool.remove(choosenWeapon);
		return choosenWeapon;
	}
	
	public ItemData getItemData()
	{
		return this.itemData;
	}
	
	public void setItemData(ItemData itemData)
	{
		this.itemData = itemData;
	}
	
	public HashSet<Weapon> getWeaponPool()
	{
		return this.weaponPool;
	}
	
	public void setWeaponPool(HashSet<Weapon> weaponPool)
	{
		this.weaponPool = weaponPool;
	}
	
	public HashSet<Player> getAlivePlayers()
	{
		return this.alivePlayers;
	}
	
	public void setAlivePlayers(HashSet<Player> alivePlayers)
	{
		this.alivePlayers = alivePlayers;
	}
	
	public int getDay()
	{
		return this.day;
	}
	
	public void setDay(int day)
	{
		this.day = day;
	}
	
	public void incrementDay()
	{
		this.day++;
	}
	
	public HashSet<Player> getDeadPlayers()
	{
		return this.deadPlayers;
	}
	
	public void setDeadPlayers(HashSet<Player> deadPlayers)
	{
		this.deadPlayers = deadPlayers;
	}
	
	public HashSet<Player> getInvolvedPlayers()
	{
		return this.involvedPlayers;
	}
	
	public void setInvolvedPlayers(HashSet<Player> involvedPlayers)
	{
		this.involvedPlayers = involvedPlayers;
	}
	
	public HashSet<Player> getCombatPlayers()
	{
		return this.combatPlayers;
	}
	
	public void setCombatPlayers(HashSet<Player> combatPlayers)
	{
		this.combatPlayers = combatPlayers;
	}
	
	public HashSet<Player> getHealedPlayers()
	{
		return this.healedPlayers;
	}
	
	public void setHealedPlayers(HashSet<Player> healedPlayers)
	{
		this.healedPlayers = healedPlayers;
	}
	
	public HashSet<Player> getFoundItemPlayers()
	{
		return this.foundItemPlayers;
	}
	
	public void setFoundItemPlayers(HashSet<Player> foundItemPlayers)
	{
		this.foundItemPlayers = foundItemPlayers;
	}
	
	public HashSet<Player> getIncidentPlayers()
	{
		return this.incidentPlayers;
	}
	
	public void setIncidentPlayers(HashSet<Player> incidentPlayers)
	{
		this.incidentPlayers = incidentPlayers;
	}
	
	public HashSet<Player> getSleepPlayers()
	{
		return this.sleepPlayers;
	}
	
	public void setSleepPlayers(HashSet<Player> sleepPlayers)
	{
		this.sleepPlayers = sleepPlayers;
	}
	
	public String getGuildID()
	{
		return this.guildID;
	}
}
