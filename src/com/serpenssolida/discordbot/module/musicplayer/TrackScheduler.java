package com.serpenssolida.discordbot.module.musicplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.Guild;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackScheduler extends AudioEventAdapter
{
	private final AudioPlayer player;
	private final BlockingQueue<AudioTrack> queue;
	private final Guild guild;
	private final TrackEventHandler listener;
	
	/**
	 * @param player
	 * 		The audio player this scheduler uses.
	 * @param guild
	 * 		The guild the track scheduler will be linked to.
	 * @param listener
	 */
	public TrackScheduler(AudioPlayer player, Guild guild, TrackEventHandler listener)
	{
		this.player = player;
		this.guild = guild;
		this.listener = listener;
		this.queue = new LinkedBlockingQueue<>();
	}
	
	/**
	 * Add the next track to queue or play right away if nothing is in the queue.
	 *
	 * @param track
	 * 		The track to play or add to queue.
	 */
	public void queue(AudioTrack track)
	{
		//Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
		//something is playing, it returns false and does nothing. In that case the player was already playing so this
		//track goes to the queue instead.
		if (!this.player.startTrack(track, true))
			this.queue.offer(track);
		else
			this.listener.onNewTrack(this.guild);
	}
	
	/**
	 * Start the next track, stopping the current one if it is playing.
	 */
	public boolean nextTrack()
	{
		//Get the track from the queue.
		AudioTrack track = this.queue.poll();
		
		//Start playing the track.
		boolean playing =  this.player.startTrack(track, false);
		
		//Check if the queue returned a track.
		if (track == null)
			this.listener.onQueueEmpty(this.guild);
		else
			this.listener.onNewTrack(this.guild);
		
		return playing;
	}
	
	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason)
	{
		//Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
		if (endReason.mayStartNext)
			this.nextTrack();
	}
	
	@Override
	public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception)
	{
		//Notify the handler the track raised an error.
		this.listener.onTrackError(this.guild);
		
		//If the queue is empty notify it to the handler.
		if (this.isEmpty())
			this.listener.onQueueEmpty(this.guild);
	}
	
	public List<AudioTrack> getTrackQueue()
	{
		return new ArrayList<>(this.queue);
	}
	
	public AudioPlayer getPlayer()
	{
		return this.player;
	}
	
	public Guild getGuild()
	{
		return this.guild;
	}
	
	public boolean isEmpty()
	{
		return this.queue.isEmpty();
	}
}