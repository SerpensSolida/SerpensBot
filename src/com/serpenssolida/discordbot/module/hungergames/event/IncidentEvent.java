package com.serpenssolida.discordbot.module.hungergames.event;

import com.serpenssolida.discordbot.RandomChoice;
import com.serpenssolida.discordbot.module.hungergames.HungerGames;
import com.serpenssolida.discordbot.module.hungergames.Player;

import java.util.HashSet;
import java.util.Set;

public class IncidentEvent implements HungerGamesEvent
{
	private final String[] messages =
			{
					"user cade da un dirupo perdendo damageHP.",
					"user inciampa e si rompe la caviglia facedosi damageHP di danno.",
					"user tira un pugno sul muro dalla rabbia, user perde damageHP.",
					"user ha un capogiro, cade sbattendo la testa prendendo damageHP di danno.",
					"user viene investito da un autobus perdendo damageHP.",
					"user viene colpito da un fulmine e perde damageHP.",
					"user perde damageHP per colpa di uno stand nemico."
			};
	
	public EventResult doEvent(HungerGames hg)
	{
		StringBuilder stringBuilder = new StringBuilder();
		Set<Player> alivePlayers = hg.getAlivePlayers();
		Set<Player> deadPlayers = hg.getDeadPlayers();
		Set<Player> involvedPlayers = hg.getInvolvedPlayers();
		Set<Player> incidentPlayers = hg.getIncidentPlayers();
		
		//List of player that did not have an incident this turn.
		Set<Player> noIncidentPlayers = new HashSet<>(alivePlayers);
		noIncidentPlayers.removeAll(incidentPlayers);
		noIncidentPlayers.removeAll(involvedPlayers);
		
		if (noIncidentPlayers.isEmpty())
			return new EventResult("", EventResult.State.FAILED); //Quit the event.
		
		//Chose a random player.
		Player player = (Player) RandomChoice.getRandom(noIncidentPlayers.toArray());
		
		//Damage the player.
		float damage = (15 + RandomChoice.getRandom().nextInt(30));
		player.setHealth(player.getHealth() - damage);
		
		String eventMessage = (String) RandomChoice.getRandom(this.messages);
		eventMessage = eventMessage.replace("damage", "" + (int) damage);
		eventMessage = eventMessage.replace("user", "**" + player + "**");
		stringBuilder.append(eventMessage + "\n");
		
		if (player.isDead())
		{
			alivePlayers.remove(player);
			deadPlayers.add(player);
			
			player.removeRelationships();
			
			stringBuilder.append("**" + player + "** è morto.\n");
		}
		
		involvedPlayers.add(player);
		incidentPlayers.add(player);
		
		return new EventResult(stringBuilder.toString(), EventResult.State.SUCCESSFUL);
	}
}
