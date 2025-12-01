package dev.ynki.manager.dragManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import dev.ynki.manager.IMinecraft;
import dev.ynki.util.color.ColorUtil;
import dev.ynki.util.render.RenderUtil;
import org.joml.Vector4f;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class DragManager implements IMinecraft {
    public static LinkedHashMap<String, Dragging> draggables = new LinkedHashMap<>();
    
    // Selection system
    private boolean isSelecting = false;
    private boolean hasSelection = false;
    private boolean isMovingSelection = false;
    private float selectionStartX = 0f;
    private float selectionStartY = 0f;
    private float selectionCurrentX = 0f;
    private float selectionCurrentY = 0f;
    private final List<Dragging> selectedDraggables = new ArrayList<>();
    private final List<Float> initialXPositions = new ArrayList<>();
    private final List<Float> initialYPositions = new ArrayList<>();
    private float selectionOffsetX = 0f;
    private float selectionOffsetY = 0f;

    public final File DRAG_DATA = new File(MinecraftClient.getInstance().runDirectory, "\\files\\drag.ew");
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

    public void init() {
        if (!DRAG_DATA.exists()) {
            System.out.println("Файл с позициями draggable не найден. Будет создан новый файл после сохранения.");
            return;
        }

        try {
            Dragging[] loadedDrags = GSON.fromJson(Files.readString(DRAG_DATA.toPath()), Dragging[].class);

            if (loadedDrags != null) {
                for (Dragging dragging : loadedDrags) {
                    if (dragging != null) {
                        Dragging currentDrag = draggables.get(dragging.getName());
                        if (currentDrag != null) {
                            currentDrag.setX(dragging.getX());
                            currentDrag.setY(dragging.getY());
                            draggables.put(dragging.getName(), currentDrag);
                        } else {
                            draggables.put(dragging.getName(), dragging);
                        }
                    }
                }
                System.out.println("Позиции draggable элементов загружены из файла.");

            } else {
                System.out.println("Данные в файле пусты или повреждены.");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void save() {
        if (!DRAG_DATA.exists()) {
            DRAG_DATA.getParentFile().mkdirs();
        }

        try (FileWriter writer = new FileWriter(DRAG_DATA)) {
            writer.write(GSON.toJson(draggables.values()));
            System.out.println("Позиции draggable элементов успешно сохранены в файл " + DRAG_DATA.getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void reset() {
        float off = 10;
        for (Dragging dragging : draggables.values()) {
            float newX = dragging.getDefaultX() + 10;
            float newY = dragging.getDefaultY() + off;
            dragging.setX(newX);
            dragging.setY(newY);
            dragging.targetX = newX;
            dragging.targetY = newY;

            off += dragging.getHeight() + 15;
        }
        save();
    }
    
    public void startSelection(double mouseX, double mouseY) {
        isSelecting = true;
        hasSelection = false;
        selectionStartX = (float) mouseX;
        selectionStartY = (float) mouseY;
        selectionCurrentX = (float) mouseX;
        selectionCurrentY = (float) mouseY;
        selectedDraggables.clear();
        isMovingSelection = false;
        initialXPositions.clear();
        initialYPositions.clear();
    }
    
    public void updateSelection(double mouseX, double mouseY) {
        if (!isSelecting) return;
        selectionCurrentX = (float) mouseX;
        selectionCurrentY = (float) mouseY;
        updateSelectedDraggables();
    }
    
    private void updateSelectedDraggables() {
        selectedDraggables.clear();
        float minX = Math.min(selectionStartX, selectionCurrentX);
        float maxX = Math.max(selectionStartX, selectionCurrentX);
        float minY = Math.min(selectionStartY, selectionCurrentY);
        float maxY = Math.max(selectionStartY, selectionCurrentY);
        
        for (Dragging dragging : draggables.values()) {
            if (dragging.getModule() == null || !dragging.getModule().state) continue;
            
            float dragX = dragging.getX();
            float dragY = dragging.getY();
            float dragW = dragging.getWidth();
            float dragH = dragging.getHeight();
            
            float dragCenterX = dragX + dragW / 2f;
            float dragCenterY = dragY + dragH / 2f;
            
            if (dragCenterX >= minX && dragCenterX <= maxX && 
                dragCenterY >= minY && dragCenterY <= maxY) {
                if (!selectedDraggables.contains(dragging)) {
                    selectedDraggables.add(dragging);
                }
            }
        }
    }
    
    private void moveSelection(double mouseX, double mouseY) {
        if (selectedDraggables.isEmpty() || initialXPositions.size() != selectedDraggables.size()) return;
        
        float deltaX = (float) (mouseX - selectionOffsetX);
        float deltaY = (float) (mouseY - selectionOffsetY);
        
        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();
        
        for (int i = 0; i < selectedDraggables.size(); i++) {
            Dragging dragging = selectedDraggables.get(i);
            float initialX = initialXPositions.get(i);
            float initialY = initialYPositions.get(i);
            
            float newX = initialX + deltaX;
            float newY = initialY + deltaY;
            
            newX = Math.max(0, Math.min(newX, screenWidth - dragging.getWidth()));
            newY = Math.max(0, Math.min(newY, screenHeight - dragging.getHeight()));
            
            dragging.targetX = newX;
            dragging.targetY = newY;
        }
    }
    
    public void endSelection() {
        if (!isSelecting) return;
        isSelecting = false;
        hasSelection = !selectedDraggables.isEmpty();
        isMovingSelection = false;
        initialXPositions.clear();
        initialYPositions.clear();
    }
    
    public boolean tryBeginSelectionMove(double mouseX, double mouseY) {
        if (!hasSelection || selectedDraggables.isEmpty()) return false;
        if (!isMouseOverSelection(mouseX, mouseY)) return false;
        
        isMovingSelection = true;
        selectionOffsetX = (float) mouseX;
        selectionOffsetY = (float) mouseY;
        initialXPositions.clear();
        initialYPositions.clear();
        for (Dragging dragging : selectedDraggables) {
            initialXPositions.add(dragging.getX());
            initialYPositions.add(dragging.getY());
        }
        return true;
    }
    
    public void updateSelectionMove(double mouseX, double mouseY) {
        if (!isMovingSelection) return;
        moveSelection(mouseX, mouseY);
    }
    
    public void finishSelectionMove() {
        if (!isMovingSelection) return;
        isMovingSelection = false;
        hasSelection = false;
        selectedDraggables.clear();
        initialXPositions.clear();
        initialYPositions.clear();
    }
    
    public void renderSelection(MatrixStack matrices) {
        if (isSelecting) {
            float minX = Math.min(selectionStartX, selectionCurrentX);
            float maxX = Math.max(selectionStartX, selectionCurrentX);
            float minY = Math.min(selectionStartY, selectionCurrentY);
            float maxY = Math.max(selectionStartY, selectionCurrentY);
            drawSelectionBounds(matrices, minX, minY, maxX, maxY);
        } else if (hasSelection && !selectedDraggables.isEmpty()) {
            for (Dragging dragging : selectedDraggables) {
                float x = dragging.getX() - 2f;
                float y = dragging.getY() - 2f;
                float w = dragging.getWidth() + 4f;
                float h = dragging.getHeight() + 4f;
                drawSelectionBounds(matrices, x, y, x + w, y + h);
            }
        }
    }
    
    public boolean isSelecting() {
        return isSelecting;
    }
    
    public List<Dragging> getSelectedDraggables() {
        return new ArrayList<>(selectedDraggables);
    }

    public boolean hasSelection() {
        return hasSelection && !selectedDraggables.isEmpty();
    }

    public boolean isMovingSelection() {
        return isMovingSelection;
    }

    private void drawSelectionBounds(MatrixStack matrices, float minX, float minY, float maxX, float maxY) {
        float width = Math.max(0, maxX - minX);
        float height = Math.max(0, maxY - minY);
        int fillColor = ColorUtil.rgba(100, 150, 255, 30);
        int borderColor = ColorUtil.rgba(100, 150, 255, 180);
        
        RenderUtil.drawRoundedRect(matrices, minX, minY, width, height,
                new Vector4f(2, 2, 2, 2), fillColor);
        RenderUtil.drawRoundedBorder(matrices, minX, minY, width, height,
                new Vector4f(2, 2, 2, 2), 1.2f, borderColor);
    }

    private boolean isMouseOverSelection(double mouseX, double mouseY) {
        for (Dragging dragging : selectedDraggables) {
            float dragX = dragging.getX();
            float dragY = dragging.getY();
            float dragW = dragging.getWidth();
            float dragH = dragging.getHeight();
            if (mouseX >= dragX && mouseX <= dragX + dragW &&
                mouseY >= dragY && mouseY <= dragY + dragH) {
                return true;
            }
        }
        return false;
    }
}
