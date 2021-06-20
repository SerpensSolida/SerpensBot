package com.serpenssolida.discordbot.module.settings;

import com.serpenssolida.discordbot.BotMain;
import com.serpenssolida.discordbot.module.BotCommand;
import com.serpenssolida.discordbot.module.BotListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
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
		StringBuilder builder = new StringBuilder();
		
		//Check in the user has permission to run this command.
		if (!BotMain.isAdmin(authorMember) && !authorMember.isOwner())
		{
			channel.sendMessage("> Devi essere il proprietario o moderatore del server per modificare il prefisso di un modulo.").queue();
			return;
		}
		
		//Argument parsing.
		OptionMapping argument = event.getOption("value");
		if (argument != null)
		{
			boolean value = argument.getAsBoolean();
			BotMain.setDeleteCommandMessages(guild.getId(), value);
			BotMain.saveSettings(guild.getId());
			builder.append(">" + (value ? "Cancellerò" : "Lascerò") + " i comandi che sono stati inviati in chat.");
		}
		else
		{
			builder.append("> Devi inserire (true|false) come argomento.");
		}
		
		event.reply(builder.toString()).setEphemeral(argument == null).queue(); //Set ephemeral if the user didn't put the argument.
	}
	
	private void modulePrefixCommand(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping moduleID = event.getOption("module_name"); //Module id passed to the command.
		OptionMapping modulePrefix = event.getOption("new_prefix"); //Module prefix passed to the command.
		Member authorMember = guild.retrieveMember(author).complete(); //Member that sent the command.
		MessageBuilder messageBuilder = new MessageBuilder();
		EmbedBuilder embedBuilder = new EmbedBuilder();
		
		//Add footer
		embedBuilder.setFooter("Richiesto da " + author.getName(), author.getAvatarUrl());
		embedBuilder.setAuthor(BotMain.api.getSelfUser().getName(), "https://github.com/SerpensSolida/SerpensBot", BotMain.api.getSelfUser().getAvatarUrl());
		
		int argumentCount = event.getOptions().size();
		
		if (argumentCount == 0)
		{
			//Send the list of modules and their prefixes.
			embedBuilder.setTitle("Lista dei moduli e dei loro prefissi");
			
			for (BotListener listener : BotMain.getModules())
			{
				embedBuilder.addField("Modulo " + listener.getModuleName(), String.format("ID modulo: `%s`\nPrefisso: `%s`", listener.getInternalID(), listener.getModulePrefix(guild.getId()).isBlank() ? " " : listener.getModulePrefix(guild.getId())), true);
			}
			
			messageBuilder.setEmbed(embedBuilder.build());
		}
		else if (argumentCount == 1 && moduleID != null)
		{
			//Get the listener by the id passed as parameter.
			BotListener listener = BotMain.getModuleById(moduleID.getAsString());
			
			//Print the result.
			if (listener != null)
			{
				messageBuilder.appendFormat("> Il prefisso del modulo `%s` è `%s`.", listener.getInternalID(), listener.getModulePrefix(guild.getId()));
			}
			else
			{
				messageBuilder.appendFormat("> Modulo con id `%s` non trovato.", moduleID);
			}
		}
		else if (argumentCount == 2 && moduleID != null && modulePrefix != null)
		{
			//Get the listener by the id passed as parameter.
			BotListener listener = BotMain.getModuleById(moduleID.getAsString());
			String newPrefix = modulePrefix.getAsString();
			
			//Check in the user has permission to run this command.
			if (!BotMain.isAdmin(authorMember) && !authorMember.isOwner())
			{
				event.reply("> Devi essere il proprietario o moderatore del server per modificare il prefisso di un modulo.").queue();
				return;
			}
			
			//Check if the module prefix to set is suitable.
			if (!newPrefix.chars().allMatch(Character::isLetterOrDigit) || newPrefix.length() > 16)
			{
				event.reply("> Il prefisso deve essere alpha numerico e non può superare i 16 caratteri.").queue();
				return;
			}
			
			if (listener != null)
			{
				//Set the new prefix to the module.
				listener.setModulePrefix(guild.getId(), newPrefix);
				BotMain.updateGuildCommands(guild);
				BotMain.saveSettings(guild.getId());
				
				messageBuilder.appendFormat("> Prefisso del modulo `%s` è stato impostato a `%s`", listener.getInternalID(), listener.getModulePrefix(guild.getId()));
			}
			else
			{
				messageBuilder.appendFormat("> Modulo con id `%s` non trovato.", moduleID);
			}
		}
		
		event.reply(messageBuilder.build()).setEphemeral(false).queue();
	}
	
	private void setUnlistedBotCommandSymbol(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping newSymbolOption = event.getOption("value");
		Member authorMember = guild.retrieveMember(author).complete();

		Pattern pattern = Pattern.compile("[_*~>`@]"); //Regex containing illegal characters.

		//Check in the user has permission to run this command.
		if (!BotMain.isAdmin(authorMember) && !authorMember.isOwner())
		{
			event.reply("> Devi essere il proprietario o moderatore del server per resettare il simbolo dei comandi.").queue();
			return;
		}
		
		if (newSymbolOption == null)
		{
			event.reply("> Argomento non fornito.").queue(); //Note: This code should be unreachable.
			return;
		}
		
		//Check if the command symbol is suitable
		String newSymbol = newSymbolOption.getAsString();
		if (newSymbol.length() > 6 || pattern.matcher(newSymbol).find())
		{
			event.reply("> Il simbolo dei comandi non può superare i 6 caratteri e non può contenere i caratteri di markdown.").queue();
			return;
		}
		
		BotMain.setCommandSymbol(guild.getId(), newSymbol);
		BotMain.saveSettings(guild.getId());
		
		event.reply("> Simbolo per i comandi impostato a `" + newSymbol + "`.").setEphemeral(false).queue();
	}
	
}
