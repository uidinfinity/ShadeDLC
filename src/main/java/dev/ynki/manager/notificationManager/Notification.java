package dev.ynki.manager.notificationManager;

import dev.ynki.manager.fontManager.FontUtils;
import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import dev.ynki.util.animations.Animation;
import dev.ynki.util.animations.impl.EaseBackIn;

@Getter
public class Notification {
    public final Text textComponent;
    public final String icon;
    public final ItemStack itemIcon;
    public final long startTime;
    public final Animation animation = new EaseBackIn(300, 1.0, 0.7f);

    public Notification(Text textComponent, String icon) {
        this(textComponent, icon, ItemStack.EMPTY);
    }

    public Notification(Text textComponent, ItemStack itemIcon) {
        this(textComponent, "", itemIcon);
    }

    private Notification(Text textComponent, String icon, ItemStack itemIcon) {
        this.textComponent = textComponent;
        this.icon = icon;
        this.itemIcon = itemIcon == null ? ItemStack.EMPTY : itemIcon.copy();
        this.startTime = System.currentTimeMillis();
        this.animation.setDirection(net.minecraft.util.math.Direction.AxisDirection.POSITIVE);
    }

    public float getWidth() {
        var font = FontUtils.durman[12];
        float textWidth = font.getWidth(textComponent.getString());
        return 21.0F + 0.75F + 28.5F + textWidth;
    }

    public boolean hasItemIcon() {
        return !itemIcon.isEmpty();
    }
}