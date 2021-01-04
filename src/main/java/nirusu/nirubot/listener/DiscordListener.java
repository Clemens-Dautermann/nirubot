package nirusu.nirubot.listener;

import java.util.Objects;
import java.util.Optional;

import javax.security.auth.login.LoginException;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.shard.ShardingStrategy;
import nirusu.nirubot.Nirubot;
import nirusu.nirubot.core.Config;
import nirusu.nirubot.core.GuildManager;
import nirusu.nirubot.core.audio.PlayerManager;
import nirusu.nirucmd.CommandContext;
import nirusu.nirucmd.CommandDispatcher;
import nirusu.nirucmd.exception.NoSuchCommandException;

public class DiscordListener implements NiruListener {
    private final CommandDispatcher dispatcher;
    private final GatewayDiscordClient client;

    public DiscordListener() throws LoginException {
        Config conf = Nirubot.getConfig();
        dispatcher = new CommandDispatcher.Builder()
            .addPackage("nirusu.nirubot.command").build();
        
        client = Objects.requireNonNull(DiscordClient.create(conf.getToken())
            .gateway()
            .setSharding(ShardingStrategy.recommended())
            .login().block());
        
        client.getEventDispatcher().on(ReadyEvent.class).subscribe(event -> 
            Nirubot.info("Logged in as {}", event.getSelf().getUsername()));

        client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(this::onMessageRecievedEvent);
        client.onDisconnect().block();

    }

    public void onMessageRecievedEvent(MessageCreateEvent event) {
        Message mes = event.getMessage();

        Optional<User> auth = mes.getAuthor();

        if (auth.isEmpty() || auth.get().isBot()) {
            return;
        }

        // get message content
        String raw = mes.getContent();

        if (raw == null) {
            return;
        }

        CommandContext ctx = new CommandContext(event);

        String prefix;

        if (ctx.isGuild()) {
            prefix = event.getGuild().blockOptional().map(g -> {
                GuildManager mg = GuildManager.getManager(g.getId().asLong());
                return mg.prefix();
            }).orElse(Nirubot.getDefaultPrefix());
        } else if (ctx.isPrivate()) {
            prefix = "";
        } else {
            prefix = Nirubot.getDefaultPrefix();
        }

        // check if message starts with prefix !
        if (raw.startsWith(prefix) && raw.length() > prefix.length()) {
            // create the CommandContext
            ctx.setArgsAndKey(raw.substring(prefix.length()).split("\\s+"), raw.substring(prefix.length()).split("\\s+")[0], true);
            try {
                dispatcher.run(ctx, ctx.getKey());
            } catch (NoSuchCommandException e) {
                //TODO:: controlflow with exception, not good
            }
        }
    }

    @Override
    public void shutdown() {
        for (long id  : PlayerManager.getInstance().getAllIds()) {
            PlayerManager.getInstance().destroy(id);
        }
        client.logout().block();
        Nirubot.info("Discord listener is shutting down");
    }
}
