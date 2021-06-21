package com.serpenssolida.discordbot.module.hungergames;

import com.serpenssolida.discordbot.BotMain;
import com.serpenssolida.discordbot.module.BotCommand;
import com.serpenssolida.discordbot.module.BotListener;
import com.serpenssolida.discordbot.module.UnlistedBotCommand;
import com.serpenssolida.discordbot.module.hungergames.task.CreateCharacterTask;
import com.serpenssolida.discordbot.module.hungergames.task.EditCharacterTask;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;

public class HungerGamesListener extends BotListener
{
	public HungerGamesListener()
	{
		super("hg");
		this.setModuleName("HungerGames");
		
		UnlistedBotCommand unlistedBotCommand = new UnlistedBotCommand("ping", 0);
		unlistedBotCommand.setAction((guild, channel, message, author, args) ->
		{
			message.reply("pong").queue();
			return true;
		});
		this.addUnlistedBotCommand(unlistedBotCommand);
		
		//Command for creating a character.
		BotCommand command = new BotCommand("create", "Fa partire la procedura per la creazione di un personaggio.");
		command.setAction((event, guild, channel, author) ->
		{
			this.startTask(guild.getId(), new CreateCharacterTask(guild, author, channel), event);
			return true;
		});
		this.addBotCommand(command);
		
		//Command for displaying character info.
		command = new BotCommand("character", "Invia alla chat la card delle statistiche del personaggio.");
		command.setAction((event, guild, channel, author) ->
		{
			this.sendCharacterCard(event, guild, channel, author);
			return true;
		});
		command.getSubcommand()
				.addOption(OptionType.USER, "tag", "L'utente di cui visualizzare le statistiche.", false);
		this.addBotCommand(command);
		
		//Command for editing a character.
		command = new BotCommand("edit", "Fa partire la procedura di modifica del personaggio.");
		command.setAction((event, guild, channel, author) ->
		{
			this.startTask(guild.getId(), new EditCharacterTask(guild, author, channel), event);
			return true;
		});
		this.addBotCommand(command);
		
		//Command for enabling or disabling a character.
		command = new BotCommand("enable", "Abilita/Disabilita il personaggio. Un personaggio disabilitato non parteciperà agli HungerGames.");
		command.setAction((event, guild, channel, author) ->
		{
			this.setCharacterEnabled(event, guild, channel, author);
			return true;
		});
		command.getSubcommand()
				.addOption(OptionType.BOOLEAN, "value", "True per abilitare il personaggio, false per disabilitarlo.", true);
		this.addBotCommand(command);
		
		//Command for starting a new HungerGames.
		command = new BotCommand("start", "Inizia un edizione degli Hunger Games!");
		command.setAction((event, guild, channel, author) ->
		{
			event.reply("> L'hunger games sta partendo!").queue();
			HungerGamesController.startHungerGames(guild.getId(), channel);
			return true;
		});
		this.addBotCommand(command);
		
		//Command for editing playback speed of the HungerGames.
		command = new BotCommand("speed", "Modifica la velocità di riproduzione degli Hunger Games (velocità minima 1 secondo).");
		command.setAction((event, guild, channel, author) ->
		{
			this.setPlaybackSpeed(event, guild, channel, author);
			return true;
		});
		command.getSubcommand()
				.addOption(OptionType.INTEGER, "seconds", "Numero di secondi tra un messaggio e l'altro (min 1). ", true);
		this.addBotCommand(command);
		
		//Command for displaying leaderboards of the Hunger Games.
		command = new BotCommand("leaderboard", "Visualizza le classifiche degli HungerGames.");
		command.setAction((event, guild, channel, author) ->
		{
			this.sendLeaderboard(event, guild, channel, author);
			return true;
		});
		command.getSubcommand()
				.addOptions(
						new OptionData(OptionType.STRING, "type", "Il tipo di leaderboard da mostrare.", true)
								.addChoices(new Command.Choice("wins", "wins"), new Command.Choice("kills", "kills"))
				);
		this.addBotCommand(command);
		
		//Command for displaying leaderboards of the Hunger Games.
		command = new BotCommand("stop", "Interrompe l'esecuzione degli HungerGames.");
		command.setAction((event, guild, channel, author) ->
		{
			this.stopHungerGames(event, guild, channel, author);
			return true;
		});
		this.addBotCommand(command);
	}
	
