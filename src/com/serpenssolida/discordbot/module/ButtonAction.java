package com.serpenssolida.discordbot.module;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

/**
 * The action performed by a Discord button.
 */
public interface ButtonAction
{
	boolean doAction(ButtonClickEvent event, Guild guild, MessageChannel channel, Message message, User author);
}
