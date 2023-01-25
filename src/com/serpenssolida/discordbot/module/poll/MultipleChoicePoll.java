package com.serpenssolida.discordbot.module.poll;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class MultipleChoicePoll extends Poll
{
	public MultipleChoicePoll(String question, User author, MessageChannel channel, boolean keepDown)
	{
		super(question, author, channel, keepDown);
	}
	
	@Override
	public boolean switchVote(String optionId, User user)
	{
		return false;
	}
	
	@Override
	public boolean addVote(String optionId, User user)
	{
		PollOption pollOption = this.getOptions().get(optionId);
		
		if (pollOption.getUsers().contains(user))
		{
			pollOption.removeVote(user);
			return true;
		}
		
		this.getUsers().add(user);
		pollOption.addVote(user);
		return true;
	}
	
	@Override
	public boolean removeVote(User user)
	{
		if (!this.getUsers().contains(user))
			return false;
		
		for (PollOption pollOption : this.getOptions().values())
		{
			if (!pollOption.getUsers().contains(user))
				continue;
			
			this.getUsers().remove(user);
			pollOption.removeVote(user);
		}
		
		return false;
	}
}
