package com.serpenssolida.discordbot.module;

import com.serpenssolida.discordbot.BotMain;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

/**
 * Represents a command that can be sent to the chat.
 */
public class BotCommand
{
	private String id; //ID of the command, used to identify unequivocally a command.
	private SubcommandData subcommand;
	private BotCommandAction action; //Callback that is called when the command is sent to the chat.
	
	public BotCommand(String id, String description)
	{
		this.id = id;
		this.subcommand = new SubcommandData(this.id, description);
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
	public BotCommand setCommandListener(BotCommandAction action)
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

	
	/**
	 * @return The command id.
	 */
	public String getId()
	{
		return this.id;
	}
	
	public String getArgumentsDescription(String guildID)
	{
		return (BotMain.getCommandSymbol(guildID) + " " + this.id + " ").strip();
	}
	
	public SubcommandData getSubcommand()
	{
		return subcommand;
	}
	
	public void setSubcommand(SubcommandData subcommand)
	{
		this.subcommand = subcommand;
	}
}
