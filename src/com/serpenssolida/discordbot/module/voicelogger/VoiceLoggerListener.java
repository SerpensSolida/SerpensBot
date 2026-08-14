package com.serpenssolida.discordbot.module.voicelogger;

import com.serpenssolida.discordbot.MessageUtils;
import com.serpenssolida.discordbot.SerpensBot;
import com.serpenssolida.discordbot.command.BotCommand;
import com.serpenssolida.discordbot.module.BotListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;

public class VoiceLoggerListener extends BotListener
{
	public VoiceLoggerListener()
	{
		super("voicelogger");
		this.setModuleName("Voice Logger");

		//Command for creating a game.
		BotCommand command = new BotCommand("set", "Imposta un log per chi entra/esce in vocale.");
		command.setAction(this::setLogger);
		command.getCommandData()
			   .addOption(OptionType.CHANNEL, "channel", "Il canale in cui loggare", true);
		this.addBotCommand(command);

		//Command for stopping a game.
		command = new BotCommand("unset", "Rimuove un log.");
		command.setAction(this::unsetLogger);
		this.addBotCommand(command);

		VoiceLoggerController.getInstance().loadVoiceLogs();
	}

	@Override
	public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event)
	{
		super.onGuildVoiceUpdate(event);

		String guildID = event.getGuild().getId();

		if (!VoiceLoggerController.getInstance().shouldLogVoiceUpdate(guildID))
			return;

		VoiceLog voiceLog = VoiceLoggerController.getInstance().getVoiceLog(guildID);
		TextChannel channel = SerpensBot.getApi().getTextChannelById(voiceLog.channel());

		channel.sendMessage(getVoiceLogMessage(event)).queue();
	}

	private void setLogger(SlashCommandInteractionEvent event, Guild guild, MessageChannel message, User author)
	{
		OptionMapping channelArg = event.getOption("channel");

		String channelID = null;

		if (channelArg != null)
			channelID = channelArg.getAsChannel().getId();

		VoiceLoggerController.getInstance().addVoiceLog(guild.getId(), new VoiceLog(channelID));

		MessageCreateData replyMessage = MessageUtils.buildSimpleMessage("Voice Log", author, "Log creato con successo.");
		event.reply(replyMessage).queue();
	}

	private void unsetLogger(SlashCommandInteractionEvent event, Guild guild, MessageChannel message, User author)
	{
		boolean deleted = VoiceLoggerController.getInstance().deleteVoiceLog(guild.getId());

		MessageCreateData replyMessage;
		if (!deleted)
			replyMessage = MessageUtils.buildErrorMessage("Voice Log", author, "Nessun voice log per questo server.");
		else
			replyMessage = MessageUtils.buildSimpleMessage("Voice Log", author, "Log rimosso con successo.");

		event.reply(replyMessage).queue();
	}

	private static MessageCreateData getVoiceLogMessage(@NotNull GuildVoiceUpdateEvent event)
	{
		AudioChannelUnion joinedChannel = event.getChannelJoined();
		AudioChannelUnion leftChannel = event.getChannelLeft();

		String message;

		if (joinedChannel != null && leftChannel != null)
			message = " switched voice channels " + leftChannel.getAsMention() + " -> " + joinedChannel.getAsMention();
		else if (leftChannel == null && joinedChannel != null)
			message = " joined voice channel " + joinedChannel.getAsMention();
		else
			message = " left voice channel " + leftChannel.getAsMention();


		EmbedBuilder embed = MessageUtils.getDefaultEmbed("Voice Log");
		embed.setDescription(event.getEntity().getAsMention() + message);
		return new MessageCreateBuilder().setEmbeds(embed.build()).build();
	}
}
