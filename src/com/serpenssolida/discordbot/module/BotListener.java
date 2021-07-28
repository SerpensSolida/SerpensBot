package com.serpenssolida.discordbot.module;

import com.serpenssolida.discordbot.SerpensBot;
import com.serpenssolida.discordbot.MessageUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
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
	private HashMap<String, HashMap<User, Task>> tasks = new HashMap<>(); //List of task currently running.
	private HashMap<String, UnlistedBotCommand> unlistedBotCommands = new HashMap<>(); //List of commands of the module.
	private LinkedHashMap<String, BotCommand> botCommands = new LinkedHashMap<>(); //List of commands of the module that are displayed in the client command list.
	private HashMap<String, HashMap<String, ButtonGroup>> activeGlobalButtons = new HashMap<>();
	
	public BotListener(String modulePrefix)
	{
		//this.modulePrefix = modulePrefix;
		this.internalID = modulePrefix;
		
		BotCommand command = new BotCommand("help", SerpensBot.getMessage("botlistener_command_help_desc"));
		command.setAction((event, guild, channel, author) ->
		{
			this.sendHelp(event, guild, channel, author);
			return true;
		});
		command.getSubcommand()
				.addOption(OptionType.STRING, "command-name", SerpensBot.getMessage("botlistener_command_help_param1"), false);
		this.addBotCommand(command);
		
		command = new BotCommand("cancel", SerpensBot.getMessage("botlistener_command_cancel_desc"));
		command.setAction((event, guild, channel, author) ->
		{
			this.cancelTask(event, guild, channel, author);
			return true;
		});
		this.addBotCommand(command);
	}
	
	public void onMessageReceived(@Nonnull MessageReceivedEvent event)
	{
		//Don't accept messages from private channels.
		if (!event.isFromGuild())
			return;
		
		String message = event.getMessage().getContentDisplay().replaceAll(" +", " "); //Received message.
		Guild guild = event.getGuild();
		User author = event.getAuthor(); //Author of the message.
		MessageChannel channel = event.getChannel(); //Channel where the message was sent.
		String commandPrefix = SerpensBot.getCommandSymbol(guild.getId()) + this.getModulePrefix(guild.getId()); //Command prefix of the module.
		
		//Get the task the author is currently running.
		Task task = this.getTask(guild.getId(), author);
		
		//If the author of the message is the bot, ignore the message.
		if (SerpensBot.api.getSelfUser().getId().equals(author.getId())) return;
		
		if (message.startsWith(commandPrefix) && !commandPrefix.equals(message.strip())) //The message is a command.
		{
			UnlistedBotCommand.CommandData data = UnlistedBotCommand.getCommandDataFromString(commandPrefix, message);
			UnlistedBotCommand command = this.getUnlistedBotCommand(data.commandID);
			String[] arguments = data.arguments;
			
			//Check if the command exists.
			if (command == null)
			{
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
				if (SerpensBot.getDeleteCommandMessages(guild.getId()) && deleteMessage)
				{
					channel.deleteMessageById(event.getMessageId()).queue();
				}
			}
			else
			{
				//channel.sendMessage("> Numero argomenti errato.").queue();
				String embedTitle = SerpensBot.getMessage("botlistener_unlisted_command_title", command.getId());
				String embedDescription = SerpensBot.getMessage("botlistener_unlisted_command_argument_number_error");
				channel.sendMessage(MessageUtils.buildSimpleMessage(embedTitle, author, embedDescription)).queue();
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
		//Don't accept reaction from private channels.
		if (!event.isFromGuild())
			return;
		
		MessageReaction messageReaction = event.getReaction(); //Reaction added to the message.
		User author = event.getUser(); //The user that added the reaction.
		Guild guild = event.getGuild(); //The user that added the reaction.
		MessageChannel channel = event.getChannel(); //The user that added the reaction.
		
		if (author == null)
			return;
		
		//Ignore bot reaction.
		if (SerpensBot.api.getSelfUser().getId().equals(author.getId()))
			return;
		
		//Pass the reaction and the author to the task the user is running.
		Task task = this.getTask(guild.getId(), author);
		
		if (task == null)
			return;
		
		//Get the message the reaction was added to.
		event.retrieveMessage().queue(message ->
		{
			try
			{
				task.consumeReaction(message, messageReaction.getReactionEmote().getName());
				
				//Remove the task if it finished.
				if (!task.isRunning())
				{
					this.removeTask(guild.getId(), task);
				}
			}
			catch (PermissionException e)
			{
				this.removeTask(guild.getId(), task);
				Message errorMessage = MessageUtils.buildErrorMessage(SerpensBot.getMessage("botlistener_reaction_action_error"), event.getUser(), SerpensBot.getMessage("botlistener_missing_permmision_error", e.getPermission()));
				channel.sendMessage(errorMessage).queue();
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
		
		try
		{
			//Get the command from the list using the event command name and run it.
			BotCommand command = this.getBotCommand(event.getSubcommandName());
			command.doAction(event);
		}
		catch (PermissionException e)
		{
			Message message = MessageUtils.buildErrorMessage(SerpensBot.getMessage("botlistener_command_error"), event.getUser(), SerpensBot.getMessage("botlistener_missing_permmision_error", e.getPermission()));
			event.reply(message).setEphemeral(true).queue();
		}
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
		if (SerpensBot.api.getSelfUser().getId().equals(author.getId()))
			return;
		
		if (button == null)
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
				
				try
				{
					//Do button action.
					boolean deleteMessage = buttonCallback.doAction(event);
					//task.deleteButtons();
					
					//Delete message that has the clicked button if it should be deleted.
					if (SerpensBot.getDeleteCommandMessages(guild.getId()) && deleteMessage)
					{
						event.getHook().deleteOriginal().queue();
					}
					
					//Remove the task if it finished.
					if (!task.isRunning())
					{
						this.removeTask(guild.getId(), task);
					}
				}
				catch (PermissionException e)
				{
					this.removeTask(guild.getId(), task);
					Message message = MessageUtils.buildErrorMessage(SerpensBot.getMessage("botlistener_button_action_error"), event.getUser(), SerpensBot.getMessage("botlistener_missing_permmision_error", e.getPermission()));
					event.reply(message).setEphemeral(true).queue();
				}
			}
		}
		else if (buttonGroup != null) //If no button group is found and the user hasn't got any task the user cannot press a button.
		{
			ButtonCallback buttonCallback = buttonGroup.getButton(button.getId());
			//event.deferEdit().queue(); //Let discord know we know the button has been clicked.
			
			try
			{
				//Do button action.
				boolean deleteMessage = buttonCallback.doAction(event);
				
				//Delete message that has the clicked button if it should be deleted.
				if (SerpensBot.getDeleteCommandMessages(guild.getId()) && deleteMessage)
				{
					event.getHook().deleteOriginal().queue();
				}
			}
			catch (PermissionException e)
			{
				Message message = MessageUtils.buildErrorMessage(SerpensBot.getMessage("botlistener_button_action_error"), event.getUser(), SerpensBot.getMessage("botlistener_missing_permmision_error", e.getPermission()));
				event.reply(message).setEphemeral(true).queue();
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
		Task task = this.getTask(guild.getId(), author);
		
		//Check if the user has a task active.
		if (task == null)
		{
			Message message = MessageUtils.buildErrorMessage(SerpensBot.getMessage("botlistener_command_cancel_title"), event.getUser(), SerpensBot.getMessage("botlistener_command_cancel_no_task_error"));
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Remove the task
		this.removeTask(guild.getId(), this.getTask(guild.getId(), author));
		
		Message message = MessageUtils.buildSimpleMessage(SerpensBot.getMessage("botlistener_command_cancel_title"), author, SerpensBot.getMessage("botlistener_command_cancel_info"));
		event.reply(message).setEphemeral(false).queue();
	}
	
	/**
	 * Method for the "help" command of a module. Sends the module listed and unlisted command list.
	 */
	private void sendHelp(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping commandName = event.getOption("command-name");
		
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("", author);
		
		if (commandName == null)
		{
			//Send list of commands.
			embedBuilder.setTitle(SerpensBot.getMessage("botlistener_command_help_list_title", this.getModuleName()));
			
			if (!this.botCommands.isEmpty())
			{
				StringBuilder commandField = new StringBuilder();
				for (BotCommand command : this.botCommands.values())
				{
					commandField
							.append("`/" + this.getModulePrefix(guild.getId()) + " " + command.getSubcommand().getName() + "`")
							.append(" " + command.getSubcommand().getDescription() + "\n");
				}
				
				embedBuilder.addField(SerpensBot.getMessage("botlistener_command_help_listed_commands"), commandField.toString(), false);
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
				
				embedBuilder.addField(SerpensBot.getMessage("botlistener_command_help_unlisted_commands"), commandField.toString(), false);
			}
			
		}
		else
		{
			//Send command info.
			String commandID = commandName.getAsString();
			UnlistedBotCommand command = this.getUnlistedBotCommand(commandID);
			
			if (command == null)
			{
				String embedTitle = SerpensBot.getMessage("botlistener_command_help_command_title", commandID);
				String embedDescription = SerpensBot.getMessage("botlistener_command_help_command_not_found_error", commandID);
				Message message = MessageUtils.buildErrorMessage(embedTitle, event.getUser(), embedDescription);
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			embedBuilder.setTitle(SerpensBot.getMessage("botlistener_command_help_command_title", command.getId()));
			embedBuilder.appendDescription(SerpensBot.getMessage("botlistener_command_help_command_desc", command.getArgumentsDescription(guild.getId())) + "\n")
					.appendDescription(command.getHelp());
			
		}
		
		//channel.sendMessage(new MessageBuilder().setEmbed(embedBuilder.build()).build()).queue();
		event.reply(new MessageBuilder().setEmbed(embedBuilder.build()).build()).setEphemeral(false).queue();
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
			SerpensBot.loadSettings(guildID); //Try loading the settings.
			
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
