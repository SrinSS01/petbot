package me.srin.petbot.events;

import me.srin.petbot.database.Database;
import me.srin.petbot.database.Pet;
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
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
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
//        long guildId = guild.getIdLong();
        Button button = event.getButton();
        String id = button.getId();
        if (id == null) {
            return;
        }
        User user = event.getUser();
//        long userId = user.getIdLong();
        final Member member = event.getMember();
        if (member == null) {
            event.deferEdit().queue();
            return;
        }
        switch (id) {
            case "select" -> {
                Database.PetLab petLab = getPetLab(event, member);
                if (petLab == null) return;
                Pet pet = petLab.getPet();
                EmbedBuilder petStatsEmbed = Utils.getPetDetails(pet);
                petStatsEmbed.setFooter(Date.from(Instant.now()).toString(), user.getAvatarUrl());
                MessageEmbed embed = petStatsEmbed.build();
                event.editMessageEmbeds(embed)
                        .setComponents(
                                ActionRow.of(
                                        Button.primary("change-name", "Change name"),
                                        Button.primary("set-pfp", "Set pfp"),
                                        Button.success("save", "Save Changes"),
                                        Button.danger("remove", "Remove")
                                )
                        )
                        .queue(hook -> hook.editOriginalComponents(
                                ActionRow.of(
                                        Button.primary("change-name", "Change name").asDisabled(),
                                        Button.primary("set-pfp", "Set pfp").asDisabled(),
                                        Button.success("save", "Save Changes").asDisabled(),
                                        Button.danger("remove", "Remove").asDisabled()
                                )
                        ).queueAfter(3, TimeUnit.MINUTES));
                petLab.getTask().cancel(true);
            }
            case "remove" -> {
                if (getPetLab(event, member) == null) return;
                Database.MEMBER_PET_LAB_MAP.remove(member);
                event.getMessage().delete().queue();
            }
            case "save" -> {
                Database.PetLab petLab = getPetLab(event, member);
                if (petLab == null) return;
                Pet pet = petLab.getPet();
                pet = database.getPetRepo().save(pet);
                event.editMessageEmbeds(Utils.getPetDetails(pet).build()).queue(hook ->
                        hook.sendMessage("Changes saved").setEphemeral(true).queue());
            }
            case "change-name" -> {
                if (getPetLab(event, member) == null) return;
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
                if (getPetLab(event, member) == null) return;
                TextInput name = TextInput.create("pfp", "Profile picture", TextInputStyle.SHORT)
                        .setPlaceholder("Enter an url of the pfp")
                        .build();
                Modal modal = Modal.create("set-pfp", "Set pet profile picture")
                        .addActionRow(name)
                        .build();
                event.replyModal(modal).queue();
            }
            case "train-pet" -> {
                Pet pet = Database.MESSAGE_PET_STATUS_MAP_SELECTED.get(event.getMessageIdLong());
                long cooldown = pet.getCooldown();
                long currentTime = System.currentTimeMillis();
                if ((currentTime - cooldown) <= database.getConfig().getTrainingCooldownInSeconds()) {

                }
            }
        }
    }

    @Nullable
    private static Database.PetLab getPetLab(@NotNull ButtonInteractionEvent event, Member member) {
        Database.PetLab petLab = Database.MEMBER_PET_LAB_MAP.get(member);
        if (petLab == null || member == null) {
            event.deferEdit().queue();
            return null;
        }
        return petLab;
    }
}
