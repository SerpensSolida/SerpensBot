package com.serpenssolida.discordbot;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

/**
 * Used by {@link Command} as callback when the command is used in a chat.
 */
public interface CommandAction
{
	/**
	 * Called when the command is sent to the chat.
	 *
	 * @param channel
	 * 		Channel where the message was sent.
	 * @param message
	 * 		Message containing the command.
	 * @param author
	 * 		Author of the message.
	 * @param args
	 * 		Arguments passed to the command.
	 *
	 * @return
	 */
	boolean doAction(MessageChannel channel, Message message, User author, String[] args);
}
