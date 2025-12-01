package dev.ynki.modules.player;

import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import dev.ynki.modules.setting.BooleanSetting;
import dev.ynki.events.Event;
import dev.ynki.events.impl.EventPacket;
import dev.ynki.manager.Manager;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;

import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("All")
@FunctionAnnotation(name = "AutoAccept",keywords = "TpaAccept", type = Type.Player, desc = "")
public class AutoAccept extends Function {

    private final BooleanSetting onlyFriend = new BooleanSetting("Только друзей", true);
    public AutoAccept() {
        addSettings(onlyFriend);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventPacket eventPacket) {
            if (eventPacket.getPacket() instanceof GameMessageS2CPacket packet) {
                String message = packet.content().getString();
                if (message.contains("хочет телепортироваться к вам.") || message.contains("просит телепортироваться к Вам")) {
                    String sender = extractName(message);
                    String realName = nameCheck(sender);

                    if (!onlyFriend.get() || Manager.FRIEND_MANAGER.isFriend(realName)) {
                        mc.player.networkHandler.sendCommand("tpaccept");
                    }
                }
            }
        }
    }

    private String extractName(String message) {
        String clean = message.replaceAll("§.", "");
        int spaceIndex = clean.indexOf(' ');
        if (spaceIndex != -1) {
            return clean.substring(0, spaceIndex);
        }

        return "UNKNOWN";
    }

    public static String nameCheck(String notSolved) {
        AtomicReference<String> result = new AtomicReference<>(notSolved);
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().getListedPlayerListEntries().forEach(player -> {
                if (notSolved.equalsIgnoreCase(player.getProfile().getName())) {
                    result.set(player.getProfile().getName());
                }
            });
        }
        return result.get();
    }
}
