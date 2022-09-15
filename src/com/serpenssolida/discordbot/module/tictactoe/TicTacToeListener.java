package com.serpenssolida.discordbot.module.tictactoe;

import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.command.BotCommand;
import com.serpenssolida.discordbot.interaction.ButtonAction;
import com.serpenssolida.discordbot.interaction.InteractionCallback;
import com.serpenssolida.discordbot.interaction.InteractionGroup;
import com.serpenssolida.discordbot.module.BotListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;

public class TicTacToeListener extends BotListener
{
	private final HashMap<String, TicTacToeGame> games = new HashMap<>();
	
	public TicTacToeListener()
	{
		super("tictactoe");
		this.setModuleName("TicTacToe");
		
		//Command for creating a game.
		BotCommand command = new BotCommand("start", "Comincia una partita di tris.");
		command.setAction(this::startGame);
		command.getSubcommand()
				.addOption(OptionType.USER, "opponent", "L'avversario della partita", true);
		this.addBotCommand(command);
		
		//Command for stopping a game.
		command = new BotCommand("stop", "Ferma una partita di tris.");
		command.setAction(this::removeGame);
		command.getSubcommand()
				.addOption(OptionType.STRING, "game-id", "Id della partita", true);
		this.addBotCommand(command);
	}
	
	/**
	 * Callback for "start" command.
	 */
	private void startGame(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping opponentArg = event.getOption("opponent");
		
		if (opponentArg == null)
			return;
		
		//Get the opponent and create a new game.
		User opponent = opponentArg.getAsUser();
		TicTacToeGame game = new TicTacToeGame(author, opponent);
		
		MessageEditBuilder messageBuilder = TicTacToeListener.generateGameMessage(game, author);
		
		//Send the game message and get its id.
		InteractionHook hook = event.reply(MessageCreateData.fromEditData(messageBuilder.build()))
				.setEphemeral(false)
				.complete();
		Message message = hook.retrieveOriginal().complete();
		String messageId = message.getId();
		
		//Set the game id, register it and refresh the game message.
		game.setMessageId(messageId);
		this.games.put(messageId, game);
		this.addInteractionGroup(guild.getId(), messageId, this.generateGameCallback());
		TicTacToeListener.refreshGameMessage(game, message, author);
		
		//Set a timer of 5 minutes to stop the game.
		Timer timer = new Timer(5 * 60 * 1000, e -> this.stopGame(game, guild, channel));
		timer.setRepeats(false);
		timer.start();
	}
	
	/**
	 * Callback for "stop" command.
	 */
	private void removeGame(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
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
	
	/**
	 * Stop the given game.
	 *
	 * @param game
	 * 		The game to stop.
	 * @param guild
	 *		The guild the game is in.
	 * @param channel
	 * 		The channel the game is in.
	 */
	private void stopGame(TicTacToeGame game, Guild guild, MessageChannel channel)
	{
		if (game == null)
			return;
		
		game.setInterrupted(true);
		
		Message gameMessage = channel.retrieveMessageById(game.getMessageId()).complete();
		TicTacToeListener.refreshGameMessage(game, gameMessage, game.getPlayer(0));
		
		this.games.remove(game.getMessageId());
		this.removeInteractionGroup(guild.getId(), game.getMessageId());
	}
	
	private static void addTicTacToeButtons(MessageEditBuilder messageBuilder, TicTacToeGame game)
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
						break;
					default:
						break;
				}
				
				Button button = Button.secondary("" + (i + j * TicTacToeGame.FIELD_SIZE), label);
				
				if (game.isCellFull(i, j) || game.isFinished())
					button = button.asDisabled();
				
				buttons.add(button);
			}
			
			actionRows.add(ActionRow.of(buttons));
			buttons.clear();
		}

		messageBuilder.setComponents(actionRows);
	}
	
	private InteractionGroup generateGameCallback()
	{
		InteractionGroup interactionGroup = new InteractionGroup();
		for (int i = 0; i < TicTacToeGame.FIELD_SIZE; i++)
		{
			for (int j = 0; j < TicTacToeGame.FIELD_SIZE; j++)
			{
				int x = i;
				int y = j;
				
				ButtonAction button = (event, guild, channel, message, author) ->
				{
					TicTacToeGame game = this.games.get(message.getId());
					
					//Game must not be null.
					if (game == null)
					{
						event.reply(MessageUtils.buildErrorMessage("TicTacToe", author, "Si è verificato un errore.")).setEphemeral(true).queue();
						return InteractionCallback.DELETE_MESSAGE;
					}
					
					//Let discord know the bot received the interaction.
					event.deferEdit().queue();
					
					//Check if is the turn of the user.
					if (!author.equals(game.getCurrentUser()))
						return InteractionCallback.LEAVE_MESSAGE;
					
					//Check if the cell is empty.
					if (game.isCellFull(x, y))
						return InteractionCallback.LEAVE_MESSAGE;
					
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
						this.removeInteractionGroup(guild.getId(), message.getId());
					}
					
					return InteractionCallback.LEAVE_MESSAGE;
				};
				
				interactionGroup.addButtonCallback("" + (i + j * TicTacToeGame.FIELD_SIZE), button);
			}
		}
		
		return interactionGroup;
	}
	
	private static void refreshGameMessage(TicTacToeGame game, Message message, User author)
	{
		message.editMessage(TicTacToeListener.generateGameMessage(game, author).build()).queue();
	}
	
	private static MessageEditBuilder generateGameMessage(TicTacToeGame game, User author)
	{
		MessageEditBuilder messageBuilder = new MessageEditBuilder();
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
		
		messageBuilder.setEmbeds(embedBuilder.build());
		
		return messageBuilder;
	}
}
