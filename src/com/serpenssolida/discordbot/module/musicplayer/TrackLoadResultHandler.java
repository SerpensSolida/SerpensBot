package com.serpenssolida.discordbot.module.musicplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.serpenssolida.discordbot.MessageUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TrackLoadResultHandler implements AudioLoadResultHandler
{
	private static final Logger logger = LoggerFactory.getLogger(TrackLoadResultHandler.class);
	private final GuildAudioController audioController;
	private final SlashCommandEvent event;
	private final TrackEventHandler listener;
	
	public TrackLoadResultHandler(GuildAudioController audioController, SlashCommandEvent event, MusicPlayerListener listener)
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
		
		this.event.reply(new MessageBuilder().setEmbeds(embedBuilder.build()).build()).setEphemeral(false).queue();
		
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
				.setDescription("Playlist aggiunta alla coda: **" + playlist.getName() + "**.\n")
				.appendDescription("\n**Lista tracce**:");
		
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
		
		this.event.reply(new MessageBuilder().setEmbeds(embedBuilder.build()).build()).setEphemeral(false).queue();
		
		//Add tracks to the queue.
		for (AudioTrack track : tracks)
			this.audioController.getScheduler().queue(track);
	}
	
	@Override
	public void noMatches()
	{
		Message message = MessageUtils.buildErrorMessage("Music Player", this.event.getUser(), "Traccia non trovata.");
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
		Message message = MessageUtils.buildErrorMessage("Music Player", this.event.getUser(), "Si è verificato un errore durante il caricamento della traccia: *" + exception.getMessage() + "*");
		this.event.reply(message).setEphemeral(true).queue();
	}
}
