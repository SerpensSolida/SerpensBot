package com.serpenssolida.discordbot.module.voicelogger;

import java.util.HashMap;
import java.util.Map;

public class VoiceLogData
{
	private Map<String, VoiceLog> voiceLogs = new HashMap<>();

	public VoiceLogData(Map<String, VoiceLog> voiceLogs)
	{
		this.voiceLogs.putAll(voiceLogs);
	}

	public Map<String, VoiceLog> getReminders()
	{
		return this.voiceLogs;
	}

	public void setReminders(Map<String, VoiceLog> voiceLogs)
	{
		this.voiceLogs = voiceLogs;
	}
}
