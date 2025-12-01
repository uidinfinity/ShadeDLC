package dev.ynki;

import dev.ynki.manager.ClientManager;
import dev.ynki.manager.IntelliManager.*;
import dev.ynki.manager.Manager;
import dev.ynki.manager.SyncManager;
import lombok.Getter;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import dev.ynki.manager.accountManager.AccountManager;
import dev.ynki.manager.ircManager.IrcManager;
import dev.ynki.manager.modulesManager.ChestStealerManager;
import dev.ynki.manager.proxyManager.ProxyManager;
import dev.ynki.manager.themeManager.StyleManager;
import dev.ynki.protect.NativeHelper;
import dev.ynki.modules.setting.BindBooleanSetting;
import dev.ynki.modules.setting.Setting;
import dev.ynki.events.Event;
import dev.ynki.events.impl.input.EventKey;
import dev.ynki.manager.commandManager.CommandManager;
import dev.ynki.manager.configManager.ConfigManager;
import dev.ynki.manager.dragManager.DragManager;
import dev.ynki.manager.dragManager.Dragging;
import dev.ynki.manager.friendManager.FriendManager;
import dev.ynki.manager.macroManager.MacroManager;
import dev.ynki.manager.notificationManager.NotificationManager;
import dev.ynki.manager.staffManager.StaffManager;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionManager;
import dev.ynki.modules.misc.UnHook;
import dev.ynki.screens.dropdown.ClickGUI;
import dev.ynki.manager.fontManager.FontUtils;
import dev.ynki.util.color.ColorUtil;
import dev.ynki.util.render.providers.ResourceProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.Objects;

@SuppressWarnings("All")
public final class Velyasik implements ModInitializer {
	private static Velyasik instance;
	private final File directory;
	private final File directoryAddon;
	public final String name = "shadedlc - return"; /// почему чит настолько паста :crying:
	@Getter
	boolean initialized;

	public static Velyasik getInstance() {
		return instance;
	}

	public Velyasik() {
		instance = this;
		this.directory = new File(Objects.requireNonNull(MinecraftClient.getInstance().runDirectory), "files");
		this.directoryAddon = new File(Objects.requireNonNull(MinecraftClient.getInstance().runDirectory), "files/modules");
	}

	private void setupProtection() {
		NativeHelper.setProfile();
	}

	@Override
	public void onInitialize() {
		setupProtection();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				shutDown();
			} catch (Exception ignored) {}
		}));
	}

	public void init() {
		ensureDirectoryExists();
		try {
			Manager.SYNC_MANAGER = new SyncManager();
			Manager.FUNCTION_MANAGER = new FunctionManager();
			Manager.STYLE_MANAGER = new StyleManager();
			Manager.STYLE_MANAGER.init();
			Manager.ACCOUNT_MANAGER = new AccountManager();
			Manager.ACCOUNT_MANAGER.init();
			Manager.FONT_MANAGER = new FontUtils();
			Manager.FONT_MANAGER.init();
			Manager.COMMAND_MANAGER = new CommandManager();
            Manager.INTELLI_MANAGER = new IntelliManager();
			Manager.DRAG_MANAGER = new DragManager();
			Manager.DRAG_MANAGER.init();
			Manager.MACROS_MANAGER = new MacroManager();
			Manager.MACROS_MANAGER.init();
			Manager.FRIEND_MANAGER = new FriendManager();
			Manager.FRIEND_MANAGER.init();
			Manager.STAFF_MANAGER = new StaffManager();
			Manager.STAFF_MANAGER.init();
			Manager.NOTIFICATION_MANAGER = new NotificationManager();
			Manager.CHESTSTEALER_MANAGER = new ChestStealerManager();
			Manager.PROXY_MANAGER = new ProxyManager();
			Manager.PROXY_MANAGER.init();

			Manager.IRC_MANAGER = new IrcManager();
			Manager.IRC_MANAGER.connect(Manager.USER_PROFILE.getName());

			Manager.CONFIG_MANAGER = new ConfigManager();
			Manager.CONFIG_MANAGER.init();

			ColorUtil.loadImage(ResourceProvider.color_image);
			initialized = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void keyPress(int key) {
		int processedKey = key >= 0 ? key : -(100 + key + 2);
		Event.call(new EventKey(processedKey));

		if (key == Manager.FUNCTION_MANAGER.unHook.unHookKey.getKey() && ClientManager.legitMode) {
			UnHook.functionsToBack.forEach(function -> function.setState(true));
			File folder = new File("C:\\velyasik");
			if (folder.exists()) {
				try {
					Path folderPathObj = folder.toPath();
					DosFileAttributeView attributes = Files.getFileAttributeView(folderPathObj, DosFileAttributeView.class);
					attributes.setHidden(false);
				} catch (IOException ignored) {
				}
			}
			UnHook.functionsToBack.clear();
			ClientManager.legitMode = false;
		}

		if (!ClientManager.legitMode) {
			for (Function module : Manager.FUNCTION_MANAGER.getFunctions()) {
				if (module.bind == processedKey) {
					module.toggle();
				}
				for (Setting setting : module.getSettings()) {
					if (setting instanceof BindBooleanSetting bindSetting) {
						bindSetting.onKeyPress(key, true);
					}
				}
			}

			if (key == Manager.FUNCTION_MANAGER.clickGUI.getBindCode()) {
				MinecraftClient.getInstance().setScreen(new ClickGUI());
			}
			if (Manager.MACROS_MANAGER != null) {
				Manager.MACROS_MANAGER.onKeyPressed(key);
			}
		}
	}

	public void shutDown() {
		Manager.DRAG_MANAGER.save();
		Manager.ACCOUNT_MANAGER.saveAccounts();
		Manager.ACCOUNT_MANAGER.saveLastAlt();
		Manager.CONFIG_MANAGER.saveConfiguration("autocfg");
		Manager.IRC_MANAGER.shutdown();
		Manager.FUNCTION_MANAGER.globals.clear();
		System.out.println("[-] Client shutdown");
	}
	public static void openURL(String url) {
		try {
			String os = System.getProperty("os.name").toLowerCase();

			if (os.contains("win")) {
				Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", url});
			} else if (os.contains("mac")) {
				Runtime.getRuntime().exec(new String[]{"open", url});
			} else {
				Runtime.getRuntime().exec(new String[]{"xdg-open", url});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Dragging createDrag(Function function, String name, float x, float y) {
		DragManager.draggables.put(name, new Dragging(function, name, x, y));
		return DragManager.draggables.get(name);
	}
	private void ensureDirectoryExists() {
		if (!directory.exists() && !directory.mkdirs()) {
			System.err.println("Failed to create directory: " + directory.getAbsolutePath());
		}
		if (!directoryAddon.exists() && !directoryAddon.mkdirs()) {
			System.err.println("Failed to create directory: " + directoryAddon.getAbsolutePath());
		}
	}
}