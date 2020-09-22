package com.serpenssolida.discordbot.hungergames;

import com.serpenssolida.discordbot.BotListener;
import com.serpenssolida.discordbot.BotMain;
import com.serpenssolida.discordbot.Command;
import com.serpenssolida.discordbot.hungergames.task.CreateCharacterTask;
import com.serpenssolida.discordbot.hungergames.task.EditCharacterTask;

import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class HungerGamesListener extends BotListener
{
	public HungerGamesListener()
	{
		super("hg");
		
		//Command for creating a character.
		Command command = (new Command("create", 0)).setCommandListener((guild, channel, message, author, args) ->
		{
			if (this.stopIfHungerGamesIsRunning(channel))
				this.addTask(new CreateCharacterTask(author, channel));
			return true;
		});
		command.setHelp("Fa partire la procedura per la creazione di un personaggio.");
		this.addCommand(command);
		
		//Command for displaying character info.
		command = (new Command("character", 1)).setCommandListener((guild, channel, message, author, args) ->
		{
			this.sendCharacterCard(channel, message, author, args);
			return true;
		});
		command.setMinArgumentNumber(0);
		command.setHelp("Invia alla chat la card delle statistiche del personaggio.");
		this.addCommand(command);
		
		//Command for editing a character.
		command = (new Command("edit", 0)).setCommandListener((guild, channel, message, author, args) ->
		{
			if (this.stopIfHungerGamesIsRunning(channel))
				this.addTask(new EditCharacterTask(author, channel));
			return true;
		});
		command.setHelp("Fa partire la procedura di modifica del personaggio.");
		this.addCommand(command);
		
		//Command for enabling or disabling a character.
		command = (new Command("enable", 1)).setCommandListener((guild, channel, message, author, args) ->
		{
			if (this.stopIfHungerGamesIsRunning(channel))
				this.setCharacterEnabled(channel, author, args);
			return true;
		});
		command.setHelp("Abilita/Disabilita il personaggio. Un personaggio disabilitato non parteciperà agli HungerGames.");
		command.setArgumentsDescription("(true|false)");
		this.addCommand(command);
		
		//Command for starting a new HungerGames.
		command = (new Command("start", 0)).setCommandListener((guild, channel, message, author, args) ->
		{
			if (this.stopIfHungerGamesIsRunning(channel))
				HungerGamesController.startHungerGames(channel);
			return true;
		});
		command.setHelp("Inizia un edizione degli Hunger Games!");
		this.addCommand(command);
		
		//Command for editing playback speed of the HungerGames.
		command = (new Command("speed", 1)).setCommandListener((guild, channel, message, author, args) ->
		{
			this.setPlaybackSpeed(channel, args);
			return true;
		});
		command.setHelp("Modifica la velocità di riproduzione degli Hunger Games (velocità minima 1 secondo).");
		command.setArgumentsDescription("secondi");
		this.addCommand(command);
		
		//Command for displaying leaderboards of the Hunger Games.
		command = (new Command("leaderboard", 1)).setCommandListener((guild, channel, message, author, args) ->
		{
			this.sendLeaderboard(channel, args);
			return true;
		});
		command.setHelp("Modifica la velocità di riproduzione degli Hunger Games (velocità minima 1 secondo).");
		command.setArgumentsDescription("(wins|kills)");
		this.addCommand(command);
		
		//Command for displaying leaderboards of the Hunger Games.
		command = (new Command("stop", 0)).setCommandListener((guild, channel, message, author, args) ->
		{
			this.stopHungerGames(channel, author);
			return true;
		});
		command.setHelp("Interrompe l'esecuzione degli HungerGames.");
		this.addCommand(command);
	}
	
	private void sendCharacterCard(MessageChannel channel, Message message, User author, String[] args)
	{
		Character character = null;
		
		if (args == null)
		{
			character = HungerGamesController.getCharacter(author.getId());
		}
		else
		{
			List<User> users = message.getMentionedUsers();
			
			if (users.size() > 0)
			{
				character = HungerGamesController.getCharacter(users.get(0).getId());
			}
			else
			{
				channel.sendMessage("> Devi taggare un utente.").queue();
				return;
			}
		}
		
		if (character != null)
		{
			User owner = BotMain.api.retrieveUserById(character.getOwnerID()).complete();
			String ownerName = (owner != null) ? owner.getName() : null;
			
			String[] statsName = {"Vitalità", "Forza", "Abilità", "Special", "Velocità", "Resistenza", "Gusto"};
			String[] statsValue = {
					"" + character.getVitality(),
					"" + character.getStrength(),
					"" + character.getAbility(),
					"" + character.getSpecial(),
					"" + character.getSpeed(),
					"" + character.getEndurance(),
					"" + character.getTaste()
			};
			
			//Names of the characteristics of the player.
			StringBuilder nameColumn = new StringBuilder();
			
			for (String s : statsName)
			{
				nameColumn.append(s + "\n");
			}
			
			//Values of the characteristics of the player.
			StringBuilder valueColumn = new StringBuilder();
			
			for (String s : statsValue)
			{
				valueColumn.append(s + "\n");
			}
			
			//Stats of the player.
			StringBuilder stats = new StringBuilder();
			stats.append("HungerGames vinti: " + character.getWins() + "\n")
					.append("Uccisioni totali: " + character.getKills() + "\n");
			
			EmbedBuilder embedBuilder = new EmbedBuilder();
			embedBuilder.setTitle(character.getDisplayName());
			
			//Add characteristic table.
			embedBuilder.addField("Caratteristiche", nameColumn.toString(), true);
			embedBuilder.addField("", valueColumn.toString(), true);
			
			//Add player stats.
			embedBuilder.addField("Statistiche", stats.toString(), false);
			
			embedBuilder.setColor(0);
			embedBuilder.setThumbnail((owner != null) ? owner.getEffectiveAvatarUrl() : null);
			embedBuilder.setFooter("Creato da " + ownerName);
			
			channel.sendMessage((new MessageBuilder()).setEmbed(embedBuilder.build()).build()).queue();
		}
		else
		{
			channel.sendMessage("> L'utente non ha creato nessun personaggio.").queue();
		}
	}
	
	private void setCharacterEnabled(MessageChannel channel, User author, String[] args)
	{
		Character character = HungerGamesController.getCharacter(author.getId());
		MessageBuilder builder = new MessageBuilder();
		
		if (character != null)
		{
			boolean enable;
			
			switch (args[0])
			{
				case "true":
					enable = true;
					break;
				case "false":
					enable = false;
					break;
				default:
					builder.append("> L'argomento deve essere (true|false).");
					channel.sendMessage(builder.build()).queue();
					return;
			}
			
			builder.appendFormat("> **%s** è stato %s.", character.getDisplayName(), enable ? "abilitato" : "disabilitato");
			character.setEnabled(enable);
			HungerGamesController.save();
		}
		else
		{
			builder.append("> Non hai creato nessun personaggio. Crea il tuo personaggio prima.");
		}
		
		channel.sendMessage(builder.build()).queue();
	}
	
	private void setPlaybackSpeed(MessageChannel channel, String[] args)
	{
		StringBuilder builder = new StringBuilder();
		float playbackSpeed = Float.parseFloat(args[0]);
		
		if (playbackSpeed <= 1.0f)
		{
			builder.append("> Il valore di speed deve essere > 1");
		}
		else
		{
			HungerGamesController.setMessageSpeed(playbackSpeed * 1000);
			HungerGamesController.saveSettings();
			builder.append("> Velocità di riproduzione degli HungerGames settata a " + playbackSpeed + " secondi.");
		}
		
		channel.sendMessage(builder.toString()).queue();
	}
	
	private void sendLeaderboard(MessageChannel channel, String[] args)
	{
		String fieldName;
		
		EmbedBuilder embedBuilder = new EmbedBuilder();
		MessageBuilder messageBuilder = new MessageBuilder();
		StringBuilder names = new StringBuilder();
		StringBuilder values = new StringBuilder();
		
		ArrayList<Character> leaderboard = new ArrayList<>(HungerGamesController.getCharacters().values());
		
		switch (args[0])
		{
			case "wins":
				embedBuilder.setTitle("Classifica vittorie Hunger Games");
				leaderboard.sort((character1, character2) -> character2.getWins() - character1.getWins());
				
				for (Character character : leaderboard)
				{
					values.append("" + character.getWins() + "\n");
				}
				
				fieldName = "Vittorie";
				break;
			
			case "kills":
				embedBuilder.setTitle("Classifica uccisioni Hunger Games");
				leaderboard.sort((character1, character2) -> character2.getKills() - character1.getKills());
				
				for (Character character : leaderboard)
				{
					values.append("" + character.getKills() + "\n");
				}
				
				fieldName = "Uccisioni";
				break;
			
			default:
				messageBuilder.append("> L'argomento deve essere (wins|kills).");
				channel.sendMessage(messageBuilder.build()).queue();
				return;
		}
		
		for (Character character : leaderboard)
		{
			names.append(character.getDisplayName() + "\n");
		}
		
		embedBuilder.addField("Nome", names.toString(), true);
		embedBuilder.addField(fieldName, values.toString(), true);
		messageBuilder.setEmbed(embedBuilder.build());
		
		channel.sendMessage(messageBuilder.build()).queue();
	}
	
	private void stopHungerGames(MessageChannel channel, User author)
	{
		MessageBuilder builder = new MessageBuilder();
		
		if (HungerGamesController.isHungerGamesRunning())
		{
			HungerGamesController.stopHungerGames();
			
			builder.appendFormat("> Gli HungerGames sono stati fermati da %s.", author.getName());
		}
		else
		{
			builder.appendFormat("> Nessun HungerGames in esecuzione.");
		}
		
		channel.sendMessage(builder.build()).queue();
	}
	
	private boolean stopIfHungerGamesIsRunning(MessageChannel channel)
	{
		//TODO: Find more elegant way.
		if (HungerGamesController.isHungerGamesRunning())
		{
			MessageBuilder builder = new MessageBuilder();
			builder.append("> Non puoi usare questo comando mentre è in corso un HungerGames.");
			channel.sendMessage(builder.build()).queue();
		}
		
		return !HungerGamesController.isHungerGamesRunning();
	}
}
