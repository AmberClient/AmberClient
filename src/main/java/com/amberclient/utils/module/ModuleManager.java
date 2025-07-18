package com.amberclient.utils.module;

import com.amberclient.events.core.EventManager;
import com.amberclient.modules.combat.*;
import com.amberclient.modules.miscellaneous.DiscordRPC;
import com.amberclient.modules.miscellaneous.ActiveMods;
import com.amberclient.modules.miscellaneous.NoAnimations;
import com.amberclient.modules.miscellaneous.Transparency;
import com.amberclient.modules.minigames.MMFinder;
import com.amberclient.modules.movement.AutoClutch;
import com.amberclient.modules.movement.NoFall;
import com.amberclient.modules.movement.SafeWalk;
import com.amberclient.modules.player.AntiHunger;
import com.amberclient.modules.player.FakeLag;
import com.amberclient.modules.player.FastBreak;
import com.amberclient.modules.player.FastPlace;
import com.amberclient.modules.render.EntityESP;
import com.amberclient.modules.render.Fullbright;
import com.amberclient.modules.render.NoHurtCam;
import com.amberclient.modules.render.Trajectory;
import com.amberclient.modules.render.xray.Xray;
import com.amberclient.modules.world.GhostHand;
import com.amberclient.modules.world.MacroRecorder.MacroController;
import com.amberclient.utils.input.keybinds.KeybindConfigManager;
import com.amberclient.utils.input.keybinds.CustomKeybindManager;
import com.amberclient.utils.input.keybinds.KeybindsManager;
import com.amberclient.utils.minecraft.MinecraftUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModuleManager {
    private static final ModuleManager INSTANCE = new ModuleManager();
    private final List<Module> modules = new ArrayList<>();
    private boolean keybindsInitialized = false;

    private ModuleManager() {
        KeybindsManager.INSTANCE.initialize();
        CustomKeybindManager.INSTANCE.initialize();

        // Register modules
        registerModule(new AutoClicker());
        registerModule(new Hitbox());
        registerModule(new KillAura());
        registerModule(new ActiveMods());
        registerModule(new Transparency());
        registerModule(new DiscordRPC());
        registerModule(new NoFall());
        registerModule(new SafeWalk());
        registerModule(new FastBreak());
        registerModule(new FastPlace());
        registerModule(new Xray());
        registerModule(new EntityESP());
        registerModule(new Fullbright());
        registerModule(new MMFinder());
        registerModule(new AutoClutch());
        registerModule(new Velocity());
        registerModule(new NoHurtCam());
        registerModule(new AimAssist());
        registerModule(new MacroController());
        registerModule(new AutoPotion());
        registerModule(new AntiHunger());
        registerModule(new FakeLag());
        registerModule(new Trajectory());
        registerModule(new GhostHand());
        registerModule(new NoAnimations());

        for (Module module : modules) {
            EventManager.getInstance().register(module);
        }
    }

    public static ModuleManager getInstance() {
        return INSTANCE;
    }

    public void initializeKeybinds() {
        if (keybindsInitialized) return;

        System.out.println("[ModuleManager] Initializing keybind system...");

        loadModuleKeybinds();

        keybindsInitialized = true;
        System.out.println("[ModuleManager] Keybind system initialized!");
    }

    private void loadModuleKeybinds() {
        Map<String, String> savedKeybinds = KeybindConfigManager.INSTANCE.getAllModuleKeybinds();

        for (Map.Entry<String, String> entry : savedKeybinds.entrySet()) {
            String moduleName = entry.getKey();
            String keyName = entry.getValue();

            Module module = modules.stream()
                    .filter(m -> m.getName().equalsIgnoreCase(moduleName))
                    .findFirst()
                    .orElse(null);

            if (module != null) {
                bindKeyToModuleInternal(module, keyName, false);
                System.out.println("[ModuleManager] Restored keybind: " + keyName + " -> " + moduleName);
            } else {
                System.out.println("[ModuleManager] Warning: Module '" + moduleName + "' not found for saved keybind");
            }
        }
    }

    public void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        modules.stream()
                .filter(Module::isEnabled)
                .forEach(module -> {
                    try {
                        module.onTick();
                    } catch (Exception e) {
                        System.err.println("Error in " + module.getName() + ": " + e.getMessage());
                    }
                });

        CustomKeybindManager.INSTANCE.tick();
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> getEnabledModules() {
        return modules.stream()
                .filter(Module::isEnabled)
                .collect(Collectors.toList());
    }

    public void toggleModule(Module module) {
        if (module != null) module.toggle();
    }

    public void registerModule(Module module) {
        if (module != null && !modules.contains(module)) {
            modules.add(module);
        }
    }

    public void handleKeyInputs() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || MinecraftUtils.isChatOpen()) {
            return;
        }

        for (Map.Entry<String, KeyBinding> entry : KeybindsManager.INSTANCE.getKeyBindings().entrySet()) {
            KeyBinding keyBinding = entry.getValue();

            while (keyBinding.wasPressed()) {
                modules.stream()
                        .filter(m -> m.getKeyBinding() == keyBinding)
                        .findFirst().ifPresent(Module::toggle);
            }
        }
    }

    public void bindKeyToModule(Module module, String keyName) {
        bindKeyToModuleInternal(module, keyName, true);
    }

    private void bindKeyToModuleInternal(Module module, String keyName, boolean saveToConfig) {
        int keyCode = KeybindsManager.INSTANCE.getKeyCodeFromName(keyName);

        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) {
            System.err.println("[ModuleManager] Invalid key name: " + keyName);
            return;
        }

        unbindModuleInternal(module, false);

        String actionId = "module_" + module.getName().toLowerCase().replace(" ", "_");

        CustomKeybindManager.INSTANCE.bindKey(
                keyCode,
                actionId,
                "Toggle " + module.getName(),
                true,
                new Runnable() {
                    @Override
                    public void run() {
                        // Check if chat is open before toggling
                        if (MinecraftUtils.isChatOpen()) {
                            return;
                        }

                        module.toggle();
                        System.out.println("Toggled " + module.getName() + " via custom keybind");
                    }
                }
        );

        module.setCustomKeyCode(keyCode);

        if (saveToConfig) {
            KeybindConfigManager.INSTANCE.setModuleKeybind(module.getName(), keyName);
        }

        System.out.println("[ModuleManager] Successfully bound " + CustomKeybindManager.INSTANCE.getKeyName(keyCode) +
                " to " + module.getName());
    }

    public void unbindModule(Module module) {
        unbindModuleInternal(module, true);
    }

    private void unbindModuleInternal(Module module, boolean removeFromConfig) {
        int currentKeyCode = module.getCustomKeyCode();
        if (currentKeyCode != -1) {
            String actionId = "module_" + module.getName().toLowerCase().replace(" ", "_");
            CustomKeybindManager.INSTANCE.unbindAction(currentKeyCode, actionId);
            module.setCustomKeyCode(-1);

            if (removeFromConfig) {
                KeybindConfigManager.INSTANCE.removeModuleKeybind(module.getName());
            }

            System.out.println("[ModuleManager] Unbound " + module.getName() + " from key");
        }
    }

    public String getModuleKeybind(Module module) {
        return KeybindConfigManager.INSTANCE.getModuleKeybind(module.getName());
    }

    public boolean hasModuleKeybind(Module module) {
        return getModuleKeybind(module) != null;
    }

    public Map<String, String> getAllModuleKeybinds() {
        return KeybindConfigManager.INSTANCE.getAllModuleKeybinds();
    }

    public String getModuleKeyName(Module module) {
        int keyCode = module.getCustomKeyCode();
        if (keyCode == -1) {
            return "Not bound";
        }
        return CustomKeybindManager.INSTANCE.getKeyName(keyCode);
    }

    public void listAllKeybinds() {
        System.out.println("=== Custom Keybinds ===");
        Map<Integer, List<CustomKeybindManager.KeybindAction>> bindings = CustomKeybindManager.INSTANCE.getAllBindings();

        if (bindings.isEmpty()) {
            System.out.println("No custom keybinds registered.");
            return;
        }

        bindings.forEach((keyCode, actions) -> {
            String keyName = CustomKeybindManager.INSTANCE.getKeyName(keyCode);
            System.out.println(keyName + ":");
            actions.forEach(action -> {
                System.out.println("  - " + action.getDescription());
            });
        });
    }

    public Module findModuleByName(String name) {
        return modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}