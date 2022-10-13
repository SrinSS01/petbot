package me.srin.petbot.events;

import me.srin.petbot.database.Database;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuildReady extends GuildEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuildReady.class);
    private GuildReady(Database database) {
        super(database);
    }

    public static GuildReady create(Database database) {
        return new GuildReady(database);
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        Guild guild = event.getGuild();
        LOGGER.info("Guild ready: " + guild.getName());
        guild.updateCommands().addCommands(
                Commands.slash("buy-pet", "buy a pet to keep as your own"),
                Commands.slash("view-stats", "view user status"),
                Commands.slash("view-pets", "view pet details"),
                Commands.slash("view-pets-user", "view pet details of a certain user")
                        .addOption(OptionType.USER, "user", "user who's pet details will be shown", true),
                Commands.slash("start-training", "train your pet")
                        .addOption(OptionType.STRING, "pet", "name of the pet to train", true)
        ).queue();

        for (Member member : guild.getMembers()) {
            User user = member.getUser();
            if (user.isBot()) {
                continue;
            }
            insertUserIfNotPresent(guild, user);
        }
    }
}
