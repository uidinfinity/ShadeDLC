package dev.ynki.modules.render;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import dev.ynki.events.impl.input.EventMouse;
import dev.ynki.manager.fontManager.RenderFonts;
import dev.ynki.modules.setting.*;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import dev.ynki.Velyasik;
import dev.ynki.events.Event;
import dev.ynki.events.impl.EventUpdate;
import dev.ynki.events.impl.render.EventRender2D;
import dev.ynki.manager.ClientManager;
import dev.ynki.manager.Manager;
import dev.ynki.manager.dragManager.Dragging;
import dev.ynki.manager.fontManager.FontUtils;
import dev.ynki.manager.themeManager.StyleManager;
import dev.ynki.mixin.iface.BossBarHudAccessor;
import dev.ynki.mixin.iface.ItemCooldownEntryAccessor;
import dev.ynki.mixin.iface.ItemCooldownManagerAccessor;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import dev.ynki.modules.setting.*;
import dev.ynki.util.animations.Animation;
import dev.ynki.util.animations.impl.EaseBackIn;
import dev.ynki.util.animations.impl.EaseInOutQuad;
import dev.ynki.util.color.ColorUtil;
import dev.ynki.util.math.MathUtil;
import dev.ynki.util.render.RenderAddon;
import dev.ynki.util.render.RenderUtil;
import dev.ynki.util.render.Scissor;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.ynki.util.color.ColorUtil.hud_color;
import static dev.ynki.util.render.RenderUtil.*;

@SuppressWarnings("All")
@FunctionAnnotation(name = "HUD", desc = "Интерфейс клиента", type = Type.Render)
public class HUD extends Function {
    public final MultiSetting setting = new MultiSetting(
            "Элементы",
            Arrays.asList("WaterMark", "TargetHUD", "KeyBinds", "StaffList", "PotionHUD", "ItemCoolDownHUD", "Coordinates / TPS","ArmorHUD", "Notifications", "DynamicIsland", "MusicRender", "Scoreboard"),
            new String[]{"WaterMark", "TargetHUD", "KeyBinds", "StaffList", "PotionHUD", "ItemCoolDownHUD", "Coordinates / TPS","ArmorHUD", "Notifications", "DynamicIsland", "MusicRender", "Scoreboard"});


    private final ModeSetting hudColor = new ModeSetting("Цвет худа","Обычный","Обычный","Зависит от темы");
    private final ModeSetting gradientType = new ModeSetting(() -> hudColor.is("Зависит от темы"),"Тип градиента", "Слева направо", "Слева направо", "Справа налево");

    private final SliderSetting customAlpha = new SliderSetting("Прозрачность", 120, 120, 255, 5);
    private final BooleanSetting visibleCrosshair = new BooleanSetting("Показывать TargetHUD при навидении", false, "показывает таргетхуд при навидении на игрока", () -> setting.get("TargetHUD"));
    private final BooleanSetting blur = new BooleanSetting("Размытие", false, "Рендерит размытие на все элементы худа");
    private final BooleanSetting liquidglass = new BooleanSetting("LiquidGlass", false, "Рендерит liquidglass эффект на все элементы худа");
    
    public boolean isLiquidGlassEnabled() {
        return liquidglass.get();
    }
    
    public boolean isBlurEnabled() {
        return blur.get();
    }
    private final SliderSetting roundingSilaSanya = new SliderSetting("Закругление головы", 2f, 0f, 12f, 1f);
    private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");
    private static final Pattern PREFIX_MATCHES = Pattern.compile(".*(mod|мод|adm|адм|help|хелп|curat|курат|own|овн|dev|supp|сапп|yt|ют|сотруд).*", Pattern.CASE_INSENSITIVE);

    private static final Item[] TRACKED_ITEMS = {
            Items.ENDER_PEARL, Items.CHORUS_FRUIT, Items.FIREWORK_ROCKET, Items.SHIELD,
            Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.TOTEM_OF_UNDYING,
            Items.SNOWBALL, Items.DRIED_KELP, Items.ENDER_EYE, Items.NETHERITE_SCRAP,
            Items.EXPERIENCE_BOTTLE, Items.PHANTOM_MEMBRANE
    };

    private static final Map<Item, String> ITEM_NAMES;
    static {
        Map<Item, String> tmp = new HashMap<>(16);
        tmp.put(Items.ENDER_PEARL, "Эндер-жемчюг");
        tmp.put(Items.CHORUS_FRUIT, "Хорус");
        tmp.put(Items.FIREWORK_ROCKET, "Фейрверк");
        tmp.put(Items.SHIELD, "Щит");
        tmp.put(Items.GOLDEN_APPLE, "Золотое яблоко");
        tmp.put(Items.ENCHANTED_GOLDEN_APPLE, "Чарка");
        tmp.put(Items.TOTEM_OF_UNDYING, "Тотем");
        tmp.put(Items.SNOWBALL, "Снежок");
        tmp.put(Items.DRIED_KELP, "Пласт");
        tmp.put(Items.ENDER_EYE, "Дезориентация");
        tmp.put(Items.NETHERITE_SCRAP, "Трапка");
        tmp.put(Items.EXPERIENCE_BOTTLE, "Пузырёк опыта");
        tmp.put(Items.PHANTOM_MEMBRANE, "Аура");
        ITEM_NAMES = Collections.unmodifiableMap(tmp);
    }

    public HUD() {
        addSettings(setting,hudColor,gradientType, customAlpha, visibleCrosshair, blur, liquidglass, roundingSilaSanya);
        mediaPlayerRenderer = new MediaPlayerRenderer("MusicRender", 10, 250);
    }

    public MediaPlayerRenderer mediaPlayerRenderer;

    public final Dragging watermarkDrag = Velyasik.getInstance().createDrag(this, "WaterMark", 10, 10);
    public final Dragging targethudDrag = Velyasik.getInstance().createDrag(this, "TargetHUD", 10, 45);
    public final Dragging keybindsDrag = Velyasik.getInstance().createDrag(this, "KeyBindsHUD", 10, 95);
    public final Dragging stafflistDrag = Velyasik.getInstance().createDrag(this, "StaffListHUD", 10, 128);
    public final Dragging itemcooldownDrag = Velyasik.getInstance().createDrag(this, "CoolDownHUD", 10, 165);
    public final Dragging potionhudDrag = Velyasik.getInstance().createDrag(this, "PotionHUD", 10, 198);
    public final Dragging coordinateshudDrag = Velyasik.getInstance().createDrag(this, "CoordinatesHUD", 10, 198);
    public final Dragging armorDrag = Velyasik.getInstance().createDrag(this, "ArmorHUD", 478, 468);
    public final Dragging scoreboardDrag = Velyasik.getInstance().createDrag(this, "ScoreboardHUD", 10, 10);

    Animation tHudAnimation = new EaseBackIn(300, 1.0, 0.7f);
    private final Vector4f corner = new Vector4f(3, 0, 0, 3);
    LivingEntity target = null;
    AbstractClientPlayerEntity targetPlayer = null;
    float health = 0f;
    float health2 = 0f;
    int activeModules = 0;
    private float heightDynamic = 0f;
    private double scale = 0.0D;

    private final List<StaffPlayer> staffPlayers = new ArrayList<>(32);
    private final Set<String> addedPlayers = new HashSet<>(64);

    private String serverAddressCache = "";
    private boolean isLocalServerCache = false;

    private final Map<String, KeybindAnimation> keybindAnimations = new HashMap<>();
    private final Map<String, StaffAnimation> staffAnimations = new HashMap<>();

    private float potionListHeightDynamic = 0;

    private float cooldownListHeightDynamic = 0;

    private int activeStaff = 0;
    private float hDynam = 0;
    private float widthDynamic = 0;
    private float nameWidth = 0;

    private float keybindsHeightDynamic = 0;

    // DynamicIsland
    private final Animation internetAnimation = new EaseInOutQuad(300, 1.0);
    private final Animation mediaAnimation = new EaseInOutQuad(300, 1.0);
    private final Animation pvpAnimation = new EaseInOutQuad(300, 1.0);
    private float animatedWidth = 0f;
    private long lastUpdateTime = 0;
    private final ExecutorService mediaExecutor = Executors.newSingleThreadExecutor();
    private volatile MediaInfo currentMediaInfo = null;
    private volatile IMediaSession currentSession = null;
    private volatile byte[] pendingArtwork = null;
    private volatile boolean needsTextureUpdate = false;
    private Identifier artworkTexture = null;
    private NativeImageBackedTexture artworkBackedTexture = null;
    private boolean artworkRegistered = false;
    private int pvpTimer = 30;
    private final Pattern numberPattern = Pattern.compile("\\d+");

    @Override
    public void onEvent(Event event) {
        if (mc == null || mc.player == null || mc.world == null) return;

        if (event instanceof EventUpdate) {
            if (setting.get("StaffList")) {
                updateStaffPlayers(mc);
            }
            if (setting.get("DynamicIsland")) {
                updateDynamicIsland();
            }
        }
        if (event instanceof EventRender2D eventRender2D) {
            boolean sWaterMark = setting.get("WaterMark");
            boolean sTargetHUD = setting.get("TargetHUD");
            boolean sStaffList = setting.get("StaffList");
            boolean sKeyBinds = setting.get("KeyBinds");
            boolean sItemCooldown = setting.get("ItemCoolDownHUD");
            boolean sPotion = setting.get("PotionHUD");
            boolean sCoordinates = setting.get("Coordinates / TPS");
            boolean sArmorHUD = setting.get("ArmorHUD");
            boolean sMusicRender = setting.get("MusicRender");
            boolean sScoreboard = setting.get("Scoreboard");

            if (sWaterMark) waterMark(eventRender2D);
            if (sTargetHUD) targethud(eventRender2D);
            if (sStaffList) staffList(eventRender2D);
            if (sKeyBinds) keybindHud(eventRender2D);
            if (sItemCooldown) cooldown(eventRender2D);
            if (sPotion) potion(eventRender2D);
            if (sCoordinates) coordinates(eventRender2D);
            if (sArmorHUD) armor(eventRender2D);
            if (setting.get("DynamicIsland")) dynamicIsland(eventRender2D);
            if (sMusicRender && mediaPlayerRenderer != null) {
                mediaPlayerRenderer.tick();
                
                // Check if music should be merged with scoreboard
                boolean shouldHideMusic = false;
                if (sScoreboard) {
                    float scoreboardX = scoreboardDrag.getX();
                    float scoreboardY = scoreboardDrag.getY();
                    float musicX = mediaPlayerRenderer.mediaDrag.getX();
                    float musicY = mediaPlayerRenderer.mediaDrag.getY();
                    
                    float distanceY = scoreboardY - (musicY + mediaPlayerRenderer.mediaDrag.getHeight());
                    float distanceX = Math.abs(scoreboardX - musicX);
                    
                    if (distanceY >= 0 && distanceY <= 50 && distanceX <= 20 && 
                        mediaPlayerRenderer.trackName != null && !mediaPlayerRenderer.trackName.isEmpty()) {
                        shouldHideMusic = true; // Hide music render when merged
                    }
                }
                
                if (!shouldHideMusic) {
                    mediaPlayerRenderer.render(eventRender2D);
                }
            }
            if (sScoreboard) renderScoreboard(eventRender2D);
        }
        if (event instanceof EventMouse mouseEvent) {
            if (setting.get("MusicRender") && mediaPlayerRenderer != null && mc.mouse != null) {
                double scale = mc.getWindow().getScaleFactor();
                double mouseX = mc.mouse.getX() / scale;
                double mouseY = mc.mouse.getY() / scale;
                mediaPlayerRenderer.handleClick(mouseX, mouseY, mouseEvent.getButton());
            }
        }
    }
    private void drawBlurOrLiquidGlass(MatrixStack matrices, float x, float y, float width, float height, Vector4f rounding, float blurRadius, int color) {
        if (liquidglass.get()) {
            // Параметры точно как в оригинальном примере: smoothness 1f, glassDirection 9.0f, glassQuality 25.0f, glassSize 1.2f
            RenderUtil.drawLiquidGlass(matrices, x, y, width, height, rounding, 1.0f, 9.0f, 25.0f, 1.2f, color);
        } else if (blur.get()) {
            drawBlur(matrices, x, y, width, height, rounding, blurRadius, color);
        }
    }

    private int getBackgroundColor() {
        // Когда liquidglass включен, используем лёгкий серый фон наподобие примера
        if (liquidglass.get()) {
            return ColorUtil.rgba(59, 59, 59, 90);
        } else {
            return ColorUtil.rgba(20, 20, 20, (int)(255 * 0.72)); // Обычный темный фон
        }
    }

    private void armor(EventRender2D eventRender2D) {
        float x = armorDrag.getX();
        float y = armorDrag.getY();
        int armorCount = 0;
        for (int i = 0; i < 4; i++) {
            if (!mc.player.getInventory().armor.get(i).isEmpty()) armorCount++;
        }

        int width = armorCount > 0 ? 20 * armorCount : 35;
        armorDrag.setWidth(width);
        armorDrag.setHeight(18);

        float startX = x + width - 20;
        for (int i = 0; i < 4; i++) {
            ItemStack itemStack = mc.player.getInventory().armor.get(i);
            if (!itemStack.isEmpty()) {
                eventRender2D.getDrawContext().getMatrices().push();
                eventRender2D.getDrawContext().getMatrices().translate(startX, y + 0.2f, 0);
                eventRender2D.getDrawContext().getMatrices().scale(1, 1, 1);
                eventRender2D.getDrawContext().drawItem(itemStack, 0, 0, 0);
                eventRender2D.getDrawContext().drawStackOverlay(mc.textRenderer, itemStack, 0, 0);
                eventRender2D.getDrawContext().getMatrices().pop();
                startX -= 20;
            }
        }
    }


