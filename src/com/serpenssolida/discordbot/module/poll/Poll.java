package com.serpenssolida.discordbot.module.poll;

import net.dv8tion.jda.api.entities.User;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Poll
{
	private String messageId; //Id of the message the poll is in.
	private String question; //Question of the poll.
	private boolean finished; //If the poll is finished.
	private User author; //The author of the poll.
	private LinkedHashMap<String, PollOption> options = new LinkedHashMap<>(); //Options of the poll.
	private HashSet<User> users = new HashSet<>(); //Users that added a vote in the poll.

	public Poll(String question, User author)
	{
		this.question = question;
		this.author = author;
	}
	
	/**
	 * Return the winners of the poll.
	 * @return A list of winners of the poll.
	 */
	public ArrayList<PollOption> getWinners()
	{
		ArrayList<PollOption> winners = new ArrayList<>();
		Stream<PollOption> sorted = this.getOptions().parallelStream().sorted((o1, o2) -> o2.getVotesCount() - o1.getVotesCount());
		
		Optional<PollOption> topOptional = sorted.findFirst();
		
		if (topOptional.isEmpty())
			return winners;
		
		//Get the option with most votes.
		PollOption top = topOptional.get();
		winners.add(top);
		
		//Recreate the stream and find other option that may have the same vote as the top option.
		sorted = this.getOptions().parallelStream().sorted((o1, o2) -> o2.getVotesCount() - o1.getVotesCount());
		for (PollOption pollOption : sorted.collect(Collectors.toList()))
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
	 * Generate a pie chart picture displaying options and their votes.
	 * @return The picture generated as {@link ByteArrayOutputStream}.
	 */
	public ByteArrayOutputStream generatePieChart() //TODO: find a better one.
	{
		//Get dataset values.
		DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
		
		for (PollOption option : this.getOptions())
		{
			dataset.setValue(option.getText() + "\n" +( this.getPercent(option.getId()) * 100) + "% (voti: " + option.getVotesCount() + ")", this.getPercent(option.getId()));
		}
		
		//Create pie chart image.
		JFreeChart chart = ChartFactory.createPieChart(null, dataset, false, true, false);
		chart.setBorderVisible(false);
		chart.setBackgroundPaint(null);
		BufferedImage pieChartImage = chart.createBufferedImage(512, 512, 512, 512, new ChartRenderingInfo());
		
		//Convert it to bytes.
		ByteArrayOutputStream outputStream = null;
		
		try
		{
			outputStream = new ByteArrayOutputStream();
			ImageIO.write(pieChartImage, "png", outputStream);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return outputStream;
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
	 * @return Whether or not the option was removed.
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
		LinkedHashMap<String, PollOption> options = new LinkedHashMap<>();
		int k = 1;
		for (PollOption option : this.options.values())
		{
			option.setId("option" + k);
			options.put(option.getId(), option);
			k++;
		}
		this.options = options;
		
		return removed != null;
	}
	
	/**
	 * Adds the vote of the given user to the given option.
	 * @param optionId The option to add the vote to.
	 * @param user The user who voted for the option.
	 * @return Whether or not the vote was correctly added.
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
	
	public String getMessageId()
	{
		return this.messageId;
	}
	
	public void setMessageId(String messageId)
	{
		this.messageId = messageId;
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
	
	public static class PollOption
	{
		private String text;
		private String id;
		private int votesCount = 0;
		private HashSet<User> users = new HashSet<>();
		
		public PollOption(String id, String text)
		{
			this.text = text;
			this.id = id;
		}
		
		public boolean addVote(User user)
		{
			if (this.users.contains(user))
				return false;
			
			this.votesCount++;
			this.users.add(user);
			return true;
		}
		
		public boolean removeVote(User user)
		{
			boolean removed = this.users.remove(user);
			
			if (removed)
				this.votesCount--;
			
			return removed;
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
		
		public HashSet<User> getUsers()
		{
			return this.users;
		}
	}
}
