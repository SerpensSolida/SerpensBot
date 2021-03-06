package com.serpenssolida.discordbot.module.hungergames;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.serpenssolida.discordbot.RandomChoice;
import com.serpenssolida.discordbot.avatar.AvatarCache;
import com.serpenssolida.discordbot.module.hungergames.event.*;
import com.serpenssolida.discordbot.module.hungergames.inventory.Weapon;
import com.serpenssolida.discordbot.module.hungergames.io.ItemData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.utils.AttachmentOption;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashSet;

public class HungerGamesThread extends Thread
{
	private String guildID;
	private MessageChannel channel;
	private HungerGames hg;
	
	private static HungerGamesEvent[] events = {
			new BattleEvent(),
			new EatFoodEvent(),
			new FoundItemEvent(),
			new SurpriseAttackEvent(),
			new IncidentEvent(),
			new SleepEvent(),
			new RandomEvent(),
			new RelationshipEvent()
	};
	
	public HungerGamesThread(String guildID, MessageChannel channel)
	{
		RandomChoice.resetRandom();
		this.guildID = guildID;
		this.channel = channel;
		this.hg = new HungerGames(this.guildID, this.loadItems());
	}
	
	public void run()
	{
		try
		{
			this.runHungerGames();
		}
		catch (InterruptedException e)
		{
			System.out.println("Gli HungerGames sono stati fermati.");
			Thread.currentThread().interrupt();
		}
	}
	
	private void runHungerGames() throws InterruptedException
	{
		MessageBuilder messageBuilder = new MessageBuilder();
		EmbedBuilder embedBuilder = new EmbedBuilder();
		StringBuilder stringBuilder = new StringBuilder();
		HashSet<Player> alivePlayers = this.hg.getAlivePlayers();
		
		//Check if there are enough player to play the game.
		if (alivePlayers.size() < 2)
		{
			messageBuilder.append("> Numero giocatori insufficenti. Chiedi a qualcuno di creare o abilitare il suo personaggio.");
			this.channel.sendMessage(messageBuilder.build()).queue();
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
		messageBuilder.setEmbed(embedBuilder.build());
		
		this.channel.sendMessage(messageBuilder.build()).queue();
		
		//Main game loop.
		while (this.isHungerGamesRunning() && !this.isInterrupted())
		{
			//Chose a random number of turns for the day.
			int turnsNum = 4 + RandomChoice.random.nextInt(4);
			
			Thread.sleep(HungerGamesController.getMessageSpeed(this.guildID));
			
			embedBuilder = new EmbedBuilder()
					.setTitle("Alba del " + this.hg.getDay() + "° giorno.");
			
			this.channel.sendMessage(embedBuilder.build()).queue();
			
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
				this.channel.sendMessage(embedBuilder.build()).queue();
			}
			
			this.hg.incrementDay();
		}
		
		if (!this.isInterrupted())
		{
			if (alivePlayers.size() > 0)
			{
				Player winner = (Player) alivePlayers.toArray()[0]; //This player is the winner.
				
				messageBuilder = new MessageBuilder();
				embedBuilder = (new EmbedBuilder()).setTitle("Il vincitore è **" + winner + "**!").setThumbnail(winner.getOwner().getAvatarUrl());
				messageBuilder.setEmbed(embedBuilder.build());
				
				//Update player statistics.
				winner.getCharacter().incrementWins();
				HungerGamesController.getWinners(this.guildID).add(winner.getOwner().getId());
			}
			else
			{
				//All player died, no winner.
				messageBuilder = new MessageBuilder();
				embedBuilder = new EmbedBuilder();
				
				embedBuilder.setTitle("Nessun vincitore!");
				embedBuilder.setDescription("Sono morti tutti i giocatori.");
				
				messageBuilder.setEmbed(embedBuilder.build());
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
		MessageBuilder messageBuilder = new MessageBuilder();
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
			int eventNum = 6 + RandomChoice.random.nextInt(4);
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
				while (eventResult.getState() == EventResult.State.Failed && tries > 0);
				
				//Append the result to the message.
				if (eventResult.getState() != EventResult.State.Failed && !eventResult.getEventMessage().isBlank())
				{
					stringBuilder.append("" + eventResult + "\n");
				}
			}
		}
		
		//Set the status image of the message.
		ByteArrayOutputStream statusImage = this.generateStatusImage();
		
		//Send the turn description.
		embedBuilder.setTitle("Hunger Games - " + this.hg.getDay() + "° giorno.")
					.setDescription(stringBuilder.toString())
					.setFooter("Giocatori ancora in vita: " + this.hg.getAlivePlayers().size())
					.setImage("attachment://status.png");
		messageBuilder.setEmbed(embedBuilder.build());
		
		this.channel.sendMessage(messageBuilder.build())
				.addFile(statusImage.toByteArray(), "status.png", new AttachmentOption[0])
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
	 * @return Whether or not the HungerGames is not over.
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
		File file = new File(HungerGamesController.folder + "/items.json");
		Gson gson = (new GsonBuilder()).setPrettyPrinting().enableComplexMapKeySerialization().create();
		ItemData data = new ItemData();
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			try
			{
				data = gson.fromJson(reader, ItemData.class);
				reader.close();
			}
			catch (Throwable throwable)
			{
				try
				{
					reader.close();
				}
				catch (Throwable throwable1)
				{
					throwable.addSuppressed(throwable1);
				}
				throw throwable;
			}
		}
		catch (FileNotFoundException e)
		{
			System.out.println("Nessun file degli oggetti da caricare.");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return data;
	}
	
