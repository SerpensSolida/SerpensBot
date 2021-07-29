package com.serpenssolida.discordbot.module.tictactoe;

import com.serpenssolida.discordbot.module.ButtonGroup;
import com.serpenssolida.discordbot.MessageUtils;
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
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;

public class TicTacToeListener extends BotListener
{
	private HashMap<String, TicTacToeGame> games = new HashMap<>();
	
	public TicTacToeListener()
	{
		super("tictactoe");
		this.setModuleName("TicTacToe");
		
		//This module has no task.
		this.removeBotCommand("cancel");
		
		//Command for creating a game.
		BotCommand command = new BotCommand("start", "Comincia una partita di tris.");
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
		command = new BotCommand("stop", "Ferma una partita di tris.");
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
		TicTacToeGame game = new TicTacToeGame(author, opponent);
		
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
		TicTacToeListener.refreshGameMessage(game, message, author);
		
		//Set a timer of 5 minutes to stop the game.
		Timer timer = new Timer(5 * 60 * 1000, e -> this.stopGame(game, guild, channel));
		timer.setRepeats(false);
		timer.start();
	}
	
	private void removeGame(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping gameIdArg = event.getOption("game-id");
		
		if (gameIdArg == null)
			return;
		
		//Get the game by id.
		TicTacToeGame game = this.games.get(gameIdArg.getAsString());
		
		//Check if a game was found.
		if (game == null)
		{
			event.reply(MessageUtils.buildErrorMessage("TicTacToe", author, "Nessuna partita trovata con l'id:" + gameIdArg.getAsString())).setEphemeral(true).queue();
			return;
		}
		
		//Check if the user is one of the players.
		if (!game.getPlayers().contains(author))
		{
			event.reply(MessageUtils.buildErrorMessage("TicTacToe", author, "Non puoi fermare una partita di cui non sei il partecipante")).setEphemeral(true).queue();
			return;
		}
		
		//Stop the game.
		this.stopGame(game, guild, channel);
		
		event.reply(MessageUtils.buildSimpleMessage("TicTacToe", author, "La partita è stata interrotta con successo.")).setEphemeral(false).queue();
	}
	
	private void stopGame(TicTacToeGame game, Guild guild, MessageChannel channel)
	{
		if (game == null)
			return;
		
		game.setInterrupted(true);
		
		Message gameMessage = channel.retrieveMessageById(game.getMessageId()).complete();
		TicTacToeListener.refreshGameMessage(game, gameMessage, game.getPlayer(0));
		
		this.games.remove(game.getMessageId());
		this.removeButtonGroup(guild.getId(), game.getMessageId());
	}
	
	private static void addTicTacToeButtons(MessageBuilder messageBuilder, TicTacToeGame game)
	{
		ArrayList<Button> buttons = new ArrayList<>();
		ArrayList<ActionRow> actionRows = new ArrayList<>();
		
		//Generate the rows for the buttons.
		for (int i = 0; i < TicTacToeGame.FIELD_SIZE; i++)
		{
			for (int j = 0; j < TicTacToeGame.FIELD_SIZE; j++)
			{
				String label = " ";
				switch (game.getCell(i, j))
				{
					case 0:
						label = "❌";
						break;
					case 1:
						label = "⭕";
				}
				
				Button button = Button.secondary("" + (i + j * TicTacToeGame.FIELD_SIZE), label);
				
				if (!game.isCellEmpty(i, j) || game.isFinished())
					button = button.asDisabled();
				
				buttons.add(button);
			}
			
			actionRows.add(ActionRow.of(buttons));
			buttons.clear();
		}

		messageBuilder.setActionRows(actionRows);
	}
	
	private ButtonGroup generateGameCallback()
	{
		ButtonGroup buttonGroup = new ButtonGroup();
		for (int i = 0; i < TicTacToeGame.FIELD_SIZE; i++)
		{
			for (int j = 0; j < TicTacToeGame.FIELD_SIZE; j++)
			{
				int x = i;
				int y = j;
				ButtonCallback button = new ButtonCallback("" + (i + j * TicTacToeGame.FIELD_SIZE), (event, guild, channel, message, author) ->
				{
					TicTacToeGame game = this.games.get(message.getId());
					
					//Game must not be null.
					if (game == null)
					{
						event.reply(MessageUtils.buildErrorMessage("TicTacToe", author, "Si è verificato un errore.")).setEphemeral(true).queue();
						return ButtonCallback.DELETE_MESSAGE;
					}
					
					//Let discord know the bot received the interaction.
					event.deferEdit().queue();
					
					//Check if is the turn of the user.
					if (!author.equals(game.getCurrentUser()))
						return ButtonCallback.LEAVE_MESSAGE;
					
					//Check if the cell is empty.
					if (!game.isCellEmpty(x, y))
						return ButtonCallback.LEAVE_MESSAGE;
					
					//Execute player move.
					int turn = game.getCurrentTurn();
					game.setCell(game.getCurrentTurn(), x, y);
					game.incrementTurn();
					
					//Check if the player made a winning move.
					boolean winningMove = game.checkMove(turn, x, y);
					
					if (winningMove)
					{
						game.setFinished(true);
						game.setWinner(game.getPlayer(turn));
					}
					else if (game.isFieldFull()) //Check if the field is full.
					{
						game.setFinished(true);
					}
					
					//Refresh game message.
					TicTacToeListener.refreshGameMessage(game, message, author);
					
					//If the game is finished remove the game and its callback.
					if (game.isFinished())
					{
						this.games.remove(message.getId());
						this.removeButtonGroup(guild.getId(), message.getId());
					}
					
					return ButtonCallback.LEAVE_MESSAGE;
				});
				
				buttonGroup.addButton(button);
			}
		}
		
		return buttonGroup;
	}
	
	private static void refreshGameMessage(TicTacToeGame game, Message message, User author)
	{
		message.editMessage(TicTacToeListener.generateGameMessage(game, author).build()).queue();
	}
	
	private static MessageBuilder generateGameMessage(TicTacToeGame game, User author)
	{
		MessageBuilder messageBuilder = new MessageBuilder();
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Partita a TicTacToe", author);
		
		if (game.getMessageId() != null)
			embedBuilder.setFooter("Richiesto da " + game.getPlayer(0).getName() + " | ID: " + game.getMessageId(), game.getPlayer(0).getAvatarUrl());
		else
			embedBuilder.setFooter("Richiesto da " + game.getPlayer(0).getName(), game.getPlayer(0).getAvatarUrl());
		
		embedBuilder.setDescription("**" + game.getPlayer(0).getName() + "** (X) vs. **" + game.getPlayer(1).getName() + "** (O)\n\n");
		
		if (game.isFinished())
		{
			User winner = game.getWinner();
			embedBuilder.appendDescription(winner != null ? "Il vincitore è " + winner.getName() + "." : "La partita si è conclusa con un pareggio.");
			TicTacToeListener.addTicTacToeButtons(messageBuilder, game);
		}
		else if (game.isInterrupted())
		{
			embedBuilder.appendDescription("Partita interrotta.");
		}
		else
		{
			embedBuilder.appendDescription("Turno: **" + game.getCurrentUser().getName() + "**\nSimbolo: " + (game.getCurrentTurn() == 0 ? "**❌**" : "**⭕**"));
			TicTacToeListener.addTicTacToeButtons(messageBuilder, game);
		}
		
		messageBuilder.setEmbed(embedBuilder.build());
		
		return messageBuilder;
	}
}