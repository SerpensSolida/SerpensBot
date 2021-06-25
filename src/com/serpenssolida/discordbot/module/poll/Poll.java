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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class Poll
{
	private String messageId;
	private HashMap<String, PollOption> options = new HashMap<>();
	private HashSet<User> users = new HashSet<>();
	
	public Poll(String messageId)
	{
		this.messageId = messageId;
	}
	
	public Poll()
	{

	}
	
	public ByteArrayOutputStream generatePieChart()
	{
		//Get dataset values.
		DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
		
		for (PollOption option : this.getOptions())
		{
			dataset.setValue(option.getText(), this.getPercent(option.getId()));
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
	
	public float getPercent(String optionId)
	{
		int votesCount = this.options.values().parallelStream().mapToInt(PollOption::getVotesCount).sum();
		
		if (votesCount == 0)
			return 0;
		
		return ((float) this.options.get(optionId).getVotesCount()) / votesCount;
	}
	
	public void addOption(PollOption option)
	{
		this.options.put(option.getId(), option);
	}
	
	public boolean addVote(String optionId, User user)
	{
		if (this.users.contains(user))
			return false;
		
		this.users.add(user);
		this.options.get(optionId).votesCount++;
		return true;
	}
	
	public String getMessageId()
	{
		return this.messageId;
	}
	
	public void setMessageId(String messageId)
	{
		this.messageId = messageId;
	}
	
	public static class PollOption
	{
		private String text;
		private String id;
		private int votesCount = 0;
		
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
	}
}
