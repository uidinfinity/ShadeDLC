package dev.ynki.modules.render;

import dev.ynki.events.Event;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import dev.ynki.modules.setting.ModeSetting;

@FunctionAnnotation(name = "ItemPhysic",desc  = "Красиво лежат предметы на земле", type = Type.Render)
public class ItemPhysic extends Function {

    public final ModeSetting mode = new ModeSetting("Физика","Обычная","Обычная","2D");
    public ItemPhysic() {
        addSettings(mode);
    }

    @Override
    public void onEvent(Event event) {
    }
}