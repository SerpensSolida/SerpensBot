package com.serpenssolida.discordbot.module.owner;

import com.serpenssolida.discordbot.SerpensBot;
import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.module.BotListener;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

import javax.annotation.Nonnull;

public class OwnerListener extends BotListener
{
	public OwnerListener()
	{
		super("owner");
		this.setModuleName("Owner");
		
		//Module has no tasks and cannot get help.
		this.getBotCommands().clear();
		this.getUnlistedBotCommands().clear();
	}
	
	@Override
	public void onPrivateMessageReceived(@Nonnull PrivateMessageReceivedEvent event)
	{
		PrivateChannel channel = event.getChannel();
		User user = event.getAuthor();
		String message = event.getMessage().getContentDisplay();
		
		//If the author of the message is the bot ignore it.
		if (SerpensBot.getApi().getSelfUser().equals(user))
			return;
		
		//Check if the author of the message is the owner of the bot.
		if (!SerpensBot.getOwnerId().equals(user.getId()))
		{
			channel.sendMessage(MessageUtils.buildErrorMessage("Mi dispiace!", user, "Rispondo solo al mio creatore.")).queue();
			return;
		}
		
		if (!message.startsWith("!"))
			return;
		
		//Parse arguments
		message = message.substring(1);
		String[] content = message.split("::");
		String channelId;
		String title;
		String text;
		
		try
		{
			channelId = content[0];
			title = content[1];
			text = content[2];
		}
		catch (IndexOutOfBoundsException ignored)
		{
			channel.sendMessage(MessageUtils.buildErrorMessage("Invio messaggio al canale", user, "Errore nella lettura dei parametri.")).queue();
			return;
		}
		
		MessageChannel messageChannel = SerpensBot.getApi().getTextChannelById(channelId);
		
		//Check if the channel exists.
		if (messageChannel == null)
		{
			channel.sendMessage(MessageUtils.buildErrorMessage("Invio messaggio al canale", user, "Non è stato trovato nessun canale con id: " + channelId)).queue();
			return;
		}
		
		messageChannel.sendMessage(MessageUtils.buildSimpleMessage(title, user, text)).queue();
	}
	
	@Override
	public String getModulePrefix(String guildID)
	{
		return "";
	}
}