    private void updateStaffPlayers(MinecraftClient mc) {
        staffPlayers.clear();
        addedPlayers.clear();

        Map<String, PlayerListEntry> nameToEntry = new HashMap<>(mc.player.networkHandler.getPlayerList().size() + 4);
        for (PlayerListEntry e : mc.player.networkHandler.getPlayerList()) {
            if (e.getProfile() != null && e.getProfile().getName() != null) {
                nameToEntry.put(e.getProfile().getName().toLowerCase(Locale.ROOT), e);
            }
        }

        String ourName = mc.player.getName().getString();
        Scoreboard scoreboard = mc.world.getScoreboard();

        for (Team team : scoreboard.getTeams()) {
            Text prefixComponent = team.getPrefix();
            String prefix = prefixComponent.getString();
            String cleanPrefixLower = repairString(prefix).toLowerCase(Locale.ROOT);

            for (String member : team.getPlayerList()) {
                if (member == null || member.equals(ourName) || addedPlayers.contains(member)) continue;
                if (!NAME_PATTERN.matcher(member).matches()) continue;

                PlayerListEntry entry = nameToEntry.get(member.toLowerCase(Locale.ROOT));
                boolean isVanished = (entry == null);

                boolean matchesStaff = PREFIX_MATCHES.matcher(cleanPrefixLower).matches() || Manager.STAFF_MANAGER.isStaff(member);

                if (!isVanished) {
                    if (matchesStaff) {
                        java.util.UUID uuid = entry.getProfile().getId();
                        staffPlayers.add(new StaffPlayer(member, prefixComponent, uuid));
                        addedPlayers.add(member);
                    }
                } else {
                    if (!prefix.isEmpty() && matchesStaff) {
                        staffPlayers.add(new StaffPlayer(member, prefixComponent, null));
                        addedPlayers.add(member);
                    }
                }
            }
        }

        if (!staffPlayers.isEmpty()) {
            staffPlayers.sort(Comparator.comparing(StaffPlayer::getName));
        }
    }

    private final Map<Integer, Integer> effectDurations = new HashMap<>();

    private void potion(EventRender2D eventRender2D) {
        float posX = potionhudDrag.getX();
        float posY = potionhudDrag.getY();

        var matrices = eventRender2D.getDrawContext().getMatrices();
        var font = FontUtils.durman[12];
        var iconFont = FontUtils.velyasik[11];

        int lineHeight = 11;
        int headerHeight = 19;
        int padding = 7;
        float maxWidth = 96;

        List<StatusEffectInstance> activeEffects = new ArrayList<>(mc.player.getStatusEffects());

        // Анимация скейла
        float scaleAnimation = activeEffects.isEmpty() ? 0f : 1f;

        // Обновляем анимации для каждого эффекта
        Set<String> currentEffects = new HashSet<>();
        for (StatusEffectInstance eff : activeEffects) {
            String effectKey = I18n.translate(eff.getEffectType().value().getTranslationKey());
            currentEffects.add(effectKey);
            PotionAnimation anim = potionAnimations.computeIfAbsent(effectKey, k -> new PotionAnimation());
            anim.update(true, lineHeight);
        }

        // Обновляем анимации исчезновения
        potionAnimations.entrySet().removeIf(entry -> {
            if (!currentEffects.contains(entry.getKey())) {
                entry.getValue().update(false, lineHeight);
                return entry.getValue().height < 0.1f && entry.getValue().alpha < 1f;
            }
            return false;
        });

        // Считаем общую высоту
        float contentHeight = 0;
        for (Map.Entry<String, PotionAnimation> entry : potionAnimations.entrySet()) {
            if (entry.getValue().shouldRender()) {
                contentHeight += entry.getValue().height;
            }
        }

        float totalHeight = headerHeight + (contentHeight > 0 ? contentHeight + 6 : 0);
        potionListHeightDynamic = MathUtil.fast(potionListHeightDynamic, totalHeight, 15);

        // Общая анимация скейла
        matrices.push();
        matrices.translate(posX + maxWidth / 2f, posY + potionListHeightDynamic / 2f, 0);
        matrices.scale(scaleAnimation, scaleAnimation, scaleAnimation);
        matrices.translate(-(posX + maxWidth / 2f), -(posY + potionListHeightDynamic / 2f), 0);

        // Blur или LiquidGlass для всего списка
        if ((blur.get() || liquidglass.get()) && potionListHeightDynamic > 1) {
            drawBlurOrLiquidGlass(matrices, posX, posY, maxWidth, potionListHeightDynamic,
                    new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());
        }

        // Фон (светлый для liquidglass, темный для обычного)
        int bgColor = getBackgroundColor();
        drawRoundedRect(matrices, posX, posY, maxWidth, potionListHeightDynamic,
                new Vector4f(6, 6, 6, 6), bgColor);

        // Иконка и заголовок "Potions"
        iconFont.drawLeftAligned(matrices, "e", posX + 7.5f, posY + 7.5f, -1);
        font.drawLeftAligned(matrices, "Potions", posX + 16.5f, posY + 7f, -1);

        // Декоративная линия справа в заголовке
        drawRoundedRect(matrices, posX + maxWidth - 12, posY + 6, 6, 1,
                new Vector4f(0.25f, 0.25f, 0.25f, 0.25f),
                ColorUtil.rgba(255, 255, 255, (int)(255 * 0.21)));

        // Разделительная линия после заголовка
        if (!activeEffects.isEmpty() && contentHeight > 0) {
            drawRoundedRect(matrices, posX, posY + headerHeight, maxWidth, 1,
                    new Vector4f(0, 0, 0, 0),
                    ColorUtil.rgba(255, 255, 255, (int)(255 * 0.21)));
        }

        float yOffset = posY + headerHeight + 6;

        StatusEffectSpriteManager spriteManager = mc.getStatusEffectSpriteManager();

        // Рисуем все эффекты
        for (StatusEffectInstance eff : activeEffects) {
            StatusEffect effect = eff.getEffectType().value();
            String effectName = I18n.translate(effect.getTranslationKey());

            PotionAnimation anim = potionAnimations.get(effectName);
            if (anim == null || !anim.shouldRender()) continue;

            boolean isDisappearing = !currentEffects.contains(effectName);
            float animHeight = anim.height;
            int animAlpha = (int) Math.min(255, anim.alpha);

            if (animAlpha < 10) {
                yOffset += animHeight;
                continue;
            }

            // Скейлинг для плавного появления/исчезновения
            matrices.push();
            float scale = Math.min(1f, animHeight / lineHeight);
            matrices.translate(posX + maxWidth / 2f, yOffset + animHeight / 2f, 0);
            matrices.scale(1f, scale, 1f);
            matrices.translate(-(posX + maxWidth / 2f), -(yOffset + animHeight / 2f), 0);

            // Цвета
            int textColor = ColorUtil.rgba(255, 255, 255, animAlpha);
            int amplifierColor = ColorUtil.rgba(255, 255, 255, (int)(animAlpha * 0.42));
            int durationColor = ColorUtil.rgba(255, 255, 255, (int)(animAlpha * 0.72));

            // Уровень эффекта
            int level = eff.getAmplifier() + 1;
            boolean hideAmplifier = effect == StatusEffects.NIGHT_VISION && eff.getAmplifier() >= 255;
            String amplifierText = hideAmplifier ? "" : (level > 1 ? I18n.translate("enchantment.level." + level) : "");

            // Длительность
            String duration = formatDuration(eff);

            // Иконка эффекта (6x6)
            RegistryEntry<StatusEffect> holder = eff.getEffectType();
            Sprite texture = spriteManager.getSprite(holder);
            eventRender2D.getDrawContext().drawSpriteStretched(
                    RenderLayer::getGuiTextured,
                    texture,
                    (int)(posX + 7),
                    (int)(yOffset),
                    6, 6, -1
            );

            // Вертикальная линия-разделитель
            drawRoundedRect(matrices, posX + 16, yOffset + 2.5f, 2, 1,
                    new Vector4f(0.25f, 0.25f, 0.25f, 0.25f),
                    ColorUtil.rgba(255, 255, 255, (int)(animAlpha * 0.21)));

            // Название эффекта
            font.drawLeftAligned(matrices, effectName, posX + 21, yOffset, textColor);

            // Уровень (справа от названия)
            if (!amplifierText.isEmpty()) {
                float nameWidth = font.getWidth(effectName);
                font.drawLeftAligned(matrices, amplifierText,
                        posX + 21 + nameWidth + 1, yOffset, amplifierColor);
            }

            // Длительность справа
            if (!isDisappearing) {
                float durationWidth = font.getWidth(duration);
                font.drawLeftAligned(matrices, duration,
                        posX + maxWidth - durationWidth - 7, yOffset, durationColor);
            }

            matrices.pop();
            yOffset += animHeight;
        }

        matrices.pop();

        potionhudDrag.setWidth(maxWidth);
        potionhudDrag.setHeight(potionListHeightDynamic);
    }

    private String formatDuration(StatusEffectInstance eff) {
        if (eff.isInfinite() || eff.getDuration() > 18000) {
            return "**:**";
        }
        String raw = StatusEffectUtil.getDurationText(eff, 1.0F, 20.0f).getString();
        return raw.replace("{", "").replace("}", "");
    }

    // Класс для анимации эффектов
    private static class PotionAnimation {
        float height = 0f;
        float alpha = 0f;

        void update(boolean visible, float targetHeight) {
            height = MathUtil.fast(height, visible ? targetHeight : 0f, 10);
            alpha = MathUtil.fast(alpha, visible ? 255f : 0f, 10);
        }

        boolean shouldRender() {
            return height > 0.1f || alpha > 1f;
        }
    }

    private final Map<String, PotionAnimation> potionAnimations = new HashMap<>();

    private void cooldown(EventRender2D eventRender2D) {
        float posX = itemcooldownDrag.getX();
        float posY = itemcooldownDrag.getY();
        int headerHeight = 22;
        int padding = 8;
        int lineHeight = 13;
        List<Item> activeItems = new ArrayList<>();
        float maxWidth = 120f;

        ItemCooldownManager manager = mc.player.getItemCooldownManager();
        ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor) manager;

        for (Item item : TRACKED_ITEMS) {
            ItemStack stack = new ItemStack(item);
            if (manager.isCoolingDown(stack)) {
                activeItems.add(item);

                String itemName = ITEM_NAMES.getOrDefault(item, stack.getName().getString());
                Identifier id = manager.getGroup(stack);
                Object rawEntry = accessor.getEntries().get(id);

                float remainingSeconds = 0f;
                if (rawEntry instanceof ItemCooldownEntryAccessor entry) {
                    int end = entry.getEndTick();
                    int current = accessor.getTick();
                    float remainingTicks = end - (current + mc.getRenderTickCounter().getTickDelta(true));
                    remainingSeconds = Math.max(0f, remainingTicks / 20.0f);
                }

                String timeLeft = formatCooldownTime(remainingSeconds);
                float nameWidth = FontUtils.durman[13].getWidth(itemName);
                float timeWidth = FontUtils.durman[13].getWidth(timeLeft);
                float totalWidth = padding * 2 + 26 + nameWidth + padding + timeWidth;
                if (totalWidth > maxWidth) maxWidth = totalWidth;
            }
        }

        float listHeightTarget = activeItems.size() * lineHeight;
        cooldownListHeightDynamic = MathUtil.fast(cooldownListHeightDynamic, listHeightTarget, 12);
        float contentHeight = cooldownListHeightDynamic <= 0.5f ? 0f : cooldownListHeightDynamic + padding + 2;
        float totalHeight = headerHeight + (contentHeight > 0 ? contentHeight : 10f);

        var matrices = eventRender2D.getDrawContext().getMatrices();
        matrices.push();

        if ((blur.get() || liquidglass.get()) && totalHeight > 1f) {
            drawBlurOrLiquidGlass(matrices, posX, posY, maxWidth, totalHeight,
                    new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());
            }

        drawRoundedRect(matrices, posX, posY, maxWidth, totalHeight,
                new Vector4f(6, 6, 6, 6), getBackgroundColor());

        // Header
        RenderUtil.drawTexture(matrices, "images/hud/cooldown.png",
                posX + 8, posY + 5, 11, 11, 0, Color.white.getRGB());
        FontUtils.durman[14].drawLeftAligned(matrices, "Cooldowns", posX + 24, posY + 6, -1);

        int accent = ColorUtil.rgba(255, 255, 255, (int) (255 * 0.21f));
        drawRoundedRect(matrices, posX + maxWidth - 14, posY + 8, 8, 1,
                new Vector4f(0.25f, 0.25f, 0.25f, 0.25f), accent);

        if (contentHeight > 0) {
            drawRoundedRect(matrices, posX, posY + headerHeight, maxWidth, 1,
                    new Vector4f(0, 0, 0, 0), accent);
        }

        // List background overlay for liquid glass vibe
        if (contentHeight > 0) {
            int listBg = ColorUtil.rgba(255, 255, 255, liquidglass.get() ? 35 : 25);
            drawRoundedRect(matrices, posX + 4, posY + headerHeight + 4,
                    maxWidth - 8, cooldownListHeightDynamic + padding,
                    new Vector4f(4, 4, 6, 6), listBg);
        }

        Scissor.push();
        Scissor.setFromComponentCoordinates(posX, posY, maxWidth, totalHeight);

