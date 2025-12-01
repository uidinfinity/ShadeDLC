package dev.ynki.modules.misc;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Formatting;
import dev.ynki.modules.setting.BindSetting;
import dev.ynki.modules.setting.BooleanSetting;
import dev.ynki.events.Event;
import dev.ynki.events.impl.input.EventKey;
import dev.ynki.events.impl.EventUpdate;
import dev.ynki.manager.ClientManager;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import dev.ynki.util.player.InventoryUtil;
import dev.ynki.util.player.TimerUtil;

@FunctionAnnotation(name = "ElytraHelper", desc = "Помогает свапать элитры с инвентаря", type = Type.Misc)
public class ElytraHelper extends Function {
    private final BindSetting swapChestKey = new BindSetting("Элитры", 0);
    private final BindSetting fireWorkKey = new BindSetting("Фейерверк", 0);
    private final BooleanSetting autoFly = new BooleanSetting("Авто взлёт", true);
    private final BooleanSetting autoJump = new BooleanSetting("Авто прыжок", true);
    private final BooleanSetting autofireWork = new BooleanSetting("Авто фейерверк", false);
    private final BooleanSetting swap = new BooleanSetting("Фейр в левую руку", false);
    private final BooleanSetting autofireWorkstart = new BooleanSetting("Только при взлёте", false, () -> this.autofireWork.get());

    ItemStack currentStack;
    private final TimerUtil stopWatch;
    boolean fireworkUsed;
    private final TimerUtil timerUtil;
    private boolean recentlySwapped;
    private final TimerUtil swapCooldownTimer;
    private boolean hasFiredOnStart;

    public ElytraHelper() {
        this.currentStack = ItemStack.EMPTY;
        this.stopWatch = new TimerUtil();
        this.timerUtil = new TimerUtil();
        this.recentlySwapped = false;
        this.swapCooldownTimer = new TimerUtil();
        this.hasFiredOnStart = false;
        this.addSettings(swapChestKey, fireWorkKey, autoJump, autoFly, autofireWork, autofireWorkstart, swap);
    }

