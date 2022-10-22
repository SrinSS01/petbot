package me.srin.petbot.events;

import me.srin.petbot.database.Database;
import me.srin.petbot.database.Pet;
import me.srin.petbot.utils.Utils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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
                Map<Long, Pet> petMap = Database.MEMBER_PET_STATUS_MAP.get(event.getMessageIdLong());
                if (petMap == null) {
                    return;
                }
                Pet pet = petMap.get(Long.parseLong(selectOption.getValue()));
                var petStats = Utils.getPetStats(pet, database.getConfig().getStatusBackground());
                event.editMessage(petStats).queue();
            }
        }
    }
}
