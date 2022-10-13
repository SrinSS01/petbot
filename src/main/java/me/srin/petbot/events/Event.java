package me.srin.petbot.events;

import lombok.AllArgsConstructor;
import me.srin.petbot.database.Database;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Random;

@AllArgsConstructor
public abstract class Event extends ListenerAdapter {
    protected final Database database;
}
