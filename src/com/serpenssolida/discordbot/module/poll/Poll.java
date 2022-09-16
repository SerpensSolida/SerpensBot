package com.serpenssolida.discordbot.module.poll;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.*;
import java.util.stream.Stream;

public class Poll
{
	private final User author; //The author of the poll.
	private final HashSet<User> users = new HashSet<>(); //Users that added a vote in the poll.
	private final MessageChannel channel;
	private String messageID; //ID of the message the poll is in.
	private String question; //Question of the poll.
	private Map<String, PollOption> options = new LinkedHashMap<>(); //Options of the poll.
	private boolean keepDown;
	private boolean finished; //If the poll is finished.
	private int messageCount = 0; //Number of message that are below the poll.

	public Poll(String question, User author, MessageChannel channel, boolean keepDown)
	{
		this.question = question;
		this.author = author;
		this.channel = channel;
		this.keepDown = keepDown;
	}
	
	/**
	 * Return the winners of the poll.
	 * @return A list of winners of the poll.
	 */
	public List<PollOption> getWinners()
	{
		List<PollOption> winners = new ArrayList<>();
		Stream<PollOption> sorted = this.getOptions().parallelStream().sorted((o1, o2) -> o2.getVotesCount() - o1.getVotesCount());
		
		Optional<PollOption> topOptional = sorted.findFirst();
		
		if (topOptional.isEmpty())
			return winners;
		
		//Get the option with most votes.
		PollOption top = topOptional.get();
		winners.add(top);
		
		//Recreate the stream and find other option that may have the same vote as the top option.
		sorted = this.getOptions().stream().sorted((o1, o2) -> o2.getVotesCount() - o1.getVotesCount());
		for (PollOption pollOption : sorted.toList())
		{
			if (top == pollOption)
				continue;
			
			if (top.getVotesCount() == pollOption.getVotesCount())
				winners.add(pollOption);
			else
				break;
		}
		
		return winners;
	}
	
	/**
	 * Get a collection of {@link PollOption} of this poll.
	 * @return A collection of {@link PollOption}.
	 */
	public Collection<PollOption> getOptions()
	{
		return this.options.values();
	}
	
	/**
	 * Get the option with the given id.
	 * @param optionId The id of the option.
	 * @return The option with the given id.
	 */
	public PollOption getOption(String optionId)
	{
		return this.options.get(optionId);
	}
	
	/**
	 * Returns the sum of all votes of each option added together.
	 * @return All the votes added to the poll.
	 */
	public int getVotesCount()
	{
		return this.options.values().parallelStream().mapToInt(PollOption::getVotesCount).sum();
	}
	
	/**
	 * Get the percentage of votes added to the given option.
	 * @param optionId The option to get the percentage of.
	 * @return The percentage of vote of the given option (0 - 1).
	 */
	public float getPercent(String optionId)
	{
		int votesCount = this.getVotesCount();
		
		if (votesCount == 0)
			return 0;
		
		return ((float) this.options.get(optionId).getVotesCount()) / votesCount;
	}
	
	/**
	 * Add the given option to the poll.
	 * @param option The option to add to the poll.
	 */
	public void addOption(PollOption option)
	{
		this.options.put(option.getId(), option);
	}
	
	/**
	 * Remove the option with the given id from the poll.
	 * @param optionId The id of the option to remove from the poll.
	 * @return Whether the option was removed.
	 */
	public boolean removeOption(String optionId)
	{
		PollOption removed = this.options.remove(optionId);
		
		if (removed != null)
		{
			//Remove the users that voted this option from the users set.
			this.users.removeAll(removed.users);
		}
		
		//Rebuild hashmap.
		LinkedHashMap<String, PollOption> newOptions = new LinkedHashMap<>();
		int k = 1;
		for (PollOption option : this.options.values())
		{
			option.setId("option" + k);
			newOptions.put(option.getId(), option);
			k++;
		}
		this.options = newOptions;
		
		return removed != null;
	}
	
	/**
	 * Adds the vote of the given user to the given option.
	 * @param optionId The option to add the vote to.
	 * @param user The user who voted for the option.
	 * @return Whether the vote was correctly added.
	 */
	public boolean addVote(String optionId, User user)
	{
		if (this.users.contains(user))
			return false;
		
		PollOption pollOption = this.options.get(optionId);
		
		this.users.add(user);
		pollOption.addVote(user);
		return true;
	}
	
	public boolean removeVote(User user)
	{
		if (!this.users.contains(user))
			return false;
		
		for (PollOption pollOption : this.options.values())
		{
			if (pollOption.users.contains(user))
			{
				this.users.remove(user);
				pollOption.removeVote(user);
				return true;
			}
		}
		
		return false;
	}
	
	public boolean switchVote(String optionId, User user)
	{
		PollOption option = this.getVote(user);
		
		if (option.getId().equals(optionId))
			return false;
		
		this.removeVote(user);
		this.addVote(optionId, user);
		return true;
	}
	
	public PollOption getVote(User user)
	{
		if (!this.users.contains(user))
			return null;
		
		for (PollOption pollOption : this.options.values())
		{
			if (pollOption.users.contains(user))
			{
				return pollOption;
			}
		}
		
		return null;
	}
	
	public boolean hasUserVoted(User author)
	{
		return this.users.contains(author);
	}
	
	public String getMessageID()
	{
		return this.messageID;
	}
	
	public void setMessageID(String messageID)
	{
		this.messageID = messageID;
	}
	
	public MessageChannel getChannel()
	{
		return this.channel;
	}
	
	public String getQuestion()
	{
		return this.question;
	}
	
	public void setQuestion(String question)
	{
		this.question = question;
	}
	
	public void setFinished(boolean finished)
	{
		this.finished = finished;
	}
	
	public boolean isFinished()
	{
		return this.finished;
	}
	
	public User getAuthor()
	{
		return this.author;
	}
	
	public boolean isKeepDown()
	{
		return this.keepDown;
	}
	
	public void setKeepDown(boolean keepDown)
	{
		this.keepDown = keepDown;
	}
	
	public int getMessageCount()
	{
		return this.messageCount;
	}
	
	public void setMessageCount(int messageCount)
	{
		this.messageCount = messageCount;
	}
	
	public static class PollOption
	{
		private final String text;
		private String id;
		private int votesCount = 0;
		private final HashSet<User> users = new HashSet<>();
		
		public PollOption(String id, String text)
		{
			this.text = text;
			this.id = id;
		}
		
		public void addVote(User user)
		{
			if (this.users.contains(user))
				return;
			
			this.votesCount++;
			this.users.add(user);
		}
		
		public void removeVote(User user)
		{
			boolean removed = this.users.remove(user);
			
			if (removed)
				this.votesCount--;
			
		}
		
		public int getVotesCount()
		{
			return this.votesCount;
		}
		
		public String getText()
		{
			return this.text;
		}
		
		public String getId()
		{
			return this.id;
		}
		
		public void setId(String id)
		{
			this.id = id;
		}
		
		public Set<User> getUsers()
		{
			return this.users;
		}
	}
}
