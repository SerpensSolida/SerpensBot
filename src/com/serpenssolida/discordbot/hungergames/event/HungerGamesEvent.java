package com.serpenssolida.discordbot.hungergames.event;

import com.serpenssolida.discordbot.hungergames.HungerGames;

public abstract class HungerGamesEvent
{
	public abstract EventResult doEvent(HungerGames paramHungerGames);
}
