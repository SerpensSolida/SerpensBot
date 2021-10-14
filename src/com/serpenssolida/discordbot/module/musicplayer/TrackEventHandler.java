package com.serpenssolida.discordbot.module.musicplayer;

import net.dv8tion.jda.api.entities.Guild;

public interface TrackEventHandler
{
	void onQueueEmpty(Guild guild);
	
	void onNewTrack(Guild guild);
	
	void onTrackError(Guild guild);
	
	void onLoadError(Guild guild);
}
