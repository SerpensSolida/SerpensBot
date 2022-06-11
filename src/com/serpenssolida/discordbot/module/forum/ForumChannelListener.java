package com.serpenssolida.discordbot.module.forum;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.SerpensBot;
import com.serpenssolida.discordbot.command.BotCommand;
import com.serpenssolida.discordbot.interaction.ButtonAction;
import com.serpenssolida.discordbot.interaction.InteractionGroup;
import com.serpenssolida.discordbot.modal.ModalCallback;
import com.serpenssolida.discordbot.module.BotListener;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.HashMap;

public class ForumChannelListener extends BotListener
{
	private final HashMap<String, Forum> forums = new HashMap<>();
	
	private static final Logger logger = LoggerFactory.getLogger(Forum.class);
	
	public ForumChannelListener()
	{
		super("forum");
		this.setModuleName("ForumChannel");
		
		//This module has no task.
		this.removeBotCommand("cancel");
		
		//Command for creating an embed.
		BotCommand command = new BotCommand("create", "Crea un canale da usare come forum.");
		command.setAction(this::initForum);
		command.getSubcommand()
				.addOption(OptionType.STRING, "channel-name", "Il nome del canale da creare.", true);
		this.addBotCommand(command);
		
		//Command for creating an embed.
		command = new BotCommand("init", "Converte un canale in un forum.");
		command.setAction(this::convertChannelToForum);
		command.getSubcommand()
				.addOption(OptionType.CHANNEL, "channel", "Il canale da convertire", true);
		this.addBotCommand(command);
		
		//Load saved forums.
		this.loadForums();
	}
	
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
			
			for (Forum forum : this.forums.values())
			{
				//Add button callback.
				InteractionGroup startThreadButtonGroup = new InteractionGroup();
				startThreadButtonGroup.addButtonCallback("create_thread", this.getCreateThreadButtonCallback());
				this.addInteractionGroup(forum.getGuildID(), forum.getMessageID(), startThreadButtonGroup);
			}
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
	