	/**
	 * @return The status image of the turn.
	 */
	public ByteArrayOutputStream generateStatusImage()
	{
		BufferedImage tombImage = this.getTombImage(); //Tombstone image.
		
		//List of players that participated to the turn.
		HashSet<Player> involvedPlayers = this.hg.getInvolvedPlayers();
		
		int avatarSize = 64; //Size of the avatrs.
		int avatarNum = involvedPlayers.size(); //Count of the avatars.
		
		//Calculate image dimensions.
		int imageWidth = (avatarNum > 6) ? (6 * avatarSize) : (avatarNum * avatarSize);
		int imageHeight = (avatarNum / 6 + ((avatarNum % 6 == 0) ? 0 : 1)) * avatarSize;
		
		//Create the image and get its Graphics.
		BufferedImage statusImage = new BufferedImage(imageWidth, imageHeight, 6);
		Graphics g = statusImage.getGraphics();
		
		int i = 0;
		
		for (Player involvedPlayer : involvedPlayers)
		{
			BufferedImage avatar = AvatarCache.getAvatar(involvedPlayer.getOwner());
			
			//Position of the avatar.
			int posX = i % 6 * avatarSize;
			int posY = i / 6 * avatarSize;
			
			//Place the avatar into the image.
			Image avatarImage = avatar.getScaledInstance(avatarSize, avatarSize, 4);
			g.drawImage(avatarImage, posX, posY, null);
			
			//Paint the avatar red if the player died during this turn.
			if (involvedPlayer.isDead())
			{
				g.setColor(new Color(255, 0, 0, 100));
				g.fillRect(posX, posY, avatarSize, avatarSize);
				
				Image tomb = tombImage.getScaledInstance(avatarSize / 2, avatarSize / 2, 4);
				g.drawImage(tomb, posX, posY + avatarSize - avatarSize / 2 - avatarSize / 5, null);
			}
			
			//Draw HP remaining.
			g.setColor(new Color(197, 197, 197, 160));
			g.fillRect(posX, posY + avatarSize - avatarSize / 5, avatarSize, avatarSize / 5);
			g.setColor(Color.black);
			g.setFont(HungerGamesController.font);
			g.drawString("HP: " + (int) involvedPlayer.getHealth(), posX + 4, posY + avatarSize - avatarSize / 10 + 5);
			
			i++;
		}
		
		ByteArrayOutputStream outputStream = null;
		
		try
		{
			outputStream = new ByteArrayOutputStream();
			ImageIO.write(statusImage, "png", outputStream);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return outputStream;
	}
	
	private BufferedImage getTombImage()
	{
		File tombFile = new File(HungerGamesController.folder + "/tombstone.png");
		
		try
		{
			return ImageIO.read(tombFile);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
	}
	
	/**
	 * @return A list of relationship currently active in the HungerGames.
	 */
	private String getRelationships()
	{
		StringBuilder stringBuilder = new StringBuilder();
		
		for (Player alivePlayer : this.hg.getAlivePlayers())
		{
			HashSet<Player> friends = alivePlayer.getFriends();
			HashSet<Player> enemies = alivePlayer.getEnemies();
			
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
	
	private void calculate()
	{
		float specialNum = 0.0F;
		float strengthNum = 0.0F;
		float abilityNum = 0.0F;
		float specialValue = 0.0F;
		float strengthValue = 0.0F;
		float abilityValue = 0.0F;
		for (Weapon weapon : this.hg.getItemData().getWeapons())
		{
			switch (weapon.getType())
			{
				case Strength:
					strengthNum++;
					strengthValue += weapon.getDamage();
				case Ability:
					abilityNum++;
					abilityValue += weapon.getDamage();
				case Special:
					specialNum++;
					specialValue += weapon.getDamage();
			}
		}
		float strengthAv = strengthValue / strengthNum;
		float abilityAv = abilityValue / abilityNum;
		float specialAv = specialValue / specialNum;
		System.out.println("Forza");
		System.out.println("\tNumero:" + strengthNum);
		System.out.println("\tMedia:" + strengthAv);
		System.out.println("Abilità");
		System.out.println("\tNumero:" + abilityNum);
		System.out.println("\tMedia:" + abilityAv);
		System.out.println("Special:");
		System.out.println("\tNumero:" + specialNum);
		System.out.println("\tMedia:" + specialAv);
	}
}
