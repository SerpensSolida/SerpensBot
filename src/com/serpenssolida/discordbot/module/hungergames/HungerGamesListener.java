package com.serpenssolida.discordbot.module.hungergames;

import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.SerpensBot;
import com.serpenssolida.discordbot.command.BotCommand;
import com.serpenssolida.discordbot.interaction.InteractionCallback;
import com.serpenssolida.discordbot.interaction.InteractionGroup;
import com.serpenssolida.discordbot.modal.ModalCallback;
import com.serpenssolida.discordbot.module.BotListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class HungerGamesListener extends BotListener
{
	private static final Logger logger = LoggerFactory.getLogger(HungerGamesListener.class);
	
	public HungerGamesListener()
	{
		super("hg");
		this.setModuleName("HungerGames");
		
		//Command for creating a character.
		BotCommand command = new BotCommand("character", "Fa partire la procedura per la creazione/modifica del personaggio.");
		command.setAction(this::createCharacter);
		this.addBotCommand(command);
		
		//Command for displaying character info.
		command = new BotCommand("card", "Invia alla chat la card delle statistiche del personaggio.");
		command.setAction(this::sendCharacterCard);
		command.getSubcommand()
				.addOption(OptionType.USER, "tag", "L'utente di cui visualizzare le statistiche.", false);
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
	
	/**
	 * Callback for "character" command.
	 */
	private void createCharacter(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
	{
		if (HungerGamesController.isHungerGamesRunning(guild.getId()))
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Forum channel", author, "Non puoi usare questo comando perchè è in corso un HungerGames.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		Character character = HungerGamesController.getCharacter(guild.getId(), author.getId());
		
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed(character == null ? "Creazione del personaggio" : "Modifica del personaggio", author)
				.appendDescription("Stai " + (character == null ? "creando" : "modificando") +" il tuo personaggio. Ti verrà inviato un modulo da compilare con il nome e le caratteristiche del personnaggio")
				.appendDescription("\nLe caratteristiche sono: ")
				.appendDescription("**V**italità, **F**orza, **A**bilità, **S**pecial, **Ve**locità, **R**esistenza e **G**usto. ")
				.appendDescription("Per impostare correttamente le caratteristiche devi inserire 7 numeri separati da uno spazio, in questo modo: **V F A S Ve R G** (Es: 10 5 5 10 4 1 5)")
				.appendDescription("\nLa somma dei valori delle caratteristiche deve essere " + HungerGamesController.SUM_STATS + " punti e ogni caratteristica deve essere compresa tra 0 e 10!");
		
		MessageCreateBuilder messageBuilder = new MessageCreateBuilder()
				.setEmbeds(embedBuilder.build())
				.addActionRow(List.of(Button.primary("continue", "Continua"), Button.danger("abort", "Annulla")));
		
		InteractionGroup interactionGroup = this.generateCharacterTaskInteractionGroup();
		
		InteractionHook hook = event.reply(messageBuilder.build()).setEphemeral(true).complete();
		Message message = hook.retrieveOriginal().complete();
		
		this.addInteractionGroup(guild.getId(), message.getId(), interactionGroup);
	}
	
	/**
	 * Callback for "start" command.
	 */
	private static void startHungerGames(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
	{
		//Check if no Hunger Games is running.
		if (HungerGamesController.isHungerGamesRunning(guild.getId()))
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Hunger Games", author, "Non puoi usare questo comando mentre è in corso un HungerGames.");
			event.reply(message).queue();
			return;
		}
		
		//Start the Hunger Games.
		HungerGamesController.startHungerGames(guild.getId(), channel, author);
		
		//Send message info.
		event.reply(MessageUtils.buildSimpleMessage("Hunger Games", author, "Gli Hunger Games stanno per iniziare!")).queue();
	}
	
	/**
	 * Callback for "card" command.
	 */
	private void sendCharacterCard(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
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
			MessageCreateData message = MessageUtils.buildErrorMessage("Card personaggio", author, "L'utente non ha creato nessun personaggio.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		User owner = SerpensBot.getApi().retrieveUserById(character.getOwnerID()).complete();
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
			nameColumn.append(s + "\n");
		
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
		MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
		messageBuilder.setEmbeds(embedBuilder.build());
		
		event.reply(messageBuilder.build()).setEphemeral(false).queue();
	}
	
	/**
	 * Callback for "enable" command.
	 */
	private void setCharacterEnabled(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
	{
		Character character = HungerGamesController.getCharacter(guild.getId(), author.getId());
		OptionMapping valueArg = event.getOption("value");
		
		//This command cannot be used while HungerGames is running.
		if (HungerGamesController.isHungerGamesRunning(guild.getId()))
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Attivazione/disattivazione personaggio", author, "Non puoi usare questo comando mentre è in corso un HungerGames.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if the user has a character.
		if (character == null)
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Attivazione/disattivazione personaggio", author, "Nessun personaggio trovato, crea il tuo personaggio.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check the argument.
		if (valueArg == null)
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Attivazione/disattivazione personaggio", author, "L'argomento deve essere (true|false).");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Enable character.
		character.setEnabled(valueArg.getAsBoolean());
		HungerGamesController.save(guild.getId());
		
		//Send info message.
		String embedDescrition = "**" + character.getDisplayName() + "** è stato " + (valueArg.getAsBoolean() ? "abilitato." : "disabilitato.");
		MessageCreateData message = MessageUtils.buildSimpleMessage("Attivazione/disattivazione personaggio", author, embedDescrition);
		
		event.reply(message).setEphemeral(false).queue();
	}
	
	/**
	 * Callback for "speed" command.
	 */
	private void setPlaybackSpeed(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping secondsArg = event.getOption("seconds");
		
		//Check argument format.
		if (secondsArg == null || secondsArg.getAsLong() < 1.0f)
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Velocità degli Hunger Games", author, "L'argomento seconds non è stato inviato oppure il suo valore è minore di 1 secondo.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}

		//Set the playback speed.
		float playbackSpeed = secondsArg.getAsLong();
		HungerGamesController.setMessageSpeed(guild.getId(), playbackSpeed * 1000);
		HungerGamesController.saveSettings(guild.getId());
		
		//Send info message.
		MessageCreateData message = MessageUtils.buildSimpleMessage("Velocità degli Hunger Games", author, "Velocità di riproduzione degli HungerGames settata a " + playbackSpeed + " secondi.");
		event.reply(message).setEphemeral(false).queue();
	}
	
	/**
	 * Callback for "leaderboard" command.
	 */
	private void sendLeaderboard(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping typeArg = event.getOption("type");
		String fieldName;
		
		//Check argument.
		if (typeArg == null)
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Classifiche Hunger Games", author, "L'argomento deve essere (wins|kills).");
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
				MessageCreateData message = MessageUtils.buildErrorMessage("Classifiche Hunger Games", author, "Sei riuscito a mettere un argomento invalido. Bravo.");
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
		MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
		messageBuilder.setEmbeds(embedBuilder.build());
		
		event.reply(messageBuilder.build()).setEphemeral(false).queue();
	}
	
	/**
	 * Callback for "stop" command.
	 */
	private void stopHungerGames(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
	{
		boolean isRunning = HungerGamesController.isHungerGamesRunning(guild.getId());
		
		//Check if there is a Hunger Games running.
		if (!isRunning)
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Hunger Games", author, "Nessun HungerGames in esecuzione.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Stop the currently running hunger games.
		HungerGamesController.stopHungerGames(guild.getId());
		
		//Send message info.
		MessageCreateData message = MessageUtils.buildSimpleMessage("Hunger Games", author, "Gli HungerGames sono stati fermati da " + author.getName() + ".");
		event.reply(message).setEphemeral(false).queue();
	}
	
	/**
	 * Parses stats and check their format.
	 * @param stats
	 * 		{@link String} containing the stats to be parsed and checked.
	 *
	 * @return
	 * 		An array containing the stats.
	 *
	 * @throws StatsExceedMaximunException
	 * 		When the sum of stats is greater that the maximum allowed.
	 * @throws StatOutOfRangeException
	 * 		When a single stats is out of range.
	 * @throws WrongNumberOfStatsException
	 * 		When there was an error while parsing a stat.
	 */
	private static int[] checkStats(String stats) throws StatsExceedMaximunException, StatOutOfRangeException, WrongNumberOfStatsException
	{
		String[] abilitiesList = stats.strip().replaceAll(" +", " ").split(" ");
		int[] abilities = new int[abilitiesList.length];
		int sum = 0;
		
		//Check number of ability sent with the message.
		if (abilities.length != 7)
			throw new WrongNumberOfStatsException("Le caratteristiche devono essere 7.");
		
		//Check abilities format.
		for (int i = 0; i < abilitiesList.length; i++)
		{
			abilities[i] = Integer.parseInt(abilitiesList[i]);
			sum += abilities[i];
			
			if (abilities[i] < 0 || abilities[i] > 10)
				throw new StatOutOfRangeException("le caratteristiche devono essere tra 0 e 10.");
		}
		
		if (sum != HungerGamesController.SUM_STATS)
			throw new StatsExceedMaximunException("La somma delle statistiche non può superare: " + HungerGamesController.SUM_STATS, sum);
		
		return abilities;
	}
	
	/**
	 * Generate the {@link InteractionGroup} for the create/edit task.
	 * @return
	 */
	private InteractionGroup generateCharacterTaskInteractionGroup()
	{
		InteractionGroup interactionGroup = new InteractionGroup();
		interactionGroup.addButtonCallback("continue", (buttonInteractionEvent, guild, messageChannel, message, author) ->
		{
			Character character = HungerGamesController.getCharacter(guild.getId(), author.getId());
			
			//Create and send the modal.
			Modal modal = this.generateCharacterTaskModal(character);
			buttonInteractionEvent.replyModal(modal).queue();
			this.addModalCallback(guild.getId(), author.getId(), this.generateCharacterTaskModalCallback(message));
			return InteractionCallback.LEAVE_MESSAGE;
		});
		interactionGroup.addButtonCallback("abort", (buttonInteractionEvent, guild1, messageChannel, message, user) ->
		{
			buttonInteractionEvent.editComponents(List.of()).queue();
			return InteractionCallback.LEAVE_MESSAGE;
		});
		
		return interactionGroup;
	}
	
	/**
	 * Generate the modal for the create/edit character task.
	 *
	 * @param character
	 * 		The {@link Character} of the user.
	 *
	 * @return
	 * 		The {@link Modal} generated.
	 */
	private Modal generateCharacterTaskModal(Character character)
	{
		String characterName = character != null ? character.getName() : null;
		String characterStats = character != null ? String.join(" ", "" + character.getVitality(), "" + character.getStrength(), "" + character.getAbility(), "" + character.getSpecial(), "" + character.getSpeed(), "" + character.getEndurance(), "" + character.getTaste()) : null;
		
		//Create the fields.
		TextInput name = TextInput.create("character_name", "Nome del personaggio.", TextInputStyle.SHORT)
				.setMaxLength(16)
				.setValue(characterName)
				.build();
		TextInput stats = TextInput.create("stats", "Statistiche, V F A S Ve R G", TextInputStyle.SHORT)
				.setMaxLength(100)
				.setValue(characterStats)
				.setPlaceholder("Vitalità Forza Abilità Special Velocità Resistenza Gusto")
				.build();
		
		//Create modal.
		return Modal.create("create_character", "Creazione pesonaggio")
				.addActionRows(ActionRow.of(name), ActionRow.of(stats))
				.build();
	}
	
	/**
	 * Generate the {@link ModalCallback} for the create/edit task.
	 *
	 * @param message
	 * 		The message of the task.
	 *
	 * @return
	 * 		The {@link ModalCallback} of the create/edit task.
	 */
	private ModalCallback generateCharacterTaskModalCallback(Message message)
	{
		return (event, guild, channel, author) ->
		{
			ModalMapping nameArg = event.getValue("character_name");
			ModalMapping statsArg = event.getValue("stats");
			
			String guildID = guild.getId();
			String authorID = author.getId();
			
			if (nameArg == null || statsArg == null)
				return;
			
			Character character = HungerGamesController.getCharacter(guildID, authorID);
			
			String name = nameArg.getAsString();
			String strStats = statsArg.getAsString();
			int[] abilities;
			
			EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed(character == null ? "Creazione del personaggio" : "Modifica del personaggio", author);
			
			try
			{
				abilities = HungerGamesListener.checkStats(strStats);
			}
			catch (StatsExceedMaximunException e)
			{
				embedBuilder.appendDescription("La somma dei valori delle caratteristiche deve essere " +  HungerGamesController.SUM_STATS + " punti! Somma dei valori inseriti: " + e.getSum());
				
				MessageEditBuilder messageBuilder = new MessageEditBuilder()
						.setEmbeds(embedBuilder.build())
						.setActionRow(List.of(Button.primary("continue", "Continua"), Button.danger("abort", "Annulla")));
				
				event.editMessage(messageBuilder.build()).queue();
				return;
			}
			catch (StatOutOfRangeException | NumberFormatException e)
			{
				embedBuilder.appendDescription("Formato delle caratteristiche errato. Inserisci solo numeri tra 0 e 10!");
				
				MessageEditBuilder messageBuilder = new MessageEditBuilder()
						.setEmbeds(embedBuilder.build())
						.setActionRow(List.of(Button.primary("continue", "Continua"), Button.danger("abort", "Annulla")));
				
				event.editMessage(messageBuilder.build()).queue();
				return;
			}
			catch (WrongNumberOfStatsException e)
			{
				embedBuilder.appendDescription("Inserisci tutte le caratteristiche!")
						.appendDescription("\nLe caratteristiche sono: ")
						.appendDescription("Vitalità, Forza, Abilità, Special, Velocità, Resistenza e Gusto. ")
						.appendDescription("\nLa somma dei valori delle caratteristiche deve essere " + HungerGamesController.SUM_STATS + " punti e ogni caratteristica deve essere compresa tra 0 e 10!");
				
				MessageEditBuilder messageBuilder = new MessageEditBuilder()
						.setEmbeds(embedBuilder.build())
						.setActionRow(List.of(Button.primary("continue", "Continua"), Button.danger("abort", "Annulla")));
				
				event.editMessage(messageBuilder.build()).queue();
				return;
			}
			
			if (character == null)
			{
				character = new Character(authorID);
				character.setName(name);
				character.setStats(abilities);
				HungerGamesController.addCharacter(guildID, character);
				
				embedBuilder.appendDescription("Creazione personaggio completata!");
			}
			else
			{
				character.setName(name);
				character.setStats(abilities);
				HungerGamesController.save(guildID);
				
				embedBuilder.appendDescription("Modifica personaggio completata!");
			}
			
			this.removeInteractionGroup(guild.getId(), message.getId());
			
			MessageCreateBuilder messageBuilder = new MessageCreateBuilder()
					.setEmbeds(embedBuilder.build());
			
			event.editComponents(List.of()).queue();
			channel.sendMessage(messageBuilder.build()).queue();
		};
	}
	
	private static class StatsExceedMaximunException extends Exception
	{
		private final int sum;
		
		public StatsExceedMaximunException(String message, int sum)
		{
			super(message);
			this.sum = sum;
		}
		
		private int getSum()
		{
			return this.sum;
		}
	}
	
	private static class StatOutOfRangeException extends Exception
	{
		public StatOutOfRangeException(String message)
		{
			super(message);
		}
	}
	
	private static class WrongNumberOfStatsException extends Exception
	{
		public WrongNumberOfStatsException(String message)
		{
			super(message);
		}
	}
}
