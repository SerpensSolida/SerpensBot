package com.serpenssolida.discordbot.module.forum;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.SerpensBot;
import com.serpenssolida.discordbot.UserUtils;
import com.serpenssolida.discordbot.command.BotCommand;
import com.serpenssolida.discordbot.interaction.ButtonAction;
import com.serpenssolida.discordbot.interaction.InteractionGroup;
import com.serpenssolida.discordbot.modal.ModalCallback;
import com.serpenssolida.discordbot.module.BotListener;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

public class ForumChannelListener extends BotListener
{
	private static final Logger logger = LoggerFactory.getLogger(ForumChannelListener.class);
	
	private final HashMap<String, Forum> forums = new HashMap<>();
	
	public ForumChannelListener()
	{
		super("forum");
		this.setModuleName("ForumChannel");
		
		//Command for creating a forum channel.
		BotCommand command = new BotCommand("create", "Crea un canale da usare come forum.");
		command.setAction(this::initForum);
		command.getSubcommand()
				.addOption(OptionType.STRING, "channel-name", "Il nome del canale da creare.", true);
		this.addBotCommand(command);
		
		//Command for converting a channel into a forum channel.
		command = new BotCommand("init", "Converte un canale in un forum.");
		command.setAction(this::convertChannelToForum);
		command.getSubcommand()
				.addOption(OptionType.CHANNEL, "channel", "Il canale da convertire", true);
		this.addBotCommand(command);
		
		//Load saved forums.
		this.loadForums();
	}
	
