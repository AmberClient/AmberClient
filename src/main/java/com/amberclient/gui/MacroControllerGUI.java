package com.amberclient.gui;

//
// TODO: Don't mind this mess, imma clean this later
//

import com.amberclient.modules.world.MacroRecorder.MacroPlaybackSystem;
import com.amberclient.modules.world.MacroRecorder.MacroRecordingSystem;
import com.amberclient.modules.world.MacroRecorder.MacrosManager;
import com.amberclient.utils.module.ModuleManager;
import com.amberclient.modules.miscellaneous.Transparency;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.*;

public class MacroControllerGUI extends Screen {
    // Theme colors
    private static final int BASE_BG = new Color(20, 20, 25, 200).getRGB();
    private static final int PANEL_BG = new Color(30, 30, 35, 255).getRGB();
    private static final int ACCENT = new Color(255, 165, 0).getRGB();
    private static final int ACCENT_HOVER = new Color(255, 190, 50).getRGB();
    private static final int TEXT = new Color(220, 220, 220).getRGB();
    private static final int OUTLINE = new Color(255, 255, 255, 180).getRGB();
    private static final int SUCCESS_COLOR = new Color(0, 255, 0).getRGB();
    private static final int ERROR_COLOR = new Color(255, 0, 0).getRGB();
    private static final int RECORDING_COLOR = new Color(255, 100, 100).getRGB();

    // State variables
    private float animProgress = 0.0f;
    private float macroListScroll = 0.0f;
    private float recorderScroll = 0.0f;
    private long lastTime = System.currentTimeMillis();

    // Macro recorder state
    private boolean isRecording = false;
    private final boolean isPlaying = false;
    private String currentMacroName = "New Macro";
    private final List<MacrosManager.SavedMacro> savedMacros = new ArrayList<>();
    private MacrosManager.SavedMacro selectedMacro = null;
    private List<MacroRecordingSystem.MacroAction> selectedMacroActions = new ArrayList<>();
    private float actionsScroll = 0.0f;
    private int selectedTab = 0; // 0 = Recorder, 1 = Saved Macros, 2 = Actions
    private final ScrollState actionsScrollState = new ScrollState();

    // UI state
    private boolean editingName = false;

    private final MacroRecordingSystem recordingSystem;
    private MacroRecordingSystem.MacroRecordingListener recordingListener;

    private String nameBuffer = "";
    private int cursorPosition = 0;

    private final MacrosManager persistenceManager;

    public MacroControllerGUI() {
        super(Text.literal("Macro Recorder - Amber Client"));
        this.recordingSystem = MacroRecordingSystem.getInstance();
        this.persistenceManager = new MacrosManager();
        initSavedMacros();
        setupRecordingListener();
    }

    private static class ScrollState {
        boolean isDragging = false;
        int dragStartY;
        float dragStartOffset;
    }

    private record PanelBounds(int x, int y, int width, int height) {}

    private void setupRecordingListener() {
        recordingListener = new MacroRecordingSystem.MacroRecordingListener() {
            @Override
            public void onRecordingStarted() {
                isRecording = true;
                setStatusMessage("Recording started", RECORDING_COLOR);
            }

            @Override
            public void onRecordingStopped() {
                isRecording = false;
                setStatusMessage("Recording stopped", SUCCESS_COLOR);
            }
        };
        recordingSystem.addListener(recordingListener);
    }

    private void initSavedMacros() {
        try {
            savedMacros.clear();
            savedMacros.addAll(persistenceManager.loadMacros());
        } catch (Exception e) {
            setStatusMessage("Failed to load saved macros", ERROR_COLOR);
        }
    }

    private float getTransparency() {
        return ModuleManager.getInstance().getModules().stream()
                .filter(m -> m instanceof Transparency && m.isEnabled())
                .map(m -> ((Transparency) m).getTransparencyLevel())
                .findFirst()
                .orElse(0.75f);
    }

    private int applyTransparency(int color, float alpha) {
        return ((int) (((color >> 24) & 0xFF) * alpha) << 24) | (color & 0xFFFFFF);
    }

