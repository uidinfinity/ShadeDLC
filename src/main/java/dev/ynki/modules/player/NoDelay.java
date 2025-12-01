package dev.ynki.modules.player;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import dev.ynki.events.Event;
import dev.ynki.events.impl.EventUpdate;
import dev.ynki.mixin.iface.MinecraftClientAccessor;
import dev.ynki.mixin.iface.MixinLivingEntityAccessor;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import dev.ynki.modules.setting.BooleanSetting;

@FunctionAnnotation(name = "NoDelay" ,desc  = "Убирает задержку предметам", type = Type.Player)
public class NoDelay extends Function {

    private final BooleanSetting jump = new BooleanSetting("Прыжок",true);
    private final BooleanSetting xp = new BooleanSetting("Пузырёк опыта",true);
    private final BooleanSetting crystal = new BooleanSetting("Кристаллы",true);
    private final BooleanSetting place = new BooleanSetting("ПКМ",false);

    public NoDelay() {
        addSettings(jump,xp,crystal,place);
    }


    @Override
    public void onEvent(Event event) {
        if (event instanceof EventUpdate) {
            if (jump.get()) {
                ((MixinLivingEntityAccessor) mc.player).setLastJumpCooldown(0);
            }
            if (check(mc.player.getMainHandStack().getItem()))
                ((MinecraftClientAccessor) mc).setUseCooldown(0);
        }
    }
    private boolean check(Item item) {
        return (item instanceof BlockItem && place.get()) || (item == Items.END_CRYSTAL && crystal.get()) || (item == Items.EXPERIENCE_BOTTLE && xp.get());
    }
}
