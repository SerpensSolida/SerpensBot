package com.serpenssolida.discordbot;

import com.serpenssolida.discordbot.module.connect4.Connect4Listener;
import com.serpenssolida.discordbot.module.embed.EmbedListener;
import com.serpenssolida.discordbot.module.hungergames.HungerGamesListener;
import com.serpenssolida.discordbot.module.owner.OwnerListener;
import com.serpenssolida.discordbot.module.poll.PollListener;
import com.serpenssolida.discordbot.module.tictactoe.TicTacToeListener;

public class BotMain
{
	public static void main(String[] args)
	{
		SerpensBot.start();
		
		SerpensBot.addModule(new HungerGamesListener());
		SerpensBot.addModule(new PollListener());
		SerpensBot.addModule(new TicTacToeListener());
		SerpensBot.addModule(new OwnerListener());
		SerpensBot.addModule(new EmbedListener());
		SerpensBot.addModule(new Connect4Listener());
	}
}
