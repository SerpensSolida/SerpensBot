package com.serpenssolida.discordbot.module.musicplayer;

import net.dv8tion.jda.api.entities.Guild;

public interface TrackEventHandler
{
	/**
	 * Called when the queue is emptied.
	 *
	 * @param guild
	 * 		The {@link Guild} that triggered the event.
	 */
	void onQueueEmpty(Guild guild);
	
	/**
	 * Called when a new track began playing.
	 *
	 * @param guild
	 * 		The {@link Guild} that triggered the event.
	 */
	void onNewTrack(Guild guild);
	
	/**
	 * Called when a track triggers an error.
	 *
	 * @param guild
	 * 		The {@link Guild} that triggered the event.
	 */
	void onTrackError(Guild guild);
	
	/**
	 * Called when a track fails to load.
	 *
	 * @param guild
	 * 		The {@link Guild} that triggered the event.
	 */
	void onLoadError(Guild guild);
}
