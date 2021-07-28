package com.serpenssolida.discordbot.module.base;

import com.serpenssolida.discordbot.SerpensBot;
import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.module.BotListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class BaseListener extends BotListener
{
	public BaseListener()
	{
		super("base");
		this.setModuleName("Base");
		//this.setModulePrefix(guildID, "");
		
		//Module has no tasks and cannot get help.
		this.getBotCommands().clear();
		this.getUnlistedBotCommands().clear();
	}
	
	@Override
	public ArrayList<CommandData> generateCommands(Guild guild)
	{
		ArrayList<CommandData> commandList = new ArrayList<>();
		CommandData mainCommand = new CommandData("help" , SerpensBot.getMessage("base_command_help_description"));
		
		commandList.add(mainCommand);
		return commandList;
	}
	
	@Override
	public void onMessageReceived(@Nonnull MessageReceivedEvent event)
	{
		//Don't accept messages from private channels.
		if (!event.isFromGuild())
			return;
		
		String message = event.getMessage().getContentDisplay().replaceAll(" +", " "); //Received message.
		Guild guild = event.getGuild();
		User author = event.getAuthor(); //Author of the message.
		MessageChannel channel = event.getChannel(); //Channel where the message was sent.
		
		//If the author of the message is the bot, ignore the message.
		if (SerpensBot.api.getSelfUser().getId().equals(author.getId())) return;
		
		//Parse special commands.
		if ("!!reset symbol".equals(message))
		{
			this.resetCommandSymbol(guild, channel, author);
		}
		else if ("!!reset prefixes".equals(message))
		{
			this.resetPrefixes(guild, channel, author);
		}
	}
	
	@Override
	public void onSlashCommand(@Nonnull SlashCommandEvent event)
	{
		if (!"help".equals(event.getName()))
			return;
		
		this.sendModuleHelp(event, event.getGuild(), event.getChannel(), event.getUser());
	}
	
	/**
	 * Reset the command symbol, if something bad happens while setting command symbol this will reset it to default.
	 */
	private void resetCommandSymbol(Guild guild, MessageChannel channel, User author)
	{
		Member authorMember = guild.retrieveMember(author).complete();
		
		//Check in the user has permission to run this command.
		if (!SerpensBot.isAdmin(authorMember) && !authorMember.isOwner())
		{
			Message message = MessageUtils.buildErrorMessage(SerpensBot.getMessage("base_command_symbol_title"), author, SerpensBot.getMessage("base_command_symbol_permission_error"));
			channel.sendMessage(message).queue();
			return;
		}
		
		SerpensBot.setCommandSymbol(guild.getId(), "/");
		SerpensBot.saveSettings(guild.getId());
		
		Message message = MessageUtils.buildSimpleMessage(SerpensBot.getMessage("base_command_symbol_title"), author, SerpensBot.getMessage("base_command_symbol_info"));
		channel.sendMessage(message).queue();
	}
	
	/**
	 * Reset the command symbol, if something bad happens while setting command symbol this will reset it to default.
	 */
	private void resetPrefixes(Guild guild, MessageChannel channel, User author)
	{
		Member authorMember = guild.retrieveMember(author).complete();
		
		//Check in the user has permission to run this command.
		if (!SerpensBot.isAdmin(authorMember) && !authorMember.isOwner())
		{
			Message message = MessageUtils.buildErrorMessage(SerpensBot.getMessage("base_command_reset_prefix_title"), author, SerpensBot.getMessage("base_command_reset_prefix_permission_error"));
			channel.sendMessage(message).queue();
			return;
		}
		
		for (BotListener listener : SerpensBot.getModules())
		{
			listener.setModulePrefix(guild.getId(), listener.getInternalID());
		}
		
		SerpensBot.updateGuildCommands(guild);
		SerpensBot.saveSettings(guild.getId());
		
		Message message = MessageUtils.buildSimpleMessage(SerpensBot.getMessage("base_command_reset_prefix_title"), author, SerpensBot.getMessage("base_command_reset_prefix_info"));
		channel.sendMessage(message).queue();
	}
	
	/**
	 * Send a message containing all help commands of the modules.
	 */
	private void sendModuleHelp(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		MessageBuilder messageBuilder = new MessageBuilder();
		
		StringBuilder builderList = new StringBuilder();
		StringBuilder builderCommands = new StringBuilder();
		
		//Add footer
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed(SerpensBot.getMessage("base_command_help_title"), author);

		//Add module list to the embed.
		for (BotListener listener : SerpensBot.getModules())
		{
			/*//Don't add this listener to the list.
			if (listener instanceof BaseListener)
				continue;*/
			
			String modulePrefix = listener.getModulePrefix(guild.getId());
			
			if (modulePrefix.isBlank())
				continue;
			
			//Add listener to the list.
			builderList.append(SerpensBot.getMessage("base_command_help_command_field_value", listener.getModuleName()) + "\n");
			builderCommands.append("`" + SerpensBot.getCommandSymbol(guild.getId()) + modulePrefix + " help`\n");
		}
		
		embedBuilder.addField(SerpensBot.getMessage("base_command_help_command_field_title"), builderList.toString(), true);
		embedBuilder.addField(SerpensBot.getMessage("base_command_help_help_field_title"), builderCommands.toString(), true);
		
		messageBuilder.setEmbed(embedBuilder.build());
		event.reply(messageBuilder.build()).setEphemeral(false).queue();
	}
	
	@Override
	public void setModulePrefix(String guildID, String modulePrefix)
	{
		super.setModulePrefix(guildID, "");
	}
	
	@Override
	public String getModulePrefix(String guildID)
	{
		return "";
	}
}
