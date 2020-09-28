package com.serpenssolida.discordbot.avatar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import javax.imageio.ImageIO;

import net.dv8tion.jda.api.entities.User;

public class AvatarCache
{
	/**
	 * Get the give user's avatar from the cache.
	 *
	 * @param user
	 * 		The user whose avatar is to be retrieved.
	 *
	 * @return The avatar.
	 */
	public static BufferedImage getAvatar(User user)
	{
		try
		{
			AvatarData data = loadCache();
			Avatar avatar = data.avatars.get(user.getAvatarId());
			
			if (avatar != null)
			{
				//The avatar was found in the cache.
				return ImageIO.read(new File(avatar.file));
			}
			
			//The avatar was not found in the cache, download it.
			return downloadAvatar(user);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	private static BufferedImage downloadAvatar(User user) throws IOException
	{
		if (user.getAvatarUrl() == null) return null;
		
		URL url = new URL(user.getAvatarUrl());
		File file = new File("avatar_cache/" + user.getAvatarId() + ".png");
		
		BufferedImage bufferedAvatar = ImageIO.read(url);
		
		AvatarData data = loadCache();
		
		if (!file.exists())
		{
			file.getParentFile().mkdirs();
			file.createNewFile();
		}
		
		ImageIO.write(bufferedAvatar, "png", file);
		
		Avatar avatar = new Avatar(user.getId(), user.getAvatarId(), user.getAvatarUrl(), file.getPath());
		data.avatars.put(user.getAvatarId(), avatar);
		AvatarCache.saveCache(data);
		
		return bufferedAvatar;
	}
	
	private static AvatarData loadCache() throws IOException
	{
		File file = new File("avatar_cache/avatar.json");
		Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
		
		BufferedReader reader;
		
		if (!file.exists())
		{
			file.getParentFile().mkdirs();
			file.createNewFile();
		}
		
		reader = new BufferedReader(new FileReader(file));
		AvatarData data = gson.fromJson(reader, AvatarData.class);
		
		if (data == null)
		{
			data = new AvatarData();
		}
		
		return data;
	}
	
	private static void saveCache(AvatarData data) throws IOException
	{
		File file = new File("avatar_cache/avatar.json");
		Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
		
		try (PrintWriter writer = new PrintWriter(new FileWriter(file)))
		{
			writer.write(gson.toJson(data));
			
		}
		catch (FileNotFoundException e)
		{
			file.getParentFile().mkdirs();
			if (file.createNewFile())
				saveCache(data);
		}
	}
}
