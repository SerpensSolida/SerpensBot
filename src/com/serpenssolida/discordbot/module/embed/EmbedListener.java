package com.serpenssolida.discordbot.module.embed;

import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.module.BotCommand;
import com.serpenssolida.discordbot.module.BotListener;
import com.serpenssolida.discordbot.module.poll.Poll;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.util.HashMap;

public class EmbedListener extends BotListener
{
	private HashMap<String, Poll> polls = new HashMap<>();
	
	public EmbedListener()
	{
		super("embed");
		this.setModuleName("Embed");
		
		//This module has no task.
		this.removeBotCommand("cancel");
		
		//Command for creating an embed.
		BotCommand command = new BotCommand("send", "Invia un embed nel canale corrente.");
		command.setAction((event, guild, channel, author) ->
		{
			this.sendEmbed(event, guild, channel, author);
			return true;
		});
		command.getSubcommand()
				.addOption(OptionType.STRING, "title", "Titolo dell'embed", true)
				.addOption(OptionType.STRING, "description", "Descrizione dell'embed", true)
				.addOption(OptionType.STRING, "color", "Colore dell'embed, formato esadecimale (es: FF0000 = rosso)", false)
				.addOption(OptionType.STRING, "thumbnail", "Url dell'immagine da mostra come thumbnail dell embed.", false);
		this.addBotCommand(command);
		
		//Command for creating an embed from a message.
		command = new BotCommand("generate", "Genera un embed usando il contenuto di un messaggio esistente");
		command.setAction((event, guild, channel, author) ->
		{
			this.generateEmbed(event, guild, channel, author);
			return true;
		});
		command.getSubcommand()
				.addOption(OptionType.STRING, "title", "Titolo dell'embed", true)
				.addOption(OptionType.STRING, "message-id", "Messaggio da convertire in embed", true)
				.addOption(OptionType.BOOLEAN, "delete-original", "Se cancellare il messaggio originale o no", true)
				.addOption(OptionType.STRING, "color", "Colore dell'embed, formato esadecimale (es: FF0000 = rosso)", false)
				.addOption(OptionType.STRING, "thumbnail", "Url dell'immagine da mostrare come thumbnail dell'embed.", false);
		this.addBotCommand(command);
	}
	
