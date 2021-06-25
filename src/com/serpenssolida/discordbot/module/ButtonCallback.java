package com.serpenssolida.discordbot.module;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

public class ButtonCallback
{
	private String id;
	//private User user;
	//private MessageChannel channel;
	private ButtonAction action;
	
	public static boolean DELETE_MESSAGE = true;
	public static boolean LEAVE_MESSAGE = false;
	
	public ButtonCallback(String id, User user, MessageChannel channel, ButtonAction action)
	{
		this.id = id;
		//this.user = user;
		//this.channel = channel;
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
	
	/*public User getUser()
	{
		return this.user;
	}
	
	public void setUser(User user)
	{
		this.user = user;
	}*/
	
	/*public MessageChannel getChannel()
	{
		return this.channel;
	}
	
	public void setChannel(MessageChannel channel)
	{
		this.channel = channel;
	}*/
	
	public ButtonAction getAction()
	{
		return this.action;
	}
	
	public void setAction(ButtonAction action)
	{
		this.action = action;
	}
}
