package com.serpenssolida.discordbot.module.hungergames.event;

import com.serpenssolida.discordbot.RandomChoice;
import com.serpenssolida.discordbot.module.hungergames.Player;
import com.serpenssolida.discordbot.module.hungergames.inventory.Item;

import java.util.HashSet;

public interface BattleEvent extends HungerGamesEvent
{
	/**
	 *
	 * @param killer
	 * 		The player that assasinated the other player.
	 * @param victim
	 * 		The victim of the assasination.
	 * @param alivePlayers
	 * 		Set of all alive player in the hunger games.
	 * @param deadPlayers
	 * 		Set of all dead player in the hunger games.
	 *
	 * @return
	 * 		The string of the event.
	 */
	public default String onPlayerKilled(Player killer, Player victim, HashSet<Player> alivePlayers, HashSet<Player> deadPlayers)
	{
		StringBuilder stringBuilder = new StringBuilder();
		
		//Move the victim into the correct set.
		alivePlayers.remove(victim);
		deadPlayers.add(victim);
		
		//Remove relationship.
		victim.removeRelationships();
		
		//Increment player body count.
		killer.getCharacter().incrementKills();
		
		stringBuilder.append("**" + victim + "** è morto.\n");
		
		//The killer steal a random item.
		if (RandomChoice.randomChance(10))
		{
			Item stolenItem = victim.getInventory().getRandomItemFromInventory();
			
			if (stolenItem != null)
			{
				killer.getInventory().addItem(stolenItem, 1);
				
				stringBuilder.append(String.format("**%s** ha trovato *%s* nel corpo di **%s**.\n", killer, stolenItem, victim));
			}
		}
		
		return stringBuilder.toString();
	}
	
	/**
	 * Get a set of relationship based on the other type reletionship sets.
	 *
	 * @param player
	 * 		The player which has the relationship sets.
	 * @param alivePlayers
	 * 		Set of all alive player in the hunger games.
	 * @param availablePlayers
	 * 		Set of all available player in the hungergames.
	 *
	 * @return
	 * 		A set of relationship.
	 */
	public default HashSet<Player> getRelationshipSet(Player player, HashSet<Player> alivePlayers, HashSet<Player> availablePlayers)
	{
		//List of alive players that are neutral to the player.
		HashSet<Player> neutralAlivePlayers = new HashSet<>(alivePlayers);
		neutralAlivePlayers.remove(player);
		neutralAlivePlayers.removeAll(player.getFriends());
		neutralAlivePlayers.removeAll(player.getEnemies());
		
		//List of available player that are neutral to the player.
		HashSet<Player> neutralPlayers = new HashSet<>(availablePlayers);
		neutralPlayers.removeAll(player.getFriends());
		neutralPlayers.removeAll(player.getEnemies());
		
		//List of available enemies of the player.
		HashSet<Player> enemyPlayers = new HashSet<>(availablePlayers);
		enemyPlayers.removeAll(neutralPlayers);
		enemyPlayers.removeAll(player.getFriends());
		
		//List of available friends of the player.
		HashSet<Player> friendsPlayer = new HashSet<>(availablePlayers);
		friendsPlayer.removeAll(neutralPlayers);
		friendsPlayer.removeAll(player.getEnemies());
		
		HashSet<Player> chosenCategory = new HashSet<>();
		
		//Chose a category.
		if (!player.getEnemies().isEmpty())
			chosenCategory = enemyPlayers;
		else if (!neutralAlivePlayers.isEmpty())
			chosenCategory = neutralPlayers;
		else if (!player.getFriends().isEmpty())
			chosenCategory = friendsPlayer;
		
		return chosenCategory;
	}
}
