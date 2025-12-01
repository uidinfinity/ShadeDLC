package dev.ynki.manager;

import dev.ynki.manager.IntelliManager.*;
import dev.ynki.manager.accountManager.AccountManager;
import dev.ynki.manager.commandManager.CommandManager;
import dev.ynki.manager.configManager.ConfigManager;
import dev.ynki.manager.dragManager.DragManager;
import dev.ynki.manager.friendManager.FriendManager;
import dev.ynki.manager.ircManager.IrcManager;
import dev.ynki.manager.macroManager.MacroManager;
import dev.ynki.manager.modulesManager.ChestStealerManager;
import dev.ynki.manager.notificationManager.NotificationManager;
import dev.ynki.manager.proxyManager.ProxyManager;
import dev.ynki.manager.staffManager.StaffManager;
import dev.ynki.manager.themeManager.StyleManager;
import dev.ynki.modules.FunctionManager;
import dev.ynki.modules.combat.rotation.RotationController;
import dev.ynki.protect.UserProfile;
import dev.ynki.manager.fontManager.FontUtils;

public class Manager {
    public static final RotationController ROTATION = RotationController.get();
    public static UserProfile USER_PROFILE;
    public static FunctionManager FUNCTION_MANAGER;
    public static StyleManager STYLE_MANAGER;
    public static NotificationManager NOTIFICATION_MANAGER;
    public static FriendManager FRIEND_MANAGER;
    public static ConfigManager CONFIG_MANAGER;
    public static MacroManager MACROS_MANAGER;
    public static IntelliManager INTELLI_MANAGER;
    public static StaffManager STAFF_MANAGER;
    public static CommandManager COMMAND_MANAGER;
    public static DragManager DRAG_MANAGER;
    public static SyncManager SYNC_MANAGER;
    public static FontUtils FONT_MANAGER;
    public static AccountManager ACCOUNT_MANAGER;
    public static ChestStealerManager CHESTSTEALER_MANAGER;
    public static IrcManager IRC_MANAGER;
    public static ProxyManager PROXY_MANAGER;
}