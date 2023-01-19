package me.srin.petbot.events;

import me.srin.petbot.database.Database;
import me.srin.petbot.database.Pet;
import me.srin.petbot.utils.Utils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.utils.AttachedFile;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Optional;

public class SelectionMenu extends Event {
    private SelectionMenu(Database database) {
        super(database);
    }

    public static SelectionMenu create(Database database) {
        return new SelectionMenu(database);
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        event.deferEdit().queue();
        Guild guild = event.getGuild();
        if (guild == null) {
            return;
        }
        String id = event.getComponentId();
        var selectOption = event.getSelectedOptions().get(0);
//        String selected = selectOption.getLabel();
        Member member = event.getMember();
        if (member == null) {
            return;
        }
        if (id.equals("pet-stats")) {
            long petId = Long.parseLong(selectOption.getValue());
            Pet unsavedPetData = Database.PET_MAP.get(petId);
            if (unsavedPetData != null) {
                database.getPetRepo().save(unsavedPetData);
            }
            Optional<Pet> petOptional = database.getPetRepo().findById(petId);
            if (petOptional.isEmpty()) {
                return;
            }
            Pet pet = petOptional.get();
            int rank = database.getPetRepo().getRankById(petId);
            pet.setRank(rank);
            Database.MESSAGE_PET_STATUS_MAP_SELECTED.put(event.getMessageIdLong(), petId);
            var petStats = Utils.getPetStats(pet, database.getConfig());
            Request request = new Request.Builder()
                    .url(petStats)
                    .method("GET", null)
                    .build();
            try (Response response = Utils.client.newCall(request).execute()) {
                if (response.body() == null) {
                    return;
                }
                byte[] bytes = response.body().bytes();
                event.getHook().editOriginalAttachments(AttachedFile.fromData(bytes, "stats.png")).queue();
            } catch (IOException ignored) {}
        }
    }
}
