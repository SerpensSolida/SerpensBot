package com.serpenssolida.discordbot.module.hungergames.io;

import com.serpenssolida.discordbot.module.hungergames.Character;

import java.util.Map;

public class CharacterData
{
	private Map<String, Character> characters;
	
	public Map<String, Character> getCharacters()
	{
		return this.characters;
	}
	
	public void setCharacters(Map<String, Character> characters)
	{
		this.characters = characters;
	}
}
