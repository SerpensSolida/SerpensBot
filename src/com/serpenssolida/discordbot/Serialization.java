package com.serpenssolida.discordbot;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

@Deprecated
public class Serialization
{
	public static String serialize(Object obj)
	{
		String str = null;
		try
		{
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(byteArrayOutputStream);
			oos.writeObject(obj);
			oos.close();
			str = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return str;
	}
	
	public static Object deserialize(String string)
	{
		Object obj = null;
		try
		{
			byte[] d = Base64.getDecoder().decode(string);
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(d));
			obj = ois.readObject();
			ois.close();
		}
		catch (IOException | ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		return obj;
	}
}
