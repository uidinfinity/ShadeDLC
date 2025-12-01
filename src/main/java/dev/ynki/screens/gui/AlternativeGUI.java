package dev.ynki.screens.gui;

import dev.ynki.manager.Manager;
import dev.ynki.modules.Type;
import dev.ynki.modules.render.ClickGUI;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import dev.ynki.util.color.ColorUtil;
import dev.ynki.util.render.RenderUtil;
import dev.ynki.manager.fontManager.FontUtils;
import java.awt.Color;

import static dev.ynki.manager.IMinecraft.*;

public class AlternativeGUI extends Screen {
    private static final int ITEM_WIDTH = 150;
    private static final int ITEM_HEIGHT = 30;
    private static final int MARGIN = 10;
    private static final int MAX_VISIBLE = 8;

    private int scrollOffset = 0;
    private int scrollTarget = 0;

    public AlternativeGUI() {
        super(Text.literal("AlternativeGUI"));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ClickGUI cfg = Manager.FUNCTION_MANAGER.clickGUI;
        int alpha = cfg.alpha.get().intValue();
        Color baseColor = cfg.getGuiColor();
        float rounding = cfg.rounding.get().floatValue();

        // Фон с размытием (если включено)
        if (cfg.blur.get() && cfg.blurSetting.get("Альтернативный")) {
            RenderUtil.drawBlur(ctx.getMatrices(), 0, 0, width, height, 10, 8, -1);
        }

        // Обновление скролла
        scrollOffset = (int) MathHelper.lerp(scrollOffset, scrollTarget, 0.2f);

        // Центр экрана для горизонтального списка
        int centerY = height / 2 - ITEM_HEIGHT / 2;

        java.util.List<Type> categories = java.util.List.of(Type.Combat, Type.Move, Type.Render, Type.Player, Type.Misc);
        int totalWidth = (ITEM_WIDTH + MARGIN) * categories.size() - MARGIN;
        int startX = (width - totalWidth) / 2;

        for (int i = 0; i < categories.size(); i++) {
            int x = startX + i * (ITEM_WIDTH + MARGIN);
            int y = centerY;

            boolean hovered = mouseX >= x && mouseX <= x + ITEM_WIDTH && mouseY >= y && mouseY <= y + ITEM_HEIGHT;

// Подсветка при наведении
            int baseColorInt = baseColor.getRGB();
            int bgColor = hovered ? ColorUtil.brighter(baseColorInt, 30) : baseColorInt;
            RenderUtil.drawRoundedRect(ctx.getMatrices(), x, y, ITEM_WIDTH, ITEM_HEIGHT, rounding, ColorUtil.applyAlpha(bgColor, alpha));

            String name = categories.get(i).name();
            int textColor = hovered ? Color.WHITE.getRGB() : Color.LIGHT_GRAY.getRGB();
            float textWidth = FontUtils.sf_medium[18].getWidth(name);
            FontUtils.sf_medium[18].drawLeftAligned(
                    ctx.getMatrices(),
                    name,
                    x + ITEM_WIDTH / 2f - textWidth / 2f,
                    y + (ITEM_HEIGHT - 18) / 2f,
                    textColor
            );
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Эмулируем горизонтальный скролл (если будет нужно)
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        // Вернёмся к обычному GUI после закрытия
        mc.setScreen(null);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}