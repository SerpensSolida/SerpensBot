package com.serpenssolida.discordbot.hungergames.event;

import com.serpenssolida.discordbot.RandomChoice;
import com.serpenssolida.discordbot.hungergames.AttackResult;
import com.serpenssolida.discordbot.hungergames.HungerGames;
import com.serpenssolida.discordbot.hungergames.Player;

import java.util.HashSet;

public class SurpriseAttack extends HungerGamesEvent
{
	String[] messages = new String[]
			{
					"user prova a prendere di sorpresa receiver",
					"user prende alle spalle receiver",
					"user prova un attacco a sorpresa contro receiver",
					"user attacca receiver in un momento di distrazione",
					"user prova a cogliere di sopresa receiver",
					"user cerca di distrarre receiver"
			};
	
	public EventResult doEvent(HungerGames hg)
	{
		StringBuilder builder = new StringBuilder();
		HashSet<Player> alivePlayers = hg.getAlivePlayers();
		HashSet<Player> deadPlayers = hg.getDeadPlayers();
		HashSet<Player> involvedPlayers = hg.getInvolvedPlayers();
		HashSet<Player> combatPlayers = hg.getCombatPlayers();
		
		//List of player that did not fight during this turn.
		HashSet<Player> availablePlayers = new HashSet<>(alivePlayers);
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
		
		if (player1.getFriends().contains(player2))
			builder.append("L'alleanza tra **" + player1 + "** e **" + player2 + "** è rotta.\n");
		
		String message = (String) RandomChoice.getRandom(this.messages);
		message = message.replaceAll("user", "**" + player1 + "**");
		message = message.replaceAll("receiver", "**" + player2 + "**");
		builder.append(message);
		
		involvedPlayers.add(player1);
		combatPlayers.add(player1);
		involvedPlayers.add(player2);
		combatPlayers.add(player2);
		
		//Make them enemies.
		player1.getFriends().remove(player2);
		player2.getFriends().remove(player1);
		player1.getEnemies().add(player2);
		player2.getEnemies().add(player1);
		
		if (RandomChoice.random.nextInt(10) < 5)
		{
			builder.append(", ma fallisce.\n");
		}
		else
		{
			AttackResult result = player1.attack(player2, 1.5f);
			builder.append("\n" + result + "\n");
			
			if (player2.isDead())
			{
				alivePlayers.remove(player2);
				deadPlayers.add(player2);
				
				player2.removeRelationships();
				player1.getCharacter().incrementKills();
				
				builder.append("**" + player2 + "** è morto.\n");
			}
		}
		
		return new EventResult(builder.toString(), EventResult.State.Successful);
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
}
