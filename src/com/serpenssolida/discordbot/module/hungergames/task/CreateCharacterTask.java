package com.serpenssolida.discordbot.module.hungergames.task;

import com.serpenssolida.discordbot.module.Task;
import com.serpenssolida.discordbot.module.hungergames.Character;
import com.serpenssolida.discordbot.module.hungergames.HungerGamesController;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
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
	
	public CreateCharacterTask(Guild guild, User user, MessageChannel channel)
	{
		super(guild, user, channel);
	}
	
	@Override
	public boolean startMessage(MessageBuilder builder)
	{
		//Abort task if there is an HungerGames running.
		if (HungerGamesController.isHungerGamesRunning(this.getGuild().getId()))
		{
			builder.append("> Non puoi usare questo comando perchè è in corso un HungerGames.");
			this.sendMessage(builder.build());
			
			this.setInterrupted(true);
			return true;
		}
		
		builder.append("> Stai creando un personaggio! Inserisci il nome del tuo personaggio. (max 16 caratteri)");
		this.addCancelButton(builder);
		
		this.state = State.NAME_CHARACTER;
		this.character = new Character(this.user.getId());
		return false;
	}
	
	public void consumeMessage(Message receivedMessage)
	{
		if (!receivedMessage.getAuthor().equals(this.getUser()))
			return;
		
		//Remove buttons from last message.
		this.deleteButtons();
		
		//Abort task if there is an HungerGames running.
		if (HungerGamesController.isHungerGamesRunning(this.getGuild().getId()))
		{
			MessageBuilder builder = new MessageBuilder()
					.append("> Non puoi completare la procedura perchè è in corso un HungerGames.");
			this.sendMessage(builder.build());
			
			this.running = false;
			return;
		}
		
		switch (this.getState())
		{
			case NAME_CHARACTER: //Naming character.
				this.manageNameCharacterState(receivedMessage);
				return;
			case ASSIGN_STATS: //Assigning stas.
				this.manageAssignStatsState(receivedMessage);
				return;
		}
		
		//This state is not possible, you broke my FSM :(
		receivedMessage.getChannel().sendMessage("Stato illegale, fai schifo con le MSF.").queue();
		
		this.running = false;
	}
	
	public void reactionAdded(Message message, String reaction)
	{
		//Abort task if there is an HungerGames running.
		if (HungerGamesController.isHungerGamesRunning(this.getGuild().getId()))
		{
			MessageBuilder builder = new MessageBuilder()
					.append("> Non puoi completare la procedura perchè è in corso un HungerGames.");
			this.sendMessage(builder.build());
			
			this.running = false;
			return;
		}
		
		this.running = false;
	}
	
	private void manageNameCharacterState(Message receivedMessage)
	{
		String message = receivedMessage.getContentDisplay();
		String name = message.strip();
		
		if (name.length() <= 0 || name.length() > 16)
		{
			MessageBuilder messageBuilder = (new MessageBuilder()).appendCodeLine((name.length() <= 0) ? "Devi inserire un nome!" : "Il nome non può essere più lungo di 15 caratteri!");
			this.sendWithCancelButton(messageBuilder);
			return;
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
		
		this.sendWithCancelButton(builder);
	}
	
	private void manageAssignStatsState(Message receivedMessage)
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
			
			this.sendWithCancelButton(messageBuilder);
			return;
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
			
			this.sendWithCancelButton(messageBuilder);
			return;
		}
		
		if (sum != HungerGamesController.SUM_STATS)
		{
			MessageBuilder messageBuilder = new MessageBuilder()
					.appendFormat("\n> La somma dei valori delle caratteristiche deve essere %d punti! Somma dei valori inseriti: %s", HungerGamesController.SUM_STATS, sum);
			
			this.sendWithCancelButton(messageBuilder);
			return;
		}
		
		this.character.setStats(abilities);
		
		HungerGamesController.addCharacter(this.getGuild().getId(), this.character);
		MessageBuilder builder = new MessageBuilder()
				.append("> Creazione personaggio completata!");
		
		this.channel.sendMessage(builder.build()).queue();
		
		this.state = State.FINISHED;
		this.running = false;
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
