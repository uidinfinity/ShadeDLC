package dev.ynki.manager.notificationManager;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import dev.ynki.manager.IMinecraft;
import dev.ynki.manager.Manager;
import dev.ynki.manager.fontManager.FontUtils;
import dev.ynki.util.color.ColorUtil;
import dev.ynki.util.render.RenderUtil;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager implements IMinecraft {
    private static final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();
    private static final Map<String, Long> lastTotemNotification = new ConcurrentHashMap<>();

    public static void add(Text textComponent, String icon) {
        notifications.add(new Notification(textComponent, icon));
    }

    public static void add(Text textComponent, ItemStack itemIcon) {
        notifications.add(new Notification(textComponent, itemIcon));
    }

    public static void addTotemPop(String playerName) {
        if (playerName == null || playerName.isEmpty()) return;
        String key = playerName.toLowerCase();
        long now = System.currentTimeMillis();
        Long lastTime = lastTotemNotification.get(key);
        if (lastTime != null && now - lastTime < 500) {
            return;
        }
        lastTotemNotification.put(key, now);

        String displayName = playerName;
        try {
            if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.nameProtect != null) {
                displayName = Manager.FUNCTION_MANAGER.nameProtect.getProtectedName(playerName);
            }
        } catch (Exception ignored) {}
        Text text = Text.literal("Игроку \"" + displayName + "\" снесли тотем!");
        notifications.add(new Notification(text, new ItemStack(Items.TOTEM_OF_UNDYING)));
    }

    public void draw(DrawContext context) {
        MatrixStack stack = context.getMatrices();
        if (mc == null || mc.getWindow() == null) {
            return; // Safety check for null context
        }

        notifications.removeIf((e) -> System.currentTimeMillis() - e.startTime > 3000L &&
                e.animation.finished(Direction.AxisDirection.NEGATIVE));

        float yOffset = 0.0F;

        for (Notification notification : notifications) {
            if (System.currentTimeMillis() - notification.startTime > 3000L) {
                notification.animation.setDirection(Direction.AxisDirection.NEGATIVE);
            }

            // Динамический расчёт ширины: иконка + разделитель + ширина текста + отступы
            var font12 = FontUtils.durman[12];
            var icon10 = FontUtils.velyasik[11];
            float textWidth = font12.getWidth(notification.textComponent.getString());
            float notificationWidth = 21.0F + 0.75F + textWidth + 10.0F; // 10 - общий отступ справа (можно подкорректировать)

            float x = ((float)mc.getWindow().getScaledWidth() - notificationWidth) / 2.0F;
            float y = (float)mc.getWindow().getScaledHeight() / 2.0F + 55.0F + yOffset;

            // Apply animation transform using MatrixStack instead of direct GL calls
            stack.push();
            stack.translate(x + notificationWidth / 2.0F, y + 10.5F, 0.0F);
            stack.scale((float)notification.animation.getOutput(), (float)notification.animation.getOutput(), (float)notification.animation.getOutput());
            stack.translate(-(x + notificationWidth / 2.0F), -(y + 10.5F), 0.0F);

            // Blur или LiquidGlass background
            boolean liquidglass = Manager.FUNCTION_MANAGER != null && 
                                Manager.FUNCTION_MANAGER.hud != null && 
                                Manager.FUNCTION_MANAGER.hud.isLiquidGlassEnabled();
            boolean blur = Manager.FUNCTION_MANAGER != null && 
                          Manager.FUNCTION_MANAGER.hud != null && 
                          Manager.FUNCTION_MANAGER.hud.isBlurEnabled();
            
            if (liquidglass) {
                // Параметры точно как в оригинальном примере
                RenderUtil.drawLiquidGlass(stack, x, y, notificationWidth, 21.0F, 
                    new org.joml.Vector4f(6, 6, 6, 6), 1.0f, 9.0f, 25.0f, 1.2f, Color.white.getRGB());
            } else if (blur) {
                RenderUtil.drawBlur(stack, x, y, notificationWidth, 21.0F, 6.0F, 24.0F, Color.white.getRGB());
            }

            // Background (лёгкий серый при liquidglass)
            int bgColor = liquidglass
                ? ColorUtil.rgba(59, 59, 59, 90)
                : ColorUtil.rgba(20, 20, 20, (int)(255 * 0.72));
            RenderUtil.drawRoundedRect(stack, x, y, notificationWidth, 21.0F, 6.0F, bgColor);

            // Icon (centered in 21x21 area)
            if (notification.hasItemIcon()) {
                stack.push();
                float iconCenterX = x + 2.0F + 8.0F;
                float iconCenterY = y + 2.0F + 8.0F;
                stack.translate(iconCenterX, iconCenterY, 0.0F);
                stack.scale(0.75F, 0.75F, 1.0F);
                stack.translate(-iconCenterX, -iconCenterY, 0.0F);
                context.drawItem(notification.getItemIcon(), (int)(x + 2.0F), (int)(y + 2.0F), 0);
                stack.pop();
            } else {
                icon10.drawLeftAligned(stack, notification.getIcon(),
                        x + 1.0F + (21.0F - icon10.getWidth(notification.getIcon())) / 2.0F,
                        y + (21.0F - icon10.getHeight()) / 2.0F, -1);
            }

            // Vertical separator
            RenderUtil.drawRoundedRect(stack, x + 21.0F, y, 0.75F, 21.0F, 0.0F,
                    ColorUtil.rgba(255, 255, 255, (int)(255 * 0.21)));

            // Text (сдвинут на 2 пикселя левее)
            font12.drawLeftAligned(stack, notification.textComponent.getString(),
                    x + 26.5F,
                    y + (21.0F - font12.getHeight()) / 2.0F - 1.0F, -1);

            stack.pop();

            yOffset += 25.0F;
        }
    }

    public boolean onClick(double mouseX, double mouseY, int button) {
        // Notifications in SkyCore style don't need drag handling
        return false;
    }

    public void onRelease(int button) {
        // Notifications in SkyCore style don't need drag handling
    }
}