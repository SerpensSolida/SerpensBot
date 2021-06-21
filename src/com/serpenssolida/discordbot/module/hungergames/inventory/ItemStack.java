package com.serpenssolida.discordbot.module.hungergames.inventory;

public class ItemStack<T extends Item>
{
	private int num;
	private T item;
	
	public ItemStack(T item, int num)
	{
		this.item = item;
		this.num = num;
	}
	
	public int getNum()
	{
		return this.num;
	}
	
	public void setNum(int num)
	{
		this.num = num;
	}
	
	public T getItem()
	{
		return this.item;
	}
	
	public void setItem(T item)
	{
		this.item = item;
	}
	
	public String toString()
	{
		StringBuilder stringBuilder = new StringBuilder();
		if (this.getNum() > 1)
			stringBuilder.append(this.getNum())
					.append("x ");
		stringBuilder.append(this.getItem().name);
		return stringBuilder.toString();
	}
}
