package com.serpenssolida.discordbot.module.hungergames.inventory;

import com.serpenssolida.discordbot.RandomChoice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Inventory
{
	private final HashMap<Food, ItemStack<Food>> foods = new HashMap<>();
	private final HashMap<Weapon, ItemStack<Weapon>> weapons = new HashMap<>();
	
	public void removeItem(Item item)
	{
		if (item instanceof Weapon weapon)
		{
			ItemStack<Weapon> itemStack = this.weapons.get(weapon);
			if (itemStack != null)
			{
				itemStack.setNum(itemStack.getNum() - 1);
				if (itemStack.getNum() <= 0)
					this.weapons.remove(weapon);
			}
		}
		else if (item instanceof Food food)
		{
			ItemStack<Food> itemStack = this.foods.get(food);
			if (itemStack != null)
			{
				itemStack.setNum(itemStack.getNum() - 1);
				if (itemStack.getNum() <= 0)
					this.foods.remove(food);
			}
		}
	}
	
	public void addItem(Item item, int num)
	{
		if (item instanceof Weapon weapon)
		{
			if (this.weapons.containsKey(weapon))
			{
				ItemStack<Weapon> itemStack = this.weapons.get(item);
				itemStack.setNum(itemStack.getNum() + 1);
			}
			else
			{
				this.weapons.put(weapon, new ItemStack<>(weapon, num));
			}
		}
		else if (item instanceof Food food)
		{
			if (this.foods.containsKey(food))
			{
				ItemStack<Food> itemStack = this.foods.get(item);
				itemStack.setNum(itemStack.getNum() + 1);
			}
			else
			{
				this.foods.put(food, new ItemStack<>(food, num));
			}
		}
	}
	
	public List<Food> getFoods()
	{
		return new ArrayList<>(this.foods.keySet());
	}
	
	public List<Weapon> getWeapons()
	{
		return new ArrayList<>(this.weapons.keySet());
	}
	
	public List<Item> getItems()
	{
		ArrayList<Item> items = new ArrayList<>();
		
		items.addAll(this.foods.keySet());
		items.addAll(this.weapons.keySet());
		
		return items;
	}
	
	public String toString()
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Inventario: ");
		if (this.foods.size() <= 0 && this.weapons.size() <= 0)
		{
			stringBuilder.append("Vuoto.");
			return stringBuilder.toString();
		}
		for (Map.Entry<Food, ItemStack<Food>> foodEntry : this.foods.entrySet())
		{
			ItemStack<Food> stack = foodEntry.getValue();
			stringBuilder.append("" + stack + ", ");
		}
		for (Map.Entry<Weapon, ItemStack<Weapon>> foodEntry : this.weapons.entrySet())
		{
			ItemStack<Weapon> stack = foodEntry.getValue();
			stringBuilder.append("" + stack + ", ");
		}
		stringBuilder.replace(stringBuilder.length() - 2, stringBuilder.length() - 1, "");
		return stringBuilder.toString();
	}
	
	/**
	 * Returns a random item from the inventory.
	 *
	 * @return
	 * 		A random item inside the inventory.
	 */
	public Item getRandomItemFromInventory()
	{
		List<Item> items = this.getItems();
		
		return !items.isEmpty() ? (Item) RandomChoice.getRandom(items.toArray()) : null;
	}
}
