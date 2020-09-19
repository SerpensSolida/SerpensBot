package com.serpenssolida.discordbot.hungergames;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.serpenssolida.discordbot.hungergames.io.CharacterData;
import com.serpenssolida.discordbot.hungergames.io.HungerGamesData;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import net.dv8tion.jda.api.entities.MessageChannel;

public class HungerGamesController
{
	public static HungerGamesController instance; //Singleton data.
	public static String folder = "hungergames/"; //Singleton data.
	public static int SUM_STATS = 40;
	public static Font font;
	
	private boolean running = false; //Whether or not the HungerGames is running.
	private int count = 0; //Number of editions of the HungerGames.
	private long messageSpeed = 1000; //Speed of the messages.
	private ArrayList<String> winners = new ArrayList<>(); //List of winners.
	public HashMap<String, Character> characters = new HashMap<>(); //List of characters.
	
	private static HungerGamesController getInstance()
	{
		if (instance == null)
		{
			instance = new HungerGamesController();
			
			try
			{
				//Create the font.
				HungerGamesController.font = Font.createFont(Font.TRUETYPE_FONT, new File(HungerGamesController.folder + "tahoma.ttf"))
						.deriveFont(12f);
			}
			catch (FontFormatException | IOException e)
			{
				e.printStackTrace();
			}
			
			HungerGamesController.load();
		}
		return instance;
	}
	
	/**
	 * Start a new edition of HungerGames.
	 *
	 * @param channel
	 * 		The channel where to send the messages.
	 */
	public static void startHungerGames(MessageChannel channel)
	{
		HungerGamesController.setRunning(true);
		
		Thread t = new HungerGamesThread(channel);
		t.start();
	}
	
	public static void load()
	{
		File fileCharacters = new File(HungerGamesController.folder + "characters.json");
		Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
		System.out.println("Cariamento dati HungerGames.");
		
		try (BufferedReader reader = new BufferedReader(new FileReader(fileCharacters)))
		{
			CharacterData characterData = gson.fromJson(reader, CharacterData.class);
			HungerGamesController.getInstance().characters = characterData.getCharacters();
		}
		catch (FileNotFoundException e)
		{
			System.out.println("Nessun file dei personaggi da caricare.");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		HungerGamesController.loadSettings();
	}
	
	public static void save()
	{
		File fileCharacters = new File(HungerGamesController.folder + "characters.json");
		Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
		System.out.println("Salvataggio dati HungerGames.");
		
		try (PrintWriter writer = new PrintWriter(new FileWriter(fileCharacters)))
		{
			CharacterData characterData = new CharacterData();
			characterData.setCharacters((getInstance()).characters);
			writer.println(gson.toJson(characterData));
			
		}
		catch (FileNotFoundException e)
		{
			try
			{
				if (fileCharacters.createNewFile())
					HungerGamesController.save();
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
		
		HungerGamesController.saveSettings();
	}
	
	public static void loadSettings()
	{
		File fileData = new File(HungerGamesController.folder + "hg_data.json");
		Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
		
		try (BufferedReader reader = new BufferedReader(new FileReader(fileData)))
		{
			HungerGamesData hgData = gson.fromJson(reader, HungerGamesData.class);
			HungerGamesController.setCount(hgData.getHgCount());
			HungerGamesController.setMessageSpeed(hgData.getMessageSpeed());
			HungerGamesController.getInstance().winners = hgData.getWinners();
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
	
	public static void saveSettings()
	{
		File fileData = new File(HungerGamesController.folder + "hg_data.json");
		Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
		
		try (PrintWriter writer = new PrintWriter(new FileWriter(fileData)))
		{
			HungerGamesData data = new HungerGamesData();
			data.setHgCount(HungerGamesController.getCount());
			data.setWinners(HungerGamesController.getWinners());
			data.setMessageSpeed(HungerGamesController.getMessageSpeed());
			
			writer.println(gson.toJson(data));
		}
		catch (FileNotFoundException e)
		{
			try
			{
				if (fileData.createNewFile())
					HungerGamesController.saveSettings();
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
	
	public static boolean isHungerGamesRunning()
	{
		return HungerGamesController.getInstance().running;
	}
	
	public static void setRunning(boolean running)
	{
		HungerGamesController.getInstance().running = running;
	}
	
	public static Character getCharacter(String authorID)
	{
		return HungerGamesController.getInstance().characters.get(authorID);
	}
	
	public static void addCharacter(Character character)
	{
		HungerGamesController.getInstance().characters.put(character.getOwnerID(), character);
		HungerGamesController.save();
	}
	
	public static HashMap<String, Character> getCharacters()
	{
		return HungerGamesController.getInstance().characters;
	}
	
	public static int getCount()
	{
		return HungerGamesController.getInstance().count;
	}
	
	public static void setCount(int count)
	{
		HungerGamesController.getInstance().count = count;
	}
	
	public static ArrayList<String> getWinners()
	{
		return HungerGamesController.getInstance().winners;
	}
	
	public static long getMessageSpeed()
	{
		return HungerGamesController.getInstance().messageSpeed;
	}
	
	public static void setMessageSpeed(float milliseconds)
	{
		HungerGamesController.getInstance().messageSpeed = (long) milliseconds;
	}
}
