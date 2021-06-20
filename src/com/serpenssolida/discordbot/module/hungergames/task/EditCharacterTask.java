package com.serpenssolida.discordbot.module.hungergames.task;

import com.serpenssolida.discordbot.ButtonGroup;
import com.serpenssolida.discordbot.module.ButtonCallback;
import com.serpenssolida.discordbot.module.Task;
import com.serpenssolida.discordbot.module.hungergames.Character;
import com.serpenssolida.discordbot.module.hungergames.HungerGamesController;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;

import static com.serpenssolida.discordbot.module.hungergames.HungerGamesController.SUM_STATS;

public class EditCharacterTask extends Task
{
	private Character character;
	private State state;
	private Message reactionCheckMessage;
	
	public EditCharacterTask(Guild guild, User user, MessageChannel channel)
	{
		super(guild, user, channel);
	}
	
	private enum State
	{
		MENU,
		NAME_CHARACTER,
		ASSIGN_STATS,
		ASSIGN_AVATAR, //TODO: Ability to send avatar of the character.
		FINISHED
	}
	
	@Override
	public boolean startMessage(MessageBuilder builder)
	{
		//Abort task if there is an HungerGames running.
		if (HungerGamesController.isHungerGamesRunning(this.getGuild().getId()))
		{
			builder.append("> Non puoi usare questo comando mentre è in corso un HungerGames.");
			//this.channel.sendMessage(builder.build()).queue();
			this.setInterrupted(true);
			return true;
		}
		
		//Initialize task.
		this.state = State.MENU;
		this.character = HungerGamesController.getCharacter(this.getGuild().getId(), this.user.getId());
		
		if (this.character == null)
		{
			builder.append("> Non è stato trovato nessun personaggio.");
			this.setInterrupted(true);
			return true;
		}
		
		//Send a message with the menu.
		this.createMenuMessage(builder);
		return false;
	}
	
	public void consumeMessage(Message receivedMessage)
	{
		//Check if the user that started the task is the one who sent the message (This check should return ALWAYS true).
		if (!receivedMessage.getAuthor().equals(this.getUser()))
			return;
		
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
			case MENU: //Waiting for reaction.
				return;
			case NAME_CHARACTER: //Naming the character.
				this.manageNameCharacterState(receivedMessage);
				return;
			case ASSIGN_STATS: //Assigning stats to the character.
				this.manageAssignStatsState(receivedMessage);
				return;
		}
		
		//This state is not possible, you broke my FSM :(
		receivedMessage.getChannel().sendMessage("Stato illegale, fai schifo con le MSF.").queue();
		
