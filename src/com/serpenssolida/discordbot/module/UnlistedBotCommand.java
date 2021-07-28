package com.serpenssolida.discordbot.module;

import com.serpenssolida.discordbot.SerpensBot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.HashMap;

/**
 * Represents a command that can be sent to the chat.
 */
public class UnlistedBotCommand
{
	private String id; //ID of the command, used to identify unequivocally a command.
	private int maxArgumentNumber; //Max number of argument of the command.
	private int minArgumentNumber; //Min number of argument of the command.
	private boolean joinArguments; //Whether or not arguments are joined together before checking them.
	private String help; //String that describe the command.
	private String argumentsDescription; //String that describe arguments of the command.
	private HashMap<String, String> modulePrefix = new HashMap<>(); //Prefix of the module that owns this command.
	private UnlistedBotCommandAction action; //Callback that is called when the command is sent to the chat.
	private String defaultModulePrefix; //Default module prefix.
	
	public UnlistedBotCommand(String id, int maxArgumentNumber)
	{
		this.id = id;
		this.maxArgumentNumber = maxArgumentNumber;
		this.minArgumentNumber = maxArgumentNumber;
		this.argumentsDescription = "";
		this.action = (guild, channel, message, author, args) ->
		{
			message.reply("Ops, qualcuno si è scordato di settare una callback per questo comando!").queue();
			return true;
		};
	}
	
	/**
	 * Set the callback that is called when the command is sent to the chat.
	 *
	 * @param action
	 * 		The callback that will be called when the cmmand is sent to the chat.
	 *
	 * @return The command.
	 */
	public UnlistedBotCommand setAction(UnlistedBotCommandAction action)
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
	 */
	public boolean doAction(Guild guild, MessageChannel channel, Message message, User author, String[] args)
	{
		if (this.action != null)
		{
			return this.action.doAction(guild, channel, message, author, args);
		}
		
		System.err.println("Action not set for command: " + this.getId());
		
		return false;
	}
	
	public static CommandData getCommandDataFromString(String commandPrefix, String str)
	{
		String commandID = str.substring(commandPrefix.length()).strip().split(" ")[0]; //Get the command id.
		String[] arguments = str.substring(commandPrefix.length() + commandID.length() + 1).strip().split(" "); //Get the arguments.
		
		CommandData data = new CommandData(commandID, arguments);
		
		if (arguments.length == 1 && arguments[0].isBlank())
		{
			//If there are no arguments return null.
			//return null;
			data.arguments = null;
		}
		
		return data;
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
	
	public boolean doJoinArguments()
	{
		return this.joinArguments;
	}
	
	public void setJoinArguments(boolean joinArguments)
	{
		this.joinArguments = joinArguments;
	}
	
	public String getHelp()
	{
		return this.help;
	}
	
	public void setHelp(String help)
	{
		this.help = help;
	}
	
	public String getArgumentsDescription(String guildID)
	{
		return (SerpensBot.getCommandSymbol(guildID) + this.getModulePrefix(guildID) + " " + this.id + " " + this.argumentsDescription).strip();
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
	
	public String getModulePrefix(String guildID)
	{
		if (!this.modulePrefix.containsKey(guildID))
		{
			this.modulePrefix.put(guildID, this.defaultModulePrefix);
		}
		
		return this.modulePrefix.get(guildID);
	}
	
	public void setModulePrefix(String guildID, String modulePrefix)
	{
		this.modulePrefix.put(guildID, modulePrefix);
	}
	
	public void setDefaultPrefix(String defaultModulePrefix)
	{
		this.defaultModulePrefix = defaultModulePrefix;
	}
	
	public String getDefaultModulePrefix()
	{
		return this.defaultModulePrefix;
	}
	
	public void setDefaultModulePrefix(String defaultModulePrefix)
	{
		this.defaultModulePrefix = defaultModulePrefix;
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
