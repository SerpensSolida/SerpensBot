package com.serpenssolida.discordbot.module.settings;

import com.serpenssolida.discordbot.SerpensBot;
import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.module.BotCommand;
import com.serpenssolida.discordbot.module.BotListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.regex.Pattern;

public class SettingsListener extends BotListener
{
	public SettingsListener()
	{
		super("settings");
		this.setModuleName("Settings");
		
		//Command for changing the symbol for calling a command.
		BotCommand command = new BotCommand("symbol", SerpensBot.getMessage("settings_command_symbol_desc"));
		command.setAction((event, guild, channel, author) ->
		{
			this.setUnlistedBotCommandSymbol(event, guild, channel, author);
			return true;
		});
		command.getSubcommand()
				.addOption(OptionType.STRING, "value", "Il nuovo simbolo da impostare per i comandi non listati.", true);
		this.addBotCommand(command);
		
		//Command for changing the module prefix of a module.
		command = new BotCommand("prefix", SerpensBot.getMessage("settings_command_prefix_desc"));
		command.setAction((event, guild, channel, author) ->
		{
			this.modulePrefixCommand(event, guild, channel, author);
			return true;
		});
		command.getSubcommand()
				.addOptions(new OptionData(OptionType.STRING, "module_name", SerpensBot.getMessage("settings_command_prefix_param1"), false).setRequired(false))
				.addOption(OptionType.STRING, "new_prefix", SerpensBot.getMessage("settings_command_prefix_param2"), false);
		this.addBotCommand(command);
		
		//Command for changing the module prefix of a module.
		command = new BotCommand("deletecommand", SerpensBot.getMessage("settings_command_deletecommand_desc"));
		command.setAction((event, guild, channel, author) ->
		{
			this.setDeleteCommandMessages(event, guild, channel, author);
			return true;
		});
		command.getSubcommand()
				.addOption(OptionType.BOOLEAN, "value", SerpensBot.getMessage("settings_command_deletecommand_param1"), true);
		this.addBotCommand(command);
		
		//This listener does not create any tasks so there is non need for a "cancel" command.
		this.removeBotCommand("cancel");
	}
	
