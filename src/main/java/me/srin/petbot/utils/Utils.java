package me.srin.petbot.utils;

import me.srin.petbot.database.Pet;
import net.dv8tion.jda.api.EmbedBuilder;
import okhttp3.OkHttpClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Utils {
    private static final Random RANDOM = new Random();
    public static final OkHttpClient client = new OkHttpClient().newBuilder().build();
    public static final ScheduledThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(2);
//    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    public static int randomColor() {
        int a = RANDOM.nextInt(256);
        int r = RANDOM.nextInt(256);
        int g = RANDOM.nextInt(256);
        int b = RANDOM.nextInt(256);
        return (a << 8 << 8 << 8) | (r << 8 << 8) | (g << 8) | b;
    }

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
        urlBuilder.append("https://petbot-next-app.vercel.app/api/og?")
                .append("xp=").append(pet.getXp()).append('&')
                .append("xp-limit=").append(pet.getXpLimit()).append('&')
                .append("color=").append(config.getStatusColor()).append('&')
                .append("level=").append(pet.getLevel()).append('&')
                .append("rank=").append(pet.getRank()).append('&')
                .append("type=").append(pet.getType()).append('&')
                .append("id=").append(pet.getId()).append('&')
                .append("background=").append(URLEncoder.encode(config.getStatusBackground(), StandardCharsets.UTF_8)).append('&')
                .append(petName == null? '\0': ("name=" + URLEncoder.encode(petName, StandardCharsets.UTF_8))).append('&')
                .append(profile == null? '\0': ("avatar=" + URLEncoder.encode(profile, StandardCharsets.UTF_8)));
        return urlBuilder.toString();
    }
}
