package dev.ynki.modules.combat;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.Direction;
import org.joml.Vector2f;
import dev.ynki.events.Event;
import dev.ynki.events.impl.EventUpdate;
import dev.ynki.events.impl.input.EventKeyBoard;
import dev.ynki.events.impl.move.EventMotion;
import dev.ynki.events.impl.world.EventObsidianPlace;
import dev.ynki.manager.Manager;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import dev.ynki.modules.setting.MultiSetting;
import dev.ynki.modules.setting.BooleanSetting;
import dev.ynki.modules.setting.SliderSetting;
import dev.ynki.util.move.MoveUtil;
import dev.ynki.util.player.InventoryUtil;
import dev.ynki.util.player.TimerUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@FunctionAnnotation(name = "AutoExplosion", type = Type.Combat, desc = "Автоматически размещает и взрывает кристаллы")
public class AutoExplosion extends Function {
    private final MultiSetting protection = new MultiSetting("Не взрывать", Arrays.asList("Себя", "Друзей", "Предметы"), new String[]{"Себя", "Друзей", "Предметы"});
    private final BooleanSetting correction = new BooleanSetting("Коррекция движения", true);
    private final SliderSetting speedplace = new SliderSetting("Задержка взрыва", 1.5F, 0.5F, 2.0F, 0.1F);

    private Entity crystalEntity = null;
    private BlockPos obsidianPos = null;
    private int oldCurrentSlot = -1;
    public Vector2f serverRot;
    private final TimerUtil attackStopWatch = new TimerUtil();
    private final TimerUtil obsidianPlaceTimer = new TimerUtil();
    private int bestSlot = -1;
    private int oldSlot = -1;
    private boolean waitingForCrystalPlacement = false;

    public AutoExplosion() {
        this.addSettings(protection, correction, speedplace);
        // Устанавливаем начальные значения для защиты
        protection.setSelected(Arrays.asList("Себя", "Друзей", "Предметы"));
    }

    @Override
    public void onEvent(Event event) {
        if (mc.player == null) return;

        if (event instanceof EventObsidianPlace) {
            EventObsidianPlace e = (EventObsidianPlace) event;
            if (e.getBlock() == Blocks.OBSIDIAN) {
                this.handleObsidianPlace(e.getPos());
            }
        } else if (event instanceof EventUpdate) {
            this.handleUpdate();
        } else if (event instanceof EventMotion) {
            EventMotion e = (EventMotion) event;
            this.handleMotion(e);
        } else if (event instanceof EventKeyBoard) {
            EventKeyBoard e = (EventKeyBoard) event;
            this.handleInput(e);
        }
    }

    @Override
    protected void onDisable() {
        this.reset();
        super.onDisable();
    }

    private void handleObsidianPlace(BlockPos obsidianPos) {
        this.obsidianPos = obsidianPos;
        this.waitingForCrystalPlacement = true;
        this.obsidianPlaceTimer.reset();
    }

    private void handleUpdate() {
        if (this.waitingForCrystalPlacement && this.obsidianPos != null && this.obsidianPlaceTimer.hasTimeElapsed((long) (this.speedplace.get().floatValue() * 70L))) {
            boolean isOffHand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
            int slotInHotBar = InventoryUtil.getHotBarSlot(Items.END_CRYSTAL);
            int slotInInventory = InventoryUtil.getItemSlot(Items.END_CRYSTAL);

            this.bestSlot = this.findBestSlotInHotBar();

            if (isOffHand && this.bestSlot >= 0 && this.bestSlot < 9) {
                this.setAndUseCrystal(this.bestSlot, this.obsidianPos);
            } else {
                if (slotInHotBar >= 0 && slotInHotBar < 9) {
                    this.oldCurrentSlot = mc.player.getInventory().selectedSlot;
                    this.setAndUseCrystal(slotInHotBar, this.obsidianPos);
                    mc.player.getInventory().selectedSlot = this.oldCurrentSlot;
                } else if (slotInInventory >= 0 && slotInInventory < 36 && this.bestSlot >= 0 && this.bestSlot < 9) {
                    this.oldCurrentSlot = mc.player.getInventory().selectedSlot;
                    this.oldSlot = slotInInventory;
                    this.inventorySwapClick(Items.END_CRYSTAL, slotInInventory, this.bestSlot);
                    this.setAndUseCrystal(this.bestSlot, this.obsidianPos);
                    if (mc.player.getInventory().getStack(this.bestSlot).getItem() == Items.END_CRYSTAL) {
                        this.inventorySwapClick(Items.END_CRYSTAL, this.bestSlot, this.oldSlot);
                    }
                    mc.player.getInventory().selectedSlot = this.oldCurrentSlot;
                }
            }

            this.waitingForCrystalPlacement = false;
        }

        // Взрываем все кристаллы в радиусе досягаемости
        this.findAllNearbyCrystals().forEach(this::attackCrystal);

        if (this.crystalEntity != null && !this.crystalEntity.isAlive()) {
            this.reset();
        }

        if (this.crystalEntity != null) {
            this.updateRotation();
        }
    }

