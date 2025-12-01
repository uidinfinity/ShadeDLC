package dev.ynki.modules.combat;


import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.*;
import net.minecraft.text.Text;
import dev.ynki.modules.setting.BindSetting;
import dev.ynki.modules.setting.BooleanSetting;
import dev.ynki.modules.setting.ModeSetting;
import dev.ynki.events.Event;
import dev.ynki.events.impl.input.EventKey;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import dev.ynki.manager.notificationManager.NotificationManager;
import dev.ynki.util.player.InventoryUtil;
import dev.ynki.util.player.TimerUtil;

@FunctionAnnotation(name = "AutoSwap", type = Type.Combat,desc = "Позволяет менять предметы по бинду")
public class AutoSwap extends Function {
    private final BindSetting itemSwapKey = new BindSetting("Кнопка смены предмета", 0);
    private final ModeSetting firstItem = new ModeSetting("Первый предмет", "Щит", "Щит", "Яблоко", "Тотем", "Шар", "Фейерверк");
    private final ModeSetting secondItem = new ModeSetting("Второй предмет", "Щит", "Щит", "Яблоко", "Тотем", "Шар", "Фейерверк");

    private final BooleanSetting swapSwordWithAxe = new BooleanSetting("Свап топора на меч", true);
    private final BooleanSetting funTimeAndHolyWorldBypass = new BooleanSetting("Обход FT/HW", false);
    
    private final TimerUtil timer = new TimerUtil();
    private final TimerUtil attackSpeedTimer = new TimerUtil();
    private boolean bypassActive = false;
    private boolean awaitingSwap = false;
    private int pendingSlot = -1;
    private Item pendingNotificationItem = null;
    private boolean hadAttackSpeedRecently = false;
    private int axeSlot = -1;
    private ItemStack lastOffhandStack = null;
    private boolean lastAttackSpeedResult = false;
    private boolean isSwapping = false;

    public AutoSwap() {
        addSettings(itemSwapKey, firstItem, secondItem, swapSwordWithAxe, funTimeAndHolyWorldBypass);
    }

