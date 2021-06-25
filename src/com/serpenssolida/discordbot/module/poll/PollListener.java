package com.serpenssolida.discordbot.module.poll;

import com.serpenssolida.discordbot.BotMain;
import com.serpenssolida.discordbot.ButtonGroup;
import com.serpenssolida.discordbot.module.BotCommand;
import com.serpenssolida.discordbot.module.BotListener;
import com.serpenssolida.discordbot.module.ButtonCallback;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;

import java.io.ByteArrayOutputStream;
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
		this.addBotCommand(command);
		
		//Command for deleting a poll.
		command = new BotCommand("stop", "Ferma un sondaggio in corso.");
		command.setAction((event, guild, channel, author) ->
		{
			this.stopPoll(event, guild, channel, author);
			return true;
		});
		command.getSubcommand()
				.addOption(OptionType.STRING, "poll-id", "Identificatore univoco del sondaggio", true);

		this.addBotCommand(command);
	}
	
	private void stopPoll(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping pollIdArg = event.getOption("poll-id");
		
		if (pollIdArg == null)
			return;
		
		Poll poll = this.polls.remove(pollIdArg.getAsString());
		
		EmbedBuilder embedBuilder = BotMain.getDefaultEmbed("Rimozione sondaggio", author);
		MessageBuilder messageBuilder = new MessageBuilder();
		
		if (poll == null)
		{
			embedBuilder.setDescription("Nessun sondaggio trovato con id: " + pollIdArg.getAsString());
		}
		else
		{
			Message message = channel.retrieveMessageById(poll.getMessageId()).complete();
			MessageEmbed embed = message.getEmbeds().get(0);
			MessageBuilder pollMessage = this.refreshPollMessage(poll, embed);
			pollMessage.setActionRows();
			message.editMessage(pollMessage.build()).queue();
			
			embedBuilder.setDescription("Sondaggio fermato correttamente.");
		}
		
		messageBuilder.setEmbed(embedBuilder.build());
		event.reply(messageBuilder.build()).setEphemeral(poll == null).queue();
		
	}
	
	private void createNewPoll(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping questionArg = event.getOption("question");
		
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
		
		Poll poll = new Poll();
		
		for (int i = 1; i <= 9; i++)
		{
			OptionMapping optionArg = event.getOption("option" + i);
			
			if (optionArg == null)
				continue;
			
			Poll.PollOption option = new Poll.PollOption(optionArg.getName(), optionArg.getAsString());
			poll.addOption(option);
		}
		
		//Create the poll message.
		EmbedBuilder embedBuilder = BotMain.getDefaultEmbed(author.getName() + " ha creato un sondaggio!", author);
		embedBuilder.setDescription("*" + questionArg.getAsString() + "*");
		
		MessageBuilder messageBuilder = new MessageBuilder();
		messageBuilder.setEmbed(embedBuilder.build());
		
		//Create buttons
		messageBuilder.setActionRows(this.buildPollMessageButtons(poll));
		ButtonGroup buttonGroup = this.buildPollButtons(poll);
		
		//Send the pool and get the message id.
		InteractionHook hook = event.reply(messageBuilder.build())
				//.addFile(pieChartImage.toByteArray(), "pie_chart.png")
				.setEphemeral(false)
				.complete();
		String messageId = hook.retrieveOriginal().complete().getId();
		
		poll.setMessageId(messageId);
		this.addButtonGroup(guild.getId(), messageId, buttonGroup);
		this.polls.put(messageId, poll);
		
		//Poll poll = new Poll()
	}
	
	private ArrayList<ActionRow> buildPollMessageButtons(Poll poll)
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
	
	private ButtonGroup buildPollButtons(Poll poll)
	{
		ButtonGroup buttonGroup = new ButtonGroup();
		
		for (Poll.PollOption option : poll.getOptions())
		{
			buttonGroup.addButton(new ButtonCallback(option.getId(), null, null, (event, guild, messageChannel, message, author) ->
			{
				boolean voteAdded = poll.addVote(option.getId(), author);
				
				if (!voteAdded)
					return ButtonCallback.LEAVE_MESSAGE;
				
				MessageEmbed embed = event.getMessage().getEmbeds().get(0);
				MessageBuilder messageBuilder = this.refreshPollMessage(poll, embed);
				
				ByteArrayOutputStream pieChartImage = poll.generatePieChart();
				
				event.getMessage().editMessage(messageBuilder.build())
						.retainFiles(new ArrayList<>())
						.addFile(pieChartImage.toByteArray(), "pie_chart.png")
						.queue();
				System.out.println("VOTATO L'OPZIONE: " + option.getId());
				System.out.println("Voti: " + poll.getPercent(option.getId()));
				
				return ButtonCallback.LEAVE_MESSAGE;
			}));
		}
		
		return buttonGroup;
	}
	
	private MessageBuilder refreshPollMessage(Poll poll, MessageEmbed embed)
	{
		//Create the poll message.
		EmbedBuilder embedBuilder = new EmbedBuilder(embed);
		
		//Refresh poll status in the embed.
		/*embedBuilder.clearFields();
		StringBuilder names = new StringBuilder();
		StringBuilder rateos = new StringBuilder();
		
		Stream<Poll.PollOption> sortedOptions = poll.getOptions().parallelStream().sorted(Comparator.comparingInt(Poll.PollOption::getVotesCount));
		
		for (Poll.PollOption option : sortedOptions.collect(Collectors.toCollection(ArrayList::new)))
		{
			names.append(option.getText() + "\n");
			rateos.append((poll.getPercent(option.getId())) * 100 + "%\n");
		}
		
		embedBuilder.addField("Opzioni", names.toString(), true);
		embedBuilder.addField("Risultati", rateos.toString(), true);*/
		
		embedBuilder.setImage("attachment://pie_chart.png");
		
		MessageBuilder messageBuilder = new MessageBuilder();
		messageBuilder.setEmbed(embedBuilder.build());
		
		//Create buttons
		messageBuilder.setActionRows(this.buildPollMessageButtons(poll));
		return messageBuilder;
		//ButtonGroup buttonGroup = this.buildPollButtons(poll);
		//this.remo
		//this.addButtonGroup(guildID, );
	}
}
