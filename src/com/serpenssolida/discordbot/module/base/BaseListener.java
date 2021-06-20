package com.serpenssolida.discordbot.module.base;

import com.serpenssolida.discordbot.BotMain;
import com.serpenssolida.discordbot.module.BotListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
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
	}
	
	@Override
	public ArrayList<CommandData> generateCommands(Guild guild)
	{
		ArrayList<CommandData> commandList = new ArrayList<>();
		CommandData mainCommand = new CommandData("help" , "Mostra la lista di moduli disponibili.");
		
		commandList.add(mainCommand);
		return commandList;
	}
	
	@Override
	public void onMessageReceived(@Nonnull MessageReceivedEvent event)
	{
		String message = event.getMessage().getContentDisplay().replaceAll(" +", " "); //Received message.
		Guild guild = event.getGuild();
		User author = event.getAuthor(); //Author of the message.
		MessageChannel channel = event.getChannel(); //Channel where the message was sent.
		
		//If the author of the message is the bot, ignore the message.
		if (BotMain.api.getSelfUser().getId().equals(author.getId())) return;
		
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
		if (!BotMain.isAdmin(authorMember) && !authorMember.isOwner())
		{
			channel.sendMessage("> Devi essere il proprietario o moderatore del server per resettare il simbolo dei comandi.").queue();
			return;
		}
		
		BotMain.setCommandSymbol(guild.getId(), "/");
		BotMain.saveSettings(guild.getId());
		
		channel.sendMessage("> Simbolo per i comandi impostato a `/`.").queue();
	}
	
	/**
	 * Reset the command symbol, if something bad happens while setting command symbol this will reset it to default.
	 */
	private void resetPrefixes(Guild guild, MessageChannel channel, User author)
	{
		Member authorMember = guild.retrieveMember(author).complete();
		
		//Check in the user has permission to run this command.
		if (!BotMain.isAdmin(authorMember) && !authorMember.isOwner())
		{
			channel.sendMessage("> Devi essere il proprietario o moderatore del server per resettare i prefissi.").queue();
			return;
		}
		
		for (BotListener listener : BotMain.getModules())
		{
			listener.setModulePrefix(guild.getId(), listener.getInternalID());
		}
		
		BotMain.updateGuildCommands(guild);
		BotMain.saveSettings(guild.getId());
		
		channel.sendMessage("> Prefisso dei moduli resettato correttamente.").queue();
	}
	
	/**
	 * Send a message containing all help commands of the modules.
	 */
	private void sendModuleHelp(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		MessageBuilder messageBuilder = new MessageBuilder();
		EmbedBuilder embedBuilder = new EmbedBuilder();
		
		StringBuilder builderList = new StringBuilder();
		StringBuilder builderCommands = new StringBuilder();
		
		//Add footer
		embedBuilder.setTitle("Lista moduli disponibili");
		embedBuilder.setFooter("Richiesto da " + author.getName(), author.getAvatarUrl());
		embedBuilder.setAuthor(BotMain.api.getSelfUser().getName(), "https://github.com/SerpensSolida/SerpensBot", BotMain.api.getSelfUser().getAvatarUrl());
		
		//Add module list to the embed.
		for (BotListener listener : BotMain.getModules())
		{
			//Don't add this listener to the list.
			if (listener instanceof BaseListener) continue;
			
			//Add listener to the list.
			builderList.append("Modulo " + listener.getModuleName() + "\n");
			builderCommands.append("`" + BotMain.getCommandSymbol(guild.getId()) + listener.getModulePrefix(guild.getId()) + " help`\n");
		}
		
		embedBuilder.addField("Moduli", builderList.toString(), true);
		embedBuilder.addField("Comando help", builderCommands.toString(), true);
		
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
