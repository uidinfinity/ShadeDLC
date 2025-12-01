package dev.ynki.modules.combat;


import dev.ynki.events.Event;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;

@FunctionAnnotation(name = "NoFriendDamage", keywords = {"NFD","FriendDamage"}, type = Type.Combat, desc = "Отключает урон по друзьям")
public class NoFriendDamage extends Function {
    public NoFriendDamage() {
        addSettings();
    }
    @Override
    public void onEvent(Event event) {
    }
}
