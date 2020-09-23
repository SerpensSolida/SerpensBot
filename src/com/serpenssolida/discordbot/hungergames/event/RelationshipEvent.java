package com.serpenssolida.discordbot.hungergames.event;

import com.serpenssolida.discordbot.RandomChoice;
import com.serpenssolida.discordbot.hungergames.HungerGames;
import com.serpenssolida.discordbot.hungergames.Player;

import java.util.HashSet;

public class RelationshipEvent extends HungerGamesEvent
{
	private String[] friendMessages = new String[]
			{
					"user e receiver hanno stretto un alleanza.",
					"user e receiver sono diventati amici per la pelle.",
					"user e receiver si sono abbracciati per riscaldarsi a vicenda.",
					"user e receiver si mettono a cantare in insieme.",
					"user e receiver si sono alleati.",
					"E' nata una nuova alleanza fra user e receiver.",
					"user e receiver firmano un armistizio.",
					"user e receiver fanno un patto di sangue.",
					"user: \"Niceu niceu, verry niceu receiver-chan\""
			};
	
	private String[] enemyMessages = new String[]
			{
					"user e receiver si guardano male.",
					"user e receiver si approcciano minacciosamente.",
					"user offende pesantemente receiver.",
					"user chiede a receiver di non azzardarsi a nominare sua madre, MAI, MAI PIU'!",
					"user dice a receiver di mettere la mascherina mentre si fa i selfie con le signore.",
					"user sputa in faccia a receiver.",
					"user dice che la mamma di receiver è così grassa da avere un campo gravitazionale proprio.",
					"user e receiver giocano insieme ad Among us."
			};
	
	public EventResult doEvent(HungerGames hg)
	{
		StringBuilder builder = new StringBuilder();
		HashSet<Player> alivePlayers = hg.getAlivePlayers();
		HashSet<Player> involvedPlayers = hg.getInvolvedPlayers();
		HashSet<Player> availablePlayers = new HashSet<>(alivePlayers);
		
		if (availablePlayers.isEmpty())
			return new EventResult("", EventResult.State.Failed); //Quit the event.
		
		//Chose a random player.
		Player player1 = (Player) RandomChoice.getRandom(availablePlayers.toArray());
		
		//Remove all friends and enemies from the available player.
		availablePlayers.remove(player1);
		availablePlayers.removeAll(player1.getEnemies());
		availablePlayers.removeAll(player1.getFriends());
		
		if (availablePlayers.isEmpty())
			return new EventResult("", EventResult.State.Failed); //Quit the event.
		
		//Chose the second player.
		Player player2 = (Player) RandomChoice.getRandom(availablePlayers.toArray());
		
		String message;
		
		if (RandomChoice.random.nextBoolean())
		{
			//Make the two player friends.
			player1.getFriends().add(player2);
			player2.getFriends().add(player1);
			
			message = (String) RandomChoice.getRandom(this.friendMessages);
		}
		else
		{
			//Make the two player enemies.
			player1.getEnemies().add(player2);
			player2.getEnemies().add(player1);
			
			message = (String) RandomChoice.getRandom(this.enemyMessages);
		}
		
		involvedPlayers.add(player1);
		involvedPlayers.add(player2);
		
		message = message.replaceAll("user", "**" + player1 + "**");
		message = message.replaceAll("receiver", "**" + player2 + "**");
		builder.append(message + "\n");
		
		return new EventResult(builder.toString(), EventResult.State.Successful);
	}
}
