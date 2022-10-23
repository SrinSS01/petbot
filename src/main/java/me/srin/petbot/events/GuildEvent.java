package me.srin.petbot.events;

import me.srin.petbot.database.Database;

public abstract class GuildEvent extends Event {
    public GuildEvent(Database database) {
        super(database);
    }
}
