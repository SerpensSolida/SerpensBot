package com.serpenssolida.discordbot.hungergames.task;

import com.serpenssolida.discordbot.Task;
import com.serpenssolida.discordbot.hungergames.Character;
import com.serpenssolida.discordbot.hungergames.HungerGamesController;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

public class EditCharacterTask extends Task
{
	private Character character;
	
	private State state;
	
	private Message reactionCheckMessage;
	
	public enum State
	{
		NOONE, NAME_CHARACTER, ASSIGN_STATS, IMAGE, FINISHED
	}
	
	public EditCharacterTask(User user, MessageChannel channel)
	{
		super(user, channel);
		this.state = State.NOONE;
		this.character = HungerGamesController.getCharacter(user.getId());
		if (this.character == null)
		{
			this.setInterrupted(true);
			return;
		}
		this.sendMenu();
	}
	
	public Task.TaskResult consumeMessage(Message receivedMessage)
	{
		
		Task.TaskResult result;
		
		
		if (!receivedMessage.getAuthor().equals(this.getUser()))
			return Task.TaskResult.NotFinished;
		
		switch (this.getState())
		{
			case NOONE:
				result = Task.TaskResult.NotFinished;
				return result;
			case NAME_CHARACTER:
				result = this.manageNameCharacterState(receivedMessage);
				return result;
			case ASSIGN_STATS:
				result = this.manageAssignStatsState(receivedMessage);
				return result;
		}
		
		receivedMessage.getChannel().sendMessage("Stato illegale, fai schifo con le MSF.").queue();
		result = TaskResult.Finished;
		return result;
	}
	
	public Task.TaskResult consumeReaction(Message message, String reactionName)
	{
		MessageBuilder builder = new MessageBuilder();
		if (this.getState() != State.NOONE)
			return Task.TaskResult.NotFinished;
		if (this.reactionCheckMessage.getId().equals(message.getId()))
			switch (reactionName)
			{
				case "🇦":
					this.state = State.NAME_CHARACTER;
					builder = (new MessageBuilder()).append("> Inserisci il nuovo nome del tuo personaggio. (max 15 caratteri)");
					break;
				case "🇧":
					builder = (new MessageBuilder()).appendFormat("> Stai modificando le caratteristiche di **%s**", this.getCharacter().getDisplayName()).append("\n> Assegna le caratteristiche al personaggio. Invia un messaggio con 7 numeri che rappresentano le caratteristiche del tuo personaggio.").append("\n> Le caratteristiche sono: ").append("Vitalità, Forza, Abilità, Special, Velocità, Resistenza e Gusto. ").appendFormat("\n> La somma dei valori delle caratteristiche deve essere %d punti.", Integer.valueOf(HungerGamesController.SUM_STATS));
					this.state = State.ASSIGN_STATS;
					break;
				case "🇨":
					this.getChannel().deleteMessageById(this.reactionCheckMessage.getId()).queue();
					this.state = State.FINISHED;
					return Task.TaskResult.Finished;
			}
		this.sendMessage(builder.build());
		return Task.TaskResult.NotFinished;
	}
	
	private Task.TaskResult manageNameCharacterState(Message receivedMessage)
	{
		String message = receivedMessage.getContentDisplay();
		String name = message.strip();
		if (name.length() <= 0 || name.length() > 16)
		{
			MessageBuilder messageBuilder = (new MessageBuilder()).appendCodeLine((name.length() <= 0) ? "Devi inserire un nome!" : "Il nome non può essere più lungo di 15 caratteri!");
			this.sendMessage(messageBuilder.build());
			return Task.TaskResult.NotFinished;
		}
		this.character.setName(name);
		HungerGamesController.save();
		this.state = State.NOONE;
		MessageBuilder builder = (new MessageBuilder()).appendFormat("Nuovo nome del personaggio: %s", this.character.getDisplayName());
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
		try
		{
			for (int i = 0; i < abilitiesList.length; i++)
			{
				abilities[i] = Integer.parseInt(abilitiesList[i]);
				sum += abilities[i];
			}
			System.out.println("" + HungerGamesController.SUM_STATS + " = " + HungerGamesController.SUM_STATS);
		}
		catch (NumberFormatException e)
		{
			MessageBuilder messageBuilder = (new MessageBuilder()).append("> Formato delle caratteristiche errato. Inserisci solo numeri!");
			this.sendMessage(messageBuilder.build());
			return Task.TaskResult.NotFinished;
		}
		if (abilities.length != 7)
		{
			MessageBuilder messageBuilder = (new MessageBuilder()).append("> Inserisci tutte le caratteristiche!\n").append("> Le caratteristiche sono: ").append("Vitalità, Forza, Abilità, Special, Velocità, Resistenza e Gusto.\n").append("> La somma dei valori delle caratteristiche deve essere 30 punti.");
			this.sendMessage(messageBuilder.build());
			return Task.TaskResult.NotFinished;
		}
		if (sum != HungerGamesController.SUM_STATS)
		{
			MessageBuilder messageBuilder = (new MessageBuilder()).appendFormat("> La somma dei valori delle caratteristiche deve essere %d punti!", Integer.valueOf(HungerGamesController.SUM_STATS));
			this.sendMessage(messageBuilder.build());
			return Task.TaskResult.NotFinished;
		}
		this.character.setStats(abilities);
		HungerGamesController.save();
		MessageBuilder builder = (new MessageBuilder()).append("> Caratteristiche impostate correttamente.");
		this.sendMessage(builder.build());
		this.state = State.NOONE;
		this.sendMenu();
		return Task.TaskResult.NotFinished;
	}
	
	public void sendMenu()
	{
		MessageBuilder builder = (new MessageBuilder()).append("> Cosa vuoi modificare del tuo personaggio?\n> \n").append("> \t:regional_indicator_a: - Nome.\n").append("> \t:regional_indicator_b: - Statistiche.\n").append("> \t:regional_indicator_c: - Esci.\n").append("> _\n");
		MessageAction messageAction = this.getChannel().sendMessage(builder.build());
		messageAction.queue(message ->
		{
			this.reactionCheckMessage = message;
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
