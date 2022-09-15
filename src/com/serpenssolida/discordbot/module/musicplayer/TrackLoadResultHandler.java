package com.serpenssolida.discordbot.module.musicplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.serpenssolida.discordbot.MessageUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TrackLoadResultHandler implements AudioLoadResultHandler
{
	private static final Logger logger = LoggerFactory.getLogger(TrackLoadResultHandler.class);
	private final GuildAudioController audioController;
	private final SlashCommandInteractionEvent event;
	private final TrackEventHandler listener;
	
	public TrackLoadResultHandler(GuildAudioController audioController, SlashCommandInteractionEvent event, TrackEventHandler listener)
	{
		this.audioController = audioController;
		this.event = event;
		this.listener = listener;
	}
	
	@Override
	public void trackLoaded(AudioTrack track)
	{
		//Create the message.
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Music player", this.event.getUser())
				.setThumbnail("https://img.youtube.com/vi/" + track.getIdentifier() + "/hqdefault.jpg")
				.setDescription("Traccia aggiunta alla coda: **" + track.getInfo().title + "**.");
		
		this.event.reply(new MessageCreateBuilder().setEmbeds(embedBuilder.build()).build()).setEphemeral(false).queue();
		
		//Add the track to the queue.
		this.audioController.getScheduler().queue(track);
	}
	
	@Override
	public void playlistLoaded(AudioPlaylist playlist)
	{
		List<AudioTrack> tracks = playlist.getTracks();
		
		//Create message.
		EmbedBuilder embedBuilder = MessageUtils.getDefaultEmbed("Music player", this.event.getUser())
				.setThumbnail("https://img.youtube.com/vi/" + playlist.getSelectedTrack().getIdentifier() + "/hqdefault.jpg")
				.setDescription("Playlist aggiunta alla coda: *" + playlist.getName() + "*.\n")
				.appendDescription("\n**Lista tracce**:");
		
		//Generate playlist text.
		ImmutablePair<String, String> fields = MusicPlayerListener.generateEmbedPlaylistFields(tracks);
		embedBuilder.addField("N.", fields.getRight(), true);
		embedBuilder.addField("Titolo", fields.getLeft(), true);
		
		this.event.reply(new MessageCreateBuilder().setEmbeds(embedBuilder.build()).build()).setEphemeral(false).queue();
		
		//Add tracks to the queue.
		for (AudioTrack track : tracks)
			this.audioController.getScheduler().queue(track);
	}
	
	@Override
	public void noMatches()
	{
		MessageCreateData message = MessageUtils.buildErrorMessage("Music Player", this.event.getUser(), "Traccia non trovata.");
		this.event.reply(message).setEphemeral(true).queue();
	}
	
	@Override
	public void loadFailed(FriendlyException exception)
	{
		//Notify the handler the track raised an error.
		this.listener.onLoadError(this.event.getGuild());
		
		//If the queue is empty notify it to the handler.
		if (this.audioController.getScheduler().isEmpty())
			this.listener.onQueueEmpty(this.event.getGuild());
		
		logger.error(exception.getMessage());
		MessageCreateData message = MessageUtils.buildErrorMessage("Music Player", this.event.getUser(), "Si è verificato un errore durante il caricamento della traccia: *" + exception.getMessage() + "*");
		this.event.reply(message).setEphemeral(true).queue();
	}
}
