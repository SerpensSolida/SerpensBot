package com.serpenssolida.discordbot.module.settings;

import com.serpenssolida.discordbot.module.BotListener;
import com.serpenssolida.discordbot.BotMain;
import com.serpenssolida.discordbot.module.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;

import java.util.regex.Pattern;

public class SettingsListener extends BotListener
{
	public SettingsListener()
	{
		super("settings");
		this.setModuleName("Settings");
		
		//Command for changing the symbol for calling a command.
		Command command = new Command("symbol", 1).setCommandListener((guild, channel, message, author, args) ->
		{
			this.setBotCommandSymbol(guild, channel, author, args);
			return true;
		});
		command.setHelp("Imposta il simbolo usato all'inizio dei comandi.");
		command.setArgumentsDescription("string");
		this.addCommand(command);
		
		//Command for changing the module prefix of a module.
		command = new Command("prefix", 2).setCommandListener((guild, channel, message, author, args) ->
		{
			this.modulePrefixCommand(guild, channel, author, args);
			return true;
		});
		command.setMinArgumentNumber(0);
		command.setHelp("Mostra la lista di moduli e i loro prefissi oppure cambia o mostra il prefisso usato da un modulo.");
		command.setArgumentsDescription("[module_name] [new_prefix]");
		this.addCommand(command);
		
		//Command for changing the module prefix of a module.
		command = new Command("deletecommand", 1).setCommandListener((guild, channel, message, author, args) ->
		{
			this.setDeleteCommandMessages(guild, channel, author, args);
			return true;
		});
		command.setHelp("Se settato a true il bot cancellera i comandi inviati in chat.");
		command.setArgumentsDescription("(true|false)");
		this.addCommand(command);
		
		//Listener does not create tasks so there is non need for cancel command.
		this.removeCommand("cancel");
	}
	
	private void setDeleteCommandMessages(Guild guild, MessageChannel channel, User author, String[] args)
	{
		String argument = args[0];
		Member authorMember = guild.retrieveMember(author).complete();
		StringBuilder builder = new StringBuilder();
		
		//Check in the user has permission to run this command.
		if (!BotMain.isAdmin(authorMember) && !authorMember.isOwner())
		{
			channel.sendMessage("> Devi essere il proprietario o moderatore del server per modificare il prefisso di un modulo.").queue();
			return;
		}
		
		switch (argument)
		{
			case "true":
				BotMain.deleteCommandMessages.put(guild.getId(), true);
				builder.append("> Cancellerò i comandi che sono stati inviati in chat.");
				break;
				
			case "false":
				BotMain.deleteCommandMessages.put(guild.getId(), false);
				builder.append("> Lascierò i comandi che sono stati inviati in chat.");
				break;
				
			default:
				builder.append("> Devi inserire (true|false) come argomento.");
		}
		
		channel.sendMessage(builder.toString()).queue();
		BotMain.saveSettings(guild.getId());
	}
	
	private void modulePrefixCommand(Guild guild, MessageChannel channel, User author, String[] args)
	{
		String moduleID; //Module id passed to the command.
		String modulePrefix; //Module prefix passed to the command.
		Member authorMember = guild.retrieveMember(author).complete(); //Member that sent the command.
		MessageBuilder messageBuilder = new MessageBuilder();
		EmbedBuilder embedBuilder = new EmbedBuilder();
		
		//Add footer
		embedBuilder.setFooter("Richiesto da " + author.getName(), author.getAvatarUrl());
		embedBuilder.setAuthor(BotMain.api.getSelfUser().getName(), "https://github.com/SerpensSolida/SerpensBot", BotMain.api.getSelfUser().getAvatarUrl());
		
		if (args == null)
		{
			//Send the list of modules and their prefixes.
			embedBuilder.setTitle("Lista dei moduli e dei loro prefissi");
			
			for (BotListener listener : BotMain.getModules())
			{
				embedBuilder.addField("Modulo " + listener.getModuleName(), String.format("ID modulo: `%s`\nPrefisso: `%s`", listener.getInternalID(), listener.getModulePrefix(guild.getId()).isBlank() ? " " : listener.getModulePrefix(guild.getId())), true);
			}
			
			messageBuilder.setEmbed(embedBuilder.build());
		}
		else if (args.length == 1)
		{
			moduleID = args[0];
			BotListener listener = BotMain.getModuleById(moduleID);
			
			if (listener != null)
			{
				messageBuilder.appendFormat("> Il prefisso del modulo `%s` è `%s`.", listener.getInternalID(), listener.getModulePrefix(guild.getId()));
			}
			else
			{
				messageBuilder.appendFormat("> Modulo con id `%s` non trovato.", moduleID);
			}
		}
		else
		{
			moduleID = args[0];
			modulePrefix = args[1];
			BotListener listener = BotMain.getModuleById(moduleID);
			
			//Check in the user has permission to run this command.
			if (!BotMain.isAdmin(authorMember) && !authorMember.isOwner())
			{
				channel.sendMessage("> Devi essere il proprietario o moderatore del server per modificare il prefisso di un modulo.").queue();
				return;
			}
			
			//Check if the module prefix to set is suitable.
			if (!modulePrefix.chars().allMatch(Character::isLetterOrDigit) || modulePrefix.length() > 16)
			{
				channel.sendMessage("> Il prefisso deve essere alpha numerico e non può superare i 16 caratteri.").queue();
				return;
			}
			
			if (listener != null)
			{
				listener.setModulePrefix(guild.getId(), modulePrefix);
				BotMain.saveSettings(guild.getId());
				
				messageBuilder.appendFormat("> Prefisso del modulo `%s` è stato impostato a `%s`", listener.getInternalID(), listener.getModulePrefix(guild.getId()));
			}
			else
			{
				messageBuilder.appendFormat("> Modulo con id `%s` non trovato.", moduleID);
			}
		}
		
		channel.sendMessage(messageBuilder.build()).queue();
	}
	
	private void setBotCommandSymbol(Guild guild, MessageChannel channel, User author, String[] args)
	{
		String symbol = args[0];
		Member authorMember = guild.retrieveMember(author).complete();

		Pattern pattern = Pattern.compile("[_*~>`@]"); //Regex containing illegal characters.

		//Check in the user has permission to run this command.
		if (!BotMain.isAdmin(authorMember) && !authorMember.isOwner())
		{
			channel.sendMessage("> Devi essere il proprietario o moderatore del server per resettare il simbolo dei comandi.").queue();
			return;
		}
		
		//Check if the command symbol is suitable
		if (symbol.length() > 6 || pattern.matcher(symbol).find())
		{
			channel.sendMessage("> Il simbolo dei comandi non può superare i 6 caratteri e non può contenere i caratteri di markdown.").queue();
			return;
		}
		
		BotMain.commandSymbol.put(guild.getId(), symbol);
		BotMain.saveSettings(guild.getId());
		
		channel.sendMessage("> Simbolo per i comandi impostato a `" + symbol + "`.").queue();
	}
	
}
