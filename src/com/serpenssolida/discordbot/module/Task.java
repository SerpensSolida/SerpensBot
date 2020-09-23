package com.serpenssolida.discordbot.module;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

/**
 * A Task is a routine that gets input from non-command messages sent by a specific {@link User} to a specific {@link MessageChannel}.
 */
public abstract class Task
{
	private User user; //The user doing this task.
	private MessageChannel channel; //The channel the task is running on.
	private Message lastMessage; //Last message sent by the task.
	private boolean interrupted; //If the task was cancelled or not.
	
	public enum TaskResult
	{
		NotFinished,
		Finished
	}
	
	public Task(User user, MessageChannel channel)
	{
		this.user = user;
		this.channel = channel;
		this.interrupted = false;
	}
	
	/**
	 * Consumes the given message and returns whether or not the task has finished.
	 *
	 * @param message
	 * 		The message the task will consume.
	 *
	 * @return Whether or not the task has finished and can be removed.
	 */
	public abstract TaskResult consumeMessage(Message message);
	
	
	/**
	 * Consumes the given reaction that has been added in the given message and returns whether or not the task has finished.
	 *
	 * @param message
	 * 		The message that the reaction has been added on.
	 * @param reaction
	 * 		The reaction the task will consume.
	 *
	 * @return Whether or not the task has finished.
	 */
	protected abstract TaskResult reactionAdded(Message message, String reaction);
	
	/**
	 * Check if the reaction added by the user is the :x: reaction. If yes the task will be aborted if not method reactionAdded
	 * will be called.
	 *
	 * @param message
	 * 		The message that the reaction has been added on.
	 * @param reaction
	 * 		The reaction the task will consume.
	 *
	 * 	@return Whether or not the task has finished.
	 */
	public TaskResult consumeReaction(Message message, String reaction)
	{
		MessageBuilder builder = new MessageBuilder();
		
		//Check if the player
		if (this.getLastMessage() != null && this.getLastMessage().getId().equals(message.getId()))
		{
			if ("❌".equals(reaction))
			{
				builder.append("> La procedura è stata annullata.");
				this.getChannel().sendMessage(builder.build()).queue();
				
				return TaskResult.Finished;
			}
		}
		
		return this.reactionAdded(message, reaction);
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
	 * @param message
	 * 		Message to send to the channel.
	 */
	public void sendMessageWithAbort(Message message)
	{
		this.getChannel().sendMessage(message).queue(sentMessage ->
		{
			this.lastMessage = sentMessage;
			sentMessage.addReaction("❌").queue();
		});
	}
	
	/**
	 * @return The user doing the task.
	 */
	public User getUser()
	{
		return this.user;
	}
	
	/*public void setUser(User user)
	{
		this.user = user;
	}*/
	
	/**
	 * @return The channel the task is currently running on.
	 */
	public MessageChannel getChannel()
	{
		return this.channel;
	}
	
	/*public void setChannel(MessageChannel channel)
	{
		this.channel = channel;
	}*/
	
	
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
}
