package dev.ynki.modules.player;

import dev.ynki.events.Event;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;

@FunctionAnnotation(name = "NoRayTrace",keywords = {"NoEntityTrace"}, desc = "Убирает хитбокс энтити", type = Type.Player)
public class NoRayTrace extends Function {

    @Override
    public void onEvent(Event event) {
    }
}