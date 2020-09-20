package com.serpenssolida.discordbot;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class BotListener extends ListenerAdapter
{
	private String modulePrefix = ""; //Prefix of the module, used for commands.
	private String internalID = ""; //Internal id used for retrieving listeners from the list.
	private HashMap<User, Task> tasks = new HashMap<>(); //List of task currently running. //TODO: Make task unique per player and not per module.
	private HashMap<String, Command> commands = new HashMap<>(); //List of commands of the module.
	
	public BotListener(String modulePrefix)
	{
		this.modulePrefix = modulePrefix;
		this.internalID = this.modulePrefix;
		
		Command command = (new Command("cancel", 0)).setCommandListener((guild, channel, message, author, args) ->
		{
			this.cancelTask(channel, author);
			return true;
		});
		command.setHelp("Cancella la procedura corrente.");
		this.addCommand(command);
		
		command = (new Command("help", 1)).setCommandListener((guild, channel, message, author, args) ->
		{
			this.sendHelp(channel, author, args);
			return true;
		});
		command.setMinArgumentNumber(0);
		command.setHelp("Mostra questo messaggio oppure le info su come usare il comando dato.");
		command.setArgumentsDescription("[nome_comando]");
		this.addCommand(command);
	}
	
	public void onMessageReceived(@Nonnull MessageReceivedEvent event)
	{
		String message = event.getMessage().getContentDisplay().replaceAll(" +", " "); //Received message.
		Guild guild = event.getGuild();
		User author = event.getAuthor(); //Author of the message.
		MessageChannel channel = event.getChannel(); //Channel where the message was sent.
		String commandPrefix = BotMain.commandSymbol + this.modulePrefix; //Command prefix of the module.
		
		//Get the task the author is currently running.
		Task task = this.getTask(author);
		
		//If the author of the message is the bot, ignore the message.
		if (BotMain.api.getSelfUser().getId().equals(author.getId())) return;
		
		if (message.startsWith(commandPrefix) && !commandPrefix.equals(message.strip())) //The message is a command.
		{
			Command.CommandData data = Command.getCommandDataFromString(commandPrefix, message);
			Command command = this.getCommand(data.commandID);
			String[] arguments = data.arguments;
			
			if (command == null) //Command was not found.
			{
				channel.sendMessage("> Il comando `" + data.commandID + "` non esiste.").queue();
				return;
			}
			
			//Number of arguments sent with the message.
			int argNum = (arguments == null) ? 0 : arguments.length;
			
			//Check if the number of arguments is correct.
			if (argNum >= command.getMinArgumentNumber() && argNum <= command.getMaxArgumentNumber())
			{
				command.doAction(guild, channel, event.getMessage(), author, arguments); //Run the command.
			}
			else
			{
				channel.sendMessage("> Errore, numero argomenti errato.").queue();
			}
		}
		else if (task != null && task.getChannel().equals(channel))
		{
			//Pass the event to the task.
			Task.TaskResult result = task.consumeMessage(event.getMessage());
			
			//Remove the task if it finished.
			if (result == Task.TaskResult.Finished)
			{
				this.removeTask(task);
			}
		}
	}
	
	public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event)
	{
		MessageReaction messageReaction = event.getReaction(); //Reaction added to the message.
		User author = event.getUser(); //The user that added the reaction.
		
		if (author == null) return;
		
		//Ignore bot reaction.
		if (BotMain.api.getSelfUser().getId().equals(author.getId())) return;
		
		//Get the message the reaction was added to.
		event.getChannel().retrieveMessageById(event.getMessageId()).queue(message ->
		{
			//Pass the reaction and the author to the task the user is running.
			Task task = this.getTask(author);
			
			if (task == null)
				return;
			
			Task.TaskResult result = task.consumeReaction(message, messageReaction.getReactionEmote().getName());
			
			//Remove the task if it finished.
			if (result == Task.TaskResult.Finished)
			{
				this.removeTask(task);
			}
		});
	}
	
	private void cancelTask(MessageChannel channel, User author)
	{
		MessageBuilder builder = new MessageBuilder();
		
		if (this.getTask(author) != null)
		{
			this.removeTask(this.getTask(author));
			builder.append("> La procedura corrente è stata annullata.");
		}
		else
		{
			builder.append("> Nessuna procedura in corso.");
		}
		
		channel.sendMessage(builder.build()).queue();
	}
	
	private void sendHelp(MessageChannel channel, User author, String[] args)
	{
		MessageBuilder builder = new MessageBuilder();
		if (args == null)
		{
			builder.append("> Lista comandi modulo Hunger Games\n> \n");
			for (Map.Entry<String, Command> command : this.commands.entrySet())
			{
				builder.appendFormat("> `%s` %s\n", command.getValue().getArgumentsDescription(), command.getValue().getHelp());
			}
		}
		else
		{
			String commandID = args[0];
			Command command = this.getCommand(commandID);
			if (command != null)
			{
				builder.appendFormat("> Comando `%s`\n> \n", command.getId()).appendFormat("> Utilizzo: `%s`\n", command.getArgumentsDescription()).appendFormat("> %s", command.getHelp());
			}
			else
			{
				builder.appendFormat("> Il comando `%s` non esiste.", commandID);
			}
		}
		channel.sendMessage(builder.build()).queue();
	}
	
	public void setModulePrefix(String modulePrefix)
	{
		for (Command command : this.commands.values())
		{
			command.setModulePrefix(modulePrefix);
		}
		
		this.modulePrefix = modulePrefix;
	}
	
	public String getModulePrefix()
	{
		return this.modulePrefix;
	}
	
	public String getInternalID()
	{
		return this.internalID;
	}
	
	public void setInternalID(String internalID)
	{
		this.internalID = internalID;
	}
	
	public void addCommand(Command command)
	{
		this.commands.put(command.getId(), command);
	}
	
	public void removeCommand(String id)
	{
		this.commands.remove(id);
	}
	
	public Command getCommand(String id)
	{
		return this.commands.get(id);
	}
	
	public Task getTask(User user)
	{
		return this.tasks.get(user);
	}
	
	public void addTask(Task task)
	{
		if (task.isInterrupted())
		{
			return;
		}
		
		User user = task.getUser();
		Task currentUserTask = this.getTask(user);
		
		//Replace the current task (if there is one) with the new one.
		if (currentUserTask != null)
		{
			System.out.println("L'utente ha già una task, annullamento task corrente.");
			this.removeTask(currentUserTask);
		}
		
		this.tasks.put(user, task);
	}
	
	public void removeTask(Task task)
	{
		this.tasks.remove(task.getUser());
	}
}
