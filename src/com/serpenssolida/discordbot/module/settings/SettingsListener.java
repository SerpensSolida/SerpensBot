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
		BotCommand command = new BotCommand("symbol", "Imposta il simbolo usato all'inizio dei comandi.");
		command.setAction((event, guild, channel, author) ->
		{
			this.setUnlistedBotCommandSymbol(event, guild, channel, author);
			return true;
		});
		command.getSubcommand()
				.addOption(OptionType.STRING, "value", "Il nuovo simbolo da impostare per i comandi non listati.", true);
		this.addBotCommand(command);
		
		//Command for changing the module prefix of a module.
		command = new BotCommand("prefix", "Mostra la lista di moduli e i loro prefissi oppure cambia o mostra il prefisso usato da un modulo.");
		command.setAction((event, guild, channel, author) ->
		{
			this.modulePrefixCommand(event, guild, channel, author);
			return true;
		});
		command.getSubcommand()
				.addOptions(new OptionData(OptionType.STRING, "module_name", "Il nome del modulo di cui mostrare/impostare il prefisso.", false).setRequired(false))
				.addOption(OptionType.STRING, "new_prefix", "Il nuovo prefisso da impostare al modulo.", false);
		this.addBotCommand(command);
		
		//Command for changing the module prefix of a module.
		command = new BotCommand("deletecommand", "Imposta se il bot cancellerà i comandi non listati inviati in chat oppure no.");
		command.setAction((event, guild, channel, author) ->
		{
			this.setDeleteCommandMessages(event, guild, channel, author);
			return true;
		});
		command.getSubcommand()
				.addOption(OptionType.BOOLEAN, "value", "Se impostato a true il bot cancellerà i comandi non listati inviati in chat.", true);
		this.addBotCommand(command);
		
		//This listener does not create any tasks so there is non need for a "cancel" command.
		this.removeBotCommand("cancel");
	}
	
	private void setDeleteCommandMessages(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		Member authorMember = guild.retrieveMember(author).complete();
		
		//Argument parsing.
		OptionMapping argument = event.getOption("value");
		
		//Check in the user has permission to run this command.
		if (!SerpensBot.isAdmin(authorMember) && !authorMember.isOwner())
		{
			Message message = MessageUtils.buildErrorMessage("Cancellazione messaggi", author, "Devi essere il proprietario o moderatore del server per modificare questa impostazione.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		if (argument == null)
		{
			Message message = MessageUtils.buildErrorMessage("Cancellazione messaggi", author, "Devi inserire l'argomento.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Update option.
		boolean value = argument.getAsBoolean();
		SerpensBot.setDeleteCommandMessages(guild.getId(), value);
		SerpensBot.saveSettings(guild.getId());
		
		Message message = MessageUtils.buildSimpleMessage("Cancellazione dei messaggi", author, (value ? "Cancellerò" : "Lascerò") + " i comandi che sono stati inviati in chat.");
		event.reply(message).setEphemeral(false).queue(); //Set ephemeral if the user didn't put the argument.
	}
	
	private void modulePrefixCommand(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping moduleID = event.getOption("module_name"); //Module id passed to the command.
		OptionMapping modulePrefix = event.getOption("new_prefix"); //Module prefix passed to the command.
		Member authorMember = guild.retrieveMember(author).complete(); //Member that sent the command.
		MessageBuilder messageBuilder = new MessageBuilder();
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Prefissi dei comandi", author);
		
		int argumentCount = event.getOptions().size();
		
		if (argumentCount == 0)
		{
			//Send the list of modules and their prefixes.
			embedBuilder.setTitle("Lista dei moduli e dei loro prefissi");
			
			for (BotListener listener : SerpensBot.getModules())
			{
				embedBuilder.addField("Modulo " + listener.getModuleName(), String.format("ID modulo: `%s`\nPrefisso: `%s`", listener.getInternalID(), listener.getModulePrefix(guild.getId()).isBlank() ? " " : listener.getModulePrefix(guild.getId())), true);
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
				Message message = MessageUtils.buildErrorMessage("Prefissi dei comandi", author, "Modulo con id `" + moduleID + "` non trovato.");
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			embedBuilder.appendDescription("Il prefisso del modulo `" + listener.getInternalID() + "` è `" + listener.getModulePrefix(guild.getId()) + "`.");
		}
		else if (argumentCount == 2 && moduleID != null && modulePrefix != null)
		{
			//Get the listener by the id passed as parameter.
			BotListener listener = SerpensBot.getModuleById(moduleID.getAsString());
			String newPrefix = modulePrefix.getAsString();
			
			//Check in the user has permission to run this command.
			if (!SerpensBot.isAdmin(authorMember) && !authorMember.isOwner())
			{
				Message message = MessageUtils.buildErrorMessage("Prefissi dei comandi", author, "Devi essere il proprietario o moderatore del server per modificare il prefisso di un modulo.");
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			//Check if the module prefix to set is suitable.
			if (!newPrefix.chars().allMatch(Character::isLetterOrDigit) || newPrefix.length() > 16)
			{
				Message message = MessageUtils.buildErrorMessage("Prefissi dei comandi", author, "Il prefisso deve essere alfanumerico e non può superare i 16 caratteri.");
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			if (listener == null)
			{
				Message message = MessageUtils.buildErrorMessage("Prefissi dei comandi", author, "Modulo con id `" + moduleID + "` non trovato.");
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			//Set the new prefix to the module.
			listener.setModulePrefix(guild.getId(), newPrefix);
			SerpensBot.updateGuildCommands(guild);
			SerpensBot.saveSettings(guild.getId());
			
			embedBuilder.setDescription("Prefisso del modulo `" + listener.getInternalID() + "` è stato impostato a `" + listener.getModulePrefix(guild.getId()) + "`.");
		}
		else
		{
			Message message = MessageUtils.buildErrorMessage("Prefissi dei comandi", author, "Devi inserire tutti gli argomenti per modificare il nome di un prefisso.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		messageBuilder.setEmbed(embedBuilder.build());
		event.reply(messageBuilder.build()).setEphemeral(false).queue();
	}
	
	private void setUnlistedBotCommandSymbol(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Simbolo comandi non listati", author);
		MessageBuilder messageBuilder = new MessageBuilder();
		OptionMapping newSymbolOption = event.getOption("value");
		Member authorMember = guild.retrieveMember(author).complete();

		Pattern pattern = Pattern.compile("[_*~>`@]"); //Regex containing illegal characters.

		//Check in the user has permission to run this command.
		if (!SerpensBot.isAdmin(authorMember) && !authorMember.isOwner())
		{
			embedBuilder.appendDescription("Devi essere il proprietario o moderatore del server per resettare il simbolo dei comandi.");
			messageBuilder.setEmbed(embedBuilder.build());
			
			event.reply(messageBuilder.build()).setEphemeral(true).queue();
			return;
		}
		
		if (newSymbolOption == null)
		{
			//Note: This code should be unreachable.
			embedBuilder.appendDescription("Argomento non fornito.");
			messageBuilder.setEmbed(embedBuilder.build());
			
			event.reply(messageBuilder.build()).setEphemeral(true).queue();
			return;
		}
		
		//Check if the command symbol is suitable
		String newSymbol = newSymbolOption.getAsString();
		if (newSymbol.length() > 6 || pattern.matcher(newSymbol).find())
		{
			embedBuilder.appendDescription("Il simbolo dei comandi non può superare i 6 caratteri e non può contenere i caratteri di markdown.");
			messageBuilder.setEmbed(embedBuilder.build());
			
			event.reply(messageBuilder.build()).setEphemeral(true).queue();
			return;
		}
		
		SerpensBot.setCommandSymbol(guild.getId(), newSymbol);
		SerpensBot.saveSettings(guild.getId());
		
		embedBuilder.appendDescription("Simbolo per i comandi impostato a `" + newSymbol + "`.");
		messageBuilder.setEmbed(embedBuilder.build());
		
		event.reply(messageBuilder.build()).setEphemeral(false).queue();
	}
	
}
