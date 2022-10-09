package me.srin.petbot.events;

import me.srin.petbot.database.Database;
import me.srin.petbot.database.Pet;
import me.srin.petbot.utils.Utils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.util.Pair;

public class SelectionMenu extends Event {

    private SelectionMenu(Database database) {
        super(database);
    }

    public static SelectionMenu create(Database database) {
        return new SelectionMenu(database);
    }

    @Override
    public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent event) {
        event.deferEdit().queue();
        User user = event.getUser();
        Guild guild = event.getGuild();
        if (guild == null) {
            return;
        }
        String id = event.getComponentId();
        var selectOption = event.getSelectedOptions().get(0);
        String selected = selectOption.getLabel();
        switch (id) {
            case "pet-selection" -> {
                long userIdLong = user.getIdLong();
                if (!Database.USER_SELECTIONS.containsKey(userIdLong)) {
                    me.srin.petbot.database.User usr = me.srin.petbot.database.User.create();
                    Database.USER_SELECTIONS.put(userIdLong, usr);
                }
                me.srin.petbot.database.User usr = Database.USER_SELECTIONS.get(userIdLong);
                Pet pet = Pet.create();
                pet.setName(user.getName() + "'s " + selected);
                pet.setType(selected);
                usr.setPet(pet);
            }
            case "pet-profile" -> {
                long messageId = event.getMessageIdLong();
                var longPairMap = Database.PET_CACHE.get(messageId);
                if (longPairMap == null) {
                    event.deferEdit().queue();
                    return;
                }
                Pair<Pet, User> petUserPair = longPairMap.get(Long.parseLong(selectOption.getValue()));
                Database.SELECTED_PET_CACHE.get(messageId).setPet(petUserPair.getFirst());
                MessageEmbed embed = Utils.getPetStatsEmbed(petUserPair.getFirst(), petUserPair.getSecond());
                event.getHook().editOriginalEmbeds(embed).queue();
            }
        }
    }
}
