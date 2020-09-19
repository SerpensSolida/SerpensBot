package com.serpenssolida.discordbot.hungergames.task;

import com.serpenssolida.discordbot.Task;
import com.serpenssolida.discordbot.hungergames.Character;
import com.serpenssolida.discordbot.hungergames.HungerGamesController;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.Random;

import static com.serpenssolida.discordbot.hungergames.HungerGamesController.SUM_STATS;

public class EditCharacterTask extends Task
{
	private Character character;
	private State state;
	private Message reactionCheckMessage;
	
	private enum State
	{
		MENU,
		NAME_CHARACTER,
		ASSIGN_STATS,
		ASSIGN_AVATAR, //TODO: Ability to send avatar of the character.
		FINISHED
	}
	
	public EditCharacterTask(User user, MessageChannel channel)
	{
		super(user, channel);
		
		//Initialize task.
		this.state = State.MENU;
		this.character = HungerGamesController.getCharacter(user.getId());
		
		if (this.character == null)
		{
			this.setInterrupted(true);
			return;
		}
		
		//Send a message with the menu.
		this.sendMenu();
	}
	
	public Task.TaskResult consumeMessage(Message receivedMessage)
	{
		Task.TaskResult result;
		
		//Check if the user that started the task is the one who sent the message (This check should return ALWAYS true).
		if (!receivedMessage.getAuthor().equals(this.getUser()))
			return Task.TaskResult.NotFinished;
		
		switch (this.getState())
		{
			case MENU: //Waiting for reaction.
				result = Task.TaskResult.NotFinished;
				return result;
			case NAME_CHARACTER: //Naming the character.
				result = this.manageNameCharacterState(receivedMessage);
				return result;
			case ASSIGN_STATS: //Assigning stats to the character.
				result = this.manageAssignStatsState(receivedMessage);
				return result;
		}
		
		//This state is not possible, you broke my FSM :(
		receivedMessage.getChannel().sendMessage("Stato illegale, fai schifo con le MSF.").queue();
		result = TaskResult.Finished;
		
		return result;
	}
	
	public Task.TaskResult consumeReaction(Message message, String reactionName)
	{
		MessageBuilder builder = new MessageBuilder();
		
		if (this.getState() != State.MENU)
			return Task.TaskResult.NotFinished;
		
		//Check if the reaction is added to the menu.
		if (this.reactionCheckMessage.getId().equals(message.getId()))
		{
			switch (reactionName)
			{
				case "🇦":
					//Edit name of the player.
					this.state = State.NAME_CHARACTER;
					
					builder.append("> Inserisci il nuovo nome del tuo personaggio. (max 15 caratteri)");
					
					break;
				
				case "🇧":
					//Edit stats of the player.
					this.state = State.ASSIGN_STATS;
					
					builder.appendFormat("> Stai modificando le caratteristiche di **%s**", this.getCharacter().getDisplayName())
							.append("\n> Assegna le caratteristiche al personaggio. Invia un messaggio con 7 numeri separati da uno spazio che rappresentano le caratteristiche del tuo personaggio.")
							.append("\n> Le caratteristiche sono: ")
							.append("Vitalità, Forza, Abilità, Special, Velocità, Resistenza e Gusto. ")
							.appendFormat("\n> La somma dei valori delle caratteristiche deve essere %d punti e ogni carateristica deve essere compresa tra 0 e 10.", SUM_STATS);
					
					break;
				
				case "🇨":
					this.getChannel().deleteMessageById(this.reactionCheckMessage.getId()).queue(); //Delete the menu.
					
					this.state = State.FINISHED;
					return Task.TaskResult.Finished;
			}
		}
		
		this.sendMessage(builder.build());
		return Task.TaskResult.NotFinished;
	}
	
