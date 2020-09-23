package com.serpenssolida.discordbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.serpenssolida.discordbot.hungergames.HungerGamesController;
import com.serpenssolida.discordbot.hungergames.HungerGamesListener;

import javax.security.auth.login.LoginException;

import com.serpenssolida.discordbot.hungergames.io.CharacterData;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

public class BotMain
{
	public static JDA api;
	public static String commandSymbol = "!";
	public static boolean deleteCommandMessages;
	
	public static void main(String[] args)
	{
		String token = BotMain.getBotToken(); //Loading the token from file.
		
		//Setting headless mode. We are using some drawing function without the gui.
		System.setProperty("java.awt.headless", "true");
		
		try
		{
			api = JDABuilder
					.createDefault(token)
					.setMemberCachePolicy(MemberCachePolicy.ALL)
					.enableIntents(GatewayIntent.GUILD_MEMBERS)
					.build();
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
	
	public static ArrayList<Member> findUsersByName(Guild guild, String userName)
	{
		HashSet<Member> users = new HashSet<>();
		
		users.addAll(guild.getMembersByName(userName, true));
		users.addAll(guild.getMembersByNickname(userName, true));
		
		return new ArrayList<>(users);
	}
	
	private static String getBotToken()
	{
		File tokenFile = new File("bot.json");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println("Caricamento token.");
		
		try (BufferedReader reader = new BufferedReader(new FileReader(tokenFile)))
		{
			BotToken botToken = gson.fromJson(reader, BotToken.class);
			
			System.out.println("Token caricato: " + botToken.getToken());
			
			return botToken.getToken();
		}
		catch (FileNotFoundException e)
		{
			System.out.println("Nessun file dei impostazioni da caricare.");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return "";
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
			BotMain.deleteCommandMessages = settingsData.getDeleteCommandMessages();
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
			
			//Add delete command messages flag.
			settingsData.setDeleteCommandMessages(BotMain.deleteCommandMessages);
			
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
