package com.serpenssolida.discordbot.module.hungergames;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.serpenssolida.discordbot.module.hungergames.io.CharacterData;
import com.serpenssolida.discordbot.module.hungergames.io.HungerGamesData;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HungerGamesController
{
	private static final Logger logger = LoggerFactory.getLogger(HungerGamesController.class);
	private static final HashMap<String, HungerGamesController> instance = new HashMap<>(); //Singleton data.
	protected static final String FOLDER = "hungergames";
	protected static Font font;
	public static final int SUM_STATS = 40;
	
	private boolean running = false; //Whether the HungerGames is running.
	private int count = 0; //Number of editions of the HungerGames.
	private long messageSpeed = 1000; //Speed of the messages.
	private Thread gameThread;
	private List<String> winners = new ArrayList<String>(); //List of winners.
	private Map<String, Character> characters = new HashMap<>(); //List of characters.
	
	private static HungerGamesController getInstance(String guildID)
	{
		HungerGamesController controller = instance.get(guildID);
		HungerGamesController.loadFont();
		
		if (controller == null)
		{
			controller = new HungerGamesController();
			HungerGamesController.load(controller, guildID);
			HungerGamesController.instance.put(guildID, controller);
		}
		
		return controller;
	}
	
	/**
	 * Start a new edition of HungerGames.
	 *
	 * @param channel
	 * 		The channel where to send the messages.
	 * @param author
	 * 		The user who started the HungerGames.
	 */
	public static void startHungerGames(String guildID, MessageChannel channel, User author)
	{
		//Cannot start a new HungerGames if there is already one running.
		if (HungerGamesController.isHungerGamesRunning(guildID))
			return;
		
		HungerGamesController.setRunning(guildID, true);
		
		Thread t = new HungerGamesThread(guildID, channel, author);
		t.start();
		
		HungerGamesController.getInstance(guildID).gameThread = t;
	}
	
	public static void load(HungerGamesController controller, String guildID)
	{
		File fileCharacters = new File(Paths.get("server_data", guildID, HungerGamesController.FOLDER,  "characters.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		logger.info("Cariamento dati HungerGames.");
		
		try (BufferedReader reader = new BufferedReader(new FileReader(fileCharacters)))
		{
			CharacterData characterData = gson.fromJson(reader, CharacterData.class);
			controller.characters = characterData.getCharacters();
		}
		catch (FileNotFoundException e)
		{
			logger.info("Nessun file dei personaggi da caricare.");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		HungerGamesController.loadSettings(controller, guildID);
	}
	
	public static void save(String guildID)
	{
		File fileCharacters = new File(Paths.get("server_data", guildID, HungerGamesController.FOLDER, "characters.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		logger.info("Salvataggio dati HungerGames.");
		
		try (PrintWriter writer = new PrintWriter(new FileWriter(fileCharacters)))
		{
			CharacterData characterData = new CharacterData();
			characterData.setCharacters(HungerGamesController.getInstance(guildID).characters);
			writer.println(gson.toJson(characterData));
			
		}
		catch (FileNotFoundException e)
		{
			try
			{
				fileCharacters.getParentFile().mkdirs();
				
				if (fileCharacters.createNewFile())
					HungerGamesController.save(guildID);
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
		
		HungerGamesController.saveSettings(guildID);
	}
	
	public static void loadSettings(HungerGamesController controller, String guildID)
	{
		File fileData = new File(Paths.get("server_data", guildID, HungerGamesController.FOLDER,  "hg_data.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		try (BufferedReader reader = new BufferedReader(new FileReader(fileData)))
		{
			HungerGamesData hgData = gson.fromJson(reader, HungerGamesData.class);
			controller.count = hgData.getHgCount();
			controller.messageSpeed = hgData.getMessageSpeed();
			controller.winners = hgData.getWinners();
		}
		catch (FileNotFoundException e)
		{
			logger.info("Nessun file degli HungerGames da caricare.");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void saveSettings(String guildID)
	{
		File fileData = new File(Paths.get("server_data", guildID, HungerGamesController.FOLDER,  "hg_data.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		try (PrintWriter writer = new PrintWriter(new FileWriter(fileData)))
		{
			HungerGamesData data = new HungerGamesData();
			data.setHgCount(HungerGamesController.getCount(guildID));
			data.setWinners(HungerGamesController.getWinners(guildID));
			data.setMessageSpeed(HungerGamesController.getMessageSpeed(guildID));
			
			writer.println(gson.toJson(data));
		}
		catch (FileNotFoundException e)
		{
			try
			{
				fileData.getParentFile().mkdirs();
				
				if (fileData.createNewFile())
					HungerGamesController.saveSettings(guildID);
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
	
	public static void loadFont()
	{
		try
		{
			if (HungerGamesController.font == null)
			{
				//Create the font.
				HungerGamesController.font = Font.createFont(Font.TRUETYPE_FONT, new File(HungerGamesController.FOLDER + "/tahoma.ttf"))
						.deriveFont(12f);
			}
		}
		catch (FontFormatException | IOException | RuntimeException e)
		{
			e.printStackTrace();
		}
	}
	
	public static boolean isHungerGamesRunning(String guildID)
	{
		return HungerGamesController.getInstance(guildID).running;
	}
	
	public static void setRunning(String guildID, boolean running)
	{
		HungerGamesController.getInstance(guildID).running = running;
	}
	
	public static void stopHungerGames(String guildID)
	{
		HungerGamesController.getInstance(guildID).gameThread.interrupt();
		HungerGamesController.getInstance(guildID).running = false;
	}
	
	public static Character getCharacter(String guildID, String authorID)
	{
		return HungerGamesController.getInstance(guildID).characters.get(authorID);
	}
	
	public static void addCharacter(String guildID, Character character)
	{
		HungerGamesController.getInstance(guildID).characters.put(character.getOwnerID(), character);
		HungerGamesController.save(guildID);
	}
	
	public static Map<String, Character> getCharacters(String guildID)
	{
		return HungerGamesController.getInstance(guildID).characters;
	}
	
	public static int getCount(String guildID)
	{
		return HungerGamesController.getInstance(guildID).count;
	}
	
	public static void setCount(String guildID, int count)
	{
		HungerGamesController.getInstance(guildID).count = count;
	}
	
	public static List<String> getWinners(String guildID)
	{
		return HungerGamesController.getInstance(guildID).winners;
	}
	
	public static long getMessageSpeed(String guildID)
	{
		return HungerGamesController.getInstance(guildID).messageSpeed;
	}
	
	public static void setMessageSpeed(String guildID, float milliseconds)
	{
		HungerGamesController.getInstance(guildID).messageSpeed = (long) milliseconds;
	}
}
