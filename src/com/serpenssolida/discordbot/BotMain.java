package com.serpenssolida.discordbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.serpenssolida.discordbot.module.BotListener;
import com.serpenssolida.discordbot.module.base.BaseListener;
import com.serpenssolida.discordbot.module.hungergames.HungerGamesListener;
import com.serpenssolida.discordbot.module.poll.PollListener;
import com.serpenssolida.discordbot.module.settings.SettingsData;
import com.serpenssolida.discordbot.module.settings.SettingsListener;
import com.serpenssolida.discordbot.module.tictactoe.TicTacToeListener;
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

public class BotMain
{
	public static JDA api;
	public static HashMap<String, String> commandSymbol = new HashMap<>();
	public static HashMap<String, Boolean> deleteCommandMessages = new HashMap<>();
	
	public static String settingsFolder = "settings";
	
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
		api.addEventListener(new PollListener());
		api.addEventListener(new TicTacToeListener());
		api.addEventListener(new BaseListener());
		
		System.out.println("Bot ready!");
	}
	
	/**
	 * Update command list for all guilds.
	 */
	public static void updateAllGuildsCommands()
	{
		for (Guild guild : api.getGuilds())
		{
			BotMain.updateGuildCommands(guild);
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
		for (Object registeredListener : BotMain.api.getRegisteredListeners())
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
	 * Load the token from file.
	 *
	 * @return
	 * 		The token of the bot as {@link String};
	 */
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
	
	/**
	 * @return The list of modules of the bot.
	 */
	public static ArrayList<BotListener> getModules()
	{
		ArrayList<BotListener> modules = new ArrayList<>();
		
		for (Object registeredListener : BotMain.api.getRegisteredListeners())
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
		for (BotListener listener : BotMain.getModules())
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
		File settingsFile = new File(Paths.get("server_data", guildID, BotMain.settingsFolder, "settings.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println("Caricamento impostazioni per server con id: " + guildID + ".");
		ArrayList<BotListener> loadedListeners = new ArrayList<>();
		
		try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile)))
		{
			SettingsData settingsData = gson.fromJson(reader, SettingsData.class);
			
			BotMain.commandSymbol.put(guildID, settingsData.getCommandSymbol());
			BotMain.deleteCommandMessages.put(guildID, settingsData.getDeleteCommandMessages());
			HashMap<String, String> modulePrefixes = settingsData.getModulePrefixes();
			
			for (BotListener listener : BotMain.getModules())
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
			
			Guild guild = BotMain.api.getGuildById(guildID);
			if (guild != null)
			{
				/*for (BotListener loadedListener : loadedListeners)
				{
					//BotMain.updateGuildCommands(guild);
				}*/
				BotMain.updateGuildCommands(guild);
			}
		}
		catch (FileNotFoundException e)
		{
			System.out.println("Nessun file dei impostazioni da caricare.");
			System.out.println("Creazione impostazioni di default.");
			
			BotMain.commandSymbol.put(guildID, "/");
			BotMain.deleteCommandMessages.put(guildID, false);
			
			BotMain.saveSettings(guildID);
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
		File settingsFile = new File(Paths.get("server_data", guildID, BotMain.settingsFolder, "settings.json").toString());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		try (PrintWriter writer = new PrintWriter(new FileWriter(settingsFile)))
		{
			SettingsData settingsData = new SettingsData();
			HashMap<String, String> modulePrefixes = new HashMap<>();
			
			//Add command symbol.
			settingsData.setCommandSymbol(BotMain.getCommandSymbol(guildID));
			
			//Add list of prefixes
			for (BotListener listener : BotMain.getModules())
			{
				modulePrefixes.put(listener.getInternalID(), listener.getModulePrefix(guildID));
			}
			
			settingsData.setModulePrefixes(modulePrefixes);
			
			//Add delete command messages flag.
			settingsData.setDeleteCommandMessages(BotMain.getDeleteCommandMessages(guildID));
			
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
					BotMain.saveSettings(guildID);
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
		if (!BotMain.commandSymbol.containsKey(guildID))
		{
			BotMain.loadSettings(guildID);
		}
		
		BotMain.commandSymbol.put(guildID, symbol);
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
		if (!BotMain.commandSymbol.containsKey(guildID))
		{
			BotMain.loadSettings(guildID);
		}
		
		return BotMain.commandSymbol.get(guildID);
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
		if (!BotMain.deleteCommandMessages.containsKey(guildID))
		{
			BotMain.loadSettings(guildID);
		}
		
		BotMain.deleteCommandMessages.put(guildID, value);
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
		if (!BotMain.deleteCommandMessages.containsKey(guildID))
		{
			BotMain.loadSettings(guildID);
		}
		
		return BotMain.deleteCommandMessages.get(guildID);
	}
}
