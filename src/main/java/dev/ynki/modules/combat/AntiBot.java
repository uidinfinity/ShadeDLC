package dev.ynki.modules.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import dev.ynki.modules.setting.BooleanSetting;
import dev.ynki.events.Event;
import dev.ynki.events.impl.EventUpdate;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("All")
@FunctionAnnotation(name = "AntiBot", desc = "Удаляет ботов на сервере", type = Type.Combat)
public class AntiBot extends Function {
    private final BooleanSetting removeWorld = new BooleanSetting("Удалить из мира", false);
    public static final List<Entity> isBot = new ArrayList<>();

    public AntiBot() {
        addSettings(removeWorld);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventUpdate) {
            detectBots();
        }
    }

    private void detectBots() {
        if (mc.world == null || mc.player == null) return;
        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (entity == mc.player) continue;

            if (matchesBotTemplate(entity)) {
                if (!isBot.contains(entity)) {
                    isBot.add(entity);
                    if (removeWorld.get()) {
                        mc.world.removeEntity(entity.getId(), Entity.RemovalReason.KILLED);
                    }
                }
            } else {
                isBot.remove(entity);
            }
        }
    }

    private boolean matchesBotTemplate(PlayerEntity entity) {
        ItemStack boots = entity.getInventory().getArmorStack(0);
        ItemStack leggings = entity.getInventory().getArmorStack(1);
        ItemStack chestplate = entity.getInventory().getArmorStack(2);
        ItemStack helmet = entity.getInventory().getArmorStack(3);

        boolean fullArmor = !boots.isEmpty() && !leggings.isEmpty() && !chestplate.isEmpty() && !helmet.isEmpty();
        boolean enchantable = boots.isEnchantable() && leggings.isEnchantable() && chestplate.isEnchantable() && helmet.isEnchantable();

        boolean validArmorTypes =
                boots.getItem() == Items.LEATHER_BOOTS || leggings.getItem() == Items.LEATHER_LEGGINGS
                        || chestplate.getItem() == Items.LEATHER_CHESTPLATE || helmet.getItem() == Items.LEATHER_HELMET
                        || boots.getItem() == Items.IRON_BOOTS || leggings.getItem() == Items.IRON_LEGGINGS
                        || chestplate.getItem() == Items.IRON_CHESTPLATE || helmet.getItem() == Items.IRON_HELMET;

        boolean offhandEmpty = entity.getOffHandStack().isEmpty();
        boolean mainHandNotEmpty = !entity.getMainHandStack().isEmpty();
        boolean armorNotDamaged = !boots.isDamaged() && !leggings.isDamaged() && !chestplate.isDamaged() && !helmet.isDamaged();
        boolean foodFull = entity.getHungerManager().getFoodLevel() == 20;

        return fullArmor && enchantable && validArmorTypes && offhandEmpty && mainHandNotEmpty && armorNotDamaged && foodFull;
    }

    public boolean check(LivingEntity entity) {
        return checkBot(entity);
    }

    public static boolean checkBot(LivingEntity entity) {
        return entity instanceof PlayerEntity && isBot.contains(entity);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        isBot.clear();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        isBot.clear();
    }
}
