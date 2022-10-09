package me.srin.petbot.events;

import me.srin.petbot.database.Database;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import org.jetbrains.annotations.NotNull;

public class GuildJoin extends GuildEvent {
    public GuildJoin(Database database) {
        super(database);
    }

    public static GuildJoin create(Database database) {
        return new GuildJoin(database);
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        Guild guild = event.getGuild();
        for (Member member : guild.getMembers()) {
            User user = member.getUser();
            if (user.isBot()) {
                continue;
            }
            insertUserIfNotPresent(guild, user);
        }
    }
}
