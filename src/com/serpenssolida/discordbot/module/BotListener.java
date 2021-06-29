package com.serpenssolida.discordbot.module;

import com.serpenssolida.discordbot.BotMain;
import com.serpenssolida.discordbot.ButtonGroup;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.Button;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class BotListener extends ListenerAdapter
{
	private HashMap<String, String> modulePrefix = new HashMap<>(); //Prefix of the module, used for commands.
	private String internalID = ""; //Internal id used for retrieving listeners from the list.
	private String moduleName = ""; //Readable name of the module.
	private HashMap<String, HashMap<User, Task>> tasks = new HashMap<>(); //List of task currently running. //TODO: Make task linked to servers.
	private HashMap<String, UnlistedBotCommand> unlistedBotCommands = new HashMap<>(); //List of commands of the module.
	private LinkedHashMap<String, BotCommand> botCommands = new LinkedHashMap<>(); //List of commands of the module that are displayed in the client command list. //TODO: Encapsulate.
	private HashMap<String, HashMap<String, ButtonGroup>> activeGlobalButtons = new HashMap<>();
	
	public BotListener(String modulePrefix)
	{
		//this.modulePrefix = modulePrefix;
		this.internalID = modulePrefix;
		
		//TODO: Add methods to add these commands manually
		
		BotCommand command = new BotCommand("help", "Mostra un messaggio di aiuto per il modulo.");
		command.setAction((event, guild, channel, author) ->
		{
			this.sendHelp(event, guild, channel, author);
			return true;
		});
		this.addBotCommand(command);
		
		command = new BotCommand("cancel", "Cancella la procedura corrente.");
		command.setAction((event, guild, channel, author) ->
		{
			this.cancelTask(event, guild, channel, author);
			return true;
		});
		this.addBotCommand(command);
	}
	
	public void onMessageReceived(@Nonnull MessageReceivedEvent event)
	{
		String message = event.getMessage().getContentDisplay().replaceAll(" +", " "); //Received message.
		Guild guild = event.getGuild();
		User author = event.getAuthor(); //Author of the message.
		MessageChannel channel = event.getChannel(); //Channel where the message was sent.
		String commandPrefix = BotMain.getCommandSymbol(guild.getId()) + this.getModulePrefix(guild.getId()); //Command prefix of the module.
		
		//Get the task the author is currently running.
		Task task = this.getTask(guild.getId(), author);
		
		//If the author of the message is the bot, ignore the message.
		if (BotMain.api.getSelfUser().getId().equals(author.getId())) return;
		
		if (message.startsWith(commandPrefix) && !commandPrefix.equals(message.strip())) //The message is a command.
		{
			UnlistedBotCommand.CommandData data = UnlistedBotCommand.getCommandDataFromString(commandPrefix, message);
			UnlistedBotCommand command = this.getUnlistedBotCommand(data.commandID);
			String[] arguments = data.arguments;
			
			//Check if the command exists.
			if (command == null)
			{
				channel.sendMessage("> Il comando `" + data.commandID + "` non esiste.").queue();
				return;
			}
			
			//If the command wants the arguments to be joined together instead of be splitted by " ".
			if (command.doJoinArguments() && arguments != null)
			{
				arguments = new String[1];
				arguments[0] = String.join(" ", data.arguments);
			}
			
			//Number of arguments sent with the message.
			int argNum = (arguments == null) ? 0 : arguments.length;
			
			//Check if the number of arguments is correct.
			if (argNum >= command.getMinArgumentNumber() && argNum <= command.getMaxArgumentNumber())
			{
				//Do command action.
				boolean deleteMessage = command.doAction(guild, channel, event.getMessage(), author, arguments); //Run the command.
				
				//Delete command message if the command was successfully ran.
				if (BotMain.getDeleteCommandMessages(guild.getId()) && deleteMessage)
				{
					channel.deleteMessageById(event.getMessageId()).queue();
				}
			}
			else
			{
				channel.sendMessage("> Numero argomenti errato.").queue();
			}
		}
		else if (task != null && task.getChannel().equals(channel))
		{
			//Pass the event to the task.
			task.consumeMessage(event.getMessage());
			
			//Remove the task if it finished.
			if (!task.isRunning())
			{
				this.removeTask(guild.getId(), task);
			}
		}
	}
	
	public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event)
	{
		MessageReaction messageReaction = event.getReaction(); //Reaction added to the message.
		User author = event.getUser(); //The user that added the reaction.
		Guild guild = event.getGuild(); //The user that added the reaction.
		
		if (author == null)
			return;
		
		//Ignore bot reaction.
		if (BotMain.api.getSelfUser().getId().equals(author.getId()))
			return;
		
		//Pass the reaction and the author to the task the user is running.
		Task task = this.getTask(guild.getId(), author);
		
		if (task == null)
			return;
		
		//Get the message the reaction was added to.
		event.retrieveMessage().queue(message ->
		{
			task.consumeReaction(message, messageReaction.getReactionEmote().getName());
			
			//Remove the task if it finished.
			if (!task.isRunning())
			{
				this.removeTask(guild.getId(), task);
			}
		});
	}
	
	@Override
	public void onSlashCommand(@Nonnull SlashCommandEvent event)
	{
		Guild guild = event.getGuild();
		
		if (guild == null)
			return;
		
		if (!event.getName().equals(this.getModulePrefix(guild.getId())))
			return;
		
		//Get the command from the list using the event command name and run it.
		BotCommand command = this.getBotCommand(event.getSubcommandName());
		command.doAction(event);
		
	}
	
	@Override
	public void onButtonClick(@Nonnull ButtonClickEvent event)
	{
		Button button = event.getButton(); //Reaction added to the message.
		User author = event.getUser(); //The user that added the reaction.
		Guild guild = event.getGuild(); //The user that added the reaction.
		MessageChannel channel = event.getChannel(); //Channel where the event occurred.
		
		//If this event is not from a guild ignore it.
		if (guild == null)
			return;
		
		//Ignore bot reaction.
		if (BotMain.api.getSelfUser().getId().equals(author.getId()))
			return;
		
		//Get the button that the user can press.
		ButtonGroup buttonGroup = this.getButtonGroup(guild.getId(), event.getMessageId());
		
		//Task can have buttons too.
		Task task = this.getTask(guild.getId(), author);
		
		if (task != null)
		{
			buttonGroup = task.getButtonGroup();
			
			if (buttonGroup != null)
			{
				ButtonCallback buttonCallback = buttonGroup.getButton(button.getId());
				
				//Do button action.
				boolean deleteMessage = buttonCallback.doAction(event);
				//task.deleteButtons();
				
				//Delete message that has the clicked button if it should be deleted.
				if (BotMain.getDeleteCommandMessages(guild.getId()) && deleteMessage)
				{
					event.getHook().deleteOriginal().queue();
				}
				
				//Remove the task if it finished.
				if (!task.isRunning())
				{
					this.removeTask(guild.getId(), task);
				}
			}
		}
		else if (buttonGroup != null) //If no button group is found and the user hasn't got any task the user cannot press a button.
		{
			ButtonCallback buttonCallback = buttonGroup.getButton(button.getId());
			//event.deferEdit().queue(); //Let discord know we know the button has been clicked.
			
			//Do button action.
			boolean deleteMessage = buttonCallback.doAction(event);
			
			//Delete message that has the clicked button if it should be deleted.
			if (BotMain.getDeleteCommandMessages(guild.getId()) && deleteMessage)
			{
				event.getHook().deleteOriginal().queue();
			}
		}
	}
	
	/**
	 * Generate a list of commands for the given guild.
	 *
	 * @param guild
	 * 		The guild.
	 *
	 * @return
	 * 		An {@link ArrayList<com.serpenssolida.discordbot.module.UnlistedBotCommand.CommandData>} of commands.
	 */
	public ArrayList<CommandData> generateCommands(Guild guild)
	{
		ArrayList<CommandData> commandList = new ArrayList<>();
		
		if (this.getModulePrefix(guild.getId()).isEmpty())
			return commandList;
		
		CommandData mainCommand = new CommandData(this.getModulePrefix(guild.getId()) , "Main module command");
		for (BotCommand botCommand : this.botCommands.values())
		{
			mainCommand.addSubcommands(botCommand.getSubcommand());
		}
		
		commandList.add(mainCommand);
		return commandList;
	}
	
	/**
	 * Method for the "cancel" command of a module. Based on the event data it will cancel a task.
	 */
	private void cancelTask(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		MessageBuilder messageBuilder = new MessageBuilder();
		Task task = this.getTask(guild.getId(), author);
		
		if (task != null)
		{
			this.removeTask(guild.getId(), this.getTask(guild.getId(), author));
			messageBuilder.append("> La procedura corrente è stata annullata.");
		}
		else
		{
			messageBuilder.append("> Nessuna procedura in corso.");
		}
		
		event.reply(messageBuilder.build()).setEphemeral(task == null).queue();
	}
	
	/**
	 * Method for the "help" command of a module. Sends the module listed and unlisted command list.
	 */
	private void sendHelp(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		EmbedBuilder embedBuilder = new EmbedBuilder();
		
		//TODO: Fix this command.
		
		//Add footer
		embedBuilder.setFooter("Richiesto da " + author.getName(), author.getAvatarUrl());
		embedBuilder.setAuthor(BotMain.api.getSelfUser().getName(), "https://github.com/SerpensSolida/SerpensBot", BotMain.api.getSelfUser().getAvatarUrl());
		
		OptionMapping commandName = event.getOption("nomecomando");
		if (commandName == null)
		{
			//Send list of commands.
			embedBuilder.setTitle("Lista comandi modulo " + this.getModuleName());
			
			if (!this.botCommands.isEmpty())
			{
				StringBuilder commandField = new StringBuilder();
				for (BotCommand command : this.botCommands.values())
				{
					commandField
							.append("`/" + this.getModulePrefix(guild.getId()) + " " + command.getSubcommand().getName() + "`")
							.append(" " + command.getSubcommand().getDescription() + "\n");
				}
				
				embedBuilder.addField("**Comandi listati**", commandField.toString(), false);
			}
			
			if (!this.unlistedBotCommands.isEmpty())
			{
				StringBuilder commandField = new StringBuilder();
				for (UnlistedBotCommand command : this.unlistedBotCommands.values())
				{
					commandField
							.append("`" + command.getArgumentsDescription(guild.getId()) + "`")
							.append(" " + command.getHelp() + "\n");
				}
				
				embedBuilder.addField("**Comandi non listati**", commandField.toString(), false);
			}
			
		}
		else
		{
			//Send command info.
			String commandID = commandName.getAsString();
			UnlistedBotCommand command = this.getUnlistedBotCommand(commandID);
			
			if (command != null)
			{
				embedBuilder.setTitle(String.format("Descrizione comando *%s*", command.getId()));
				embedBuilder.appendDescription(String.format("Utilizzo: `%s`\n", command.getArgumentsDescription(guild.getId())))
						.appendDescription(command.getHelp());
				//TODO: Add long help.
			}
			else
			{
				embedBuilder.appendDescription(String.format("> Il comando `%s` non esiste.", commandID));
			}
		}
		
		//channel.sendMessage(new MessageBuilder().setEmbed(embedBuilder.build()).build()).queue();
		event.reply(new MessageBuilder().setEmbed(embedBuilder.build()).build()).setEphemeral(false).queue();
	}
	
	protected static Message buildSimpleMessage(String title, User author, String description)
	{
		EmbedBuilder embedBuilder = BotMain.getDefaultEmbed(title, author);
		embedBuilder.setDescription(description);
		MessageBuilder messageBuilder = new MessageBuilder();
		messageBuilder.setEmbed(embedBuilder.build());
		
//		event.reply(messageBuilder.build()).setEphemeral(true).queue();
		return messageBuilder.build();
	}
	
	/**
	 * Sets the prefix of the module for the given guild.
	 *
	 * @param guildID
	 * 		The id of the guild.
	 * @param modulePrefix
	 * 		The new prefix of the module for the given guild.
	 */
	public void setModulePrefix(String guildID, String modulePrefix)
	{
		//Change unlisted command prefix.
		for (UnlistedBotCommand command : this.unlistedBotCommands.values())
		{
			command.setModulePrefix(guildID, modulePrefix);
		}
		
		this.modulePrefix.put(guildID, modulePrefix);
	}
	
	/**
	 * Get the module prefix of the module for the given guild.
	 *
	 * @param guildID
	 * 		The id of the guild.
	 *
	 * @return
	 * 		The prefix of the module for the given guild.
	 */
	public String getModulePrefix(String guildID)
	{
		if (!this.modulePrefix.containsKey(guildID))
		{
			BotMain.loadSettings(guildID); //Try loading the settings.
			
			if (!this.modulePrefix.containsKey(guildID)) //Settings for this guild not found.
				this.modulePrefix.put(guildID, this.internalID);
		}
		
		return this.modulePrefix.get(guildID);
	}
	
	public String getInternalID()
	{
		return this.internalID;
	}
	
	public void addBotCommand(BotCommand command)
	{
		if (command != null)
		{
			//command.setDefaultPrefix(this.internalID);
			this.botCommands.put(command.getId(), command);
		}
	}
	
	public void removeBotCommand(String id)
	{
		this.botCommands.remove(id);
	}
	
	public BotCommand getBotCommand(String id)
	{
		return this.botCommands.get(id);
	}
	
	public void addUnlistedBotCommand(UnlistedBotCommand command)
	{
		if (command != null)
		{
			command.setDefaultPrefix(this.internalID);
			this.unlistedBotCommands.put(command.getId(), command);
		}
	}
	
	public void removeUnlistedBotCommand(String id)
	{
		this.unlistedBotCommands.remove(id);
	}
	
	public UnlistedBotCommand getUnlistedBotCommand(String id)
	{
		return this.unlistedBotCommands.get(id);
	}
	
	public Task getTask(String guildID, User user)
	{
		if (!this.tasks.containsKey(guildID))
		{
			this.tasks.put(guildID, new HashMap<>());
		}
		
		return this.tasks.get(guildID).get(user);
	}
	
	protected void startTask(String guildID, Task task, GenericInteractionCreateEvent event)
	{
		this.addTask(guildID, task);
		task.start(event);
	}
	
	protected void addTask(String guildID, Task task)
	{
		if (task.isInterrupted())
		{
			return;
		}
		
		User user = task.getUser();
		Task currentUserTask = this.getTask(guildID, user);
		
		//Replace the current task (if there is one) with the new one.
		if (currentUserTask != null)
		{
			System.out.println("L'utente ha già una task, annullamento task corrente.");
			this.removeTask(guildID, currentUserTask);
		}
		
		if (!this.tasks.containsKey(guildID))
		{
			this.tasks.put(guildID, new HashMap<>());
		}
		
		
		this.tasks.get(guildID).put(user, task);
	}
	
	protected void removeTask(String guildID, Task task)
	{
		if (!this.tasks.containsKey(guildID))
		{
			this.tasks.put(guildID, new HashMap<>());
		}
		
		this.tasks.get(guildID).remove(task.getUser());
	}
	
	public void addButtonGroup(String guildID, String messageId, ButtonGroup buttonGroup)
	{
		HashMap<String, ButtonGroup> guildButtonGroups = this.activeGlobalButtons.computeIfAbsent(guildID, k -> new HashMap<>());
		guildButtonGroups.put(messageId, buttonGroup);
	}
	
	public ButtonGroup getButtonGroup(String guildID, String messageId)
	{
		HashMap<String, ButtonGroup> guildButtonGroups = this.activeGlobalButtons.computeIfAbsent(guildID, k -> new HashMap<>());
		return guildButtonGroups.get(messageId);
	}
	
	public void removeButtonGroup(String guildID, String messageId)
	{
		HashMap<String, ButtonGroup> guildButtonGroups = this.activeGlobalButtons.computeIfAbsent(guildID, k -> new HashMap<>());
		guildButtonGroups.remove(messageId);
	}
	
	public String getModuleName()
	{
		return this.moduleName;
	}
	
	public void setModuleName(String moduleName)
	{
		this.moduleName = moduleName;
	}
	
	public HashMap<String, BotCommand> getBotCommands()
	{
		return this.botCommands;
	}
	
	public HashMap<String, UnlistedBotCommand> getUnlistedBotCommands()
	{
		return this.unlistedBotCommands;
	}
}
