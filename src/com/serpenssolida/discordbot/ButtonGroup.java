package com.serpenssolida.discordbot;

import com.serpenssolida.discordbot.module.ButtonCallback;

import java.util.HashMap;

/**
 * This class represent the group of button (their callbacks) of a message.
 */
public class ButtonGroup
{
	private HashMap<String, ButtonCallback> buttons = new HashMap<>();
	
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
}
