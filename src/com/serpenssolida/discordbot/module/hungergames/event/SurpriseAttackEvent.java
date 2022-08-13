package com.serpenssolida.discordbot.module.hungergames.event;

import com.serpenssolida.discordbot.RandomChoice;
import com.serpenssolida.discordbot.module.hungergames.AttackResult;
import com.serpenssolida.discordbot.module.hungergames.HungerGames;
import com.serpenssolida.discordbot.module.hungergames.Player;

import java.util.HashSet;

public class SurpriseAttackEvent implements BattleEvent
{
	private final String[] messages = new String[]
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
		StringBuilder stringBuilder = new StringBuilder();
		HashSet<Player> alivePlayers = hg.getAlivePlayers();
		HashSet<Player> deadPlayers = hg.getDeadPlayers();
		HashSet<Player> involvedPlayers = hg.getInvolvedPlayers();
		HashSet<Player> combatPlayers = hg.getCombatPlayers();
		
		//List of player that did not fight during this turn.
		HashSet<Player> availablePlayers = new HashSet<>(alivePlayers);
		availablePlayers.removeAll(combatPlayers);
		
		if (availablePlayers.size() < 2)
			return new EventResult("", EventResult.State.FAILED); //Quit event.
		
		//Chose a random player.
		Player player1 = (Player) RandomChoice.getRandom(availablePlayers.toArray());
		availablePlayers.remove(player1);
		
		//Get the target category where the second player will be chosen from.
		HashSet<Player> targetSet = this.getRelationshipSet(player1, alivePlayers, availablePlayers);
		
		if (targetSet.isEmpty())
			return new EventResult("", EventResult.State.FAILED); //Quit event.
		
		//Get a random player from the chosen category.
		Player player2 = (Player) RandomChoice.getRandom(targetSet.toArray());
		
		if (player1.getFriends().contains(player2))
			stringBuilder.append("L'alleanza tra **" + player1 + "** e **" + player2 + "** è rotta.\n");
		
		String message = (String) RandomChoice.getRandom(this.messages);
		message = message.replace("user", "**" + player1 + "**");
		message = message.replace("receiver", "**" + player2 + "**");
		stringBuilder.append(message);
		
		involvedPlayers.add(player1);
		combatPlayers.add(player1);
		involvedPlayers.add(player2);
		combatPlayers.add(player2);
		
		//Make them enemies.
		player1.getFriends().remove(player2);
		player2.getFriends().remove(player1);
		player1.getEnemies().add(player2);
		player2.getEnemies().add(player1);
		
		if (RandomChoice.randomChance(50))
		{
			stringBuilder.append(", ma fallisce.\n");
		}
		else
		{
			AttackResult result = player1.attack(player2, 1.5f);
			stringBuilder.append("\n" + result + "\n");
			
			
			if (player2.isDead())
				stringBuilder.append(this.onPlayerKilled(player1, player2, alivePlayers, deadPlayers));
		}
		
		return new EventResult(stringBuilder.toString(), EventResult.State.SUCCESSFUL);
	}
}
