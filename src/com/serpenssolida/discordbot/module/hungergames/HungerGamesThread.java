package com.serpenssolida.discordbot.module.hungergames;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.RandomChoice;
import com.serpenssolida.discordbot.module.hungergames.event.*;
import com.serpenssolida.discordbot.module.hungergames.io.ItemData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Set;

public class HungerGamesThread extends Thread
{
	private final String guildID;
	private final MessageChannel channel;
	private final HungerGames hg;
	private final User author;
	
	private static final Logger logger = LoggerFactory.getLogger(HungerGamesThread.class);
	
	private static final HungerGamesEvent[] events =
	{
			new DuelEvent(),
			new EatFoodEvent(),
			new FoundItemEvent(),
			new SurpriseAttackEvent(),
			new IncidentEvent(),
			new SleepEvent(),
			new RandomEvent(),
			new RelationshipEvent()
	};
	
	public HungerGamesThread(String guildID, MessageChannel channel, User author)
	{
		RandomChoice.resetRandom();
		this.guildID = guildID;
		this.channel = channel;
		this.author = author;
		this.hg = new HungerGames(this.guildID, this.loadItems());
	}
	
	@Override
	public void run()
	{
		try
		{
			this.runHungerGames();
		}
		catch (InterruptedException e)
		{
			logger.info("Gli HungerGames sono stati fermati.");
			Thread.currentThread().interrupt();
		}
	}
	
	private void runHungerGames() throws InterruptedException
	{
		MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
		EmbedBuilder embedBuilder = new EmbedBuilder();
		Set<Player> alivePlayers = this.hg.getAlivePlayers();
		
		//Check if there are enough player to play the game.
		if (alivePlayers.size() < 2)
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Connect4", this.author, "Numero giocatori insufficenti. Chiedi a qualcuno di creare o abilitare il suo personaggio.");
			this.channel.sendMessage(message).queue();
			HungerGamesController.setRunning(this.guildID,false);
			return;
		}
		
		//Send a message with the list of player and their status.
		int c = 1;
		for (Player player : alivePlayers)
		{
			//Player field.
			embedBuilder.addField(player.toString(), "HP:" + player.getMaxHealth() + "\n" + player.getInventory(), true);
			c++;
			
			//Add a blank field in the third column.
			if (c % 3 == 0)
			{
				embedBuilder.addBlankField(true);
				c++;
			}
		}
		
		embedBuilder//.setDescription(stringBuilder.toString())
				.setTitle("**Partecipanti alla " + (HungerGamesController.getCount(this.guildID) + 1) + "° edizione degli Hunger Games!**");
		messageBuilder.setEmbeds(embedBuilder.build());
		
		this.channel.sendMessage(messageBuilder.build()).queue();
		
		//Main game loop.
		while (this.isHungerGamesRunning() && !this.isInterrupted())
		{
			//Chose a random number of turns for the day.
			int turnsNum = 4 + RandomChoice.getRandom().nextInt(4);
			
			Thread.sleep(HungerGamesController.getMessageSpeed(this.guildID));
			
			embedBuilder = new EmbedBuilder()
					.setTitle("Alba del " + this.hg.getDay() + "° giorno.");
			
			this.channel.sendMessageEmbeds(embedBuilder.build()).queue();
			
			//Process the turns.
			for (int i = 0; i < turnsNum && this.isHungerGamesRunning(); i++)
			{
				this.doTurnCycle();
				Thread.sleep(HungerGamesController.getMessageSpeed(this.guildID));
			}
			
			//Send a message containing the relashionships between players.
			if (this.isHungerGamesRunning())
			{
				embedBuilder = (new EmbedBuilder()).setTitle("Alleanze e rivalità").setDescription(this.getRelationships());
				this.channel.sendMessageEmbeds(embedBuilder.build()).queue();
			}
			
			this.hg.incrementDay();
		}
		