    @Override
    public void onEvent(Event event) {
        // Обработка нажатия клавиши свапа
        if (event instanceof EventKey eventKey && eventKey.key == itemSwapKey.getKey()) {
            if (timer.hasTimeElapsed(300) && !isSwapping) {
                isSwapping = true;
                Item itemA = getItem(firstItem.getIndex());
                Item itemB = getItem(secondItem.getIndex());
                if (itemA == null || itemB == null) {
                    isSwapping = false;
                    return;
                }

                ItemStack offhandStack = mc.player.getOffHandStack();
                Item selectedItem = itemA;
                Item swapItem = itemB;
                
                // Если в оффхенде уже выбранный предмет, свапаем на второй
                if (offhandStack.getItem() == selectedItem) {
                    int slot = getSlot(swapItem);
                    if (slot >= 0) {
                        if (funTimeAndHolyWorldBypass.get()) {
                            timer.reset();
                            bypassActive = true;
                            awaitingSwap = true;
                            pendingSlot = slot;
                            pendingNotificationItem = swapItem;
                        } else {
                            // Используем swapSlotsUniversal для работы во время движения (как в AutoTotem)
                            InventoryUtil.swapSlotsUniversal(slot, 40, false, true);
                            timer.reset();
                            notifySwap(swapItem);
                        }
                    }
                } else {
                    // Иначе свапаем на первый предмет
                    int slot = getSlot(selectedItem);
                    if (slot >= 0) {
                        if (funTimeAndHolyWorldBypass.get()) {
                            timer.reset();
                            bypassActive = true;
                            awaitingSwap = true;
                            pendingSlot = slot;
                            pendingNotificationItem = selectedItem;
                        } else {
                            // Используем swapSlotsUniversal для работы во время движения (как в AutoTotem)
                            InventoryUtil.swapSlotsUniversal(slot, 40, false, true);
                            timer.reset();
                            notifySwap(selectedItem);
                        }
                    }
                }
                isSwapping = false;
            }
        }

        // Обработка обхода FT/HW
        if (bypassActive) {
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.sprintKey.setPressed(false);

            if (awaitingSwap && timer.hasTimeElapsed(90)) {
                awaitingSwap = false;

                if (pendingSlot != -1) {
                    // Используем swapSlotsUniversal для работы во время движения (как в AutoTotem)
                    InventoryUtil.swapSlotsUniversal(pendingSlot, 40, false, true);
                    pendingSlot = -1;
                    notifySwap(pendingNotificationItem);
                    pendingNotificationItem = null;
                }
            }

            if (timer.hasTimeElapsed(150)) {
                bypassActive = false;
                awaitingSwap = false;
                pendingSlot = -1;
                pendingNotificationItem = null;

                updateKeyBinding(mc.options.forwardKey);
                updateKeyBinding(mc.options.backKey);
                updateKeyBinding(mc.options.leftKey);
                updateKeyBinding(mc.options.rightKey);
                updateKeyBinding(mc.options.sprintKey);
            }
        }

        // Проверка attack speed и автоматический свап топора/меча
        if (mc.player != null && (float)mc.player.age % 0.5F == 0.0F) {
            ItemStack offhandItemStack = mc.player.getOffHandStack();
            if (offhandItemStack != lastOffhandStack) {
                lastAttackSpeedResult = hasAttackSpeedDescription(offhandItemStack);
                lastOffhandStack = offhandItemStack;
            }

            if (lastAttackSpeedResult) {
                hadAttackSpeedRecently = true;
                attackSpeedTimer.reset();
            } else if (attackSpeedTimer.hasTimeElapsed(500)) {
                hadAttackSpeedRecently = false;
            }

            if (swapSwordWithAxe.get() && timer.hasTimeElapsed(300)) {
                ItemStack mainHandItemStack = mc.player.getMainHandStack();
                
                // Если в руке меч и в оффхенде шар с attack speed, свапаем на топор
                if (mainHandItemStack.getItem() instanceof SwordItem && 
                    offhandItemStack.getItem() == Items.PLAYER_HEAD && 
                    lastAttackSpeedResult && 
                    timer.hasTimeElapsed(300)) {
                    int axeSlot = getAxeSlot();
                    if (axeSlot != -1) {
                        mc.player.getInventory().selectedSlot = axeSlot;
                        timer.reset();
                    }
                }

                boolean hasAxeInHotbar = getAxeSlot() != -1;
                boolean hasSwordInHotbar = getSwordSlot() != -1;
                
                if (hasAxeInHotbar && hasSwordInHotbar) {
                    // Если нет attack speed, но был недавно, и в руке топор - свапаем на меч
                    if (!lastAttackSpeedResult && hadAttackSpeedRecently && mainHandItemStack.getItem() instanceof AxeItem) {
                        this.axeSlot = mc.player.getInventory().selectedSlot;
                        int swordSlot = getSwordSlot();
                        if (swordSlot != -1) {
                            mc.player.getInventory().selectedSlot = swordSlot;
                            timer.reset();
                        }
                    } 
                    // Если есть attack speed и в руке меч, свапаем обратно на топор
                    else if (lastAttackSpeedResult && mainHandItemStack.getItem() instanceof SwordItem && this.axeSlot != -1) {
                        int axeSlot = getAxeSlot();
                        if (axeSlot != -1) {
                            mc.player.getInventory().selectedSlot = axeSlot;
                            this.axeSlot = -1;
                            timer.reset();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.axeSlot = -1;
        this.hadAttackSpeedRecently = false;
        super.onDisable();
    }

    private int getSlot(Item item) {
        // Возвращаем обычный слот (0-35), swapSlotsUniversal сам сделает конвертацию
        for (int i = 0; i < 36; ++i) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private Item getItem(int index) {
        if (index == 0) {
            return Items.SHIELD;
        } else if (index == 1) {
            return Items.GOLDEN_APPLE;
        } else if (index == 2) {
            return Items.TOTEM_OF_UNDYING;
        } else if (index == 3) {
            return Items.PLAYER_HEAD;
        } else if (index == 4) {
            return Items.FIREWORK_ROCKET;
        } else {
            return null;
        }
    }

    private int getSwordSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof SwordItem) {
                return i;
            }
        }
        return -1;
    }

    private int getAxeSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasAttackSpeedDescription(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        
        // Проверка на специальное имя "Кобры"
        String displayName = itemStack.getName().getString();
        if (displayName.contains("Кобры")) {
            return true;
        }
        
        // Проверка тултипа на наличие attack speed
        // В версии 1.21.4 используем упрощенную проверку через имя и возможные модификаторы
        try {
            // Пытаемся получить тултип через рефлексию для совместимости
            java.lang.reflect.Method getTooltipMethod = null;
            try {
                getTooltipMethod = itemStack.getClass().getMethod("getTooltip", 
                    net.minecraft.entity.player.PlayerEntity.class);
            } catch (NoSuchMethodException e) {
                try {
                    getTooltipMethod = itemStack.getClass().getMethod("getTooltip", 
                        net.minecraft.entity.player.PlayerEntity.class, 
                        java.lang.Object.class);
                } catch (NoSuchMethodException e2) {
                    // Метод не найден, используем только имя
                }
            }
            
            if (getTooltipMethod != null) {
                Object result = getTooltipMethod.invoke(itemStack, mc.player);
                if (result instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Text> tooltip = (java.util.List<Text>) result;
                    for (Text component : tooltip) {
                        String text = component.getString();
                        if (text.contains("Скорости Атаки") || 
                            text.contains("Скорость атаки") || 
                            text.contains("Attack Speed")) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки рефлексии
        }

        return false;
    }

    private void updateKeyBinding(KeyBinding keyMapping) {
        keyMapping.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), keyMapping.getDefaultKey().getCode()));
    }

    private void notifySwap(Item item) {
        if (item == null) {
            return;
        }
        ItemStack stack = new ItemStack(item);
        if (stack.isEmpty()) {
            return;
        }
        String itemName = stack.getName().getString();
        Text text = Text.literal("Свапуто на \"" + itemName + "\"!");
        NotificationManager.add(text, stack);
    }
}
