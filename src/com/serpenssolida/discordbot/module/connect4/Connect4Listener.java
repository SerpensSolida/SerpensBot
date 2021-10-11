package com.serpenssolida.discordbot.module.connect4;

import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.SerpensBot;
import com.serpenssolida.discordbot.command.BotCommand;
import com.serpenssolida.discordbot.interaction.ButtonAction;
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
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Connect4Listener extends BotListener
{
	private final HashMap<String, Connect4Game> games = new HashMap<>();
	
	public Connect4Listener()
	{
		super("connect4");
		this.setModuleName("Connect4");
		
		//This module has no task.
		this.removeBotCommand("cancel");
		
		//Command for creating a game.
		BotCommand command = new BotCommand("start", "Comincia una partita di forza 4.");
		command.setAction(this::startGame);
		command.getSubcommand()
				.addOption(OptionType.USER, "opponent", "L'avversario della partita", true);
		this.addBotCommand(command);
		
		//Command for stopping a game.
		command = new BotCommand("stop", "Ferma una partita di forza 4.");
		command.setAction(this::removeGame);
		command.getSubcommand()
				.addOption(OptionType.STRING, "game-id", "Id della partita", true);
		this.addBotCommand(command);
		
		//Command for displaying the leader board.
		command = new BotCommand("leaderboard", "Mostra la classifica di Connect4.");
		command.setAction(this::sendLeaderboard);
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
		this.addInteractionGroup(guild.getId(), messageId, this.generateGameCallback());
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
			Message message = MessageUtils.buildErrorMessage("Connect4", author, "Nessuna partita trovata con l'id: " + gameIdArg.getAsString());
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if the user is one of the players.
		if (!game.getPlayers().contains(author))
		{
			Message message = MessageUtils.buildErrorMessage("Connect4", author, "Non puoi fermare una partita di cui non sei il partecipante.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Stop the game.
		this.stopGame(game, guild, channel);
		
		Message message = MessageUtils.buildSimpleMessage("Connect4", author, "La partita è stata interrotta con successo.");
		event.reply(message).setEphemeral(false).queue();
	}
	
	private void stopGame(Connect4Game game, Guild guild, MessageChannel channel)
	{
		if (game == null)
			return;
		
		game.setInterrupted(true);
		
		Message gameMessage = channel.retrieveMessageById(game.getMessageId()).complete();
		Connect4Listener.refreshGameMessage(game, gameMessage, game.getPlayer(0));
		
		this.games.remove(game.getMessageId());
		this.removeInteractionGroup(guild.getId(), game.getMessageId());
	}
	
	private void sendLeaderboard(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		//Get the leaderboard.
		Connect4Leaderboard leaderboard = Connect4Controller.getLeaderboard(guild.getId());
		
		//Sort leaderboard
		ArrayList<Map.Entry<String, Connect4UserData>> sortedLeaderboard = new ArrayList<>(leaderboard.getDataAsList());
		sortedLeaderboard.sort((data1, data2) ->
		{
			Connect4UserData userData1 = data1.getValue();
			Connect4UserData userData2 = data2.getValue();
			return (userData2.getWins() - userData1.getWins()) * 100 - (userData2.getLoses() - userData1.getLoses()) * 10 + (userData2.getLoses() - userData1.getLoses());
		});
		
		//Create embed fields.
		StringBuilder names = new StringBuilder();
		StringBuilder values = new StringBuilder();
		
		for (Map.Entry<String, Connect4UserData> data : sortedLeaderboard)
		{
			User user = SerpensBot.api.getUserById(data.getKey());
			
			if (user != null)
			{
				names.append(String.format("%s\n", user.getName()));
				values.append(String.format("%s\n", data.getValue().getWins()));
			}
		}
		
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Classifica Connect4", author);
		embedBuilder.addField("Nome", names.toString(), true);
		embedBuilder.addField("Vittorie", values.toString(), true);
		
		//Send the leaderboard.
		MessageBuilder messageBuilder = new MessageBuilder();
		messageBuilder.setEmbeds(embedBuilder.build());
		
		event.reply(messageBuilder.build()).queue();
	}
	
	private static void addConnect4Buttons(MessageBuilder messageBuilder, Connect4Game game)
	{
		ArrayList<Button> buttons = new ArrayList<>();
		ArrayList<ActionRow> actionRows = new ArrayList<>();
		
		//Generate the buttons.
		for (int i = 0; i < Connect4Game.FIELD_WIDTH; i++)
		{
			Button button = Button.secondary("" + i, "" + (i + 1));
			
			if (game.getHeight(i) >= Connect4Game.FIELD_HEIGHT || game.isFinished())
				button = button.asDisabled();
			
			buttons.add(button);
			
			if (buttons.size() == 5)
			{
				actionRows.add(ActionRow.of(buttons));
				buttons.clear();
			}
		}
		
		//Check if there are remaining buttons to add.
		if (!buttons.isEmpty())
			actionRows.add(ActionRow.of(buttons));
		
		messageBuilder.setActionRows(actionRows);
	}
	
	private InteractionGroup generateGameCallback()
	{
		InteractionGroup interactionGroup = new InteractionGroup();
		for (int i = 0; i < Connect4Game.FIELD_WIDTH; i++)
		{
			int x = i;
			
			ButtonAction button = (event, guild, channel, message, author) ->
			{
				Connect4Game game = this.games.get(message.getId());
				
				if (game == null)
				{
					event.reply(MessageUtils.buildErrorMessage("Connect4", author, "Si è verificato un errore.")).setEphemeral(true).queue();
					return InteractionCallback.DELETE_MESSAGE;
				}
				
				//Let discord know the bot received the interaction.
				event.deferEdit().queue();
				
				//Check if is the turn of the player to play.
				if (!author.equals(game.getCurrentUser()))
					return InteractionCallback.LEAVE_MESSAGE;
				
				//Place the piece.
				int turn = game.getCurrentTurn();
				int height = game.getHeight(x);
				
				//Check if it is a valid move.
				if ((height < 0 || height >= Connect4Game.FIELD_HEIGHT) || !game.isCellEmpty(x, height))
					return InteractionCallback.LEAVE_MESSAGE;
				
				game.setCell(game.getCurrentTurn(), x, height);
				game.setLastMove(x);
				game.incrementTurn();
				
				//Check if it was a winning move.
				boolean winningMove = game.checkMove(turn, x, height);
				
				if (winningMove)
				{
					game.setFinished(true);
					game.setWinner(game.getPlayer(turn));
					
					//Update player statistics.
					for (User player : game.getPlayers())
					{
						Connect4UserData data = Connect4Controller.getUserData(guild.getId(), player.getId());
						
						if (player.equals(game.getWinner()))
						{
							data.setWins(data.getWins() + 1);
						}
						else
						{
							data.setLoses(data.getLoses() + 1);
						}
						
						Connect4Controller.setUserData(guild.getId(), player.getId(), data);
					}
				}
				else if (game.isFieldFull()) //Check if the field is full.
				{
					//Update player statistics.
					for (User player : game.getPlayers())
					{
						Connect4UserData data = Connect4Controller.getUserData(guild.getId(), player.getId());
						data.setDraws(data.getDraws() + 1);
						Connect4Controller.setUserData(guild.getId(), player.getId(), data);
					}
					
					game.setFinished(true);
				}
				
				//If the game is finished remove the game and its callback.
				if (game.isFinished())
				{
					this.games.remove(message.getId());
					this.removeInteractionGroup(guild.getId(), message.getId());
				}
				
				//Refresh the message.
				Connect4Listener.refreshGameMessage(game, message, author);
				
				return InteractionCallback.LEAVE_MESSAGE;
			};
			
			interactionGroup.addButtonCallback("" + i, button);
		}
		
		return interactionGroup;
	}
	
	private static void refreshGameMessage(Connect4Game game, Message message, User author)
	{
		MessageAction editMessage = message.editMessage(Connect4Listener.generateGameMessage(game, author).build());
		
		byte[] fieldImage = Connect4GameDrawer.generateFieldImage(game);
		
		//Check if the image was generated correctly.
		if (fieldImage != null)
		{
			//Remove all old attachments to create new ones.
			editMessage.retainFiles(new ArrayList<>());
			
			//Add field imaged to the message.
			editMessage.addFile(fieldImage, "field.png");
		}
		
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
		
		messageBuilder.setEmbeds(embedBuilder.build());
		
		return messageBuilder;
	}
}