	private void setDeleteCommandMessages(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		Member authorMember = guild.retrieveMember(author).complete();
		
		//Argument parsing.
		OptionMapping valueArg = event.getOption("value");
		
		//Check in the user has permission to run this command.
		if (!SerpensBot.isAdmin(authorMember) && !authorMember.isOwner())
		{
			Message message = MessageUtils.buildErrorMessage(SerpensBot.getMessage("settings_command_deletecommand_title"), author, SerpensBot.getMessage("settings_permission_error"));
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		if (valueArg == null)
		{
			Message message = MessageUtils.buildErrorMessage(SerpensBot.getMessage("settings_command_deletecommand_title"), author, SerpensBot.getMessage("settings_command_deletecommand_missing_value_error"));
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Update option.
		boolean value = valueArg.getAsBoolean();
		SerpensBot.setDeleteCommandMessages(guild.getId(), value);
		SerpensBot.saveSettings(guild.getId());
		
		String description = (value ? SerpensBot.getMessage("settings_command_deletecommand_delete_info") : SerpensBot.getMessage("settings_command_deletecommand_leave_info"));
		Message message = MessageUtils.buildSimpleMessage(SerpensBot.getMessage("settings_command_deletecommand_title"), author,  description);
		event.reply(message).setEphemeral(false).queue(); //Set ephemeral if the user didn't put the argument.
	}
	
	private void modulePrefixCommand(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping moduleID = event.getOption("module_name"); //Module id passed to the command.
		OptionMapping modulePrefix = event.getOption("new_prefix"); //Module prefix passed to the command.
		
		Member authorMember = guild.retrieveMember(author).complete(); //Member that sent the command.
		MessageBuilder messageBuilder = new MessageBuilder();
		
		String embedTitle = SerpensBot.getMessage("settings_command_prefix_title");
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed(embedTitle, author);
		
		int argumentCount = event.getOptions().size();
		
		if (argumentCount == 0)
		{
			//Send the list of modules and their prefixes.
			embedBuilder.setTitle(SerpensBot.getMessage("settings_command_prefix_list_title"));
			
			for (BotListener listener : SerpensBot.getModules())
			{
				String fieldTitle = SerpensBot.getMessage("settings_command_prefix_list_field_title", listener.getModuleName());
				String fieldValue = SerpensBot.getMessage("settings_command_prefix_list_field_value", listener.getInternalID(), listener.getModulePrefix(guild.getId()).isBlank() ? " " : listener.getModulePrefix(guild.getId()));
				embedBuilder.addField(fieldTitle, fieldValue, true);
			}
			
			messageBuilder.setEmbed(embedBuilder.build());
		}
		else if (argumentCount == 1 && moduleID != null)
		{
			//Get the listener by the id passed as parameter.
			BotListener listener = SerpensBot.getModuleById(moduleID.getAsString());
			
			//Print the result.
			if (listener == null)
			{
				Message message = MessageUtils.buildErrorMessage(embedTitle, author, SerpensBot.getMessage("settings_module_not_found_error", moduleID));
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			embedBuilder.appendDescription(SerpensBot.getMessage("settings_command_prefix_prefix_info", listener.getModuleName(), listener.getModulePrefix(guild.getId())));
		}
		else if (argumentCount == 2 && moduleID != null && modulePrefix != null)
		{
			//Get the listener by the id passed as parameter.
			BotListener listener = SerpensBot.getModuleById(moduleID.getAsString());
			String newPrefix = modulePrefix.getAsString();
			
			//Check in the user has permission to run this command.
			if (!SerpensBot.isAdmin(authorMember) && !authorMember.isOwner())
			{
				Message message = MessageUtils.buildErrorMessage(embedTitle, author, SerpensBot.getMessage("settings_permission_error"));
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			//Check if the module prefix to set is suitable.
			if (!newPrefix.chars().allMatch(Character::isLetterOrDigit) || newPrefix.length() > 16)
			{
				Message message = MessageUtils.buildErrorMessage(embedTitle, author, SerpensBot.getMessage("settings_command_prefix_edit_format_error"));
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			if (listener == null)
			{
				Message message = MessageUtils.buildErrorMessage(embedTitle, author, SerpensBot.getMessage("settings_module_not_found_error", moduleID));
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			//Set the new prefix to the module.
			listener.setModulePrefix(guild.getId(), newPrefix);
			SerpensBot.updateGuildCommands(guild);
			SerpensBot.saveSettings(guild.getId());
			
			embedBuilder.setDescription(SerpensBot.getMessage("settings_command_prefix_set_info", listener.getInternalID(), listener.getModulePrefix(guild.getId())));
		}
		else
		{
			Message message = MessageUtils.buildErrorMessage(embedTitle, author, SerpensBot.getMessage("settings_command_prefix_missing_argument_error"));
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		messageBuilder.setEmbed(embedBuilder.build());
		event.reply(messageBuilder.build()).setEphemeral(false).queue();
	}
	
	private void setUnlistedBotCommandSymbol(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed(SerpensBot.getMessage("settings_command_symbol_title"), author);
		MessageBuilder messageBuilder = new MessageBuilder();
		OptionMapping newSymbolOption = event.getOption("value");
		Member authorMember = guild.retrieveMember(author).complete();

		Pattern pattern = Pattern.compile("[_*~>`@]"); //Regex containing illegal characters.

		//Check in the user has permission to run this command.
		if (!SerpensBot.isAdmin(authorMember) && !authorMember.isOwner())
		{
			embedBuilder.appendDescription(SerpensBot.getMessage("settings_permission_error"));
			messageBuilder.setEmbed(embedBuilder.build());
			
			event.reply(messageBuilder.build()).setEphemeral(true).queue();
			return;
		}
		
		if (newSymbolOption == null)
		{
			//Note: This code should be unreachable.
			embedBuilder.appendDescription(SerpensBot.getMessage("settings_command_symbol_missing_argument_error"));
			messageBuilder.setEmbed(embedBuilder.build());
			
			event.reply(messageBuilder.build()).setEphemeral(true).queue();
			return;
		}
		
		//Check if the command symbol is suitable
		String newSymbol = newSymbolOption.getAsString();
		if (newSymbol.length() > 6 || pattern.matcher(newSymbol).find())
		{
			embedBuilder.appendDescription(SerpensBot.getMessage("settings_command_symbol_format_error"));
			messageBuilder.setEmbed(embedBuilder.build());
			
			event.reply(messageBuilder.build()).setEphemeral(true).queue();
			return;
		}
		
		SerpensBot.setCommandSymbol(guild.getId(), newSymbol);
		SerpensBot.saveSettings(guild.getId());
		
		embedBuilder.appendDescription(SerpensBot.getMessage("settings_command_symbol_set_info", newSymbol));
		messageBuilder.setEmbed(embedBuilder.build());
		
		event.reply(messageBuilder.build()).setEphemeral(false).queue();
	}
	
}
