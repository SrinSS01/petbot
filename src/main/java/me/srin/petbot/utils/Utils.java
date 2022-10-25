package me.srin.petbot.utils;

import me.srin.petbot.database.Database;
import me.srin.petbot.database.Pet;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;

import java.awt.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Utils {
    public static final SelectMenu.Builder SELECTION_BUILDER = SelectMenu.create("create-pet-selection");
    private static final Random RANDOM = new Random();
    public static final ScheduledThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(2);
//    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    public static int randomColor() {
        int a = RANDOM.nextInt(256);
        int r = RANDOM.nextInt(256);
        int g = RANDOM.nextInt(256);
        int b = RANDOM.nextInt(256);
        return (a << 8 << 8 << 8) | (r << 8 << 8) | (g << 8) | b;
    }
    public static void createPet(InteractionHook hook, Member member) {
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("**Pet shop**")
                .setDescription("_Select a pet type_")
                .setColor(Color.CYAN)
                .build();
        hook.editOriginalEmbeds(embed)
                .setComponents(
                        ActionRow.of(SELECTION_BUILDER.build()),
                        ActionRow.of(Button.success("select", "select"))
                ).queue(message -> {
                    Database.PetLab petLab = new Database.PetLab();
                    petLab.setPet(Pet.create());
                    long guildId = member.getGuild().getIdLong();
                    long memberId = member.getIdLong();
                    var task = message.editMessageComponents(
                            ActionRow.of(SELECTION_BUILDER.build().asDisabled()),
                            ActionRow.of(Button.success("select", "select").asDisabled())
                    ).queueAfter(1, TimeUnit.MINUTES, m -> Database.MEMBER_PET_LAB_MAP.get(guildId).remove(memberId));
                    petLab.setTask(task);
                    Database.MEMBER_PET_LAB_MAP.computeIfAbsent(guildId, k -> Map.of());
                    Database.MEMBER_PET_LAB_MAP.get(guildId).put(memberId, petLab);
                });
    }

    /*public static String getProgressBar(int xp, int xpLimit) {
        double ratio = xp / (xpLimit * 1.0);
        int progressLength = (int) (ratio * 10);
        int empty = 10 - progressLength;
        return "`%s%s %d%%`".formatted("\u2588".repeat(progressLength), "_".repeat(empty), (int) (ratio * 100));
    }*/
    public static String petDetails(Pet pet) {
        String petName = pet.getName();
        String type = pet.getType();
        long petId = pet.getId();
        return
                """
                `pet name`: **%s**
                `pet id`: **%s**
                `pet type`: **%s**
                """.formatted(
                        petName == null? "_unknown_": petName,
                        petId == 0? "_unknown_": petId,
                        type
                );
    }

    public static EmbedBuilder getPetDetails(Pet pet) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("**Pet shop**")
                .setDescription(petDetails(pet))
                .setThumbnail(pet.getPfp());
        return embedBuilder;
    }

    public static String getPetStats(Pet pet, Config config) {
        StringBuilder urlBuilder = new StringBuilder();
        String petName = pet.getName();
        String profile = pet.getPfp();
        urlBuilder.append("https://next-app-two-phi.vercel.app/api/og?")
                .append("xp=").append(pet.getXp()).append('&')
                .append("xp-limit=").append(pet.getXpLimit()).append('&')
                .append("color=").append(config.getStatusColor()).append('&')
                .append("level=").append(pet.getLevel()).append('&')
                .append("rank=").append(pet.getRank()).append('&')
                .append("type=").append(pet.getType()).append('&')
                .append("id=").append(pet.getId()).append('&')
                .append("background=").append(URLEncoder.encode(config.getStatusBackground(), StandardCharsets.UTF_8)).append('&')
                .append(petName == null? '\0': ("name=" + URLEncoder.encode(petName, StandardCharsets.UTF_8))).append('&')
                .append(profile == null? '\0': ("profile=" + URLEncoder.encode(profile, StandardCharsets.UTF_8)));
        return urlBuilder.toString();
    }
    /*public static EmbedBuilder getPetStats(Pet pet) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("**pet stats**").setDescription("""
                %s
                
                %s
                """.formatted(petDetails(pet), getStats(pet))).setThumbnail(pet.getPfp());
        return builder;
    }*/

    /*private static String getStats(Pet pet) {
        StringBuilder builder = new StringBuilder();
        int xp = pet.getXp();
        int xpLimit = pet.getXpLimit();
        builder.append("`xp`: ").append(xp).append('/').append(xpLimit).append('\n')
                .append("`level`: ").append(pet.getLevel()).append('\n')
                .append("_progress_").append('\n')
                .append('`').append(getProgressBar(xp, xpLimit)).append('`');
        return builder.toString();
    }*/
}
