package com.serpenssolida.discordbot.module.hungergames.event;

import com.serpenssolida.discordbot.module.hungergames.HungerGames;

public abstract class HungerGamesEvent
{
	public abstract EventResult doEvent(HungerGames paramHungerGames);
}
