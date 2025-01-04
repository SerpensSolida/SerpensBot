package com.serpenssolida.discordbot.module.reminder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ReminderController
{
    private static final Logger logger = LoggerFactory.getLogger(ReminderController.class);
    
    private static ReminderController instance;
    
    private final Scheduler scheduler;
    private final Map<String, Reminder> reminders = new HashMap<>();
    
    public ReminderController()
    {
        try
        {
            this.scheduler = new StdSchedulerFactory().getScheduler();
            this.scheduler.start();
        }
        catch (SchedulerException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    public static ReminderController getInstance()
    {
        if (ReminderController.instance == null)
            ReminderController.instance = new ReminderController();
        
        return ReminderController.instance;
    }
    
    public boolean deleteReminder(String channelID)
    {
        return this.reminders.remove(channelID) != null;
    }
    
    public void addReminder(Reminder reminder)
    {
        this.reminders.put(reminder.getChannelID(), reminder);
        this.saveReminders();
    }
    
    /**
     * Loads reminder data.
     */
    public void loadReminders()
    {
        File fileCharacters = new File(Paths.get("global_data", "reminder",  "reminder.json").toString());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        
        logger.info("Cariamento dei reminder.");
        
        //Load data from file.
        try (BufferedReader reader = new BufferedReader(new FileReader(fileCharacters)))
        {
            ReminderData reminderData = gson.fromJson(reader, ReminderData.class);
            this.reminders.putAll(reminderData.getReminders());
            
            for (Reminder reminder : this.reminders.values())
            {
                this.scheduleReminder(reminder);
            }
        }
        catch (FileNotFoundException e)
        {
            logger.info("File dati dei reminder non trovato.");
        }
        catch (IOException e)
        {
            logger.error("", e);
        }
    }
    
    /**
     * Save reminder data.
     */
    public void saveReminders()
    {
        File forumFile = new File(Paths.get("global_data", "reminder",  "reminder.json").toString());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        
        logger.info("Salvataggio/aggiornamento dei reminder.");
        
        //Save data to file.
        try (PrintWriter writer = new PrintWriter(new FileWriter(forumFile)))
        {
            ReminderData reminderData = new ReminderData(this.reminders);
            writer.println(gson.toJson(reminderData));
        }
        catch (FileNotFoundException e)
        {
            try
            {
                forumFile.getParentFile().mkdirs();
                
                if (forumFile.createNewFile())
                    this.saveReminders();
            }
            catch (IOException ex)
            {
                logger.error("", ex);
            }
        }
        catch (IOException e)
        {
            logger.error("", e);
        }
    }
    
    public void scheduleReminder(Reminder reminder)
    {
        JobDataMap map = new JobDataMap();
        map.put("reminder", reminder);
        
        JobDetail job = JobBuilder.newJob(SendReminderJob.class)
                .usingJobData(map)
                .build();
        
        TriggerBuilder<Trigger> trigger = TriggerBuilder.newTrigger()
                .forJob(job)
                .startAt(reminder.getDate());
        
        if (reminder.getIntervalUnit() != null)
        {
            CalendarIntervalScheduleBuilder schedule = CalendarIntervalScheduleBuilder.calendarIntervalSchedule()
                    .withInterval(reminder.getIntervalValue(), reminder.getIntervalUnit());
            
            trigger.withSchedule(schedule);
        }
        
        try
        {
            this.scheduler.scheduleJob(job, trigger.build());
        }
        catch (SchedulerException e)
        {
            logger.error("", e);
        }
        
        this.addReminder(reminder);
    }
}
