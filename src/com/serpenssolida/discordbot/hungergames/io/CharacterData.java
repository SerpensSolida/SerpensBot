package com.serpenssolida.discordbot.hungergames.io;

import com.serpenssolida.discordbot.hungergames.Character;

import java.io.Serializable;
import java.util.HashMap;

public class CharacterData implements Serializable
{
	private HashMap<String, Character> characters;
	
	public HashMap<String, Character> getCharacters()
	{
		return this.characters;
	}
	
	public void setCharacters(HashMap<String, Character> characters)
	{
		this.characters = characters;
	}
}