		if (!this.isInterrupted())
		{
			if (!alivePlayers.isEmpty())
			{
				Player winner = (Player) alivePlayers.toArray()[0]; //This player is the winner.
				
				messageBuilder = new MessageCreateBuilder();
				embedBuilder = (new EmbedBuilder()).setTitle("Il vincitore è **" + winner + "**!").setThumbnail(winner.getOwner().getAvatarUrl());
				messageBuilder.setEmbeds(embedBuilder.build());
				
				//Update player statistics.
				winner.getCharacter().incrementWins();
				HungerGamesController.getWinners(this.guildID).add(winner.getOwner().getId());
			}
			else
			{
				//All player died, no winner.
				messageBuilder = new MessageCreateBuilder();
				embedBuilder = new EmbedBuilder();
				
				embedBuilder.setTitle("Nessun vincitore!");
				embedBuilder.setDescription("Sono morti tutti i giocatori.");
				
				messageBuilder.setEmbeds(embedBuilder.build());
				HungerGamesController.getWinners(this.guildID).add(null);
			}
			
			this.channel.sendMessage(messageBuilder.build()).queue();
			
			//Update HungerGames statistics.
			HungerGamesController.setCount(this.guildID, HungerGamesController.getCount(this.guildID) + 1);
			HungerGamesController.setRunning(this.guildID, false);
			HungerGamesController.save(this.guildID);
		}
	}
	
	private void doTurnCycle()
	{
		MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
		StringBuilder stringBuilder = new StringBuilder();
		EmbedBuilder embedBuilder = new EmbedBuilder();
		
		if (RandomChoice.randomChance(1) && this.isHungerGamesRunning() && !this.isInterrupted())
		{
			//Global event.
			EventResult eventResult = new GlobalDamageEvent().doEvent(this.hg);
			stringBuilder.append("" + eventResult + "\n");
		}
		else
		{
			//Choose a random number of local events and execute them.
			int eventNum = 6 + RandomChoice.getRandom().nextInt(4);
			int tries = 8; //Number of tries.
			
			for (int i = 0; i < eventNum && this.isHungerGamesRunning() && !this.isInterrupted(); i++)
			{
				EventResult eventResult; //Result of the event.
				
				do
				{
					HungerGamesEvent event = this.generateRandomEvent();
					eventResult = event.doEvent(this.hg);
					tries--;
				}
				while (eventResult.getState() == EventResult.State.FAILED && tries > 0);
				
				//Append the result to the message.
				if (eventResult.getState() != EventResult.State.FAILED && !eventResult.getEventMessage().isBlank())
				{
					stringBuilder.append("" + eventResult + "\n");
				}
			}
		}
		
		//Set the status image of the message.
		byte[] statusImage = HungerGameStatusImageDrawer.generateStatusImage(this.hg);
		
		//Send the turn description.
		embedBuilder.setTitle("Hunger Games - " + this.hg.getDay() + "° giorno.")
					.setDescription(stringBuilder.toString())
					.setFooter("Giocatori ancora in vita: " + this.hg.getAlivePlayers().size())
					.setImage("attachment://status.png");
		messageBuilder.setEmbeds(embedBuilder.build());
		
		//Check if the image was generated correctly.
		if (statusImage != null)
			this.channel.sendMessage(messageBuilder.build())
					.setFiles(FileUpload.fromData(statusImage, "status.png"))
					.queue();
		
		//Clear all lists used by events.
		this.hg.getInvolvedPlayers().clear();
		this.hg.getCombatPlayers().clear();
		this.hg.getHealedPlayers().clear();
		this.hg.getFoundItemPlayers().clear();
		this.hg.getIncidentPlayers().clear();
		this.hg.getSleepPlayers().clear();
	}
	
	/**
	 * @return Whether the HungerGames is not over.
	 */
	private boolean isHungerGamesRunning()
	{
		return (this.hg.getAlivePlayers().size() > 1);
	}
	
	/**
	 * Loads the item from json.
	 *
	 * @return Data of the items.
	 */
	public ItemData loadItems()
	{
		File file = new File(HungerGamesController.FOLDER + "/items.json");
		Gson gson = (new GsonBuilder()).setPrettyPrinting().enableComplexMapKeySerialization().create();
		ItemData data = new ItemData();
		
		try (BufferedReader reader = new BufferedReader(new FileReader(file)))
		{
			data = gson.fromJson(reader, ItemData.class);
		}
		catch (FileNotFoundException e)
		{
			logger.info("Nessun file degli oggetti da caricare.");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return data;
	}
	
	/**
	 * @return A list of relationship currently active in the HungerGames.
	 */
	private String getRelationships()
	{
		StringBuilder stringBuilder = new StringBuilder();
		
		for (Player alivePlayer : this.hg.getAlivePlayers())
		{
			Set<Player> friends = alivePlayer.getFriends();
			Set<Player> enemies = alivePlayer.getEnemies();
			
			if (!friends.isEmpty() || !enemies.isEmpty())
			{
				stringBuilder.append("**" + alivePlayer + "** - ");
				stringBuilder.append("Alleati: ");
				
				if (!friends.isEmpty())
				{
					for (Player friend : friends)
					{
						stringBuilder.append("**" + friend + "**, ");
					}
					
					stringBuilder.deleteCharAt(stringBuilder.length() - 1);
					stringBuilder.deleteCharAt(stringBuilder.length() - 1);
					stringBuilder.append(". ");
				}
				else
				{
					stringBuilder.append("nessuno. ");
				}
				
				stringBuilder.append("Nemici: ");
				
				if (!enemies.isEmpty())
				{
					for (Player enemy : enemies)
					{
						stringBuilder.append("**" + enemy + "**, ");
					}
					
					stringBuilder.deleteCharAt(stringBuilder.length() - 1);
					stringBuilder.deleteCharAt(stringBuilder.length() - 1);
					stringBuilder.append(". ");
				}
				else
				{
					stringBuilder.append("nessuno. ");
				}
				
				stringBuilder.append("\n");
			}
		}
		
		if (stringBuilder.toString().isBlank())
		{
			stringBuilder.append("Nessuna amicizia o rivalità.");
		}
		
		return stringBuilder.toString();
	}
	
	/**
	 * @return A random event.
	 */
	private HungerGamesEvent generateRandomEvent()
	{
		float[] probs = {0.35F, 0.25F, 0.05F, 0.05F, 0.05F, 0.05F, 0.1F, 0.1F};
		return (HungerGamesEvent) RandomChoice.getRandomWithProbability(events, probs);
	}
}
