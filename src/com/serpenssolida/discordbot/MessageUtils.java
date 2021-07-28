package com.serpenssolida.discordbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;

public class MessageUtils
{
	/**
	 * Method used to generate simple embed error messages.
	 *
	 * @param title
	 * 		Title of the embed.
	 * @param author
	 * 		Author of the embed.
	 * @param description
	 * 		String showed as description of the embed.
	 *
	 * @return The message containing the generated embed.
	 */
	public static Message buildErrorMessage(String title, User author, String description)
	{
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed(title, author);
		embedBuilder.setDescription(description);
		embedBuilder.setThumbnail("https://i.imgur.com/N5QySPm.png[/img]");
		embedBuilder.setColor(Color.RED);
		MessageBuilder messageBuilder = new MessageBuilder();
		messageBuilder.setEmbed(embedBuilder.build());
		
		//		event.reply(messageBuilder.build()).setEphemeral(true).queue();
		return messageBuilder.build();
	}
	
	/**
	 * Method used to generate simple embed messages.
	 *
	 * @param title
	 * 		Title of the embed.
	 * @param author
	 * 		Author of the embed.
	 * @param description
	 * 		String showed as description of the embed.
	 *
	 * @return The message containing the generated embed.
	 */
	public static Message buildSimpleMessage(String title, User author, String description)
	{
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed(title, author);
		embedBuilder.setDescription(description);
		MessageBuilder messageBuilder = new MessageBuilder();
		messageBuilder.setEmbed(embedBuilder.build());
		
		return messageBuilder.build();
	}
	
	/**
	 * Method used to generate simple embed messages.
	 *
	 * @param title
	 * 		Title of the embed.
	 * @param author
	 * 		Author of the embed.
	 * @param description
	 * 		String showed as description of the embed.
	 * @param color Color of the embed.
	 *
	 * @return
	 * 		The message containing the generated embed.
	 */
	public static Message buildSimpleMessage(String title, User author, String description, Color color)
	{
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed(title, author);
		embedBuilder.setDescription(description);
		embedBuilder.setColor(color);
		MessageBuilder messageBuilder = new MessageBuilder();
		messageBuilder.setEmbed(embedBuilder.build());
		
		return messageBuilder.build();
	}
	
	/**
	 * Create an {@link EmbedBuilder} with standard content for ease of use.
	 *
	 * @param title
	 * 		The title to give to the embed.
	 *
	 * @return
	 * 		A {@link EmbedBuilder} with standard content.
	 */
	public static EmbedBuilder getDefaultEmbed(String title)
	{
		EmbedBuilder embedBuilder = new EmbedBuilder();
		
		//Add footer
		embedBuilder.setTitle(title);
		embedBuilder.setAuthor(SerpensBot.api.getSelfUser().getName(), "https://github.com/SerpensSolida/SerpensBot", SerpensBot.api.getSelfUser().getAvatarUrl());
		
		return embedBuilder;
	}
	
	/**
	 * Create an {@link EmbedBuilder} with standard content and author footer for ease of use.
	 *
	 * @param title
	 * 		The title to give to the embed.
	 *
	 * @return
	 * 		A {@link EmbedBuilder} with standard content.
	 */
	public static EmbedBuilder getDefaultEmbed(String title, User author)
	{
		return MessageUtils.getDefaultEmbed(title)
				.setFooter(SerpensBot.getMessage("requested", author.getName()), author.getAvatarUrl());
	}
}
