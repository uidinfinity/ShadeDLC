package dev.ynki.screens.mainmenu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;
import dev.ynki.screens.altmanager.AltManager;
import dev.ynki.util.color.ColorUtil;
import dev.ynki.manager.fontManager.FontUtils;
import dev.ynki.util.render.RenderUtil;

import java.awt.*;

import static dev.ynki.util.render.RenderUtil.*;

@SuppressWarnings("All")
public class MainMenu extends Screen {

    private Button singleplayerButton;
    private Button multiplayerButton;
    private Button altmanagerButton;
    private CombinedButton optionsQuitButton;

    private static final Identifier BACKGROUND_TEXTURE = Identifier.of("velyasik", "mainmenu/backgraund.jpg");

    public MainMenu() {
        super(Text.literal("Custom Main Menu"));
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 30;

        singleplayerButton = new Button("Singleplayer", "s", 0, 0, buttonWidth, buttonHeight, false);
        multiplayerButton = new Button("Multiplayer", "I", 0, 0, buttonWidth, buttonHeight, true);
        altmanagerButton = new Button("AltManager", "A", 0, 0, buttonWidth, buttonHeight, true);
        optionsQuitButton = new CombinedButton(0, 0, buttonWidth, buttonHeight, "Options", "Quit");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        var matrices = context.getMatrices();
        
        // Background texture
        RenderUtil.drawTexture(matrices, BACKGROUND_TEXTURE, 0, 0, this.width, this.height, 0, Color.white.getRGB());
        
        // Надпись shadedlc - return по центру (другой шрифт)
        var titleFont = FontUtils.durman[24];
        String title = "shadedlc - return";
        float titleY = this.height / 5f;
        
        titleFont.centeredDraw(matrices, title, this.width / 2f, titleY, -1);

        int spacing = 12;
        int buttonWidth = 200;
        int buttonHeight = 30;
        float titleHeight = titleFont.getHeight();
        float buttonsStartY = titleY + titleHeight + spacing * 2;

        int centerX = this.width / 2 - buttonWidth / 2;

        singleplayerButton.x = centerX;
        singleplayerButton.y = (int)buttonsStartY;

        multiplayerButton.x = centerX;
        multiplayerButton.y = (int)(buttonsStartY + buttonHeight + spacing);

        altmanagerButton.x = centerX;
        altmanagerButton.y = (int)(buttonsStartY + 2 * (buttonHeight + spacing));

        optionsQuitButton.x = centerX;
        optionsQuitButton.y = (int)(buttonsStartY + 3 * (buttonHeight + spacing));
        optionsQuitButton.width = buttonWidth;

        singleplayerButton.render(context, mouseX, mouseY, delta);
        multiplayerButton.render(context, mouseX, mouseY, delta);
        altmanagerButton.render(context, mouseX, mouseY, delta);
        optionsQuitButton.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (singleplayerButton.isHovered(mouseX, mouseY)) {
            this.client.setScreen(new SelectWorldScreen(this));
            return true;
        }
        if (multiplayerButton.isHovered(mouseX, mouseY)) {
            this.client.setScreen(new MultiplayerScreen(this));
            return true;
        }
        if (altmanagerButton.isHovered(mouseX, mouseY)) {
            this.client.setScreen(new AltManager(this));
            return true;
        }
        if (optionsQuitButton.isOptionHovered(mouseX, mouseY)) {
            this.client.setScreen(new OptionsScreen(this, client.options));
            return true;
        }
        if (optionsQuitButton.isQuitHovered(mouseX, mouseY)) {
            this.client.scheduleStop();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private class Button {
        final String name;
        final String icon;
        final boolean largeIcon;
        int x, y, width, height;

        private float hoverAnim = 0f;

        Button(String name, String icon, int x, int y, int width, int height, boolean largeIcon) {
            this.name = name;
            this.icon = icon;
            this.largeIcon = largeIcon;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        void render(DrawContext context, int mouseX, int mouseY, float delta) {
            var matrices = context.getMatrices();
            boolean hovered = isHovered(mouseX, mouseY);

            float animSpeed = 0.04f;

            if (hovered) {
                hoverAnim = Math.min(1f, hoverAnim + animSpeed);
            } else {
                hoverAnim = Math.max(0f, hoverAnim - animSpeed);
            }

            // Blur for button (как в HUD)
            drawBlur(matrices, x, y, width, height,
                    new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());

            int baseColor = ColorUtil.rgba(20, 20, 20, (int)(255 * 0.72));
            int hoverColor = ColorUtil.rgba(30, 30, 30, (int)(255 * 0.75));
            int bgColor = ColorUtil.blendColorsInt(baseColor, hoverColor, hoverAnim);

            // Фон (как в HUD)
            drawRoundedRect(matrices, x, y, width, height, new Vector4f(6, 6, 6, 6), bgColor);

            // Текст слева
            var textFont = FontUtils.durman[15];
            float textY = y + (height - textFont.getHeight()) / 2f;
            float textX = x + 10; // Отступ слева
            textFont.drawLeftAligned(matrices, name, textX, textY, -1);

            // Иконка справа
            var iconFont = FontUtils.mainicon[largeIcon ? 24 : 18];
            float iconY = y + (height - iconFont.getHeight()) / 2f;
            float iconX = x + width - iconFont.getWidth(icon) - 10; // Отступ справа
            iconFont.drawLeftAligned(matrices, icon, iconX, iconY, -1);
        }

        boolean isHovered(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private class CombinedButton {
        int x, y, width, height;
        final String leftName, rightName;

        private float leftHoverAnim = 0f;
        private float rightHoverAnim = 0f;

        CombinedButton(int x, int y, int width, int height, String leftName, String rightName) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.leftName = leftName;
            this.rightName = rightName;
        }

        void render(DrawContext context, int mouseX, int mouseY, float delta) {
            var matrices = context.getMatrices();
            int buttonGap = 2;
            int halfWidth = width / 2;
            int shrink = 4;

            boolean leftHovered = isOptionHovered(mouseX, mouseY);
            boolean rightHovered = isQuitHovered(mouseX, mouseY);

            float animSpeed = 0.04f;

            if (leftHovered) {
                leftHoverAnim = Math.min(1f, leftHoverAnim + animSpeed);
            } else {
                leftHoverAnim = Math.max(0f, leftHoverAnim - animSpeed);
            }

            if (rightHovered) {
                rightHoverAnim = Math.min(1f, rightHoverAnim + animSpeed);
            } else {
                rightHoverAnim = Math.max(0f, rightHoverAnim - animSpeed);
            }

            int baseColor = ColorUtil.rgba(20, 20, 20, (int)(255 * 0.72));
            int hoverColor = ColorUtil.rgba(30, 30, 30, (int)(255 * 0.75));

            int buttonWidth = halfWidth - shrink;

            // Left button (Options)
            int leftX = x + buttonGap;
            int leftBg = ColorUtil.blendColorsInt(baseColor, hoverColor, leftHoverAnim);
            
            drawBlur(matrices, leftX, y, buttonWidth, height,
                    new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());
            drawRoundedRect(matrices, leftX, y, buttonWidth, height, new Vector4f(6, 6, 6, 6), leftBg);
            
            var textFont = FontUtils.durman[15];
            float textY = y + (height - textFont.getHeight()) / 2f;
            float textX = leftX + 10; // Текст слева
            textFont.drawLeftAligned(matrices, leftName, textX, textY, -1);
            
            // Иконка "c" справа (маленькая)
            var iconFont = FontUtils.mainicon[18];
            float iconY = y + (height - iconFont.getHeight()) / 2f;
            float iconX = leftX + buttonWidth - iconFont.getWidth("c") - 10;
            iconFont.drawLeftAligned(matrices, "c", iconX, iconY, -1);

            // Right button (Quit)
            int rightX = x + halfWidth + buttonGap;
            int rightBg = ColorUtil.blendColorsInt(baseColor, hoverColor, rightHoverAnim);
            
            drawBlur(matrices, rightX, y, buttonWidth, height,
                    new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());
            drawRoundedRect(matrices, rightX, y, buttonWidth, height, new Vector4f(6, 6, 6, 6), rightBg);
            
            float rightTextX = rightX + 10; // Текст слева
            textFont.drawLeftAligned(matrices, rightName, rightTextX, textY, -1);
            
            // Иконка "x" справа (маленькая)
            float rightIconX = rightX + buttonWidth - iconFont.getWidth("x") - 10;
            iconFont.drawLeftAligned(matrices, "x", rightIconX, iconY, -1);
        }

        boolean isOptionHovered(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width / 2 - 1 && mouseY >= y && mouseY <= y + height;
        }

        boolean isQuitHovered(double mouseX, double mouseY) {
            return mouseX > x + width / 2 + 1 && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
