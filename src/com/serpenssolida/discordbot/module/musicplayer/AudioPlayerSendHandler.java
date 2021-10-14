package com.serpenssolida.discordbot.module.musicplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import java.nio.ByteBuffer;

public class AudioPlayerSendHandler implements AudioSendHandler
{
	private final AudioPlayer audioPlayer;
	private AudioFrame lastFrame;
	
	public AudioPlayerSendHandler(AudioPlayer audioPlayer)
	{
		this.audioPlayer = audioPlayer;
	}
	
	@Override
	public boolean canProvide()
	{
		this.lastFrame = this.audioPlayer.provide();
		return this.lastFrame != null;
	}
	
	@Override
	public ByteBuffer provide20MsAudio()
	{
		return ByteBuffer.wrap(this.lastFrame.getData());
	}
	
	@Override
	public boolean isOpus()
	{
		return true;
	}
}