package com.serpenssolida.discordbot.module.hungergames.event;

import com.serpenssolida.discordbot.RandomChoice;
import com.serpenssolida.discordbot.module.hungergames.AttackResult;
import com.serpenssolida.discordbot.module.hungergames.HungerGames;
import com.serpenssolida.discordbot.module.hungergames.Player;

import java.util.HashSet;

public class DuelEvent implements BattleEvent
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
			if (RandomChoice.getRandom().nextBoolean())
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
			stringBuilder.append(this.onPlayerKilled(player1, player2, alivePlayers, deadPlayers));
			
			return new EventResult(stringBuilder.toString(), EventResult.State.SUCCESSFUL);
		}
		
		result = player2.attack(player1);
		stringBuilder.append("" + result + "\n");
		
		if (player1.isDead())
			stringBuilder.append(this.onPlayerKilled(player2, player1, alivePlayers, deadPlayers));
		
		return new EventResult(stringBuilder.toString(), EventResult.State.SUCCESSFUL);
	}
}
