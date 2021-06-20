package com.serpenssolida.discordbot;

import com.serpenssolida.discordbot.module.ButtonCallback;
import net.dv8tion.jda.api.entities.User;

import java.util.HashMap;

public class ButtonGroup
{
	private HashMap<String, ButtonCallback> buttons = new HashMap<>();
	private User user;
	
	public ButtonGroup(User user)
	{
	    this.user = user;
	}
	
	public void addButton(ButtonCallback button)
	{
		this.buttons.put(button.getId(), button);
	}
	
	public ButtonCallback getButton(String id)
	{
		return this.buttons.get(id);
	}
	
	public void removeButton(String id)
	{
		this.buttons.remove(id);
	}
	
	public User getUser()
	{
		return this.user;
	}
	
	public void setUser(User user)
	{
		this.user = user;
	}
}