	private void saveForums()
	{
		File fileCharacters = new File(Paths.get("global_data", "forum",  "forums.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		logger.info("Salvataggio/aggiornamento dei forum aperti.");
		
		//Save data to file.
		try (PrintWriter writer = new PrintWriter(new FileWriter(fileCharacters)))
		{
			ForumData forumData = new ForumData(this.forums);
			writer.println(gson.toJson(forumData));
		}
		catch (FileNotFoundException e)
		{
			try
			{
				fileCharacters.getParentFile().mkdirs();
				
				if (fileCharacters.createNewFile())
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
		Message forumMessage = this.createStartMessage(guild, event.getTextChannel(), forum);
		
		//Update forum data.
		forum.setMessageID(forumMessage.getId());
		this.saveForums();
		
		logger.info("Messaggio del forum nel canale #{} ripristinato correttamente", channel.getName());
	}
	
	private void initForum(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping channelNameArg = event.getOption("channel-name");
		
		//Check if user is an admin.
		if (!SerpensBot.isAdmin(event.getMember()))
		{
			Message message = MessageUtils.buildErrorMessage("Forum channel", author, "Devi essere un admin per creare un forum.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check argument.
		if (channelNameArg == null)
		{
			Message message = MessageUtils.buildErrorMessage("Forum channel", author, "Devi inserire il parametro channel_name.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Get parameters.
		String channelName = channelNameArg.getAsString();
		
		//Check if a channel with the given name already exists.
		if (!guild.getTextChannelsByName(channelName, true).isEmpty())
		{
			Message message = MessageUtils.buildErrorMessage("Forum channel", author, "Un canale con il nome \"" + channelName + "\" è già presente.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Create and send the modal.
		Modal modal = this.getForumDataModal();
		event.replyModal(modal).queue();
		this.addModalCallback(guild.getId(), author.getId(), this.generateCreateForumCallback(channelName));
	}
	
	private void convertChannelToForum(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping channelArg = event.getOption("channel");
		
		//Check if user is an admin.
		if (!SerpensBot.isAdmin(event.getMember()))
		{
			Message message = MessageUtils.buildErrorMessage("Forum channel", author, "Devi essere un admin per convertire un canale in un forum.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check argumets.
		if (channelArg == null)
		{
			Message message = MessageUtils.buildErrorMessage("Forum channel", author, "Devi inserire il parametro channel.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Get parameters.
		TextChannel targetChannel = channelArg.getAsTextChannel();
		
		//Create and send the modal.
		Modal modal = this.getForumDataModal();
		event.replyModal(modal).queue();
		this.addModalCallback(guild.getId(), author.getId(), this.generateConvertChannelCallback(targetChannel));
	}
	
	@NotNull
	private Modal getForumDataModal()
	{
		//Create the fields.
		TextInput forumTitle = TextInput.create("title", "Titolo del messaggio", TextInputStyle.SHORT)
				.build();
		TextInput forumDescription = TextInput.create("description", "Descrizione del messaggio", TextInputStyle.PARAGRAPH)
				.build();
		TextInput buttonLabel = TextInput.create("button_label", "Testo del bottone", TextInputStyle.SHORT)
				.build();
		
		//Create modal.
		return Modal.create("forum", "Forum Channel")
				.addActionRows(ActionRow.of(forumTitle), ActionRow.of(forumDescription), ActionRow.of(buttonLabel))
				.build();
	}
	
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
				Message message = MessageUtils.buildErrorMessage("Forum channel", author, "Devi inserire tutti i parametri.");
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
			
			Message message = MessageUtils.buildSimpleMessage("Forum channel", author, "Forum inizializzato correttamente.");
			event.reply(message).queue();
		};
	}
	
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
				Message message = MessageUtils.buildErrorMessage("Forum channel", author, "Devi inserire tutti i parametri.");
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
			
			Message message = MessageUtils.buildSimpleMessage("Forum channel", author, "Canale inizializzato correttamente.");
			event.reply(message).queue();
		};
	}
	
	private ModalCallback generateCreateThreadModalCallback()
	{
		return (event, guild, channel, author) ->
		{
			ModalMapping titleArg = event.getValue("title");
			
			//Check modal input.
			if (titleArg == null)
			{
				Message message = MessageUtils.buildErrorMessage("Forum channel", author, "Devi inserire tutti i parametri.");
				event.reply(message).setEphemeral(true).queue();
				return;
			}
			
			//Get modal input.
			String title = titleArg.getAsString();
			
			//Create thread.
			ThreadChannel threadChannel = event.getTextChannel().createThreadChannel(title).complete();
			threadChannel.addThreadMember(author).queue();
			
			//Get the forum data.
			Forum forum = this.forums.get(event.getTextChannel().getId());
			
			//Delete the previous forum message.
			Message previousForumMessage = event.getTextChannel().retrieveMessageById(forum.getMessageID()).complete();
			previousForumMessage.delete().complete();
			this.removeInteractionGroup(guild.getId(), previousForumMessage.getId());
			
			//Recreate the forum message.
			Message forumMessage = this.createStartMessage(guild, event.getTextChannel(), forum);
			
			//Update forum data.
			forum.setMessageID(forumMessage.getId());
			this.saveForums();
			
			Message message = MessageUtils.buildSimpleMessage("Forum channel", author, "Thread creato correttamente.");
			event.reply(message).setEphemeral(true).queue();
		};
	}
	
	private ButtonAction getCreateThreadButtonCallback()
	{
		return (event, guild, channel, message, author) ->
		{
			//Create modal fields.
			TextInput forumTitle = TextInput.create("title", "Titolo del thread", TextInputStyle.SHORT)
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
	
	private Message createStartMessage(Guild guild, TextChannel channel, Forum forum)
	{
		//Build forum message.
		MessageBuilder initMessageBuilder = new MessageBuilder(MessageUtils.buildSimpleMessage(forum.getTitle(), SerpensBot.getApi().getSelfUser(), forum.getDescription()));
		initMessageBuilder.setActionRows(ActionRow.of(Button.primary("create_thread", forum.getButtonLabel())));
		Message forumMessage = channel.sendMessage(initMessageBuilder.build()).complete();
		
		//Add button callback.
		InteractionGroup startThreadButtonGroup = new InteractionGroup();
		startThreadButtonGroup.addButtonCallback("create_thread", this.getCreateThreadButtonCallback());
		this.addInteractionGroup(guild.getId(), forumMessage.getId(), startThreadButtonGroup);
		
		return forumMessage;
	}
}
