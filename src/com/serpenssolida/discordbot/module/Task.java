package com.serpenssolida.discordbot.module;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;

/**
 * A Task is a routine that gets input from non-command messages sent by a specific {@link User} to a specific {@link MessageChannel}.
 */
public abstract class Task
{
	protected User user; //The user doing this task.
	protected MessageChannel channel; //The channel the task is running on.
	protected Message lastMessage; //Last message sent by the task.
	protected Guild guild;
	protected boolean interrupted; //If the task was cancelled or not.
	protected boolean running; //If the task is running or not.
	protected ButtonGroup buttonGroup; //Buttons that the user can press.
	
	public Task(Guild guild, User user, MessageChannel channel)
	{
		this.guild = guild;
		this.user = user;
		this.channel = channel;
		this.interrupted = false;
		this.running = true;
	}
	
	/**
	 * Starts the task without using an event.
	 */
	public void start()
	{
		MessageBuilder messageBuilder = new MessageBuilder();
		
		//Reply with the starting message.
		this.channel.sendMessage(messageBuilder.build()).queue(this::setLastMessage);
	}
	
	/**
	 * Starts the task using the given event.
	 */
	public void start(GenericInteractionCreateEvent event)
	{
		MessageBuilder messageBuilder = new MessageBuilder();
		boolean ephemeral = this.startMessage(messageBuilder);
		
		//Reply with the starting message.
		//Refer to this: https://f8n-ipfs-production.imgix.net/QmaWn5w91si1shcFZfssY8yQQ58DpvVwQ4GoHyHnTx4dMA/nft.jpg?fit=fill&q=100&w=2560
		event.reply(messageBuilder.build())
				.setEphemeral(ephemeral)
				.queue(interactionHook ->
				{
					if (!ephemeral)
						interactionHook.retrieveOriginal().queue(this::setLastMessage);
				});
	}
	
	/**
	 * Fills the given {@link MessageBuilder} with the start message of the task.
	 *
	 * @param messageBuilder
	 * 		The {@link MessageBuilder} to fill.
	 *
	 * @return
	 * 		True if the message will be an ephemeral message. false otherwise.
	 */
	public abstract boolean startMessage(MessageBuilder messageBuilder);
	
	/**
	 * Consumes the given message and returns whether or not the task has finished.
	 *
	 * @param message
	 * 		The message the task will consume.
	 *
	 */
	public abstract void consumeMessage(Message message);
	
	/**
	 * Consumes the given reaction that has been added in the given message and returns whether or not the task has finished.
	 *
	 * @param message
	 * 		The message that the reaction has been added on.
	 * @param reaction
	 * 		The reaction the task will consume.
	 */
	protected abstract void reactionAdded(Message message, String reaction);
	
	/**
	 * Check if the reaction added by the user is the :x: reaction. If yes the task will be aborted if not method reactionAdded
	 * will be called.
	 *
	 * @param message
	 * 		The message that the reaction has been added on.
	 * @param reaction
	 * 		The reaction the task will consume.
	 */
	public void consumeReaction(Message message, String reaction)
	{
		//Call the reaction added event.
		this.reactionAdded(message, reaction);
	}
	
	/**
	 * Send a message in the channel the task is running on.
	 *
	 * @param message
	 * 		Message to send to the channel.
	 */
	public void sendMessage(Message message)
	{
		this.getChannel().sendMessage(message).queue();
	}
	
	/**
	 * Sends a message in the channel the task is running on and add a reaction to it.
	 * The message will also be stored in the variable this.lastMessage.
	 *
	 * @param messageBuilder
	 * 		The messageBuilder that will be sent with the cancel button.
	 */
	public void sendWithCancelButton(MessageBuilder messageBuilder)
	{
		this.addCancelButton(messageBuilder);
		
		this.channel.sendMessage(messageBuilder.build()).queue(sentMessage ->
		{
			this.lastMessage = sentMessage;
		});
	}
	
	/**
	 * Adds a button, used to cancel the task, to the given {@link MessageBuilder}.
	 *
	 * @param messageBuilder
	 * 		The {@link MessageBuilder} where the buttons will be added.
	 */
	public void addCancelButton(MessageBuilder messageBuilder)
	{
		messageBuilder.setActionRows(ActionRow.of(Button.danger("cancel-task", "Esci dalla procedura")));
		this.registerCancelButton();
	}
	
	/**
	 * Register a "cancel" button that will cancel the task.
	 */
	public void registerCancelButton()
	{
		if (this.buttonGroup == null)
			this.buttonGroup = new ButtonGroup();
		
		this.buttonGroup.addButton(new ButtonCallback("cancel-task", this.CANCEL_BUTTON));
	}
	
	/**
	 * Removes the buttons from the last message.
	 */
	public void deleteButtons()
	{
		if (this.lastMessage != null)
		{
			//Copy the last message and edits it without the buttons.
			MessageBuilder messageBuilder = new MessageBuilder()
					.append(this.lastMessage.getContentDisplay());
			
			for (MessageEmbed embed : this.lastMessage.getEmbeds())
			{
				messageBuilder.setEmbed(embed);
			}
			
			this.lastMessage.editMessage(messageBuilder.build()).queue();
		}
	}
	
	/**
	 * @return The guild the task is running on.
	 */
	public Guild getGuild()
	{
		return this.guild;
	}
	
	/**
	 * @return The user doing the task.
	 */
	public User getUser()
	{
		return this.user;
	}
	
	/**
	 * @return The channel the task is currently running on.
	 */
	public MessageChannel getChannel()
	{
		return this.channel;
	}
	
	public boolean isRunning()
	{
		return this.running;
	}
	
	public void setRunning(boolean running)
	{
		this.running = running;
	}
	
	public boolean isInterrupted()
	{
		return this.interrupted;
	}
	
	public void setInterrupted(boolean interrupted)
	{
		this.interrupted = interrupted;
	}
	
	public Message getLastMessage()
	{
		return this.lastMessage;
	}
	
	public void setLastMessage(Message lastMessage)
	{
		this.lastMessage = lastMessage;
	}
	
	public ButtonGroup getButtonGroup()
	{
		return this.buttonGroup;
	}
	
	public void setButtonGroup(ButtonGroup buttonGroup)
	{
		this.buttonGroup = buttonGroup;
	}
	
	/**
	 * This is the default callback used by the "cancel" button to cancel the task.
	 */
	public final ButtonAction CANCEL_BUTTON = (event, guild, channel, message, author) ->
	{
		MessageBuilder b = new MessageBuilder(event.getMessage());
		b.setActionRows();
		//b.append(event.getMessage().getContentDisplay());//.append("> La procedura è stata annullata.");
		//this.getChannel().sendMessage(b.build()).queue();
		this.running = false;
		
		event.deferEdit().queue();
		event.getHook().deleteOriginal().queue();
		//event.getHook().editOriginal(b.build()).queue(); //Remove buttons from the original message.
		
		this.buttonGroup = null;
		return ButtonCallback.DELETE_MESSAGE;
	};
}
