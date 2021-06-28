package com.serpenssolida.discordbot.module;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

/**
 * Used by {@link UnlistedBotCommand} as callback when the command is used in a chat.
 */
public interface UnlistedBotCommandAction
{
	/**
	 * Called when the command is sent to the chat.
	 *
	 * @param guild
	 * 		The guild the commands was sent at.
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
	 * 		Whether or not to delete the command message.
	 */
	boolean doAction(Guild guild, MessageChannel channel, Message message, User author, String[] args);
}