	private void sendEmbed(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping titleArg = event.getOption("title");
		OptionMapping descriptionArg = event.getOption("description");
		OptionMapping colorArg = event.getOption("color");
		OptionMapping thumbnailArg = event.getOption("thumbnail");
		
		//Check arugment title.
		if (titleArg == null)
		{
			Message message = MessageUtils.buildErrorMessage("Creazione embed", author, "Devi impostare un titolo.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check argument description.
		if (descriptionArg == null)
		{
			Message message = MessageUtils.buildErrorMessage("Creazione embed", author, "Devi impostare una descrizione.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Get title and description.
		String title = titleArg.getAsString();
		String description = descriptionArg.getAsString();
		
		//Create the embed.
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed(title, author);
		embedBuilder.setDescription(description);
		
		//Check if the user passed the color argument.
		if (colorArg != null)
		{
			String colorString = colorArg.getAsString();
			int colorValue = 0;
			
			//A color is only 6 character long.
			if (colorString.length() > 6)
			{
				Message message = MessageUtils.buildErrorMessage("Creazione embed", author, "Formato del colore errato.");
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			//Parse the color as int.
			try
			{
				colorValue = Integer.parseInt(colorString, 16);
			}
			catch (NumberFormatException e)
			{
				Message message = MessageUtils.buildErrorMessage("Creazione embed", author, "Formato del colore errato.");
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			//Check if the color isn't outside range.
			if (colorValue < 0 || colorValue > 0xFFFFFF)
			{
				Message message = MessageUtils.buildErrorMessage("Creazione embed", author, "Formato del colore errato.");
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			//Set the embed color.
			Color color = new Color(colorValue);
			embedBuilder.setColor(color);
		}
		
		//Check if the user passed the thumbnail argument.
		if (thumbnailArg != null)
		{
			//Set the thumbnail of the embed.
			try
			{
				embedBuilder.setThumbnail(thumbnailArg.getAsString());
			}
			catch (IllegalArgumentException e)
			{
				Message message = MessageUtils.buildErrorMessage("Creazione embed", author, "Formato del link della thumbnail errato.");
				event.reply(message).setEphemeral(true).queue();
				return;
			}
		}
		
		//Build and send the message with the embed..
		MessageBuilder messageBuilder = new MessageBuilder();
		messageBuilder.setEmbed(embedBuilder.build());
		
		event.reply(messageBuilder.build()).queue();
	}
	
	private void generateEmbed(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping titleArg = event.getOption("title");
		OptionMapping messageIdArg = event.getOption("message-id");
		OptionMapping colorArg = event.getOption("color");
		OptionMapping thumbnailArg = event.getOption("thumbnail");
		OptionMapping deleteOriginalArg = event.getOption("delete-original");
		
		//Check arugment title.
		if (titleArg == null)
		{
			Message message = MessageUtils.buildErrorMessage("Creazione embed", author, "Devi impostare un titolo.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check argument description.
		if (messageIdArg == null)
		{
			Message message = MessageUtils.buildErrorMessage("Creazione embed", author, "Devi impostare un id.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Retrieive message to convert it to embed.
		String messageId = messageIdArg.getAsString();
		Message originalMessage;
		
		try
		{
			originalMessage = channel.retrieveMessageById(messageId).complete();
		}
		catch (ErrorResponseException ignored)
		{
			originalMessage = null;
		}
		
		//Check if the message was found.
		if (originalMessage == null)
		{
			Message message = MessageUtils.buildErrorMessage("Creazione embed", author, "Nessun messaggio trovato con id:" + messageIdArg.getAsString());
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Get title and description.
		String title = titleArg.getAsString();
		String description = originalMessage.getContentDisplay();
		
		//Check description content.
		if (description.isBlank())
		{
			Message message = MessageUtils.buildErrorMessage("Creazione embed", author, "Il messaggio specificato non continene del testo.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Create the embed.
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed(title, author);
		embedBuilder.setDescription(description);
		
		//Check if the user passed the color argument.
		if (colorArg != null)
		{
			String colorString = colorArg.getAsString();
			int colorValue;
			
			//A color is only 6 character long.
			if (colorString.length() > 6)
			{
				Message message = MessageUtils.buildErrorMessage("Creazione embed", author, "Formato del colore errato.1");
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			//Parse the color as int.
			try
			{
				colorValue = Integer.parseInt(colorString, 16);
			}
			catch (NumberFormatException e)
			{
				Message message = MessageUtils.buildErrorMessage("Creazione embed", author, "Formato del colore errato.2");
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			//Check if the color isn't outside range.
			if (colorValue < 0 || colorValue > 0xFFFFFF)
			{
				Message message = MessageUtils.buildErrorMessage("Creazione embed", author, "Formato del colore errato.3");
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			//Set the embed color.
			Color color = new Color(colorValue);
			embedBuilder.setColor(color);
		}
		
		//Check if the user passed the thumbnail argument.
		if (thumbnailArg != null)
		{
			//Set the thumbnail of the embed.
			try
			{
				embedBuilder.setThumbnail(thumbnailArg.getAsString());
			}
			catch (IllegalArgumentException e)
			{
				Message message = MessageUtils.buildErrorMessage("Creazione embed", author, "Formato del link della thumbnail errato.");
				event.reply(message).setEphemeral(true).queue();
				return;
			}
		}
		
		//Build the message with the embed.
		MessageBuilder messageBuilder = new MessageBuilder();
		messageBuilder.setEmbed(embedBuilder.build());
		
		//Delete the original message if specified.
		if (deleteOriginalArg != null && deleteOriginalArg.getAsBoolean())
			originalMessage.delete().queue();
		
		//Send the message with the embed.
		event.reply(messageBuilder.build()).queue();
	}
}
