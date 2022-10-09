package me.srin.petbot;

import lombok.RequiredArgsConstructor;
import me.srin.petbot.database.Database;
import me.srin.petbot.database.PetRepo;
import me.srin.petbot.database.PetUserRDBMSRepository;
import me.srin.petbot.database.UserRepo;
import me.srin.petbot.events.*;
import me.srin.petbot.utils.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

import static net.dv8tion.jda.api.requests.GatewayIntent.*;

@Component
@RequiredArgsConstructor
public class Main implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private final PetRepo petRepo;
    private final UserRepo userRepo;
    private final PetUserRDBMSRepository petUserRDBMSRepository;

    @Value("${bot-token}")
    private String token;

    @Value("${pet-species}")
    private String[] species;

    @Value("${training-cooldown-in-seconds}")
    private long trainingSeconds;

    @Value("${pet-limit}")
    private int petLimit;

    @Override
    public void run(String... args) {
        Database database = Database.create(
                userRepo,
                petRepo,
                petUserRDBMSRepository,
                new Config(
                        trainingSeconds,
                        species,
                        petLimit
                )
        );
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
                        GuildReady.create(database),
                        GuildJoin.create(database),
                        GuildMemberJoin.create(database),
                        SlashCommand.create(database),
                        SelectionMenu.create(database),
                        ButtonPress.create(database),
                        ModalInteraction.create(database)
                )
                .build();
        Thread stopThread = new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            String command = null;
            while (!"stop".equals(command)) {
                command = sc.next();
            }
            sc.close();
            bot.shutdownNow();
        }, "stop thread");
        stopThread.start();
    }
}
