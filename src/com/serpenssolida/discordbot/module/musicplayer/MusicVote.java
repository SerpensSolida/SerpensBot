package com.serpenssolida.discordbot.module.musicplayer;

import net.dv8tion.jda.api.entities.User;

import java.util.HashSet;

public class MusicVote
{
	private final HashSet<User> votes = new HashSet<>();
	private int voteCount = 0;
	
	public int getVoteCount()
	{
		return this.voteCount;
	}
	
	public void setVoteCount(int voteCount)
	{
		this.voteCount = voteCount;
	}
	
	public void addVote(User user)
	{
		if (!this.votes.contains(user))
		{
			this.voteCount++;
			this.votes.add(user);
		}
	}
	
	public void clearVotes()
	{
		this.votes.clear();
		this.voteCount = 0;
	}
	
	public void removeVote(User user)
	{
		if (this.votes.contains(user))
		{
			this.voteCount--;
			this.votes.remove(user);
		}
	}
}
