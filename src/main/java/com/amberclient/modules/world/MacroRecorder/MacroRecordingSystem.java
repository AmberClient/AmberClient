package com.amberclient.modules.world.MacroRecorder;

import com.amberclient.gui.MacroControllerGUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MacroRecordingSystem {
    private static final Logger LOGGER = LogManager.getLogger("amberclient-macrorecordingsystem");
    private static MacroRecordingSystem instance;

    private boolean isRecording = false;
    private final List<MacroAction> recordedActions = new CopyOnWriteArrayList<>();
    private final List<MacroRecordingListener> listeners = new ArrayList<>();
    private long recordingStartTime = 0;
    private final MinecraftClient client;

    private final boolean[] previousKeyStates = new boolean[512];
    private final boolean[] previousMouseStates = new boolean[8];
    private float lastYaw = 0;
    private float lastPitch = 0;

    private MacroRecordingSystem() {
        this.client = MinecraftClient.getInstance();
    }

    public static MacroRecordingSystem getInstance() {
        if (instance == null) {
            instance = new MacroRecordingSystem();
        }
        return instance;
    }

    public void startRecording() {
        if (isRecording) {
            LOGGER.warn("Already recording a macro!");
            return;
        }

        isRecording = true;
        recordedActions.clear();
        recordingStartTime = System.currentTimeMillis();

        initializePreviousStates();

        if (client.currentScreen instanceof MacroControllerGUI) {
            client.setScreen(null);
        }

        LOGGER.info("=== MACRO RECORDING STARTED ===");
        LOGGER.info("Recording start time: {}", recordingStartTime);
        LOGGER.info("Initial action count: {}", recordedActions.size());

        notifyListeners(true);
    }

    public void stopRecording() {
        if (!isRecording) {
            return;
        }

        isRecording = false;
        LOGGER.info("Stopped recording macro with {} actions", recordedActions.size());
        notifyListeners(false);
    }

    public void tick() {
        if (!isRecording || client.player == null) {
            return;
        }

        if (System.currentTimeMillis() % 1000 < 50) {
            LOGGER.info("MacroRecordingSystem tick - Recording: {}, Actions: {}",
                    isRecording, recordedActions.size());
        }

        if (client.currentScreen instanceof MacroControllerGUI) {
            stopRecording();
            return;
        }

        long currentTime = System.currentTimeMillis() - recordingStartTime;

        recordKeyboardActions(currentTime);
        recordMouseActions(currentTime);
        recordMouseMovement(currentTime);
        recordInteractions(currentTime);
    }

    private void initializePreviousStates() {
        for (int i = 0; i < previousKeyStates.length; i++) {
            try {
                previousKeyStates[i] = InputUtil.isKeyPressed(client.getWindow().getHandle(), i);
            } catch (Exception e) {
                previousKeyStates[i] = false;
            }
        }

        for (int i = 0; i < previousMouseStates.length; i++) {
            try {
                int mouseButton = GLFW.GLFW_MOUSE_BUTTON_1 + i;
                if (mouseButton <= GLFW.GLFW_MOUSE_BUTTON_8) {
                    previousMouseStates[i] = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), mouseButton) == GLFW.GLFW_PRESS;
                } else {
                    previousMouseStates[i] = false;
                }
            } catch (Exception e) {
                previousMouseStates[i] = false;
            }
        }

        if (client.player != null) {
            lastYaw = client.player.getYaw();
            lastPitch = client.player.getPitch();
        }
    }

    private void recordKeyboardActions(long timestamp) {
        int[] validKeys = {
                // Letters
                GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_E,
                GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_J,
                GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_M, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_O,
                GLFW.GLFW_KEY_P, GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_T,
                GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_Y,
                GLFW.GLFW_KEY_Z,

                // Numbers
                GLFW.GLFW_KEY_0, GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_4,
                GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_7, GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9,

                // Special keys
                GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_BACKSPACE,
                GLFW.GLFW_KEY_DELETE, GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT,
                GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL, GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT,

                // Arrows
                GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT,

                // Functions keys
                GLFW.GLFW_KEY_F1, GLFW.GLFW_KEY_F2, GLFW.GLFW_KEY_F3, GLFW.GLFW_KEY_F4,
                GLFW.GLFW_KEY_F5, GLFW.GLFW_KEY_F6, GLFW.GLFW_KEY_F7, GLFW.GLFW_KEY_F8,
                GLFW.GLFW_KEY_F9, GLFW.GLFW_KEY_F10, GLFW.GLFW_KEY_F11, GLFW.GLFW_KEY_F12,

                // Other common keys
                GLFW.GLFW_KEY_CAPS_LOCK, GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER,
                GLFW.GLFW_KEY_MENU, GLFW.GLFW_KEY_INSERT, GLFW.GLFW_KEY_HOME, GLFW.GLFW_KEY_END,
                GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_PAGE_DOWN,

                // Symbols
                GLFW.GLFW_KEY_SEMICOLON, GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_MINUS,
                GLFW.GLFW_KEY_PERIOD, GLFW.GLFW_KEY_SLASH, GLFW.GLFW_KEY_GRAVE_ACCENT,
                GLFW.GLFW_KEY_LEFT_BRACKET, GLFW.GLFW_KEY_BACKSLASH, GLFW.GLFW_KEY_RIGHT_BRACKET,
                GLFW.GLFW_KEY_APOSTROPHE
        };

        for (int keyCode : validKeys) {
            try {
                boolean currentState = InputUtil.isKeyPressed(client.getWindow().getHandle(), keyCode);
                boolean previousState = keyCode < previousKeyStates.length && previousKeyStates[keyCode];

                if (currentState != previousState) {
                    MacroAction action = new MacroAction(
                            currentState ? MacroAction.Type.KEY_PRESS : MacroAction.Type.KEY_RELEASE,
                            timestamp,
                            keyCode
                    );
                    recordedActions.add(action);

                    if (keyCode < previousKeyStates.length) {
                        previousKeyStates[keyCode] = currentState;
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to check key state for key {}: {}", keyCode, e.getMessage());
            }
        }
    }

    private void checkKeybinding(KeyBinding keyBinding, long timestamp, String actionName) {
        if (keyBinding.wasPressed()) {
            MacroAction action = new MacroAction(
                    MacroAction.Type.KEYBINDING_PRESS,
                    timestamp,
                    actionName
            );
            recordedActions.add(action);
        }
    }

    private void recordMouseActions(long timestamp) {
        int[] mouseButtons = {
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
                GLFW.GLFW_MOUSE_BUTTON_4,
                GLFW.GLFW_MOUSE_BUTTON_5,
                GLFW.GLFW_MOUSE_BUTTON_6,
                GLFW.GLFW_MOUSE_BUTTON_7,
                GLFW.GLFW_MOUSE_BUTTON_8
        };

        for (int i = 0; i < mouseButtons.length && i < previousMouseStates.length; i++) {
            try {
                boolean currentState = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), mouseButtons[i]) == GLFW.GLFW_PRESS;
                boolean previousState = previousMouseStates[i];

                if (currentState != previousState) {
                    MacroAction action = new MacroAction(
                            currentState ? MacroAction.Type.MOUSE_PRESS : MacroAction.Type.MOUSE_RELEASE,
                            timestamp,
                            mouseButtons[i]
                    );
                    recordedActions.add(action);
                    previousMouseStates[i] = currentState;
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to check mouse button state for button {}: {}", mouseButtons[i], e.getMessage());
            }
        }
    }

    private void recordMouseMovement(long timestamp) {
        if (client.player == null) return;

        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();

        if (Math.abs(currentYaw - lastYaw) > 0.1f || Math.abs(currentPitch - lastPitch) > 0.1f) {
            MacroAction action = new MacroAction(
                    MacroAction.Type.MOUSE_MOVE,
                    timestamp,
                    currentYaw,
                    currentPitch
            );
            recordedActions.add(action);

            lastYaw = currentYaw;
            lastPitch = currentPitch;
        }
    }

    private void registerScrollCallback() {
        GLFW.glfwSetScrollCallback(client.getWindow().getHandle(), (window, xoffset, yoffset) -> {
            if (isRecording) {
                long currentTime = System.currentTimeMillis() - recordingStartTime;

                MacroAction scrollAction = new MacroAction(
                        MacroAction.Type.MOUSE_SCROLL,
                        currentTime,
                        xoffset,
                        yoffset
                );
                recordedActions.add(scrollAction);

                LOGGER.debug("Recorded scroll: x={}, y={} at time {}", xoffset, yoffset, currentTime);
            }
        });
    }

    private void recordInteractions(long timestamp) {
        if (client.player == null) return;

        if (client.crosshairTarget != null) {
            if (client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) client.crosshairTarget;

                // Record block interaction data
                MacroAction blockInteraction = new MacroAction(
                        MacroAction.Type.BLOCK_INTERACT,
                        timestamp,
                        blockHit.getBlockPos().toString(),
                        blockHit.getSide().toString()
                );
                recordedActions.add(blockInteraction);

                LOGGER.debug("Recorded block interaction at {} on face {}",
                        blockHit.getBlockPos(), blockHit.getSide());

            } else if (client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) client.crosshairTarget;

                MacroAction entityInteraction = new MacroAction(
                        MacroAction.Type.ENTITY_INTERACT,
                        timestamp,
                        entityHit.getEntity().getUuidAsString(),
                        entityHit.getEntity().getType().toString()
                );
                recordedActions.add(entityInteraction);

                LOGGER.debug("Recorded entity interaction with {} (UUID: {})",
                        entityHit.getEntity().getType(), entityHit.getEntity().getUuidAsString());
            }
        }
    }

    public void loadActions(List<MacroAction> actions) {
        if (isRecording) {
            LOGGER.warn("Cannot load actions while recording!");
            return;
        }

        recordedActions.clear();
        recordedActions.addAll(actions);
        LOGGER.info("Loaded {} actions into recording system", actions.size());
    }

    public void addListener(MacroRecordingListener listener) {
        listeners.add(listener);
    }

    public void removeListener(MacroRecordingListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(boolean started) {
        for (MacroRecordingListener listener : listeners) {
            if (started) {
                listener.onRecordingStarted();
            } else {
                listener.onRecordingStopped();
            }
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public List<MacroAction> getRecordedActions() {
        return new ArrayList<>(recordedActions);
    }

    public int getActionCount() {
        return recordedActions.size();
    }

    public void clearRecording() {
        recordedActions.clear();
    }

    public interface MacroRecordingListener {
        void onRecordingStarted();
        void onRecordingStopped();
    }

    public static class MacroAction {
        public enum Type {
            KEY_PRESS,
            KEY_RELEASE,
            KEYBINDING_PRESS,
            MOUSE_PRESS,
            MOUSE_RELEASE,
            MOUSE_MOVE,
            MOUSE_SCROLL,
            BLOCK_INTERACT,
            ENTITY_INTERACT,
            CHAT_MESSAGE,
            INVENTORY_ACTION
        }

        private final Type type;
        private final long timestamp;
        private final Object data;
        private final Object data2;

        public MacroAction(Type type, long timestamp, Object data) {
            this.type = type;
            this.timestamp = timestamp;
            this.data = data;
            this.data2 = null;
        }

        public MacroAction(Type type, long timestamp, Object data, Object data2) {
            this.type = type;
            this.timestamp = timestamp;
            this.data = data;
            this.data2 = data2;
        }

        public Type getType() { return type; }
        public long getTimestamp() { return timestamp; }
        public Object getData() { return data; }
        public Object getData2() { return data2; }

        @Override
        public String toString() {
            return String.format("MacroAction{type=%s, timestamp=%d, data=%s, data2=%s}",
                    type, timestamp, data, data2);
        }
    }
}