	private Task.TaskResult manageNameCharacterState(Message receivedMessage)
	{
		String message = receivedMessage.getContentDisplay();
		String name = message.strip();
		
		//Check if the name is empty or its length is greater than 16.
		if (name.length() <= 0 || name.length() > 16)
		{
			MessageBuilder messageBuilder = new MessageBuilder();
			messageBuilder.appendCodeLine((name.length() <= 0) ? "Devi inserire un nome!" : "Il nome non può essere più lungo di 16 caratteri!");
			
			this.sendMessage(messageBuilder.build());
			return Task.TaskResult.NotFinished;
		}
		
		//Set the name of the character.
		this.character.setName(name);
		HungerGamesController.save();
		
		this.state = State.MENU; //Back to menu.
		
		MessageBuilder builder = new MessageBuilder();
		builder.appendFormat("Nuovo nome del personaggio: %s", this.character.getDisplayName());
		
		this.sendMessage(builder.build());
		this.sendMenu();
		return Task.TaskResult.NotFinished;
	}
	
	private Task.TaskResult manageAssignStatsState(Message receivedMessage)
	{
		String message = receivedMessage.getContentDisplay();
		String[] abilitiesList = message.strip().replaceAll(" +", " ").split(" ");
		int[] abilities = new int[abilitiesList.length];
		int sum = 0;
		
		//Check number of ability sent with the message.
		if (abilities.length != 7)
		{
			MessageBuilder messageBuilder = new MessageBuilder()
					.append("> Inserisci tutte le caratteristiche!\n")
					.append("> Le caratteristiche sono: ")
					.append("Vitalità, Forza, Abilità, Special, Velocità, Resistenza e Gusto.\n")
					.appendFormat("> La somma dei valori delle caratteristiche deve essere %s punti e ogni caratteristica deve essere compresa tra 0 e 10!", SUM_STATS);
			
			this.sendMessage(messageBuilder.build());
			return Task.TaskResult.NotFinished;
		}
		
		//Check abilities format.
		try
		{
			for (int i = 0; i < abilitiesList.length; i++)
			{
				abilities[i] = Integer.parseInt(abilitiesList[i]);
				sum += abilities[i];
				
				if (abilities[i] < 0 || abilities[i] > 10)
				{
					throw new NumberFormatException(""); //This is ugly.
				}
			}
		}
		catch (NumberFormatException e)
		{
			MessageBuilder messageBuilder = new MessageBuilder();
			messageBuilder.append("> Formato delle caratteristiche errato. Inserisci solo numeri tra 0 e 10!");
			
			this.sendMessage(messageBuilder.build());
			return Task.TaskResult.NotFinished;
		}
		
		//Sum of ability values must be equal to SUM_STATS.
		if (sum != SUM_STATS)
		{
			MessageBuilder messageBuilder = new MessageBuilder()
					.appendFormat("> La somma dei valori delle caratteristiche deve essere %d punti e ogni caratteristica deve essere compresa tra 0 e 10!", SUM_STATS);
			
			this.sendMessage(messageBuilder.build());
			return Task.TaskResult.NotFinished;
		}
		
		//Set character stats.
		this.character.setStats(abilities);
		HungerGamesController.save();
		
		MessageBuilder builder = new MessageBuilder();
		builder.append("> Caratteristiche impostate correttamente.");
		
		this.sendMessage(builder.build());
		this.state = State.MENU;
		
		//Send the menu.
		this.sendMenu();
		
		return Task.TaskResult.NotFinished;
	}
	
	/**
	 * Send the menu of the task to the channel, the task will check for this message when a reaction is sent.
	 */
	public void sendMenu()
	{
		MessageBuilder builder = new MessageBuilder()
				.append("> Cosa vuoi modificare del tuo personaggio?\n> \n")
				.append("> \t:regional_indicator_a: - Nome.\n")
				.append("> \t:regional_indicator_b: - Statistiche.\n")
				.append("> \t:regional_indicator_c: - Esci.\n");
		
		MessageAction messageAction = this.getChannel().sendMessage(builder.build());
		
		//Add the reaction to the menu.
		messageAction.queue(message ->
		{
			this.reactionCheckMessage = message; //This message is the one used for checking the reaction.
			message.addReaction("🇦").queue();
			message.addReaction("🇧").queue();
			message.addReaction("🇨").queue();
		});
	}
	
	public Character getCharacter()
	{
		return this.character;
	}
	
	public State getState()
	{
		return this.state;
	}
}