    private void handleMotion(EventMotion e) {
        if (this.check()) {
            e.setYaw(this.serverRot.x);
            e.setPitch(this.serverRot.y);
            mc.player.setYaw(this.serverRot.x);
            mc.player.setBodyYaw(this.serverRot.x);
            mc.player.setPitch(this.serverRot.y);
        }
    }

    private void handleInput(EventKeyBoard e) {
        if (this.check()) {
            MoveUtil.fixMovement(e, this.serverRot.x);
        }
    }

    public boolean check() {
        return this.serverRot != null && this.correction.get() && this.crystalEntity != null && this.obsidianPos != null;
    }

    private void attackCrystal(Entity entity) {
        if (this.isValid(entity)) {
            if (mc.player.getAttackCooldownProgress(0) >= 1.0F && this.attackStopWatch.hasTimeElapsed((long) (this.speedplace.get().floatValue() * 100L))) {
                mc.interactionManager.attackEntity(mc.player, entity);
                mc.player.swingHand(Hand.MAIN_HAND);
                this.crystalEntity = entity;
                this.attackStopWatch.reset();
            }
        }

        if (!entity.isAlive()) {
            this.reset();
        }
    }

    private void setAndUseCrystal(int slot, BlockPos pos) {
        if (slot >= 0 && slot < 9) {
            boolean isOffHand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
            Vec3d center = Vec3d.ofCenter(pos);

            if (!isOffHand) {
                mc.player.getInventory().selectedSlot = slot;
            }

            Hand hand = isOffHand ? Hand.OFF_HAND : Hand.MAIN_HAND;

            BlockHitResult hitResult = new BlockHitResult(center, Direction.UP, pos, false);
            if (mc.interactionManager.interactBlock(mc.player, hand, hitResult) == ActionResult.SUCCESS) {
                mc.player.swingHand(hand);
            }
        }
    }

