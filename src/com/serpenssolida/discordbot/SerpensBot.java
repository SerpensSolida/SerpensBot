package com.serpenssolida.discordbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.serpenssolida.discordbot.module.BotListener;
import com.serpenssolida.discordbot.module.base.BaseListener;
import com.serpenssolida.discordbot.module.settings.SettingsData;
import com.serpenssolida.discordbot.module.settings.SettingsListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class SerpensBot
{
	public static JDA api;
	public static HashMap<String, String> commandSymbol = new HashMap<>();
	public static HashMap<String, Boolean> deleteCommandMessages = new HashMap<>();
	public static String ownerId;
	
	public static String settingsFolder = "settings";
	
	public static void start()
	{
		//Load data from file.
		BotData data = SerpensBot.loadBotData();
		
		//Setting headless mode. We are using some drawing function without the gui.
		System.setProperty("java.awt.headless", "true");
		
		try
		{
			api = JDABuilder
					.createDefault(data.getToken())
					.setMemberCachePolicy(MemberCachePolicy.ALL)
					.enableIntents(GatewayIntent.GUILD_MEMBERS)
					.build();
			api.awaitReady();
		}
		catch (LoginException e)
		{
			System.err.println("Login error.");
			e.printStackTrace();
			return;
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
			return;
		}
		
		api.addEventListener(new BaseListener());
		api.addEventListener(new SettingsListener());
		
		if (data.getOwner() == null || data.getOwner().isBlank())
		{
			System.err.println("No bot owner set in the json file.");
			return;
		}
		
		//Set the owner of the bot.
		SerpensBot.ownerId = data.getOwner();
		
		System.out.println("Bot ready!");
	}
	
	/**
	 * Register a {@link BotListener}.
	 * @param listener The {@link BotListener} to register.
	 */
	public static void addModule(BotListener listener)
	{
		SerpensBot.api.addEventListener(listener);
	}
	
	/**
	 * Update command list for all guilds.
	 */
	public static void updateAllGuildsCommands()
	{
		for (Guild guild : api.getGuilds())
		{
			SerpensBot.updateGuildCommands(guild);
		}
	}
	
	/**
	 * Update the command list of the given guild.
	 *
	 * @param guild
	 * 		The guild that will receive the update command list.
	 */
	public static void updateGuildCommands(Guild guild)
	{
		CommandListUpdateAction commands = guild.updateCommands();
		for (Object registeredListener : SerpensBot.api.getRegisteredListeners())
		{
			if (registeredListener instanceof BotListener)
			{
				BotListener listener = (BotListener) registeredListener;
				
				for (CommandData commandData : listener.generateCommands(guild))
				{
					commands.addCommands(commandData);
				}
			}
		}
		
		commands.queue(a -> System.out.println("Comandi per la guild \"" + guild.getName() + "\" cambiati correttamente."));
	}
	
	/**
	 * Search multiple users by the given name.
	 *
	 * @param guild
	 * 		The guild from where to search from.
	 * @param userName
	 * 		The username of the users to search for.
	 *
	 * @return
	 * 		An {@link ArrayList<Member>} that contains all the match.
	 */
	public static ArrayList<Member> findUsersByName(Guild guild, String userName)
	{
		HashSet<Member> users = new HashSet<>();
		
		users.addAll(guild.getMembersByName(userName, true));
		users.addAll(guild.getMembersByNickname(userName, true));
		
		return new ArrayList<>(users);
	}
	
	/**
	 * Check if the user has administration permission.
	 *
	 * @param member
	 * 		The user to check.
	 *
	 * @return
	 * 		-True if the user has administration permission.
	 * 		-False if the user has not administration permission.
	 */
	public static boolean isAdmin(Member member)
	{
		if (member == null) return false;
		
		for (Role role : member.getRoles())
		{
			if (role.hasPermission(Permission.MANAGE_SERVER))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Load the bot data from file.
	 *
	 * @return
	 * 		The data read from file.
	 */
	private static BotData loadBotData()
	{
		File tokenFile = new File("bot.json");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println("Caricamento token.");
		
		try (BufferedReader reader = new BufferedReader(new FileReader(tokenFile)))
		{
			BotData botData = gson.fromJson(reader, BotData.class);
			
			System.out.println("Token caricato: " + botData.getToken());
			
			return botData;
		}
		catch (FileNotFoundException e)
		{
			System.out.println("Nessun file dei impostazioni da caricare.");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return new BotData();
	}
	
	/**
	 * @return The list of modules of the bot.
	 */
	public static ArrayList<BotListener> getModules()
	{
		ArrayList<BotListener> modules = new ArrayList<>();
		
		for (Object registeredListener : SerpensBot.api.getRegisteredListeners())
		{
			if (registeredListener instanceof BotListener)
			{
				modules.add((BotListener) registeredListener);
			}
		}
		
		return modules;
	}
	
	/**
	 * Get the module with the given id.
	 *
	 * @param moduleID
	 * 		The id of the module to get.
	 *
	 * @return The module with the passed id.
	 */
	public static BotListener getModuleById(String moduleID)
	{
		//Get the module with the correct id and get its module prefix.
		for (BotListener listener : SerpensBot.getModules())
		{
			if (listener.getInternalID().equals(moduleID))
			{
				return listener;
			}
		}
		
		return null;
	}
	
	/**
	 * Load the setting of the bot for the given guild.
	 *
	 * @param guildID
	 * 		The id of the guild.
	 */
	public static void loadSettings(String guildID)
	{
		File settingsFile = new File(Paths.get("server_data", guildID, SerpensBot.settingsFolder, "settings.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println("Caricamento impostazioni per server con id: " + guildID + ".");
		ArrayList<BotListener> loadedListeners = new ArrayList<>();
		
		try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile)))
		{
			SettingsData settingsData = gson.fromJson(reader, SettingsData.class);
			
			SerpensBot.commandSymbol.put(guildID, settingsData.getCommandSymbol());
			SerpensBot.deleteCommandMessages.put(guildID, settingsData.getDeleteCommandMessages());
			HashMap<String, String> modulePrefixes = settingsData.getModulePrefixes();
			
			for (BotListener listener : SerpensBot.getModules())
			{
				if (modulePrefixes.containsKey(listener.getInternalID()))
				{
					listener.setModulePrefix(guildID, modulePrefixes.get(listener.getInternalID()));
				}
				else
				{
					listener.setModulePrefix(guildID, listener.getInternalID()); //Reset listener if key is not found.
				}
			}
			
			Guild guild = SerpensBot.api.getGuildById(guildID);
			if (guild != null)
			{
				/*for (BotListener loadedListener : loadedListeners)
				{
					//BotMain.updateGuildCommands(guild);
				}*/
				SerpensBot.updateGuildCommands(guild);
			}
		}
		catch (FileNotFoundException e)
		{
			System.out.println("Nessun file dei impostazioni da caricare.");
			System.out.println("Creazione impostazioni di default.");
			
			//Initialize default values.
			SerpensBot.commandSymbol.put(guildID, "/");
			SerpensBot.deleteCommandMessages.put(guildID, false);
			
			for (BotListener module : getModules())
			{
				module.setModulePrefix(guildID, module.getInternalID());
			}
			
			SerpensBot.saveSettings(guildID);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Save the guild setting.
	 *
	 * @param guildID
	 * 		The id of the guild.
	 */
	public static void saveSettings(String guildID)
	{
		File settingsFile = new File(Paths.get("server_data", guildID, SerpensBot.settingsFolder, "settings.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		try (PrintWriter writer = new PrintWriter(new FileWriter(settingsFile)))
		{
			SettingsData settingsData = new SettingsData();
			HashMap<String, String> modulePrefixes = new HashMap<>();
			
			//Add command symbol.
			settingsData.setCommandSymbol(SerpensBot.getCommandSymbol(guildID));
			
			//Add list of prefixes
			for (BotListener listener : SerpensBot.getModules())
			{
				modulePrefixes.put(listener.getInternalID(), listener.getModulePrefix(guildID));
			}
			
			settingsData.setModulePrefixes(modulePrefixes);
			
			//Add delete command messages flag.
			settingsData.setDeleteCommandMessages(SerpensBot.getDeleteCommandMessages(guildID));
			
			System.out.println("Salvataggio impostazioni.");
			writer.println(gson.toJson(settingsData));
		}
		catch (FileNotFoundException e)
		{
			System.out.println("Nessun file impostazioni del server con id: " + guildID + ".");
			
			try
			{
				settingsFile.getParentFile().mkdirs();
				
				if (settingsFile.createNewFile())
					SerpensBot.saveSettings(guildID);
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
	 * Set the unlisted command symbol for the given guild.
	 *
	 * @param guildID
	 * 		The id of the guild.
	 * @param symbol
	 * 		The symbol that will be used for unlisted commands.
	 */
	public static void setCommandSymbol(String guildID, String symbol)
	{
		//If key is not found the settings are not loaded or are not created.
		if (!SerpensBot.commandSymbol.containsKey(guildID))
		{
			SerpensBot.loadSettings(guildID);
		}
		
		SerpensBot.commandSymbol.put(guildID, symbol);
	}
	
	/**
	 * Get the current unlisted command symbol for the given guild.
	 *
	 * @param guildID
	 * 		The id of the guild.
	 *
	 * @return
	 * 		The symbol used for unlisted commands of the given guild.
	 */
	public static String getCommandSymbol(String guildID)
	{
		//If key is not found the settings are not loaded or are not created.
		if (!SerpensBot.commandSymbol.containsKey(guildID))
		{
			SerpensBot.loadSettings(guildID);
		}
		
		return SerpensBot.commandSymbol.get(guildID);
	}
	
	/**
	 * Set the flag for deleting messsage after an unlisted command sent in the given guild.
	 *
	 * @param guildID
	 * 		The id of the guild.
	 * @param value
	 *		If set to true the bot will delete message when receiving an unlisted command, false will instead do the opposite.
	 */
	public static void setDeleteCommandMessages(String guildID, boolean value)
	{
		//If key is not found the settings are not loaded or are not created.
		if (!SerpensBot.deleteCommandMessages.containsKey(guildID))
		{
			SerpensBot.loadSettings(guildID);
		}
		
		SerpensBot.deleteCommandMessages.put(guildID, value);
	}
	
	/**
	 * Get the flag for deleting message after an unlisted command sent in the given guild.
	 *
	 * @param guildID
	 * 		The id of the guild.
	 *
	 * @return
	 * 		True if the bot should delete unlisted command messages, false otherwise.
	 */
	public static boolean getDeleteCommandMessages(String guildID)
	{
		//If key is not found the settings are not loaded or are not created.
		if (!SerpensBot.deleteCommandMessages.containsKey(guildID))
		{
			SerpensBot.loadSettings(guildID);
		}
		
		return SerpensBot.deleteCommandMessages.get(guildID);
	}
}
