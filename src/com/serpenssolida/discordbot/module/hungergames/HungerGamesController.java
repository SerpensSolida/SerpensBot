package com.serpenssolida.discordbot.module.hungergames;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.serpenssolida.discordbot.module.hungergames.io.CharacterData;
import com.serpenssolida.discordbot.module.hungergames.io.HungerGamesData;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.awt.*;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class HungerGamesController
{
	public static HashMap<String, HungerGamesController> instance = new HashMap<>(); //Singleton data.
	public static String folder = "hungergames";
	public static int SUM_STATS = 40;
	public static Font font;
	
	private boolean running = false; //Whether or not the HungerGames is running.
	private int count = 0; //Number of editions of the HungerGames.
	private long messageSpeed = 1000; //Speed of the messages.
	private Thread gameThread;
	private ArrayList<String> winners = new ArrayList<>(); //List of winners.
	private HashMap<String, Character> characters = new HashMap<>(); //List of characters.
	
	private static HungerGamesController getInstance(String guildID)
	{
		HungerGamesController controller = instance.get(guildID);
		
		if (controller == null)
		{
			controller = new HungerGamesController();
			HungerGamesController.instance.put(guildID, controller);
			HungerGamesController.loadFont();
			
			HungerGamesController.load(guildID);
		}
		
		return controller;
	}
	
	/**
	 * Start a new edition of HungerGames.
	 *
	 * @param channel
	 * 		The channel where to send the messages.
	 */
	public static void startHungerGames(String guildID, MessageChannel channel)
	{
		//Cannot start a new HungerGames if there is already one running.
		if (HungerGamesController.isHungerGamesRunning(guildID))
		{
			MessageBuilder messageBuilder = new MessageBuilder();
			messageBuilder.append("> Non puoi usare questo comando mentre è in corso un HungerGames.");
			channel.sendMessage(messageBuilder.build()).queue();
			return;
		}
		
		HungerGamesController.setRunning(guildID, true);
		
		Thread t = new HungerGamesThread(guildID, channel);
		t.start();
		
		HungerGamesController.getInstance(guildID).gameThread = t;
	}
	
	public static void load(String guildID)
	{
		File fileCharacters = new File(Paths.get("server_data", guildID, HungerGamesController.folder,  "characters.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println("Cariamento dati HungerGames.");
		
		try (BufferedReader reader = new BufferedReader(new FileReader(fileCharacters)))
		{
			CharacterData characterData = gson.fromJson(reader, CharacterData.class);
			HungerGamesController.getInstance(guildID).characters = characterData.getCharacters();
		}
		catch (FileNotFoundException e)
		{
			System.out.println("Nessun file dei personaggi da caricare.");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		HungerGamesController.loadSettings(guildID);
	}
	
	public static void save(String guildID)
	{
		File fileCharacters = new File(Paths.get("server_data", guildID, HungerGamesController.folder, "characters.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println("Salvataggio dati HungerGames.");
		
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
	
	public static void loadSettings(String guildID)
	{
		File fileData = new File(Paths.get("server_data", guildID, HungerGamesController.folder,  "hg_data.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		try (BufferedReader reader = new BufferedReader(new FileReader(fileData)))
		{
			HungerGamesData hgData = gson.fromJson(reader, HungerGamesData.class);
			HungerGamesController.setCount(guildID, hgData.getHgCount());
			HungerGamesController.setMessageSpeed(guildID, hgData.getMessageSpeed());
			HungerGamesController.getInstance(guildID).winners = hgData.getWinners();
		}
		catch (FileNotFoundException e)
		{
			System.out.println("Nessun file degli HungerGames da caricare.");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void saveSettings(String guildID)
	{
		File fileData = new File(Paths.get("server_data", guildID, HungerGamesController.folder,  "hg_data.json").toString());
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
				HungerGamesController.font = Font.createFont(Font.TRUETYPE_FONT, new File(HungerGamesController.folder + "/tahoma.ttf"))
						.deriveFont(12f);
			}
		}
		catch (FontFormatException | IOException e)
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
	
	public static HashMap<String, Character> getCharacters(String guildID)
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
	
	public static ArrayList<String> getWinners(String guildID)
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
