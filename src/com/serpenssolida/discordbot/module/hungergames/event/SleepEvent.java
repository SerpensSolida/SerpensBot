package com.serpenssolida.discordbot.module.hungergames.event;

import com.serpenssolida.discordbot.RandomChoice;
import com.serpenssolida.discordbot.module.hungergames.HungerGames;
import com.serpenssolida.discordbot.module.hungergames.Player;

import java.util.HashSet;
import java.util.Set;

public class SleepEvent implements HungerGamesEvent
{
	private final String[] messages = new String[]
			{
					"user non si regge più in piedi e decide di fare un sonnellino, user guadagna damageHP.",
					"user si fa un sonnellino recuperando damageHP.",
					"user si appisola su una pietra e recupera damageHP.",
					"user dorme sereno guadagnando damageHP.",
					"user si mette a dormire per terra. user recupera damageHP."
			};
	
	public EventResult doEvent(HungerGames hg)
	{
		StringBuilder stringBuilder = new StringBuilder();
		
		Set<Player> alivePlayers = hg.getAlivePlayers();
		Set<Player> involvedPlayers = hg.getInvolvedPlayers();
		Set<Player> sleepPlayers = hg.getSleepPlayers();
		Set<Player> combatPlayers = hg.getCombatPlayers();
		Set<Player> healedPlayers = hg.getHealedPlayers();
		Set<Player> foundItemPlayers = hg.getFoundItemPlayers();
		
		//Get list of player that did not sleep during this turn.
		HashSet<Player> noSleepPlayers = new HashSet<>(alivePlayers);
		noSleepPlayers.removeAll(sleepPlayers);
		
		if (noSleepPlayers.isEmpty())
			return new EventResult("", EventResult.State.FAILED); //Quit the event.
		
		//Get a random player.
		Player player = (Player) RandomChoice.getRandom(noSleepPlayers.toArray());
		
		if (player.getHealth() >= player.getMaxHealth())
			return new EventResult("", EventResult.State.FAILED); //Quit the event.
		
		//The player sleeps, so he heals.
		float damage = (15 + RandomChoice.getRandom().nextInt(30));
		player.setHealth(player.getHealth() + damage);
		
		String eventMessage = (String) RandomChoice.getRandom(this.messages);
		eventMessage = eventMessage.replace("damage", "" + (int) damage);
		eventMessage = eventMessage.replace("user", "**" + player + "**");
		
		stringBuilder.append(eventMessage + "\n");
		
		//The player can't do anything else during this turn.
		involvedPlayers.add(player);
		combatPlayers.add(player);
		foundItemPlayers.add(player);
		healedPlayers.add(player);
		sleepPlayers.add(player);
		
		return new EventResult(stringBuilder.toString(), EventResult.State.SUCCESSFUL);
	}
}
