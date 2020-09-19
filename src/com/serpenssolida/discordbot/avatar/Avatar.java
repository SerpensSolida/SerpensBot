package com.serpenssolida.discordbot.avatar;

public class Avatar {
  public String ownerID;
  public String url;
  public String id;
  public String file;
  
  public Avatar(String ownerID, String id, String url, String file) {
    this.file = file;
    this.url = url;
    this.id = id;
    this.ownerID = ownerID;
  }
}
