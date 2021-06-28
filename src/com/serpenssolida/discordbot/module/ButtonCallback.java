package com.serpenssolida.discordbot.module;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

/**
 * This class is used to map a Discord button to an action.
 */
public class ButtonCallback
{
	private String id;
	private ButtonAction action;
	
	public static boolean DELETE_MESSAGE = true;
	public static boolean LEAVE_MESSAGE = false;
	
	public ButtonCallback(String id, ButtonAction action)
	{
		this.id = id;
		this.action = action;
	}
	
	/**
	 * Calls the callback of the button, if no collback is set the function return false.
	 *
	 * @param event
	 * 		The event being performed.
	 */
	public boolean doAction(ButtonClickEvent event)
	{
		if (this.action != null)
		{
			return this.action.doAction(event, event.getGuild(), event.getChannel(), event.getMessage(), event.getUser());
		}
		
		System.err.println("Action not set for command: " + this.getId());
		
		return false;
	}
	
	public String getId()
	{
		return this.id;
	}
	
	public void setId(String id)
	{
		this.id = id;
	}
	
	public ButtonAction getAction()
	{
		return this.action;
	}
	
	public void setAction(ButtonAction action)
	{
		this.action = action;
	}
}
