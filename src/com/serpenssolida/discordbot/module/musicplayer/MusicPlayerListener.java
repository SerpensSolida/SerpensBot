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
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
		
		//Command for adding a song to the queue.
		BotCommand command = new BotCommand("play", "Agginunge alla coda una traccia.");
		command.getCommandData()
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

		playerManager.registerSourceManager(new YoutubeAudioSourceManager());
		AudioSourceManagers.registerRemoteSources(MusicPlayerListener.playerManager);
		AudioSourceManagers.registerRemoteSources(playerManager, com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class);


	}
	
	/**
	 * Callback for "play" command.
	 */
	private void playTrack(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
	{
		OptionMapping songArg = event.getOption("song");
		
		//Check if the argument is present.
		if (songArg == null)
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Music Player", author, "Devi inserire il parametro song.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Get voice state of the user
		GuildVoiceState authorVoiceState = MusicPlayerListener.getUserVoiceState(event);
		
		//Check if the user state is valid.
		if (authorVoiceState == null)
			return;
		
		//Get user voice channel.
		AudioChannel voiceChannel = authorVoiceState.getChannel();
		
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
			MessageCreateData message = MessageUtils.buildErrorMessage("Music Player", author, "Non sei nel canale vocale corretto. Vai dentro **#" + audioController.getVoiceChannel().getName() + "**.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		//Check if the argument is a YouTube URL.
		String song = songArg.getAsString();
		Pattern p = Pattern.compile(YOUTUBE_REGEX);
		Matcher m = p.matcher(song);
		
		if (!m.find())
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Music Player", author, "Devi inserire un link di YouTube.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		logger.info("Caricamento traccia: " + song);
		
		//Load the song.
		MusicPlayerListener.playerManager.loadItem(song, new TrackLoadResultHandler(audioController, event, this));
	}
	
	/**
	 * Callback for "skip" command.
	 */
	private void skipTrack(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
	{
		//Get voice state of the user
		GuildVoiceState authorVoiceState = MusicPlayerListener.getUserVoiceState(event);
		
		//Check if the user state is valid.
		if (authorVoiceState == null)
		{
			return;
		}
		
		//Get user voice channel.
		AudioChannel voiceChannel = authorVoiceState.getChannel();
		
		//Get guild audio controller.
		GuildAudioController audioController = this.getGuildAudioController(guild.getId());
		
		//Create new audio controller for guild if there isn't an active one yet.
		if (audioController == null)
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Music Player", author, "Non sono nel canale vocale. Usa il comando `/" + this.getModulePrefix(guild.getId()) + " play` per riprodurre una traccia.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		else if (!audioController.getVoiceChannel().equals(voiceChannel)) //Check if the user voice channel is the same one of the bot.
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Music Player", author, "Non sei nel canale vocale corretto. Vai dentro **#" + audioController.getVoiceChannel().getName() + "**.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		logger.info("Salto traccia.");
		
		boolean playingSong = audioController.getScheduler().nextTrack();
		
		//Check if there is a song playing.
		if (!playingSong)
		{
			MessageCreateData message = MessageUtils.buildSimpleMessage("Music Player", author, "Playlist conclusa.");
			event.reply(message).setEphemeral(false).queue();
		}
		else
		{
			MessageCreateData message = MessageUtils.buildSimpleMessage("Music player", author, "Traccia saltata.");
			event.reply(message).queue();
		}
	}
	
	/**
	 * Callback for "stop" command.
	 */
	private void stopPlayback(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
	{
		//Get voice state of the user
		GuildVoiceState authorVoiceState = MusicPlayerListener.getUserVoiceState(event);
		
		//Check if the user state is valid.
		if (authorVoiceState == null)
			return;
		
		//Get user voice channel.
		AudioChannel voiceChannel = authorVoiceState.getChannel();
		
		//Get guild audio controller.
		GuildAudioController audioController = this.getGuildAudioController(guild.getId());
		
		//Create new audio controller for guild if there isn't an active one yet.
		if (audioController == null)
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Music Player", author, "Non sono nel canale vocale. Usa il comando `/" + this.getModulePrefix(guild.getId()) + " play` per riprodurre una traccia.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		else if (!audioController.getVoiceChannel().equals(voiceChannel)) //Check if the user voice channel is the same one of the bot.
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Music Player", author, "Non sei nel canale vocale corretto. Vai dentro **#" + audioController.getVoiceChannel().getName() + "**.");
			event.reply(message).setEphemeral(true).queue();
			return;
		}
		
		logger.info("Stop playback");
		
		//Remove audio controller and quit the voice channel.
		this.closeConnection(guild);
		
		MessageCreateData message = MessageUtils.buildSimpleMessage("Music Player", author, "Riproduzione interrotta.");
		event.reply(message).setEphemeral(false).queue();
		
		this.deleteTrackStatusMessage(guild, audioController);
	}
	
	@Override
	public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event)
	{
		if (event.getChannelLeft() != null)
			this.onVoiceLeave(event);
		
		if (event.getChannelJoined() != null)
			this.onVoiceJoin(event);
		
	}
	
	private void onVoiceLeave(GuildVoiceUpdateEvent event)
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
		if (!event.getChannelLeft().equals(audioController.getVoiceChannel()) && !event.getChannelJoined().equals(audioController.getVoiceChannel()))
			return;
		
		//Remove the user votes and update the track status message.
		audioController.removeVote(event.getMember().getUser());
		MessageEditBuilder messageBuilder = MessageEditBuilder.from(MessageEditData.fromMessage(audioController.getStatusMessage()));
		MusicPlayerListener.generateControlButtons(audioController, messageBuilder);
		audioController.getStatusMessage().editMessage(messageBuilder.build()).queue();
		
		//Check if all user have left the channel.
		if (event.getChannelLeft().asVoiceChannel().getMembers().size() > 1)
			return;
		
		//Remove audio controller and quit the voice channel.
		this.closeConnection(event.getGuild());
		this.deleteTrackStatusMessage(event.getGuild(), audioController);
	}
	
	private void onVoiceJoin(GuildVoiceUpdateEvent event)
	{
		//Check if the user that joined the channel is the bot.
		if (SerpensBot.getApi().getSelfUser().equals(event.getMember().getUser()))
			return;
		
		//Get audio controller.
		GuildAudioController audioController = this.getGuildAudioController(event.getGuild().getId());
		
		//Check if the bot is playing tracks.
		if (audioController == null)
			return;
		
		//Check if the channel the user joined is the one the bot is playing tracks.
		if (!event.getChannelJoined().equals(audioController.getVoiceChannel()))
			return;
		
		//Update the track status message.
		MessageEditBuilder messageBuilder = MessageEditBuilder.from(MessageEditData.fromMessage(audioController.getStatusMessage()));
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
	
	/**
	 * Generate the status message of the music player.
	 *
	 * @param audioController
	 * 		The {@link GuildAudioController} containing the status to be reppresented inside the message.
	 *
	 * @return The message containing the status of the given {@link GuildAudioController}.
	 */
	public static MessageEditData generateTrackStatusMessage(GuildAudioController audioController)
	{
		//Get track that is currently playing.
		AudioTrack track = audioController.getPlayer().getPlayingTrack();
		
		//Generate track status message.
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Music player")
				.setThumbnail("https://img.youtube.com/vi/" + track.getIdentifier() + "/hqdefault.jpg")
				.setDescription("Now playing:\n**" + track.getInfo().title + "**");
		
		MessageEditBuilder messageBuilder = new MessageEditBuilder();
		messageBuilder.setEmbeds(embedBuilder.build());
		
		//Add buttons to the message.
		MusicPlayerListener.generateControlButtons(audioController, messageBuilder);
		
		return messageBuilder.build();
	}
	
	/**
	 * Delete the old status message and create a new status message.
	 *
	 * @param guild
	 * 		{@link Guild} the status message is in.
	 * @param audioController
	 * 		The {@link GuildAudioController} containing the status of the music player.
	 */
	public void createNewTrackStatusMessage(Guild guild, GuildAudioController audioController)
	{
		//Delete old track status message.
		this.deleteTrackStatusMessage(guild, audioController);
		
		//Generate and send new track status message.
		MessageChannel channel = audioController.getMessageChannel();
		Message statusMessage = channel.sendMessage(MessageCreateData.fromEditData(MusicPlayerListener.generateTrackStatusMessage(audioController))).complete();
		
		audioController.setStatusMessage(statusMessage);
	}
	
	/**
	 * Delete the current track status message of the given {@link GuildAudioController} and remove its interaction group.
	 *
	 * @param guild
	 * 		The {@link Guild} the message is in.
	 * @param audioController
	 * 		The {@link GuildAudioController} that owns the status message.
	 */
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
	
	/**
	 * Adds the control buttons (play, skip, stop) to the given {@link MessageCreateBuilder}.
	 *
	 * @param audioController
	 * 		The {@link GuildAudioController} containing the status of the player.
	 * @param messageBuilder
	 * 		The {@link MessageCreateBuilder} to add control buttons to.
	 */
	private static void generateControlButtons(GuildAudioController audioController, MessageEditBuilder messageBuilder)
	{
		//Get voice channel users.
		AudioChannel voiceChannel = audioController.getVoiceChannel();
		HashSet<User> voiceChannelMembers = voiceChannel.getMembers().stream().map(Member::getUser).collect(Collectors.toCollection(HashSet::new));
		
		//Get users count.
		voiceChannelMembers.remove(SerpensBot.getApi().getSelfUser());
		int count = voiceChannelMembers.size(); //Number of user that joined the voice channel minus bot.
		
		String skipLabel = "Skip (" + audioController.getSkipVotes().getVoteCount() + "/" + (count / 2 + 1) + ")";
		String stopLabel = "Stop (" + audioController.getStopVotes().getVoteCount() + "/" + (count / 2 + 1) + ")";
		
		//Don't show votes is there is only one user in voice channel.
		if (count <= 1)
		{
			skipLabel = "Skip";
			stopLabel = "Stop";
		}
		
		Button skipButton = Button.primary("skip", skipLabel);
		Button stopButton = Button.danger("stop", stopLabel);
		Button showQueue = Button.secondary("show-queue", "Show queue");
		messageBuilder.setComponents(ActionRow.of(skipButton, stopButton, showQueue));
	}
	
	/**
	 * Register the control button callbacks.
	 *
	 * @param guild
	 * 		The {@link Guild} that the message is in.
	 * @param message
	 * 		The {@link Message} that owns the buttons.
	 */
	private void registerControlButtonsCallback(Guild guild, Message message)
	{
		InteractionGroup interactionGroup = new InteractionGroup();
		
		ButtonAction skipButtonAction = (event, guild1, channel, statusMessage, author) ->
		{
			event.deferEdit().queue();
			
			GuildAudioController audioController = this.getGuildAudioController(guild1.getId());
			AudioChannel voiceChannel = audioController.getVoiceChannel();
			
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
			AudioChannel voiceChannel = audioController.getVoiceChannel();
			
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
			
			//Generate playlist text.
			List<AudioTrack> tracks = audioController.getScheduler().getTrackQueue();
			ImmutablePair<String, String> fields = MusicPlayerListener.generateEmbedPlaylistFields(tracks);
			embedBuilder.addField("N.", fields.getRight(), true);
			embedBuilder.addField("Titolo", fields.getLeft(), true);
			
			MessageEditBuilder messageBuilder = new MessageEditBuilder();
			messageBuilder.setEmbeds(embedBuilder.build());
			
			event.getHook().editOriginal(messageBuilder.build()).queue();
			
			return InteractionCallback.LEAVE_MESSAGE;
		};
		interactionGroup.addButtonCallback("show-queue", showQueueButtonAction);
		
		this.addInteractionGroup(guild.getId(), message.getId(), interactionGroup);
	}
	
	/**
	 * Generate the fields for the playlist embed.
	 *
	 * @param tracks
	 * 		The list of tracks in the playlist.
	 *
	 * @return
	 * 		An {@link ImmutablePair} containing the fields of the playlist.
	 */
	protected static ImmutablePair<String, String> generateEmbedPlaylistFields(List<AudioTrack> tracks)
	{
		//Create message.
		StringBuilder numberField = new StringBuilder();
		StringBuilder titleField = new StringBuilder();
		
		//Add list of track to the embed.
		int i = 0;
		for (AudioTrack track : tracks)
		{
			String title = track.getInfo().title;
			
			if (title.length() > 50)
				title = title.substring(0, 50) + "...";
			
			if (titleField.length() < 900 && numberField.length() < 900)
			{
				numberField.append((i + 1) + ".\n");
				titleField.append("*" + title + "*\n");
			}
			else
			{
				titleField.append("...altre " + (tracks.size() - i) + " tracce.");
				break;
			}
			i++;
		}
		
		return new ImmutablePair<>(titleField.toString(), numberField.toString());
	}
	
	/**
	 * Get the state of the user.
	 *
	 * @param event
	 * 		The {@link SlashCommandInteractionEvent} containing the author of the event.
	 *
	 * @return
	 * 		The {@link GuildVoiceState} of the user.
	 */
	private static GuildVoiceState getUserVoiceState(SlashCommandInteractionEvent event)
	{
		Member authorMember = event.getMember();
		User author = event.getUser();
		
		//Check if the member was correctly retrieved.
		if (authorMember == null)
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Music Player", author, "Utente non trovato.");
			event.reply(message).setEphemeral(true).queue();
			return null;
		}
		
		//Check voice state for the user.
		GuildVoiceState authorVoiceState = authorMember.getVoiceState();
		if (authorVoiceState == null)
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Music Player", author, "Impossibile ottenere lo stato del utente.");
			event.reply(message).setEphemeral(true).queue();
			return null;
		}
		
		if (!authorVoiceState.inAudioChannel())
		{
			MessageCreateData message = MessageUtils.buildErrorMessage("Music Player", author, "Devi essere in un canale vocale per usare questo comando.");
			event.reply(message).setEphemeral(true).queue();
			return null;
		}
		
		return authorVoiceState;
	}
	
	/**
	 * Returns {@link GuildAudioController} of the given guild.
	 *
	 * @param guildID
	 * 		The id of the guild that owns the controller.
	 *
	 * @return
	 * 		The {@link GuildAudioController} of the guild.
	 */
	private GuildAudioController getGuildAudioController(String guildID)
	{
		return this.activeAudioController.get(guildID);
	}
	
	/**
	 * Set the {@link GuildAudioController} of the given guild.
	 *
	 * @param guildID
	 * 		The id of the {@link Guild} that the {@link GuildAudioController} will be assigned to.
	 * @param guildAudioController
	 * 		The {@link GuildAudioController} that will be assigned to the given guild.
	 */
	private void setGuildAudioController(String guildID, GuildAudioController guildAudioController)
	{
		this.activeAudioController.put(guildID, guildAudioController);
	}
	
	/**
	 * Remove the {@link GuildAudioController} from the given guild.
	 *
	 * @param guildID
	 * 		The id of the {@link Guild}.
	 */
	private void removeGuildAudioController(String guildID)
	{
		this.activeAudioController.remove(guildID);
	}
}
