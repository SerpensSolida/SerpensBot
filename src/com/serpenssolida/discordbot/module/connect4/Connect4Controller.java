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
	private static final HashMap<String, Connect4Controller> instances = new HashMap<>(); //Singleton data.
	private static final String FOLDER = "connect4";
	private static final Logger logger = LoggerFactory.getLogger(Connect4Controller.class);
	
	private Connect4Leaderboard leaderboard = new Connect4Leaderboard();
	
	private static Connect4Controller getInstance(String guildID)
	{
		Connect4Controller controller = instances.get(guildID);
		
		if (controller == null)
		{
			controller = new Connect4Controller();
			Connect4Controller.load(guildID);
			Connect4Controller.instances.put(guildID, controller);
		}
		
		return controller;
	}
	
	/**
	 * Loads the connect4 leaderboard for the given guild.
	 *
	 * @param guildID
	 * 		The id of the guild the leaderboard will be loaded for.
	 */
	public static void load(String guildID)
	{
		File connect4File = new File(Paths.get("server_data", guildID, Connect4Controller.FOLDER,  "leaderboard.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		logger.info("Cariamento dati Connect4.");
		
		try (BufferedReader reader = new BufferedReader(new FileReader(connect4File)))
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
	
	/**
	 * Saves the connect4 leaderboard for the given guild.
	 *
	 * @param guildID
	 * 		The id of the guild the leaderboard that will be saved.
	 */
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
	
	/**
	 * Gets the leaderboard for the given guild.
	 *
	 * @param guildID
	 * 		The id of the guild the leaderboard is from.
	 *
	 * @return
	 * 		The leaderboard of the given guild.
	 */
	public static Connect4Leaderboard getLeaderboard(String guildID)
	{
		return Connect4Controller.getInstance(guildID).leaderboard;
	}
	
	/**
	 * Gets the connect4 data for the given user.
	 *
	 * @param guildID
	 * 		The id of guild the user is from.
	 * @param userId
	 * 		The id of the user.
	 *
	 * @return
	 * 		The connect4 data of the user.
	 */
	public static Connect4UserData getUserData(String guildID, String userId)
	{
		return Connect4Controller.getInstance(guildID).leaderboard.getUserData().getOrDefault(userId, new Connect4UserData());
	}
	
	/**
	 * Sets the data for the given user in the given guild.
	 *
 	 * @param guildID
	 * 		The id of the guild the user is from.
	 * @param userId
	 * 		The id of the user.
	 * @param data
	 * 		The data that will be assigned to the user.
	 */
	public static void setUserData(String guildID, String userId, Connect4UserData data)
	{
		Connect4Controller.getInstance(guildID).leaderboard.getUserData().put(userId, data);
		Connect4Controller.save(guildID);
	}
}
