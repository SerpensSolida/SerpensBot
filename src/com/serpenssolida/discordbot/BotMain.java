package com.serpenssolida.discordbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.serpenssolida.discordbot.hungergames.HungerGamesController;
import com.serpenssolida.discordbot.hungergames.HungerGamesListener;

import javax.security.auth.login.LoginException;

import com.serpenssolida.discordbot.hungergames.io.CharacterData;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class BotMain
{
	public static JDA api;
	public static String commandSymbol = "/";
	
	public static void main(String[] args)
	{
		try
		{
			api = JDABuilder.createDefault(Security.BOT_TOKEN).build();
			api.awaitReady();
			System.out.println("Bot ready!");
		}
		catch (LoginException e)
		{
			System.out.println("Login error.");
			e.printStackTrace();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		api.addEventListener(new SettingsListener());
		api.addEventListener(new HungerGamesListener());
		
		BotMain.loadSettings();
	}
	
	public static void loadSettings()
	{
		File settingsFile = new File("settings.json");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println("Caricamento impostazioni.");
		
		try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile)))
		{
			SettingsData settingsData = gson.fromJson(reader, SettingsData.class);
			
			BotMain.commandSymbol = settingsData.getCommandSymbol();
			HashMap<String, String> modulePrefixes = settingsData.getModulePrefixes();
			
			for (Object registeredListener : api.getEventManager().getRegisteredListeners())
			{
				if (registeredListener instanceof BotListener)
				{
					BotListener listener = (BotListener) registeredListener;
					
					if (modulePrefixes.containsKey(listener.getInternalID()))
					{
						listener.setModulePrefix(modulePrefixes.get(listener.getInternalID()));
					}
				}
			}
		}
		catch (FileNotFoundException e)
		{
			System.out.println("Nessun file dei impostazioni da caricare.");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void saveSettings()
	{
		File settingsFile = new File( "settings.json");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println("Salvataggio impostazioni.");
		
		try (PrintWriter writer = new PrintWriter(new FileWriter(settingsFile)))
		{
			SettingsData settingsData = new SettingsData();
			HashMap<String, String> modulePrefixes = new HashMap<>();
			
			//Add command symbol.
			settingsData.setCommandSymbol(BotMain.commandSymbol);
			
			//Add list of prefixes
			for (Object registeredListener : api.getEventManager().getRegisteredListeners())
			{
				if (registeredListener instanceof BotListener)
				{
					BotListener listener = (BotListener) registeredListener;
					
					modulePrefixes.put(listener.getInternalID(), listener.getModulePrefix());

				}
			}
			
			settingsData.setModulePrefixes(modulePrefixes);
			
			writer.println(gson.toJson(settingsData));
		}
		catch (FileNotFoundException e)
		{
			try
			{
				if (settingsFile.createNewFile())
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
}
