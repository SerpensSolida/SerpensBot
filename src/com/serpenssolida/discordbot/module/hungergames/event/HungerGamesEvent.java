package com.serpenssolida.discordbot.module.hungergames.event;

import com.serpenssolida.discordbot.module.hungergames.HungerGames;

public interface HungerGamesEvent
{
	/**
	 * Execute the event and returns a String describing it.
	 *
	 * @param hg
	 * 		The {@link HungerGames} the event is happening in.
	 *
	 * @return
	 * 		The {@link String} describing the event.
	 */
	EventResult doEvent(HungerGames hg);
}
