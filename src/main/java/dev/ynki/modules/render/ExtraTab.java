package dev.ynki.modules.render;

import dev.ynki.events.Event;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;

@FunctionAnnotation(name = "ExtraTab",desc  = "Количество игроков табе больше", type = Type.Render)
public class ExtraTab extends Function {

    @Override
    public void onEvent(Event event) {

    }
}