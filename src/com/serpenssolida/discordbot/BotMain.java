package com.serpenssolida.discordbot;

import com.serpenssolida.discordbot.module.channelfilter.ChannelFilterListener;
import com.serpenssolida.discordbot.module.connect4.Connect4Listener;
import com.serpenssolida.discordbot.module.embed.EmbedListener;
import com.serpenssolida.discordbot.module.forum.ForumChannelListener;
import com.serpenssolida.discordbot.module.hungergames.HungerGamesListener;
import com.serpenssolida.discordbot.module.musicplayer.MusicPlayerListener;
import com.serpenssolida.discordbot.module.owner.OwnerListener;
import com.serpenssolida.discordbot.module.poll.PollListener;
import com.serpenssolida.discordbot.module.reminder.ReminderListener;
import com.serpenssolida.discordbot.module.thread.ThreadListener;
import com.serpenssolida.discordbot.module.tictactoe.TicTacToeListener;

public class BotMain
{
	public static void main(String[] args)
	{
		SerpensBot.setOnInitCallback(() ->
		{
			SerpensBot.addModule(new HungerGamesListener());
			SerpensBot.addModule(new PollListener());
			SerpensBot.addModule(new TicTacToeListener());
			SerpensBot.addModule(new OwnerListener());
			SerpensBot.addModule(new EmbedListener());
			SerpensBot.addModule(new Connect4Listener());
			SerpensBot.addModule(new ChannelFilterListener());
			SerpensBot.addModule(new MusicPlayerListener());
			SerpensBot.addModule(new ForumChannelListener());
			SerpensBot.addModule(new ThreadListener());
			SerpensBot.addModule(new ReminderListener());
		});
		
		SerpensBot.start();
	}
}
