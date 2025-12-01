package dev.ynki.modules.combat;

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import dev.ynki.modules.setting.ModeSetting;
import dev.ynki.events.Event;
import dev.ynki.events.impl.EventPacket;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;

@FunctionAnnotation(name = "Velocity",keywords = {"AKB","AntiKnockBack"}, type = Type.Combat, desc = "Отключает отбрасывание")
public class Velocity extends Function {
    public ModeSetting mode = new ModeSetting("Тип", "Cancel", "Cancel");

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventPacket eventPacket) {
            if (mode.is("Cancel")) {
                if (eventPacket.getPacket() instanceof EntityVelocityUpdateS2CPacket entityVelocityUpdateS2CPacket) {
                    if (entityVelocityUpdateS2CPacket.getEntityId() == mc.player.getId()) {
                        eventPacket.setCancel(true);
                    }
                }
            }
        }
    }
}