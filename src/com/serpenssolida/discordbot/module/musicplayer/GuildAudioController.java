package com.serpenssolida.discordbot.module.musicplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.entities.*;

public class GuildAudioController
{
	private final AudioPlayer player;
	private final TrackScheduler scheduler;
	private final AudioChannel voiceChannel;
	private final MessageChannel messageChannel;
	
	private Message statusMessage;
	private final MusicVote skipVotes = new MusicVote();
	private final MusicVote stopVotes = new MusicVote();
	
	public GuildAudioController(AudioPlayer player, TrackScheduler scheduler, AudioChannel voiceChannel, MessageChannel messageChannel)
	{
		this.player = player;
		this.scheduler = scheduler;
		this.voiceChannel = voiceChannel;
		this.messageChannel = messageChannel;
		
		this.player.addListener(scheduler);
	}
	
	public AudioPlayer getPlayer()
	{
		return this.player;
	}
	
	public TrackScheduler getScheduler()
	{
		return this.scheduler;
	}
	
	public AudioChannel getVoiceChannel()
	{
		return this.voiceChannel;
	}
	
	public MessageChannel getMessageChannel()
	{
		return this.messageChannel;
	}
	
	public Message getStatusMessage()
	{
		return this.statusMessage;
	}
	
	public void setStatusMessage(Message statusMessage)
	{
		this.statusMessage = statusMessage;
	}
	
	public MusicVote getSkipVotes()
	{
		return this.skipVotes;
	}
	
	public MusicVote getStopVotes()
	{
		return this.stopVotes;
	}
	
	public void clearVotes()
	{
		this.skipVotes.clearVotes();
		this.stopVotes.clearVotes();
	}
	
	public void removeVote(User user)
	{
		this.skipVotes.removeVote(user);
		this.stopVotes.removeVote(user);
	}
}
