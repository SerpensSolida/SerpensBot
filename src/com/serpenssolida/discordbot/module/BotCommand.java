package com.serpenssolida.discordbot.module;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

/**
 * Represents a command that can be sent to the chat.
 */
public class BotCommand
{
	private String id; //ID of the command, used to identify unequivocally a command.
	private SubcommandData subcommand; //The subcommand used by this BotCommand.
	private BotCommandAction action; //Callback that is called when the command is sent to the chat.
	
	public BotCommand(String id, String description)
	{
		this.id = id;
		this.subcommand = new SubcommandData(this.id, description);
		this.action = (event, guild, channel, author) ->
		{
			event.reply("Ops, qualcuno si è scordato di settare una callback per questo comando!").queue();
			return true;
		};
		/*this.maxArgumentNumber = maxArgumentNumber;
		this.minArgumentNumber = maxArgumentNumber;
		this.argumentsDescription = "";*/
	}
	
	/**
	 * Set the callback that is called when the command is sent to the chat.
	 *
	 * @param action
	 * 		The callback that will be called when the cmmand is sent to the chat.
	 *
	 * @return The command.
	 */
	public BotCommand setAction(BotCommandAction action)
	{
		this.action = action;
		return this;
	}
	
	/**
	 * Calls the callback of the command, if no collback is set the function return false.
	 *
	 * @param event
	 * 		The event being performed.
	 */
	public boolean doAction(SlashCommandEvent event)
	{
		if (this.action != null)
		{
			return this.action.doAction(event, event.getGuild(), event.getChannel(), event.getUser());
		}
		
		System.err.println("Action not set for command: " + this.getId());
		
		return false;
	}
	
	public String getId()
	{
		return this.id;
	}
	
	public SubcommandData getSubcommand()
	{
		return this.subcommand;
	}
}
