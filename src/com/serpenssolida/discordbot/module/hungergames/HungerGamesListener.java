package com.serpenssolida.discordbot.module.hungergames;

import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.SerpensBot;
import com.serpenssolida.discordbot.command.BotCommand;
import com.serpenssolida.discordbot.interaction.InteractionCallback;
import com.serpenssolida.discordbot.interaction.InteractionGroup;
import com.serpenssolida.discordbot.interaction.WrongInteractionEventException;
import com.serpenssolida.discordbot.module.BotListener;
import com.serpenssolida.discordbot.module.hungergames.task.CreateCharacterTask;
import com.serpenssolida.discordbot.module.hungergames.task.EditCharacterTask;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

public class HungerGamesListener extends BotListener
{
	private final HashMap<String, HashMap<User, Task>> tasks = new HashMap<>(); //List of task currently running.
	
	private static final Logger logger = LoggerFactory.getLogger(HungerGamesListener.class);
	
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
		
		//Command for cancelling a task.
		command = new BotCommand("cancel", SerpensBot.getMessage("Interrompe la task corrente."));
		command.setAction(this::cancelTask);
		this.addBotCommand(command);
	}
	
	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event)
	{
		//Don't accept messages from private channels.
		if (!event.isFromGuild())
			return;
		
		Guild guild = event.getGuild();
		User author = event.getAuthor(); //Author of the message.
		MessageChannel channel = event.getChannel(); //Channel where the message was sent.
		
		//Get the task the author is currently running.
		Task task = this.getTask(guild.getId(), author);
		
		//If the author of the message is the bot ignore the message.
		if (SerpensBot.getApi().getSelfUser().getId().equals(author.getId()))
			return;
		
		//Ignore the message if no task is found.
		if (task == null || !task.getChannel().equals(channel))
			return;
		
		//Pass the event to the task.
		task.consumeMessage(event.getMessage());
		
		//Remove the task if it finished.
		if (!task.isRunning())
			this.removeTask(guild.getId(), task);
	}
	
	@Override
	public void onGenericComponentInteractionCreate(@NotNull GenericComponentInteractionCreateEvent event)
	{
		super.onGenericComponentInteractionCreate(event);
		
		String componendId = event.getComponentId();
		User author = event.getUser(); //The user that added the reaction.
		Guild guild = event.getGuild(); //The user that added the reaction.
		
		//If this event is not from a guild ignore it.
		if (guild == null)
			return;
		
		//Ignore bot reaction.
		if (SerpensBot.getApi().getSelfUser().getId().equals(author.getId()))
			return;
		
		//Get the interacction that the user can interact with.
		InteractionGroup interactionGroup;
		
		//Task can have buttons too.
		Task task = this.getTask(guild.getId(), author);
		
		if (task != null)
		{
			interactionGroup = task.getInteractionGroup();
			InteractionCallback interactionCallback = interactionGroup.getComponentCallback(componendId);
			
			//Check if a callback was found.
			if (interactionCallback == null)
			{
				logger.warn(SerpensBot.getMessage("Nessuna callback per l'inetrazione \"{}\" nella task.", componendId));
				return;
			}
			
			try
			{
				//Do button action.
				boolean deleteMessage = interactionCallback.doAction(event);
				
				//Delete message that has the clicked button if it should be deleted.
				if (deleteMessage)
				{
					event.getHook().deleteOriginal().queue();
				}
				
				//Remove the task if it finished.
				if (!task.isRunning())
				{
					this.removeTask(guild.getId(), task);
				}
			}
			catch (WrongInteractionEventException e)
			{
				//Abort task.
				this.removeTask(guild.getId(), task);
				
				//Send error message.
				String embedTitle = SerpensBot.getMessage("botlistener_button_action_error");
				String embedDescription = SerpensBot.getMessage("botlistener_interaction_event_type_error", e.getInteractionId(), e.getExpected(), e.getFound());
				Message message = MessageUtils.buildErrorMessage(embedTitle, event.getUser(), embedDescription);
				event.reply(message).setEphemeral(true).queue();
				
				//Log the error.
				logger.error(e.getLocalizedMessage(), e);
			}
			catch (PermissionException e)
			{
				//Abort task.
				this.removeTask(guild.getId(), task);
				
				//Send error message.
				String embedTitle = SerpensBot.getMessage("botlistener_button_action_error");
				String embedDescription = SerpensBot.getMessage("botlistener_missing_permmision_error", e.getPermission());
				Message message = MessageUtils.buildErrorMessage(embedTitle, event.getUser(), embedDescription);
				event.reply(message).setEphemeral(true).queue();
				
				//Log the error.
				logger.error(e.getLocalizedMessage(), e);
			}
		}
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
	
	public Task getTask(String guildID, User user)
	{
		this.tasks.putIfAbsent(guildID, new HashMap<>());
		return this.tasks.get(guildID).get(user);
	}
	
	/**
	 * Start the given task in the given guild id.
	 *
	 * @param guildID
	 * 		The guild id the task is currently active.
	 * @param task
	 * 		The task that will be started.
	 * @param event
	 *  	The event that started the task.
	 */
	protected void startTask(String guildID, Task task, GenericInteractionCreateEvent event)
	{
		this.addTask(guildID, task);
		task.start(event);
	}
	
	/**
	 * Start the given task in the given guild id.
	 *
	 * @param guildID
	 * 		The guild id the task is currently active.
	 * @param task
	 * 		The task that will be started.
	 */
	protected void startTask(String guildID, Task task)
	{
		this.addTask(guildID, task);
		task.start();
	}
	
	protected void addTask(String guildID, Task task)
	{
		if (task.isInterrupted())
			return;
		
		User user = task.getUser();
		Task currentUserTask = this.getTask(guildID, user);
		
		//Replace the current task (if there is one) with the new one.
		if (currentUserTask != null)
			this.removeTask(guildID, currentUserTask);
		
		//Create the guild map if absent.
		this.tasks.putIfAbsent(guildID, new HashMap<>());
		
		this.tasks.get(guildID).put(user, task);
	}

	/**
	 * Method for the "cancel" command of a module. Based on the event data it will cancel a task.
	 */
	private void cancelTask(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		Task task = this.getTask(guild.getId(), author);
		
		//Check if the user has a task active.
		if (task == null)
		{
			Message message = MessageUtils.buildErrorMessage(SerpensBot.getMessage("botlistener_command_cancel_title"), event.getUser(), SerpensBot.getMessage("botlistener_command_cancel_no_task_error"));
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Remove the task
		this.removeTask(guild.getId(), this.getTask(guild.getId(), author));
		
		Message message = MessageUtils.buildSimpleMessage(SerpensBot.getMessage("botlistener_command_cancel_title"), author, SerpensBot.getMessage("botlistener_command_cancel_info"));
		event.reply(message).setEphemeral(false).queue();
	}
	
	protected void removeTask(String guildID, Task task)
	{
		this.tasks.putIfAbsent(guildID, new HashMap<>());
		
		this.tasks.get(guildID).remove(task.getUser());
	}
}
