package me.srin.petbot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.srin.petbot.database.Database;
import me.srin.petbot.events.*;
import me.srin.petbot.utils.Utils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static net.dv8tion.jda.api.requests.GatewayIntent.*;

@Component
@AllArgsConstructor
@Getter @Setter
public class Main implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private final Database database;

    private final GuildReady guildReady;

    @Override
    public void run(String... args) {
        String token = database.getConfig().getToken();
        LOGGER.info("Started bot with token: {}", token);
        JDA bot = JDABuilder.createDefault(token).enableIntents(
                    GUILD_PRESENCES,
                    GUILD_MEMBERS,
                    GUILD_MESSAGES,
                    GUILD_EMOJIS_AND_STICKERS,
                    GUILD_VOICE_STATES
                )
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(CacheFlag.CLIENT_STATUS)
                .disableCache(
                        CacheFlag.EMOJI,
                        CacheFlag.STICKER,
                        CacheFlag.VOICE_STATE
                )
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.playing("Selling pets :cat:"))
                .addEventListeners(
                        guildReady,
                        SlashCommand.create(database),
                        SelectionMenu.create(database),
                        ButtonPress.create(database),
                        ModalInteraction.create(database)
                )
                .build();
        Scanner sc = new Scanner(System.in);
        Utils.EXECUTOR.scheduleWithFixedDelay(() -> {
            if (sc.next().equals("stop")) {
                Utils.EXECUTOR.shutdownNow();
                bot.shutdownNow();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}
