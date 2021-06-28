package com.serpenssolida.discordbot.module.poll;

import com.serpenssolida.discordbot.BotMain;
import com.serpenssolida.discordbot.ButtonGroup;
import com.serpenssolida.discordbot.module.BotCommand;
import com.serpenssolida.discordbot.module.BotListener;
import com.serpenssolida.discordbot.module.ButtonCallback;
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
		command.setAction((event, guild, channel, author) ->
		{
			this.createNewPoll(event, guild, channel, author);
			return true;
		});
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
		command.setAction((event, guild, channel, author) ->
		{
			this.removePoll(event, guild, channel, author);
			return true;
		});
		command.getSubcommand()
				.addOption(OptionType.STRING, "poll-id", "Identificatore univoco del sondaggio", true);

		this.addBotCommand(command);
	}
	
	private void removePoll(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping pollIdArg = event.getOption("poll-id");
		
		if (pollIdArg == null)
			return;
		
		//Poll poll = stopPoll(poll, )
		Poll poll = this.polls.get(pollIdArg.getAsString());
		
		EmbedBuilder embedBuilder = BotMain.getDefaultEmbed("Rimozione sondaggio", author);
		MessageBuilder messageBuilder = new MessageBuilder();
		
		if (poll == null)
		{
			embedBuilder.setDescription("Nessun sondaggio trovato con id: " + pollIdArg.getAsString());
		}
		else
		{
			this.stopPoll(poll, channel);
			
			embedBuilder.setDescription("Sondaggio fermato correttamente.");
		}
		
		messageBuilder.setEmbed(embedBuilder.build());
		event.reply(messageBuilder.build()).setEphemeral(true).queue();
		
	}
	
	private void createNewPoll(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping questionArg = event.getOption("question");
		OptionMapping durationArg = event.getOption("duration");
		
		//Null check for arguments.
		if (questionArg == null)
		{
			EmbedBuilder embedBuilder = BotMain.getDefaultEmbed("Sondaggio!", author);
			embedBuilder.setDescription("Errore con gli argomenti.");
			
			MessageBuilder messageBuilder = new MessageBuilder();
			messageBuilder.setEmbed(embedBuilder.build());
			
			event.reply(messageBuilder.build()).setEphemeral(true).queue();
			
			return;
		}
		
		//Get duration of the poll
		int duration = 60;
		if (durationArg != null)
			duration = (int) durationArg.getAsLong();
		
		//Create the poll.
		Poll poll = new Poll(questionArg.getAsString(), author);
		for (int i = 1; i <= 9; i++)
		{
			OptionMapping optionArg = event.getOption("option" + i);
			
			if (optionArg == null)
				continue;
			
			Poll.PollOption option = new Poll.PollOption(optionArg.getName(), optionArg.getAsString());
			poll.addOption(option);
		}
		
		//Create the poll message.
		MessageBuilder messageBuilder = PollListener.generatePollMessage(poll, author);
		ButtonGroup buttonGroup = PollListener.buildPollButtons(poll);
		
		//Send the pool and get the message id.
		InteractionHook hook = event.reply(messageBuilder.build())
				.setEphemeral(false)
				.complete();
		Message message = hook.retrieveOriginal().complete();
		String messageId = message.getId();
		
		poll.setMessageId(messageId); //Set poll message.
		this.addButtonGroup(guild.getId(), messageId, buttonGroup); //Register button group.
		this.polls.put(messageId, poll); //Add poll to the list.
		
		//Refresh message after sending it.
		PollListener.refreshPollMessage(poll, message);
		
		//Set a timer to stop the poll.
		Timer timer = new Timer(duration * 60 * 1000, e -> this.stopPoll(poll, channel));
		timer.setRepeats(false);
		timer.start();
		
		//Poll poll = new Poll()
	}
	
	private void stopPoll(Poll poll, MessageChannel channel)
	{
		Poll removedPoll = this.polls.remove(poll.getMessageId());
		
		if (removedPoll == null)
			return;
		
		removedPoll.setFinished(true);
		Message message = channel.retrieveMessageById(poll.getMessageId()).complete();
		PollListener.refreshPollMessage(poll, message);
	}
	
	private static ArrayList<ActionRow> buildPollMessageButtons(Poll poll)
	{
		ArrayList<ActionRow> rows = new ArrayList<>();
		ArrayList<Button> buttons = new ArrayList<>();
		
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
	
	private static ButtonGroup buildPollButtons(Poll poll)
	{
		ButtonGroup buttonGroup = new ButtonGroup();
		
		for (Poll.PollOption option : poll.getOptions())
		{
			buttonGroup.addButton(new ButtonCallback(option.getId(), (event, guild, messageChannel, message, author) ->
			{
				boolean voteAdded = poll.addVote(option.getId(), author);
				
				if (!voteAdded)
					return ButtonCallback.LEAVE_MESSAGE;
				
				PollListener.refreshPollMessage(poll, message);
				
				return ButtonCallback.LEAVE_MESSAGE;
			}));
		}
		
		return buttonGroup;
	}
	
	private static MessageBuilder generatePollMessage(Poll poll, User author)
	{
		MessageBuilder messageBuilder = new MessageBuilder();
		EmbedBuilder embedBuilder = BotMain.getDefaultEmbed(poll.getQuestion(), author)
				.setDescription("*Sondaggio in corso...*")
				.setFooter("Richiesto da " + author.getName() + " | ID: " + poll.getMessageId(), author.getAvatarUrl());
		
		if (poll.isFinished())
			embedBuilder.setDescription(PollListener.getWinnerDescription(poll));
		else
			messageBuilder.setActionRows(PollListener.buildPollMessageButtons(poll)); //Add buttons
		
		if (poll.getVotesCount() > 0)
			embedBuilder.setImage("attachment://pie_chart.png");
		
		messageBuilder.setEmbed(embedBuilder.build());
		
		return messageBuilder;
	}
	
	private static String getWinnerDescription(Poll pool)
	{
		StringBuilder stringBuilder = new StringBuilder();
		
		ArrayList<Poll.PollOption> winners = pool.getWinners();
		
		if (winners.size() == 1)
			return "*Sondaggio temrinato!*\nRisultato: **" + winners.get(0).getText() + "** (voti: " + winners.get(0).getVotesCount() + ")";
		
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
		
		if (poll.getVotesCount() > 0)
		{
			editMessage.retainFiles(new ArrayList<>());
			editMessage.addFile(poll.generatePieChart().toByteArray(), "pie_chart.png");
		}
		
		editMessage.queue();
	}
}
