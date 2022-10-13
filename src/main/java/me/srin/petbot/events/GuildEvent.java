package me.srin.petbot.events;

import me.srin.petbot.database.Database;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

public abstract class GuildEvent extends Event {
    public GuildEvent(Database database) {
        super(database);
    }

    protected void insertUserIfNotPresent(Guild guild, User user) {
        long userIdLong = user.getIdLong();
        long guildIdLong = guild.getIdLong();
        if (!database.getUserRepo().existsById(me.srin.petbot.database.User.ID.of(userIdLong, guildIdLong))) {
            me.srin.petbot.database.User usr = me.srin.petbot.database.User.create(userIdLong, guildIdLong);
            usr.setTag(user.getAsTag());
            database.getUserRepo().save(usr);
        }
    }
}
