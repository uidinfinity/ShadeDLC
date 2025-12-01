package dev.ynki.events.impl.world;

import net.minecraft.client.render.FogShape;
import dev.ynki.events.Event;

public class EventFog extends Event {
    public boolean modified = false;
    public float r, g, b, alpha;
    public float start, end;
    public FogShape shape;
}
