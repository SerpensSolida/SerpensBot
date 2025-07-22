package com.serpenssolida.discordbot.module.reminder;

import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.command.BotCommand;
import com.serpenssolida.discordbot.module.BotListener;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.quartz.DateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReminderListener extends BotListener
{
    private static final Logger logger = LoggerFactory.getLogger(ReminderListener.class);

    public ReminderListener()
    {
        super("remind");
        this.setModuleName("Reminder");
        
        //Command for creating a reminder.
        BotCommand command = new BotCommand("create", "Crea un nuovo reminder.");
        command.setAction(this::createReminder);
        command.getCommandData()
                .addOption(OptionType.STRING, "date", "Data del reminder. (DD/MM/YYYY mm:HH)", true)
                .addOption(OptionType.STRING, "description", "Descrizione del reminder.", true)
                .addOption(OptionType.STRING, "mentions", "Lista di utenti da pingare.");
        
        OptionData optionData = new OptionData(OptionType.STRING, "interval-unit", "Unità di tempo per la ripetizione del reminder (es: ore, giorni, ecc).")
                .addChoice("Minuti", DateBuilder.IntervalUnit.MINUTE.toString())
                .addChoice("Ore", DateBuilder.IntervalUnit.HOUR.toString())
                .addChoice("Giorni", DateBuilder.IntervalUnit.DAY.toString())
                .addChoice("Mesi", DateBuilder.IntervalUnit.MONTH.toString())
                .addChoice("Anni", DateBuilder.IntervalUnit.YEAR.toString());
        
        command.getCommandData()
                .addOptions(optionData)
                .addOption(OptionType.INTEGER, "interval-value", "Quantità di tempo per la ripetizione del reminder (es: 2 ore, 3 giorni, ecc).");
        this.addBotCommand(command);
        
        //Command for creating a reminder.
        command = new BotCommand("delete", "Cancella il reminder in questo canale.");
        command.setAction(this::deleteReminder);
       this.addBotCommand(command);
        
        ReminderController.getInstance().loadReminders();
    }
    
    private void createReminder(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
    {
        OptionMapping dateArg = event.getOption("date");
        OptionMapping descriptionArg = event.getOption("description");
        OptionMapping mentionsArg = event.getOption("mentions");
        OptionMapping intervalUnitArg = event.getOption("interval-unit");
        OptionMapping intervalValueArg = event.getOption("interval-value");
        
        if (dateArg == null || descriptionArg == null)
            return;
        
        String dateString = dateArg.getAsString();
        
        //Check if the argument was set.
        if (dateString.isEmpty())
        {
            MessageCreateData message = MessageUtils.buildErrorMessage("Reminder", author, "Formato data errato.");
            event.reply(message).setEphemeral(true).queue();
            return;
        }
        
        //Parse the date from string.
        Date date = ReminderListener.parseDate(dateString);
        
        if (date == null)
        {
            MessageCreateData message = MessageUtils.buildErrorMessage("Reminder", author, "Formato data errato.");
            event.reply(message).setEphemeral(true).queue();
            return;
        }
        
        List<String> mentions = new ArrayList<>();
        
        //Check if the argument was set.
        if (mentionsArg != null)
        {
            mentions.addAll(mentionsArg.getMentions().getUsers().stream().map(ISnowflake::getId).toList());
        }
        
        DateBuilder.IntervalUnit intervalUnit = null;
        int intervalValue = 0;
        
        //Check if the argument was set.
        if (intervalUnitArg != null)
        {
            String unitString = intervalUnitArg.getAsString();
            intervalUnit = DateBuilder.IntervalUnit.valueOf(unitString);
            
            if (intervalValueArg == null)
            {
                MessageCreateData message = MessageUtils.buildErrorMessage("Reminder", author, "Quantità di tempo di repitzione mancante.");
                event.reply(message).setEphemeral(true).queue();
                return;
            }
            
            intervalValue = intervalValueArg.getAsInt();
        }
        
        //Create a new reminder.
        Reminder reminder = new Reminder(author.getId(), channel.getId(), descriptionArg.getAsString(), date, intervalUnitArg != null, mentions, intervalUnit, intervalValue);
        
        //Schedule the reminder.
        ReminderController.getInstance().scheduleReminder(reminder);
        
        MessageCreateData replyMessage = MessageUtils.buildSimpleMessage("Reminder", author, "Reminder creato con successo.\n\nData: *" + date + "*");
        event.reply(replyMessage).queue();
    }
    
    private void deleteReminder(SlashCommandInteractionEvent event, Guild guild, MessageChannel channel, User author)
    {
        boolean deleted = ReminderController.getInstance().deleteReminder(channel.getId());
        
        //Check if the argument was set.
        if (!deleted)
        {
            MessageCreateData message = MessageUtils.buildErrorMessage("Reminder", author, "Nessun reminder trovato per questo canale.");
            event.reply(message).setEphemeral(true).queue();
            return;
        }
        
        ReminderController.getInstance().saveReminders();
        
        MessageCreateData replyMessage = MessageUtils.buildSimpleMessage("Reminder", author, "Reminder eliminato con successo.");
        event.reply(replyMessage).queue();
    }
    
    private static Date parseDate(String dateString)
    {
        try
        {
            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            return dateFormat.parse(dateString);
        }
        catch (ParseException e)
        {
            return null;
        }
    }
}
