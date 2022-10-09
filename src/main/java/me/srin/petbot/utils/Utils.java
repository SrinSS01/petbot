package me.srin.petbot.utils;

import me.srin.petbot.database.Database;
import me.srin.petbot.database.Pet;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Utils {
    public static final SelectMenu.Builder SELECTION_BUILDER = SelectMenu.create("pet-selection");
    public static void buyPet(InteractionHook hook, long user_id) {
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("**Pet shop**")
                .setDescription("_Feel free to select a pet that you want to buy_")
                .build();
        hook.editOriginalEmbeds(embed)
                .setComponents(
                        ActionRow.of(SELECTION_BUILDER.build()),
                        ActionRow.of(Button.success("buy", "Buy"))
                ).queue(message -> message.editMessageComponents(
                        ActionRow.of(SELECTION_BUILDER.build().asDisabled()),
                        ActionRow.of(Button.success("buy", "Buy").asDisabled())
                ).queueAfter(1, TimeUnit.MINUTES, m -> Database.USER_SELECTIONS.remove(user_id)));
    }

    public static String getProgressBar(int xp, int xpLimit) {
        double ratio = xp / (xpLimit * 1.0);
        int progressLength = (int) (ratio * 10);
        int empty = 10 - progressLength;
        return "`%s%s %d%%`".formatted("\u2588".repeat(progressLength), "_".repeat(empty), (int) (ratio * 100));
    }

    public static MessageEmbed getPetStatsEmbed(Pet pet, User user) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        String petName = pet.getName();
        int petXp = pet.getXp();
        int petXpLimit = pet.getXpLimit();
        String progressBar = getProgressBar(petXp, petXpLimit);

        embedBuilder.setTitle("**%s's pets**".formatted(user.getAsTag()))
                .setDescription(
                        """
                        _%s's stats_
                        `pet type`: %s
                        `xp`: (%d/%d)
                        `level`: %d
                        
                        _progress_
                        %s
                        """.formatted(
                                petName,
                                pet.getType(),
                                petXp, petXpLimit, pet.getLevel(), progressBar
                        )).setThumbnail(pet.getPfp()).setFooter(Date.from(Instant.ofEpochMilli(System.currentTimeMillis())).toString(), user.getAvatarUrl());
        return embedBuilder.build();
    }
}
