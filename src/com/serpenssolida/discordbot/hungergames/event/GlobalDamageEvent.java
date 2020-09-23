package com.serpenssolida.discordbot.hungergames.event;

import com.serpenssolida.discordbot.RandomChoice;
import com.serpenssolida.discordbot.hungergames.HungerGames;
import com.serpenssolida.discordbot.hungergames.Player;

import java.util.HashSet;

public class GlobalDamageEvent extends HungerGamesEvent
{
	public String[] messages =
			{
					"Una folata di vento spazza via tutti i giocatori che perdono damageHP.",
					"Un terremoto magnitudo 9 squote la terra danneggiando tutti i giocatori di damageHP.",
					"I giocatori sono risultati positivi al coronavirus, tutti i giocatori si sentono male e perdono damageHP."
			};
	
	public EventResult doEvent(HungerGames hg)
	{
		StringBuilder builder = new StringBuilder();
		HashSet<Player> alivePlayers = hg.getAlivePlayers();
		HashSet<Player> deadPlayers = hg.getDeadPlayers();
		HashSet<Player> involvedPlayers = hg.getInvolvedPlayers();
		
		//Damage of the event.
		float damage = (15 + RandomChoice.random.nextInt(30));
		
		String eventString = ((String) RandomChoice.getRandom(this.messages)).replaceAll("damage", "" + (int) damage);
		builder.append(eventString + "\n");
		
		//Damage all the players.
		for (Player alivePlayer : alivePlayers)
		{
			alivePlayer.setHealth(alivePlayer.getHealth() - damage);
			
			if (alivePlayer.isDead())
			{
				deadPlayers.add(alivePlayer);
				alivePlayer.removeRelationships();
				
				builder.append("**" + alivePlayer + "** è morto.\n");
			}
		}
		
		involvedPlayers.addAll(alivePlayers);
		alivePlayers.removeAll(deadPlayers);
		
		return new EventResult(builder.toString(), EventResult.State.Successful);
	}
}
