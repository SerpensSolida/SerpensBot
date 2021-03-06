package com.serpenssolida.discordbot.module.hungergames.event;

import com.serpenssolida.discordbot.RandomChoice;
import com.serpenssolida.discordbot.module.hungergames.AttackResult;
import com.serpenssolida.discordbot.module.hungergames.HungerGames;
import com.serpenssolida.discordbot.module.hungergames.Player;
import com.serpenssolida.discordbot.module.hungergames.inventory.Inventory;
import com.serpenssolida.discordbot.module.hungergames.inventory.Item;

import java.util.ArrayList;
import java.util.HashSet;

public class BattleEvent extends HungerGamesEvent
{
	public EventResult doEvent(HungerGames hg)
	{
		
		StringBuilder stringBuilder = new StringBuilder();
		HashSet<Player> alivePlayers = hg.getAlivePlayers();
		HashSet<Player> deadPlayers = hg.getDeadPlayers();
		HashSet<Player> involvedPlayers = hg.getInvolvedPlayers();
		HashSet<Player> combatPlayers = hg.getCombatPlayers();
		
		//List of player that did not fight during this turn.
		HashSet<Player> availablePlayers = new HashSet<>(hg.getAlivePlayers());
		availablePlayers.removeAll(combatPlayers);
		
		if (availablePlayers.size() < 2)
			return new EventResult("", EventResult.State.Failed); //Quit event.
		
		//Chose a random player.
		Player player1 = (Player) RandomChoice.getRandom(availablePlayers.toArray());
		availablePlayers.remove(player1);
		
		//Get the target category where the second player will be chosen from.
		HashSet<Player> targetSet = this.getRelationshipSet(player1, alivePlayers, availablePlayers);
		
		if (targetSet.isEmpty())
			return new EventResult("", EventResult.State.Failed); //Quit event.
		
		//Get a random player from the chosen category.
		Player player2 = (Player) RandomChoice.getRandom(targetSet.toArray());
		
		involvedPlayers.add(player1);
		combatPlayers.add(player1);
		involvedPlayers.add(player2);
		combatPlayers.add(player2);
		
		//Sort the player by speed.
		if (player2.getCharacter().getSpeed() > player1.getCharacter().getSpeed())
		{
			Player temp = player1;
			player1 = player2;
			player2 = temp;
		}
		else if (player2.getCharacter().getSpeed() == player1.getCharacter().getSpeed())
		{
			//If they have the same speed choose randomly.
			if (RandomChoice.random.nextBoolean())
			{
				Player temp = player1;
				player1 = player2;
				player2 = temp;
			}
		}
		
		if (player1.getFriends().contains(player2))
			stringBuilder.append("L'alleanza tra **" + player1 + "** e **" + player2 + "** è rotta.\n");
		
		//Make them enemies.
		player1.getFriends().remove(player2);
		player2.getFriends().remove(player1);
		player1.getEnemies().add(player2);
		player2.getEnemies().add(player1);
		
		AttackResult result = player1.attack(player2);
		stringBuilder.append("" + result + "\n");
		
		if (player2.isDead())
		{
			alivePlayers.remove(player2);
			deadPlayers.add(player2);
			
			player2.removeRelationships();
			player1.getCharacter().incrementKills();
			
			stringBuilder.append("**" + player2 + "** è morto.\n");
			
			if (RandomChoice.randomChance(10))
			{
				Item stolenItem = this.getRandomItemFromInventory(player2.getInventory());
				
				if (stolenItem != null)
				{
					player1.getInventory().addItem(stolenItem, 1);
					
					stringBuilder.append(String.format("**%s** ha trovato *%s* nel corpo di **%s**.\n", player1, stolenItem, player2));
				}
			}
			
			return new EventResult(stringBuilder.toString(), EventResult.State.Successful);
		}
		
		result = player2.attack(player1);
		stringBuilder.append("" + result + "\n");
		
		if (player1.isDead())
		{
			alivePlayers.remove(player1);
			deadPlayers.add(player1);
			
			player1.removeRelationships();
			player2.getCharacter().incrementKills();
			
			stringBuilder.append("**" + player1 + "** è morto.\n");
			
			if (RandomChoice.randomChance(10))
			{
				Item stolenItem = this.getRandomItemFromInventory(player1.getInventory());
				
				if (stolenItem != null)
				{
					player2.getInventory().addItem(stolenItem, 1);
					
					stringBuilder.append(String.format("**%s** ha trovato *%s* nel corpo di **%s**.\n", player2, stolenItem, player1));
				}
			}
		}
		
		return new EventResult(stringBuilder.toString(), EventResult.State.Successful);
	}
	
	private HashSet<Player> getRelationshipSet(Player player, HashSet<Player> alivePlayers, HashSet<Player> availablePlayers)
	{
		//List of alive players that are neutral to the player.
		HashSet<Player> neutralAlivePlayers = new HashSet<>(alivePlayers);
		neutralAlivePlayers.remove(player);
		neutralAlivePlayers.removeAll(player.getFriends());
		neutralAlivePlayers.removeAll(player.getEnemies());
		
		//List of available player that are neutral to the player.
		HashSet<Player> neutralPlayers = new HashSet<>(availablePlayers);
		neutralPlayers.removeAll(player.getFriends());
		neutralPlayers.removeAll(player.getEnemies());
		
		//List of available enemies of the player.
		HashSet<Player> enemyPlayers = new HashSet<>(availablePlayers);
		enemyPlayers.removeAll(neutralPlayers);
		enemyPlayers.removeAll(player.getFriends());
		
		//List of available friends of the player.
		HashSet<Player> friendsPlayer = new HashSet<>(availablePlayers);
		friendsPlayer.removeAll(neutralPlayers);
		friendsPlayer.removeAll(player.getEnemies());
		
		HashSet<Player> chosenCategory = new HashSet<>();
		
		//Chose a category.
		if (!player.getEnemies().isEmpty())
		{
			chosenCategory = enemyPlayers;
		}
		else if (!neutralAlivePlayers.isEmpty())
		{
			chosenCategory = neutralPlayers;
		}
		else if (!player.getFriends().isEmpty())
		{
			chosenCategory = friendsPlayer;
		}
		
		return chosenCategory;
	}
	
	private Item getRandomItemFromInventory(Inventory inventory)
	{
		ArrayList<Item> items = inventory.getItems();
		Item item = null;
		
		if (!items.isEmpty())
		{
			item = (Item) RandomChoice.getRandom(items.toArray());
		}
		
		return item;
	}
}
