package com.serpenssolida.discordbot.module.thread;

import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.contextmenu.MessageContextMenuOption;
import com.serpenssolida.discordbot.module.BotListener;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;

public class ThreadListener extends BotListener
{
	public ThreadListener()
	{
		super("thread");
		this.setModuleName("ThreadOwner");
		
		//Context menu "Pinna messaggio".
		MessageContextMenuOption option = new MessageContextMenuOption("Pinna messaggio");
		option.setAction(this::pinMessage);
		this.addMessageContextMenuOption(option);
		
		//Context menu "Spinna messaggio".
		option = new MessageContextMenuOption("Spinna messaggio");
		option.setAction(this::unpinMessage);
		this.addMessageContextMenuOption(option);
	}
	
	/**
	 * Callback for "Pin message" context menù.
	 */
	private void pinMessage(MessageContextInteractionEvent event)
	{
		Message eventMessage = event.getTarget();
		MessageChannel channel = event.getChannel();
		User author = event.getUser();
		
		//Check if the channel is null.
		if (channel == null)
		{
			Message message = MessageUtils.buildErrorMessage("Pinna messaggio", author, "Non è stato possibile trovare il canale specificato.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if the channel is a thread.
		if (!channel.getType().isThread())
		{
			Message message = MessageUtils.buildErrorMessage("Pinna messaggio", author, "Puoi usare questa opzione solo su un messaggio all'interno di un thread.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if the author of the event is the owner of the thread.
		if (!author.getId().equals(event.getThreadChannel().getOwnerId()))
		{
			Message message = MessageUtils.buildErrorMessage("Pinna messaggio", author, "Puoi usare questa opzione solo se sei il creatore del thread.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Pin the message and send confirmation.
		eventMessage.pin().complete();
		
		Message message = MessageUtils.buildSimpleMessage("Pinna messaggio", author, "Messaggio pinnato correttamente.");
		event.reply(message).setEphemeral(true).queue();
	}
	
	
	/**
	 * Callback for "Unpin message" context menù.
	 */
	private void unpinMessage(MessageContextInteractionEvent event)
	{
		Message eventMessage = event.getTarget();
		MessageChannel channel = event.getChannel();
		User author = event.getUser();
		
		//Check if the channel is null.
		if (channel == null)
		{
			Message message = MessageUtils.buildErrorMessage("Spinna messaggio", author, "Non è stato possibile trovare il canale specificato.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if the channel is a thread.
		if (!channel.getType().isThread())
		{
			Message message = MessageUtils.buildErrorMessage("Spinna messaggio", author, "Puoi usare questa opzione solo su un messaggio all'interno di un thread.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if the author of the event is the owner of the thread.
		if (!author.getId().equals(event.getThreadChannel().getOwnerId()))
		{
			Message message = MessageUtils.buildErrorMessage("Spinna messaggio", author, "Puoi usare questa opzione solo se sei il creatore del thread.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Pin the message and send confirmation.
		eventMessage.unpin().complete();
		
		Message message = MessageUtils.buildSimpleMessage("Spinna messaggio", author, "Messaggio spinnato correttamente.");
		event.reply(message).setEphemeral(true).queue();
	}
}
