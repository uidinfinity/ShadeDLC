package dev.ynki.modules.misc;

import dev.ynki.modules.setting.BooleanSetting;
import dev.ynki.modules.setting.TextSetting;
import dev.ynki.events.Event;
import dev.ynki.manager.Manager;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;

@FunctionAnnotation(name = "NameProtect", desc = "", type = Type.Misc)
public class NameProtect extends Function {
    public final TextSetting text = new TextSetting("Ник","uidinfinity");
    public final BooleanSetting friend = new BooleanSetting("Скрывать друзей",true);

    public NameProtect() {
        addSettings(text,friend);
    }
    public String getCustomName() {
        return Manager.FUNCTION_MANAGER.nameProtect.state ? text.getValue().replaceAll("&", "\u00a7") : mc.getGameProfile().getName();
    }
    public String getProtectedName(String originalName) {
        if (!Manager.FUNCTION_MANAGER.nameProtect.state) return originalName;

        if (isSelf(originalName)) {
            return applyFormatting(text.getValue());
        }

        if (friend.get() && Manager.FRIEND_MANAGER.isFriend(originalName)) {
            return applyFormatting(text.getValue());
        }

        return originalName;
    }
    private String applyFormatting(String name) {
        return name.replace('&', '§');
    }

    private boolean isSelf(String name) {
        return name.equals(mc.getSession().getUsername());
    }
    @Override
    public void onEvent(Event event) {

    }
}