    @Override
    public void onEvent(Event event) {
        if (mc.player == null) return;

        if (event instanceof EventUpdate) {
            // Автопрыжок - проверка через armorInventory как в monotone
            if (this.autoJump.get()) {
                if (!mc.player.getAbilities().flying) {
                    if (mc.player.isOnGround()) {
                        // Проверяем слот нагрудника (индекс 2 в armorInventory)
                        ItemStack chestStack = mc.player.getInventory().armor.get(2);
                        if (chestStack.getItem() == Items.ELYTRA && !mc.options.jumpKey.isPressed()) {
                            if (!mc.player.isSubmergedInWater()) {
                                if (!mc.player.isInLava()) {
                                    mc.player.jump();
                                }
                            }
                        }
                    }
                }
            }

            // Автовзлёт - используем startFallFlying() и пакет как в monotone
            if (this.autoFly.get()) {
                if (!mc.player.getAbilities().flying) {
                    if (!mc.player.isOnGround()) {
                        if (!mc.player.isGliding()) {
                            // Проверяем слот нагрудника (индекс 2 в armorInventory)
                            ItemStack chestStack = mc.player.getInventory().armor.get(2);
                            if (chestStack.getItem() == Items.ELYTRA) {
                                // Начинаем полёт
                                mc.player.startGliding();
                                // Отправляем пакет на сервер
                                mc.player.networkHandler.sendPacket(
                                        new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
                                );

                                // Автофейерверк при взлёте
                                if (this.autofireWork.get() && this.autofireWorkstart.get() && !this.hasFiredOnStart) {
                                    if (InventoryUtil.getItemSlot(Items.FIREWORK_ROCKET) != -1) {
                                        InventoryUtil.inventorySwapClick2(Items.FIREWORK_ROCKET, false, false);
                                        this.hasFiredOnStart = true;
                                    } else {
                                        ClientManager.message(Formatting.WHITE + "У вас не были найдены" + Formatting.RED + " фейерверки");
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Сброс флага при приземлении
            if (mc.player.isOnGround() || mc.player.isSubmergedInWater() || mc.player.isInLava()) {
                this.hasFiredOnStart = false;
            }

            // Автофейерверк во время полёта
            if (mc.player.isGliding() && this.autofireWork.get() && !this.autofireWorkstart.get() && this.timerUtil.hasTimeElapsed(570L)) {
                if (InventoryUtil.getItemSlot(Items.FIREWORK_ROCKET) != -1) {
                    InventoryUtil.inventorySwapClick2(Items.FIREWORK_ROCKET, false, false);
                } else {
                    ClientManager.message(Formatting.WHITE + "У вас не были найдены" + Formatting.RED + " фейерверки");
                }
                this.timerUtil.reset();
            }

            this.currentStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
            if (this.recentlySwapped && this.swapCooldownTimer.hasTimeElapsed(2000L)) {
                this.recentlySwapped = false;
            }

            if (this.fireworkUsed) {
                this.useFirework();
                this.fireworkUsed = false;
            }
        }

        if (event instanceof EventKey) {
            EventKey e = (EventKey) event;
            if (e.key == this.swapChestKey.getKey() && this.stopWatch.hasTimeElapsed(150L)) {
                if (this.getItemSlot(Items.ELYTRA) == -1) {
                    this.changeChestPlate(this.currentStack);
                    this.stopWatch.reset();
                } else {
                    this.changeChestPlate(this.currentStack);
                    this.stopWatch.reset();
                }

                this.recentlySwapped = true;
                this.swapCooldownTimer.reset();
            }

            if (e.key == this.fireWorkKey.getKey()) {
                this.fireworkUsed = true;
            }
        }
    }

    private void changeChestPlate(ItemStack stack) {
        int armorSlot;
        int freeSlot;
        if (stack.getItem() != Items.ELYTRA) {
            armorSlot = this.getItemSlot(Items.ELYTRA);
            freeSlot = this.findFreeInventorySlot();
            if (armorSlot >= 0) {
                InventoryUtil.moveItem(armorSlot, 6, false);
            } else if (freeSlot >= 0) {
                // Empty else block
            }
        } else {
            armorSlot = this.getChestPlateSlot();
            freeSlot = this.findFreeInventorySlot();
            if (armorSlot >= 0) {
                InventoryUtil.moveItem(armorSlot, 6, false);
            } else if (freeSlot >= 0) {
                InventoryUtil.moveItem(6, freeSlot, false);
            }
        }
    }

    private int findFreeInventorySlot() {
        for (int i = 10; i < 36; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private int getChestPlateSlot() {
        Item[] items = new Item[]{Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.IRON_CHESTPLATE, Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE};
        Item[] var2 = items;
        int var3 = items.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            Item item = var2[var4];

            for (int i = 0; i < 36; ++i) {
                Item stack = mc.player.getInventory().getStack(i).getItem();
                if (stack == item) {
                    if (i < 9) {
                        i += 36;
                    }
                    return i;
                }
            }
        }

        return -1;
    }

    private int getItemSlot(Item item) {
        int finalSlot = -1;

        for (int i = 0; i < 36; ++i) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                finalSlot = i;
                break;
            }
        }

        if (finalSlot < 9 && finalSlot != -1) {
            finalSlot += 36;
        }

        return finalSlot;
    }

    private void useFirework() {
        if (InventoryUtil.getItemSlot(Items.FIREWORK_ROCKET) == -1) {
            if (mc.player.isGliding()) {
                ClientManager.message(Formatting.WHITE + "У вас не были найдены" + Formatting.RED + " фейерверки");
            }
        } else {
            InventoryUtil.inventorySwapClick2(Items.FIREWORK_ROCKET, false, false);
        }
    }

    @Override
    protected void onDisable() {
        this.stopWatch.reset();
        this.timerUtil.reset();
        this.hasFiredOnStart = false;
        super.onDisable();
    }
}