package com.serpenssolida.discordbot.hungergames.task;

import com.serpenssolida.discordbot.Task;
import com.serpenssolida.discordbot.hungergames.Character;
import com.serpenssolida.discordbot.hungergames.HungerGamesController;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class CreateCharacterTask extends Task
{
	private Character character;
	private State state;
	
	public enum State
	{
		NAME_CHARACTER,
		ASSIGN_STATS,
		ASSIGN_AVATAR,
		FINISHED
	}
	
	public CreateCharacterTask(User user, MessageChannel channel)
	{
		super(user, channel);
		
		MessageBuilder builder = (new MessageBuilder()).append("> Stai creando un personaggio! Inserisci il nome del tuo personaggio. (max 16 caratteri)");
		this.sendMessage(builder.build());
		
		this.state = State.NAME_CHARACTER;
		this.character = new Character(user.getId());
	}
	
	public Task.TaskResult consumeMessage(Message receivedMessage)
	{
		if (!receivedMessage.getAuthor().equals(this.getUser()))
			return Task.TaskResult.NotFinished;
		
		Task.TaskResult result;
		
		switch (this.getState())
		{
			case NAME_CHARACTER: //Naming character.
				result = this.manageNameCharacterState(receivedMessage);
				return result;
			case ASSIGN_STATS: //Assigning stas.
				result = this.manageAssignStatsState(receivedMessage);
				return result;
		}
		
		//This state is not possible, you broke my FSM :(
		receivedMessage.getChannel().sendMessage("Stato illegale, fai schifo con le MSF.").queue();
		result = TaskResult.Finished;
		return result;
	}
	
	public Task.TaskResult reactionAdded(Message message, String reaction)
	{
		return TaskResult.Finished;
	}
	
	private Task.TaskResult manageNameCharacterState(Message receivedMessage)
	{
		String message = receivedMessage.getContentDisplay();
		String name = message.strip();
		
		if (name.length() <= 0 || name.length() > 16)
		{
			MessageBuilder messageBuilder = (new MessageBuilder()).appendCodeLine((name.length() <= 0) ? "Devi inserire un nome!" : "Il nome non può essere più lungo di 15 caratteri!");
			this.sendMessageWithAbort(messageBuilder.build());
			return Task.TaskResult.NotFinished;
		}
		
		this.character.setName(name);
		this.state = State.ASSIGN_STATS;
		
		System.out.println("Nome assegnato: " + name);
		MessageBuilder builder = (new MessageBuilder())
				.append("> Nome selezionato: ")
				.append(name, MessageBuilder.Formatting.BOLD)
				.append("\n> Adesso assegna le caratteristiche al personaggio. Invia un messaggio con 7 numeri separati da uno spazio che rappresentano le caratteristiche del tuo personaggio.")
				.append("\n> Le caratteristiche sono: ")
				.append("Vitalità, Forza, Abilità, Special, Velocità, Resistenza e Gusto. ")
				.appendFormat("\n> La somma dei valori delle caratteristiche deve essere %s punti e ogni caratteristica deve essere compresa tra 0 e 10!", HungerGamesController.SUM_STATS);
		
		this.sendMessageWithAbort(builder.build());
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
					.append("> Inserisci tutte le caratteristiche!")
					.append("\n> Le caratteristiche sono: ")
					.append("Vitalità, Forza, Abilità, Special, Velocità, Resistenza e Gusto. ")
					.appendFormat("\n> La somma dei valori delle caratteristiche deve essere %s punti e ogni caratteristica deve essere compresa tra 0 e 10!", HungerGamesController.SUM_STATS);
			
			this.sendMessageWithAbort(messageBuilder.build());
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
			MessageBuilder messageBuilder = new MessageBuilder()
					.append("> Formato delle caratteristiche errato. Inserisci solo numeri tra 0 e 10!");
			
			this.sendMessageWithAbort(messageBuilder.build());
			return Task.TaskResult.NotFinished;
		}
		
		if (sum != HungerGamesController.SUM_STATS)
		{
			MessageBuilder messageBuilder = new MessageBuilder()
					.appendFormat("\n> La somma dei valori delle caratteristiche deve essere %d punti! Somma dei valori inseriti: %s", HungerGamesController.SUM_STATS, sum);
			
			this.sendMessageWithAbort(messageBuilder.build());
			return Task.TaskResult.NotFinished;
		}
		
		this.character.setStats(abilities);
		
		HungerGamesController.addCharacter(this.character);
		MessageBuilder builder = new MessageBuilder()
				.append("> Creazione personaggio completata!");
		
		this.sendMessageWithAbort(builder.build());
		this.state = State.FINISHED;
		
		return Task.TaskResult.Finished;
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
