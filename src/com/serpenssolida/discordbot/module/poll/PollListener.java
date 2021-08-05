package com.serpenssolida.discordbot.module.poll;

import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.command.BotCommand;
import com.serpenssolida.discordbot.interaction.InteractionCallback;
import com.serpenssolida.discordbot.interaction.InteractionGroup;
import com.serpenssolida.discordbot.module.BotListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;

public class PollListener extends BotListener
{
	private HashMap<String, Poll> polls = new HashMap<>();
	
	public PollListener()
	{
		super("poll");
		this.setModuleName("Poll");
		
		//This module has no task.
		this.removeBotCommand("cancel");
		
		//Command for creating a poll.
		BotCommand command = new BotCommand("create", "Crea un nuovo sondaggio");
		command.setAction(this::createNewPoll);
		SubcommandData subCommand = command.getSubcommand();
		subCommand
				.addOption(OptionType.STRING, "question", "La domanda del sondaggio", true)
				.addOption(OptionType.STRING, "option1", "Opzione del sondaggio", true)
				.addOption(OptionType.STRING, "option2", "Opzione del sondaggio", true);
		for (int i = 3; i <= 9; i++)
		{
			subCommand.addOption(OptionType.STRING, "option" + i, "Opzione del sondaggio", false);
		}
		subCommand.addOption(OptionType.INTEGER, "duration", "Durata in minuti del sondaggio. (default: 60 minuti)", false);
		this.addBotCommand(command);
		
		//Command for deleting a poll.
		command = new BotCommand("stop", "Ferma un sondaggio in corso.");
		command.setAction(this::removePoll);
		command.getSubcommand()
				.addOption(OptionType.STRING, "poll-id", "Identificatore univoco del sondaggio", true);
		this.addBotCommand(command);
		
		//Command for deleting a poll.
		command = new BotCommand("edit", "Modifica la descrizione di un sondaggio.");
		command.setAction(this::editPollDescription);
		command.getSubcommand()
				.addOption(OptionType.STRING, "poll-id", "Identificatore univoco del sondaggio", true)
				.addOption(OptionType.STRING, "description", "Nuova descrizione del sondaggio", true);
		this.addBotCommand(command);
		
		//Command for adding an option to the pool.
		command = new BotCommand("add", "Aggiunge un opzione al sondaggio.");
		command.setAction(this::addOption);
		command.getSubcommand()
				.addOption(OptionType.STRING, "poll-id", "Identificatore univoco del sondaggio", true)
				.addOption(OptionType.STRING, "description", "Testo dell'opzione da aggiungere", true);
				//.addOption(OptionType.STRING, "option-index", "Indice dell'opzione da rimuovere");
		this.addBotCommand(command);
		
		//Command for removing a vote from the pool.
		command = new BotCommand("remove-vote", "Rimuove il proprio voto da un sondaggio.");
		command.setAction(this::removeVote);
		command.getSubcommand()
				.addOption(OptionType.STRING, "poll-id", "Identificatore univoco del sondaggio", true);
		this.addBotCommand(command);
		
		//Command for removing an option from the pool.
		command = new BotCommand("remove-option", "Rimuove un opzione dal sondaggio.");
		command.setAction(this::removeOption);
		command.getSubcommand()
				.addOption(OptionType.STRING, "poll-id", "Identificatore univoco del sondaggio", true)
				.addOption(OptionType.STRING, "position", "Posizione dell'opzione da rimuovere (parte da 1)", true);
		this.addBotCommand(command);
	}
	
