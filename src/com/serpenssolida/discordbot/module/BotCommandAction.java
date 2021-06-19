package com.serpenssolida.discordbot.module;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

/**
 * Used by {@link UnlistedBotCommand} as callback when the command is used in a chat.
 */
public interface BotCommandAction
{
	/**
	 * Called when the command is sent to the chat.
	 *
	 *
	 * @param guild
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
	boolean doAction(SlashCommandEvent event, Guild guild, MessageChannel channel, User author);
}
