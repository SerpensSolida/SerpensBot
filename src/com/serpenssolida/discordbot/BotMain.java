package com.serpenssolida.discordbot;

import com.serpenssolida.discordbot.hungergames.HungerGamesListener;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class BotMain
{
	public static JDA api;
	public static String commandSymbol = "/";
	
	public static void main(String[] args)
	{
		try
		{
			api = (new JDABuilder(Security.BOT_TOKEN)).build();
			api.awaitReady();
			System.out.println("Bot ready!");
		}
		catch (LoginException e)
		{
			System.out.println("Login error.");
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		api.addEventListener(new HungerGamesListener());
	}
}
