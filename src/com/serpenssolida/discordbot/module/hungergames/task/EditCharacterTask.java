package com.serpenssolida.discordbot.module.hungergames.task;

import com.serpenssolida.discordbot.module.ButtonGroup;
import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.module.ButtonCallback;
import com.serpenssolida.discordbot.module.Task;
import com.serpenssolida.discordbot.module.hungergames.Character;
import com.serpenssolida.discordbot.module.hungergames.HungerGamesController;
import net.dv8tion.jda.api.EmbedBuilder;
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
	}
	
	@Override
	public boolean startMessage(MessageBuilder messageBuilder)
	{
		//Abort task if there is an HungerGames running.
		if (HungerGamesController.isHungerGamesRunning(this.getGuild().getId()))
		{
			EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Modifica del personaggio", this.getUser())
					.setDescription("Non puoi usare questo comando perchè è in corso un HungerGames.");
			messageBuilder.setEmbed(embedBuilder.build());
			
			this.setInterrupted(true);
			this.running = false;
			return true;
		}
		
		//Initialize task.
		this.state = State.MENU;
		this.character = HungerGamesController.getCharacter(this.getGuild().getId(), this.user.getId());
		
		if (this.character == null)
		{
			EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Modifica del personaggio", this.getUser())
					.setDescription("Non è stato trovato nessun personaggio.");
			messageBuilder.setEmbed(embedBuilder.build());

			this.setInterrupted(true);
			this.running = false;
			return true;
		}
		
		//Send a message with the menu.
		this.insertMenu(messageBuilder);
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
			this.deleteButtons();
			
			EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Modifica del personaggio", this.getUser())
					.setDescription("Non puoi completare la procedura perchè è in corso un HungerGames.");
			MessageBuilder messageBuilder = new MessageBuilder()
					.setEmbed(embedBuilder.build());
			
			this.channel.sendMessage(messageBuilder.build()).queue();
			
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
		this.channel.sendMessage("Stato illegale, fai schifo con le MSF.").queue();
		this.deleteButtons();
		
		this.running = false;
	}
	
	public void reactionAdded(Message message, String reaction) {}
	
	private void manageNameCharacterState(Message receivedMessage)
	{
		String message = receivedMessage.getContentDisplay();
		String name = message.strip();
		
		//Remove buttons from last message.
		this.deleteButtons();
		
		//Check if the name is empty or its length is greater than 16.
		if (name.length() <= 0 || name.length() > 16)
		{
			EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Modifica nome del personaggio", this.getUser())
					.appendDescription((name.length() <= 0) ? "Devi inserire un nome!" : "Il nome non può essere più lungo di 16 caratteri!");
			
			MessageBuilder messageBuilder = new MessageBuilder()
					.setEmbed(embedBuilder.build());
			
			this.sendWithCancelButton(messageBuilder);
			return;
		}
		
		//Set the name of the character.
		this.character.setName(name);
		HungerGamesController.save(this.getGuild().getId());
		
		this.state = State.MENU; //Back to menu.
		
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Modifica del personaggio", this.getUser())
				.appendDescription("Nuovo nome del personaggio: " + this.character.getDisplayName());
		
		MessageBuilder messageBuilder = new MessageBuilder()
				.setEmbed(embedBuilder.build());
		
		this.getChannel().sendMessage(messageBuilder.build()).queue();
		this.channel.sendMessage(this.createMenuMessage().build()).queue();
	}
	
	private void manageAssignStatsState(Message receivedMessage)
	{
		String message = receivedMessage.getContentDisplay();
		String[] abilitiesList = message.strip().replaceAll(" +", " ").split(" ");
		int[] abilities = new int[abilitiesList.length];
		int sum = 0;
		
		//Remove buttons from last message.
		this.deleteButtons();
		
		//Check number of ability sent with the message.
		if (abilities.length != 7)
		{
			EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Modifica caratteristiche del personaggio", this.getUser())
					.appendDescription("Inserisci tutte le caratteristiche!\n")
					.appendDescription("Le caratteristiche sono: ")
					.appendDescription("Vitalità, Forza, Abilità, Special, Velocità, Resistenza e Gusto.\n")
					.appendDescription("La somma dei valori delle caratteristiche deve essere " + HungerGamesController.SUM_STATS + " punti e ogni caratteristica deve essere compresa tra 0 e 10!");
			
			
			MessageBuilder messageBuilder = new MessageBuilder()
					.setEmbed(embedBuilder.build());
			
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
			EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Modifica caratteristiche del personaggio", this.getUser())
					.appendDescription("Formato delle caratteristiche errato. Inserisci solo numeri tra 0 e 10!");
			
			MessageBuilder messageBuilder = new MessageBuilder()
					.setEmbed(embedBuilder.build());
			
			this.sendWithCancelButton(messageBuilder);
			return;
		}
		
		//Sum of ability values must be equal to SUM_STATS.
		if (sum != SUM_STATS)
		{
			EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Modifica caratteristiche del personaggio", this.getUser())
					.appendDescription("La somma dei valori delle caratteristiche deve essere " + HungerGamesController.SUM_STATS + " punti! Somma dei valori inseriti: " + sum);
			
			MessageBuilder messageBuilder = new MessageBuilder()
					.setEmbed(embedBuilder.build());
			
			this.sendWithCancelButton(messageBuilder);
			return;
		}
		
		//Set character stats.
		this.character.setStats(abilities);
		HungerGamesController.save(this.getGuild().getId());
		
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Modifica caratteristiche del personaggio", this.getUser())
				.appendDescription("Caratteristiche impostate correttamente");
		
		MessageBuilder messageBuilder = new MessageBuilder()
				.setEmbed(embedBuilder.build());
		
		this.getChannel().sendMessage(messageBuilder.build()).queue();
		this.state = State.MENU;
		
		//Send the menu.
		this.channel.sendMessage(this.createMenuMessage().build()).queue();
	}
	
	/**
	 * Create a MessageBuilder that contains the menu of the task to the message.
	 */
	public MessageBuilder createMenuMessage()
	{
		MessageBuilder messageBuilder = new MessageBuilder();
		
		this.insertMenu(messageBuilder);
		
		return messageBuilder;
				//.append("> \t:regional_indicator_a: - Nome.\n")
				//.append("> \t:regional_indicator_b: - Statistiche.\n")
				//.append("> \t:x: - Esci.\n");
		
		/*Button editName = Button.primary("edit-name", "Modifica il nome");
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
		messageBuilder.setActionRows(ActionRow.of(editName, editStats, cancelTask));
		//this.getChannel().sendMessage(messageBuilder.build()).queue();
		return messageBuilder;*/

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
	 * Add the menu of the task to the given MessageBuilder.
	 */
	public void insertMenu(MessageBuilder messageBuilder)
	{
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Modifica del personaggio", this.getUser())
				.appendDescription("Seleziona cosa vuoi modificare del tuo personaggio.\n");
		
		messageBuilder.setEmbed(embedBuilder.build());
		
		Button editName = Button.primary("edit-name", "Modifica il nome");
		Button editStats = Button.primary("edit-stats", "Modifica le caratteristiche");
		Button cancelTask = Button.danger("cancel-task", "Esci");
		
		this.buttonGroup = new ButtonGroup();
		
		this.buttonGroup.addButton(new ButtonCallback("edit-name", (event, guild, channel, message, author) ->
		{
			this.state = State.NAME_CHARACTER;
			
			EmbedBuilder embedB = MessageUtils.getDefaultEmbed("Modifica nome del personaggio", this.getUser())
					.appendDescription("Inserisci il nuovo nome del tuo personaggio. (max 15 caratteri)");
			
			MessageBuilder messageB = new MessageBuilder()
					.setEmbed(embedB.build());
			
			event.deferEdit().queue();
			event.getHook().deleteOriginal().queue(); //Remove the original message.
			
			this.buttonGroup = null;
			this.sendWithCancelButton(messageB);
			
			return ButtonCallback.LEAVE_MESSAGE;
		}));
		
		this.buttonGroup.addButton(new ButtonCallback("edit-stats", (event, guild, channel, message, author) ->
		{
			this.state = State.ASSIGN_STATS;
			MessageBuilder b = new MessageBuilder();
			
			event.deferEdit().queue();
			event.getHook().deleteOriginal().queue(); //Remove the original message.
			
			EmbedBuilder embedB = MessageUtils.getDefaultEmbed("Modifica caratteristiche del personaggio", this.getUser())
					.appendDescription("Stai modificando le caratteristiche di **" + this.getCharacter().getDisplayName() + "**")
					.appendDescription("\n Assegna le caratteristiche al personaggio. Invia un messaggio con 7 numeri separati da uno spazio che rappresentano le caratteristiche del tuo personaggio.")
					.appendDescription("\nLe caratteristiche sono: ")
					.appendDescription("Vitalità, Forza, Abilità, Special, Velocità, Resistenza e Gusto. ")
					.appendDescription("\nLa somma dei valori delle caratteristiche deve essere " + HungerGamesController.SUM_STATS + " punti e ogni carateristica deve essere compresa tra 0 e 10.");
			
			MessageBuilder messageB = new MessageBuilder()
					.setEmbed(embedB.build());
			
			this.buttonGroup = null;
			this.sendWithCancelButton(messageB);
			
			return ButtonCallback.LEAVE_MESSAGE;
		}));
		
		this.registerCancelButton();
		
		//Add button to the message.
		messageBuilder.setActionRows(ActionRow.of(editName, editStats, cancelTask));
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