	private void createNewPoll(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping questionArg = event.getOption("question");
		OptionMapping durationArg = event.getOption("duration");
		
		//Null check for arguments.
		if (questionArg == null)
			return;
		
		//Get duration of the poll
		int duration = 60;
		if (durationArg != null)
			duration = (int) durationArg.getAsLong();
		
		//Create the poll.
		Poll poll = new Poll(questionArg.getAsString(), author);
		
		//Generate poll options.
		int k = 1;
		for (int i = 1; i <= 9; i++)
		{
			OptionMapping optionArg = event.getOption("option" + i);
			
			if (optionArg == null)
				continue;
			
			Poll.PollOption option = new Poll.PollOption("option" + k, optionArg.getAsString());
			poll.addOption(option);
			k++;
		}
		
		//Create the poll message.
		MessageBuilder messageBuilder = PollListener.generatePollMessage(poll, author);
		InteractionGroup interactionGroup = PollListener.generateButtonCallback(poll);
		
		//Send the pool and get the message id.
		InteractionHook hook = event.reply(messageBuilder.build())
				.setEphemeral(false)
				.complete();
		Message message = hook.retrieveOriginal().complete();
		String messageId = message.getId();
		
		poll.setMessageId(messageId); //Set poll message.
		this.addInteractionGroup(guild.getId(), messageId, interactionGroup); //Register button group.
		this.polls.put(messageId, poll); //Add poll to the list.
		
		//Refresh message after sending it.
		PollListener.refreshPollMessage(poll, message);
		
		//Set a timer to stop the poll.
		Timer timer = new Timer(duration * 60 * 1000, e -> this.stopPoll(poll, guild, channel));
		timer.setRepeats(false);
		timer.start();
	}
	
