package com.serpenssolida.discordbot.module.channelfilter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.SerpensBot;
import com.serpenssolida.discordbot.command.BotCommand;
import com.serpenssolida.discordbot.module.BotListener;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChannelFilterListener extends BotListener
{
	private static final String FOLDER = "channelfilter";
	private static final Logger logger = LoggerFactory.getLogger(ChannelFilterListener.class);
	
	private final HashMap<String, GuildFilterData> channelFilters = new HashMap<>();
	
	public ChannelFilterListener()
	{
		super("channelfilter");
		this.setModuleName("Channel filter");
		
		//Command for creating a game.
		BotCommand command = new BotCommand("set", "Setta o modifica il filtro per il canale specificato.");
		command.setAction(this::setChannelFilter);
		command.getSubcommand()
				.addOption(OptionType.CHANNEL, "channel", "Canale su cui settare il filtro.", true)
				.addOption(OptionType.BOOLEAN, "requires_images", "Se settato a true i messaggi dovranno contenere un immagine per essere accettati.", false)
				.addOption(OptionType.BOOLEAN, "requires_links", "Se settato a true i messaggi dovranno contenere un link per essere accettati.", false);
		this.addBotCommand(command);
		
		//Command for creating a game.
		command = new BotCommand("remove", "Rimuove il filtro per il canale specificato");
		command.setAction(this::removeChannelFilter);
		command.getSubcommand()
				.addOption(OptionType.CHANNEL, "channel", "Canale da cui rimuovere il filtro.", true);
		this.addBotCommand(command);
	}
	
	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event)
	{
		//Don't accept messages from private channels.
		if (!event.isFromGuild())
			return;
		
		Message message = event.getMessage();
		Guild guild = event.getGuild();
		MessageChannel channel = event.getChannel(); //Channel where the message was sent.
		User author = event.getAuthor();
		
		//TODO: add support for threads when they are supported.
		
		//If the author of the message is the bot, ignore the message.
		if (SerpensBot.api.getSelfUser().getId().equals(author.getId()))
			return;
		
		FilterData filter = this.getFilter(guild.getId(), channel.getId());
		
		//If the is no filter for the channel the message will not be checked.
		if (filter == null)
			return;
		
		//Check if the message contains images.
		boolean hasImages = false;
		for (Message.Attachment attachment : message.getAttachments())
		{
			if (attachment.isImage())
			{
				hasImages = true;
				break;
			}
		}
		
		//Check if the message contains URLs.
		String linkRegex = "https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)";
		Pattern p = Pattern.compile(linkRegex);
		Matcher m = p.matcher(message.getContentDisplay());
		boolean hasLinks = m.find();
		
		//Check filters.
		if ((filter.getRequiresImages() && !hasImages) || (filter.getRequiresLinks() && !hasLinks))
		{
			message.delete().queue();
			String filterText;
			
			if (filter.getRequiresImages() && !filter.getRequiresLinks())
				filterText = "delle immagini.";
			else if (!filter.getRequiresImages() && filter.getRequiresLinks())
				filterText = "degli URL.";
			else
				filterText = "delle immagini o degli URL.";
			
			Message errorMessage = MessageUtils.buildErrorMessage("Messaggio non permesso", author, "I messaggi inviati nel canale *#" + channel.getName() + "* di **" + guild.getName() + "** devono contenere " + filterText);
			PrivateChannel privateChannel = author.openPrivateChannel().complete();
			privateChannel.sendMessage(errorMessage).queue();
			privateChannel.close().queue();
		}
	}
	
	private void setChannelFilter(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping channelArg = event.getOption("channel");
		OptionMapping requireImagesArg = event.getOption("requires_images");
		OptionMapping requireLinksArg = event.getOption("requires_links");
		
		//Check if user is an admin.
		if (!SerpensBot.isAdmin(event.getMember()))
		{
			Message message = MessageUtils.buildErrorMessage("Channel Filter", author, "Devi essere un admin per impostare un filtro.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if argument is present.
		if (channelArg == null)
		{
			Message message = MessageUtils.buildErrorMessage("Channel Filter", author, "Parametro channel assente.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Create the filter.
		FilterData filter = new FilterData();
		
		//Set image filter.
		if (requireImagesArg != null)
			filter.setRequiresImages(requireImagesArg.getAsBoolean());
		
		//Set link filter.
		if (requireLinksArg != null)
			filter.setRequiresLinks(requireLinksArg.getAsBoolean());
		
		//Update the filter and save the data.
		this.setFilter(guild.getId(), channelArg.getAsGuildChannel().getId(), filter);
		this.saveFilters(guild.getId());
		
		//Reply to the event.
		Message message = MessageUtils.buildSimpleMessage("Channel Filter", author, "Filtro per il canale assegnato correttamente.");
		event.reply(message).setEphemeral(false).queue();
	}
	
	private void removeChannelFilter(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping channelArg = event.getOption("channel");
		
		//Check if user is an admin.
		if (!SerpensBot.isAdmin(event.getMember()) && !event.getMember().isOwner())
		{
			Message message = MessageUtils.buildErrorMessage("Channel Filter", author, "Devi essere un admin per rimuovere un filtro.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if argument is present.
		if (channelArg == null)
		{
			Message message = MessageUtils.buildErrorMessage("Channel Filter", author, "Parametro channel assente.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if teh channel has a filter.
		if (this.getFilter(guild.getId(), channelArg.getAsGuildChannel().getId()) == null)
		{
			Message message = MessageUtils.buildErrorMessage("Channel Filter", author, "Il canale non ha nessun filtro assegnato.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Remove the filter.
		this.removeFilter(guild.getId(), channelArg.getAsGuildChannel().getId());
		this.saveFilters(guild.getId());
		
		//Reply to the event.
		Message message = MessageUtils.buildSimpleMessage("Channel Filter", author, "Filtro per il canale rimosso correttamente.");
		event.reply(message).setEphemeral(false).queue();
	}
	
	private FilterData getFilter(String guildID, String channelID)
	{
		if (!this.channelFilters.containsKey(guildID))
			this.loadFilters(guildID);
		
		GuildFilterData filters = this.channelFilters.get(guildID);
		return filters.getFilter(channelID);
	}
	
	private void setFilter(String guildID, String channelID, FilterData filter)
	{
		if (!this.channelFilters.containsKey(guildID))
			this.loadFilters(guildID);
		
		GuildFilterData filters = this.channelFilters.get(guildID);
		filters.setFilter(channelID, filter);
	}
	
	private void removeFilter(String guildID, String channelID)
	{
		if (!this.channelFilters.containsKey(guildID))
			this.loadFilters(guildID);
		
		GuildFilterData filters = this.channelFilters.get(guildID);
		filters.removeFilter(channelID);
	}
	
	private void loadFilters(String guildID)
	{
		File fitersFile = new File(Paths.get("server_data", guildID, ChannelFilterListener.FOLDER,  "filters.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Guild guild = SerpensBot.api.getGuildById(guildID);
		
		if (guild == null)
			return;
		
		logger.info("Cariamento filtri per il server `{}`.", guild.getName());
		
		try (BufferedReader reader = new BufferedReader(new FileReader(fitersFile)))
		{
			GuildFilterData guildFilterData = gson.fromJson(reader, GuildFilterData.class);
			this.channelFilters.put(guildID, guildFilterData);
		}
		catch (FileNotFoundException e)
		{
			logger.info("Nessun file da caricare.");
			this.channelFilters.put(guildID, new GuildFilterData());
			this.saveFilters(guildID);
		}
		catch (IOException e)
		{
			logger.error("", e);
			this.channelFilters.put(guildID, new GuildFilterData());
			this.saveFilters(guildID);
		}
	}
	
	public void saveFilters(String guildID)
	{
		File fitersFile = new File(Paths.get("server_data", guildID, ChannelFilterListener.FOLDER,  "filters.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Guild guild = SerpensBot.api.getGuildById(guildID);
		
		if (guild == null)
			return;
		
		logger.info("Salvataggio filtri per il server \"{}\".", guild.getName());
		
		try (PrintWriter writer = new PrintWriter(new FileWriter(fitersFile)))
		{
			GuildFilterData filters = this.channelFilters.get(guildID);
			writer.println(gson.toJson(filters));
		}
		catch (FileNotFoundException e)
		{
			try
			{
				fitersFile.getParentFile().mkdirs();
				
				if (fitersFile.createNewFile())
					this.saveFilters(guildID);
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
}
