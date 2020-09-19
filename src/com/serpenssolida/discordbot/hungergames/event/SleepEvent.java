package com.serpenssolida.discordbot.hungergames.event;

import com.serpenssolida.discordbot.RandomChoice;
import com.serpenssolida.discordbot.hungergames.HungerGames;
import com.serpenssolida.discordbot.hungergames.Player;

import java.util.HashSet;

public class SleepEvent extends HungerGamesEvent
{
	public String[] messages = new String[]
            {
                    "user non si regge più in piedi e decide di fare un sonnellino, user guadagna damageHP.",
                    "user si fa un sonnellino recuperando damageHP.",
                    "user si appisola su una pietra e recupera damageHP.",
                    "user dorme sereno guadagnando damageHP.",
                    "user si mette a dormire per terra. user recupera damageHP."
            };
	
	public EventResult doEvent(HungerGames hg)
	{
		StringBuilder builder = new StringBuilder();
		
		HashSet<Player> alivePlayers = hg.getAlivePlayers();
		HashSet<Player> involvedPlayers = hg.getInvolvedPlayers();
		HashSet<Player> sleepPlayers = hg.getSleepPlayers();
		HashSet<Player> combatPlayers = hg.getCombatPlayers();
		HashSet<Player> healedPlayers = hg.getHealedPlayers();
		HashSet<Player> foundItemPlayers = hg.getFoundItemPlayers();
		
		//Get list of player that did not sleep during this turn.
		HashSet<Player> noSleepPlayers = new HashSet<>(alivePlayers);
		noSleepPlayers.removeAll(sleepPlayers);
		
		if (noSleepPlayers.isEmpty())
			return new EventResult("", EventResult.State.Failed); //Quit the event.
		
		//Get a random player.
		Player player = (Player) RandomChoice.getRandom(noSleepPlayers.toArray());
		
		if (player.getHealth() >= player.getMaxHealth())
			return new EventResult("", EventResult.State.Failed); //Quit the event.
		
		//The player sleeps, so he heals.
		float damage = (15 + RandomChoice.random.nextInt(30));
		player.setHealth(player.getHealth() + damage);
		
		String eventMessage = (String) RandomChoice.getRandom(this.messages);
		eventMessage = eventMessage.replaceAll("damage", "" + (int) damage);
		eventMessage = eventMessage.replaceAll("user", "**" + player + "**");
		
		builder.append(eventMessage + "\n");
		
		//The player can't do anything else during this turn.
		involvedPlayers.add(player);
		combatPlayers.add(player);
		foundItemPlayers.add(player);
		healedPlayers.add(player);
		sleepPlayers.add(player);
		
		return new EventResult(builder.toString(), EventResult.State.Successful);
	}
}
