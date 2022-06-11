package com.serpenssolida.discordbot.module.poll;

import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.SerpensBot;
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
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PollListener extends BotListener
{
	private final HashMap<String, Poll> polls = new HashMap<>();
	
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
			subCommand.addOption(OptionType.STRING, "option" + i, "Opzione del sondaggio", false);
		
		subCommand.addOption(OptionType.INTEGER, "duration", "Durata in minuti del sondaggio. (default: 60 minuti)", false);
		subCommand.addOption(OptionType.BOOLEAN, "keep-down", "Se tenere il sondaggio in fondo alla chat oppure no. (default: true)", false);
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
	
	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event)
	{
		//Don't accept messages from private channels.
		if (!event.isFromGuild())
			return;
		
		Guild guild = event.getGuild();
		MessageChannel channel = event.getChannel(); //Channel where the message was sent.
		User author = event.getAuthor();
		
		//If the author of the message is the bot, ignore the message.
		if (SerpensBot.getApi().getSelfUser().getId().equals(author.getId()))
			return;
		
		for (Map.Entry<String, Poll> pollEntry : this.polls.entrySet())
		{
			Poll poll = pollEntry.getValue();
			
			if (!poll.getChannel().equals(channel))
				continue;
			
			if (!poll.isKeepDown())
				break;
			
			if (poll.getMessageCount() < 10)
			{
				poll.setMessageCount(poll.getMessageCount() + 1);
				break;
			}
			
			//Reset message count.
			poll.setMessageCount(0);
			
			//Get previous poll message.
			Message previousPollMessage = channel.retrieveMessageById(poll.getMessageID()).complete();
			previousPollMessage.delete().queue();
			
			//Resend message.
			Message pollMessage = channel.sendMessage(PollListener.generatePollMessage(poll, author).build()).complete();
			
			//Set the correct message id for poll, map, and interaction group.
			this.switchInteractionGroupMessage(guild.getId(), poll.getMessageID(), pollMessage.getId());
			poll.setMessageID(pollMessage.getId());
			this.polls.remove(previousPollMessage.getId());
			this.polls.put(poll.getMessageID(), poll);
			
			//Refresh message after sending it.
			PollListener.refreshPollMessage(poll, pollMessage);
			
			break;
		}
	}
	
	private void createNewPoll(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping questionArg = event.getOption("question");
		OptionMapping durationArg = event.getOption("duration");
		OptionMapping keepDownArg = event.getOption("keep-down");
		
		//Null check for arguments.
		if (questionArg == null)
			return;
		
		//Get duration of the poll.
		int duration = 60;
		if (durationArg != null)
			duration = (int) durationArg.getAsLong();
		
		//Get the keep down flag.
		boolean keepDown = keepDownArg == null || keepDownArg.getAsBoolean();
		System.out.println("DO I NEED TO KEEP DOWN:" + keepDown);
		//Create the poll.
		Poll poll = new Poll(questionArg.getAsString(), author, channel, keepDown);
		
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
		
		poll.setMessageID(messageId); //Set poll message.
		this.addInteractionGroup(guild.getId(), messageId, interactionGroup); //Register button group.
		this.polls.put(messageId, poll); //Add poll to the list.
		
		//Refresh message after sending it.
		PollListener.refreshPollMessage(poll, message);
		
		//Set a timer to stop the poll.
		Timer timer = new Timer(duration * 60 * 1000, e -> this.stopPoll(poll, guild, channel));
		timer.setRepeats(false);
		timer.start();
	}
	
	private void removeVote(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
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
		Message message = channel.retrieveMessageById(poll.getMessageID()).complete();
		PollListener.refreshPollMessage(poll, message);
		
		message = MessageUtils.buildSimpleMessage("Rimozione voto", author, "Voto rimosso con successo.");
		event.reply(message).setEphemeral(true).queue();
	}
	
	private void editPollDescription(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
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
		
		//Check if the user trying to edit the poll is also its author.
		if (!author.equals(poll.getAuthor()))
		{
			Message message = MessageUtils.buildErrorMessage("Modifica descrizione del sondaggio", author, "Questo sondaggio non appartiene a te.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Edit the description.
		poll.setQuestion(descriptionArg.getAsString());
		
		//Refresh poll message.
		Message pollMessage = channel.retrieveMessageById(poll.getMessageID()).complete();
		PollListener.refreshPollMessage(poll, pollMessage);
		
		InteractionGroup interactionGroup = PollListener.generateButtonCallback(poll);
		this.addInteractionGroup(guild.getId(), poll.getMessageID(), interactionGroup);
		
		Message message = MessageUtils.buildSimpleMessage("Modifica descrizione del sondaggio", author, "Descrizione cambiata con successo.");
		event.reply(message).setEphemeral(false).queue();
	}
	
	private void addOption(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
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
		Message pollMessage = channel.retrieveMessageById(poll.getMessageID()).complete();
		PollListener.refreshPollMessage(poll, pollMessage);
		
		InteractionGroup interactionGroup = PollListener.generateButtonCallback(poll);
		this.addInteractionGroup(guild.getId(), poll.getMessageID(), interactionGroup);
		
		Message message = MessageUtils.buildSimpleMessage("Aggiunta opzione al sondaggio", author, "Aggiunta l'opzione \"" + option.getText() + "\" al sondaggio.");
		event.reply(message).setEphemeral(false).queue();
	}
	
	private void removeOption(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
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
		Message message = channel.retrieveMessageById(poll.getMessageID()).complete();
		PollListener.refreshPollMessage(poll, message);
		
		//Send message info.
		Message messageInfo = MessageUtils.buildSimpleMessage("Rimozione opzione dal sondaggio", author, "Opzione rimossa con successo.");
		event.reply(messageInfo).setEphemeral(false).queue();
	}
	
	private void removePoll(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
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
		Poll removedPoll = this.polls.remove(poll.getMessageID());
		
		if (removedPoll == null)
			return;
		
		//Stop the poll, refresh its message and remove buttons callback.
		removedPoll.setFinished(true);
		Message message = channel.retrieveMessageById(poll.getMessageID()).complete();
		PollListener.refreshPollMessage(poll, message);
		this.removeInteractionGroup(guild.getId(), poll.getMessageID());
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
						event.reply(MessageUtils.buildErrorMessage("Votazione sondaggio", author, "Hai già votato per l'opzione: *" + option.getText() + "*")).setEphemeral(true).queue();
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
		EmbedBuilder pollEmbedBuilder= MessageUtils.getDefaultEmbed(poll.getQuestion(), author)
				.setDescription("*Sondaggio in corso...*")
				.setFooter("Richiesto da " + author.getName() + " | ID: " + poll.getMessageID(), author.getAvatarUrl());
		EmbedBuilder legendEmbedBuilder = new EmbedBuilder();
		
		if (poll.isFinished())
			pollEmbedBuilder.setDescription(PollListener.getWinnerDescription(poll));
		else
			messageBuilder.setActionRows(PollListener.buildPollMessageButtons(poll)); //Add buttons.
		
		if (poll.getVotesCount() > 0)
		{
			pollEmbedBuilder.setImage("attachment://pie_chart.png");
			legendEmbedBuilder.setImage("attachment://chart_legend.png");
			messageBuilder.setEmbeds(pollEmbedBuilder.build(), legendEmbedBuilder.build());
		}
		else
			messageBuilder.setEmbeds(pollEmbedBuilder.build());
		
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
			ImmutablePair<byte[], byte[]> pieChartImages = PollDrawer.getPieChartImages(poll);
			
			byte[] pollChart = pieChartImages.getLeft();
			byte[] pollLegend = pieChartImages.getRight();
			
			if (pollChart != null)
			{
				editMessage.addFile(pollChart, "pie_chart.png");
				editMessage.addFile(pollLegend, "chart_legend.png");
			}
		}
		
		editMessage.queue();
	}
}
