package com.serpenssolida.discordbot.module.reminder;

import net.dv8tion.jda.api.entities.User;
import org.quartz.DateBuilder;

import java.util.Date;
import java.util.List;

public class Reminder
{
    private String authorID;
    private String channelID;
    private String message;
    private Date date;
    private List<User> mentions;
    private DateBuilder.IntervalUnit intervalUnit;
    private int intervalValue;
    private boolean repeat;
    
    public Reminder(String author, String channelID, String message, Date date, boolean repeat, List<User> mentions, DateBuilder.IntervalUnit intervalUnit, int intervalValue)
    {
        this.authorID = author;
        this.channelID = channelID;
        this.message = message;
        this.date = date;
        this.repeat = repeat;
        this.mentions = mentions;
        this.intervalUnit = intervalUnit;
        this.intervalValue = intervalValue;
    }
    
    public String getAuthorID()
    {
        return this.authorID;
    }
    
    public void setAuthorID(String authorID)
    {
        this.authorID = authorID;
    }
    
    public String getChannelID()
    {
        return this.channelID;
    }
    
    public void setChannelID(String channelID)
    {
        this.channelID = channelID;
    }
    
    public String getMessage()
    {
        return this.message;
    }
    
    public void setMessage(String message)
    {
        this.message = message;
    }
    
    public boolean isRepeat()
    {
        return this.repeat;
    }
    
    public void setRepeat(boolean repeat)
    {
        this.repeat = repeat;
    }
    
    public Date getDate()
    {
        return this.date;
    }
    
    public void setDate(Date date)
    {
        this.date = date;
    }
    
    public List<User> getMentions()
    {
        return this.mentions;
    }
    
    public void setMentions(List<User> mentions)
    {
        this.mentions = mentions;
    }
    
    public DateBuilder.IntervalUnit getIntervalUnit()
    {
        return this.intervalUnit;
    }
    
    public void setIntervalUnit(DateBuilder.IntervalUnit intervalUnit)
    {
        this.intervalUnit = intervalUnit;
    }
    
    public int getIntervalValue()
    {
        return this.intervalValue;
    }
    
    public void setIntervalValue(int intervalValue)
    {
        this.intervalValue = intervalValue;
    }
}