    private void updateRotation() {
        if (this.crystalEntity != null) {
            Vec3d targetPos = this.crystalEntity.getPos();
            Vec3d vec = targetPos.subtract(mc.player.getEyePos());
            float yawToTarget = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0F);
            float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vec.y, Math.sqrt(vec.x * vec.x + vec.z * vec.z))));

            if (this.serverRot == null) {
                this.serverRot = new Vector2f(mc.player.getYaw(), mc.player.getPitch());
            }

            float pitch = MathHelper.clamp(pitchToTarget, -89.0F, 89.0F);
            float gcd = this.getGCDValue();
            float yaw = yawToTarget - (yawToTarget - this.serverRot.x) % gcd;
            pitch -= (pitch - this.serverRot.y) % gcd;

            this.serverRot = new Vector2f(yaw, pitch);
        }
    }

    private float getGCDValue() {
        float sensitivity = (float) (mc.options.getMouseSensitivity().getValue() * 0.6000000238418579D + 0.20000000298023224D);
        float gcd = sensitivity * sensitivity * sensitivity * 8.0F;
        return gcd * 0.15F;
    }

    private boolean isValid(Entity base) {
        if (base == null) {
            return false;
        } else {
            if (this.protection.get("Себя") && this.obsidianPos != null) {
                if (mc.player.getY() > (double) this.obsidianPos.getY()) {
                    return false;
                }
            }

            if (this.protection.get("Друзей") && base instanceof EndCrystalEntity && this.isNearFriend(base)) {
                return false;
            } else {
                return !this.protection.get("Предметы") || !this.isNearItem(base) ? this.isCorrectDistanceToCrystal(base) : false;
            }
        }
    }

    private boolean isNearFriend(Entity crystal) {
        Box box = new Box(crystal.getX() - 6.0D, crystal.getY() - 6.0D, crystal.getZ() - 6.0D, crystal.getX() + 6.0D, crystal.getY() + 6.0D, crystal.getZ() + 6.0D);
        return mc.world.getOtherEntities(null, box).stream()
                .filter(entity -> entity instanceof PlayerEntity)
                .anyMatch(entity -> Manager.FRIEND_MANAGER.isFriend(entity.getName().getString()));
    }

    private boolean isNearItem(Entity crystal) {
        Box box = new Box(crystal.getX() - 6.0D, crystal.getY() - 6.0D, crystal.getZ() - 6.0D, crystal.getX() + 6.0D, crystal.getY() + 6.0D, crystal.getZ() + 6.0D);
        return mc.world.getOtherEntities(null, box).stream()
                .filter(entity -> entity instanceof net.minecraft.entity.ItemEntity)
                .anyMatch(entity -> {
                    net.minecraft.entity.ItemEntity itemEntity = (net.minecraft.entity.ItemEntity) entity;
                    Item item = itemEntity.getStack().getItem();
                    return item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS
                            || item == Items.DIAMOND_HELMET || item == Items.DIAMOND_CHESTPLATE || item == Items.DIAMOND_LEGGINGS || item == Items.DIAMOND_BOOTS
                            || item == Items.NETHERITE_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_PICKAXE || item == Items.DIAMOND_PICKAXE
                            || item == Items.NETHERITE_SHOVEL || item == Items.DIAMOND_SHOVEL || item == Items.NETHERITE_AXE || item == Items.DIAMOND_AXE
                            || item == Items.TOTEM_OF_UNDYING || item == Items.END_CRYSTAL || item == Items.APPLE || item == Items.ENCHANTED_GOLDEN_APPLE
                            || item == Items.GOLDEN_CARROT || item == Items.ENDER_PEARL || item == Items.TRIDENT || item == Items.CROSSBOW
                            || item == Items.PLAYER_HEAD || item == Items.ELYTRA;
                });
    }

    private boolean isCorrectDistanceToCrystal(Entity crystal) {
        if (crystal == null) {
            return false;
        } else {
            return mc.player.getPos().distanceTo(crystal.getPos()) <= 6.0;
        }
    }


    private List<Entity> findAllNearbyCrystals() {
        Vec3d playerPos = mc.player.getPos();
        double reach = 6.0;
        Box box = new Box(playerPos.x - reach, playerPos.y - reach, playerPos.z - reach, playerPos.x + reach, playerPos.y + reach, playerPos.z + reach);
        return mc.world.getOtherEntities(null, box).stream()
                .filter(entity -> entity instanceof EndCrystalEntity)
                .filter(entity -> mc.player.getPos().distanceTo(entity.getPos()) <= reach)
                .collect(Collectors.toList());
    }

    private void reset() {
        this.crystalEntity = null;
        this.obsidianPos = null;
        this.serverRot = null;
        this.oldCurrentSlot = -1;
        this.bestSlot = -1;
        this.oldSlot = -1;
        this.waitingForCrystalPlacement = false;
        this.attackStopWatch.reset();
        this.obsidianPlaceTimer.reset();
    }

    private int findBestSlotInHotBar() {
        for (int i = 0; i < 9; ++i) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }

        for (int i = 0; i < 9; ++i) {
            if (mc.player.getInventory().getStack(i).getItem() != Items.END_CRYSTAL) {
                if (mc.player.getInventory().getStack(i).getItem() != Items.OBSIDIAN) {
                    return i;
                }
            }
        }

        return -1;
    }

    private void inventorySwapClick(Item item, int fromSlot, int toSlot) {
        if (fromSlot != toSlot && fromSlot >= 0 && toSlot >= 0 && fromSlot < 36 && toSlot < 36) {
            if (item == Items.END_CRYSTAL) {
                if (mc.player.getInventory().getStack(fromSlot).getItem() == Items.END_CRYSTAL) {
                    int from = fromSlot < 9 ? fromSlot + 36 : fromSlot;
                    int to = toSlot < 9 ? toSlot + 36 : toSlot;

                    boolean slotNotNull = !mc.player.getInventory().getStack(toSlot).isEmpty();

                    if (slotNotNull) {
                        mc.interactionManager.clickSlot(0, from, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(0, to, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(0, from, 0, SlotActionType.PICKUP, mc.player);
                    } else {
                        mc.interactionManager.clickSlot(0, from, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(0, to, 0, SlotActionType.PICKUP, mc.player);
                    }
                }
            }
        }
    }
}



