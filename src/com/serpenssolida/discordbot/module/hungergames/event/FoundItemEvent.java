package com.serpenssolida.discordbot.module.hungergames.event;

import com.serpenssolida.discordbot.RandomChoice;
import com.serpenssolida.discordbot.module.hungergames.HungerGames;
import com.serpenssolida.discordbot.module.hungergames.Player;
import com.serpenssolida.discordbot.module.hungergames.inventory.Item;
import com.serpenssolida.discordbot.module.hungergames.inventory.Weapon;

import java.util.HashSet;

public class FoundItemEvent implements HungerGamesEvent
{
	private final String[] foundMessage = new String[]
			{
					"user trova item rovistando nella spazzatura.",
					"user inciampa e cade, girandosi vede che è inciampato su item,",
					"user scorge da lontano item.",
					"Aprendo una scatola user trova item.",
					"user trova item dentro un sacchetto chiuso.",
					"user sbatte la testa su item.",
					"user incappa in item per caso."
			};
	
	public EventResult doEvent(HungerGames hg)
	{
		HashSet<Player> involvedPlayers = hg.getInvolvedPlayers();
		HashSet<Player> foundItemPlayers = hg.getFoundItemPlayers();
		
		//List of player that did not found an item this turn.
		HashSet<Player> notFoundItemPlayers = new HashSet<>(hg.getAlivePlayers());
		notFoundItemPlayers.removeAll(hg.getFoundItemPlayers());
		
		if (notFoundItemPlayers.isEmpty())
			return new EventResult("", EventResult.State.FAILED); //Quit event.
		
		//Get a random player.
		Player player = (Player) RandomChoice.getRandom(notFoundItemPlayers.toArray());
		
		//Chose a random item form item pool.
		Object[] items = RandomChoice.getRandom().nextBoolean() ? hg.getItemData().getFoods().toArray() : hg.getWeaponPool().toArray();
		Item item = (Item) RandomChoice.getRandom(items);
		
		//Weapon are unique so if the item found is a weapon it must be removed from the weapon pool.
		if (item instanceof Weapon)
		{
			hg.getWeaponPool().remove(item);
		}
		
		//Add the item to the player's invetory.
		player.getInventory().addItem(item, 1);
		
		foundItemPlayers.add(player);
		involvedPlayers.add(player);
		
		String eventString = this.replace((String) RandomChoice.getRandom(this.foundMessage), item, player) + "\n";
		return new EventResult(eventString, EventResult.State.SUCCESSFUL);
	}
	
	private String replace(String message, Item item, Player player)
	{
		message = message.replace("user", "**" + player + "**");
		message = message.replace("item", "*" + item.getName() + "*");
		return message;
	}
}
