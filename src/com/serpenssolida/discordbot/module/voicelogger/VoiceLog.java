package com.serpenssolida.discordbot.module.voicelogger;

public class VoiceLog
{
	private String channel;

	public VoiceLog(String channel)
	{
		this.channel = channel;
	}

	public String getChannel()
	{
		return channel;
	}

	public void setChannel(String channel)
	{
		this.channel = channel;
	}
}
