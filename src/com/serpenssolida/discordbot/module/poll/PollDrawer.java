package com.serpenssolida.discordbot.module.poll;

import com.serpenssolida.discordbot.image.ImageUtils;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.style.PieStyler;
import org.knowm.xchart.style.Styler;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;

public class PollDrawer
{
	private static final Color[] colors =
			{
					new Color(0x957d95),
					new Color(0x904c77),
					new Color(0xe49ab0),
					new Color(0xB5BA72),
					new Color(0xf0f2a6),
					new Color(0x8cd867),
					new Color(0x2fbf71),
					new Color(0x729EA1),
					new Color(0x84dccf),
					new Color(0x288AFF),
					new Color(0x132FB2),
					new Color(0x6D30E7),
					new Color(0xEE42C6),
					new Color(0xFF3F3F),
					new Color(0xEA8842),
					new Color(0xFFEC19),
					new Color(0x71E70F),
					new Color(0x294820),
					new Color(0x127528),
					new Color(0x0D7269),
					new Color(0x223552),
					new Color(0x3E124F),
			};
	
	private PollDrawer() {}
	
	/**
	 * Generate a pie chart picture displaying options and their votes.
	 *
	 * @return
	 * 		The picture generated as {@link ByteArrayOutputStream}.
	 */
	public static byte[] generatePieChart(PieChart chart)
	{
		PieStyler styler = chart.getStyler();
		
		//Set label style.
		styler.setLabelsFont(styler.getLabelsFont().deriveFont(Font.PLAIN, 24));
		styler.setLabelType(PieStyler.LabelType.Percentage);
		styler.setLegendVisible(false);
		
		//Remove background colors.
		Color transparent = new Color(0, 0, 0, 0);
		styler.setPlotBackgroundColor(transparent);
		styler.setChartBackgroundColor(transparent);
		styler.setChartTitleBoxBackgroundColor(transparent);
		styler.setPlotBorderVisible(false);
		
		//Generate image.
		BufferedImage pieChartImage = new BufferedImage(chart.getWidth(), chart.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
		chart.paint((Graphics2D) pieChartImage.getGraphics(), chart.getWidth(), chart.getHeight());
		
		//Convert it to bytes.
		return ImageUtils.toByteArray(pieChartImage);
	}
	
	/**
	 * Generate a pie chart legend displaying options and their votes.
	 *
	 * @return
	 * 		The picture generated as {@link ByteArrayOutputStream}.
	 */
	public static byte[] generatePieChartLegend(Poll poll, PieChart chart)
	{
		Font legendFont = chart.getStyler().getLegendFont().deriveFont(Font.PLAIN, 18);
		
		ArrayList<PollOptionChartEntry> pollOptionChartEntry = new ArrayList<>();
		Color[] seriesColors = chart.getStyler().getSeriesColors();
		
		//Create legend entries and sort them.
		int i = 0;
		for (Poll.PollOption option : poll.getOptionsCollection())
		{
			pollOptionChartEntry.add(new PollOptionChartEntry(option, seriesColors[i]));
			i++;
		}
		pollOptionChartEntry.sort(Comparator.comparingInt(entry -> -entry.option().getVotesCount()));
		
		//Create image.
		BufferedImage pieChartImage = new BufferedImage(300, 40 + (pollOptionChartEntry.size() - 1) * 35, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = pieChartImage.createGraphics();
		g.setFont(legendFont);
		
		//Draw legend.
		i = 0;
		for (PollOptionChartEntry option : pollOptionChartEntry)
		{
			//Truncate option text.
			String optionText = PollDrawer.truncateString(legendFont, option.option().getText(), g, 200);
			int votes = option.option().getVotesCount();
			
			//Draw color.
			g.setColor(option.color());
			g.fillRect(0, i * 35, 30, 30);
			
			//Draw option text.
			g.setColor(Color.lightGray);
			g.drawString(optionText, 35, 24 + i * 35);
			g.drawString("" + votes, 300 - 35, 24 + i * 35);
			i++;
		}
		
		//Convert it to bytes.
		return ImageUtils.toByteArray(pieChartImage);
	}
	
	public static ImmutablePair<byte[], byte[]> getPieChartImages(Poll poll)
	{
		PieChart chart = new PieChartBuilder()
				.width(512)
				.theme(Styler.ChartTheme.Matlab)
				.height(512)
				.build();
		
		//Set color palette.
		chart.getStyler().setSeriesColors(colors);
		
		//Add poll data to the chart.
		for (Poll.PollOption option : poll.getOptionsCollection())
			chart.addSeries(option.getId(), option.getVotesCount());
		
		byte[] pieChart = generatePieChart(chart);
		byte[] legend = generatePieChartLegend(poll, chart);
		
		return new ImmutablePair<>(pieChart, legend);
	}
	
	public static String truncateString(Font font, String text, Graphics2D g, int width)
	{
		FontMetrics fontMetrics = g.getFontMetrics(font);
		
		//Check if the size is already suitable.
		if (fontMetrics.stringWidth(text) < width)
			return text;
		
		//Get the maximun length of the string.
		String truncatedText = text;
		for (int i = text.length() - 1; i >= 0 ; i--)
		{
			truncatedText = text.substring(0, i) + "...";
			
			if (fontMetrics.stringWidth(truncatedText) < width)
				break;
		}
		
		return truncatedText;
	}
	
	private record PollOptionChartEntry(Poll.PollOption option, Color color) {}
}
