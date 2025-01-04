package com.serpenssolida.discordbot.module.reminder;

import java.util.*;

public class ReminderData
{
    private Map<String, Reminder> reminders = new HashMap<>();
    
    public ReminderData(Map<String, Reminder> reminders)
    {
        this.reminders.putAll(reminders);
    }
    
    public Map<String, Reminder> getReminders()
    {
        return this.reminders;
    }
    
    public void setReminders(Map<String, Reminder> reminders)
    {
        this.reminders = reminders;
    }
}