	private void sendCharacterCard(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		Character character = null;
		OptionMapping tagArg = event.getOption("tag");
		
		if (tagArg == null)
		{
			character = HungerGamesController.getCharacter(guild.getId(), author.getId());
		}
		else
		{
			//ArrayList<Member> members = BotMain.findUsersByName(guild, args[0]);
			//List<User> taggedUsers = message.getMentionedUsers();
			User user = tagArg.getAsUser();
			character = HungerGamesController.getCharacter(guild.getId(), user.getId());
			/*if (members.isEmpty())
			{
				//Try using tag.
				if (taggedUsers.isEmpty())
				{
					MessageBuilder builder = new MessageBuilder()
							.appendFormat("> L'utente con il nome/nickname `%s` non esiste oppure non è stato salvato dentro la cache, prova a usare il tag.", args[0]);
					
					channel.sendMessage(builder.build()).queue();
					
					return;
				}
				else
				{
					character = HungerGamesController.getCharacter(guild.getId(), taggedUsers.get(0).getId());
				}
				
			}
			else if (members.size() > 1)
			{
				MessageBuilder builder = new MessageBuilder()
						.appendFormat("> L'utente con il nome/nickname %s non esiste oppure non è stato salvato dentro la cache, prova a usare il tag.", args[0]);
				
				channel.sendMessage(builder.build()).queue();
			}
			else
			{
				character = HungerGamesController.getCharacter(guild.getId(), members.get(0).getId());
			}*/
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
			StringBuilder stats = new StringBuilder()
					.append("HungerGames vinti: " + character.getWins() + "\n")
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
			
			event.reply((new MessageBuilder()).setEmbed(embedBuilder.build()).build()).setEphemeral(false).queue();
		}
		else
		{
			event.reply("> L'utente non ha creato nessun personaggio.").setEphemeral(false).queue();
		}
	}
	
	private void setCharacterEnabled(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		Character character = HungerGamesController.getCharacter(guild.getId(), author.getId());
		MessageBuilder builder = new MessageBuilder();
		OptionMapping valueArg = event.getOption("value");
		//This command cannot be used while HungerGames is running.
		if (HungerGamesController.isHungerGamesRunning(guild.getId()))
		{
			builder = new MessageBuilder();
			builder.append("> Non puoi usare questo comando mentre è in corso un HungerGames.");
			event.reply(builder.build()).queue();
			return;
		}
		
		if (character != null)
		{
			
			if (valueArg != null)
			{
				boolean enable = valueArg.getAsBoolean();
				
				builder.appendFormat("> **%s** è stato %s.", character.getDisplayName(), enable ? "abilitato" : "disabilitato");
				character.setEnabled(enable);
				HungerGamesController.save(guild.getId());
			}
			else
			{
				builder.append("> L'argomento deve essere (true|false).");
			}
		}
		else
		{
			builder.append("> Nessun personaggio trovato, crea il tuo personaggio.");
		}
		
		event.reply(builder.build()).setEphemeral(character == null || valueArg == null ).queue();
	}
	
	private void setPlaybackSpeed(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		StringBuilder builder = new StringBuilder();
		OptionMapping secondsArg = event.getOption("seconds");
		
		if (secondsArg != null && secondsArg.getAsLong() >= 1.0f)
		{
			float playbackSpeed = secondsArg.getAsLong();
			
			HungerGamesController.setMessageSpeed(guild.getId(), playbackSpeed * 1000);
			HungerGamesController.saveSettings(guild.getId());
			builder.append("> Velocità di riproduzione degli HungerGames settata a " + playbackSpeed + " secondi.");
		}
		else
		{
			builder.append("> L'argomento seconds non è stato inviato oppure il suo valore è minore di 1 secondo.");
		}
		
		event.reply(builder.toString()).setEphemeral(secondsArg == null || secondsArg.getAsLong() < 1.0f).queue();
	}
	
	private void sendLeaderboard(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		String fieldName = "Bug";
		
		EmbedBuilder embedBuilder = new EmbedBuilder();
		MessageBuilder messageBuilder = new MessageBuilder();
		StringBuilder names = new StringBuilder();
		StringBuilder values = new StringBuilder();
		
		ArrayList<Character> leaderboard = new ArrayList<>(HungerGamesController.getCharacters(guild.getId()).values());
		OptionMapping typeArg = event.getOption("type");
		
		if (typeArg != null)
		{
			switch (typeArg.getAsString())
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
			}
		}
		else
		{
			messageBuilder.append("> L'argomento deve essere (wins|kills).");
			event.reply(messageBuilder.build()).setEphemeral(false).queue();
			return;
		}
		
		for (Character character : leaderboard)
		{
			names.append(character.getDisplayName() + "\n");
		}
		
		embedBuilder.addField("Nome", names.toString(), true);
		embedBuilder.addField(fieldName, values.toString(), true);
		messageBuilder.setEmbed(embedBuilder.build());
		
		event.reply(messageBuilder.build()).setEphemeral(true).queue();
	}
	
	private void stopHungerGames(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		MessageBuilder builder = new MessageBuilder();
		boolean isRunning = HungerGamesController.isHungerGamesRunning(guild.getId());
		
		if (isRunning)
		{
			HungerGamesController.stopHungerGames(guild.getId());
			
			builder.appendFormat("> Gli HungerGames sono stati fermati da %s.", author.getName());
		}
		else
		{
			builder.appendFormat("> Nessun HungerGames in esecuzione.");
		}
		
		event.reply(builder.build()).setEphemeral(!isRunning).queue();
	}
}