    @Override
    protected void init() {
        super.init();
        addDrawableChild(ButtonWidget.builder(Text.literal("×"), b -> close())
                .dimensions(width - 25, 5, 20, 20)
                .tooltip(Tooltip.of(Text.literal("Close")))
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        recordingSystem.tick();

        isRecording = recordingSystem.isRecording();
    }

    @Override
    public void render(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        long time = System.currentTimeMillis();
        animProgress = MathHelper.clamp(animProgress + (time - lastTime) / 300.0f, 0.0f, 1.0f);
        lastTime = time;

        float trans = getTransparency();
        renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, width, height, applyTransparency(BASE_BG, trans));

        super.render(context, mouseX, mouseY, delta);

        int centerX = width / 2;
        context.drawCenteredTextWithShadow(textRenderer, "MACRO RECORDER", centerX, 52, ACCENT);

        PanelBounds mainPanel = calcMainPanel();
        context.fill(mainPanel.x, mainPanel.y, mainPanel.x + mainPanel.width, mainPanel.y + mainPanel.height,
                applyTransparency(PANEL_BG, animProgress * trans));

        renderTabs(context, mainPanel, mouseX, mouseY, trans);

        if (selectedTab == 0) {
            renderRecorderTab(context, mainPanel, mouseX, mouseY, trans);
        } else if (selectedTab == 1) {
            renderSavedMacrosTab(context, mainPanel, mouseX, mouseY, trans);
        } else if (selectedTab == 2) {
            renderActionsTab(context, mainPanel, mouseX, mouseY, trans);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private PanelBounds calcMainPanel() {
        int w = Math.min(width - 40, 900);
        int h = Math.min(height - 100, 500);
        float scale = 0.8f + 0.2f * animProgress;
        int scaledW = (int)(w * scale);
        int scaledH = (int)(h * scale);
        return new PanelBounds(width / 2 - scaledW / 2, 82 + (h - scaledH) / 2, scaledW, scaledH);
    }

    private void renderTabs(DrawContext context, PanelBounds panel, int mouseX, int mouseY, float trans) {
        String[] tabs = {"Recorder", "Saved Macros", "Actions"};
        int tabWidth = 120;
        int tabHeight = 30;
        int startX = panel.x + 10;
        int tabY = panel.y + 10;

        for (int i = 0; i < tabs.length; i++) {
            int tabX = startX + i * (tabWidth + 5);
            boolean isSelected = selectedTab == i;
            boolean isHovered = isMouseOver(mouseX, mouseY, tabX, tabY, tabWidth, tabHeight);

            int bgColor = isSelected ? ACCENT :
                    isHovered ? applyTransparency(new Color(50, 50, 55, 220).getRGB(), trans) :
                            applyTransparency(new Color(40, 40, 45, 200).getRGB(), trans);

            context.fill(tabX, tabY, tabX + tabWidth, tabY + tabHeight, bgColor);
            drawBorder(context, tabX, tabY, tabWidth, tabHeight);

            int textColor = isSelected ? Color.WHITE.getRGB() : TEXT;
            context.drawCenteredTextWithShadow(textRenderer, tabs[i], tabX + tabWidth / 2, tabY + (tabHeight - 8) / 2, textColor);
        }
    }

    private void renderRecorderTab(DrawContext context, PanelBounds panel, int mouseX, int mouseY, float trans) {
        int contentX = panel.x + 20;
        int contentY = panel.y + 50;
        int contentWidth = panel.width - 40;
        int contentHeight = panel.height - 100;

        context.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);

        int macroInfoY = contentY - (int)recorderScroll;
        int controlsY = macroInfoY + 120 + 10;
        int statsY = controlsY + 100 + 10;

        renderMacroInfoSection(context, contentX, macroInfoY, contentWidth, mouseX, mouseY, trans);
        renderControlsSection(context, contentX, controlsY, contentWidth, mouseX, mouseY, trans);
        renderStatsSection(context, contentX, statsY, contentWidth, mouseX, mouseY, trans);

        int totalContentHeight = statsY + 150 - contentY;

        if (totalContentHeight > contentHeight) {
            float scrollRatio = (float) contentHeight / totalContentHeight;
            int thumbHeight = Math.max(20, (int)(contentHeight * scrollRatio));
            int thumbY = contentY + (int)((contentHeight - thumbHeight) * (recorderScroll / (totalContentHeight - contentHeight)));
            context.fill(contentX + contentWidth - 15, contentY, contentX + contentWidth - 10, contentY + contentHeight, applyTransparency(new Color(50, 50, 55).getRGB(), trans));
            context.fill(contentX + contentWidth - 15, thumbY, contentX + contentWidth - 10, thumbY + thumbHeight, ACCENT);
        }

        context.disableScissor();
    }

    private void renderMacroInfoSection(DrawContext context, int x, int y, int width, int mouseX, int mouseY, float trans) {
        context.fill(x, y, x + width, y + 120, applyTransparency(new Color(40, 40, 45, 220).getRGB(), trans));
        drawBorder(context, x, y, width, 120);

        context.drawTextWithShadow(textRenderer, "Macro configuration", x + 10, y + 10, ACCENT);

        context.drawTextWithShadow(textRenderer, "Name:", x + 10, y + 30, TEXT);
        int nameFieldX = x + 60;
        int nameFieldY = y + 25;
        int nameFieldW = 200;
        int nameFieldH = 20;

        boolean nameHovered = isMouseOver(mouseX, mouseY, nameFieldX, nameFieldY, nameFieldW, nameFieldH);
        int nameFieldColor = editingName ? ACCENT_HOVER : (nameHovered ? new Color(60, 60, 65).getRGB() : new Color(50, 50, 55).getRGB());
        context.fill(nameFieldX, nameFieldY, nameFieldX + nameFieldW, nameFieldY + nameFieldH, nameFieldColor);
        drawBorder(context, nameFieldX, nameFieldY, nameFieldW, nameFieldH);

        String displayText = editingName ? nameBuffer : currentMacroName;
        context.drawTextWithShadow(textRenderer, displayText, nameFieldX + 5, nameFieldY + 5, Color.WHITE.getRGB());

        if (editingName && System.currentTimeMillis() % 1000 < 500) {
            String textBeforeCursor = nameBuffer.substring(0, Math.min(cursorPosition, nameBuffer.length()));
            int cursorX = nameFieldX + 5 + textRenderer.getWidth(textBeforeCursor);
            context.drawVerticalLine(cursorX, nameFieldY + 3, nameFieldY + nameFieldH - 3, Color.WHITE.getRGB());
        }
    }

    private void renderControlsSection(DrawContext context, int x, int y, int width, int mouseX, int mouseY, float trans) {
        context.fill(x, y, x + width, y + 100, applyTransparency(new Color(40, 40, 45, 220).getRGB(), trans));
        drawBorder(context, x, y, width, 100);

        context.drawTextWithShadow(textRenderer, "Controls", x + 10, y + 10, ACCENT);

        int buttonWidth = 100;
        int buttonHeight = 25;
        int buttonSpacing = 10;
        int buttonsStartX = x + 20;
        int buttonsY = y + 35;

        boolean recordHovered = isMouseOver(mouseX, mouseY, buttonsStartX, buttonsY, buttonWidth, buttonHeight);
        int recordColor = isRecording ? RECORDING_COLOR : (recordHovered ? ACCENT_HOVER : ACCENT);
        context.fill(buttonsStartX, buttonsY, buttonsStartX + buttonWidth, buttonsY + buttonHeight, recordColor);
        drawBorder(context, buttonsStartX, buttonsY, buttonWidth, buttonHeight);
        String recordText = isRecording ? "Stop" : "Record";
        context.drawCenteredTextWithShadow(textRenderer, recordText, buttonsStartX + buttonWidth / 2, buttonsY + 8, Color.WHITE.getRGB());

        int playButtonX = buttonsStartX + buttonWidth + buttonSpacing;
        boolean playHovered = isMouseOver(mouseX, mouseY, playButtonX, buttonsY, buttonWidth, buttonHeight);
        int playColor = isPlaying ? SUCCESS_COLOR : (playHovered ? ACCENT_HOVER : ACCENT);
        boolean playEnabled = !isRecording && !isPlaying;
        if (!playEnabled) playColor = new Color(100, 100, 100).getRGB();
        context.fill(playButtonX, buttonsY, playButtonX + buttonWidth, buttonsY + buttonHeight, playColor);
        drawBorder(context, playButtonX, buttonsY, buttonWidth, buttonHeight);
        String playText = isPlaying ? "Playback.." : "Play";
        context.drawCenteredTextWithShadow(textRenderer, playText, playButtonX + buttonWidth / 2, buttonsY + 8, Color.WHITE.getRGB());

        int saveButtonX = playButtonX + buttonWidth + buttonSpacing;
        boolean saveHovered = isMouseOver(mouseX, mouseY, saveButtonX, buttonsY, buttonWidth, buttonHeight);
        int saveColor = saveHovered ? ACCENT_HOVER : ACCENT;
        boolean saveEnabled = !isRecording && !isPlaying;
        if (!saveEnabled) saveColor = new Color(100, 100, 100).getRGB();
        context.fill(saveButtonX, buttonsY, saveButtonX + buttonWidth, buttonsY + buttonHeight, saveColor);
        drawBorder(context, saveButtonX, buttonsY, buttonWidth, buttonHeight);
        context.drawCenteredTextWithShadow(textRenderer, "Save", saveButtonX + buttonWidth / 2, buttonsY + 8, Color.WHITE.getRGB());

        int resetButtonX = saveButtonX + buttonWidth + buttonSpacing;
        boolean resetHovered = isMouseOver(mouseX, mouseY, resetButtonX, buttonsY, buttonWidth, buttonHeight);
        int resetColor = resetHovered ? new Color(255, 100, 100).getRGB() : new Color(200, 80, 80).getRGB();
        boolean resetEnabled = !isRecording && !isPlaying;
        if (!resetEnabled) resetColor = new Color(100, 100, 100).getRGB();
        context.fill(resetButtonX, buttonsY, resetButtonX + buttonWidth, buttonsY + buttonHeight, resetColor);
        drawBorder(context, resetButtonX, buttonsY, buttonWidth, buttonHeight);
        context.drawCenteredTextWithShadow(textRenderer, "Reinitialize", resetButtonX + buttonWidth / 2, buttonsY + 8, Color.WHITE.getRGB());
    }

    private void renderStatsSection(DrawContext context, int x, int y, int width, int mouseX, int mouseY, float trans) {
        context.fill(x, y, x + width, y + 150, applyTransparency(new Color(40, 40, 45, 220).getRGB(), trans));
        drawBorder(context, x, y, width, 150);

        context.drawTextWithShadow(textRenderer, "Statistics", x + 10, y + 10, ACCENT);

        String statusText = isRecording ? "Recording in progress.." :
                isPlaying ? "▶ Playback in progress.." : "⏸Ready";
        int statusColor = isRecording ? RECORDING_COLOR :
                isPlaying ? SUCCESS_COLOR : TEXT;
        context.drawTextWithShadow(textRenderer, statusText, x + 10, y + 30, statusColor);

        int actionCount = recordingSystem.getActionCount();
        context.drawTextWithShadow(textRenderer, "Actions in the current macro: " + actionCount, x + 10, y + 50, TEXT);
        context.drawTextWithShadow(textRenderer, "Saved macros: " + savedMacros.size(), x + 10, y + 70, TEXT);
    }

    private void renderSavedMacrosTab(DrawContext context, PanelBounds panel, int mouseX, int mouseY, float trans) {
        int contentY = panel.y + 50;
        int contentHeight = panel.height - 100;

        context.drawTextWithShadow(textRenderer, "Saved macros (" + savedMacros.size() + ")", panel.x + 20, contentY, ACCENT);

        int listX = panel.x + 20;
        int listY = contentY + 20;
        int listWidth = panel.width - 15;
        int listHeight = contentHeight - 20;

        context.enableScissor(listX, listY, listX + listWidth, listY + listHeight);

        int macroHeight = 50;
        int macroSpacing = 5;
        int totalContentHeight = savedMacros.size() * (macroHeight + macroSpacing) - macroSpacing;

        macroListScroll = MathHelper.clamp(macroListScroll, 0, Math.max(0, totalContentHeight - listHeight));

        for (int i = 0; i < savedMacros.size(); i++) {
            MacrosManager.SavedMacro macro = savedMacros.get(i);
            int macroY = listY + i * (macroHeight + macroSpacing) - (int)macroListScroll;

            if (macroY + macroHeight < listY || macroY > listY + listHeight) continue;

            boolean isSelected = selectedMacro == macro;
            boolean isHovered = isMouseOver(mouseX, mouseY, listX, macroY, listWidth - 20, macroHeight);

            int bgColor = isSelected ? ACCENT :
                    isHovered ? applyTransparency(new Color(50, 50, 55, 220).getRGB(), trans) :
                            applyTransparency(new Color(40, 40, 45, 200).getRGB(), trans);

            context.fill(listX, macroY, listX + listWidth - 20, macroY + macroHeight, bgColor);
            drawBorder(context, listX, macroY, listWidth - 20, macroHeight);

            context.drawTextWithShadow(textRenderer, macro.name, listX + 10, macroY + 8, Color.WHITE.getRGB());
            context.drawTextWithShadow(textRenderer, "Actions: " + macro.actionCount, listX + 10, macroY + 22, new Color(180, 180, 180).getRGB());

            // Calcul dynamique de la position des boutons
            int buttonY = macroY + 10;
            int buttonHeight = 20;
            int buttonSpacing = 8;

            // Largeurs adaptées au contenu
            int playButtonWidth = 50;
            int editButtonWidth = 50;
            int viewActionsButtonWidth = 85; // Plus large pour "View Actions"
            int deleteButtonWidth = 60;

            int totalButtonsWidth = playButtonWidth + editButtonWidth + viewActionsButtonWidth + deleteButtonWidth + (3 * buttonSpacing);

            int availableWidth = listWidth - 20;
            int buttonsStartX = listX + availableWidth - totalButtonsWidth - 10;

            // Play button
            boolean playButtonHovered = isMouseOver(mouseX, mouseY, buttonsStartX, buttonY, playButtonWidth, buttonHeight);
            int playButtonColor = playButtonHovered ? SUCCESS_COLOR : new Color(0, 150, 0).getRGB();
            context.fill(buttonsStartX, buttonY, buttonsStartX + playButtonWidth, buttonY + buttonHeight, playButtonColor);
            drawBorder(context, buttonsStartX, buttonY, playButtonWidth, buttonHeight);
            context.drawCenteredTextWithShadow(textRenderer, "Play", buttonsStartX + playButtonWidth / 2, buttonY + 5, Color.WHITE.getRGB());

            // Edit button
            int editButtonX = buttonsStartX + playButtonWidth + buttonSpacing;
            boolean editButtonHovered = isMouseOver(mouseX, mouseY, editButtonX, buttonY, editButtonWidth, buttonHeight);
            int editButtonColor = editButtonHovered ? ACCENT_HOVER : ACCENT;
            context.fill(editButtonX, buttonY, editButtonX + editButtonWidth, buttonY + buttonHeight, editButtonColor);
            drawBorder(context, editButtonX, buttonY, editButtonWidth, buttonHeight);
            context.drawCenteredTextWithShadow(textRenderer, "Edit", editButtonX + editButtonWidth / 2, buttonY + 5, Color.WHITE.getRGB());

            // View Actions button
            int viewActionsButtonX = editButtonX + editButtonWidth + buttonSpacing;
            boolean viewActionsButtonHovered = isMouseOver(mouseX, mouseY, viewActionsButtonX, buttonY, viewActionsButtonWidth, buttonHeight);
            int viewActionsButtonColor = viewActionsButtonHovered ? new Color(100, 150, 255).getRGB() : new Color(70, 120, 200).getRGB();
            context.fill(viewActionsButtonX, buttonY, viewActionsButtonX + viewActionsButtonWidth, buttonY + buttonHeight, viewActionsButtonColor);
            drawBorder(context, viewActionsButtonX, buttonY, viewActionsButtonWidth, buttonHeight);
            context.drawCenteredTextWithShadow(textRenderer, "View Actions", viewActionsButtonX + viewActionsButtonWidth / 2, buttonY + 5, Color.WHITE.getRGB());

            // Delete button
            int deleteButtonX = viewActionsButtonX + viewActionsButtonWidth + buttonSpacing;
            boolean deleteButtonHovered = isMouseOver(mouseX, mouseY, deleteButtonX, buttonY, deleteButtonWidth, buttonHeight);
            int deleteButtonColor = deleteButtonHovered ? new Color(255, 100, 100).getRGB() : ERROR_COLOR;
            context.fill(deleteButtonX, buttonY, deleteButtonX + deleteButtonWidth, buttonY + buttonHeight, deleteButtonColor);
            drawBorder(context, deleteButtonX, buttonY, deleteButtonWidth, buttonHeight);
            context.drawCenteredTextWithShadow(textRenderer, "Delete", deleteButtonX + deleteButtonWidth / 2, buttonY + 5, Color.WHITE.getRGB());
        }

        if (totalContentHeight > listHeight) {
            float scrollRatio = (float) listHeight / totalContentHeight;
            int thumbHeight = Math.max(20, (int)(listHeight * scrollRatio));
            int thumbY = listY + (int)((listHeight - thumbHeight) * (macroListScroll / Math.max(1, totalContentHeight - listHeight)));
            context.fill(listX + listWidth - 15, listY, listX + listWidth - 10, listY + listHeight, applyTransparency(new Color(50, 50, 55).getRGB(), trans));
            context.fill(listX + listWidth - 15, thumbY, listX + listWidth - 10, thumbY + thumbHeight, ACCENT);
        }

        context.disableScissor();
    }

    private void renderActionsTab(DrawContext context, PanelBounds panel, int mouseX, int mouseY, float trans) {
        int contentY = panel.y + 50;
        int contentHeight = panel.height - 100;

        if (selectedMacro == null) {
            context.drawCenteredTextWithShadow(textRenderer, "No macro selected",
                    panel.x + panel.width / 2, contentY + contentHeight / 2, TEXT);
            return;
        }

        context.drawTextWithShadow(textRenderer, "Actions in: " + selectedMacro.name, panel.x + 20, contentY, ACCENT);

        int listX = panel.x + 20;
        int listY = contentY + 20;
        int listWidth = panel.width - 40;
        int listHeight = contentHeight - 20;

        context.enableScissor(listX, listY, listX + listWidth, listY + listHeight);

        int actionHeight = 40;
        int actionSpacing = 2;
        int totalContentHeight = selectedMacroActions.size() * (actionHeight + actionSpacing) - actionSpacing;

        actionsScroll = MathHelper.clamp(actionsScroll, 0, Math.max(0, totalContentHeight - listHeight));

        for (int i = 0; i < selectedMacroActions.size(); i++) {
            MacroRecordingSystem.MacroAction action = selectedMacroActions.get(i);
            int actionY = listY + i * (actionHeight + actionSpacing) - (int)actionsScroll;

            if (actionY + actionHeight < listY || actionY > listY + listHeight) continue;

            boolean isHovered = isMouseOver(mouseX, mouseY, listX, actionY, listWidth - 20, actionHeight);

            int bgColor = isHovered ? applyTransparency(new Color(50, 50, 55, 220).getRGB(), trans) :
                    applyTransparency(new Color(40, 40, 45, 200).getRGB(), trans);

            context.fill(listX, actionY, listX + listWidth - 20, actionY + actionHeight, bgColor);
            drawBorder(context, listX, actionY, listWidth - 20, actionHeight);

            String actionType = getActionTypeString(action);
            context.drawTextWithShadow(textRenderer, "#" + (i + 1) + " " + actionType,
                    listX + 10, actionY + 8, ACCENT);

            String actionDetails = getActionDetails(action);
            context.drawTextWithShadow(textRenderer, actionDetails,
                    listX + 10, actionY + 22, new Color(180, 180, 180).getRGB());
        }

        // Scrollbar
        if (totalContentHeight > listHeight) {
            float scrollRatio = (float) listHeight / totalContentHeight;
            int thumbHeight = Math.max(20, (int)(listHeight * scrollRatio));
            int thumbY = listY + (int)((listHeight - thumbHeight) * (actionsScroll / Math.max(1, totalContentHeight - listHeight)));
            context.fill(listX + listWidth - 15, listY, listX + listWidth - 10, listY + listHeight,
                    applyTransparency(new Color(50, 50, 55).getRGB(), trans));
            context.fill(listX + listWidth - 15, thumbY, listX + listWidth - 10, thumbY + thumbHeight, ACCENT);
        }

        context.disableScissor();
    }

    private void drawBorder(DrawContext context, int x, int y, int w, int h) {
        context.drawHorizontalLine(x, x + w, y, OUTLINE);
        context.drawHorizontalLine(x, x + w, y + h, OUTLINE);
        context.drawVerticalLine(x, y, y + h, OUTLINE);
        context.drawVerticalLine(x + w, y, y + h, OUTLINE);
    }

    private boolean isMouseOver(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void setStatusMessage(String message, int color) {
        long statusMessageTime = System.currentTimeMillis();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (animProgress < 1.0f) return false;

        PanelBounds panel = calcMainPanel();

        String[] tabs = {"Recorder", "Saved Macros", "Actions"};
        int tabWidth = 120;
        int tabHeight = 30;
        int startX = panel.x + 10;
        int tabY = panel.y + 10;

        for (int i = 0; i < tabs.length; i++) {
            int tabX = startX + i * (tabWidth + 5);
            if (isMouseOver((int)mx, (int)my, tabX, tabY, tabWidth, tabHeight)) {
                selectedTab = i;
                return true;
            }
        }

        if (selectedTab == 0) {
            double adjustedMy = my + recorderScroll;
            return handleRecorderTabClick(mx, adjustedMy, button, panel);
        } else if (selectedTab == 1) {
            return handleSavedMacrosTabClick(mx, my, button, panel);
        } else if (selectedTab == 2) {
            return handleActionsTabClick(mx, my, button, panel);
        }

        return false;
    }

    private boolean handleRecorderTabClick(double mx, double my, int button, PanelBounds panel) {
        int contentY = panel.y + 50;

        int nameFieldX = panel.x + 80;
        int nameFieldY = contentY + 25;
        int nameFieldW = 200;
        int nameFieldH = 20;
        if (isMouseOver((int)mx, (int)my, nameFieldX, nameFieldY, nameFieldW, nameFieldH)) {
            editingName = true;
            nameBuffer = currentMacroName;
            cursorPosition = nameBuffer.length();
            return true;
        } else if (editingName) {
            editingName = false;
            currentMacroName = nameBuffer;
            nameBuffer = "";
            cursorPosition = 0;
        }

        int buttonWidth = 100;
        int buttonHeight = 25;
        int buttonSpacing = 10;
        int buttonsStartX = panel.x + 40;
        int buttonsY = contentY + 165;

        if (isMouseOver((int)mx, (int)my, buttonsStartX, buttonsY, buttonWidth, buttonHeight)) {
            toggleRecording();
            return true;
        }

        int playButtonX = buttonsStartX + buttonWidth + buttonSpacing;
        if (isMouseOver((int)mx, (int)my, playButtonX, buttonsY, buttonWidth, buttonHeight) && !isRecording && !isPlaying) {
            playMacro();
            return true;
        }

        int saveButtonX = playButtonX + buttonWidth + buttonSpacing;
        if (isMouseOver((int)mx, (int)my, saveButtonX, buttonsY, buttonWidth, buttonHeight) && !isRecording && !isPlaying) {
            saveMacro();
            return true;
        }

        int resetButtonX = saveButtonX + buttonWidth + buttonSpacing;
        if (isMouseOver((int)mx, (int)my, resetButtonX, buttonsY, buttonWidth, buttonHeight) && !isRecording && !isPlaying) {
            resetMacro();
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    private boolean handleSavedMacrosTabClick(double mx, double my, int button, PanelBounds panel) {
        int contentY = panel.y + 50;
        int listX = panel.x + 20;
        int listY = contentY + 20;
        int listWidth = panel.width - 15;
        int listHeight = panel.height - 120;

        int macroHeight = 50;
        int macroSpacing = 5;

        for (int i = 0; i < savedMacros.size(); i++) {
            MacrosManager.SavedMacro macro = savedMacros.get(i);
            int macroY = listY + i * (macroHeight + macroSpacing) - (int)macroListScroll;

            if (macroY + macroHeight < listY || macroY > listY + listHeight) continue;

            if (isMouseOver((int)mx, (int)my, listX, macroY, listWidth - 20, macroHeight)) {
                selectedMacro = macro;

                try {
                    selectedMacroActions = persistenceManager.loadMacroActions(macro.id);
                } catch (Exception e) {
                    selectedMacroActions.clear();
                    setStatusMessage("Failed to load macro actions: " + e.getMessage(), ERROR_COLOR);
                }

                int buttonY = macroY + 10;
                int buttonHeight = 20;
                int buttonSpacing = 8;

                int playButtonWidth = 50;
                int editButtonWidth = 50;
                int viewActionsButtonWidth = 85;
                int deleteButtonWidth = 60;

                int totalButtonsWidth = playButtonWidth + editButtonWidth + viewActionsButtonWidth + deleteButtonWidth + (3 * buttonSpacing);
                int availableWidth = listWidth - 20;
                int buttonsStartX = listX + availableWidth - totalButtonsWidth - 10;

                // Play button
                if (isMouseOver((int)mx, (int)my, buttonsStartX, buttonY, playButtonWidth, buttonHeight)) {
                    playSavedMacro(macro);
                    return true;
                }

                // Edit button
                int editButtonX = buttonsStartX + playButtonWidth + buttonSpacing;
                if (isMouseOver((int)mx, (int)my, editButtonX, buttonY, editButtonWidth, buttonHeight)) {
                    editSavedMacro(macro);
                    return true;
                }

                // View Actions button
                int viewActionsButtonX = editButtonX + editButtonWidth + buttonSpacing;
                if (isMouseOver((int)mx, (int)my, viewActionsButtonX, buttonY, viewActionsButtonWidth, buttonHeight)) {
                    viewMacroActions(macro);
                    return true;
                }

                // Delete button
                int deleteButtonX = viewActionsButtonX + viewActionsButtonWidth + buttonSpacing;
                if (isMouseOver((int)mx, (int)my, deleteButtonX, buttonY, deleteButtonWidth, buttonHeight)) {
                    deleteSavedMacro(macro);
                    return true;
                }

                return true;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    private void viewMacroActions(MacrosManager.SavedMacro macro) {
        try {
            selectedMacro = macro;
            selectedMacroActions = persistenceManager.loadMacroActions(macro.id);
            selectedTab = 2;
            actionsScroll = 0;
            setStatusMessage("Viewing actions for: " + macro.name, ACCENT);
        } catch (Exception e) {
            selectedMacroActions.clear();
            setStatusMessage("Failed to load macro actions: " + e.getMessage(), ERROR_COLOR);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        PanelBounds panel = calcMainPanel();
        if (selectedTab == 0) {
            int contentHeight = panel.height - 100;
            int totalContentHeight = 390;
            recorderScroll = (float) MathHelper.clamp(recorderScroll - scrollY * 15, 0, Math.max(0, totalContentHeight - contentHeight));
        } else if (selectedTab == 1) {
            int listHeight = panel.height - 120;
            int totalContentHeight = savedMacros.size() * 55 - 5;
            macroListScroll = (float) MathHelper.clamp(macroListScroll - scrollY * 15, 0, Math.max(0, totalContentHeight - listHeight));
        } else if (selectedTab == 2) {
            int listHeight = panel.height - 120;
            int totalContentHeight = selectedMacroActions.size() * 42 - 2;
            actionsScroll = (float) MathHelper.clamp(actionsScroll - scrollY * 15, 0, Math.max(0, totalContentHeight - listHeight));
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int mods) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (editingName) {
                editingName = false;
                nameBuffer = "";
                cursorPosition = 0;
                return true;
            }
            close();
            return true;
        }

        if (editingName) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                editingName = false;
                currentMacroName = nameBuffer.trim().isEmpty() ? "New Macro" : nameBuffer.trim();
                nameBuffer = "";
                cursorPosition = 0;
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (cursorPosition > 0) {
                    nameBuffer = nameBuffer.substring(0, cursorPosition - 1) + nameBuffer.substring(cursorPosition);
                    cursorPosition--;
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                if (cursorPosition < nameBuffer.length()) {
                    nameBuffer = nameBuffer.substring(0, cursorPosition) + nameBuffer.substring(cursorPosition + 1);
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                if (cursorPosition > 0) {
                    cursorPosition--;
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                if (cursorPosition < nameBuffer.length()) {
                    cursorPosition++;
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_HOME) {
                cursorPosition = 0;
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_END) {
                cursorPosition = nameBuffer.length();
                return true;
            }

            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_R) toggleRecording();
        if (keyCode == GLFW.GLFW_KEY_P && !isRecording && !isPlaying) playMacro();
        if (keyCode == GLFW.GLFW_KEY_S && !isRecording && !isPlaying) saveMacro();
        return super.keyPressed(keyCode, scanCode, mods);
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        if (editingName) {
            if (chr >= 32 && chr != 127 && chr != '/' && chr != '\\' && chr != ':' && chr != '*' && chr != '?' && chr != '"' && chr != '<' && chr != '>' && chr != '|') {
                if (nameBuffer.length() < 50) {
                    nameBuffer = nameBuffer.substring(0, cursorPosition) + chr + nameBuffer.substring(cursorPosition);
                    cursorPosition++;
                }
            }
            return true;
        }
        return super.charTyped(chr, keyCode);
    }

    @Override
    public void renderBackground(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);
    }

    private void toggleRecording() {
        if (recordingSystem.isRecording()) {
            recordingSystem.stopRecording();
        } else {
            recordingSystem.startRecording();
        }
    }

    private void playMacro() {
        List<MacroRecordingSystem.MacroAction> actions = recordingSystem.getRecordedActions();
        if (actions.isEmpty()) {
            setStatusMessage("No actions to play", ERROR_COLOR);
            return;
        }

        close();

        MacroPlaybackSystem.getInstance().playMacro(actions);
    }

    private void saveMacro() {
        try {
            List<MacroRecordingSystem.MacroAction> actions = recordingSystem.getRecordedActions();

            if (actions.isEmpty()) {
                setStatusMessage("No actions to save", ERROR_COLOR);
                return;
            }

            persistenceManager.saveMacro(currentMacroName, actions);

            initSavedMacros();

            recordingSystem.clearRecording();

            setStatusMessage("Saved macro: " + currentMacroName, SUCCESS_COLOR);

        } catch (Exception e) {
            setStatusMessage("Failed to save macro: " + e.getMessage(), ERROR_COLOR);
        }
    }

    private void resetMacro() {
        currentMacroName = "New Macro";
        recordingSystem.clearRecording();
        setStatusMessage("Macro reinitialized", SUCCESS_COLOR);
    }

    private void playSavedMacro(MacrosManager.SavedMacro macro) {
        try {
            List<MacroRecordingSystem.MacroAction> actions = persistenceManager.loadMacroActions(macro.id);
            if (actions.isEmpty()) {
                setStatusMessage("No actions found for macro: " + macro.name, ERROR_COLOR);
                return;
            }

            close();

            new Thread(() -> {
                try {
                    Thread.sleep(75);
                    MacroPlaybackSystem.getInstance().playMacro(actions);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

        } catch (Exception e) {
            setStatusMessage("Failed to play macro: " + e.getMessage(), ERROR_COLOR);
        }
    }

    private void editSavedMacro(MacrosManager.SavedMacro macro) {
        try {
            List<MacroRecordingSystem.MacroAction> actions = persistenceManager.loadMacroActions(macro.id);

            recordingSystem.loadActions(actions);

            currentMacroName = macro.name;
            selectedTab = 0;
            setStatusMessage("Editing " + macro.name, ACCENT);

        } catch (Exception e) {
            setStatusMessage("Failed to edit macro: " + e.getMessage(), ERROR_COLOR);
        }
    }

    private void deleteSavedMacro(MacrosManager.SavedMacro macro) {
        try {
            persistenceManager.deleteMacro(macro.id);

            initSavedMacros();

            if (selectedMacro == macro) {
                selectedMacro = null;
            }

            setStatusMessage("Deleted macro: " + macro.name, SUCCESS_COLOR);

        } catch (Exception e) {
            setStatusMessage("Failed to delete macro: " + e.getMessage(), ERROR_COLOR);
        }
    }

    private String getActionTypeString(MacroRecordingSystem.MacroAction action) {
        return switch (action.getType()) {
            case MOUSE_PRESS, MOUSE_RELEASE -> "Mouse Click";
            case MOUSE_MOVE -> "Mouse Move";
            case MOUSE_SCROLL -> "Mouse Scroll";
            case KEY_PRESS, KEY_RELEASE -> "Key Press";
            case KEYBINDING_PRESS -> "Keybinding";
            case BLOCK_INTERACT -> "Block Interact";
            case ENTITY_INTERACT -> "Entity Interact";
            case CHAT_MESSAGE -> "Chat Message";
            case INVENTORY_ACTION -> "Inventory Action";
            default -> "Unknown";
        };
    }

    private String getActionDetails(MacroRecordingSystem.MacroAction action) {
        switch (action.getType()) {
            case KEY_PRESS:
            case KEY_RELEASE:
                if (action.getData() instanceof Number) {
                    return "Key: " + getKeyName(((Number) action.getData()).intValue()) + " | " + formatTimestamp(action.getTimestamp());
                }
                return "Key: " + action.getData() + " | " + formatTimestamp(action.getTimestamp());
            case KEYBINDING_PRESS:
                return "Keybinding: " + action.getData() + " | " + formatTimestamp(action.getTimestamp());
            case MOUSE_PRESS:
            case MOUSE_RELEASE:
                if (action.getData() instanceof Number) {
                    return "Button: " + getMouseButtonName(((Number) action.getData()).intValue()) + " | " + formatTimestamp(action.getTimestamp());
                }
                return "Button: " + action.getData() + " | " + formatTimestamp(action.getTimestamp());
            case MOUSE_MOVE:
                return "Position: " + action.getData() + " | " + formatTimestamp(action.getTimestamp());
            case MOUSE_SCROLL:
                return "Scroll: " + action.getData() + " | " + formatTimestamp(action.getTimestamp());
            case BLOCK_INTERACT:
                return "Block: " + action.getData() + " | " + formatTimestamp(action.getTimestamp());
            case ENTITY_INTERACT:
                return "Entity: " + action.getData() + " | " + formatTimestamp(action.getTimestamp());
            case CHAT_MESSAGE:
                return "Message: " + action.getData() + " | " + formatTimestamp(action.getTimestamp());
            case INVENTORY_ACTION:
                return "Inventory: " + action.getData() + " | " + formatTimestamp(action.getTimestamp());
            default:
                return "Unknown action | " + formatTimestamp(action.getTimestamp());
        }
    }

    private String getKeyName(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_A -> "A";
            case GLFW.GLFW_KEY_B -> "B";
            case GLFW.GLFW_KEY_C -> "C";
            case GLFW.GLFW_KEY_D -> "D";
            case GLFW.GLFW_KEY_E -> "E";
            case GLFW.GLFW_KEY_F -> "F";
            case GLFW.GLFW_KEY_G -> "G";
            case GLFW.GLFW_KEY_H -> "H";
            case GLFW.GLFW_KEY_I -> "I";
            case GLFW.GLFW_KEY_J -> "J";
            case GLFW.GLFW_KEY_K -> "K";
            case GLFW.GLFW_KEY_L -> "L";
            case GLFW.GLFW_KEY_M -> "M";
            case GLFW.GLFW_KEY_N -> "N";
            case GLFW.GLFW_KEY_O -> "O";
            case GLFW.GLFW_KEY_P -> "P";
            case GLFW.GLFW_KEY_Q -> "Q";
            case GLFW.GLFW_KEY_R -> "R";
            case GLFW.GLFW_KEY_S -> "S";
            case GLFW.GLFW_KEY_T -> "T";
            case GLFW.GLFW_KEY_U -> "U";
            case GLFW.GLFW_KEY_V -> "V";
            case GLFW.GLFW_KEY_W -> "W";
            case GLFW.GLFW_KEY_X -> "X";
            case GLFW.GLFW_KEY_Y -> "Y";
            case GLFW.GLFW_KEY_Z -> "Z";
            case GLFW.GLFW_KEY_0 -> "0";
            case GLFW.GLFW_KEY_1 -> "1";
            case GLFW.GLFW_KEY_2 -> "2";
            case GLFW.GLFW_KEY_3 -> "3";
            case GLFW.GLFW_KEY_4 -> "4";
            case GLFW.GLFW_KEY_5 -> "5";
            case GLFW.GLFW_KEY_6 -> "6";
            case GLFW.GLFW_KEY_7 -> "7";
            case GLFW.GLFW_KEY_8 -> "8";
            case GLFW.GLFW_KEY_9 -> "9";
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_ENTER -> "Enter";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_BACKSPACE -> "Backspace";
            case GLFW.GLFW_KEY_DELETE -> "Delete";
            case GLFW.GLFW_KEY_ESCAPE -> "Escape";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "Left Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "Right Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "Left Ctrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "Right Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT -> "Left Alt";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "Right Alt";
            case GLFW.GLFW_KEY_UP -> "Up Arrow";
            case GLFW.GLFW_KEY_DOWN -> "Down Arrow";
            case GLFW.GLFW_KEY_LEFT -> "Left Arrow";
            case GLFW.GLFW_KEY_RIGHT -> "Right Arrow";
            default -> "Key " + keyCode;
        };
    }

    private String getMouseButtonName(int button) {
        return switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "Left Click";
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "Right Click";
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "Middle Click";
            default -> "Button " + button;
        };
    }

    private String formatTimestamp(long timestamp) {
        return String.format("%.2fs", timestamp / 1000.0);
    }

    private boolean handleActionsTabClick(double mx, double my, int button, PanelBounds panel) {
        if (selectedMacro == null) {
            return super.mouseClicked(mx, my, button);
        }

        int contentY = panel.y + 50;
        int listX = panel.x + 20;
        int listY = contentY + 20;
        int listWidth = panel.width - 40;
        int listHeight = panel.height - 120;

        int actionHeight = 40;
        int actionSpacing = 2;
        int totalContentHeight = selectedMacroActions.size() * (actionHeight + actionSpacing) - actionSpacing;

        if (totalContentHeight > listHeight) {
            float scrollRatio = (float) listHeight / totalContentHeight;
            int thumbHeight = Math.max(20, (int)(listHeight * scrollRatio));
            int thumbY = listY + (int)((listHeight - thumbHeight) * (actionsScroll / Math.max(1, totalContentHeight - listHeight)));

            int scrollbarX = listX + listWidth - 15;
            int scrollbarWidth = 5;

            if (isMouseOver((int)mx, (int)my, scrollbarX, thumbY, scrollbarWidth, thumbHeight)) {
                actionsScrollState.isDragging = true;
                actionsScrollState.dragStartY = (int)my;
                actionsScrollState.dragStartOffset = actionsScroll;
                return true;
            }

            if (isMouseOver((int)mx, (int)my, scrollbarX, listY, scrollbarWidth, listHeight)) {
                float clickRatio = (float)((my - listY) / listHeight);
                float maxScroll = Math.max(0, totalContentHeight - listHeight);
                actionsScroll = MathHelper.clamp(clickRatio * maxScroll, 0, maxScroll);
                return true;
            }
        }

        for (int i = 0; i < selectedMacroActions.size(); i++) {
            MacroRecordingSystem.MacroAction action = selectedMacroActions.get(i);
            int actionY = listY + i * (actionHeight + actionSpacing) - (int)actionsScroll;

            if (actionY + actionHeight < listY || actionY > listY + listHeight) continue;

            if (isMouseOver((int)mx, (int)my, listX, actionY, listWidth - 20, actionHeight)) {
                return true;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double deltaX, double deltaY) {
        if (actionsScrollState.isDragging && selectedMacro != null) {
            PanelBounds panel = calcMainPanel();
            int contentY = panel.y + 50;
            int listY = contentY + 20;
            int listHeight = panel.height - 120;

            int actionHeight = 40;
            int actionSpacing = 2;
            int totalContentHeight = selectedMacroActions.size() * (actionHeight + actionSpacing) - actionSpacing;

            if (totalContentHeight > listHeight) {
                int dragDistance = (int)my - actionsScrollState.dragStartY;
                float scrollRatio = (float) listHeight / totalContentHeight;
                float maxScroll = Math.max(0, totalContentHeight - listHeight);

                float scrollDelta = dragDistance / scrollRatio;
                actionsScroll = MathHelper.clamp(actionsScrollState.dragStartOffset + scrollDelta, 0, maxScroll);
            }

            return true;
        }

        return super.mouseDragged(mx, my, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (actionsScrollState.isDragging) {
            actionsScrollState.isDragging = false;
            return true;
        }

        return super.mouseReleased(mx, my, button);
    }

    private void cleanupOrphanedFiles() {
        try {
            persistenceManager.cleanupOrphanedFiles();
        } catch (Exception ignored) { }
    }

    @Override
    public void close() {
        if (recordingListener != null) {
            recordingSystem.removeListener(recordingListener);
        }

        cleanupOrphanedFiles();

        super.close();
    }
}