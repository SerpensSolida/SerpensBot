package com.serpenssolida.discordbot;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

/**
 * Represents a command that can be sent to the chat.
 */
public class Command
{
	private String id; //ID of the command, used to identify unequivocally a command.
	private int maxArgumentNumber; //Max number of argument of the command.
	private int minArgumentNumber; //Min number of argument of the command.
	private String help; //String that describe the command.
	private String argumentsDescription; //String that describe arguments of the command.
	private String modulePrefix; //Prefix of the module that owns this command.
	private CommandAction action; //Callback that is called when the command is sent to the chat.
	
	public Command(String id, int maxArgumentNumber)
	{
		this.id = id;
		this.maxArgumentNumber = maxArgumentNumber;
		this.minArgumentNumber = maxArgumentNumber;
		this.argumentsDescription = "";
	}
	
	/**
	 * Set the callback that is called when the command is sent to the chat.
	 *
	 * @param action
	 * 		The callback that will be called when the cmmand is sent to the chat.
	 *
	 * @return The command.
	 */
	public Command setCommandListener(CommandAction action)
	{
		this.action = action;
		return this;
	}
	
	/**
	 * Calls the callback of the command, if no collback is set the function return false.
	 *
	 * @param channel
	 * 		Channel where the message was sent.
	 * @param message
	 * 		Message containing the command.
	 * @param author
	 * 		Author of the message.
	 * @param args
	 * 		Arguments passed to the command.
	 *
	 * @return
	 */
	public boolean doAction(Guild guild, MessageChannel channel, Message message, User author, String[] args)
	{
		return (this.action != null && this.action.doAction(guild, channel, message, author, args));
	}
	
	/**
	 * @return The command id.
	 */
	public String getId()
	{
		return this.id;
	}
	
	public int getMaxArgumentNumber()
	{
		return this.maxArgumentNumber;
	}
	
	public void setMaxArgumentNumber(int maxArgumentNumber)
	{
		this.maxArgumentNumber = maxArgumentNumber;
	}
	
	public String getHelp()
	{
		return this.help;
	}
	
	public void setHelp(String help)
	{
		this.help = help;
	}
	
	public String getArgumentsDescription()
	{
		return (BotMain.commandSymbol + this.modulePrefix + " " + this.id + " " + this.argumentsDescription).strip();
	}
	
	public void setArgumentsDescription(String argumentsDescription)
	{
		this.argumentsDescription = argumentsDescription;
	}
	
	public int getMinArgumentNumber()
	{
		return this.minArgumentNumber;
	}
	
	public void setMinArgumentNumber(int minArgumentNumber)
	{
		this.minArgumentNumber = minArgumentNumber;
	}
	
	public static CommandData getCommandDataFromString(String commandPrefix, String str)
	{
		String commandID = str.substring(commandPrefix.length()).strip().split(" ")[0]; //Get the command id.
		String[] arguments = str.substring(commandPrefix.length() + commandID.length() + 1).strip().split(" "); //Get the arguments.
		
		CommandData data = new CommandData(commandID, arguments);
		
		if (arguments.length == 1 && arguments[0].isBlank())
		{
			//If there are no arguments set them to null instead of empty string.
			data.arguments = null;
		}
		
		return data;
	}
	
	public String getModulePrefix()
	{
		return this.modulePrefix;
	}
	
	public void setModulePrefix(String modulePrefix)
	{
		this.modulePrefix = modulePrefix;
	}
	
	public static class CommandData
	{
		public String commandID;
		public String[] arguments;
		
		public CommandData(String commandID, String[] arguments)
		{
			this.commandID = commandID;
			this.arguments = arguments;
		}
	}
}