	/**
	 * Loads forum data.
	 */
	private void loadForums()
	{
		File fileCharacters = new File(Paths.get("global_data", "forum",  "forums.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		logger.info("Cariamento dei forum aperti.");
		
		//Load data from file.
		try (BufferedReader reader = new BufferedReader(new FileReader(fileCharacters)))
		{
			ForumData forumData = gson.fromJson(reader, ForumData.class);
			this.forums.putAll(forumData.getForums());
			
			HashSet<String> orphanForums = new HashSet<>();
			
			//Foreach forum create the callback.
			for (Map.Entry<String, Forum> forumEntry : this.forums.entrySet())
			{
				String channelID = forumEntry.getKey();
				Forum forum = forumEntry.getValue();
				
				Guild guild = SerpensBot.getApi().getGuildById(forum.getGuildID());
				TextChannel channel = SerpensBot.getApi().getChannelById(TextChannel.class, channelID);
				
				if (guild == null || channel == null)
				{
					logger.info("Il forum nel canale con id {} del server con id {} è diventato orfano. Cancellazione forum.", channelID, forum.getGuildID());
					orphanForums.add(channelID);
					continue;
				}
				
				if (this.checkForumMessageState(forum, guild, channel))
				{
					orphanForums.add(channelID);
					continue;
				}
				
				//Add button callback.
				InteractionGroup startThreadButtonGroup = new InteractionGroup();
				startThreadButtonGroup.addButtonCallback("create_thread", this.getCreateThreadButtonCallback());
				this.addInteractionGroup(forum.getGuildID(), forum.getMessageID(), startThreadButtonGroup);
			}
			
			//Remove all orfan forums and save the changes.
			if (this.forums.keySet().removeAll(orphanForums))
				this.saveForums();
		}
		catch (FileNotFoundException e)
		{
			logger.info("File dati dei forum non trovato.");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private boolean checkForumMessageState(Forum forum, Guild guild, TextChannel channel)
	{
		try
		{
			channel.retrieveMessageById(forum.getMessageID()).complete();
		}
		catch (ErrorResponseException e)
		{
			switch (e.getErrorResponse())
			{
				case UNKNOWN_MESSAGE: //Message was deleted.
					//Recreate the forum message.
					Message forumMessage = this.createStartMessage(guild, channel, forum);
					
					//Update forum data.
					forum.setMessageID(forumMessage.getId());
					logger.info("Messaggio del forum nel canale #{} del server \"{}\" è stato cancellato, ripristinato correttamente", channel.getName(), guild.getName());
					
					//Save data.
					this.saveForums();
					break;
					
				case MISSING_PERMISSIONS, MISSING_ACCESS: //Kicked from the guild or missing permissions..
					logger.info("Il forum nel canale #{} del server \"#{}\" è diventato orfano. Cancellazione forum.", channel.getName(), guild.getName());
					return true;
					
				default:
					break;
			}
		}
		
		return false;
	}
	
	/**
	 * Save forum data.
	 */
	private void saveForums()
	{
		File forumFile = new File(Paths.get("global_data", "forum",  "forums.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		logger.info("Salvataggio/aggiornamento dei forum aperti.");
		
		//Save data to file.
		try (PrintWriter writer = new PrintWriter(new FileWriter(forumFile)))
		{
			ForumData forumData = new ForumData(this.forums);
			writer.println(gson.toJson(forumData));
		}
		catch (FileNotFoundException e)
		{
			try
			{
				forumFile.getParentFile().mkdirs();
				
				if (forumFile.createNewFile())
					this.saveForums();
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void onMessageDelete(@NotNull MessageDeleteEvent event)
	{
		String deletedMessageID = event.getMessageId();
		MessageChannel channel = event.getChannel();
		Guild guild = event.getGuild();
		
		//Get the forum data.
		Forum forum = this.forums.get(channel.getId());
		
		if (forum == null)
			return;
		
		//Check if the deleted message is the forum message.
		if (!deletedMessageID.equals(forum.getMessageID()))
			return;
		
		//Remove the unlinked interactiong group.
		this.removeInteractionGroup(guild.getId(), deletedMessageID);
		
		//Recreate the forum message.
		Message forumMessage = this.createStartMessage(guild, event.getChannel().asTextChannel(), forum);
		
		//Update forum data.
		forum.setMessageID(forumMessage.getId());
		logger.info("Messaggio del forum nel canale #{} ripristinato correttamente", channel.getName());
		
		//Save data.
		this.saveForums();
	}
	
	@Override
	public void onChannelDelete(@NotNull ChannelDeleteEvent event)
	{
		Channel channel = event.getChannel();
		
		//Remove the forum that is inside the channel.
		Forum removedForum = this.forums.remove(channel.getId());
		
		//Check if the forum was removed.
		if (removedForum == null)
			return;
		
		logger.info("Il canale #{} nel server \"{}\" è stato rimosso. Rimozione forum associato al canale.", event.getGuild().getName(), channel.getName());
		
		//Remove interaction group and save the forum data.
		this.removeInteractionGroup(event.getGuild().getId(), removedForum.getMessageID());
		this.saveForums();
	}
	
	/**
	 * Callback for "create" command.
	 */
	private void initForum(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping channelNameArg = event.getOption("channel-name");
		
		//Check if user is an admin.
		if (!UserUtils.hasMemberAdminPermissions(event.getMember()))
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Forum channel", author, "Devi essere un admin per creare un forum.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check argument.
		if (channelNameArg == null)
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Forum channel", author, "Devi inserire il parametro channel_name.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Get parameters.
		String channelName = channelNameArg.getAsString();
		
		//Check name format.
		if (channelName.length() < 1 || channelName.length() > 100)
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Forum channel", author, "Il nome del canale deve essere lungo da 1 a 100 caratteri.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if a channel with the given name already exists.
		if (!guild.getTextChannelsByName(channelName, true).isEmpty())
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Forum channel", author, "Un canale con il nome \"" + channelName + "\" è già presente.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Create and send the modal.
		Modal modal = this.getForumDataModal();
		event.replyModal(modal).queue();
		this.addModalCallback(guild.getId(), author.getId(), this.generateCreateForumCallback(channelName));
	}
	
	/**
	 * Callback for "convert" command.
	 */
	private void convertChannelToForum(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping channelArg = event.getOption("channel");
		
		//Check if user is an admin.
		if (!UserUtils.hasMemberAdminPermissions(event.getMember()))
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Forum channel", author, "Devi essere un admin per convertire un canale in un forum.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check argumets.
		if (channelArg == null)
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Forum channel", author, "Devi inserire il parametro channel.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Get parameters.
		TextChannel targetChannel = channelArg.getAsChannel().asTextChannel();
		
		//Create and send the modal.
		Modal modal = this.getForumDataModal();
		event.replyModal(modal).queue();
		this.addModalCallback(guild.getId(), author.getId(), this.generateConvertChannelCallback(targetChannel));
	}
	
	/**
	 * Generate a Modal form that prompts the user to input data for the forum message.
	 *
	 * @return
	 * 		The Modal containing inputs for the requested data.
	 */
	@NotNull
	private Modal getForumDataModal()
	{
		//Create the fields.
		TextInput forumTitle = TextInput.create("title", "Titolo del messaggio", TextInputStyle.SHORT)
				.setMaxLength(100)
				.build();
		TextInput forumDescription = TextInput.create("description", "Descrizione del messaggio", TextInputStyle.PARAGRAPH)
				.setMaxLength(4000)
				.build();
		TextInput buttonLabel = TextInput.create("button_label", "Testo del bottone", TextInputStyle.SHORT)
				.setMaxLength(Button.LABEL_MAX_LENGTH)
				.build();
		
		//Create modal.
		return Modal.create("forum", "Forum Channel")
				.addActionRows(ActionRow.of(forumTitle), ActionRow.of(forumDescription), ActionRow.of(buttonLabel))
				.build();
	}
	
	/**
	 * Generate the callback for the "create forum" Modal.
	 *
	 * @param channelName
	 * 		The name of the channel that will be created.
	 *
	 * @return
	 * 		The modal callback for the "create forum" Modal.
	 */
	private ModalCallback generateCreateForumCallback(String channelName)
	{
		return (event, guild, channel, author) ->
		{
			ModalMapping titleArg = event.getValue("title");
			ModalMapping descriptionArg = event.getValue("description");
			ModalMapping buttonLabelArg = event.getValue("button_label");
			
			//Check modal input.
			if (titleArg == null || descriptionArg == null || buttonLabelArg == null)
			{
				MessageCreateData message = MessageUtils.buildErrorMessage("Forum channel", author, "Devi inserire tutti i parametri.");
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			//Get modal input.
			String title = titleArg.getAsString();
			String description = descriptionArg.getAsString();
			String buttonLabel = buttonLabelArg.getAsString();
			
			//Create the forum channel.
			TextChannel textChannel = guild.createTextChannel(channelName)
					.addPermissionOverride(guild.getPublicRole(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND_IN_THREADS), EnumSet.of(Permission.MESSAGE_SEND, Permission.CREATE_PRIVATE_THREADS, Permission.CREATE_PUBLIC_THREADS))
					.addPermissionOverride(guild.getBotRole(), EnumSet.of(Permission.MESSAGE_SEND, Permission.CREATE_PUBLIC_THREADS), null)
					.complete();
			
			//Create a new forum.
			Forum forum = new Forum(title, description, buttonLabel, guild.getId(), textChannel.getId());
			
			//Send the message containing the button to start a thread.
			Message forumMessage = this.createStartMessage(guild, textChannel, forum);
			
			//Add the forum to the map.
			forum.setMessageID(forumMessage.getId());
			this.forums.put(textChannel.getId(), forum);
			this.saveForums();
			
			MessageCreateData message = MessageUtils.buildSimpleMessage("Forum channel", author, "Forum inizializzato correttamente.");
			event.reply(message).queue();
		};
	}
	
	/**
	 * Generate the callback for the "convert channel" Modal.
	 *
	 * @param targetChannel
	 * 		The target channel to convert to a forum channel.
	 *
	 * @return
	 * 		The callback for the "convert channel" Modal.
	 */
	private ModalCallback generateConvertChannelCallback(TextChannel targetChannel)
	{
		return (event, guild, channel, author) ->
		{
			ModalMapping titleArg = event.getValue("title");
			ModalMapping descriptionArg = event.getValue("description");
			ModalMapping buttonLabelArg = event.getValue("button_label");
			
			//Check modal input.
			if (titleArg == null || descriptionArg == null || buttonLabelArg == null)
			{
				MessageCreateData message = MessageUtils.buildErrorMessage("Forum channel", author, "Devi inserire tutti i parametri.");
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			//Get modal input.
			String title = titleArg.getAsString();
			String description = descriptionArg.getAsString();
			String buttonLabel = buttonLabelArg.getAsString();
			
			//Set permission of the target channel
			targetChannel.upsertPermissionOverride(guild.getPublicRole())
					.setAllowed(EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND_IN_THREADS))
					.setDenied(EnumSet.of(Permission.MESSAGE_SEND, Permission.CREATE_PRIVATE_THREADS, Permission.CREATE_PUBLIC_THREADS))
					.complete();
			
			targetChannel.upsertPermissionOverride(guild.getBotRole())
					.setAllowed(EnumSet.of(Permission.MESSAGE_SEND, Permission.CREATE_PUBLIC_THREADS))
					.complete();
			
			//Create a new forum.
			Forum forum = new Forum(title, description, buttonLabel, guild.getId(), targetChannel.getId());
			
			//Send the message containing the button to start a thread.
			Message forumMessage = this.createStartMessage(guild, targetChannel, forum);
			
			//Add the forum to the map.
			forum.setMessageID(forumMessage.getId());
			this.forums.put(targetChannel.getId(), forum);
			this.saveForums();
			
			MessageCreateData message = MessageUtils.buildSimpleMessage("Forum channel", author, "Canale inizializzato correttamente.");
			event.reply(message).queue();
		};
	}
	
	/**
	 * Generate the callback for the "create thread" Modal.
	 *
	 * @return
	 * 		The callback for the "create thread" Modal.
	 */
	private ModalCallback generateCreateThreadModalCallback()
	{
		return (event, guild, channel, author) ->
		{
			ModalMapping titleArg = event.getValue("title");
			
			//Check modal input.
			if (titleArg == null)
			{
				MessageCreateData message = MessageUtils.buildErrorMessage("Forum channel", author, "Devi inserire tutti i parametri.");
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			//Get modal input.
			String title = titleArg.getAsString();
			
			//Create thread.
			ThreadChannel threadChannel = event.getChannel().asTextChannel().createThreadChannel(title).complete();
			threadChannel.addThreadMember(author).queue();
			
			//Get the forum data.
			Forum forum = this.forums.get(event.getChannel().asTextChannel().getId());
			
			//Delete the previous forum message.
			Message previousForumMessage = event.getChannel().asTextChannel().retrieveMessageById(forum.getMessageID()).complete();
			previousForumMessage.delete().complete();
			this.removeInteractionGroup(guild.getId(), previousForumMessage.getId());
			
			//Recreate the forum message.
			Message forumMessage = this.createStartMessage(guild, event.getChannel().asTextChannel(), forum);
			
			//Update forum data.
			forum.setMessageID(forumMessage.getId());
			this.saveForums();
			
			MessageCreateData message = MessageUtils.buildSimpleMessage("Forum channel", author, "Thread creato correttamente.");
			event.reply(message).setEphemeral(true).queue();
		};
	}
	
	/**
	 * Generate the callback for the button that send the "create thread" Modal.
	 *
	 * @return
	 * 		The callback for the button that send the "create thread" Modal.
	 */
	private ButtonAction getCreateThreadButtonCallback()
	{
		return (event, guild, channel, message, author) ->
		{
			//Create modal fields.
			TextInput forumTitle = TextInput.create("title", "Titolo del thread", TextInputStyle.SHORT)
					.setMaxLength(100)
					.build();
			
			//Create and send the modal.
			Modal modal = Modal.create("forum", "Forum Channel")
					.addActionRows(ActionRow.of(forumTitle))
					.build();
			event.replyModal(modal).queue();
			this.addModalCallback(guild.getId(), author.getId(), this.generateCreateThreadModalCallback());
			
			return false;
		};
	}
	
	/**
	 * Generate the forum message of a forum channel.
	 *
	 * @param guild
	 * 		The guild the forum is in.
	 * @param channel
	 * 		The channel the forum is in.
	 * @param forum
	 * 		The forum data for creating the message.
	 *
	 * @return
	 * 		The forum message for the given forum.
	 */
	private Message createStartMessage(Guild guild, TextChannel channel, Forum forum)
	{
		//Build forum message.
		MessageCreateBuilder initMessageBuilder = MessageCreateBuilder.from(MessageUtils.buildSimpleMessage(forum.getTitle(), forum.getDescription()));
		initMessageBuilder.addActionRow(List.of(Button.primary("create_thread", forum.getButtonLabel())));
		Message forumMessage = channel.sendMessage(initMessageBuilder.build()).complete();
		
		//Add button callback.
		InteractionGroup startThreadButtonGroup = new InteractionGroup();
		startThreadButtonGroup.addButtonCallback("create_thread", this.getCreateThreadButtonCallback());
		this.addInteractionGroup(guild.getId(), forumMessage.getId(), startThreadButtonGroup);
		
		return forumMessage;
	}
}
