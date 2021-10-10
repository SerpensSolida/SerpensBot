package com.serpenssolida.discordbot.module.hungergames;

import com.serpenssolida.discordbot.avatar.AvatarCache;
import com.serpenssolida.discordbot.image.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

public class HungerGameStatusImageDrawer
{
	private static final int AVATAR_SIZE = 64; //Size of the avatars.
	private static BufferedImage tombImage; //Size of the avatars.
	
	private HungerGameStatusImageDrawer() {}
	
	/**
	 * @return The status image of the turn.
	 */
	public static byte[] generateStatusImage(HungerGames hg)
	{
		//List of players that participated in the turn.
		HashSet<Player> involvedPlayers = hg.getInvolvedPlayers();
		int avatarNum = involvedPlayers.size(); //Count of the avatars.
		
		//Calculate image dimensions.
		int imageWidth = (avatarNum > 6) ? (6 * AVATAR_SIZE) : (avatarNum * AVATAR_SIZE);
		int imageHeight = (avatarNum / 6 + ((avatarNum % 6 == 0) ? 0 : 1)) * AVATAR_SIZE;
		
		//Create the image and get its Graphics.
		BufferedImage statusImage = new BufferedImage(imageWidth, imageHeight, 6);
		Graphics g = statusImage.getGraphics();
		
		//Draw players.
		HungerGameStatusImageDrawer.drawInvolvedPlayers(g, involvedPlayers);
		
		return ImageUtils.toByteArray(statusImage);
	}
	
	private static void drawInvolvedPlayers(Graphics g, HashSet<Player> involvedPlayers)
	{
		int i = 0;
		for (Player involvedPlayer : involvedPlayers)
		{
			//Position of the avatar.
			int posX = i % 6 * AVATAR_SIZE;
			int posY = i / 6 * AVATAR_SIZE;
			
			HungerGameStatusImageDrawer.drawPlayerStatus(g, involvedPlayer, posX, posY);
			
			i++;
		}
	}
	
	private static void drawPlayerStatus(Graphics g, Player involvedPlayer, int posX, int posY)
	{
		//Place the avatar into the image.
		BufferedImage avatar = AvatarCache.getAvatar(involvedPlayer.getOwner());
		Image avatarImage = avatar.getScaledInstance(AVATAR_SIZE, AVATAR_SIZE, 4);
		g.drawImage(avatarImage, posX, posY, null);
		
		//Paint the avatar red if the player died during this turn.
		if (involvedPlayer.isDead())
		{
			g.setColor(new Color(255, 0, 0, 100));
			g.fillRect(posX, posY, AVATAR_SIZE, AVATAR_SIZE);
			
			Image tomb = HungerGameStatusImageDrawer.getTombImage().getScaledInstance(AVATAR_SIZE / 2, AVATAR_SIZE / 2, 4);
			g.drawImage(tomb, posX, posY + AVATAR_SIZE - AVATAR_SIZE / 2 - AVATAR_SIZE / 5, null);
		}
		
		//Draw HP remaining.
		g.setColor(new Color(197, 197, 197, 160));
		g.fillRect(posX, posY + AVATAR_SIZE - AVATAR_SIZE / 5, AVATAR_SIZE, AVATAR_SIZE / 5);
		g.setColor(Color.black);
		g.setFont(HungerGamesController.font);
		g.drawString("HP: " + (int) involvedPlayer.getHealth(), posX + 4, posY + AVATAR_SIZE - AVATAR_SIZE / 10 + 5);
	}
	
	private static BufferedImage getTombImage()
	{
		if (HungerGameStatusImageDrawer.tombImage == null)
		{
			HungerGameStatusImageDrawer.tombImage = HungerGameStatusImageDrawer.loadTombImage();
		}
		
		return HungerGameStatusImageDrawer.tombImage;
	}
	
	private static BufferedImage loadTombImage()
	{
		File tombFile = new File(HungerGamesController.FOLDER + "/tombstone.png");
		
		try
		{
			return ImageIO.read(tombFile);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
	}
}
