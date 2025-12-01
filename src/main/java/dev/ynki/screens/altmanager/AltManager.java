package dev.ynki.screens.altmanager;

import org.lwjgl.glfw.GLFW;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;
import dev.ynki.manager.ClientManager;
import dev.ynki.manager.IMinecraft;
import dev.ynki.manager.Manager;
import dev.ynki.util.color.ColorUtil;
import dev.ynki.manager.fontManager.FontUtils;
import dev.ynki.util.math.MathUtil;
import dev.ynki.util.render.RenderUtil;
import dev.ynki.util.render.Scissor;

import static dev.ynki.util.render.RenderUtil.*;

@SuppressWarnings("All")
public class AltManager extends Screen implements IMinecraft {
    private final Screen parent;
    private boolean isTyping = false;
    private final StringBuilder inputText = new StringBuilder();
    private final List<String> accounts = Manager.ACCOUNT_MANAGER.getAccounts();
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;
    private float hoverAnimationInput = 0;
    private float[] hoverAnimations1;
    private float[] hoverAnimations2;
    private int selectedAccountIndex = -1;
    private static final float SCALE = 1.5f;

    private float createHoverAnim = 0f, clearHoverAnim = 0f, randomHoverAnim = 0f;

    private boolean showConfirmDialog = false;
    
