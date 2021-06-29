package com.serpenssolida.discordbot.module.hungergames;

import com.serpenssolida.discordbot.BotMain;
import com.serpenssolida.discordbot.module.BotCommand;
import com.serpenssolida.discordbot.module.BotListener;
import com.serpenssolida.discordbot.module.hungergames.task.CreateCharacterTask;
import com.serpenssolida.discordbot.module.hungergames.task.EditCharacterTask;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
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
		Character character;
		OptionMapping tagArg = event.getOption("tag");
		
		if (tagArg == null)
		{
			character = HungerGamesController.getCharacter(guild.getId(), author.getId());
		}
		else
		{
			User user = tagArg.getAsUser();
			character = HungerGamesController.getCharacter(guild.getId(), user.getId());
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
			
			event.reply(new MessageBuilder().setEmbed(embedBuilder.build()).build()).setEphemeral(false).queue();
		}
		else
		{
			Message message = BotListener.buildSimpleMessage("Creazione personaggio", author, "L'utente non ha creato nessun personaggio.");
			event.reply(message).setEphemeral(true).queue();
		}
	}
	
	private void setCharacterEnabled(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		Character character = HungerGamesController.getCharacter(guild.getId(), author.getId());
		OptionMapping valueArg = event.getOption("value");
		
		EmbedBuilder embedBuilder = BotMain.getDefaultEmbed("Attivazione/disattivazione personaggio", author);
		
		//This command cannot be used while HungerGames is running.
		if (HungerGamesController.isHungerGamesRunning(guild.getId()))
		{
			Message message = BotListener.buildSimpleMessage("Attivazione/disattivazione personaggio", author, "Non puoi usare questo comando mentre è in corso un HungerGames.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		if (character != null)
		{
			if (valueArg != null)
			{
				boolean enable = valueArg.getAsBoolean();
				
				embedBuilder.setDescription("**" + character.getDisplayName() + "** è stato " + (enable ? "abilitato." : "disabilitato."));
				character.setEnabled(enable);
				HungerGamesController.save(guild.getId());
			}
			else
			{
				embedBuilder.setDescription("L'argomento deve essere (true|false).");
			}
		}
		else
		{
			embedBuilder.setDescription("Nessun personaggio trovato, crea il tuo personaggio.");
		}
		
		MessageBuilder messageBuilder = new MessageBuilder()
				.setEmbed(embedBuilder.build());
		event.reply(messageBuilder.build()).setEphemeral(character == null || valueArg == null ).queue();
	}
	
	private void setPlaybackSpeed(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping secondsArg = event.getOption("seconds");
		
		EmbedBuilder embedBuilder = BotMain.getDefaultEmbed("Velocità degli Hunger Games", author);
		
		if (secondsArg != null && secondsArg.getAsLong() >= 1.0f)
		{
			float playbackSpeed = secondsArg.getAsLong();
			
			HungerGamesController.setMessageSpeed(guild.getId(), playbackSpeed * 1000);
			HungerGamesController.saveSettings(guild.getId());
			embedBuilder.setDescription("Velocità di riproduzione degli HungerGames settata a " + playbackSpeed + " secondi.");
		}
		else
		{
			embedBuilder.setDescription("L'argomento seconds non è stato inviato oppure il suo valore è minore di 1 secondo.");
		}
		
		MessageBuilder messageBuilder = new MessageBuilder()
				.setEmbed(embedBuilder.build());
		
		event.reply(messageBuilder.build()).setEphemeral(secondsArg == null || secondsArg.getAsLong() < 1.0f).queue();
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
		//MessageBuilder messageBuilder = new MessageBuilder();
		EmbedBuilder embedBuilder = BotMain.getDefaultEmbed("Hunger Games", author);
		
		boolean isRunning = HungerGamesController.isHungerGamesRunning(guild.getId());
		
		if (isRunning)
		{
			HungerGamesController.stopHungerGames(guild.getId());
			
			embedBuilder.appendDescription("Gli HungerGames sono stati fermati da " + author.getName() + ".");
		}
		else
		{
			embedBuilder.appendDescription("Nessun HungerGames in esecuzione.");
		}
		
		MessageBuilder messageBuilder = new MessageBuilder()
				.setEmbed(embedBuilder.build());
		
		event.reply(messageBuilder.build()).setEphemeral(!isRunning).queue();
	}
}