	private void removeVote(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping pollIdArg = event.getOption("poll-id");
		
		if (pollIdArg == null)
			return;
		
		//Get the poll from map.
		Poll poll = this.polls.get(pollIdArg.getAsString());
		
		//Check if a poll was found.
		if (poll == null)
		{
			Message message = MessageUtils.buildErrorMessage("Rimozione voto", author, "Nessun sondaggio trovato con id: " + pollIdArg.getAsString());
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		boolean removed = poll.removeVote(author);
		
		if (!removed)
		{
			Message message = MessageUtils.buildErrorMessage("Rimozione voto", author, "Non hai aggiunto nessun voto al sondaggio con id: " + pollIdArg.getAsString());
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Refresh the message.
		Message message = channel.retrieveMessageById(poll.getMessageId()).complete();
		PollListener.refreshPollMessage(poll, message);
		
		message = MessageUtils.buildSimpleMessage("Rimozione voto", author, "Voto rimosso con successo.");
		event.reply(message).setEphemeral(true).queue();
	}
	
	private void editPollDescription(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping pollIdArg = event.getOption("poll-id");
		OptionMapping descriptionArg = event.getOption("description");
		
		if (pollIdArg == null || descriptionArg == null)
			return;
		
		//Get the poll by id.
		Poll poll = this.polls.get(pollIdArg.getAsString());
		
		//Check if a poll was found.
		if (poll == null)
		{
			Message message = MessageUtils.buildErrorMessage("Modifica descrizione del sondaggio", author, "Nessuna poll trovata con id: " + pollIdArg.getAsString());
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if the user trying to edit the poll is also the its author.
		if (!author.equals(poll.getAuthor()))
		{
			Message message = MessageUtils.buildErrorMessage("Modifica descrizione del sondaggio", author, "Questo sondaggio non appartiene a te.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Edit the description.
		poll.setQuestion(descriptionArg.getAsString());
		
		//Refresh poll message.
		Message pollMessage = channel.retrieveMessageById(poll.getMessageId()).complete();
		PollListener.refreshPollMessage(poll, pollMessage);
		
		InteractionGroup interactionGroup = PollListener.generateButtonCallback(poll);
		this.addInteractionGroup(guild.getId(), poll.getMessageId(), interactionGroup);
		
		Message message = MessageUtils.buildSimpleMessage("Modifica descrizione del sondaggio", author, "Descrizione cambiata con successo.");
		event.reply(message).setEphemeral(false).queue();
	}
	
	private void addOption(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping pollIdArg = event.getOption("poll-id");
		OptionMapping descriptionArg = event.getOption("description");
		
		if (pollIdArg == null || descriptionArg == null)
			return;
		
		//Get the poll by id.
		Poll poll = this.polls.get(pollIdArg.getAsString());
		
		//Check if a poll was found.
		if (poll == null)
		{
			Message message = MessageUtils.buildErrorMessage("Aggiunta opzione al sondaggio", author, "Nessuna poll trovata con id: " + pollIdArg.getAsString());
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if the user is the auhor of the poll.
		if (!author.equals(poll.getAuthor()))
		{
			Message message = MessageUtils.buildErrorMessage("Aggiunta opzione al sondaggio", author, "Questo sondaggio non appartiene a te.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if poll are less than 9.
		int pollSize = poll.getOptions().size();
		if (pollSize >= 9)
		{
			Message message = MessageUtils.buildErrorMessage("Aggiunta opzione al sondaggio", author, "Non è possibile aggiungere più di 9 opzioni ad un sondaggio.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Add the option to the poll.
		Poll.PollOption option = new Poll.PollOption("option" + (pollSize + 1), descriptionArg.getAsString());
		poll.addOption(option);
		
		//Refresh poll message.
		Message pollMessage = channel.retrieveMessageById(poll.getMessageId()).complete();
		PollListener.refreshPollMessage(poll, pollMessage);
		
		InteractionGroup interactionGroup = PollListener.generateButtonCallback(poll);
		this.addInteractionGroup(guild.getId(), poll.getMessageId(), interactionGroup);
		
		Message message = MessageUtils.buildSimpleMessage("Aggiunta opzione al sondaggio", author, "Aggiunta l'opzione \"" + option.getText() + "\" al sondaggio.");
		event.reply(message).setEphemeral(false).queue();
	}
	
	private void removeOption(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping pollIdArg = event.getOption("poll-id");
		OptionMapping indexArg = event.getOption("position");
		
		if (pollIdArg == null || indexArg == null)
			return;
		
		//Get the poll by id.
		Poll poll = this.polls.get(pollIdArg.getAsString());
		
		//Check if a poll was found.
		if (poll == null)
		{
			Message message = MessageUtils.buildErrorMessage("Rimozione opzione dal sondaggio", author, "Nessuna poll trovata con id: " + pollIdArg.getAsString());
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if the user is the auhor of the poll.
		if (!author.equals(poll.getAuthor()))
		{
			Message message = MessageUtils.buildErrorMessage("Rimozione opzione dal sondaggio", author, "Questo sondaggio non appartiene a te.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Minimum number of poll option is 2.
		if (poll.getOptions().size() <= 2)
		{
			Message message = MessageUtils.buildErrorMessage("Rimozione opzione dal sondaggio", author, "Un sondaggio deve avere almeno 2 opzioni.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Try to remove the poll option.
		int optionIndex = (int) indexArg.getAsLong();
		boolean success = poll.removeOption("option" + optionIndex);
		
		//Check if the option was succesfully deleted.
		if (!success)
		{
			Message message = MessageUtils.buildErrorMessage("Rimozione opzione dal sondaggio", author, "Non è stata trovata nessun opzione con id: " + optionIndex);
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Refresh the poll message.
		Message message = channel.retrieveMessageById(poll.getMessageId()).complete();
		PollListener.refreshPollMessage(poll, message);
		
		//Send message info.
		Message messageInfo = MessageUtils.buildSimpleMessage("Rimozione opzione dal sondaggio", author, "Opzione rimossa con successo.");
		event.reply(messageInfo).setEphemeral(false).queue();
	}
	
	private void removePoll(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping pollIdArg = event.getOption("poll-id");
		
		if (pollIdArg == null)
			return;
		
		//Get the poll from map.
		Poll poll = this.polls.get(pollIdArg.getAsString());
		
		//Check if a poll was found.
		if (poll == null)
		{
			Message message = MessageUtils.buildErrorMessage("Arresto sondaggio", author, "Nessun sondaggio trovato con id: " + pollIdArg.getAsString());
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if the user is the owner of the poll.
		if (!author.equals(poll.getAuthor()))
		{
			Message message = MessageUtils.buildErrorMessage("Arresto sondaggio", author, "Non sei il proprietario del sondaggio.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Stop the poll.
		this.stopPoll(poll, guild, channel);
		
		Message message = MessageUtils.buildSimpleMessage("Arresto sondaggio", author, "Sondaggio fermato correttamente.");
		event.reply(message).setEphemeral(false).queue();
	}
	
	/**
	 * Stop the given poll and refresh its message.
	 * @param poll The poll to stop.
	 * @param guild The guild the poll is in.
	 * @param channel The channel the poll was sent in.
	 */
	private void stopPoll(Poll poll, Guild guild, MessageChannel channel)
	{
		//Get the poll by id.
		Poll removedPoll = this.polls.remove(poll.getMessageId());
		
		if (removedPoll == null)
			return;
		
		//Stop the poll, refresh its message and remove buttons callback.
		removedPoll.setFinished(true);
		Message message = channel.retrieveMessageById(poll.getMessageId()).complete();
		PollListener.refreshPollMessage(poll, message);
		this.removeInteractionGroup(guild.getId(), poll.getMessageId());
	}
	
	private static ArrayList<ActionRow> buildPollMessageButtons(Poll poll)
	{
		ArrayList<ActionRow> rows = new ArrayList<>();
		ArrayList<Button> buttons = new ArrayList<>();
		
		//Build button interaction for the message.
		for (Poll.PollOption pollOption : poll.getOptions())
		{
			Button button = Button.primary(pollOption.getId(), pollOption.getText());
			buttons.add(button);
			
			if (buttons.size() == 3)
			{
				rows.add(ActionRow.of(buttons));
				buttons.clear();
			}
		}
		
		if (!buttons.isEmpty())
			rows.add(ActionRow.of(buttons));
		
		return rows;
	}
	
	private static InteractionGroup generateButtonCallback(Poll poll)
	{
		InteractionGroup interactionGroup = new InteractionGroup();
		
		//For each option create a callback that the listener will call.
		for (Poll.PollOption option : poll.getOptions())
		{
			interactionGroup.addButtonCallback(option.getId(), (event, guild, messageChannel, message, author) ->
			{
				//If the user already voted try to switch vote.
				if (poll.hasUserVoted(author))
				{
					boolean switched = poll.switchVote(option.getId(), author);
					
					//The user tried to switch the with the same vote.
					if (!switched)
					{
						event.reply(MessageUtils.buildErrorMessage("Votazione sondaggio", author, "Hai già votato per l'opzione: *" + option.getId() + "*")).setEphemeral(true).queue();
						return InteractionCallback.LEAVE_MESSAGE;
					}
					
					event.deferEdit().queue();
					PollListener.refreshPollMessage(poll, message);
					return InteractionCallback.LEAVE_MESSAGE;
				}
				
				event.deferEdit().queue();
				
				//Add the vote.
				boolean voteAdded = poll.addVote(option.getId(), author);
				
				if (!voteAdded)
					return InteractionCallback.LEAVE_MESSAGE;
				
				//Refresh the message.
				PollListener.refreshPollMessage(poll, message);
				
				return InteractionCallback.LEAVE_MESSAGE;
			});
		}
		
		return interactionGroup;
	}
	
	private static MessageBuilder generatePollMessage(Poll poll, User author)
	{
		MessageBuilder messageBuilder = new MessageBuilder();
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed(poll.getQuestion(), author)
				.setDescription("*Sondaggio in corso...*")
				.setFooter("Richiesto da " + author.getName() + " | ID: " + poll.getMessageId(), author.getAvatarUrl());
		
		if (poll.isFinished())
			embedBuilder.setDescription(PollListener.getWinnerDescription(poll));
		else
			messageBuilder.setActionRows(PollListener.buildPollMessageButtons(poll)); //Add buttons.
		
		if (poll.getVotesCount() > 0)
			embedBuilder.setImage("attachment://pie_chart.png");
		
		messageBuilder.setEmbeds(embedBuilder.build());
		
		return messageBuilder;
	}
	
	private static String getWinnerDescription(Poll pool)
	{
		StringBuilder stringBuilder = new StringBuilder();
		
		ArrayList<Poll.PollOption> winners = pool.getWinners();
		
		//Only one winner.
		if (winners.size() == 1)
			return "*Sondaggio terminato!*\nRisultato: **" + winners.get(0).getText() + "** (voti: " + winners.get(0).getVotesCount() + ")";
		
		//Multiple winners.
		stringBuilder.append("*Il sondaggio è finito con un pareggio:*\n");
		
		for (Poll.PollOption winner : winners)
		{
			stringBuilder.append("***" + winner.getText() + "*** (voti: " + winners.get(0).getVotesCount() + ")\n");
		}
		
		return stringBuilder.toString();
	}
	
	private static void refreshPollMessage(Poll poll, Message message)
	{
		MessageBuilder messageBuilder = PollListener.generatePollMessage(poll, poll.getAuthor());
		MessageAction editMessage = message.editMessage(messageBuilder.build());
		
		//Remove all old attachments to create new ones.
		editMessage.retainFiles(new ArrayList<>());
		
		//If there are votes in the poll we can generate an image.
		if (poll.getVotesCount() > 0)
		{
			byte[] pollChart = PollDrawer.generatePieChart(poll);
			
			if (pollChart != null)
				editMessage.addFile(pollChart, "pie_chart.png");
		}
		
		editMessage.queue();
	}
}
