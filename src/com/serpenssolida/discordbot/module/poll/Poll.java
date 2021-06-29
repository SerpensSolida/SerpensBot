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
	private String messageId;
	private String question;
	private boolean finished;
	private User author;
	private LinkedHashMap<String, PollOption> options = new LinkedHashMap<>();
	private HashSet<User> users = new HashSet<>();
	
	/*public Poll(String messageId)
	{
		this.messageId = messageId;
	}*/
	
	public Poll(String question, User author)
	{
		this.question = question;
		this.author = author;
	}
	
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
	
	public Collection<PollOption> getOptions()
	{
		return this.options.values();
	}
	
	public int getVotesCount()
	{
		return this.options.values().parallelStream().mapToInt(PollOption::getVotesCount).sum();
	}
	
	public float getPercent(String optionId)
	{
		int votesCount = this.getVotesCount();
		
		if (votesCount == 0)
			return 0;
		
		return ((float) this.options.get(optionId).getVotesCount()) / votesCount;
	}
	
	public void addOption(PollOption option)
	{
		this.options.put(option.getId(), option);
	}
	
	public boolean removeOption(String optionId)
	{
		PollOption removed = this.options.remove(optionId);
		
		if (removed != null)
		{
			this.users.removeAll(removed.users);
		}
		
		LinkedHashMap<String, PollOption> options = new LinkedHashMap<>();
		int k = 1;
		for (PollOption option : this.options.values())
		{
			option.setId("option" + k);
			options.put(option.getId(), option);
			k++;
		}
		this.options = options;
		//TODO: rebuild hashmap or maybe change it to an arraylist.
		
		return removed != null;
	}
	
	public boolean addVote(String optionId, User user)
	{
		if (this.users.contains(user))
			return false;
		
		PollOption pollOption = this.options.get(optionId);
		
		this.users.add(user);
		pollOption.votesCount++;
		pollOption.users.add(user);
		return true;
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
