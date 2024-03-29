package com.serpenssolida.discordbot.module.connect4;

import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.SerpensBot;
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
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

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
		
		//Command for creating a game.
		BotCommand command = new BotCommand("start", "Comincia una partita di forza 4.");
		command.setAction(this::startGame);
		command.getCommandData()
				.addOption(OptionType.USER, "opponent", "L'avversario della partita", true);
		this.addBotCommand(command);
		
		//Command for stopping a game.
		command = new BotCommand("stop", "Ferma una partita di forza 4.");
		command.setAction(this::removeGame);
		command.getCommandData()
				.addOption(OptionType.STRING, "game-id", "Id della partita", true);
		this.addBotCommand(command);
		
		//Command for displaying the leaderboard.
		command = new BotCommand("leaderboard", "Mostra la classifica di Connect4.");
		command.setAction(this::sendLeaderboard);
		this.addBotCommand(command);
	}
	
	/**
	 * Callback for "start" command.
	 */
	private void startGame(SlashCommandInteraction event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping opponentArg = event.getOption("opponent");
		
		//Check arguments.
		if (opponentArg == null)
			return;
		
		//Get the opponent and create a new game.
		User opponent = opponentArg.getAsUser();
		Connect4Game game = new Connect4Game(author, opponent);
		
		MessageEditBuilder messageBuilder = generateGameMessage(game, author);
		
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
		Connect4Listener.refreshGameMessage(game, message, author);
		
		//Set a timer of 5 minutes to stop the game.
		Timer timer = new Timer(30 * 60 * 1000, e -> this.stopGame(game, guild, channel));
		timer.setRepeats(false);
		timer.start();
	}
	
	/**
	 * Callback for "stop" command.
	 */
	private void removeGame(SlashCommandInteraction event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping gameIdArg = event.getOption("game-id");
		
		if (gameIdArg == null)
			return;
		
		//Get the game by id.
		Connect4Game game = this.games.get(gameIdArg.getAsString());
		
		//Check if a game was found.
		if (game == null)
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Connect4", author, "Nessuna partita trovata con l'id: " + gameIdArg.getAsString());
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if the user is one of the players.
		if (!game.getPlayers().contains(author))
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Connect4", author, "Non puoi fermare una partita di cui non sei il partecipante.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Stop the game.
		this.stopGame(game, guild, channel);
		
		MessageCreateData message = MessageUtils.buildSimpleMessage("Connect4", author, "La partita è stata interrotta con successo.");
		event.reply(message).setEphemeral(false).queue();
	}
	
	/**
	 * Callback for the leaderboard command.
	 */
	private void sendLeaderboard(SlashCommandInteraction event, Guild guild, MessageChannel channel, User author)
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
			User user = SerpensBot.getApi().getUserById(data.getKey());
			
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
		MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
		messageBuilder.setEmbeds(embedBuilder.build());
		
		event.reply(messageBuilder.build()).queue();
	}
	
	/**
	 * Stops the given game.
	 *
	 * @param game
	 * 		The game to stop.
	 * @param guild
	 * 		The guild the game is in.
	 * @param channel
	 * 		The channel the game is in.
	 */
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
	
	/**
	 * Adds the game button components to the given message.
	 *
 	 * @param messageBuilder
	 * 		The MessageCreateBuilder that will have the buttons added to it.
	 * @param game
	 * 		The state of the game that is currently being played.
	 */
	private static void addConnect4Buttons(MessageEditBuilder messageBuilder, Connect4Game game)
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
		
		messageBuilder.setComponents(actionRows);
	}
	
	/**
	 * Generate the callback for the game button components.
	 *
	 * @return
	 * 	The callback used by the button components of the game.
	 */
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
	
	/**
	 * Refresh the given message containing the game with the current status of the game.
	 *
	 * @param game
	 * 		The game that is currently playing on the given message
	 * @param message
	 * 		The message containing the game.
	 * @param author
	 * 		The User that started the game.
	 */
	private static void refreshGameMessage(Connect4Game game, Message message, User author)
	{
		MessageEditAction editMessage = message.editMessage(Connect4Listener.generateGameMessage(game, author).build());
		
		byte[] fieldImage = Connect4GameDrawer.generateFieldImage(game);
		
		//Check if the image was generated correctly.
		if (fieldImage != null)
		{
			//Add field imaged to the message.
			editMessage.setFiles(FileUpload.fromData(fieldImage, "field.png"));
		}
		
		editMessage.queue();
	}
	
	/**
	 * Generate a message containing the game.
	 *
	 * @param game
	 * 		The game to be shown in the message.
	 * @param author
	 * 		The User that started the game.
	 *
	 * @return
	 * 		The message of the game.
	 */
	private static MessageEditBuilder generateGameMessage(Connect4Game game, User author)
	{
		MessageEditBuilder messageBuilder = new MessageEditBuilder();
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
		
		//Add the embeds.
		messageBuilder.setEmbeds(embedBuilder.build());
		
		return messageBuilder;
	}
}
