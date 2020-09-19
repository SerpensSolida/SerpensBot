package com.serpenssolida.discordbot.hungergames.event;

import com.serpenssolida.discordbot.RandomChoice;
import com.serpenssolida.discordbot.hungergames.HungerGames;
import com.serpenssolida.discordbot.hungergames.Player;

import java.util.HashSet;

public class IncidentEvent extends HungerGamesEvent
{
	public String[] messages =
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
		StringBuilder builder = new StringBuilder();
		HashSet<Player> alivePlayers = hg.getAlivePlayers();
		HashSet<Player> deadPlayers = hg.getDeadPlayers();
		HashSet<Player> involvedPlayers = hg.getInvolvedPlayers();
		HashSet<Player> incidentPlayers = hg.getIncidentPlayers();
		
		//List of player that did not have an incident this turn.
		HashSet<Player> noIncidentPlayers = new HashSet<>(alivePlayers);
		noIncidentPlayers.removeAll(incidentPlayers);
		
		if (noIncidentPlayers.isEmpty())
			return new EventResult("", EventResult.State.Failed); //Quit the event.
		
		//Chose a random player.
		Player player = (Player) RandomChoice.getRandom(noIncidentPlayers.toArray());
		
		//Damage the player.
		float damage = (15 + RandomChoice.random.nextInt(30));
		player.setHealth(player.getHealth() - damage);
		
		String eventMessage = (String) RandomChoice.getRandom(this.messages);
		eventMessage = eventMessage.replaceAll("damage", "" + (int) damage);
		eventMessage = eventMessage.replaceAll("user", "**" + player + "**");
		builder.append(eventMessage + "\n");
		
		if (player.isDead())
		{
			alivePlayers.remove(player);
			deadPlayers.add(player);
			
			player.removeRelationships();
			
			builder.append("**" + player + "** è morto.\n");
		}
		
		involvedPlayers.add(player);
		incidentPlayers.add(player);
		
		return new EventResult(builder.toString(), EventResult.State.Successful);
	}
}
