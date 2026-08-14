package com.serpenssolida.discordbot.module.voicelogger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class VoiceLoggerController
{
	private static VoiceLoggerController instance;
	private static final Logger logger = LoggerFactory.getLogger(VoiceLoggerController.class);

	private final Map<String, VoiceLog> voiceLoggers = new HashMap<>();

	public static VoiceLoggerController getInstance()
	{
		if (VoiceLoggerController.instance == null)
			VoiceLoggerController.instance = new VoiceLoggerController();

		return VoiceLoggerController.instance;
	}

	public boolean deleteVoiceLog(String guildID)
	{
		VoiceLog removed = this.voiceLoggers.remove(guildID);
		this.saveVoiceLogs();
		return removed != null;
	}

	public void addVoiceLog(String guildID, VoiceLog voiceLog)
	{
		this.voiceLoggers.put(guildID, voiceLog);
		this.saveVoiceLogs();
	}

	public boolean shouldLogVoiceUpdate(String guildID)
	{
		return this.voiceLoggers.containsKey(guildID);
	}

	public VoiceLog getVoiceLog(String guildID)
	{
		return this.voiceLoggers.get(guildID);
	}

	/**
	 * Loads voice log data.
	 */
	public void loadVoiceLogs()
	{
		File fileCharacters = new File(Paths.get("global_data", "voicelog",  "voicelog.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		logger.info("Cariamento dei voice log.");

		//Load data from file.
		try (BufferedReader reader = new BufferedReader(new FileReader(fileCharacters)))
		{
			VoiceLogData voiceLogData = gson.fromJson(reader, VoiceLogData.class);
			this.voiceLoggers.putAll(voiceLogData.getReminders());
		}
		catch (FileNotFoundException e)
		{
			logger.info("File dati dei voice log non trovato.");
		}
		catch (IOException e)
		{
			logger.error("", e);
		}
	}

	/**
	 * Save voice log data.
	 */
	public void saveVoiceLogs()
	{
		File voiceLogFile = new File(Paths.get("global_data", "voicelog",  "voicelog.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		logger.info("Salvataggio/aggiornamento dei voice log.");

		//Save data to file.
		try (PrintWriter writer = new PrintWriter(new FileWriter(voiceLogFile)))
		{
			VoiceLogData voiceLogData = new VoiceLogData(this.voiceLoggers);
			writer.println(gson.toJson(voiceLogData));
		}
		catch (FileNotFoundException e)
		{
			try
			{
				voiceLogFile.getParentFile().mkdirs();

				if (voiceLogFile.createNewFile())
					this.saveVoiceLogs();
			}
			catch (IOException ex)
			{
				logger.error("", ex);
			}
		}
		catch (IOException e)
		{
			logger.error("", e);
		}
	}
}
