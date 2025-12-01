package dev.ynki.events.impl.move;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.Entity;
import dev.ynki.events.Event;

@Getter
@RequiredArgsConstructor
public class EventEntitySpawn extends Event {
    private final Entity entity;
}