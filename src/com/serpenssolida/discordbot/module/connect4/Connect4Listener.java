package com.serpenssolida.discordbot.module.connect4;

import com.serpenssolida.discordbot.module.ButtonGroup;
import com.serpenssolida.discordbot.MessageUtils;
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
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Connect4Listener extends BotListener
{
	private HashMap<String, Connect4Game> games = new HashMap<>();
	
	public Connect4Listener()
	{
		super("connect4");
		this.setModuleName("Connect4");
		
		//This module has no task.
		this.removeBotCommand("cancel");
		
		//Command for creating a game.
		BotCommand command = new BotCommand("start", "Comincia una partita di forza 4.");
		command.setAction((event, guild, channel, author) ->
		{
			//this.createNewPoll(event, guild, channel, author);
			this.startGame(event, guild, channel, author);
			return true;
		});
		command.getSubcommand()
				.addOption(OptionType.USER, "opponent", "L'avversario della partita", true);
		this.addBotCommand(command);
		
		//Command for stopping a game.
		command = new BotCommand("stop", "Ferma una partita di forza 4.");
		command.setAction((event, guild, channel, author) ->
		{
			//this.createNewPoll(event, guild, channel, author);
			//this.startGame(event, guild, channel,author);
			this.removeGame(event, guild, channel, author);
			return true;
		});
		command.getSubcommand()
				.addOption(OptionType.STRING, "game-id", "Id della partita", true);
		this.addBotCommand(command);
	}
	
	private void startGame(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping opponentArg = event.getOption("opponent");
		
		if (opponentArg == null)
			return;
		
		//Get the opponent and create a new game.
		User opponent = opponentArg.getAsUser();
		Connect4Game game = new Connect4Game(author, opponent);
		
		MessageBuilder messageBuilder = generateGameMessage(game, author);
		
		//Send the game message and get its id.
		InteractionHook hook = event.reply(messageBuilder.build())
				.setEphemeral(false)
				.complete();
		Message message = hook.retrieveOriginal().complete();
		String messageId = message.getId();
		
		//Set the game id, register it and refresh the game message.
		game.setMessageId(messageId);
		this.games.put(messageId, game);
		this.addButtonGroup(guild.getId(), messageId, this.generateGameCallback());
		Connect4Listener.refreshGameMessage(game, message, author);
		
		//Set a timer of 5 minutes to stop the game.
		Timer timer = new Timer(30 * 60 * 1000, e -> this.stopGame(game, guild, channel)); //TODO: Make it configurable.
		timer.setRepeats(false);
		timer.start();
	}
	
	private void removeGame(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping gameIdArg = event.getOption("game-id");
		
		if (gameIdArg == null)
			return;
		
		//Get the game by id.
		Connect4Game game = this.games.get(gameIdArg.getAsString());
		
		//Check if a game was found.
		if (game == null)
		{
			event.reply(MessageUtils.buildErrorMessage("Connect4", author, "Nessuna partita trovata con l'id:" + gameIdArg.getAsString())).setEphemeral(true).queue();
			return;
		}
		
		//Check if the user is one of the players.
		if (!game.getPlayers().contains(author))
		{
			event.reply(MessageUtils.buildErrorMessage("Connect4", author, "Non puoi fermare una partita di cui non sei il partecipante")).setEphemeral(true).queue();
			return;
		}
		
		//Stop the game.
		this.stopGame(game, guild, channel);
		
		event.reply(MessageUtils.buildSimpleMessage("Connect4", author, "La partita è stata interrotta con successo.")).setEphemeral(false).queue();
	}
	
	private void stopGame(Connect4Game game, Guild guild, MessageChannel channel)
	{
		if (game == null)
			return;
		
		game.setInterrupted(true);
		
		Message gameMessage = channel.retrieveMessageById(game.getMessageId()).complete();
		Connect4Listener.refreshGameMessage(game, gameMessage, game.getPlayer(0));
		
		this.games.remove(game.getMessageId());
		this.removeButtonGroup(guild.getId(), game.getMessageId());
	}
	
	private static void addConnect4Buttons(MessageBuilder messageBuilder, Connect4Game game)
	{
		ArrayList<Button> buttons = new ArrayList<>();
		ArrayList<ActionRow> actionRows = new ArrayList<>();
		
		//Generate the buttons.
		for (int i = 0; i < Connect4Game.FIELD_WIDTH; i++)
		{
			int x = i;
			Button button = Button.secondary("" + i, "" + (i + 1));
			
			if (game.getHeight(x) >= Connect4Game.FIELD_HEIGHT || game.isFinished())
				button = button.asDisabled();
			
			buttons.add(button);
			
			if (buttons.size() == 5)
			{
				actionRows.add(ActionRow.of(buttons));
				buttons.clear();
			}
		}
		
		if (buttons.size() > 0)
		{
			actionRows.add(ActionRow.of(buttons));
			buttons.clear();
		}
		
		messageBuilder.setActionRows(actionRows);
	}
	
	private ButtonGroup generateGameCallback()
	{
		ButtonGroup buttonGroup = new ButtonGroup();
		for (int i = 0; i < Connect4Game.FIELD_WIDTH; i++)
		{
			int x = i;
			
			ButtonCallback button = new ButtonCallback("" + i, (event, guild, channel, message, author) ->
			{
				Connect4Game game = this.games.get(message.getId());
				
				if (game == null)
				{
					event.reply(MessageUtils.buildErrorMessage("Connect4", author, "Si è verificato un errore.")).setEphemeral(true).queue();
					return ButtonCallback.DELETE_MESSAGE;
				}
				
				//Let discord know the bot received the interaction.
				event.deferEdit().queue();
				
				//Check if is the turn of the player to play.
				if (!author.equals(game.getCurrentUser()))
					return ButtonCallback.LEAVE_MESSAGE;
				
				//Place the piece.
				int turn = game.getCurrentTurn();
				int height = game.getHeight(x);
				
				//Check if it is a valid move.
				if (height < 0 || height >= Connect4Game.FIELD_WIDTH)
					return ButtonCallback.LEAVE_MESSAGE;
				
				game.setCell(game.getCurrentTurn(), x, height);
				game.incrementTurn();
				
				//Check if it was a winning move.
				boolean winningMove = game.checkMove(turn, x, height);
				
				if (winningMove)
				{
					game.setFinished(true);
					game.setWinner(game.getPlayer(turn));
				}
				else if (game.isFieldFull()) //Check if the field is full.
				{
					game.setFinished(true);
				}
				
				//If the game is finished remove the game and its callback.
				if (game.isFinished())
				{
					this.games.remove(message.getId());
					this.removeButtonGroup(guild.getId(), message.getId());
				}
				
				//Refresh the message.
				Connect4Listener.refreshGameMessage(game, message, author);
				
				return ButtonCallback.LEAVE_MESSAGE;
			});
			
			buttonGroup.addButton(button);
		}
		
		return buttonGroup;
	}
	
	private static void refreshGameMessage(Connect4Game game, Message message, User author)
	{
		MessageAction editMessage = message.editMessage(Connect4Listener.generateGameMessage(game, author).build());
		
		//Remove all old attachments to create new ones.
		editMessage.retainFiles(new ArrayList<>());
		
		//If there are votes in the poll we can generate an image.
		editMessage.addFile(game.generateFieldImage().toByteArray(), "field.png");
		
		editMessage.queue();
	}
	
	private static MessageBuilder generateGameMessage(Connect4Game game, User author)
	{
		MessageBuilder messageBuilder = new MessageBuilder();
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Partita a Connect4", author);
		
		if (game.getMessageId() != null)
			embedBuilder.setFooter("Richiesto da " + game.getPlayer(0).getName() + " | ID: " + game.getMessageId(), game.getPlayer(0).getAvatarUrl());
		else
			embedBuilder.setFooter("Richiesto da " + game.getPlayer(0).getName(), game.getPlayer(0).getAvatarUrl());
		
		embedBuilder.setDescription("**" + game.getPlayer(0).getName() + "** vs. **" + game.getPlayer(1).getName() + "**\n\n");
		
		if (game.isFinished())
		{
			User winner = game.getWinner();
			embedBuilder.appendDescription(winner != null ? "Il vincitore è " + winner.getName() + "." : "La partita si è conclusa con un pareggio.");
		}
		else if (game.isInterrupted())
		{
			embedBuilder.appendDescription("Partita interrotta.");
		}
		else
		{
			embedBuilder.appendDescription("Turno: **" + game.getCurrentUser().getName() + "**\nPedina: " + (game.getCurrentTurn() == 0 ? ":red_circle:" : ":yellow_circle:"));
			Connect4Listener.addConnect4Buttons(messageBuilder, game);
		}
		
		//Add game immage.
		embedBuilder.setImage("attachment://field.png");
		
		messageBuilder.setEmbed(embedBuilder.build());
		
		return messageBuilder;
	}
}