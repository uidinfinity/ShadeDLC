package dev.ynki.modules.player;

import dev.ynki.modules.setting.MultiSetting;
import dev.ynki.events.Event;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import java.util.Arrays;

@FunctionAnnotation(name = "NoPush" ,desc  = "Убивает коллизию от разных типов", type = Type.Player)
public class NoPush extends Function {
    public MultiSetting mods = new MultiSetting(
            "Типы",
            Arrays.asList("Игроки", "Блоки"),
            new String[]{"Вода", "Игроки", "Блоки"}
    );
    public NoPush() {
        addSettings(mods);
    }

    @Override
    public void onEvent(Event event) {
    }
}
