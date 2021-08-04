package com.serpenssolida.discordbot.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageUtils
{
	public static byte[] toByteArray(BufferedImage image)
	{
		ByteArrayOutputStream outputStream;
		
		try
		{
			outputStream = new ByteArrayOutputStream();
			ImageIO.write(image, "png", outputStream);
		}
		catch (IOException e)
		{
			outputStream = null;
			e.printStackTrace();
		}
		
		return outputStream != null ? outputStream.toByteArray() : null;
	}
}
