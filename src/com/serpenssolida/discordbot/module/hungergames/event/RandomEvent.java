package com.serpenssolida.discordbot.module.hungergames.event;

import com.serpenssolida.discordbot.RandomChoice;
import com.serpenssolida.discordbot.module.hungergames.HungerGames;
import com.serpenssolida.discordbot.module.hungergames.Player;

import java.util.HashSet;
import java.util.Set;

public class RandomEvent implements HungerGamesEvent
{
	private final String[] messages =
			{
					"user ha paura di morire.",
					"user guarda all'orizzonte cercando di scrutare qualcosa.",
					"user, stanco di camminare, si riposa su una pietra.",
					"user vuole tornare a casa.",
					"user ha brama di sangue.",
					"user si sdraia in un prato.",
					"user si mette a danzare da solo.",
					"user sta piangendo."
			};
	
	public EventResult doEvent(HungerGames hg)
	{
		Set<Player> alivePlayers = hg.getAlivePlayers();
		Set<Player> involvedPlayers = hg.getInvolvedPlayers();
		
		//List of player that did nothing during this turn.
		HashSet<Player> availablePlayers = new HashSet<>(alivePlayers);
		availablePlayers.removeAll(involvedPlayers);
		
		if (availablePlayers.isEmpty())
			return new EventResult("", EventResult.State.FAILED); //Quit the event.
		
		//Chose a random player.
		Player player = (Player) RandomChoice.getRandom(availablePlayers.toArray());
		
		String message = (String) RandomChoice.getRandom(this.messages);
		message = message.replace("user", "**" + player + "**");
		
		//Add the player to the list of active players.
		involvedPlayers.add(player);
		
		return new EventResult(message + "\n", EventResult.State.SUCCESSFUL);
	}
}
