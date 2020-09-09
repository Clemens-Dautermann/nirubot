package nirusu.nirubot.command.fun.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.api.entities.MessageEmbed;
import nirusu.nirubot.command.CommandContext;
import nirusu.nirubot.command.ICommand;
import nirusu.nirubot.core.DiscordUtil;
import nirusu.nirubot.core.GuildManager;
import nirusu.nirubot.core.GuildMusicManager;
import nirusu.nirubot.core.PlayerManager;

public final class Next implements ICommand {

    @Override
    public void execute(CommandContext ctx) {

        if (ctx.getArgs().size() != 1) {
            return;
        }

        PlayerManager manager = PlayerManager.getInstance();
        GuildMusicManager musicManager = manager.getGuildMusicManager(ctx.getGuild());
        AudioTrack prev = musicManager.getPlayer().getPlayingTrack();

        if (!DiscordUtil.areInSameVoice(ctx.getMember(), ctx.getSelfMember())) {
            ctx.reply("You must be in the same voice channel!");
            return;
        }

        if (prev == null) {
            ctx.reply("Nothing is playing!");
            return;
        }

        manager.next(musicManager);
        AudioTrack next = musicManager.getPlayer().getPlayingTrack();
        String prevText = prev.getInfo().author + " - " + prev.getInfo().title;
        String nextText = next != null ? next.getInfo().author + " - " + next.getInfo().title : "End of queue";
        ctx.reply("Skipped: " + prevText + "\n" + "Now playing: " + nextText);
    }

    @Override
    public String getKey() {
        return "next";
    }

    @Override
    public MessageEmbed helpMessage(GuildManager gm) {
        return ICommand.createHelp("skips the current song", gm.prefix(), getKey());
    }

}
