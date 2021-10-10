package com.serpenssolida.discordbot.module.poll;

import com.serpenssolida.discordbot.image.ImageUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class PollDrawer
{
	
	private PollDrawer() {}
	/**
	 * Generate a pie chart picture displaying options and their votes.
	 * @return The picture generated as {@link ByteArrayOutputStream}.
	 */
	public static byte[] generatePieChart(Poll poll) //TODO: find a better one.
	{
		//Get dataset values.
		DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
		
		for (Poll.PollOption option : poll.getOptions())
		{
			dataset.setValue(option.getText() + "\n" +(poll.getPercent(option.getId()) * 100) + "% (voti: " + option.getVotesCount() + ")", poll.getPercent(option.getId()));
		}
		
		//Create pie chart image.
		JFreeChart chart = ChartFactory.createPieChart(null, dataset, false, true, false);
		chart.setBorderVisible(false);
		chart.setBackgroundPaint(null);
		BufferedImage pieChartImage = chart.createBufferedImage(512, 512, 512, 512, new ChartRenderingInfo());
		
		//Convert it to bytes.
		return ImageUtils.toByteArray(pieChartImage);
	}
}
