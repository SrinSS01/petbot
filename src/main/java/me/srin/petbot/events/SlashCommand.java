package me.srin.petbot.events;

import me.srin.petbot.database.Database;
import me.srin.petbot.database.Pet;
import me.srin.petbot.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.util.Pair;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static me.srin.petbot.utils.Utils.SELECTION_BUILDER;

public class SlashCommand extends Event {

    private SlashCommand(Database database) {
        super(database);
        for (String type : database.getConfig().getPetSpecies()) {
            SELECTION_BUILDER.addOption(type, type);
        }
    }

    public static SlashCommand create(Database database) {
        return new SlashCommand(database);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        Member selfMember = guild.getMemberById(event.getJDA().getSelfUser().getId());
        if (selfMember == null) {
            return;
        }
        if (!selfMember.hasPermission(Permission.VIEW_CHANNEL)) {
            event.reply("I do not have permission `VIEW_CHANNEL`").setEphemeral(true).queue();
            return;
        }
        String command = event.getName();
        User user = event.getUser();
        long user_id = user.getIdLong();
        long guild_id = guild.getIdLong();
        switch (command) {
            case "buy-pet" -> {
                var res = database.getPetUserRDBMSRepository().countByUserIdAndGuildId(user_id, guild_id);
                if (res >= database.getConfig().getPetLimit()) {
                    event.deferReply().setEphemeral(true).queue();
                    InteractionHook hook = event.getHook();
                    MessageEmbed embed = new EmbedBuilder()
                            .setTitle("**Pet shop**")
                            .setDescription("_You don't have any empty pet slots left\nYou can have upto %d pets..._".formatted(database.getConfig().getPetLimit()))
                            .build();
                    hook.editOriginalEmbeds(embed).queue();
                } else {
                    event.deferReply().queue();
                    InteractionHook hook = event.getHook();
                    Utils.buyPet(hook, user_id);
                }
            }
            case "view-stats" -> {
                var usr = database.getUserRepo().findById(me.srin.petbot.database.User.ID.of(user_id, guild_id));
                if (usr.isEmpty()) {
                    event.reply("who are you?").setEphemeral(true).queue();
                    return;
                }
                var petIds = database.getPetUserRDBMSRepository().findByUserIdAndGuildId(user_id, guild_id);
                if (petIds.isEmpty()) {
                    event.reply("You don't have any pets").setEphemeral(true).queue();
                    return;
                }
                var pets = database.getPetRepo().findAllById(petIds);
                StringBuilder sb = new StringBuilder();
                pets.forEach(pet ->
                        sb.append('`').append(pet.getName()).append('`').append('\n')
                          .append(Utils.getProgressBar(pet.getXp(), pet.getXpLimit())).append("\n\n"));
                MessageEmbed embed = new EmbedBuilder()
                        .setTitle("**User stats**")
                        .setThumbnail(user.getAvatarUrl())
                        .setDescription(
                                """
                                `user:` %s
                                `xp:` (%d/%d)
                                `level`: %d
                                
                                _progress_
                                %s
                                                            
                                _pets progress_
                                %s
                                """.formatted(
                                        user.getAsTag(),
                                        usr.get().getXp(), usr.get().getXpLimit(),
                                        usr.get().getLevel(),
                                        Utils.getProgressBar(usr.get().getXp(), usr.get().getXpLimit()),
                                        sb.toString()
                                )
                        ).setTimestamp(new Date().toInstant()).build();
                event.replyEmbeds(embed).queue();
            }
            case "view-pets", "view-pets-user" -> {
                OptionMapping option = event.getOption("user");
                boolean isModerator = false;
                if (option != null) {
                    user = option.getAsUser();
                    isModerator = true;
                }
                List<Long> petIds = database.getPetUserRDBMSRepository().findByUserIdAndGuildId(user.getIdLong(), guild_id);
                if (petIds.isEmpty()) {
                    event.reply("You don't own any pets").setEphemeral(true).queue();
                    return;
                }
                event.deferReply().queue();
                List<Pet> pets = database.getPetRepo().findAllById(petIds);
                SelectMenu.Builder builder = SelectMenu.create("pet-profile");
                for (Pet pet : pets) {
                    String petName = pet.getName();
                    builder.addOption(petName, String.valueOf(pet.getId()), pet.getType());
                }
                SelectMenu selectionMenu = builder.build();
                Pet pet = pets.get(0);
                MessageEmbed embed = Utils.getPetStatsEmbed(pet, user);
                User finalUser = user;
                Map<Long, Pair<Pet, User>> petMap = new HashMap<>();
                for (Pet p : pets) {
                    petMap.put(p.getId(), Pair.of(p, finalUser));
                }
                if (!isModerator) {
                    event.getHook().editOriginalEmbeds(embed).setComponents(
                            ActionRow.of(selectionMenu),
                            ActionRow.of(
                                    Button.primary("train-pet", "Train"),
                                    Button.primary("change-name", "Change name"),
                                    Button.primary("set-pfp", "Set pfp"),
                                    Button.danger("disown", "Disown")
                            )
                    ).queue(message -> {
                        long messageIdLong = message.getIdLong();
                        Database.PET_CACHE.put(messageIdLong, petMap);
                        me.srin.petbot.database.User usr = me.srin.petbot.database.User.create();
                        usr.setUserId(user_id);
                        usr.setPet(pet);
                        Database.SELECTED_PET_CACHE.put(messageIdLong, usr);
                        message.editMessageComponents(
                                ActionRow.of(selectionMenu.asDisabled()),
                                ActionRow.of(
                                        Button.primary("train-pet", "Train").asDisabled(),
                                        Button.primary("change-name", "Change name").asDisabled(),
                                        Button.primary("set-pfp", "Set pfp").asDisabled(),
                                        Button.danger("disown", "Disown").asDisabled()
                                )
                        ).queueAfter(10, TimeUnit.MINUTES, m -> {
                            long idLong = m.getIdLong();
                            Database.PET_CACHE.remove(idLong);
                            Database.SELECTED_PET_CACHE.remove(idLong);
                        });
                    });
                } else {
                    event.getHook().editOriginalEmbeds(embed).setComponents(ActionRow.of(selectionMenu)).queue(message -> {
                        long messageIdLong = message.getIdLong();
                        Database.PET_CACHE.put(messageIdLong, petMap);
                        me.srin.petbot.database.User usr = me.srin.petbot.database.User.create();
                        usr.setUserId(user_id);
                        usr.setPet(pet);
                        Database.SELECTED_PET_CACHE.put(messageIdLong, usr);
                        message.editMessageComponents(
                                ActionRow.of(selectionMenu.asDisabled())
                        ).queueAfter(10, TimeUnit.MINUTES, m -> {
                            long idLong = m.getIdLong();
                            Database.PET_CACHE.remove(idLong);
                            Database.SELECTED_PET_CACHE.remove(idLong);
                        });
                    });
                }
            }
            case "start-training" -> {
                OptionMapping petOption = event.getOption("pet");
                if (petOption == null) {
                    return;
                }
                String petName = petOption.getAsString();
                var petIds = database.getPetUserRDBMSRepository().findByUserIdAndGuildId(user_id, guild_id);
                var usr = database.getUserRepo().findById(me.srin.petbot.database.User.ID.of(user_id, guild_id));
                if (petIds.isEmpty()) {
                    event.reply("You don't own any pets").setEphemeral(true).queue();
                    return;
                }
                List<Pet> pets = database.getPetRepo().findAllById(petIds);
                Optional<Pet> optionalPet = pets.stream().filter(pet -> pet.getName().equals(petName)).findAny();
                if (optionalPet.isPresent() && usr.isPresent()) {
                    Pet pet = optionalPet.get();
                    Member member = event.getMember();
                    Map<Pet, Long> map = Database.LAST_TIME_CACHE.get(member);
                    Long lastTime = map == null? 0L: Objects.requireNonNullElse(map.get(pet), 0L);
                    long dTime = System.currentTimeMillis() - lastTime;
                    if (dTime <= database.getConfig().getTrainingCooldownInSeconds()) {
                        event.replyFormat("Oh your pet is still training, wait for <t:%d:T> until you can use this command again", dTime).setEphemeral(true).queue();
                        return;
                    }
                    boolean isLevelup = pet.train();
                    if (isLevelup) {
                        usr.get().levelUp();
                    }
                    database.getPetRepo().save(pet);
                    Database.LAST_TIME_CACHE.put(member, Map.of(pet, System.currentTimeMillis() / 1000));
                } else {
                    event.reply("You don't own any pets named " + petName).setEphemeral(true).queue();
                }
            }
        }
    }
}
