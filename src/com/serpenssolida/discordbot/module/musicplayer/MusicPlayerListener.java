package com.serpenssolida.discordbot.module.musicplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.SerpensBot;
import com.serpenssolida.discordbot.command.BotCommand;
import com.serpenssolida.discordbot.interaction.ButtonAction;
import com.serpenssolida.discordbot.interaction.InteractionCallback;
import com.serpenssolida.discordbot.interaction.InteractionGroup;
import com.serpenssolida.discordbot.module.BotListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MusicPlayerListener extends BotListener implements TrackEventHandler
{
	private static final Logger logger = LoggerFactory.getLogger(MusicPlayerListener.class);
	private final HashMap<String, GuildAudioController> activeAudioController = new HashMap<>();
	private static final String YOUTUBE_REGEX = "^.*((youtu.be/)|(v/)|(/u/\\w/)|(embed/)|(watch\\?))\\??v?=?([^#&?]*).*";
	private static final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
	
	public MusicPlayerListener()
	{
		super("musicplayer");
		this.setModuleName("Music player");
		
		//This module has no task.
		this.removeBotCommand("cancel");
		
		//Command for adding a song to the queue.
		BotCommand command = new BotCommand("play", "Agginunge alla coda una traccia.");
		command.getSubcommand()
						.addOption(OptionType.STRING, "song", "Youtube URL/id della traccia da riprodurre.", true);
		command.setAction(this::playTrack);
		this.addBotCommand(command);
		
		//Command for skipping the current song.
		command = new BotCommand("skip", "Salta la traccia corrente.");
		command.setAction(this::skipTrack);
		this.addBotCommand(command);
		
		//Command for stopping the playback and quitting the voice channel.
		command = new BotCommand("stop", "Ferma la riproduzione corrente, il bot perderà le tracce inserite nella coda.");
		command.setAction(this::stopPlayback);
		this.addBotCommand(command);
		
		AudioSourceManagers.registerRemoteSources(MusicPlayerListener.playerManager);
	}
	
	private void playTrack(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping songArg = event.getOption("song");
		
		//Check if the argument is present.
		if (songArg == null)
		{
			Message message = MessageUtils.buildErrorMessage("Music Player", author, "Devi inserire il parametro song.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Get voice state of the user
		GuildVoiceState authorVoiceState = MusicPlayerListener.getUserVoiceState(event);
		
		//Check if the user state is valid.
		if (authorVoiceState == null)
		{
			return;
		}
		
		//Get user voice channel.
		VoiceChannel voiceChannel = authorVoiceState.getChannel();
		
		//Get guild audio controller.
		GuildAudioController audioController = this.getGuildAudioController(guild.getId());
		
		//Create new audio controller for guild if there isn't an active one yet.
		if (audioController == null)
		{
			AudioPlayer player = MusicPlayerListener.playerManager.createPlayer();
			TrackScheduler scheduler = new TrackScheduler(player, guild, this);
			audioController = new GuildAudioController(player, scheduler, voiceChannel, channel);
			this.setGuildAudioController(guild.getId(), audioController);
		}
		else if (!audioController.getVoiceChannel().equals(voiceChannel)) //Check if the user voice channel is the same one of the bot.
		{
			Message message = MessageUtils.buildErrorMessage("Music Player", author, "Non sei nel canale vocale corretto. Vai dentro **#" + audioController.getVoiceChannel().getName() + "**.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if the argument is a YouTube URL.
		String song = songArg.getAsString();
		Pattern p = Pattern.compile(YOUTUBE_REGEX);
		Matcher m = p.matcher(song);
		
		if (!m.find())
		{
			Message message = MessageUtils.buildErrorMessage("Music Player", author, "Devi inserire un link di YouTube.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		logger.info("Caricamento traccia: " + song);
		
		//Load the song.
		MusicPlayerListener.playerManager.loadItem(song, new TrackLoadResultHandler(audioController, event, this));
	}
	
	private void skipTrack(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		//Get voice state of the user
		GuildVoiceState authorVoiceState = MusicPlayerListener.getUserVoiceState(event);
		
		//Check if the user state is valid.
		if (authorVoiceState == null)
		{
			return;
		}
		
		//Get user voice channel.
		VoiceChannel voiceChannel = authorVoiceState.getChannel();
		
		//Get guild audio controller.
		GuildAudioController audioController = this.getGuildAudioController(guild.getId());
		
		//Create new audio controller for guild if there isn't an active one yet.
		if (audioController == null)
		{
			Message message = MessageUtils.buildErrorMessage("Music Player", author, "Non sono nel canale vocale. Usa il comando `/" + this.getModulePrefix(guild.getId()) + " play` per riprodurre una traccia.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		else if (!audioController.getVoiceChannel().equals(voiceChannel)) //Check if the user voice channel is the same one of the bot.
		{
			Message message = MessageUtils.buildErrorMessage("Music Player", author, "Non sei nel canale vocale corretto. Vai dentro **#" + audioController.getVoiceChannel().getName() + "**.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		logger.info("Salto traccia.");
		
		boolean playingSong = audioController.getScheduler().nextTrack();
		
		//Check if there is a song playing.
		if (!playingSong)
		{
			Message message = MessageUtils.buildSimpleMessage("Music Player", author, "Playlist conclusa.");
			event.reply(message).setEphemeral(false).queue();
		}
		else
		{
			Message message = MessageUtils.buildSimpleMessage("Music player", author, "Traccia saltata.");
			event.reply(message).queue();
		}
	}
	
	private void stopPlayback(SlashCommandEvent event, Guild guild, MessageChannel channel, User author)
	{
		//Get voice state of the user
		GuildVoiceState authorVoiceState = MusicPlayerListener.getUserVoiceState(event);
		
		//Check if the user state is valid.
		if (authorVoiceState == null)
		{
			return;
		}
		
		//Get user voice channel.
		VoiceChannel voiceChannel = authorVoiceState.getChannel();
		
		//Get guild audio controller.
		GuildAudioController audioController = this.getGuildAudioController(guild.getId());
		
		//Create new audio controller for guild if there isn't an active one yet.
		if (audioController == null)
		{
			Message message = MessageUtils.buildErrorMessage("Music Player", author, "Non sono nel canale vocale. Usa il comando `/" + this.getModulePrefix(guild.getId()) + " play` per riprodurre una traccia.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		else if (!audioController.getVoiceChannel().equals(voiceChannel)) //Check if the user voice channel is the same one of the bot.
		{
			Message message = MessageUtils.buildErrorMessage("Music Player", author, "Non sei nel canale vocale corretto. Vai dentro **#" + audioController.getVoiceChannel().getName() + "**.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		logger.info("Stop playback");
		
		//Remove audio controller and quit the voice channel.
		this.closeConnection(guild);
		
		Message message = MessageUtils.buildSimpleMessage("Music Player", author, "Riproduzione interrotta.");
		event.reply(message).setEphemeral(false).queue();
		
		this.deleteTrackStatusMessage(guild, audioController);
	}
	
	@Override
	public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event)
	{
		//Check if the user that left the channel is not the bot.
		if (SerpensBot.getApi().getSelfUser().equals(event.getMember().getUser()))
			return;
		
		//Get audio controller.
		GuildAudioController audioController = this.getGuildAudioController(event.getGuild().getId());
		
		//Check if the bot is playing tracks.
		if (audioController == null)
			return;
		
		//Check if the channel the user left is the one the bot is playing tracks.
		if (!event.getChannelLeft().equals(audioController.getVoiceChannel()))
			return;
		
		//Remove the user votes and update the track status message.
		audioController.removeVote(event.getMember().getUser());
		MessageBuilder messageBuilder = new MessageBuilder(audioController.getStatusMessage());
		MusicPlayerListener.generateControlButtons(audioController, messageBuilder);
		audioController.getStatusMessage().editMessage(messageBuilder.build()).queue();
	}
	
	@Override
	public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent event)
	{
		//Get audio controller.
		GuildAudioController audioController = this.getGuildAudioController(event.getGuild().getId());
		
		//Check if the bot is playing tracks.
		if (audioController == null)
			return;
		
		//Check if the user that left the channel is the bot.
		if (SerpensBot.getApi().getSelfUser().equals(event.getMember().getUser()))
		{
			//Delete track status message and close the connection.
			this.deleteTrackStatusMessage(event.getGuild(), audioController);
			this.closeConnection(event.getGuild());
			return;
		}
		
		//Check if the channel the user left is the one the bot is playing tracks.
		if (!event.getChannelLeft().equals(audioController.getVoiceChannel()))
			return;
		
		//Remove the user votes and update the track status message.
		audioController.removeVote(event.getMember().getUser());
		MessageBuilder messageBuilder = new MessageBuilder(audioController.getStatusMessage());
		MusicPlayerListener.generateControlButtons(audioController, messageBuilder);
		audioController.getStatusMessage().editMessage(messageBuilder.build()).queue();
	}
	
	public void closeConnection(Guild guild)
	{
		AudioManager audioManager = guild.getAudioManager();
		
		//Remove the audio controller of the guild and quind the voice channel.
		this.removeGuildAudioController(guild.getId());
		audioManager.setSendingHandler(null);
		audioManager.closeAudioConnection();
	}
	
	@Override
	public void onQueueEmpty(Guild guild)
	{
		//Delete the status message.
		GuildAudioController audioController = this.getGuildAudioController(guild.getId());
		this.deleteTrackStatusMessage(guild, audioController);
		
		//Remove audio controller and quit the voice channel
		this.removeGuildAudioController(guild.getId());
		AudioManager audioManager = guild.getAudioManager();
		audioManager.setSendingHandler(null);
		audioManager.closeAudioConnection();
	}
	
	@Override
	public void onNewTrack(Guild guild)
	{
		//Get guild audio controller.
		GuildAudioController audioController = this.getGuildAudioController(guild.getId());
		AudioTrack track = audioController.getPlayer().getPlayingTrack();
		
		if (track != null)
		{
			//Join voice channel and start playining the song.
			AudioManager audioManager = guild.getAudioManager();
			
			if (!audioManager.isConnected())
			{
				audioManager.openAudioConnection(audioController.getVoiceChannel());
				audioManager.setSendingHandler(new AudioPlayerSendHandler(audioController.getPlayer()));
			}
			
			this.createNewTrackStatusMessage(guild, audioController);
			this.registerControlButtonsCallback(guild, audioController.getStatusMessage());
		}
		
		audioController.clearVotes();
	}
	
	@Override
	public void onTrackError(Guild guild)
	{
		GuildAudioController audioController = this.getGuildAudioController(guild.getId());
		
		if (audioController == null || audioController.getScheduler().isEmpty())
			this.closeConnection(guild);
	}
	
	@Override
	public void onLoadError(Guild guild)
	{
		GuildAudioController audioController = this.getGuildAudioController(guild.getId());
		
		//Check if the queue is empty.
		if (!audioController.getScheduler().isEmpty())
			return;
		
		//Delete track status message and close the connection.
		this.deleteTrackStatusMessage(guild, audioController);
		this.closeConnection(guild);
	}
	
	public static Message generateTrackStatusMessage(GuildAudioController audioController)
	{
		//Get track that is currently playing.
		AudioTrack track = audioController.getPlayer().getPlayingTrack();
		
		//Generate track status message.
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Music player")
				.setThumbnail("https://img.youtube.com/vi/" + track.getIdentifier() + "/hqdefault.jpg")
				.setDescription("Now playing:\n**" + track.getInfo().title + "**");
		
		MessageBuilder messageBuilder = new MessageBuilder();
		messageBuilder.setEmbeds(embedBuilder.build());
		
		//Add buttons to the message.
		MusicPlayerListener.generateControlButtons(audioController, messageBuilder);
		
		return messageBuilder.build();
	}
	
	public void createNewTrackStatusMessage(Guild guild, GuildAudioController audioController)
	{
		//Delete old track status message.
		this.deleteTrackStatusMessage(guild, audioController);
		
		//Generate and send new track status message.
		MessageChannel channel = audioController.getMessageChannel();
		Message statusMessage = channel.sendMessage(MusicPlayerListener.generateTrackStatusMessage(audioController)).complete();
		
		audioController.setStatusMessage(statusMessage);
	}
	
	public void deleteTrackStatusMessage(Guild guild, GuildAudioController audioController)
	{
		Message statusMessage = audioController.getStatusMessage();
		
		//Check if message exists.
		if (statusMessage == null)
			return;
		
		//Delete track status message and remove iteraction callbacks.
		statusMessage.delete().queue();
		this.removeInteractionGroup(guild.getId(), audioController.getMessageChannel().getId());
	}
	
	private static void generateControlButtons(GuildAudioController audioController, MessageBuilder messageBuilder)
	{
		//Get voice channel users.
		VoiceChannel voiceChannel = audioController.getVoiceChannel();
		HashSet<User> voiceChannelMembers = voiceChannel.getMembers().stream().map(Member::getUser).collect(Collectors.toCollection(HashSet::new));
		
		//Get users count.
		voiceChannelMembers.remove(SerpensBot.getApi().getSelfUser());
		int count = voiceChannelMembers.size(); //Number of user that joined the voice channel minus bot.
		
		Button skipButton = Button.primary("skip", "Skip (" + audioController.getSkipVotes().getVoteCount() + "/" + count + ")");
		Button stopButton = Button.danger("stop", "Stop (" + audioController.getStopVotes().getVoteCount() + "/" + count + ")");
		Button showQueue = Button.secondary("show-queue", "Show queue");
		messageBuilder.setActionRows(ActionRow.of(skipButton, stopButton, showQueue));
	}
	
	private void registerControlButtonsCallback(Guild guild, Message message)
	{
		InteractionGroup interactionGroup = new InteractionGroup();
		
		ButtonAction skipButtonAction = (event, guild1, channel, statusMessage, author) ->
		{
			event.deferEdit().queue();
			
			GuildAudioController audioController = this.getGuildAudioController(guild1.getId());
			VoiceChannel voiceChannel = audioController.getVoiceChannel();
			
			//Check if the user is in the correct voice channel.
			if (!voiceChannel.getMembers().contains(event.getMember()))
				return InteractionCallback.LEAVE_MESSAGE;
			
			//Add user vote.
			audioController.getSkipVotes().addVote(author);
			
			//Get voice channel users.
			HashSet<User> voiceChannelMembers = voiceChannel.getMembers().stream().map(Member::getUser).collect(Collectors.toCollection(HashSet::new));
			
			//Get users count.
			voiceChannelMembers.remove(SerpensBot.getApi().getSelfUser());
			int count = voiceChannelMembers.size(); //Number of user that joined the voice channel minus bot.
			
			//If vote counts are greater than half skip the song.
			if (audioController.getSkipVotes().getVoteCount() > count / 2)
			{
				audioController.clearVotes();
				audioController.getScheduler().nextTrack();
			}
			else
			{
				statusMessage.editMessage(MusicPlayerListener.generateTrackStatusMessage(audioController)).queue();
			}
			
			return InteractionCallback.LEAVE_MESSAGE;
		};
		interactionGroup.addButtonCallback("skip", skipButtonAction);
		
		ButtonAction stopButtonAction = (event, guild1, channel, statusMessage, author) ->
		{
			event.deferEdit().queue();
			
			GuildAudioController audioController = this.getGuildAudioController(guild1.getId());
			VoiceChannel voiceChannel = audioController.getVoiceChannel();
			
			//Check if the user is in the correct voice channel.
			if (!voiceChannel.getMembers().contains(event.getMember()))
				return InteractionCallback.LEAVE_MESSAGE;
			
			//Add user vote.
			audioController.getStopVotes().addVote(author);
			
			//Get voice channel users.
			HashSet<User> voiceChannelMembers = voiceChannel.getMembers().stream().map(Member::getUser).collect(Collectors.toCollection(HashSet::new));
			
			//Get users count.
			voiceChannelMembers.remove(SerpensBot.getApi().getSelfUser());
			int count = voiceChannelMembers.size(); //Number of user that joined the voice channel minus bot.
			
			//If vote counts are greater than half skip the song.
			if (audioController.getStopVotes().getVoteCount() > count / 2)
			{
				this.deleteTrackStatusMessage(guild1, audioController);
				this.closeConnection(guild1);
			}
			else
			{
				statusMessage.editMessage(MusicPlayerListener.generateTrackStatusMessage(audioController)).queue();
			}
			
			return InteractionCallback.LEAVE_MESSAGE;
		};
		interactionGroup.addButtonCallback("stop", stopButtonAction);
		
		ButtonAction showQueueButtonAction = (event, guild1, channel, message1, author) ->
		{
			event.deferReply(true).queue();
			
			GuildAudioController audioController = this.getGuildAudioController(guild1.getId());
			AudioTrack playingTrack = audioController.getPlayer().getPlayingTrack();
			
			//Generate track status message.
			EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Music player")
					.setThumbnail("https://img.youtube.com/vi/" + playingTrack.getIdentifier() + "/hqdefault.jpg")
					.setDescription("Now playing:\n**" + playingTrack.getInfo().title + "**");
			
			ArrayList<AudioTrack> tracks = audioController.getScheduler().getTrackQueue();
			
			StringBuilder numberField = new StringBuilder();
			StringBuilder titleField = new StringBuilder();
			
			//Add list of track to the embed.
			int i = 0;
			for (AudioTrack track : tracks)
			{
				String title = track.getInfo().title;
				if (titleField.length() < 900 && numberField.length() < 900)
				{
					numberField.append((i + 1) + ".\n");
					titleField.append("**" + title + "**\n");
				}
				else
				{
					titleField.append("...altre " + (tracks.size() - i) + " tracce.");
					break;
				}
				i++;
			}
			
			embedBuilder.addField("", numberField.toString(), true);
			embedBuilder.addField("", titleField.toString(), true);
			
			MessageBuilder messageBuilder = new MessageBuilder();
			messageBuilder.setEmbeds(embedBuilder.build());
			
			event.getHook().editOriginal(messageBuilder.build()).queue();
			
			return InteractionCallback.LEAVE_MESSAGE;
		};
		interactionGroup.addButtonCallback("show-queue", showQueueButtonAction);
		
		this.addInteractionGroup(guild.getId(), message.getId(), interactionGroup);
	}
	
	private static GuildVoiceState getUserVoiceState(SlashCommandEvent event)
	{
		Member authorMember = event.getMember();
		User author = event.getUser();
		
		//Check if the member was correctly retrieved.
		if (authorMember == null)
		{
			Message message = MessageUtils.buildErrorMessage("Music Player", author, "Utente non trovato.");
			event.reply(message).setEphemeral(true).queue();
			return null;
		}
		
		//Check voice state for the user.
		GuildVoiceState authorVoiceState = authorMember.getVoiceState();
		if (authorVoiceState == null)
		{
			Message message = MessageUtils.buildErrorMessage("Music Player", author, "Impossibile ottenere lo stato del utente.");
			event.reply(message).setEphemeral(true).queue();
			return null;
		}
		
		if (!authorVoiceState.inVoiceChannel())
		{
			Message message = MessageUtils.buildErrorMessage("Music Player", author, "Devi essere in un canale vocale per usare questo comando.");
			event.reply(message).setEphemeral(true).queue();
			return null;
		}
		
		return authorVoiceState;
	}
	
	private GuildAudioController getGuildAudioController(String guildID)
	{
		return this.activeAudioController.get(guildID);
	}
	
	private void setGuildAudioController(String guildID, GuildAudioController guildAudioController)
	{
		this.activeAudioController.put(guildID, guildAudioController);
	}
	
	private void removeGuildAudioController(String guildID)
	{
		this.activeAudioController.remove(guildID);
	}
}