    private static final Identifier BACKGROUND_TEXTURE = Identifier.of("velyasik", "mainmenu/backgraund.jpg");
    public AltManager(Screen parent) {
        super(Text.of("Account Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        var matrices = drawContext.getMatrices();
        scrollOffset = MathUtil.lerp(scrollOffset, targetScrollOffset, 8);
        
        // Background texture (как в MainMenu)
        RenderUtil.drawTexture(matrices, BACKGROUND_TEXTURE, 0, 0, this.width, this.height, 0, Color.white.getRGB());

        int centerX = width / 2;
        int centerY = height / 2;

        int inputWidth = (int)(220 * SCALE);
        int inputHeight = (int)(17 * SCALE);
        int inputX = centerX - (int)(110 * SCALE);
        int inputY = centerY - (int)(92 * SCALE);

        boolean isHoveredInput = RenderUtil.isInRegion(mouseX, mouseY, inputX, inputY, inputWidth, inputHeight);
        hoverAnimationInput = MathUtil.lerp(hoverAnimationInput, isHoveredInput ? 1 : 0, 10);
        int nameColor = ColorUtil.interpolateColor(ColorUtil.rgba(180, 180, 180, 255), ColorUtil.rgba(230, 230, 230, 255), hoverAnimationInput);

        // Input field в стиле HUD
        drawBlur(matrices, inputX, inputY, inputWidth, inputHeight, new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());
        int inputBgColor = ColorUtil.rgba(20, 20, 20, (int)(255 * 0.72));
        drawRoundedRect(matrices, inputX, inputY, inputWidth, inputHeight, new Vector4f(6, 6, 6, 6), inputBgColor);
        if (!isTyping) {
            StringBuilder placeholder = new StringBuilder("Enter your name");
            for (int i = 0; i < (System.currentTimeMillis() / 500 % 4); i++) placeholder.append(".");
            FontUtils.durman[15].drawLeftAligned(matrices, placeholder.toString(), inputX + 6, inputY + inputHeight / 2f - 7, nameColor);
        } else {
            StringBuilder builder = new StringBuilder(inputText);
            builder.append((System.currentTimeMillis() / 500 % 2) == 0 ? "_" : "");
            FontUtils.durman[15].drawLeftAligned(matrices, builder.toString(), inputX + 6, inputY + inputHeight / 2f - 7, nameColor);
        }

        int listX = inputX;
        int listY = centerY - (int)(70 * SCALE);
        int listWidth = (int)(220 * SCALE);
        int listHeight = (int)(140 * SCALE);

        // List в стиле HUD
        drawBlur(matrices, listX, listY, listWidth, listHeight, new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());
        int listBgColor = ColorUtil.rgba(20, 20, 20, (int)(255 * 0.72));
        drawRoundedRect(matrices, listX, listY, listWidth, listHeight, new Vector4f(6, 6, 6, 6), listBgColor);

        Scissor.push();
        Scissor.setFromComponentCoordinates(listX, listY, listWidth, listHeight);

        if (hoverAnimations1 == null || hoverAnimations1.length != accounts.size()) hoverAnimations1 = new float[accounts.size()];
        if (hoverAnimations2 == null || hoverAnimations2.length != accounts.size()) hoverAnimations2 = new float[accounts.size()];

        float startY = listY + 5;
        float itemHeight = 35 * SCALE;

        for (int i = 0; i < accounts.size(); i++) {
            float y = startY - scrollOffset + i * itemHeight;

            int entryX = centerX - (int)(105 * SCALE);
            int entryWidth = (int)(140 * SCALE);
            int entryHeight = (int)(30 * SCALE);

            int bgColor = (i == selectedAccountIndex) ? ColorUtil.rgba(30, 30, 30, (int)(255 * 0.75)) : ColorUtil.rgba(20, 20, 20, (int)(255 * 0.72));
            drawRoundedRect(matrices, entryX, y, entryWidth + 10, entryHeight, new Vector4f(6, 6, 6, 6), bgColor);

            FontUtils.durman[15].drawLeftAligned(matrices, accounts.get(i), entryX + 10, y + 5, ColorUtil.rgba(255, 255, 255, 255));
            FontUtils.durman[12].drawLeftAligned(matrices, "Date " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), entryX + 10, y + 20, ColorUtil.rgba(255, 255, 255, (int)(255 * 0.72)));

            int btnWidth = (int)(60 * SCALE);
            int btnHeight = (int)(13 * SCALE);

            int selectBtnX = entryX + entryWidth + (int)(10 * SCALE);
            int selectBtnY = (int)(y);
            boolean accountHovered1 = RenderUtil.isInRegion(mouseX, mouseY, selectBtnX, selectBtnY, btnWidth, btnHeight);
            hoverAnimations1[i] = MathUtil.lerp(hoverAnimations1[i], accountHovered1 ? 1 : 0, 12);

            int baseColor = ColorUtil.rgba(20, 20, 20, (int)(255 * 0.72));
            int hoverColor = ColorUtil.rgba(30, 30, 30, (int)(255 * 0.75));
            int selectBgColor = ColorUtil.blendColorsInt(baseColor, hoverColor, hoverAnimations1[i]);
            drawBlur(matrices, selectBtnX, selectBtnY, btnWidth, btnHeight + 2, new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());
            drawRoundedRect(matrices, selectBtnX, selectBtnY, btnWidth, btnHeight + 2, new Vector4f(6, 6, 6, 6), selectBgColor);
            FontUtils.durman[12].centeredDraw(matrices, "Select", selectBtnX + btnWidth / 2f, selectBtnY + btnHeight / 2f - 6, -1);

            int deleteBtnX = selectBtnX;
            int deleteBtnY = selectBtnY + btnHeight + (int)(3 * SCALE);
            boolean accountHovered2 = RenderUtil.isInRegion(mouseX, mouseY, deleteBtnX, deleteBtnY, btnWidth, btnHeight);
            hoverAnimations2[i] = MathUtil.lerp(hoverAnimations2[i], accountHovered2 ? 1 : 0, 12);
            int baseColor2 = ColorUtil.rgba(20, 20, 20, (int)(255 * 0.72));
            int hoverColor2 = ColorUtil.rgba(30, 30, 30, (int)(255 * 0.75));
            int deleteBgColor = ColorUtil.blendColorsInt(baseColor2, hoverColor2, hoverAnimations2[i]);
            drawBlur(matrices, deleteBtnX, deleteBtnY, btnWidth, btnHeight + 2, new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());
            drawRoundedRect(matrices, deleteBtnX, deleteBtnY, btnWidth, btnHeight + 2, new Vector4f(6, 6, 6, 6), deleteBgColor);
            FontUtils.durman[12].centeredDraw(matrices, "Delete", deleteBtnX + btnWidth / 2f, deleteBtnY + btnHeight / 2f - 6, -1);
        }

        Scissor.unset();
        Scissor.pop();

        int buttonsY = listY + listHeight + (int)(10 * SCALE);
        int buttonWidth = (int)(70 * SCALE);
        int buttonHeight = inputHeight;

        int createX = centerX - buttonWidth - (int)(40 * SCALE);
        int clearX = centerX - (buttonWidth / 2);
        int randomX = centerX + buttonWidth + (int)(-30 * SCALE);

        float animSpeed = 0.04f;

        // Create button в стиле HUD
        boolean isHoveredCreate = RenderUtil.isInRegion(mouseX, mouseY, createX, buttonsY, buttonWidth, buttonHeight);
        if (isHoveredCreate) {
            createHoverAnim = Math.min(1f, createHoverAnim + animSpeed);
        } else {
            createHoverAnim = Math.max(0f, createHoverAnim - animSpeed);
        }
        int baseColor = ColorUtil.rgba(20, 20, 20, (int)(255 * 0.72));
        int hoverColor = ColorUtil.rgba(30, 30, 30, (int)(255 * 0.75));
        int createBgColor = ColorUtil.blendColorsInt(baseColor, hoverColor, createHoverAnim);
        drawBlur(matrices, createX, buttonsY, buttonWidth, buttonHeight, new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());
        drawRoundedRect(matrices, createX, buttonsY, buttonWidth, buttonHeight, new Vector4f(6, 6, 6, 6), createBgColor);
        FontUtils.durman[15].centeredDraw(matrices, "Create", createX + buttonWidth / 2f, buttonsY + buttonHeight / 2f - 7, -1);

        // Clear button в стиле HUD
        boolean isHoveredClear = RenderUtil.isInRegion(mouseX, mouseY, clearX, buttonsY, buttonWidth, buttonHeight);
        if (isHoveredClear) {
            clearHoverAnim = Math.min(1f, clearHoverAnim + animSpeed);
        } else {
            clearHoverAnim = Math.max(0f, clearHoverAnim - animSpeed);
        }
        int clearBgColor = ColorUtil.blendColorsInt(baseColor, hoverColor, clearHoverAnim);
        drawBlur(matrices, clearX, buttonsY, buttonWidth, buttonHeight, new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());
        drawRoundedRect(matrices, clearX, buttonsY, buttonWidth, buttonHeight, new Vector4f(6, 6, 6, 6), clearBgColor);
        FontUtils.durman[15].centeredDraw(matrices, "Clear all", clearX + buttonWidth / 2f, buttonsY + buttonHeight / 2f - 7, -1);

        // Random button в стиле HUD
        boolean isHoveredRandom = RenderUtil.isInRegion(mouseX, mouseY, randomX, buttonsY, buttonWidth, buttonHeight);
        if (isHoveredRandom) {
            randomHoverAnim = Math.min(1f, randomHoverAnim + animSpeed);
        } else {
            randomHoverAnim = Math.max(0f, randomHoverAnim - animSpeed);
        }
        int randomBgColor = ColorUtil.blendColorsInt(baseColor, hoverColor, randomHoverAnim);
        drawBlur(matrices, randomX, buttonsY, buttonWidth, buttonHeight, new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());
        drawRoundedRect(matrices, randomX, buttonsY, buttonWidth, buttonHeight, new Vector4f(6, 6, 6, 6), randomBgColor);
        FontUtils.durman[15].centeredDraw(matrices, "Random", randomX + buttonWidth / 2f, buttonsY + buttonHeight / 2f - 7, -1);

        String accountName = mc.getSession().getUsername();
        FontUtils.durman[12].centeredDraw(matrices, "Selected account: " + accountName, centerX, buttonsY + buttonHeight + (int)(20 * SCALE), -1);
        FontUtils.durman[12].centeredDraw(matrices, "Quantity: " + accounts.size(), centerX, buttonsY + buttonHeight + (int)(40 * SCALE), -1);



        if (showConfirmDialog) {
            drawConfirmDialog(drawContext);
            return;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = width / 2;
        int centerY = height / 2;
        int inputWidth = (int)(220 * SCALE);
        int inputHeight = (int)(17 * SCALE);
        int inputX = centerX - (int)(110 * SCALE);
        int inputY = centerY - (int)(92 * SCALE);

        int buttonWidth = (int)(70 * SCALE);
        int buttonsY = centerY - (int)(70 * SCALE) + (int)(140 * SCALE) + (int)(10 * SCALE);
        int createX = centerX - buttonWidth - (int)(40 * SCALE);
        int clearX = centerX - (buttonWidth / 2);
        int randomX = centerX + buttonWidth + (int)(-30 * SCALE);

        if (RenderUtil.isInRegion(mouseX, mouseY, inputX, inputY, inputWidth, inputHeight) && !isTyping && button == 0) {
            isTyping = true;
            return true;
        }

        if (!RenderUtil.isInRegion(mouseX, mouseY, inputX, inputY, inputWidth, inputHeight) && !RenderUtil.isInRegion(mouseX, mouseY, createX, buttonsY, buttonWidth, inputHeight) && !RenderUtil.isInRegion(mouseX, mouseY, clearX, buttonsY, buttonWidth, inputHeight) && !RenderUtil.isInRegion(mouseX, mouseY, randomX, buttonsY, buttonWidth, inputHeight) && isTyping && button == 0) {
            isTyping = false;
            return true;
        }



        if (RenderUtil.isInRegion(mouseX, mouseY, createX, buttonsY, buttonWidth, inputHeight) && isTyping && button == 0) {
            String newAccount = inputText.toString().trim();
            if (!newAccount.isEmpty() && accounts.stream().noneMatch(a -> a.equalsIgnoreCase(newAccount))) {
                isTyping = false;
                accounts.add(newAccount);
                Manager.ACCOUNT_MANAGER.addAccount(newAccount);
                inputText.setLength(0);
            }
            return true;
        }

        if (showConfirmDialog) {
            int boxWidth = 300;
            int boxHeight = 130;
            int boxX = (width - boxWidth) / 2;
            int boxY = (height - boxHeight) / 2;
            int btnWidth = 90;
            int btnHeight = 28;
            int yesX = boxX + 35;
            int noX = boxX + boxWidth - 35 - btnWidth;
            int btnY = boxY + boxHeight - 50;

            if (RenderUtil.isInRegion(mouseX, mouseY, yesX, btnY, btnWidth, btnHeight)) {
                accounts.clear();
                Manager.ACCOUNT_MANAGER.clearAll();
                selectedAccountIndex = -1;
                showConfirmDialog = false;
                return true;
            }
            if (RenderUtil.isInRegion(mouseX, mouseY, noX, btnY, btnWidth, btnHeight)) {
                showConfirmDialog = false;
                return true;
            }
            return true;
        }

        if (RenderUtil.isInRegion(mouseX, mouseY, clearX, buttonsY, buttonWidth, (int)(17 * SCALE)) && button == 0) {
            showConfirmDialog = true;
            return true;
        }
        if (RenderUtil.isInRegion(mouseX, mouseY, randomX, buttonsY, buttonWidth, inputHeight) && button == 0) {
            int maxNumber = 9999999;
            String prefix = "shadedlc_";
            String randomNumberStr = String.valueOf((int)(Math.random() * (maxNumber + 1)));
            String randomName = prefix + randomNumberStr;
            if (randomName.length() > 16) {
                randomName = randomName.substring(0, 16);
            }

            if (!accounts.contains(randomName)) {
                accounts.add(randomName);
                Manager.ACCOUNT_MANAGER.addAccount(randomName);
            }

            ClientManager.loginAccount(randomName);

            selectedAccountIndex = accounts.indexOf(randomName);
            Manager.ACCOUNT_MANAGER.setLastSelectedAccount(randomName);
            return true;
        }


        int listX = inputX;
        int listY = centerY - (int)(70 * SCALE);
        int listWidth = (int)(220 * SCALE);
        int listHeight = (int)(140 * SCALE);

        if (RenderUtil.isInRegion(mouseX, mouseY, listX, listY, listWidth, listHeight)) {
            float startY = listY + 5;
            float itemHeight = 35 * SCALE;

            int btnWidth = (int)(60 * SCALE);
            int btnHeight = (int)(13 * SCALE);

            int entryX = centerX - (int)(105 * SCALE);
            int entryWidth = (int)(140 * SCALE);
            int entryHeight = (int)(30 * SCALE);

            for (int i = 0; i < accounts.size(); i++) {
                float y = startY - scrollOffset + i * itemHeight;

                if (RenderUtil.isInRegion(mouseX, mouseY, entryX, (int) y, entryWidth + 10, entryHeight) && button == 0) {
                    String selected = accounts.get(i);
                    ClientManager.loginAccount(selected);
                    selectedAccountIndex = i;
                    Manager.ACCOUNT_MANAGER.setLastSelectedAccount(selected);
                    return true;
                }

                int selectBtnX = entryX + entryWidth + (int)(10 * SCALE);
                int selectBtnY = (int)(y);
                if (RenderUtil.isInRegion(mouseX, mouseY, selectBtnX, selectBtnY, btnWidth, btnHeight) && button == 0) {
                    String selected = accounts.get(i);
                    ClientManager.loginAccount(selected);
                    selectedAccountIndex = i;
                    Manager.ACCOUNT_MANAGER.setLastSelectedAccount(selected);
                    return true;
                }

                int deleteBtnX = selectBtnX;
                int deleteBtnY = selectBtnY + btnHeight + (int)(3 * SCALE);
                if (RenderUtil.isInRegion(mouseX, mouseY, deleteBtnX, deleteBtnY, btnWidth, btnHeight) && button == 0) {
                    if (selectedAccountIndex == i) selectedAccountIndex = -1;
                    Manager.ACCOUNT_MANAGER.removeAccount(accounts.get(i));
                    accounts.remove(i);
                    int maxOffset = Math.max(0, (accounts.size() * (int)(38 * SCALE)) - (int)(135 * SCALE));
                    targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, maxOffset));
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int centerY = height / 2;
        int listY = centerY - (int)(70 * SCALE);
        int listHeight = (int)(140 * SCALE);

        if (mouseY >= listY && mouseY <= listY + listHeight) {
            targetScrollOffset -= scrollY * (int)(30 * SCALE);
            int maxOffset = Math.max(0, (accounts.size() * (int)(36 * SCALE)) - listHeight);
            targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, maxOffset));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY,scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isTyping) {
            boolean ctrl = GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
            if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
                String clipboard = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
                if (clipboard != null && !clipboard.isEmpty()) {
                    String filtered = clipboard.replaceAll("[^\\w]", "");
                    int maxLength = 16 - inputText.length();
                    if (maxLength > 0) {
                        if (filtered.length() > maxLength) {
                            filtered = filtered.substring(0, maxLength);
                        }
                        inputText.append(filtered);
                    }
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                String newAccount = inputText.toString().trim();
                if (!newAccount.isEmpty() && accounts.stream().noneMatch(a -> a.equalsIgnoreCase(newAccount))) {
                    isTyping = false;
                    accounts.add(newAccount);
                    Manager.ACCOUNT_MANAGER.addAccount(newAccount);
                    inputText.setLength(0);
                }
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && inputText.length() > 0) {
                inputText.deleteCharAt(inputText.length() - 1);
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }


    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (isTyping) {
            if (chr == '\n' || chr == '\r') return false;
            if (inputText.length() < 16 && (Character.isLetterOrDigit(chr) || chr == '_')) {
                inputText.append(chr);
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        if (parent != null) {
            mc.setScreen(parent);
        }
        super.close();
    }
    private void drawConfirmDialog(DrawContext drawContext) {
        var matrices = drawContext.getMatrices();
        int boxWidth = 300;
        int boxHeight = 130;
        int boxX = (width - boxWidth) / 2;
        int boxY = (height - boxHeight) / 2;

        // Overlay
        RenderUtil.drawRoundedRect(matrices, 0, 0, width, height, 0, new Color(0, 0, 0, 120).getRGB());

        // Dialog box в стиле HUD
        drawBlur(matrices, boxX, boxY, boxWidth, boxHeight, new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());
        int dialogBgColor = ColorUtil.rgba(20, 20, 20, (int)(255 * 0.85));
        drawRoundedRect(matrices, boxX, boxY, boxWidth, boxHeight, new Vector4f(6, 6, 6, 6), dialogBgColor);

        FontUtils.durman[15].centeredDraw(matrices, "Вы точно хотите очистить все аккаунты?", width / 2f, boxY + 30, -1);

        int btnWidth = 90;
        int btnHeight = 28;
        int yesX = boxX + 35;
        int noX = boxX + boxWidth - 35 - btnWidth;
        int btnY = boxY + boxHeight - 50;

        // Yes button в стиле HUD
        drawBlur(matrices, yesX, btnY, btnWidth, btnHeight, new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());
        drawRoundedRect(matrices, yesX, btnY, btnWidth, btnHeight, new Vector4f(6, 6, 6, 6), ColorUtil.rgba(60, 180, 75, 255));
        FontUtils.durman[15].centeredDraw(matrices, "Да", yesX + btnWidth / 2f, btnY + btnHeight / 2f - 6, -1);

        // No button в стиле HUD
        drawBlur(matrices, noX, btnY, btnWidth, btnHeight, new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());
        drawRoundedRect(matrices, noX, btnY, btnWidth, btnHeight, new Vector4f(6, 6, 6, 6), ColorUtil.rgba(200, 60, 60, 255));
        FontUtils.durman[15].centeredDraw(matrices, "Нет", noX + btnWidth / 2f, btnY + btnHeight / 2f - 6, -1);
    }
}