        float yOffset = posY + headerHeight + 6;
        for (Item item : activeItems) {
            ItemStack stack = item.getDefaultStack();
            String itemName = ITEM_NAMES.getOrDefault(item, stack.getName().getString());

            Identifier id = manager.getGroup(stack);
            Object rawEntry = accessor.getEntries().get(id);

            float remainingSeconds = 0f;
            if (rawEntry instanceof ItemCooldownEntryAccessor entry) {
                int end = entry.getEndTick();
                int current = accessor.getTick();
                float remainingTicks = end - (current + mc.getRenderTickCounter().getTickDelta(true));
                remainingSeconds = Math.max(0f, remainingTicks / 20.0f);
            }

            String timeLeft = formatCooldownTime(remainingSeconds);

            RenderAddon.renderItem(eventRender2D.getDrawContext(), stack,
                    posX + padding - 2, yOffset - 3, 0.6f, false);

            drawRoundedRect(matrices, posX + padding + 10, yOffset - 1,
                    maxWidth - padding * 2 - 12, 11,
                    new Vector4f(3, 3, 3, 3),
                    ColorUtil.rgba(0, 0, 0, liquidglass.get() ? 60 : 90));

            FontUtils.durman[13].drawLeftAligned(matrices, itemName,
                    posX + padding + 14, yOffset + 1, ColorUtil.rgba(255, 255, 255, 230));

            float timeWidth = FontUtils.durman[13].getWidth(timeLeft);
            drawRoundedRect(matrices, posX + maxWidth - timeWidth - padding - 8,
                    yOffset - 1, timeWidth + 10, 11,
                    new Vector4f(3, 3, 3, 3),
                    ColorUtil.applyAlpha(hud_color, 0.75f));

            FontUtils.durman[13].drawLeftAligned(matrices, timeLeft,
                    posX + maxWidth - timeWidth - padding - 4, yOffset + 1, -1);

            yOffset += lineHeight;
        }

        Scissor.unset();
        Scissor.pop();
        matrices.pop();

