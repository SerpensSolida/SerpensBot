package com.serpenssolida.discordbot.module.reminder;

import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.SerpensBot;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.List;

public class SendReminderJob implements Job
{
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        Reminder reminder = (Reminder) context.getJobDetail().getJobDataMap().get("reminder");
        
        if (reminder == null)
            return;
        
        User author = SerpensBot.getApi().getUserById(reminder.getAuthorID());
        MessageCreateBuilder reminderMessage;
        
        if (author == null)
            reminderMessage = MessageUtils.createSimpleMessage("Reminder", reminder.getMessage());
        else
            reminderMessage = MessageUtils.createSimpleMessage("Reminder", author, reminder.getMessage());

        List<User> list = reminder.getMentions().stream().map(s -> SerpensBot.getApi().getUserById(Long.parseLong(s))).toList();

        for (User user : list)
            reminderMessage.addContent(user.getAsMention());
        
        MessageChannel messageChannel = SerpensBot.getApi().getChannelById(MessageChannel.class, reminder.getChannelID());
        
        if (messageChannel != null)
            messageChannel.sendMessage(reminderMessage.build()).queue();
        
        reminder.setDate(context.getNextFireTime());
        
        //Delete from list if this reminder should not be repeated.
        if (!reminder.isRepeat())
            ReminderController.getInstance().deleteReminder(reminder.getChannelID());
        
        ReminderController.getInstance().saveReminders();
    }
}
