package me.srin.petbot.events;

import me.srin.petbot.database.Database;
import me.srin.petbot.database.Pet;
import me.srin.petbot.utils.Utils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class SelectionMenu extends Event {
    private SelectionMenu(Database database) {
        super(database);
    }

    public static SelectionMenu create(Database database) {
        return new SelectionMenu(database);
    }

    @Override
    public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            return;
        }
        String id = event.getComponentId();
        var selectOption = event.getSelectedOptions().get(0);
        String selected = selectOption.getLabel();
        Member member = event.getMember();
        if (member == null) {
            event.deferEdit().queue();
            return;
        }
        switch (id) {
            case "create-pet-selection" -> {
                Database.PetLab petLab = Database.MEMBER_PET_LAB_MAP.get(member);
                event.deferEdit().queue();
                if (petLab == null) {
                    return;
                }
                Pet pet = petLab.getPet();
                pet.setType(selected);
            }
            case "pet-stats" -> {
                long petId = Long.parseLong(selectOption.getValue());
                Optional<Pet> petOptional = database.getPetRepo().findById(petId);
                if (petOptional.isEmpty()) {
                    event.deferEdit().queue();
                    return;
                }
                Pet pet = petOptional.get();
                int rank = database.getPetRepo().getRankById(petId);
                pet.setRank(rank);
                Database.MESSAGE_PET_STATUS_MAP_SELECTED.put(event.getMessageIdLong(), petId);
                var petStats = Utils.getPetStats(pet, database.getConfig());
                event.editMessage(petStats).queue();
            }
        }
    }
}
