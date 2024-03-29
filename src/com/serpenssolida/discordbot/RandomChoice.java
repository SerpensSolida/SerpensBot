package com.serpenssolida.discordbot;

import java.util.Random;

public class RandomChoice
{
	private static Random random = new Random();
	
	private RandomChoice() {}
	
	/**
	 * Reset the RNG.
	 */
	public static void resetRandom()
	{
		random = new Random();
	}
	
	public static Random getRandom()
	{
		return random;
	}
	
	/**
	 * Get a random object from the give array, the object is chosen randomly based on the give probability.
	 *
	 * @param data
	 * 		Array of object the method will choose from.
	 * @param probabilities
	 * 		Array of weights that will determine the weight of the object inside the array.
	 *
	 * @return A random object from the array.
	 */
	public static Object getRandomWithProbability(Object[] data, float[] probabilities)
	{
		if (data.length != probabilities.length)
			throw new IllegalArgumentException("data and probabilites must be of the same size");
		
		float totalProbability = 0;
		
		for (float probability : probabilities)
		{
			totalProbability += probability;
		}
		
		int i = 0;
		while (random.nextFloat() * totalProbability > probabilities[i] && i < probabilities.length)
		{
			totalProbability -= probabilities[i];
			i++;
		}
		return data[i];
	}
	
	/**
	 * Get a random object of the array.
	 *
	 * @param data
	 * 		The set of object.
	 *
	 * @return A random object inside the given array.
	 */
	public static Object getRandom(Object[] data)
	{
		return data[random.nextInt(data.length)];
	}
	
	public static boolean randomChance(float probability)
	{
		return random.nextFloat() * 100 <= probability;
	}
}
