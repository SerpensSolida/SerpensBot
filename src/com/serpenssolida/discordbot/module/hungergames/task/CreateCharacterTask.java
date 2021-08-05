package com.serpenssolida.discordbot.module.hungergames.task;

import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.module.Task;
import com.serpenssolida.discordbot.module.hungergames.Character;
import com.serpenssolida.discordbot.module.hungergames.HungerGamesController;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateCharacterTask extends Task
{
	private Character character;
	private State state;
	
	private static final Logger logger = LoggerFactory.getLogger(CreateCharacterTask.class);
	
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
	public boolean startMessage(MessageBuilder messageBuilder)
	{
		//Abort task if there is an HungerGames running.
		if (HungerGamesController.isHungerGamesRunning(this.getGuild().getId()))
		{
			EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Creazione del personaggio", this.getUser())
					.setDescription("Non puoi usare questo comando perchè è in corso un HungerGames.");
			messageBuilder.setEmbeds(embedBuilder.build());

			this.setInterrupted(true);
			this.setRunning(false);
			return true;
		}
		
		//Create message.
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Creazione del personaggio", this.getUser())
				.setDescription("Stai creando un personaggio! Inserisci il nome del tuo personaggio. (max 16 caratteri)");
		messageBuilder.setEmbeds(embedBuilder.build());

		//Add cancel button to the message.
		this.addCancelButton(messageBuilder);
		
		this.state = State.NAME_CHARACTER;
		this.character = new Character(this.getUser().getId());
		return false;
	}
	
	public void consumeMessage(Message receivedMessage)
	{
		if (!receivedMessage.getAuthor().equals(this.getUser()))
			return;
		
		//Remove buttons from last message.
		this.deleteLastMessageComponents();
		
		//Abort task if there is an HungerGames running.
		if (HungerGamesController.isHungerGamesRunning(this.getGuild().getId()))
		{
			Message message = MessageUtils.buildErrorMessage("Creazione del personaggio", this.getUser(), "Non puoi completare la procedura perchè è in corso un HungerGames.");
			this.getChannel().sendMessage(message).queue();
			
			this.setRunning(false);
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
		this.setRunning(false);
	}
	
	public void reactionAdded(Message message, String reaction) {}
	
	private void manageNameCharacterState(Message receivedMessage)
	{
		String message = receivedMessage.getContentDisplay();
		String name = message.strip();
		
		if (name.length() <= 0 || name.length() > 16)
		{
			EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Creazione del personaggio", this.getUser())
					.setDescription((name.length() <= 0) ? "Devi inserire un nome!" : "Il nome non può essere più lungo di 15 caratteri!");
			MessageBuilder messageBuilder = new MessageBuilder()
					.setEmbeds(embedBuilder.build());
			
			this.sendWithCancelButton(messageBuilder);
			return;
		}
		
		this.character.setName(name);
		this.state = State.ASSIGN_STATS;
		
		logger.info("Nome assegnato: " + name);
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Creazione del personaggio", this.getUser());
		
		embedBuilder
				.appendDescription("Nome selezionato: **")
				.appendDescription(name)
				.appendDescription("**\nAdesso assegna le caratteristiche al personaggio. Invia un messaggio con 7 numeri separati da uno spazio che rappresentano le caratteristiche del tuo personaggio.")
				.appendDescription("\nLe caratteristiche sono: ")
				.appendDescription("Vitalità, Forza, Abilità, Special, Velocità, Resistenza e Gusto. ")
				.appendDescription("\nLa somma dei valori delle caratteristiche deve essere " + HungerGamesController.SUM_STATS + " punti e ogni caratteristica deve essere compresa tra 0 e 10!");
		
		MessageBuilder messageBuilder = new MessageBuilder()
				.setEmbeds(embedBuilder.build());
		this.sendWithCancelButton(messageBuilder);
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
			EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Creazione del personaggio", this.getUser())
					.appendDescription("Inserisci tutte le caratteristiche!")
					.appendDescription("\nLe caratteristiche sono: ")
					.appendDescription("Vitalità, Forza, Abilità, Special, Velocità, Resistenza e Gusto. ")
					.appendDescription("\nLa somma dei valori delle caratteristiche deve essere " + HungerGamesController.SUM_STATS + " punti e ogni caratteristica deve essere compresa tra 0 e 10!");
			
			MessageBuilder messageBuilder = new MessageBuilder()
					.setEmbeds(embedBuilder.build());
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
			EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Creazione del personaggio", this.getUser())
				.appendDescription("Formato delle caratteristiche errato. Inserisci solo numeri tra 0 e 10!");
			
			MessageBuilder messageBuilder = new MessageBuilder()
					.setEmbeds(embedBuilder.build());
			
			this.sendWithCancelButton(messageBuilder);
			return;
		}
		
		if (sum != HungerGamesController.SUM_STATS)
		{
			EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Creazione del personaggio", this.getUser())
					.appendDescription("La somma dei valori delle caratteristiche deve essere " +  HungerGamesController.SUM_STATS + " punti! Somma dei valori inseriti: " + sum);
			
			MessageBuilder messageBuilder = new MessageBuilder()
					.setEmbeds(embedBuilder.build());
			
			this.sendWithCancelButton(messageBuilder);
			return;
		}
		
		this.character.setStats(abilities);
		
		HungerGamesController.addCharacter(this.getGuild().getId(), this.character);
		
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Creazione del personaggio", this.getUser())
				.appendDescription("Creazione personaggio completata");
		
		MessageBuilder messageBuilder = new MessageBuilder()
				.setEmbed(embedBuilder.build());
		
		this.getChannel().sendMessage(messageBuilder.build()).queue();
		
		this.state = State.FINISHED;
		this.setRunning(false);
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
