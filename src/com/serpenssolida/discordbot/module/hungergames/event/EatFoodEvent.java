package com.serpenssolida.discordbot.module.hungergames.event;

import com.serpenssolida.discordbot.RandomChoice;
import com.serpenssolida.discordbot.module.hungergames.HungerGames;
import com.serpenssolida.discordbot.module.hungergames.Player;
import com.serpenssolida.discordbot.module.hungergames.inventory.Food;

import java.util.ArrayList;
import java.util.HashSet;

public class EatFoodEvent extends HungerGamesEvent
{
	private String[] noFoodMessages = new String[] {
			"user sta morendo di fame, amountHP.",
			"la fame sta assalendo user, amountHP.",
			"user non ci vede più dalla fame, amountHP.",
			"i morsi della fame attanagliano user, amountHP.",
			"user pensa a quando poteva abbufarsi di babbà, amountHP.",
			"user ha un buco allo stomaco, amountHP.",
			"user è in crisi di astinenza da cibo, amountHP."
	};
	
	private String[] fullhealthMessage = new String[] {
			"user si sente sazio.",
			"user potrebbe stare senza mangiare per giorni.",
			"user ha la pancia piena.",
			"user non ha voglia di mangiare.",
			"user è pieno e non mangia niente.",
			"user non ha appetito."
	};
	
	public EventResult doEvent(HungerGames hg)
	{
		StringBuilder stringBuilder = new StringBuilder();
		HashSet<Player> alivePlayers = hg.getAlivePlayers();
		HashSet<Player> deadPlayers = hg.getDeadPlayers();
		HashSet<Player> healedPlayers = hg.getHealedPlayers();
		HashSet<Player> involvedPlayers = hg.getInvolvedPlayers();
		
		//List of players that did not heal during this turn.
		HashSet<Player> notHeleadPlayers = new HashSet<>(alivePlayers);
		notHeleadPlayers.removeAll(healedPlayers);
		
		if (notHeleadPlayers.isEmpty())
			return new EventResult("", EventResult.State.Failed); //Quit event.
		
		//Get a random player.
		Player player = (Player) RandomChoice.getRandom(notHeleadPlayers.toArray());
		
		//List of foods from player's inventory.
		ArrayList<Food> foodList = player.getInventory().getFoods();
		
		if (foodList.isEmpty())
		{
			//If the players is hungry but does not have any food there is a chance that he'll get damage.
			
			if (RandomChoice.random.nextInt(10) > 0)
				return new EventResult("", EventResult.State.Successful); //Quit event.
			
			//Damage dealt to the player.
			float f = -(1 + RandomChoice.random.nextInt(5));
			
			String message = (String) RandomChoice.getRandom(this.noFoodMessages);
			message = message.replaceAll("user", "**" + player + "**");
			message = message.replaceAll("amount", "" + (int) f);
			stringBuilder.append(message + "\n");
			
			//Deal damage to the player.
			player.setHealth(player.getHealth() + f);
			
			healedPlayers.add(player);
			involvedPlayers.add(player);
			
			if (player.isDead())
			{
				alivePlayers.remove(player);
				deadPlayers.add(player);
				
				player.removeRelationships();
				
				stringBuilder.append("**" + player + "** è morto.\n");
			}
			
			return new EventResult(stringBuilder.toString(), EventResult.State.Successful);
		}
		
		if (player.getHealth() >= player.getMaxHealth())
		{
			//The players does not have to heal if he is at full health.
			
			if (RandomChoice.random.nextInt(10) > 0)
				return new EventResult("", EventResult.State.Successful); //Quit event.
			
			String message = (String) RandomChoice.getRandom(this.fullhealthMessage);
			message = message.replaceAll("user", "**" + player + "**");
			stringBuilder.append(message + "\n");
			
			healedPlayers.add(player);
			involvedPlayers.add(player);
			
			return new EventResult(stringBuilder.toString(), EventResult.State.Successful);
		}
		
		//Get a random food and its healing value.
		Food food = (Food) RandomChoice.getRandom(foodList.toArray());
		float hpHealed = food.getHpRestored() + food.getHpRestored() * player.getCharacter().getTaste() / 20.0F;
		hpHealed += hpHealed * (RandomChoice.random.nextFloat() - 0.5F) * 0.2F;
		
		//Heal the player.
		player.setHealth(player.getHealth() + hpHealed);
		
		//Remove the food from the inventory.
		player.getInventory().removeItem(food, 1);
		
		stringBuilder.append(this.replace(food, player, hpHealed) + "\n");
		
		healedPlayers.add(player);
		involvedPlayers.add(player);
		
		if (player.isDead())
		{
			alivePlayers.remove(player);
			deadPlayers.add(player);
			player.removeRelationships();
			stringBuilder.append("**" + player + "** è morto.\n");
		}
		
		return new EventResult(stringBuilder.toString(), EventResult.State.Successful);
	}
	
	private String replace(Food food, Player player, float hpHealed)
	{
		String useMessage = food.getUseMessage();
		
		useMessage = useMessage.replaceAll("user", "**" + player + "**");
		useMessage = useMessage.replaceAll("food", "*" + food.getName() + "*");
		useMessage = useMessage.replaceAll("amount", "" + (int) hpHealed);
		return useMessage;
	}
}
