package com.serpenssolida.discordbot.module.connect4;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;

public class Connect4Controller
{
	private static final HashMap<String, Connect4Controller> instance = new HashMap<>(); //Singleton data.
	private static final String FOLDER = "connect4";
	
	private static final Logger logger = LoggerFactory.getLogger(Connect4Controller.class);
	
	private Connect4Leaderboard leaderboard = new Connect4Leaderboard();
	
	private static Connect4Controller getInstance(String guildID)
	{
		Connect4Controller controller = instance.get(guildID);
		
		if (controller == null)
		{
			controller = new Connect4Controller();
			Connect4Controller.instance.put(guildID, controller);
			Connect4Controller.load(guildID);
		}
		
		return controller;
	}
	
	public static void load(String guildID)
	{
		File fileCharacters = new File(Paths.get("server_data", guildID, Connect4Controller.FOLDER,  "leaderboard.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		logger.info("Cariamento dati Connect4.");
		
		try (BufferedReader reader = new BufferedReader(new FileReader(fileCharacters)))
		{
			Connect4Leaderboard leaderboard = gson.fromJson(reader, Connect4Leaderboard.class);
			
			if (leaderboard != null)
				Connect4Controller.getInstance(guildID).leaderboard = leaderboard;
		}
		catch (FileNotFoundException e)
		{
			logger.info("Nessun file leaderboard da caricare.");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void save(String guildID)
	{
		File fileCharacters = new File(Paths.get("server_data", guildID, Connect4Controller.FOLDER, "leaderboard.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		logger.info("Salvataggio dati Connect4.");
		
		try (PrintWriter writer = new PrintWriter(new FileWriter(fileCharacters)))
		{
			writer.println(gson.toJson(Connect4Controller.getInstance(guildID).leaderboard));
		}
		catch (FileNotFoundException e)
		{
			try
			{
				fileCharacters.getParentFile().mkdirs();
				
				if (fileCharacters.createNewFile())
					Connect4Controller.save(guildID);
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static Connect4Leaderboard getLeaderboard(String guildID)
	{
		return Connect4Controller.getInstance(guildID).leaderboard;
	}
	
	public static Connect4UserData getUserData(String guildID, String userId)
	{
		return Connect4Controller.getInstance(guildID).leaderboard.getUserData().getOrDefault(userId, new Connect4UserData());
	}
	
	public static void setUserData(String guildID, String userId, Connect4UserData data)
	{
		Connect4Controller.getInstance(guildID).leaderboard.getUserData().put(userId, data);
		Connect4Controller.save(guildID);
	}
}