		this.running = false;
	}
	
	public void reactionAdded(Message message, String reaction)
	{
		/*MessageBuilder builder = new MessageBuilder();
		
		//Abort task if there is an HungerGames running.
		if (HungerGamesController.isHungerGamesRunning(this.getGuild().getId()))
		{
			builder = new MessageBuilder()
					.append("> Non puoi completare la procedura perchè è in corso un HungerGames.");
			this.sendMessage(builder.build());
			
			this.running = false;
			return;
		}
		
		//Check if the reaction is added to the menu.
		if (this.reactionCheckMessage != null && this.reactionCheckMessage.getId().equals(message.getId()))
		{
			if (this.getState() != State.MENU)
				return;
			
			switch (reaction)
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
				
				case "❌":
					this.getChannel().deleteMessageById(this.reactionCheckMessage.getId()).queue(); //Delete the menu.
					
					this.state = State.FINISHED;
					this.running = false;
					return;
			}
			
			this.getChannel().deleteMessageById(this.reactionCheckMessage.getId()).queue();
		}
		
		this.sendWithCancelButton(builder);*/
	}
	
	private void manageNameCharacterState(Message receivedMessage)
	{
		String message = receivedMessage.getContentDisplay();
		String name = message.strip();
		
		//Check if the name is empty or its length is greater than 16.
		if (name.length() <= 0 || name.length() > 16)
		{
			MessageBuilder messageBuilder = new MessageBuilder();
			messageBuilder.appendCodeLine((name.length() <= 0) ? "Devi inserire un nome!" : "Il nome non può essere più lungo di 16 caratteri!");
			
			this.sendWithCancelButton(messageBuilder);
			return;
		}
		
		//Set the name of the character.
		this.character.setName(name);
		HungerGamesController.save(this.getGuild().getId());
		
		this.state = State.MENU; //Back to menu.
		
		MessageBuilder builder = new MessageBuilder();
		builder.appendFormat("Nuovo nome del personaggio: %s", this.character.getDisplayName());
		
		this.getChannel().sendMessage(builder.build()).queue();
		this.channel.sendMessage(this.createMenuMessage().build()).queue();
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
					.append("> Inserisci tutte le caratteristiche!\n")
					.append("> Le caratteristiche sono: ")
					.append("Vitalità, Forza, Abilità, Special, Velocità, Resistenza e Gusto.\n")
					.appendFormat("> La somma dei valori delle caratteristiche deve essere %s punti e ogni caratteristica deve essere compresa tra 0 e 10!", SUM_STATS);
			
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
		
		//Sum of ability values must be equal to SUM_STATS.
		if (sum != SUM_STATS)
		{
			MessageBuilder messageBuilder = new MessageBuilder()
					.appendFormat("\n> La somma dei valori delle caratteristiche deve essere %d punti! Somma dei valori inseriti: %s", HungerGamesController.SUM_STATS, sum);
			
			this.sendWithCancelButton(messageBuilder);
			return;
		}
		
		//Set character stats.
		this.character.setStats(abilities);
		HungerGamesController.save(this.getGuild().getId());
		
		MessageBuilder builder = new MessageBuilder()
				.append("> Caratteristiche impostate correttamente.");
		
		this.getChannel().sendMessage(builder.build()).queue();
		this.state = State.MENU;
		
		//Send the menu.
		this.channel.sendMessage(this.createMenuMessage().build()).queue();
	}
	
	/**
	 * Create a builder that contains the menu of the task to the channel.
	 */
	public MessageBuilder createMenuMessage()
	{
		MessageBuilder builder = new MessageBuilder()
				.append("> Seleziona cosa vuoi modificare del tuo personaggio.\n");
				//.append("> \t:regional_indicator_a: - Nome.\n")
				//.append("> \t:regional_indicator_b: - Statistiche.\n")
				//.append("> \t:x: - Esci.\n");
		
		Button editName = Button.primary("edit-name", "Modifica il nome");
		Button editStats = Button.primary("edit-stats", "Modifica le caratteristiche");
		Button cancelTask = Button.danger("cancel-task", "Esci");
		
		this.buttonGroup = new ButtonGroup(this.user);
		
		this.buttonGroup.addButton(new ButtonCallback("edit-name", this.user, this.channel, (event, guild, channel, message, author) ->
		{
			this.state = State.NAME_CHARACTER;
			
			MessageBuilder b = new MessageBuilder()
					.append("> Inserisci il nuovo nome del tuo personaggio. (max 15 caratteri)");
			
			event.deferEdit().queue();
			event.getHook().deleteOriginal().queue(); //Remove the original message.
			
			this.buttonGroup = null;
			this.sendWithCancelButton(b);
			
			return true;
		}));
		
		this.buttonGroup.addButton(new ButtonCallback("edit-stats", this.user, this.channel, (event, guild, channel, message, author) ->
		{
			this.state = State.ASSIGN_STATS;
			MessageBuilder b = new MessageBuilder();
			
			event.deferEdit().queue();
			event.getHook().deleteOriginal().queue(); //Remove the original message.
			
			b.appendFormat("> Stai modificando le caratteristiche di **%s**", this.getCharacter().getDisplayName())
					.append("\n> Assegna le caratteristiche al personaggio. Invia un messaggio con 7 numeri separati da uno spazio che rappresentano le caratteristiche del tuo personaggio.")
					.append("\n> Le caratteristiche sono: ")
					.append("Vitalità, Forza, Abilità, Special, Velocità, Resistenza e Gusto. ")
					.appendFormat("\n> La somma dei valori delle caratteristiche deve essere %d punti e ogni carateristica deve essere compresa tra 0 e 10.", SUM_STATS);
			
			this.buttonGroup = null;
			this.sendWithCancelButton(b);

			return true;
		}));
		
		this.registerCancelButton();
		
		//Add button to the message.
		builder.setActionRows(ActionRow.of(editName, editStats, cancelTask));
		//this.getChannel().sendMessage(builder.build()).queue();
		return builder;

		/*//Add the reaction to the menu.
		messageAction.queue(message ->
		{
			this.reactionCheckMessage = message; //This message is the one used for checking the reaction.
			message.addReaction("🇦").queue();
			message.addReaction("🇧").queue();
			message.addReaction("❌").queue();
		});*/
	}
	
	/**
	 * Create a builder that contains the menu of the task to the channel.
	 */
	public void createMenuMessage(MessageBuilder builder)
	{
		builder.append("> Seleziona cosa vuoi modificare del tuo personaggio.\n");
		
		Button editName = Button.primary("edit-name", "Modifica il nome");
		Button editStats = Button.primary("edit-stats", "Modifica le caratteristiche");
		Button cancelTask = Button.danger("cancel-task", "Esci");
		
		this.buttonGroup = new ButtonGroup(this.user);
		
		this.buttonGroup.addButton(new ButtonCallback("edit-name", this.user, this.channel, (event, guild, channel, message, author) ->
		{
			this.state = State.NAME_CHARACTER;
			
			MessageBuilder b = new MessageBuilder()
					.append("> Inserisci il nuovo nome del tuo personaggio. (max 15 caratteri)");
			
			event.deferEdit().queue();
			event.getHook().deleteOriginal().queue(); //Remove the original message.
			
			this.buttonGroup = null;
			this.sendWithCancelButton(b);
			
			return true;
		}));
		
		this.buttonGroup.addButton(new ButtonCallback("edit-stats", this.user, this.channel, (event, guild, channel, message, author) ->
		{
			this.state = State.ASSIGN_STATS;
			MessageBuilder b = new MessageBuilder();
			
			event.deferEdit().queue();
			event.getHook().deleteOriginal().queue(); //Remove the original message.
			
			b.appendFormat("> Stai modificando le caratteristiche di **%s**", this.getCharacter().getDisplayName())
					.append("\n> Assegna le caratteristiche al personaggio. Invia un messaggio con 7 numeri separati da uno spazio che rappresentano le caratteristiche del tuo personaggio.")
					.append("\n> Le caratteristiche sono: ")
					.append("Vitalità, Forza, Abilità, Special, Velocità, Resistenza e Gusto. ")
					.appendFormat("\n> La somma dei valori delle caratteristiche deve essere %d punti e ogni carateristica deve essere compresa tra 0 e 10.", SUM_STATS);
			
			this.buttonGroup = null;
			this.sendWithCancelButton(b);
			
			return true;
		}));
		
		this.registerCancelButton();
		
		//Add button to the message.
		builder.setActionRows(ActionRow.of(editName, editStats, cancelTask));
		//this.getChannel().sendMessage(builder.build()).queue();

		/*//Add the reaction to the menu.
		messageAction.queue(message ->
		{
			this.reactionCheckMessage = message; //This message is the one used for checking the reaction.
			message.addReaction("🇦").queue();
			message.addReaction("🇧").queue();
			message.addReaction("❌").queue();
		});*/
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
