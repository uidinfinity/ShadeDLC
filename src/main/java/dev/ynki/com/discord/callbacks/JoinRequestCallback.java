package dev.ynki.com.discord.callbacks;

import com.sun.jna.Callback;
import dev.ynki.com.discord.DiscordUser;


public interface JoinRequestCallback extends Callback {
    void apply(final DiscordUser p0);
}
