package me.srin.petbot.events;

import me.srin.petbot.database.Database;
import me.srin.petbot.database.Pet;
import me.srin.petbot.database.PetUserRDBMS;
import me.srin.petbot.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ButtonPress extends Event {
//    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ButtonPress.class);

    private ButtonPress(Database database) {
        super(database);
    }

    public static ButtonPress create(Database database) {
        return new ButtonPress(database);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            return;
        }
        long guildId = guild.getIdLong();
        Button button = event.getButton();
        String id = button.getId();
        if (id == null) {
            return;
        }
        User user = event.getUser();
        long userId = user.getIdLong();
        switch (id) {
            case "buy" -> {
                if (!Database.USER_SELECTIONS.containsKey(userId)) {
                    event.deferEdit().queue();
                    return;
                }
                event.deferReply().setEphemeral(true).queue();
                var count = database.getPetUserRDBMSRepository().countByUserIdAndGuildId(userId, guildId);
                if (count >= database.getConfig().getPetLimit()) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(Color.RED)
                            .setDescription("_You don't have any empty pet slots left\nYou can have upto %d pets..._".formatted(database.getConfig().getPetLimit()))
                            .build();
                    event.getHook().editOriginalEmbeds(embed).queue();
                    return;
                }
                var pet_id = database.insertNewPet(userId);
                database.addPetToOwner(userId, guildId, pet_id);
                Pet pet = database.getPet(userId);
                MessageEmbed embed = new EmbedBuilder()
                        .setTitle("**you bought**").setDescription("a " + pet.getType()).build();
                event.getHook().editOriginalEmbeds(embed).queue();
            }
            case "disown" -> {
                long usrId = Database.SELECTED_PET_CACHE.get(event.getMessageIdLong()).getUserId();
                if (usrId != userId) {
                    event.deferEdit().queue();
                    return;
                }
                event.replyEmbeds(
                                new EmbedBuilder()
                                        .setDescription("Do you want to disown this pet?")
                                        .setFooter(event.getMessageId(), user.getAvatarUrl())
                                        .setColor(Color.RED).build()
                        )
                        .addActionRow(
                                Button.success("disown-confirm-yes", "Yes"),
                                Button.danger("disown-confirm-no", "No")
                        ).setEphemeral(true).queue(hook -> hook.editOriginalComponents(ActionRow.of(
                                Button.success("disown-confirm-yes", "Yes").asDisabled(),
                                Button.danger("disown-confirm-no", "No").asDisabled()
                        )).queueAfter(1, TimeUnit.MINUTES));
            }
            case "disown-confirm-yes" -> {
                event.deferEdit().queue();
                long messageId = Long.parseLong(Objects.requireNonNull(Objects.requireNonNull(event.getMessage().getEmbeds().get(0).getFooter()).getText()));
                me.srin.petbot.database.User usr = Database.SELECTED_PET_CACHE.get(messageId);
                Pet pet = usr.getPet();
                long petId = pet.getId();
                database.getPetUserRDBMSRepository().deleteById(PetUserRDBMS.ID.of(userId, guildId, petId));
                database.getPetRepo().deleteById(petId);
                Database.PET_CACHE.get(messageId).remove(petId);
                event.getHook().editOriginalEmbeds(
                        new EmbedBuilder()
                                .setColor(Color.GREEN)
                                .setDescription("disowned " + pet.getName())
                                .build()
                ).setComponents(List.of()).queue();
            }
            case "disown-confirm-no" -> event.editMessageEmbeds(
                    new EmbedBuilder()
                            .setColor(Color.GRAY)
                            .setDescription("canceled")
                            .build()
            ).setComponents(List.of()).queue();
            case "change-name" -> {
                if (!Database.SELECTED_PET_CACHE.containsKey(event.getMessageIdLong())) {
                    event.deferEdit().queue();
                    return;
                }
                long usrId = Database.SELECTED_PET_CACHE.get(event.getMessageIdLong()).getUserId();
                if (usrId != userId) {
                    event.deferEdit().queue();
                    return;
                }
                TextInput name = TextInput.create("name", "Name", TextInputStyle.SHORT)
                        .setPlaceholder("Enter a name for the pet")
                        .setRequiredRange(1, 100)
                        .build();
                Modal modal = Modal.create("change-name", "Change pet name")
                        .addActionRow(name)
                        .build();
                event.replyModal(modal).queue();
            }
            case "set-pfp" -> {
                if (!Database.SELECTED_PET_CACHE.containsKey(event.getMessageIdLong())) {
                    event.deferEdit().queue();
                    return;
                }
                long usrId = Database.SELECTED_PET_CACHE.get(event.getMessageIdLong()).getUserId();
                if (usrId != userId) {
                    event.deferEdit().queue();
                    return;
                }
                TextInput name = TextInput.create("pfp", "Profile picture", TextInputStyle.SHORT)
                        .setPlaceholder("Enter an url of the pfp")
                        .build();
                Modal modal = Modal.create("set-pfp", "Set pet profile picture")
                        .addActionRow(name)
                        .build();
                event.replyModal(modal).queue();
            }
            case "train-pet" -> {
                if (!Database.SELECTED_PET_CACHE.containsKey(event.getMessageIdLong())) {
                    event.deferEdit().queue();
                    return;
                }
                me.srin.petbot.database.User usr = Database.SELECTED_PET_CACHE.get(event.getMessageIdLong());
                long usrId = usr.getUserId();
                if (usrId != userId) {
                    event.deferEdit().queue();
                    return;
                }
                Member member = event.getMember();
                Pet pet = usr.getPet();
                Map<Pet, Long> map = Database.LAST_TIME_CACHE.get(member);
                long lastTime = map == null? 0L: Objects.requireNonNullElse(map.get(pet), 0L);
                long dTime = (System.currentTimeMillis() / 1000) - lastTime;
                long trainingSeconds = database.getConfig().getTrainingSeconds();
                if (dTime <= trainingSeconds) {
                    event.replyFormat(
                            "Oh your pet is still training, wait until you can use this command again at <t:%d:T>",
                                    lastTime + trainingSeconds
                            )
                            .setEphemeral(true).queue();
                    return;
                }
                event.deferEdit().queue();
                boolean isLevelup = pet.train();
                if (isLevelup) usr.levelUp();
                database.getUserRepo().save(usr);
                database.getPetRepo().save(pet);
                Database.LAST_TIME_CACHE.put(member, Map.of(pet, System.currentTimeMillis() / 1000));
                event.getHook().editOriginalEmbeds(Utils.getPetStatsEmbed(pet, user)).queue();
            }
        }
    }
}