        itemcooldownDrag.setWidth(maxWidth);
        itemcooldownDrag.setHeight(totalHeight);
    }



    private void staffList(EventRender2D render2D) {
        float posX = stafflistDrag.getX();
        float posY = stafflistDrag.getY();

        var matrices = render2D.getDrawContext().getMatrices();
        var font = FontUtils.durman[12];
        var iconFont = FontUtils.velyasik[11];

        int lineHeight = 11;
        int headerHeight = 19;
        int padding = 7;
        float maxWidth = 96;

        // Обновляем статусы всех стаффов
        for (StaffPlayer staff : staffPlayers) {
            staff.updateStatus();
        }

        // Обновляем анимации для каждого стаффа
        Set<String> currentStaff = new HashSet<>();
        for (StaffPlayer staff : staffPlayers) {
            currentStaff.add(staff.getName());
            StaffAnimation anim = staffAnimations.computeIfAbsent(staff.getName(), k -> new StaffAnimation());
            anim.lastStatus = staff.getStatus().getString();
            anim.update(true, lineHeight);
        }

        // Обновляем анимации исчезновения
        staffAnimations.entrySet().removeIf(entry -> {
            if (!currentStaff.contains(entry.getKey())) {
                entry.getValue().update(false, lineHeight);
                return entry.getValue().height < 0.1f && entry.getValue().alpha < 1f;
            }
            return false;
        });

        // Собираем все элементы включая исчезающие
        List<StaffPlayer> allStaff = new ArrayList<>();
        // Добавляем текущих стаффов
        for (StaffPlayer staff : staffPlayers) {
            allStaff.add(staff);
        }
        // Добавляем исчезающих стаффов
        for (Map.Entry<String, StaffAnimation> entry : staffAnimations.entrySet()) {
            if (!currentStaff.contains(entry.getKey()) && entry.getValue().shouldRender()) {
                String name = entry.getKey();
                // Ищем в списке стаффов
                StaffPlayer foundStaff = null;
                for (StaffPlayer staff : staffPlayers) {
                    if (staff.getName().equals(name)) {
                        foundStaff = staff;
                        break;
                    }
                }
                if (foundStaff != null && !allStaff.contains(foundStaff)) {
                    allStaff.add(foundStaff);
                } else if (foundStaff == null) {
                    // Если не найден, создаем временный
                    allStaff.add(new StaffPlayer(name, Text.empty(), null));
                }
            }
        }

        // Считаем общую высоту
        float contentHeight = 0;
        for (Map.Entry<String, StaffAnimation> entry : staffAnimations.entrySet()) {
            if (entry.getValue().shouldRender()) {
                contentHeight += entry.getValue().height;
            }
        }

        // Анимация скейла
        float scaleAnimation = contentHeight > 0 ? 1f : 0f;

        float totalHeight = headerHeight + (contentHeight > 0 ? contentHeight + 6 : 0);
        hDynam = MathUtil.fast(hDynam, totalHeight, 15);

        // Общая анимация скейла
        matrices.push();
        matrices.translate(posX + maxWidth / 2f, posY + hDynam / 2f, 0);
        matrices.scale(scaleAnimation, scaleAnimation, scaleAnimation);
        matrices.translate(-(posX + maxWidth / 2f), -(posY + hDynam / 2f), 0);

        // Blur или LiquidGlass для всего списка
        if ((blur.get() || liquidglass.get()) && hDynam > 1) {
            drawBlurOrLiquidGlass(matrices, posX, posY, maxWidth, hDynam,
                    new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());
        }

        // Фон (светлый для liquidglass, темный для обычного)
        int bgColor = getBackgroundColor();
        drawRoundedRect(matrices, posX, posY, maxWidth, hDynam,
                new Vector4f(6, 6, 6, 6), bgColor);

        // Иконка и заголовок "Staffs"
        iconFont.drawLeftAligned(matrices, "d", posX + 7.5f, posY + 7.5f, -1);
        font.drawLeftAligned(matrices, "Staffs", posX + 16.5f, posY + 7f, -1);

        // Декоративная линия справа в заголовке
        drawRoundedRect(matrices, posX + maxWidth - 12, posY + 6, 6, 1,
                new Vector4f(0.25f, 0.25f, 0.25f, 0.25f),
                ColorUtil.rgba(255, 255, 255, (int)(255 * 0.21)));

        // Разделительная линия после заголовка
        if (!allStaff.isEmpty() && contentHeight > 0) {
            drawRoundedRect(matrices, posX, posY + headerHeight, maxWidth, 1,
                    new Vector4f(0, 0, 0, 0),
                    ColorUtil.rgba(255, 255, 255, (int)(255 * 0.21)));
        }

        float yOffset = posY + headerHeight + 6;

        // Получаем информацию о игроках
        Map<String, PlayerListEntry> playerInfoMap = new HashMap<>();
        for (PlayerListEntry info : mc.getNetworkHandler().getPlayerList()) {
            playerInfoMap.put(info.getProfile().getName(), info);
        }

        // Рисуем всех стаффов
        for (StaffPlayer staff : allStaff) {
            StaffAnimation anim = staffAnimations.get(staff.getName());
            if (anim == null || !anim.shouldRender()) continue;

            boolean isDisappearing = !currentStaff.contains(staff.getName());
            float animHeight = anim.height;
            int animAlpha = (int) Math.min(255, anim.alpha);

            if (animAlpha < 10) {
                yOffset += animHeight;
                continue;
            }

            // Скейлинг для плавного появления/исчезновения
            matrices.push();
            float scale = Math.min(1f, animHeight / lineHeight);
            matrices.translate(posX + maxWidth / 2f, yOffset + animHeight / 2f, 0);
            matrices.scale(1f, scale, 1f);
            matrices.translate(-(posX + maxWidth / 2f), -(yOffset + animHeight / 2f), 0);

            // Цвета
            int textColor = ColorUtil.rgba(255, 255, 255, animAlpha);
            int statusColor = getStatusColor(staff.getStatus(), animAlpha);

            String staffName = staff.getName();
            String status = staff.getStatus().getString();

            // Размеры для центрирования
            float avatarSize = 6f;
            float textHeight = font.getHeight();
            float centerY = yOffset;
            float textCenterY = centerY + (avatarSize - textHeight) / 2f;

            // Голова игрока (6x6 как в SkyCore)
            PlayerListEntry playerInfo = playerInfoMap.get(staffName);
            if (playerInfo != null) {
                if (!(staff.getStatus() == StaffPlayer.Status.VANISHED ||
                        staff.getStatus() == StaffPlayer.Status.SPEC)) {
                    RenderAddon.drawStaffHead(matrices, playerInfo.getSkinTextures().texture(),
                            posX + 7, centerY, avatarSize, 3);
                } else {
                    // Для ванишнутых - иконка ваниша
                    RenderUtil.drawTexture(matrices, "images/hud/staffvanish.png",
                            posX + 7, centerY, avatarSize, avatarSize, 3,
                            ColorUtil.rgba(255, 255, 255, animAlpha));
                }
            } else {
                RenderUtil.drawTexture(matrices, "images/hud/staffvanish.png",
                        posX + 7, centerY, avatarSize, avatarSize, 3,
                        ColorUtil.rgba(255, 255, 255, animAlpha));
            }

            // Точечка-разделитель между головой и именем
            drawRoundedRect(matrices, posX + 16, yOffset + 4.5f, 1, 1,
                    new Vector4f(0.5f, 0.5f, 0.5f, 0.5f),
                    ColorUtil.rgba(255, 255, 255, (int)(animAlpha * 0.21)));

            // Имя стаффа (выровнено по центру аватарки)
            font.drawLeftAligned(matrices, staffName, posX + 21, textCenterY, textColor);

            // Статус справа (полупрозрачный, тоже выровнен)
            if (!isDisappearing) {
                float statusWidth = font.getWidth(status);
                font.drawLeftAligned(matrices, status,
                        posX + maxWidth - statusWidth - 7, textCenterY, statusColor);
            }

            matrices.pop();
            yOffset += animHeight;
        }

        matrices.pop();

        stafflistDrag.setWidth(maxWidth);
        stafflistDrag.setHeight(hDynam);
    }

    // Цвет статуса с учетом альфы
    private int getStatusColor(StaffPlayer.Status status, int alpha) {
        return switch (status) {
            case VANISHED -> ColorUtil.rgba(255, 85, 85, (int)(alpha * 0.72));    // Красный
            case SPEC -> ColorUtil.rgba(255, 170, 0, (int)(alpha * 0.72));        // Оранжевый
            case NONE -> ColorUtil.rgba(85, 255, 85, (int)(alpha * 0.72));       // Зеленый (играет)
            case NEAR -> ColorUtil.rgba(255, 170, 0, (int)(alpha * 0.72));       // Оранжевый (рядом)
            default -> ColorUtil.rgba(255, 255, 255, (int)(alpha * 0.72));
        };
    }

    // Класс анимации стаффа
    private static class StaffAnimation {
        float height = 0;
        float alpha = 0;
        String lastStatus = "";

        void update(boolean active, int targetHeight) {
            if (active) {
                height = MathUtil.fast(height, targetHeight, 10);
                alpha = MathUtil.fast(alpha, 255, 10);
            } else {
                height = MathUtil.fast(height, 0, 10);
                alpha = MathUtil.fast(alpha, 0, 10);
            }
        }

        boolean shouldRender() {
            return height > 0.1f || alpha > 1f;
        }
    }

    private float lastHealth = 0.0f;
    private float lastAbsorption = 0.0f;

    private void updatePlayerHealth(PlayerEntity player) {
        if (player == null || mc.world == null) return;
        String myPlayerName = String.valueOf(mc.player.getName());
        if (!player.getName().getString().equals(myPlayerName)) {
            Scoreboard scoreboard = mc.world.getScoreboard();
            ScoreboardObjective scoreObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
            if (scoreObjective != null) {
                try {
                    int hp = scoreboard.getOrCreateScore(ScoreHolder.fromName(player.getNameForScoreboard()), scoreObjective).getScore();
                    if (hp > 0) {
                        player.setHealth(Math.max(hp, 1));
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private float getPlayerHealth(AbstractClientPlayerEntity player) {
        if (player instanceof PlayerEntity) {
            String myPlayerName = String.valueOf(mc.player.getName());
            if (!player.getName().getString().equals(myPlayerName)) {
                Scoreboard scoreboard = mc.world.getScoreboard();
                ScoreboardObjective scoreObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
                if (scoreObjective != null) {
                    try {
                        int scoreboardHealth = scoreboard.getOrCreateScore(ScoreHolder.fromName(player.getNameForScoreboard()), scoreObjective).getScore();
                        if (scoreboardHealth > 0) {
                            return Math.max(scoreboardHealth, 1.0F);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return player.getHealth();
    }

    private void targethud(EventRender2D render2D) {
        var matrices = render2D.getDrawContext().getMatrices();
        float x = targethudDrag.getX();
        float y = targethudDrag.getY();

        targethudDrag.setWidth(105.0F);
        targethudDrag.setHeight(35.0F);

        // Use old getTarget() logic to find target
        target = getTarget(target);

        double animationOutput = tHudAnimation.getOutput();

        // Don't render if animation is 0 or no target
        if (animationOutput <= 0.0 || target == null) {
            return;
        }

        // Convert to AbstractClientPlayerEntity for player-specific features
        targetPlayer = null;
        if (target instanceof AbstractClientPlayerEntity) {
            targetPlayer = (AbstractClientPlayerEntity) target;
        } else if (target instanceof PlayerEntity playerTarget && mc.world != null) {
            // Try to get AbstractClientPlayerEntity from world by UUID
            Entity entity = mc.world.getPlayerByUuid(playerTarget.getUuid());
            if (entity instanceof AbstractClientPlayerEntity) {
                targetPlayer = (AbstractClientPlayerEntity) entity;
            }
        }

        // Update health if target is a player
        if (target instanceof PlayerEntity) {
            updatePlayerHealth((PlayerEntity) target);
        }

        matrices.push();
        matrices.translate(x + targethudDrag.getWidth() / 2.0F, y + targethudDrag.getHeight() / 2.0F, 0.0F);
        matrices.scale((float) animationOutput, (float) animationOutput, (float) animationOutput);
        matrices.translate(-(x + targethudDrag.getWidth() / 2.0F), -(y + targethudDrag.getHeight() / 2.0F), 0.0F);

        // Blur или LiquidGlass background (consistent with other HUDs: always check setting, use white tint, Vector4f rounding)
        if (blur.get() || liquidglass.get()) {
            drawBlurOrLiquidGlass(matrices, x, y, targethudDrag.getWidth(), targethudDrag.getHeight(),
                    new Vector4f(6, 6, 6, 6), 24.0F, Color.white.getRGB());
        }

        // Background (use Vector4f for consistency) - светлый для liquidglass
        int bgColor = getBackgroundColor();
        RenderUtil.drawRoundedRect(matrices, x, y, targethudDrag.getWidth(), targethudDrag.getHeight(),
                new Vector4f(6, 6, 6, 6), bgColor);

        // Draw head (works with any Entity)
        RenderAddon.drawHead(matrices, target, x + 4.0F, y + 4.0F, 27.0F, 3.0F);

        // Vertical separator
        RenderUtil.drawRoundedRect(matrices, x + 35.0F, y, 0.75F, targethudDrag.getHeight(), 0.0F, ColorUtil.rgba(255, 255, 255, (int)(255 * 0.21)));

        // Get health
        float health = targetPlayer != null ? getPlayerHealth(targetPlayer) : target.getHealth();
        String healthString = String.format(Locale.ENGLISH, "%.1f", health) + "hp";
        float healthStringWidth = FontUtils.durman[10].getWidth(healthString);
        String nameString = Manager.FUNCTION_MANAGER.nameProtect.getProtectedName(target.getName().getString());

        // Health bar background
        float normalizedHealthPercent = MathHelper.clamp(target.getHealth() / target.getMaxHealth(), 0.0F, 1.0F);
        RenderUtil.drawRoundedRect(matrices, x + 42.5F, y + targethudDrag.getHeight() - 7.0F - 2.0F, 55.5F, 2.0F, 0.75F, ColorUtil.rgba(255, 255, 255, (int)(255 * 0.21)));

        // Health bar fill
        RenderUtil.drawRoundedRect(matrices, x + 42.5F, y + targethudDrag.getHeight() - 7.0F - 2.0F, 55.5F * normalizedHealthPercent, 2.0F, 0.75F, ColorUtil.rgba(255, 255, 255, (int)(255 * 0.72)));

        // Icon (heart)
        FontUtils.velyasik[11].drawLeftAligned(matrices, "b", x + 43.69F, y + 10.0F, ColorUtil.rgba(255, 255, 255, (int)(255 * 0.72)));

        // Name with scissor
        Scissor.push();
        Scissor.setFromComponentCoordinates(x, y, targethudDrag.getWidth() - healthStringWidth - 7.0F, targethudDrag.getHeight());
        FontUtils.durman[12].drawLeftAligned(matrices, nameString, x + 50.5F, y + 9.5F, -1);
        Scissor.pop();

        // Health text
        FontUtils.durman[10].drawLeftAligned(matrices, healthString, x + targethudDrag.getWidth() - 7.0F - FontUtils.durman[10].getWidth(healthString), y + 10.0F, ColorUtil.rgba(255, 255, 255, (int)(255 * 0.48)));

        // Armor items (only for players) - restored original scaling logic for compatibility
        if (targetPlayer != null) {
            float offset = 0.0F;
            for (ItemStack stack : targetPlayer.getArmorItems()) {
                if (!stack.isEmpty()) {
                    matrices.push();
                    matrices.translate(x + offset + 42.5F, y + 16.0F, 0.0F);
                    matrices.scale(0.4375F, 0.4375F, 0.4375F);
                    matrices.translate(-(x + offset + 42.5F), -(y + 16.0F), 0.0F);
                    render2D.getDrawContext().drawItem(stack, (int)(x + offset + 42.5F), (int)(y + 16.0F), 0);
                    render2D.getDrawContext().drawStackOverlay(mc.textRenderer, stack, (int)(x + offset + 42.5F), (int)(y + 16.0F));
                    matrices.pop();
                } else {
                    FontUtils.velyasik[7].drawLeftAligned(matrices, "k", x + offset + 44.0F, y + 16.0F + 4.5F, -1);
                }
                offset += 8.0F;
            }
        }

        matrices.pop();
    }

    private void waterMark(EventRender2D render2D) {
        float x = watermarkDrag.getX();
        float y = watermarkDrag.getY();

        String clientName = "shadedlc";
        String userName = "uidinfinity";

        var matrices = render2D.getDrawContext().getMatrices();
        var font = FontUtils.durman[12];
        var smallFont = FontUtils.durman[10];
        var iconFont = FontUtils.velyasik[13];
        var smallIconFont = FontUtils.velyasik[11];

        float clientNameWidth = font.getWidth(clientName);
        float userNameWidth = font.getWidth(userName);

        // Дополнительное пространство так, чтобы имя не обрезалось
        float totalWidth = clientNameWidth + 21f + userNameWidth + 20f;
        float height = 21f;

        // Blur или LiquidGlass для всего watermark
        if (blur.get() || liquidglass.get()) {
            drawBlurOrLiquidGlass(matrices, x, y, totalWidth, height,
                    new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());
        }

        // Фон (светлый для liquidglass, темный для обычного)
        int bgColor = getBackgroundColor();
        drawRoundedRect(matrices, x, y, totalWidth, height,
                new Vector4f(6, 6, 6, 6), bgColor);

        // Название клиента (слева)
        float clientNameY = y - 0.5f + (height - font.getHeight()) / 2f;
        font.drawLeftAligned(matrices, clientName, x + 7f, clientNameY, -1);

        // Вертикальный разделитель — чуть ближе
        drawRoundedRect(matrices, x + clientNameWidth + 15f, y, 1, height,
                new Vector4f(0, 0, 0, 0),
                ColorUtil.rgba(255, 255, 255, (int)(255 * 0.21)));

        // Иконка пользователя — сдвинута левее
        float userIconY = y + (height - smallIconFont.getHeight()) / 2f;
        smallIconFont.drawLeftAligned(matrices, "b", x + clientNameWidth + 21f, userIconY,
                ColorUtil.rgba(255, 255, 255, (int)(255 * 0.72)));

        // Имя пользователя — тоже левее
        float userNameY = y - 0.5f + (height - font.getHeight()) / 2f;
        font.drawLeftAligned(matrices, userName, x + clientNameWidth + 31f, userNameY, -1);

        // Обновление размеров области перетаскивания
        watermarkDrag.setWidth(totalWidth);
        watermarkDrag.setHeight(height);
    }



    private void coordinates(EventRender2D render2D) {
        var matrices = render2D.getDrawContext().getMatrices();
        float x = coordinateshudDrag.getX();
        float y = coordinateshudDrag.getY();

        var font = FontUtils.durman[12];
        var icons = FontUtils.velyasik[11];

        // Format coordinates
        String coordsString = String.format(Locale.ENGLISH, "x %.0f y %.0f z %.0f",
            mc.player.getX(), mc.player.getY(), mc.player.getZ());

        // Format TPS
        String tpsString = String.format(Locale.ENGLISH, "%.0ftps", ClientManager.getTPS());

        // Format FPS
        String fpsString = ClientManager.getFps() + "fps";

        // Format ping
        String pingString = "0ms";
        if (mc.player != null && mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) {
                pingString = entry.getLatency() + "ms";
            }
        }

        // Format BPS (blocks per second)
        double dx = mc.player.getX() - mc.player.prevX;
        double dz = mc.player.getZ() - mc.player.prevZ;
        String bpsString = String.format(Locale.ENGLISH, "%.2fbps", Math.hypot(dx, dz) * 20.0);

        // Calculate widths
        float coordsStringWidth = font.getWidth(coordsString);
        float tpsStringWidth = font.getWidth(tpsString);
        float fpsStringWidth = font.getWidth(fpsString);
        float pingStringWidth = font.getWidth(pingString);
        float bpsStringWidth = font.getWidth(bpsString);

        // Set size
        float totalWidth = 83.0F + coordsStringWidth + tpsStringWidth + fpsStringWidth + pingStringWidth + bpsStringWidth + 7.0F;
        float height = 21.0F;
        coordinateshudDrag.setWidth(totalWidth);
        coordinateshudDrag.setHeight(height);

        // Blur или LiquidGlass background (always, like in SkyCore)
        if (blur.get() || liquidglass.get()) {
            drawBlurOrLiquidGlass(matrices, x, y, totalWidth, height,
                    new Vector4f(6, 6, 6, 6), 24.0F, Color.white.getRGB());
        }

        // Background (светлый для liquidglass, темный для обычного)
        int bgColor = getBackgroundColor();
        RenderUtil.drawRoundedRect(matrices, x, y, totalWidth, height, 6.0F, bgColor);

        float localX = x + 7.5F;

        // FPS icon and text
        icons.drawLeftAligned(matrices, "f", localX, y + 0.5F + (height - icons.getHeight()) / 2.0F, ColorUtil.rgba(255, 255, 255, (int)(255 * 0.72)));
        localX += 7.5F;
        font.drawLeftAligned(matrices, fpsString, localX, y + (height - font.getHeight()) / 2.0F, -1);
        localX += fpsStringWidth + 9.5F;

        // Separator dot
        RenderUtil.drawRoundedRect(matrices, localX - 5.5F, y + 0.5F + 9.5F, 2.0F, 2.0F, 0.25F, ColorUtil.rgba(255, 255, 255, (int)(255 * 0.24)));

        // Ping icon and text
        icons.drawLeftAligned(matrices, "g", localX, y + 0.5F + (height - icons.getHeight()) / 2.0F, ColorUtil.rgba(255, 255, 255, (int)(255 * 0.72)));
        localX += 7.5F;
        font.drawLeftAligned(matrices, pingString, localX, y + (height - font.getHeight()) / 2.0F, -1);
        localX += pingStringWidth + 9.5F;

        // Separator dot
        RenderUtil.drawRoundedRect(matrices, localX - 5.5F, y + 0.5F + 9.5F, 2.0F, 2.0F, 0.25F, ColorUtil.rgba(255, 255, 255, (int)(255 * 0.24)));

        // TPS icon and text
        icons.drawLeftAligned(matrices, "h", localX, y + 0.5F + (height - icons.getHeight()) / 2.0F, ColorUtil.rgba(255, 255, 255, (int)(255 * 0.72)));
        localX += 7.5F;
        font.drawLeftAligned(matrices, tpsString, localX, y + (height - font.getHeight()) / 2.0F, -1);
        localX += tpsStringWidth + 9.5F;

        // Separator dot
        RenderUtil.drawRoundedRect(matrices, localX - 5.5F, y + 0.5F + 9.5F, 2.0F, 2.0F, 0.25F, ColorUtil.rgba(255, 255, 255, (int)(255 * 0.24)));

        // BPS icon and text
        icons.drawLeftAligned(matrices, "i", localX, y + 0.5F + (height - icons.getHeight()) / 2.0F, ColorUtil.rgba(255, 255, 255, (int)(255 * 0.72)));
        localX += 7.5F;
        font.drawLeftAligned(matrices, bpsString, localX, y + (height - font.getHeight()) / 2.0F, -1);
        localX += bpsStringWidth + 9.5F;

        // Separator dot
        RenderUtil.drawRoundedRect(matrices, localX - 5.5F, y + 0.5F + 9.5F, 2.0F, 2.0F, 0.25F, ColorUtil.rgba(255, 255, 255, (int)(255 * 0.24)));

        // Coords icon and text
        icons.drawLeftAligned(matrices, "j", localX, y + 0.5F + (height - icons.getHeight()) / 2.0F, ColorUtil.rgba(255, 255, 255, (int)(255 * 0.72)));
        localX += 7.5F;
        font.drawLeftAligned(matrices, coordsString, localX, y + (height - font.getHeight()) / 2.0F, -1);
    }

    public class MediaPlayerRenderer {
        private final Dragging mediaDrag;
        // Media state
        private String trackName = null;
        private String artistName = null;
        private float progress = 0.0f;
        private long currentTime = 0;
        private long totalTime = 1;
        private boolean isPlaying = false;
        // Session management
        private IMediaSession activeSession = null;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final AtomicBoolean polling = new AtomicBoolean(false);
        private volatile long lastPollMs = 0L;
        // Cover artwork
        public final Identifier coverTextureLocation = Identifier.of("velyasik", "music_cover");
        public NativeImageBackedTexture coverTexture = null;
        private int coverHash = 0;
        // Animations
        private final Animation scaleAnimation = new EaseInOutQuad(300, 1.0);
        private final Animation playPauseAnimation = new EaseInOutQuad(200, 1.0);
        private final Animation hoverPrevAnimation = new EaseInOutQuad(150, 1.0);
        private final Animation hoverPlayAnimation = new EaseInOutQuad(150, 1.0);
        private final Animation hoverNextAnimation = new EaseInOutQuad(150, 1.0);
        // UI measurements for click detection
        private float prevButtonX = 0f, prevButtonW = 0f;
        private float playButtonX = 0f, playButtonW = 0f;
        private float nextButtonX = 0f, nextButtonW = 0f;
        private float controlsY = 0f, controlsH = 0f;
        // Hover states
        private boolean hoveringPrev = false;
        private boolean hoveringPlay = false;
        private boolean hoveringNext = false;
        public MediaPlayerRenderer(String dragName, float x, float y) {
            this.mediaDrag = Velyasik.getInstance().createDrag(HUD.this, dragName, x, y);
        }
        public void tick() {
            long now = System.currentTimeMillis();
            if (now - lastPollMs < 200L) return;
            lastPollMs = now;
            if (!polling.compareAndSet(false, true)) return;
            executor.execute(() -> {
                try {
                    IMediaSession session = MediaPlayerInfo.Instance.getMediaSessions().stream()
                            .max(Comparator.comparing(s -> s.getMedia().getPlaying()))
                            .orElse(null);
                    if (session != null) {
                        MediaInfo info = session.getMedia();
                        if (info != null && !info.getTitle().isEmpty()) {
                            updateMediaInfo(session, info);
                        } else {
                            clearMediaData();
                        }
                    } else {
                        clearMediaData();
                    }
                } catch (Throwable e) {
                    clearMediaData();
                } finally {
                    polling.set(false);
                }
            });
        }
        private void updateMediaInfo(IMediaSession session, MediaInfo info) {
            String newTrack = info.getTitle().toLowerCase();
            String newArtist = (info.getArtist() != null && !info.getArtist().isEmpty())
                    ? info.getArtist().toLowerCase() : "unknown artist";
            long newCurrent = Math.max(0L, info.getPosition());
            long newTotal = info.getDuration() > 0 ? info.getDuration() : 1;
            boolean newPlaying = info.getPlaying();
            // Handle cover artwork
            byte[] newCover = info.getArtworkPng();
            int newCoverHash = 0;
            NativeImage decodedImage = null;
            if (newCover != null && newCover.length > 0) {
                try {
                    newCoverHash = Arrays.hashCode(newCover);
                    decodedImage = NativeImage.read(new ByteArrayInputStream(newCover));
                } catch (Exception ignored) {
                    decodedImage = null;
                    newCoverHash = 0;
                }
            }
            NativeImage finalImage = decodedImage;
            int finalHash = newCoverHash;
            mc.execute(() -> {
                activeSession = session;
                trackName = newTrack;
                artistName = newArtist;
                currentTime = newCurrent;
                totalTime = newTotal;
                progress = (float) newCurrent / (float) newTotal;
                isPlaying = newPlaying;
                if (newCover == null || newCover.length == 0) {
                    clearCoverTexture();
                    coverHash = 0;
                } else if (finalImage != null) {
                    if (finalHash != coverHash) {
                        updateCoverTexture(finalImage);
                        coverHash = finalHash;
                    } else {
                        try { finalImage.close(); } catch (Exception ignored) {}
                    }
                } else {
                    clearCoverTexture();
                    coverHash = 0;
                }
                scaleAnimation.setDirection(trackName != null);
                playPauseAnimation.setDirection(isPlaying);
            });
        }
        private void clearMediaData() {
            mc.execute(() -> {
                trackName = null;
                artistName = null;
                progress = 0.0f;
                currentTime = 0;
                totalTime = 1;
                isPlaying = false;
                clearCoverTexture();
                scaleAnimation.setDirection(false);
            });
        }
        private void clearCoverTexture() {
            try {
                TextureManager tm = mc.getTextureManager();
                tm.destroyTexture(coverTextureLocation);
                if (coverTexture != null) {
                    coverTexture.close();
                    coverTexture = null;
                }
            } catch (Exception ignored) {
                coverTexture = null;
            }
            coverHash = 0;
        }
        private void updateCoverTexture(NativeImage image) {
            try {
                if (image != null) {
                    TextureManager tm = mc.getTextureManager();
                    tm.destroyTexture(coverTextureLocation);
                    if (coverTexture != null) {
                        coverTexture.close();
                        coverTexture = null;
                    }
                    coverTexture = new NativeImageBackedTexture(image);
                    tm.registerTexture(coverTextureLocation, coverTexture);
                }
            } catch (Exception e) {
                clearCoverTexture();
                try { image.close(); } catch (Exception ignored) {}
            }
        }
        public void render(EventRender2D event) {
            if (trackName == null || trackName.isEmpty()) return;
            var matrices = event.getDrawContext().getMatrices();
            var textFont = FontUtils.durman[12];
            var iconFont = FontUtils.musicRender[12];
            var musicFont = FontUtils.musicRender[16];
            float x = mediaDrag.getX();
            float y = mediaDrag.getY();
            float coverSize = 16f;
            float height = 30f;
            float radius = 6.0f;
            double scale = scaleAnimation.getOutput();
            if (scale <= 0.01) return;
            String timeText = formatTime(currentTime);
            float timeWidth = textFont.getWidth(timeText);
            float musicIconW = musicFont.getWidth("f");
            float timeIconW = iconFont.getWidth("I");
            float prevIconW = iconFont.getWidth("a");
            float playIconW = iconFont.getWidth(isPlaying ? "c" : "d");
            float nextIconW = iconFont.getWidth("b");
            // Spacing constants
            float music_padding = 4f;
            float between_music_time = 0f;
            float between_time_text = 0f;
            float after_time_sep = 4f;
            float to_controls = 2f;
            float icon_spacing = 1f;
            float after_text_gap = 8f;
            float left_to_text = 34f;
            float right_padding = 2f;

            // Calculate text first with max width
            float max_text_width = 140f;
            float min_track_width = 20f;
            float max_artist_width = 60f;

            String displayArtist = artistName != null ? ellipsize(artistName, textFont, max_artist_width) : "";
            float artistWidth = displayArtist.isEmpty() ? 0f : textFont.getWidth(displayArtist);
            float artist_space = artistWidth > 0 ? 3f : 0f;

            float track_max_width = max_text_width - artistWidth - artist_space;
            String displayTrack = ellipsize(trackName, textFont, Math.max(min_track_width, track_max_width));
            float trackWidth = textFont.getWidth(displayTrack);

            // Calculate actual text width
            float actual_text_width = trackWidth + artist_space + artistWidth;

            // Calculate controls width
            float controls_width = prevIconW + icon_spacing + playIconW + icon_spacing + nextIconW;

            // Calculate total width dynamically
            float totalWidth = left_to_text + actual_text_width + after_text_gap + music_padding +
                    musicIconW + between_music_time + timeIconW + between_time_text +
                    timeWidth + after_time_sep + to_controls + controls_width + right_padding;

            float textX = x + left_to_text;

            // Calculate fixed controls positions from the right (BEFORE scale transformation)
            // These coordinates are in screen space and will be used for click detection
            float controls_endX = x + totalWidth - right_padding;
            float nextButtonX_temp = controls_endX - nextIconW;
            float playButtonX_temp = nextButtonX_temp - icon_spacing - playIconW;
            float prevButtonX_temp = playButtonX_temp - icon_spacing - prevIconW;
            float fixed_controlsStartX = prevButtonX_temp;

            // Save button coordinates BEFORE scale (for click detection)
            prevButtonX = fixed_controlsStartX;
            prevButtonW = prevIconW;
            playButtonX = playButtonX_temp;
            playButtonW = playIconW;
            nextButtonX = nextButtonX_temp;
            nextButtonW = nextIconW;
            controlsY = y;
            controlsH = height;

            matrices.push();
            matrices.translate(x + totalWidth / 2f, y + height / 2f, 0);
            matrices.scale((float)scale, (float)scale, (float)scale);
            matrices.translate(-(x + totalWidth / 2f), -(y + height / 2f), 0);
            // Blur или LiquidGlass как в других HUD элементах
            if (blur.get() || liquidglass.get()) {
                drawBlurOrLiquidGlass(matrices, x, y, totalWidth, height,
                        new Vector4f(radius, radius, radius, radius), 24, Color.white.getRGB());
            }
            // Фон как в других HUD элементах (светлый для liquidglass)
            int bgColor = getBackgroundColor();
            RenderUtil.drawRoundedRect(matrices, x, y, totalWidth, height,
                    new Vector4f(radius, radius, radius, radius), bgColor);
            float coverX = x + 6;
            float coverY = y + 7;
            if (coverTexture != null) {
                var tex = mc.getTextureManager().getTexture(coverTextureLocation);
                int glId = tex != null ? tex.getGlId() : 0;
                if (glId != 0) {
                    RenderUtil.drawTexture(matrices, coverTextureLocation,
                            coverX, coverY, coverSize, coverSize, radius / 2f,
                            Color.white.getRGB());
                } else {
                    RenderUtil.drawRoundedRect(matrices, coverX, coverY, coverSize, coverSize,
                            radius / 2f, ColorUtil.rgba(50, 50, 50, 140));
                }
            } else {
                RenderUtil.drawRoundedRect(matrices, coverX, coverY, coverSize, coverSize,
                        radius / 2f, ColorUtil.rgba(50, 50, 50, 140));
            }
            float sep1X = x + 6 + coverSize + 6;
            RenderUtil.drawRoundedRect(matrices, sep1X, y, 1, height - 3,
                    0.0f, ColorUtil.rgba(255, 255, 255, (int)(255 * 0.21)));
            float contentHeight = height - 3;
            float textHeight = textFont.getHeight();
            float textY = y + (contentHeight - textHeight) / 2f;
            textFont.drawLeftAligned(matrices, displayTrack, textX, textY, -1);
            float currentTextX = textX + trackWidth;
            if (!displayArtist.isEmpty()) {
                currentTextX += 3;
                textFont.drawLeftAligned(matrices, displayArtist, currentTextX, textY,
                        ColorUtil.rgba(255, 255, 255, (int)(255 * 0.72)));
                currentTextX += artistWidth;
            }
            float sep2X = currentTextX + after_text_gap;
            RenderUtil.drawRoundedRect(matrices, sep2X, y, 1, height - 3,
                    0.0f, ColorUtil.rgba(255, 255, 255, (int)(255 * 0.21)));
            float iconHeight = iconFont.getHeight();
            float iconY = y + (contentHeight - iconHeight) / 2f;
            float musicIconHeight = musicFont.getHeight();
            float musicIconY = y + (contentHeight - musicIconHeight) / 2f;
            // Иконка музыки (f) из musicRender
            float musicIconX = sep2X + music_padding;
            musicFont.drawLeftAligned(matrices, "f", musicIconX, musicIconY, -1);
            // Иконка времени (I)
            float timeIconX = musicIconX + musicIconW + between_music_time;
            iconFont.drawLeftAligned(matrices, "I", timeIconX, iconY,
                    ColorUtil.rgba(255, 255, 255, (int)(255 * 0.72)));
            // Текст времени
            float timeTextX = timeIconX + timeIconW + between_time_text;
            textFont.drawLeftAligned(matrices, timeText, timeTextX, textY, -1);
            float sep3X = timeTextX + timeWidth + after_time_sep;
            RenderUtil.drawRoundedRect(matrices, sep3X, y, 1, height - 3,
                    0.0f, ColorUtil.rgba(255, 255, 255, (int)(255 * 0.21)));
            // Button coordinates are already saved before scale transformation
            double prevHover = hoverPrevAnimation.getOutput();
            int prevColor = ColorUtil.interpolateColor(
                    ColorUtil.rgba(255, 255, 255, (int)(255 * 0.72)),
                    -1,
                    (float)prevHover);
            iconFont.drawLeftAligned(matrices, "a", prevButtonX, iconY, prevColor);
            double playHover = hoverPlayAnimation.getOutput();
            int playBaseColor = isPlaying ? -1 : ColorUtil.rgba(255, 255, 255, (int)(255 * 0.72));
            int playColor = ColorUtil.interpolateColor(
                    playBaseColor,
                    -1,
                    (float)playHover);
            String playIcon = isPlaying ? "c" : "d";
            iconFont.drawLeftAligned(matrices, playIcon, playButtonX, iconY, playColor);
            double nextHover = hoverNextAnimation.getOutput();
            int nextColor = ColorUtil.interpolateColor(
                    ColorUtil.rgba(255, 255, 255, (int)(255 * 0.72)),
                    -1,
                    (float)nextHover);
            iconFont.drawLeftAligned(matrices, "b", nextButtonX, iconY, nextColor);
            // Прогресс-бар как в остальных HUD элементах
            float progressY = y + height - 3;
            float progressHeight = 3f;
            float progressRadius = progressHeight * 0.5f;
            RenderUtil.drawRoundedRect(matrices, x, progressY, totalWidth, progressHeight,
                    new Vector4f(0, 0, progressRadius, progressRadius),
                    ColorUtil.withAlpha(ColorUtil.hud_color, 0.2f));
            float filledWidth = totalWidth * Math.min(1.0f, progress);
            if (filledWidth > 0) {
                RenderUtil.drawRoundedRect(matrices, x, progressY, filledWidth, progressHeight,
                        new Vector4f(0, 0, progressRadius, 0),
                        -1);
            }
            matrices.pop();
            mediaDrag.setWidth(totalWidth);
            mediaDrag.setHeight(height);
        }
        public void handleMouseMove(double mouseX, double mouseY) {
            if (trackName == null) return;
            float x = mediaDrag.getX();
            float y = mediaDrag.getY();
            float w = mediaDrag.getWidth();
            float h = mediaDrag.getHeight();
            boolean inBounds = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
            boolean inControlsY = mouseY >= controlsY && mouseY <= controlsY + controlsH;
            if (inBounds && inControlsY) {
                boolean prevHover = mouseX >= prevButtonX && mouseX <= prevButtonX + prevButtonW;
                boolean playHover = mouseX >= playButtonX && mouseX <= playButtonX + playButtonW;
                boolean nextHover = mouseX >= nextButtonX && mouseX <= nextButtonX + nextButtonW;
                if (prevHover != hoveringPrev) {
                    hoveringPrev = prevHover;
                    hoverPrevAnimation.setDirection(prevHover);
                }
                if (playHover != hoveringPlay) {
                    hoveringPlay = playHover;
                    hoverPlayAnimation.setDirection(playHover);
                }
                if (nextHover != hoveringNext) {
                    hoveringNext = nextHover;
                    hoverNextAnimation.setDirection(nextHover);
                }
            } else {
                if (hoveringPrev) {
                    hoveringPrev = false;
                    hoverPrevAnimation.setDirection(false);
                }
                if (hoveringPlay) {
                    hoveringPlay = false;
                    hoverPlayAnimation.setDirection(false);
                }
                if (hoveringNext) {
                    hoveringNext = false;
                    hoverNextAnimation.setDirection(false);
                }
            }
        }
        public void handleClick(double mouseX, double mouseY, int button) {
            // Only handle right mouse button clicks (button == 1) to avoid conflicts with draggables
            if (button != 1 || trackName == null || trackName.isEmpty()) return;

            float x = mediaDrag.getX();
            float y = mediaDrag.getY();
            float w = mediaDrag.getWidth();
            float h = mediaDrag.getHeight();

            // Check if click is within the media player bounds
            if (!(mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h)) return;

            // Check if click is in the controls area (buttons area) - use full height
            if (!(mouseY >= controlsY && mouseY <= controlsY + controlsH)) return;

            try {
                if (activeSession == null) return;

                // Add some padding to make buttons easier to click
                float clickPadding = 2f;

                // Check which button was clicked (buttons are positioned from the right)
                // Expand click area slightly for easier clicking
                if (mouseX >= prevButtonX - clickPadding && mouseX <= prevButtonX + prevButtonW + clickPadding) {
                    activeSession.previous();
                    return;
                }
                if (mouseX >= playButtonX - clickPadding && mouseX <= playButtonX + playButtonW + clickPadding) {
                    try {
                        activeSession.playPause();
                    } catch (Throwable t) {
                        if (isPlaying) {
                            activeSession.pause();
                        } else {
                            activeSession.play();
                        }
                    }
                    return;
                }
                if (mouseX >= nextButtonX - clickPadding && mouseX <= nextButtonX + nextButtonW + clickPadding) {
                    activeSession.next();
                    return;
                }
            } catch (Throwable ignored) {}
        }
        public String ellipsize(String text, Object font, float maxWidth) {
            if (text == null) return "";
            float textWidth;
            if (font instanceof RenderFonts) {
                textWidth = ((RenderFonts)font).getWidth(text);
            } else {
                textWidth = FontUtils.durman[12].getWidth(text);
            }
            if (textWidth <= maxWidth) return text;
            String ellipsis = "...";
            int end = text.length() - 1;
            while (end > 0) {
                String sub = text.substring(0, end) + ellipsis;
                float subWidth;
                if (font instanceof RenderFonts) {
                    subWidth = ((RenderFonts)font).getWidth(sub);
                } else {
                    subWidth = FontUtils.durman[12].getWidth(sub);
                }
                if (subWidth <= maxWidth) return sub;
                end--;
            }
            return ellipsis;
        }
        public String formatTime(long time) {
            if (time < 0) time = 0;
            long totalSeconds = (time >= 100000) ? (time / 1000) : time;
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            return String.format("%d:%02d", minutes, seconds);
        }
        public Dragging getDragging() {
            return mediaDrag;
        }
        private static net.minecraft.client.MinecraftClient mc =
                net.minecraft.client.MinecraftClient.getInstance();
    }


    private void keybindHud(EventRender2D render2D) {
        float posX = keybindsDrag.getX();
        float posY = keybindsDrag.getY();
        int padding = 7;
        int lineHeight = 11;
        int headerHeight = 19;
        var matrices = render2D.getDrawContext().getMatrices();
        var font = FontUtils.durman[12]; // Основной шрифт как было
        var iconFont = FontUtils.velyasik[11]; // Только иконки из velyasik
        float maxWidth = 96;

        // Список всех активных кейбиндов
        List<KeybindData> activeKeybinds = new ArrayList<>();

        int functionIndex = 0;
        for (Function f : Manager.FUNCTION_MANAGER.getFunctions()) {
            int baseOrder = functionIndex++ * 100;
            if (f.bind != 0 && f.state) {
                String bindKey = getShortKey(ClientManager.getKey(f.bind)).toUpperCase();
                String icon = getModuleIcon(f.getCategory());
                KeybindData data = new KeybindData(f.name, bindKey, icon, true);
                data.order = baseOrder;
                activeKeybinds.add(data);
            }

            int settingOffset = 1;
            for (Setting setting : f.getSettings()) {
                if (setting instanceof BindBooleanSetting bindSetting &&
                        bindSetting.isVisible() && bindSetting.getBindKey() != 0 && bindSetting.get()) {
                    String bindKey = getShortKey(ClientManager.getKey(bindSetting.getBindKey())).toUpperCase();
                    String icon = getModuleIcon(f.getCategory());
                    KeybindData data = new KeybindData(bindSetting.getName(), bindKey, icon, false);
                    data.order = baseOrder + settingOffset++;
                    activeKeybinds.add(data);
                }
            }
        }

        // Анимация скейла
        float scaleAnimation = activeKeybinds.isEmpty() ? 0f : 1f;

        // Обновляем анимации для каждого кейбинда
        Set<String> currentKeys = new HashSet<>();
        for (KeybindData data : activeKeybinds) {
            currentKeys.add(data.name);
            KeybindAnimation anim = keybindAnimations.computeIfAbsent(data.name, k -> new KeybindAnimation());
            anim.lastIcon = data.icon;
            anim.lastBindKey = data.bindKey;
            anim.update(true, lineHeight, data.order);
        }

        // Обновляем анимации исчезновения
        keybindAnimations.entrySet().removeIf(entry -> {
            if (!currentKeys.contains(entry.getKey())) {
                entry.getValue().update(false, lineHeight, entry.getValue().order);
                return entry.getValue().height < 0.1f && entry.getValue().alpha < 1f;
            }
            return false;
        });

        // Собираем все элементы включая исчезающие
        List<KeybindData> allKeybinds = new ArrayList<>(activeKeybinds);
        for (Map.Entry<String, KeybindAnimation> entry : keybindAnimations.entrySet()) {
            if (!currentKeys.contains(entry.getKey()) && entry.getValue().shouldRender()) {
                KeybindAnimation anim = entry.getValue();
                KeybindData ghost = new KeybindData(entry.getKey(), anim.lastBindKey, anim.lastIcon, false);
                ghost.order = anim.order;
                allKeybinds.add(ghost);
            }
        }
        allKeybinds.sort(Comparator.comparingInt(k -> k.order));

        // Считаем общую высоту
        float contentHeight = 0;
        for (Map.Entry<String, KeybindAnimation> entry : keybindAnimations.entrySet()) {
            if (entry.getValue().shouldRender()) {
                contentHeight += entry.getValue().height;
            }
        }

        float totalHeight = headerHeight + (contentHeight > 0 ? contentHeight + 6 : 0);
        keybindsHeightDynamic = MathUtil.fast(keybindsHeightDynamic, totalHeight, 15);

        // Общая анимация скейла
        matrices.push();
        matrices.translate(posX + maxWidth / 2f, posY + keybindsHeightDynamic / 2f, 0);
        matrices.scale(scaleAnimation, scaleAnimation, scaleAnimation);
        matrices.translate(-(posX + maxWidth / 2f), -(posY + keybindsHeightDynamic / 2f), 0);

        // Blur или LiquidGlass для всего списка
        if ((blur.get() || liquidglass.get()) && keybindsHeightDynamic > 1) {
            drawBlurOrLiquidGlass(matrices, posX, posY, maxWidth, keybindsHeightDynamic,
                    new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());
        }

        // Фон (светлый для liquidglass, темный для обычного)
        int bgColor = getBackgroundColor();
        drawRoundedRect(matrices, posX, posY, maxWidth, keybindsHeightDynamic,
                new Vector4f(6, 6, 6, 6), bgColor);

        // Иконка и заголовок "Keybinds"
        iconFont.drawLeftAligned(matrices, "f", posX + 7.5f, posY + 7.5f, -1);
        font.drawLeftAligned(matrices, "Keybinds", posX + 16.5f, posY + 7f, -1);

        // Декоративная линия справа в заголовке
        drawRoundedRect(matrices, posX + maxWidth - 12, posY + 6, 6, 1,
                new Vector4f(0.25f, 0.25f, 0.25f, 0.25f),
                ColorUtil.rgba(255, 255, 255, (int)(255 * 0.21)));

        // Разделительная линия после заголовка
        if (!allKeybinds.isEmpty() && contentHeight > 0) {
            drawRoundedRect(matrices, posX, posY + headerHeight, maxWidth, 1,
                    new Vector4f(0, 0, 0, 0),
                    ColorUtil.rgba(255, 255, 255, (int)(255 * 0.21)));
        }

        float yOffset = posY + headerHeight + 6;

        // Рисуем все кейбинды
        for (KeybindData data : allKeybinds) {
            KeybindAnimation anim = keybindAnimations.get(data.name);
            if (anim == null || !anim.shouldRender()) continue;

            boolean isDisappearing = !currentKeys.contains(data.name);
            float animHeight = anim.height;
            int animAlpha = (int) Math.min(255, anim.alpha);

            if (animAlpha < 10) {
                yOffset += animHeight;
                continue;
            }

            // Скейлинг для плавного появления/исчезновения
            matrices.push();
            float scale = Math.min(1f, animHeight / lineHeight);
            matrices.translate(posX + maxWidth / 2f, yOffset + animHeight / 2f, 0);
            matrices.scale(1f, scale, 1f);
            matrices.translate(-(posX + maxWidth / 2f), -(yOffset + animHeight / 2f), 0);

            // Цвета
            int textColor = ColorUtil.rgba(255, 255, 255, animAlpha);
            int bindTextColor = ColorUtil.rgba(255, 255, 255, (int)(animAlpha * 0.72));

            // Иконка категории (поднята выше)
            if (data.icon != null) {
                iconFont.drawLeftAligned(matrices, data.icon, posX + 7.5f, yOffset, textColor);
            }

            // Вертикальная линия-разделитель (поднята выше)
            drawRoundedRect(matrices, posX + 16, yOffset + 2.5f, 1, 1,
                    new Vector4f(0.5f, 0.5f, 0.5f, 0.5f),
                    ColorUtil.rgba(255, 255, 255, (int)(animAlpha * 0.21)));

            // Название модуля/функции (поднято выше)
            font.drawLeftAligned(matrices, data.name, posX + 21, yOffset, textColor);

            // Текст клавиши справа (поднят выше)
            if (!isDisappearing) {
                float bindWidth = font.getWidth(data.bindKey);
                font.drawLeftAligned(matrices, data.bindKey,
                        posX + maxWidth - bindWidth - 7, yOffset, bindTextColor);
            }

            matrices.pop();
            yOffset += animHeight;
        }

        matrices.pop();

        keybindsDrag.setWidth(maxWidth);
        keybindsDrag.setHeight(keybindsHeightDynamic);
    }

    // Получение иконки по категории модуля
    private String getModuleIcon(Type category) {
        if (category == null) return "c";

        return switch (category) {
            case Combat -> "l";      // l маленькая
            case Move -> "m";        // m маленькая
            case Render -> "j";      // j маленькая
            case Player -> "b";      // b маленькая
            case Misc -> "c";        // c маленькая
            default -> "c";
        };
    }

    // Класс анимации
    private static class KeybindAnimation {
        float height = 0;
        float alpha = 0;
        long lastSeen = System.currentTimeMillis();
        boolean isDisappearing = false;
        String lastIcon = "";
        String lastBindKey = "";
        int order = Integer.MAX_VALUE;

        void update(boolean active, int targetHeight, int newOrder) {
            if (active) {
                lastSeen = System.currentTimeMillis();
                isDisappearing = false;
                height = MathUtil.fast(height, targetHeight, 10);
                alpha = MathUtil.fast(alpha, 255, 10);
                order = newOrder;
            } else {
                isDisappearing = true;
                height = MathUtil.fast(height, 0, 10);
                alpha = MathUtil.fast(alpha, 0, 10);
            }
        }

        boolean shouldRender() {
            return height > 0.1f || alpha > 1f;
        }
    }

    // Класс данных кейбинда
    private static class KeybindData {
        String name;
        String bindKey;
        String icon;
        boolean isFunction;
        int order = Integer.MAX_VALUE;

        KeybindData(String name, String bindKey, String icon, boolean isFunction) {
            this.name = name;
            this.bindKey = bindKey;
            this.icon = icon;
            this.isFunction = isFunction;
        }
    }

    private String getShortKey(String key) {
        if (key == null) return "";
        String bindText = key.toUpperCase();
        return bindText.length() > 6 ? bindText.substring(0, 6) + "…" : bindText;
    }


    public LivingEntity getTarget(LivingEntity nullTarget) {
        LivingEntity target = nullTarget;

        if (Manager.FUNCTION_MANAGER.attackAura.target instanceof LivingEntity) {
            target = (LivingEntity) Manager.FUNCTION_MANAGER.attackAura.target;
            tHudAnimation.setDirection(Direction.AxisDirection.POSITIVE);
        }
        else if (visibleCrosshair.get() && mc.crosshairTarget instanceof EntityHitResult) {
            Entity aimed = ((EntityHitResult) mc.crosshairTarget).getEntity();
            if (aimed instanceof LivingEntity) {
                target = (LivingEntity) aimed;
                tHudAnimation.setDirection(Direction.AxisDirection.POSITIVE);
            } else {
                tHudAnimation.setDirection(Direction.AxisDirection.NEGATIVE);
            }
        }
        else if (mc.currentScreen instanceof ChatScreen) {
            target = mc.player;
            tHudAnimation.setDirection(Direction.AxisDirection.POSITIVE);
        }
        else {
            tHudAnimation.setDirection(Direction.AxisDirection.NEGATIVE);
        }

        return target;
    }

    private String repairString(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            if (c >= 65281 && c <= 65374) {
                sb.append((char) (c - 65248));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public void onDisable() {
        staffPlayers.clear();
        addedPlayers.clear();
        keybindAnimations.clear();
        staffAnimations.clear();
    }
    public class StaffPlayer {
        @Getter
        private final String name;
        @Getter
        private final Text prefix;
        @Getter
        private Status status;
        @Getter
        private final long joinTime;
        @Getter
        private GameMode gameMode;
        @Getter
        private boolean isOnPlayerList;
        @Getter
        private final java.util.UUID uuid;

        public StaffPlayer(String name, Text prefix, @Nullable java.util.UUID uuid) {
            this.name = name;
            this.prefix = prefix;
            this.uuid = uuid;
            this.joinTime = System.currentTimeMillis();
            updateStatus();
        }

        public void updateStatus() {
            if (mc == null || mc.world == null || mc.getNetworkHandler() == null) {
                this.status = Status.VANISHED;
                this.isOnPlayerList = false;
                this.gameMode = null;
                return;
            }

            PlayerListEntry entry = null;
            if (this.uuid != null) {
                for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
                    if (this.uuid.equals(e.getProfile().getId())) {
                        entry = e;
                        break;
                    }
                }
            } else {
                for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
                    if (e.getProfile() != null && e.getProfile().getName() != null && e.getProfile().getName().equalsIgnoreCase(this.name)) {
                        entry = e;
                        break;
                    }
                }
            }

            this.isOnPlayerList = (entry != null);
            this.gameMode = (entry != null) ? entry.getGameMode() : null;

            boolean entityLoaded = false;
            if (entry != null) {
                var loaded = mc.world.getPlayerByUuid(entry.getProfile().getId());
                entityLoaded = (loaded != null);
            }

            if (!this.isOnPlayerList) {
                this.status = Status.VANISHED;
            } else if (this.gameMode == GameMode.SPECTATOR) {
                this.status = Status.SPEC;
            } else if (entityLoaded) {
                this.status = Status.NEAR;
            } else {
                this.status = Status.NONE;
            }
        }

        public enum Status {
            NONE("§2[ON]"),
            NEAR("§6[N]"),
            SPEC("§e[GM3]"),
            VANISHED("§c[V]");

            @Getter
            final String string;

            Status(String string) {
                this.string = string;
            }
        }
    }

    private int getStatusColor(StaffPlayer.Status status) {
        switch(status) {
            case NEAR: return Color.ORANGE.getRGB();
            case SPEC: return Color.YELLOW.getRGB();
            case VANISHED: return Color.RED.getRGB();
            default: return Color.GREEN.getRGB();
        }
    }
    private String formatCooldownTime(float seconds) {
        int totalSeconds = (int) Math.floor(seconds);
        int minutes = totalSeconds / 60;
        int secs = totalSeconds % 60;

        if (minutes > 0) {
            if (secs > 0) {
                return String.format("%dм %02dс", minutes, secs);
            } else {
                return String.format("%dм", minutes);
            }
        } else {
            return String.format("%dс", secs);
        }
    }

    private void updateDynamicIsland() {
        if (mc.player == null) return;

        updatePvPTimer();

        if (mc.player.age % 5 == 0) {
            mediaExecutor.execute(() -> {
                try {
                    IMediaSession currentSession = MediaPlayerInfo.Instance.getMediaSessions()
                            .stream().max(Comparator.comparing(s -> s.getMedia().getPlaying())).orElse(null);

                    if (currentSession != null) {
                        MediaInfo info = currentSession.getMedia();
                        if (info != null && (!info.getTitle().isEmpty() || !info.getArtist().isEmpty())) {
                            if (currentMediaInfo == null ||
                                    !currentMediaInfo.getTitle().equals(info.getTitle()) ||
                                    !currentMediaInfo.getArtist().equals(info.getArtist())) {
                                byte[] png = info.getArtworkPng();
                                    if (png != null && png.length > 0) {
                                        pendingArtwork = png;
                                        needsTextureUpdate = true;
                                        artworkRegistered = false;
                                    }
                                    currentMediaInfo = info;
                                this.currentSession = currentSession;
                            }
                        }
                    } else {
                        this.currentSession = null;
                        currentMediaInfo = null;
                    }
                } catch (Exception ignored) {}
            });
        }
    }

    private void updatePvPTimer() {
        try {
            // Try bossbar first
            if (mc.inGameHud != null && mc.inGameHud.getBossBarHud() != null) {
                Map<UUID, ClientBossBar> bossBars = ((BossBarHudAccessor) mc.inGameHud.getBossBarHud()).getBossBars();
                for (ClientBossBar bossBar : bossBars.values()) {
                    Text name = bossBar.getName();
                    String text = name.getString().toLowerCase();
                    if (text.contains("pvp") || text.contains("пвп") || text.contains("режим")) {
                        Matcher matcher = numberPattern.matcher(name.getString());
                        if (matcher.find()) {
                            pvpTimer = Integer.parseInt(matcher.group());
                            return;
                        }
                        float percent = bossBar.getPercent();
                        if (percent > 0) {
                            pvpTimer = Math.max(1, (int)(30 * percent));
                            return;
                        }
                    }
                }
            }

            // Try scoreboard
            if (mc.player != null && mc.player.getScoreboard() != null) {
                Scoreboard scoreboard = mc.player.getScoreboard();
                ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
                if (objective != null) {
                    var scores = scoreboard.getScoreboardEntries(objective);
                    for (var entry : scores) {
                        String playerName = entry.owner();
                        Team team = scoreboard.getScoreHolderTeam(playerName);
                        String line = team != null ? team.getPrefix().getString() + playerName + team.getSuffix().getString() : playerName;
                        if (line.toLowerCase().contains("pvp") || line.toLowerCase().contains("пвп")) {
                            Matcher matcher = numberPattern.matcher(line);
                            if (matcher.find()) {
                                pvpTimer = Integer.parseInt(matcher.group());
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void dynamicIsland(EventRender2D render2D) {
        if (mc.player == null) return;

        var matrices = render2D.getDrawContext().getMatrices();
        var font = FontUtils.durman[7];
        var iconFont = FontUtils.icomoon[7];

        if (needsTextureUpdate && pendingArtwork != null && !artworkRegistered) {
            try {
                if (artworkTexture == null) {
                    artworkTexture = Identifier.of("velyasik", "dynamic_island_artwork");
                }
                NativeImage image = NativeImage.read(new ByteArrayInputStream(pendingArtwork));
                TextureManager tm = mc.getTextureManager();
                tm.destroyTexture(artworkTexture);
                if (artworkBackedTexture != null) {
                    artworkBackedTexture.close();
                    artworkBackedTexture = null;
                }
                artworkBackedTexture = new NativeImageBackedTexture(image);
                tm.registerTexture(artworkTexture, artworkBackedTexture);
                artworkRegistered = true;
                needsTextureUpdate = false;
            } catch (Exception e) {
            }
        }

        boolean isPvp = ClientManager.playerIsPVP();
        boolean mediaNull = currentSession == null || currentMediaInfo == null || 
                (currentMediaInfo.getTitle().isEmpty() && currentMediaInfo.getArtist().isEmpty());

        int ping = 0;
        try {
            PlayerListEntry entry = mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        } catch (Exception ignored) {}

        internetAnimation.setDirection(ping < 150);
        mediaAnimation.setDirection(!mediaNull && !isPvp);
        pvpAnimation.setDirection(isPvp);

        float padding = 2f;
        float round = 6f;
        String name = "shadedlc - return";
        String track = (currentMediaInfo == null ? name : 
                currentMediaInfo.getTitle() + (currentMediaInfo.getArtist().isEmpty() ? "" : " - " + currentMediaInfo.getArtist()));
        String pvpText = "Вы в PvP режиме";
        String pvpTimerText = String.valueOf(pvpTimer);
        String mainText = isPvp ? pvpText : (mediaNull ? name : track);

        float baseWidth = 15 + font.getWidth(mainText) + padding * (isPvp ? 3 : 2);
        float targetWidth = isPvp ? baseWidth + 2f : baseWidth;

        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - lastUpdateTime;
        lastUpdateTime = currentTime;

        animatedWidth = lerp(animatedWidth <= 0 ? targetWidth : animatedWidth, targetWidth, deltaTime);

        float height = 15f;
        float xCenter = mc.getWindow().getScaledWidth() / 2f;
        float x = xCenter - animatedWidth / 2f;
        float y = 4f;

        Scissor.push();
        Scissor.setFromComponentCoordinates(x, y, animatedWidth + 1f, height);

        drawRoundedRect(matrices, x, y, animatedWidth, height, round, new Color(0, 0, 0, 200).getRGB());

        float iconSize = height - padding * 2;

        if (!mediaNull && !isPvp && artworkRegistered && artworkTexture != null) {
            try {
                RenderUtil.drawTexture(matrices, artworkTexture, x + padding, y + padding,
                        iconSize, iconSize, 4f,
                        ColorUtil.rgba(255, 255, 255, (int)(255 * mediaAnimation.getOutput())));
            } catch (Exception e) {
                drawRoundedRect(matrices, x + padding, y + padding, iconSize, iconSize, 4f,
                        ColorUtil.withAlpha(hud_color, (float)mediaAnimation.getOutput()));
            }
        } else if (!isPvp) {
            drawRoundedRect(matrices, x + padding, y + padding, iconSize, iconSize, 4f,
                    ColorUtil.withAlpha(hud_color, (float)(1.0f - mediaAnimation.getOutput())));
        } else {
            float circleSize = height - padding * 2;
            drawRoundedRect(matrices, x + padding, y + padding, circleSize, circleSize,
                    circleSize / 2f,
                    ColorUtil.rgba(255, 0, 0, (int)(255 * pvpAnimation.getOutput())));

            var timerFont = FontUtils.durman[6];
            float timerX = x + padding + (circleSize - timerFont.getWidth(pvpTimerText)) / 2f;
            float timerY = y + padding + (circleSize - timerFont.getHeight()) / 2f;
            timerFont.drawLeftAligned(matrices, pvpTimerText, timerX, timerY,
                    ColorUtil.rgba(255, 255, 255, (int)(255 * pvpAnimation.getOutput())));
        }

        float baselineY = y + (height - font.getHeight()) / 2f - 0.5f;

        if (!mediaNull && !isPvp) {
            font.drawLeftAligned(matrices, track, x + height, baselineY,
                    ColorUtil.rgba(255, 255, 255, (int)(255 * mediaAnimation.getOutput())));
        } else if (!isPvp) {
            font.drawLeftAligned(matrices, name, x + height, baselineY,
                    ColorUtil.rgba(255, 255, 255, (int)(255 * (1.0f - mediaAnimation.getOutput()))));
        } else {
            font.drawLeftAligned(matrices, pvpText, x + height, baselineY,
                    ColorUtil.rgba(255, 255, 255, (int)(255 * pvpAnimation.getOutput())));
        }

        Scissor.pop();

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        float timeX = x - (padding * 3f) - font.getWidth(time);
        font.drawLeftAligned(matrices, time, timeX, baselineY, Color.BLACK.getRGB());

        float iconX = x + animatedWidth + (padding * 3f);
        float iconY = baselineY + (font.getHeight() - iconFont.getHeight()) / 2f;

        iconFont.drawLeftAligned(matrices, "Q", iconX, iconY,
                ColorUtil.rgba(0, 0, 0, (int)(255 * internetAnimation.getOutput())));

        iconFont.drawLeftAligned(matrices, "P", iconX, iconY,
                ColorUtil.rgba(0, 0, 0, (int)(255 * (1.0f - internetAnimation.getOutput()))));
    }

    private float lerp(float current, float target, long deltaMs) {
        float speed = deltaMs / 200f;
        float t = Math.min(1f, speed * 0.5f);
        return current + (target - current) * t;
    }

    private void renderScoreboard(EventRender2D eventRender2D) {
        if (mc.player == null || mc.world == null) return;

        Scoreboard scoreboard = mc.player.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        
        if (objective == null) return;

        var matrices = eventRender2D.getDrawContext().getMatrices();
        var context = eventRender2D.getDrawContext();
        var textRenderer = mc.textRenderer;
        
        List<ScoreboardEntry> entries = new ArrayList<>(scoreboard.getScoreboardEntries(objective));
        entries.removeIf(ScoreboardEntry::hidden);
        entries.sort(Comparator.comparingInt(ScoreboardEntry::value).reversed());
        
        if (entries.size() > 15) {
            entries = entries.subList(0, 15);
        }
        
        if (entries.isEmpty()) return;

        Text title = objective.getDisplayName();
        int headerHeight = 19;
        int padding = 7;
        int lineHeight = 9;
        var iconFont = FontUtils.velyasik[11];
        var textFont = FontUtils.durman[12];
        var musicFont = FontUtils.musicRender[16];
        
        // Check if MusicRender is close to Scoreboard (within 50 pixels vertically and horizontally aligned)
        boolean isMerged = false;
        String displayTrack = "";
        String displayArtist = "";
        String timeText = "";
        float progress = 0f;
        float musicHeaderHeight = 0;
        
        if (setting.get("MusicRender") && mediaPlayerRenderer != null && 
            mediaPlayerRenderer.trackName != null && !mediaPlayerRenderer.trackName.isEmpty()) {
            float scoreboardX = scoreboardDrag.getX();
            float scoreboardY = scoreboardDrag.getY();
            float musicX = mediaPlayerRenderer.mediaDrag.getX();
            float musicY = mediaPlayerRenderer.mediaDrag.getY();
            
            // Check if music is above scoreboard and close (within 50 pixels)
            float distanceY = scoreboardY - (musicY + mediaPlayerRenderer.mediaDrag.getHeight());
            float distanceX = Math.abs(scoreboardX - musicX);
            
            if (distanceY >= 0 && distanceY <= 50 && distanceX <= 20) {
                isMerged = true;
                musicHeaderHeight = 19;
                
                // Prepare music info
                String trackName = mediaPlayerRenderer.trackName;
                String artistName = mediaPlayerRenderer.artistName;
                timeText = mediaPlayerRenderer.formatTime(mediaPlayerRenderer.currentTime);
                progress = mediaPlayerRenderer.progress;
                
                float max_text_width = 140f;
                float max_artist_width = 60f;
                displayArtist = artistName != null ? mediaPlayerRenderer.ellipsize(artistName, textFont, max_artist_width) : "";
                float artistWidth = displayArtist.isEmpty() ? 0f : textFont.getWidth(displayArtist);
                float artist_space = artistWidth > 0 ? 3f : 0f;
                float track_max_width = max_text_width - artistWidth - artist_space;
                displayTrack = mediaPlayerRenderer.ellipsize(trackName, textFont, Math.max(20f, track_max_width));
            }
        }
        
        // Calculate max width
        int maxContentWidth = textRenderer.getWidth(title) + 20;
        if (isMerged) {
            float trackWidth = textFont.getWidth(displayTrack);
            float artistWidth = displayArtist.isEmpty() ? 0f : textFont.getWidth(displayArtist);
            float musicInfoWidth = trackWidth + (artistWidth > 0 ? 3 + artistWidth : 0) + 20;
            maxContentWidth = Math.max(maxContentWidth, (int)musicInfoWidth);
        }
        for (ScoreboardEntry entry : entries) {
            Team team = scoreboard.getScoreHolderTeam(entry.owner());
            Text name = Team.decorateName(team, Text.literal(entry.owner()));
            int nameWidth = textRenderer.getWidth(name);
            maxContentWidth = Math.max(maxContentWidth, nameWidth);
        }
        
        float maxWidth = maxContentWidth + padding * 2;
        
        // Calculate maximum available height (screen height minus some margin)
        float screenHeight = mc.getWindow().getScaledHeight();
        float maxAvailableHeight = screenHeight - 20; // Leave 20px margin from screen edges
        
        // Calculate base height
        int baseHeight = headerHeight + 6;
        if (isMerged) {
            baseHeight += musicHeaderHeight + 1; // Add music header + separator
        }
        
        // Calculate how many entries can fit
        int maxEntriesThatFit = (int) Math.floor((maxAvailableHeight - baseHeight) / lineHeight);
        maxEntriesThatFit = Math.max(0, maxEntriesThatFit); // Allow 0 entries (header only)
        
        // Limit entries to what can fit
        if (entries.size() > maxEntriesThatFit) {
            entries = entries.subList(0, maxEntriesThatFit);
        }
        
        int totalHeight = baseHeight + (entries.isEmpty() ? 0 : entries.size() * lineHeight);
        
        float posX = scoreboardDrag.getX();
        float posY = scoreboardDrag.getY();
        if (isMerged) {
            // Adjust position to include music header
            posY = posY - musicHeaderHeight - 1;
        }
        
        // Ensure scoreboard doesn't go off screen
        if (posY + totalHeight > screenHeight - 10) {
            posY = screenHeight - totalHeight - 10;
            scoreboardDrag.setY(posY);
        }
        if (posY < 10) {
            posY = 10;
            scoreboardDrag.setY(posY);
        }
        
        scoreboardDrag.setWidth(maxWidth);
        scoreboardDrag.setHeight(totalHeight);
        
        // Blur или LiquidGlass для всего списка (как в keybinds)
        if ((blur.get() || liquidglass.get()) && totalHeight > 1) {
            drawBlurOrLiquidGlass(matrices, posX, posY, maxWidth, totalHeight,
                    new Vector4f(6, 6, 6, 6), 24, Color.white.getRGB());
        }
        
        // Фон (светлый для liquidglass, темный для обычного) (как в keybinds)
        int bgColor = getBackgroundColor();
        drawRoundedRect(matrices, posX, posY, maxWidth, totalHeight,
                new Vector4f(6, 6, 6, 6), bgColor);
        
        float currentY = posY;
        
        // Music header (if merged)
        if (isMerged) {
            float musicHeaderY = currentY;
            float musicTextY = musicHeaderY + 7f;
            float trackWidth = textFont.getWidth(displayTrack);
            
            // Album artwork (small cover) instead of icon
            float coverSize = 12f; // Small cover size
            float coverX = posX + 7.5f;
            float coverY = musicHeaderY + 5.5f;
            
            if (mediaPlayerRenderer != null && mediaPlayerRenderer.coverTexture != null) {
                var tex = mc.getTextureManager().getTexture(mediaPlayerRenderer.coverTextureLocation);
                int glId = tex != null ? tex.getGlId() : 0;
                if (glId != 0) {
                    RenderUtil.drawTexture(matrices, mediaPlayerRenderer.coverTextureLocation,
                            coverX, coverY, coverSize, coverSize, 3f,
                            Color.white.getRGB());
                } else {
                    // Fallback to gray square if texture fails
                    drawRoundedRect(matrices, coverX, coverY, coverSize, coverSize, 3f,
                            ColorUtil.rgba(50, 50, 50, 140));
                }
            } else {
                // Fallback to gray square if no cover
                drawRoundedRect(matrices, coverX, coverY, coverSize, coverSize, 3f,
                        ColorUtil.rgba(50, 50, 50, 140));
            }
            
            // Track name (adjusted position after cover)
            textFont.drawLeftAligned(matrices, displayTrack, posX + 22f, musicTextY, -1);
            
            // Artist name (adjusted position after cover)
            if (!displayArtist.isEmpty()) {
                textFont.drawLeftAligned(matrices, displayArtist, posX + 22f + trackWidth + 3, musicTextY,
                        ColorUtil.rgba(255, 255, 255, (int)(255 * 0.72)));
            }
            
            // Time on the right
            float timeWidth = textFont.getWidth(timeText);
            textFont.drawLeftAligned(matrices, timeText, posX + maxWidth - timeWidth - padding, musicTextY,
                    ColorUtil.rgba(255, 255, 255, (int)(255 * 0.72)));
            
            // Separator line after music header
            drawRoundedRect(matrices, posX, musicHeaderY + musicHeaderHeight, maxWidth, 1,
                    new Vector4f(0, 0, 0, 0),
                    ColorUtil.rgba(255, 255, 255, (int)(255 * 0.21)));
            
            currentY += musicHeaderHeight + 1;
        }
        
        // Scoreboard header
        iconFont.drawLeftAligned(matrices, "P", posX + 7.5f, currentY + 7.5f, -1);
        context.drawText(textRenderer, title, (int)(posX + 16.5f), (int)(currentY + 7f), -1, false);
        
        // Декоративная линия справа в заголовке (как в keybinds)
        drawRoundedRect(matrices, posX + maxWidth - 12, currentY + 6, 6, 1,
                new Vector4f(0.25f, 0.25f, 0.25f, 0.25f),
                ColorUtil.rgba(255, 255, 255, (int)(255 * 0.21)));
        
        // Разделительная линия после заголовка (как в keybinds)
        if (!entries.isEmpty()) {
            drawRoundedRect(matrices, posX, currentY + headerHeight, maxWidth, 1,
                    new Vector4f(0, 0, 0, 0),
                    ColorUtil.rgba(255, 255, 255, (int)(255 * 0.21)));
        }
        
        // Scoreboard entries
        float entriesStartY = currentY + headerHeight + 6;
        float entriesAreaHeight = Math.max(0, totalHeight - (entriesStartY - posY) - 6); // Leave 6px padding at bottom
        
        Scissor.push();
        Scissor.setFromComponentCoordinates(posX, entriesStartY, maxWidth, entriesAreaHeight);
        
        float yOffset = entriesStartY;
        float maxY = posY + totalHeight - 6; // Leave 6px padding at bottom
        
        for (ScoreboardEntry entry : entries) {
            // Check if entry would go beyond bounds (with lineHeight check)
            if (yOffset >= maxY) {
                break; // Stop drawing if we would exceed bounds
            }
            
            Team team = scoreboard.getScoreHolderTeam(entry.owner());
            Text name = Team.decorateName(team, Text.literal(entry.owner()));
            
            // Draw text on its own line - each entry on separate line
            // Only draw if text would be visible (yOffset + text height < maxY)
            if (yOffset + textRenderer.fontHeight < maxY) {
                context.drawText(textRenderer, name, (int)(posX + padding), (int)yOffset, 0xFFFFFF, false);
            }
            
            // Move to next line (9 pixels like in original)
            yOffset += lineHeight;
            
            // Double check after increment
            if (yOffset > maxY) {
                break;
            }
        }
        
        Scissor.unset();
        Scissor.pop();
        
        // Progress bar as white outline around entire scoreboard (if merged) - draw on top
        if (isMerged && progress > 0) {
            float progressThickness = 2f;
            float scoreboardRadius = 6f;
            float filledProgress = Math.min(1.0f, Math.max(0f, progress));
            
            // Calculate perimeter for progress (going around the border clockwise)
            float topLength = maxWidth;
            float rightLength = totalHeight;
            float bottomLength = maxWidth;
            float leftLength = totalHeight;
            float perimeter = topLength + rightLength + bottomLength + leftLength;
            float filledLength = perimeter * filledProgress;
            
            // Background outline (full perimeter, semi-transparent white)
            RenderUtil.drawRoundedBorder(matrices, posX, posY, maxWidth, totalHeight, 
                    new Vector4f(scoreboardRadius, scoreboardRadius, scoreboardRadius, scoreboardRadius),
                    progressThickness, ColorUtil.rgba(255, 255, 255, 80));
            
            // Draw filled white outline using precise masking for each segment
            if (filledLength > 0.1f) {
                int progressColor = Color.WHITE.getRGB();
                float currentPos = 0f;
                
                // Top edge (left to right) - starts from top-left corner
                if (filledLength > currentPos) {
                    float topFilled = Math.min(topLength, filledLength - currentPos);
                    if (topFilled > 0.1f) {
                        // Mask area for top edge including corner
                        Scissor.push();
                        // Include extra space for corner rounding
                        float maskWidth = Math.min(topFilled + scoreboardRadius, maxWidth);
                        Scissor.setFromComponentCoordinates(posX, posY, maskWidth, progressThickness + scoreboardRadius);
                        // Draw full border - only top portion will be visible
                        RenderUtil.drawRoundedBorder(matrices, posX, posY, maxWidth, totalHeight, 
                                new Vector4f(scoreboardRadius, scoreboardRadius, scoreboardRadius, scoreboardRadius),
                                progressThickness, progressColor);
                        Scissor.pop();
                    }
                    currentPos += topLength;
                }
                
                // Right edge (top to bottom)
                if (filledLength > currentPos) {
                    float rightFilled = Math.min(rightLength, filledLength - currentPos);
                    if (rightFilled > 0.1f) {
                        // Mask area for right edge
                        Scissor.push();
                        float rightX = posX + maxWidth - progressThickness - scoreboardRadius;
                        Scissor.setFromComponentCoordinates(rightX, posY, progressThickness + scoreboardRadius * 2, rightFilled);
                        // Draw full border - only right portion will be visible
                        RenderUtil.drawRoundedBorder(matrices, posX, posY, maxWidth, totalHeight, 
                                new Vector4f(scoreboardRadius, scoreboardRadius, scoreboardRadius, scoreboardRadius),
                                progressThickness, progressColor);
                        Scissor.pop();
                    }
                    currentPos += rightLength;
                }
                
                // Bottom edge (right to left)
                if (filledLength > currentPos) {
                    float bottomFilled = Math.min(bottomLength, filledLength - currentPos);
                    if (bottomFilled > 0.1f) {
                        // Mask area for bottom edge including corner
                        Scissor.push();
                        float bottomX = posX + maxWidth - bottomFilled;
                        float bottomY = posY + totalHeight - progressThickness - scoreboardRadius;
                        // Include extra space for corner rounding
                        float maskWidth = Math.min(bottomFilled + scoreboardRadius, maxWidth);
                        Scissor.setFromComponentCoordinates(bottomX, bottomY, maskWidth, progressThickness + scoreboardRadius);
                        // Draw full border - only bottom portion will be visible
                        RenderUtil.drawRoundedBorder(matrices, posX, posY, maxWidth, totalHeight, 
                                new Vector4f(scoreboardRadius, scoreboardRadius, scoreboardRadius, scoreboardRadius),
                                progressThickness, progressColor);
                        Scissor.pop();
                    }
                    currentPos += bottomLength;
                }
                
                // Left edge (bottom to top)
                if (filledLength > currentPos) {
                    float leftFilled = Math.min(leftLength, filledLength - currentPos);
                    if (leftFilled > 0.1f) {
                        // Mask area for left edge
                        Scissor.push();
                        float leftX = posX;
                        float leftY = posY + totalHeight - leftFilled;
                        Scissor.setFromComponentCoordinates(leftX, leftY, progressThickness + scoreboardRadius, leftFilled);
                        // Draw full border - only left portion will be visible
                        RenderUtil.drawRoundedBorder(matrices, posX, posY, maxWidth, totalHeight, 
                                new Vector4f(scoreboardRadius, scoreboardRadius, scoreboardRadius, scoreboardRadius),
                                progressThickness, progressColor);
                        Scissor.pop();
                    }
                }
            }
        }
    }
}