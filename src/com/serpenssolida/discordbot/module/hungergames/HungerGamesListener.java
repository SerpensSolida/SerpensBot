package com.serpenssolida.discordbot.module.hungergames;

import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.SerpensBot;
import com.serpenssolida.discordbot.command.BotCommand;
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
				this.startTask(guild.getId(), new CreateCharacterTask(guild, author, channel), event));
		this.addBotCommand(command);
		
		//Command for displaying character info.
		command = new BotCommand("character", "Invia alla chat la card delle statistiche del personaggio.");
		command.setAction(this::sendCharacterCard);
		command.getSubcommand()
				.addOption(OptionType.USER, "tag", "L'utente di cui visualizzare le statistiche.", false);
		this.addBotCommand(command);
		
		//Command for editing a character.
		command = new BotCommand("edit", "Fa partire la procedura di modifica del personaggio.");
		command.setAction((event, guild, channel, author) ->
				this.startTask(guild.getId(), new EditCharacterTask(guild, author, channel), event));
		this.addBotCommand(command);
		
		//Command for enabling or disabling a character.
		command = new BotCommand("enable", "Abilita/Disabilita il personaggio. Un personaggio disabilitato non parteciperà agli HungerGames.");
		command.setAction(this::setCharacterEnabled);
		command.getSubcommand()
				.addOption(OptionType.BOOLEAN, "value", "True per abilitare il personaggio, false per disabilitarlo.", true);
		this.addBotCommand(command);
		
		//Command for starting a new HungerGames.
		command = new BotCommand("start", "Inizia un edizione degli Hunger Games!");
		command.setAction(HungerGamesListener::startHungerGames);
		this.addBotCommand(command);
		
		//Command for editing playback speed of the HungerGames.
		command = new BotCommand("speed", "Modifica la velocità di riproduzione degli Hunger Games (velocità minima 1 secondo).");
		command.setAction(this::setPlaybackSpeed);
		command.getSubcommand()
				.addOption(OptionType.INTEGER, "seconds", "Numero di secondi tra un messaggio e l'altro (min 1). ", true);
		this.addBotCommand(command);
		
		//Command for displaying leaderboards of the Hunger Games.
		command = new BotCommand("leaderboard", "Visualizza le classifiche degli HungerGames.");
		command.setAction(this::sendLeaderboard);
		command.getSubcommand()
				.addOptions(
						new OptionData(OptionType.STRING, "type", "Il tipo di leaderboard da mostrare.", true)
								.addChoices(new Command.Choice("wins", "wins"), new Command.Choice("kills", "kills"))
				);
		this.addBotCommand(command);
		
		//Command for displaying leaderboards of the Hunger Games.
		command = new BotCommand("stop", "Interrompe l'esecuzione degli HungerGames.");
		command.setAction(this::stopHungerGames);
		this.addBotCommand(command);
	}
	
	private static void startHungerGames(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		//Check if no Hunger Games is running.
		if (HungerGamesController.isHungerGamesRunning(guild.getId()))
		{
			Message message = MessageUtils.buildErrorMessage("Hunger Games", author, "Non puoi usare questo comando mentre è in corso un HungerGames.");
			event.reply(message).queue();
			return;
		}
		
		//Start the Hunger Games.
		HungerGamesController.startHungerGames(guild.getId(), channel, author);
		
		//Send message info.
		event.reply(MessageUtils.buildSimpleMessage("Hunger Games", author, "Gli Hunger Games stanno per iniziare!")).queue();
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
		
		//Check if a character was found.
		if (character == null)
		{
			Message message = MessageUtils.buildErrorMessage("Creazione personaggio", author, "L'utente non ha creato nessun personaggio.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		User owner = SerpensBot.api.retrieveUserById(character.getOwnerID()).complete();
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
		
		//Add the embed to the message.
		MessageBuilder messageBuilder = new MessageBuilder();
		messageBuilder.setEmbeds(embedBuilder.build());
		
		event.reply(messageBuilder.build()).setEphemeral(false).queue();
	}
	
	private void setCharacterEnabled(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		Character character = HungerGamesController.getCharacter(guild.getId(), author.getId());
		OptionMapping valueArg = event.getOption("value");
		
		//This command cannot be used while HungerGames is running.
		if (HungerGamesController.isHungerGamesRunning(guild.getId()))
		{
			Message message = MessageUtils.buildErrorMessage("Attivazione/disattivazione personaggio", author, "Non puoi usare questo comando mentre è in corso un HungerGames.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if the user has a character.
		if (character == null)
		{
			Message message = MessageUtils.buildErrorMessage("Attivazione/disattivazione personaggio", author, "Nessun personaggio trovato, crea il tuo personaggio.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check the argument.
		if (valueArg == null)
		{
			Message message = MessageUtils.buildErrorMessage("Attivazione/disattivazione personaggio", author, "L'argomento deve essere (true|false).");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Enable character.
		character.setEnabled(valueArg.getAsBoolean());
		HungerGamesController.save(guild.getId());
		
		//Send info message.
		String embedDescrition = "**" + character.getDisplayName() + "** è stato " + (valueArg.getAsBoolean() ? "abilitato." : "disabilitato.");
		Message message = MessageUtils.buildSimpleMessage("Attivazione/disattivazione personaggio", author, embedDescrition);
		
		event.reply(message).setEphemeral(false).queue();
	}
	
	private void setPlaybackSpeed(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping secondsArg = event.getOption("seconds");
		
		//Check argument format.
		if (secondsArg == null || secondsArg.getAsLong() < 1.0f)
		{
			Message message = MessageUtils.buildErrorMessage("Velocità degli Hunger Games", author, "L'argomento seconds non è stato inviato oppure il suo valore è minore di 1 secondo.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}

		//Set the playback speed.
		float playbackSpeed = secondsArg.getAsLong();
		HungerGamesController.setMessageSpeed(guild.getId(), playbackSpeed * 1000);
		HungerGamesController.saveSettings(guild.getId());
		
		//Send info message.
		Message message = MessageUtils.buildSimpleMessage("Velocità degli Hunger Games", author, "Velocità di riproduzione degli HungerGames settata a " + playbackSpeed + " secondi.");
		event.reply(message).setEphemeral(false).queue();
	}
	
	private void sendLeaderboard(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping typeArg = event.getOption("type");
		String fieldName;
		
		//Check argument.
		if (typeArg == null)
		{
			Message message = MessageUtils.buildErrorMessage("Classifiche Hunger Games", author, "L'argomento deve essere (wins|kills).");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		EmbedBuilder embedBuilder = new EmbedBuilder();
		
		StringBuilder names = new StringBuilder();
		StringBuilder values = new StringBuilder();
		ArrayList<Character> leaderboard = new ArrayList<>(HungerGamesController.getCharacters(guild.getId()).values());
		
		//Get leaderboard values.
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
				
			default:
				Message message = MessageUtils.buildErrorMessage("Classifiche Hunger Games", author, "Sei riuscito a mettere un argomento invalido. Bravo.");
				event.reply(message).setEphemeral(true).queue();
				return;
		}
		
		//Get leaderboard names.
		for (Character character : leaderboard)
		{
			names.append(character.getDisplayName() + "\n");
		}
		
		//Add the field to the embed.
		embedBuilder.addField("Nome", names.toString(), true);
		embedBuilder.addField(fieldName, values.toString(), true);
		
		//Send the leaderboard.
		MessageBuilder messageBuilder = new MessageBuilder();
		messageBuilder.setEmbeds(embedBuilder.build());
		
		event.reply(messageBuilder.build()).setEphemeral(false).queue();
	}
	
	private void stopHungerGames(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		boolean isRunning = HungerGamesController.isHungerGamesRunning(guild.getId());
		
		//Check if there is a Hunger Games running.
		if (!isRunning)
		{
			Message message = MessageUtils.buildErrorMessage("Hunger Games", author, "Nessun HungerGames in esecuzione.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Stop the currently running hunger games.
		HungerGamesController.stopHungerGames(guild.getId());
		
		//Send message info.
		Message message = MessageUtils.buildSimpleMessage("Hunger Games", author, "Gli HungerGames sono stati fermati da " + author.getName() + ".");
		event.reply(message).setEphemeral(false).queue();
	}
}
