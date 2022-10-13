package me.srin.petbot.events;

import me.srin.petbot.database.Database;
import me.srin.petbot.database.Pet;
import me.srin.petbot.database.User;
import me.srin.petbot.utils.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;

public class ModalInteraction extends Event {
    public ModalInteraction(Database database) {
        super(database);
    }

    public static ModalInteraction create(Database database) {
        return new ModalInteraction(database);
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        Message message = event.getMessage();
        if (message == null) {
            event.deferEdit().queue();
            return;
        }
        long messageId = message.getIdLong();
        User user = Database.SELECTED_PET_CACHE.get(messageId);
        net.dv8tion.jda.api.entities.User eventUser = event.getUser();
        if (user.getUserId() != eventUser.getIdLong()) {
            event.deferEdit().queue();
            return;
        }
        Pet pet = user.getPet();
        switch (event.getModalId()) {
            case "change-name" -> {
                ModalMapping name = event.getValue("name");
                if (name == null) {
                    event.deferEdit().queue();
                    return;
                }
                String nameAsString = name.getAsString();
                pet.setName(nameAsString);
            }
            case "set-pfp" -> {
                ModalMapping pfp = event.getValue("pfp");
                if (pfp == null) {
                    event.deferEdit().queue();
                    return;
                }
                String pfpAsString = pfp.getAsString();
                if (pfpAsString.length() > 255) {
                    event.reply("PFP URL is too long!").setEphemeral(true).queue();
                    return;
                }
                pet.setPfp(pfpAsString);
            }
        }
        event.editMessageEmbeds(Utils.getPetStatsEmbed(pet, eventUser)).queue();
        database.getPetRepo().save(pet);
    }
}
