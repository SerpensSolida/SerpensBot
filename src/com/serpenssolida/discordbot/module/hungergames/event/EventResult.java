package com.serpenssolida.discordbot.module.hungergames.event;

public class EventResult
{
	private String eventMessage;
	private State state;
	
	public enum State
	{
		Successful, Failed
	}
	
	public EventResult(String eventMessage, State state)
	{
		this.eventMessage = eventMessage;
		this.state = state;
	}
	
	public String getEventMessage()
	{
		return this.eventMessage;
	}
	
	public void setEventMessage(String eventMessage)
	{
		this.eventMessage = eventMessage;
	}
	
	public State getState()
	{
		return this.state;
	}
	
	public void setState(State state)
	{
		this.state = state;
	}
	
	
	public String toString()
	{
		return this.eventMessage;
	}
}
