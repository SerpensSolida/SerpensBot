package com.serpenssolida.discordbot.module.settings;

import com.serpenssolida.discordbot.module.BotListener;
import com.serpenssolida.discordbot.BotMain;
import com.serpenssolida.discordbot.module.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

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
		
		if (!this.isAdmin(authorMember) && !authorMember.isOwner())
		{
			channel.sendMessage("> Devi essere il proprietario o moderatore del server per modificare il prefisso di un modulo.").queue();
			return;
		}
		
		switch (argument)
		{
			case "true":
				BotMain.deleteCommandMessages = true;
				builder.append("> Cancellerò i comandi che sono stati inviati in chat.");
				break;
				
			case "false":
				BotMain.deleteCommandMessages = false;
				builder.append("> Lascierò i comandi che sono stati inviati in chat.");
				break;
				
			default:
				builder.append("> Devi inserire (true|false) come argomento.");
		}
		
		channel.sendMessage(builder.toString()).queue();
		BotMain.saveSettings();
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
			
			for (Object registeredListener : BotMain.api.getEventManager().getRegisteredListeners())
			{
				if (registeredListener instanceof BotListener)
				{
					BotListener listener = (BotListener) registeredListener;
					
					embedBuilder.addField("Modulo " + listener.getModuleName(), String.format("ID modulo: `%s`\nPrefisso: `%s`", listener.getInternalID(), listener.getModulePrefix()), true);
				}
			}
			
			messageBuilder.setEmbed(embedBuilder.build());
		}
		else if (args.length == 1)
		{
			moduleID = args[0];
			BotListener listener = this.getListenerById(moduleID);
			
			if (listener != null)
			{
				messageBuilder.appendFormat("> Il prefisso del modulo `%s` è `%s`.", listener.getInternalID(), listener.getModulePrefix());
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
			BotListener listener = this.getListenerById(moduleID);
			
			if (!this.isAdmin(authorMember) && !authorMember.isOwner())
			{
				channel.sendMessage("> Devi essere il proprietario o moderatore del server per modificare il prefisso di un modulo.").queue();
				return;
			}
			
			if (listener != null)
			{
				listener.setModulePrefix(modulePrefix);
				BotMain.saveSettings();
				
				messageBuilder.appendFormat("> Prefisso del modulo `%s` è stato impostato a `%s`", listener.getInternalID(), listener.getModulePrefix());
			}
			else
			{
				messageBuilder.appendFormat("> Modulo con id `%s` non trovato.", moduleID);
			}
		}
		
		channel.sendMessage(messageBuilder.build()).queue();
	}
	
	private BotListener getListenerById(String moduleID)
	{
		//Get the module with the correct id and send its module prefix.
		for (Object registeredListener : BotMain.api.getEventManager().getRegisteredListeners())
		{
			if (registeredListener instanceof BotListener)
			{
				BotListener listener = (BotListener) registeredListener;
				
				if (listener.getInternalID().equals(moduleID))
				{
					return listener;
				}
			}
		}
		
		return null;
	}
	
	private void setBotCommandSymbol(Guild guild, MessageChannel channel, User author, String[] args)
	{
		String symbol = args[0];
		Member authorMember = guild.retrieveMember(author).complete();
		
		if (!this.isAdmin(authorMember) && !authorMember.isOwner())
		{
			channel.sendMessage("> Devi essere il proprietario o moderatore del server per modificare il prefisso di un modulo.").queue();
			return;
		}
		
		BotMain.commandSymbol = symbol;
		BotMain.saveSettings();
	}
	
	private boolean isAdmin(Member member)
	{
		if (member == null) return false;
		
		for (Role role : member.getRoles())
		{
			if (role.hasPermission(Permission.MANAGE_SERVER))
			{
				return true;
			}
		}
		
		return false;
	}
	
}
