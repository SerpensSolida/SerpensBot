package com.serpenssolida.discordbot.module;

import com.serpenssolida.discordbot.ButtonGroup;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
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
	
	public void start()
	{
		MessageBuilder builder = new MessageBuilder();
		
		//Reply with the starting message.
		this.channel.sendMessage(builder.build()).queue(this::setLastMessage);
	}
	
	public void start(GenericInteractionCreateEvent event)
	{
		MessageBuilder builder = new MessageBuilder();
		boolean ephemeral = this.startMessage(builder);
		
		//Reply with the starting message.
		//Refer to this: https://f8n-ipfs-production.imgix.net/QmaWn5w91si1shcFZfssY8yQQ58DpvVwQ4GoHyHnTx4dMA/nft.jpg?fit=fill&q=100&w=2560
		event.reply(builder.build())
				.setEphemeral(ephemeral)
				.queue(interactionHook ->
				{
					if (!ephemeral)
						interactionHook.retrieveOriginal().queue(this::setLastMessage);
				});
	}
	
	
	
	public abstract boolean startMessage(MessageBuilder builder);
	
	/**
	 * Consumes the given message and returns whether or not the task has finished.
	 *
	 * @param message
	 * 		The message the task will consume.
	 *
	 * @return Whether or not the task has finished and can be removed.
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
		MessageBuilder builder = new MessageBuilder();
		
		//TODO: Remove it, may be obsolete now.
		//Check if the player
		/*if (this.getLastMessage() != null && this.getLastMessage().getId().equals(message.getId()))
		{
			if ("❌".equals(reaction))
			{
				builder.append("> La procedura è stata annullata.");
				this.getChannel().sendMessage(builder.build()).queue();
				this.running = false;
				return;
			}
		}*/
		
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
	 * Send a message in the channel the task is running on and add a reaction tu it.
	 * The message will also be stored in the variable this.lastMessage.
	 *
	 * @param builder
	 * 		The builder that will be sent with the cancel button.
	 */
	public void sendWithCancelButton(MessageBuilder builder)
	{
		this.addCancelButton(builder);
		
		this.channel.sendMessage(builder.build()).queue(sentMessage ->
		{
			this.lastMessage = sentMessage;
		});
	}
	
	public void addCancelButton(MessageBuilder builder)
	{
		builder.setActionRows(ActionRow.of(Button.danger("cancel-task", "Esci dalla procedura")));
		this.registerCancelButton();
	}
	
	public void registerCancelButton()
	{
		if (this.buttonGroup == null)
			this.buttonGroup = new ButtonGroup(this.user);
		
		this.buttonGroup.addButton(new ButtonCallback("cancel-task", this.user, this.channel, this.CANCEL_BUTTON));
	}
	
	public void deleteButtons()
	{
		if (this.lastMessage != null)
		{
			MessageBuilder builder = new MessageBuilder()
					.append(this.lastMessage.getContentDisplay());
			this.lastMessage.editMessage(builder.build()).queue();
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
	
	public final ButtonAction CANCEL_BUTTON = (event, guild, channel, message, author) ->
	{
		MessageBuilder b = new MessageBuilder();
		b.append(event.getMessage().getContentDisplay());//.append("> La procedura è stata annullata.");
		//this.getChannel().sendMessage(b.build()).queue();
		this.running = false;
		
		event.deferEdit().queue();
		event.getHook().deleteOriginal().queue();
		//event.getHook().editOriginal(b.build()).queue(); //Remove buttons from the original message.
		
		this.buttonGroup = null;
		return true;
	};
}
