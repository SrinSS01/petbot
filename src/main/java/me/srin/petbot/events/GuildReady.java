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
                Commands.slash("create-pet", "create a pet for the users"),
                Commands.slash("edit-pet", "edit a pet")
                        .addOption(OptionType.INTEGER, "id", "pet id", true),
                Commands.slash("assign-pet", "assign a pet to a user")
                        .addOption(OptionType.INTEGER, "id", "pet id", true)
                        .addOption(OptionType.USER, "user", "user to assign the pet to", true),
                Commands.slash("pet-list", "List of all pets that were created"),
//                Commands.slash("user-stats", "view user status"),
                Commands.slash("pet-stats", "view pet stats"),
                Commands.slash("view-pets-user", "view pet stats of a certain user")
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
