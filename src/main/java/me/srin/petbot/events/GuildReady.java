package me.srin.petbot.events;

import me.srin.petbot.database.Database;
import me.srin.petbot.utils.Config;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class GuildReady extends GuildEvent {
    private final Config config;
    private static final Logger LOGGER = LoggerFactory.getLogger(GuildReady.class);
    private GuildReady(Database database, Config config) {
        super(database);
        this.config = config;
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        Guild guild = event.getGuild();
        LOGGER.info("Guild ready: " + guild.getName());
        guild.updateCommands().addCommands(
                Commands.slash("create-pet", "create a pet for the users")
                        .addOptions(
                                new OptionData(OptionType.STRING, "type", "Pet type", true)
                                        .addChoices(
                                                config.getPetSpecies().stream().map(species -> new Command.Choice(species, species)).collect(Collectors.toList())
                                        )
                        ),
                Commands.slash("edit-pet", "edit a pet")
                        .addOption(OptionType.INTEGER, "id", "pet id", true),
                Commands.slash("assign-pet", "assign a pet to a user")
                        .addOption(OptionType.INTEGER, "id", "pet id", true)
                        .addOption(OptionType.USER, "user", "user to assign the pet to", true),
                Commands.slash("pet-list", "List of all pets that were created"),
                Commands.slash("pet-stats", "view pet stats"),
                Commands.slash("view-pets-user", "view pet stats of a certain user")
                        .addOption(OptionType.USER, "user", "user who's pet details will be shown", true),
                Commands.slash("start-training", "train your pet")
                        .addOption(OptionType.INTEGER, "pet", "id of the pet to train", true)
        ).queue();
    }
}
