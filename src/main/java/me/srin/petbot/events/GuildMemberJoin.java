package me.srin.petbot.events;

import me.srin.petbot.database.Database;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import org.jetbrains.annotations.NotNull;

public class GuildMemberJoin extends GuildEvent {
    public GuildMemberJoin(Database database) {
        super(database);
    }

    public static GuildMemberJoin create(Database database) {
        return new GuildMemberJoin(database);
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        insertUserIfNotPresent(event.getGuild(), event.getUser());
    }
}
