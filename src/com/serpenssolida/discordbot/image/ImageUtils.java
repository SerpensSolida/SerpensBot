package com.serpenssolida.discordbot.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageUtils
{
	private static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);
	
	private ImageUtils() {}
	
	/**
	 * Converts the given BufferedImage into a byte array.
	 *
	 * @param image
	 * 		The image to be converted.
	 *
	 * @return
	 * 		The image as byte array.
	 */
	public static byte[] toByteArray(BufferedImage image)
	{
		ByteArrayOutputStream outputStream;
		
		try
		{
			//Create the stream and add the image to it.
			outputStream = new ByteArrayOutputStream();
			ImageIO.write(image, "png", outputStream);
		}
		catch (IOException e)
		{
			logger.error("", e);
			return null;
		}
		
		//Convert the stream to byte array and return it.
		return outputStream.toByteArray();
	}
}
