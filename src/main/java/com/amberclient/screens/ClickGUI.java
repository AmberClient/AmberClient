package com.amberclient.screens;

import com.amberclient.AmberClient;
import com.amberclient.modules.miscellaneous.Transparency;
import com.amberclient.modules.miscellaneous.NoAnimations;
import com.amberclient.utils.module.Module;
import com.amberclient.utils.module.ModuleManager;
import com.amberclient.utils.module.ConfigurableModule;
import com.amberclient.utils.module.ModuleSettings;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.*;

public class ClickGUI extends Screen {
    // Theme colors
    private static final int BASE_BG = new Color(20, 20, 25, 200).getRGB(), PANEL_BG = new Color(30, 30, 35, 255).getRGB();
    private static final int ACCENT = new Color(255, 165, 0).getRGB(), ACCENT_HOVER = new Color(255, 190, 50).getRGB();
    private static final int TEXT = new Color(255, 255, 255, 255).getRGB(), OUTLINE = new Color(255, 255, 255, 180).getRGB();

    // State
    private float animProgress = 0.0f, configAnim = 0.0f, mainScroll = 0.0f, configScroll = 0.0f;
    private long lastTime = System.currentTimeMillis();
    private final List<Category> categories = new ArrayList<>();
    private int selectedCat = 0;
    private final ScrollState main = new ScrollState();
    private final ScrollState config = new ScrollState();
    private ModuleWrapper configModule = null;
    private int configOffsetX = -350, configOffsetY = 0;
    private boolean configDragging = false;
    private int configDragX, configDragY;
    private ModuleSettings draggedSetting = null;
    private final List<ModuleWrapper> clickedModules = new ArrayList<>();
    private long clickTime = 0;
    private static final long CLICK_DURATION = 300;

    private ModuleSettings openColorPicker = null;

    private ModuleSettings openDropdown = null;

    public ClickGUI() {
        super(Text.literal("Amber Client - by @gqdThinky"));
        initCategories();
    }

    private static class ScrollState {
        boolean isDragging = false;
        int dragStartY;
        float dragStartOffset;
    }

    private record PanelBounds(int x, int y, int width, int height) { }

    private void initCategories() {
        Map<String, List<Module>> catMap = new HashMap<>();
        ModuleManager.getInstance().getModules().forEach(m -> catMap.computeIfAbsent(String.valueOf(m.getCategory()), k -> new ArrayList<>()).add(m));

        catMap.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("Miscellaneous"))
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> categories.add(new Category(entry.getKey(), entry.getValue().stream().map(ModuleWrapper::new).toList())));

        List<Module> hudModules = catMap.get("Miscellaneous");
        if (hudModules != null && !hudModules.isEmpty()) {
            List<ModuleWrapper> wrappedHudModules = hudModules.stream().map(ModuleWrapper::new).toList();
            categories.add(new Category("Miscellaneous", wrappedHudModules));
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
                .dimensions(width - 25, 5, 20, 20).tooltip(Tooltip.of(Text.literal("Close"))).build());
    }

    @Override
    public void render(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        long time = System.currentTimeMillis();
        boolean animationsDisabled = isAnimationsDisabled();
        animProgress = animationsDisabled ? 1.0f : MathHelper.clamp(animProgress + (time - lastTime) / 300.0f, 0.0f, 1.0f);
        configAnim = animationsDisabled ? (configModule != null ? 1.0f : 0.0f) :
                MathHelper.clamp(configAnim + (configModule != null ? 1 : -1) * (time - lastTime) / 200.0f, 0.0f, 1.0f);
        lastTime = time;

        float trans = getTransparency();
        renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, width, height, applyTransparency(BASE_BG, trans));

        super.render(context, mouseX, mouseY, delta);

        int centerX = width / 2;
        context.drawCenteredTextWithShadow(textRenderer, "AMBER CLIENT", centerX, 52, ACCENT);

        PanelBounds mainPanel = calcPanel();
        context.fill(mainPanel.x, mainPanel.y, mainPanel.x + mainPanel.width, mainPanel.y + mainPanel.height,
                applyTransparency(PANEL_BG, animProgress * trans));

        int sepX = mainPanel.x + 150;
        context.fill(sepX, mainPanel.y, sepX + 2, mainPanel.y + mainPanel.height, ACCENT);

        renderCategories(context, mainPanel.x, mainPanel.y, mainPanel.height, mouseX, mouseY, trans);
        renderModules(context, sepX + 10, mainPanel.y, mainPanel.width - 160, mainPanel.height, mouseX, mouseY, trans);

        int statusY = mainPanel.y + mainPanel.height + 5;
        context.fill(mainPanel.x, statusY, mainPanel.x + mainPanel.width, statusY + 20, applyTransparency(PANEL_BG, trans));
        context.drawText(textRenderer, configModule != null ? "Configuring: " + configModule.name :
                "Amber Client " + AmberClient.MOD_VERSION + " • MC 1.21.4", mainPanel.x + 10, statusY + 6, TEXT, false);

        super.render(context, mouseX, mouseY, delta);

        if (configAnim > 0.0f && configModule != null) renderConfigPanel(context, mouseX, mouseY);
    }

    private PanelBounds calcPanel() {
        int w = Math.min(width - 40, 800), h = Math.min(height - 100, 400);
        float scale = isAnimationsDisabled() ? 1.0f : 0.8f + 0.2f * animProgress;
        int scaledW = (int)(w * scale), scaledH = (int)(h * scale);
        return new PanelBounds(width / 2 - scaledW / 2, 82 + (h - scaledH) / 2, scaledW, scaledH);
    }

    private void renderCategories(DrawContext context, int x, int y, int h, int mouseX, int mouseY, float trans) {
        int catH = 40, sp = 5, totalH = categories.size() * (catH + sp) - sp, startY = y + (h - totalH) / 2;
        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            int catY = startY + i * (catH + sp), catX = x + 10;
            boolean hover = isMouseOver(mouseX, mouseY, catX, catY, 150 - 20, catH);
            int bg = selectedCat == i ? ACCENT : hover ? applyTransparency(new Color(50, 50, 55, 220).getRGB(), trans) :
                    applyTransparency(PANEL_BG, trans);
            context.fill(catX, catY, catX + 150 - 20, catY + catH, bg);
            context.drawCenteredTextWithShadow(textRenderer, cat.name, catX + (150 - 20) / 2, catY + (catH - 8) / 2,
                    selectedCat == i ? Color.WHITE.getRGB() : TEXT);
        }
    }

    private void renderModules(DrawContext context, int x, int y, int w, int h, int mouseX, int mouseY, float trans) {
        if (selectedCat < 0 || selectedCat >= categories.size()) return;
        Category cat = categories.get(selectedCat); // meow
        context.drawText(textRenderer, cat.name.toUpperCase(), x, y + 10, ACCENT, false);

        // Add warning for COMBAT category
        if (cat.name.equalsIgnoreCase("COMBAT")) {
            context.drawText(textRenderer, "Use at your own risks", x, y + 25, new Color(255, 0, 0).getRGB(), false);
        }

        int top = y + 30, areaH = h - 40;
        if (cat.name.equalsIgnoreCase("COMBAT")) {
            top += 15;
        }
        context.enableScissor(x, top, x + w, top + areaH);

        int modH = 30, sp = 5, contentH = cat.modules.size() * (modH + sp) - sp;
        mainScroll = MathHelper.clamp(mainScroll, 0, Math.max(0, contentH - areaH));
        for (int i = 0; i < cat.modules.size(); i++) {
            ModuleWrapper mod = cat.modules.get(i);
            int modY = top + i * (modH + sp) - (int)mainScroll;
            if (modY + modH < top || modY > top + areaH) continue;

            context.fill(x, modY, x + w, modY + modH, mod.isEnabled() ? new Color(ACCENT).darker().getRGB() :
                    applyTransparency(new Color(40, 40, 45, 220).getRGB(), trans));
            context.drawText(textRenderer, mod.name, x + 10, modY + 7, TEXT, false);
            context.drawText(textRenderer, mod.desc, x + 10, modY + 20, new Color(180, 180, 180).getRGB(), false);
            if (mod.isConfigurable) context.drawText(textRenderer, "⚙", x + w - 50, modY + 7, Color.WHITE.getRGB(), false);

            int togX = x + w - 24, togY = modY + 5;
            boolean hover = isMouseOver(mouseX, mouseY, togX, togY, 18, 18);
            boolean clicked = clickedModules.contains(mod) && (System.currentTimeMillis() - clickTime) < CLICK_DURATION;
            int color = clicked || hover ? ACCENT_HOVER : new Color(245, 235, 216).getRGB();
            drawBorder(context, togX, togY, 18, 18);
            context.fill(togX, togY, togX + 18, togY + 18, color);
            if (mod.isEnabled()) context.drawText(textRenderer, "✓", togX + 7, togY + 5, Color.WHITE.getRGB(), false);
        }

        if (contentH > areaH) {
            float ratio = (float) areaH / contentH;
            int thumbH = Math.max(20, (int)(areaH * ratio));
            int thumbY = top + (int)((areaH - thumbH) * (mainScroll / Math.max(0, contentH - areaH)));
            context.fill(x + w - 20, top, x + w - 10, top + areaH, applyTransparency(new Color(50, 50, 55).getRGB(), trans));
            context.fill(x + w - 20, thumbY, x + w - 10, thumbY + thumbH, ACCENT);
        }
        context.disableScissor();
    }

    private void renderConfigPanel(DrawContext context, int mouseX, int mouseY) {
        PanelBounds p = calcConfigPanel();

        int colorPickerX = -1;
        int colorPickerY = -1;

        context.fill(p.x, p.y, p.x + p.width, p.y + p.height, new Color(30, 30, 35, 255).getRGB());
        context.fill(p.x, p.y, p.x + p.width, p.y + 30, ACCENT);
        context.drawText(textRenderer, configModule.name + " Settings", p.x + 10, p.y + 10, Color.WHITE.getRGB(), false);

        boolean hover = isMouseOver(mouseX, mouseY, p.x + p.width - 25, p.y + 5, 20, 20);
        context.fill(p.x + p.width - 25, p.y + 5, p.x + p.width - 5, p.y + 25,
                hover ? new Color(255, 80, 80).getRGB() : new Color(200, 50, 50).getRGB());
        context.drawText(textRenderer, "×", p.x + p.width - 18, p.y + 10, Color.WHITE.getRGB(), false);

        List<ModuleSettings> settings = configModule.settings;
        int top = p.y + 40, areaH = p.height - 50;
        context.enableScissor(p.x, top, p.x + p.width, top + areaH);

        int setH = 40, sp = 5, contentH = settings.size() * (setH + sp) - sp;
        configScroll = MathHelper.clamp(configScroll, 0, Math.max(0, contentH - areaH));
        for (int i = 0; i < settings.size(); i++) {
            ModuleSettings s = settings.get(i);
            int setY = top + i * (setH + sp) - (int)configScroll;
            if (setY + setH < top || setY > top + areaH) continue;

            context.fill(p.x + 10, setY, p.x + p.width - 10, setY + setH, new Color(40, 40, 45, 255).getRGB());
            context.drawText(textRenderer, s.getName(), p.x + 20, setY + 10, TEXT, false);
            context.drawText(textRenderer, s.getDescription(), p.x + 20, setY + 25, new Color(180, 180, 180).getRGB(), false);

            if (s.getType() == ModuleSettings.SettingType.BOOLEAN) {
                boolean on = s.getBooleanValue(), hoverB = isMouseOver(mouseX, mouseY, p.x + p.width - 60, setY + 10, 40, 20);
                int bg = hoverB ? (on ? ACCENT_HOVER : new Color(120, 120, 120).getRGB()) : (on ? ACCENT : new Color(100, 100, 100).getRGB());
                context.fill(p.x + p.width - 60, setY + 10, p.x + p.width - 20, setY + 30, bg);
                drawBorder(context, p.x + p.width - 60, setY + 10, 40, 20);
                context.drawText(textRenderer, on ? "ON" : "OFF", p.x + p.width - 50, setY + 15, Color.WHITE.getRGB(), false);
            } else if (s.getType() == ModuleSettings.SettingType.DOUBLE && s.hasRange()) {
                double v = s.getDoubleValue(), min = s.getMinValue().doubleValue(), max = s.getMaxValue().doubleValue();
                context.fill(p.x + p.width - 100, setY + 10, p.x + p.width - 20, setY + 20, new Color(100, 100, 100).getRGB());
                context.fill(p.x + p.width - 100, setY + 10, p.x + p.width - 100 + (int)(80 * (v - min) / (max - min)), setY + 20, ACCENT);
                drawBorder(context, p.x + p.width - 100, setY + 10, 80, 10);
                String valueText = String.format("%.2f", v);
                int textWidth = textRenderer.getWidth(valueText);
                int textX = Math.max(p.x + 10, p.x + p.width - 100 - textWidth - 5);
                context.drawText(textRenderer, valueText, textX, setY + 11, TEXT, false);
            } else if (s.getType() == ModuleSettings.SettingType.ENUM) {
                Enum<?> currentValue = s.getEnumValue();
                String displayText = currentValue.name();
                int buttonX = p.x + p.width - 100;
                int buttonY = setY + 10;
                int buttonWidth = 80;
                int buttonHeight = 20;

                boolean isMouseOverButton = isMouseOver(mouseX, mouseY, buttonX, buttonY, buttonWidth, buttonHeight);

                context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight,
                        isMouseOverButton ? new Color(120, 120, 120).getRGB() : new Color(100, 100, 100).getRGB());
                context.drawText(textRenderer, displayText, buttonX + 5, buttonY + 5, Color.WHITE.getRGB(), false);
                context.drawText(textRenderer, "▼", buttonX + buttonWidth - 15, buttonY + 5, Color.WHITE.getRGB(), false);

                if (openDropdown == s) {
                    Enum<?>[] enumValues = currentValue.getDeclaringClass().getEnumConstants();
                    int optionY = buttonY + buttonHeight;
                    for (Enum<?> enumValue : enumValues) {
                        boolean optionHover = isMouseOver(mouseX, mouseY, buttonX, optionY, buttonWidth, 20);
                        int optionColor = enumValue == currentValue ? ACCENT :
                                (optionHover ? new Color(120, 120, 120).getRGB() : new Color(80, 80, 80).getRGB());
                        context.fill(buttonX, optionY, buttonX + buttonWidth, optionY + 20, optionColor);
                        drawBorder(context, buttonX, optionY, buttonWidth, 20);
                        context.drawText(textRenderer, enumValue.name(), buttonX + 5, optionY + 5, Color.WHITE.getRGB(), false);
                        optionY += 20;
                    }
                }
            } else if (s.getType() == ModuleSettings.SettingType.COLOR) {
                Color currentColor = s.getColorValue();
                int buttonX = p.x + p.width - 100;
                int buttonY = setY + 10;
                int buttonWidth = 80;
                int buttonHeight = 20;

                boolean isMouseOverButton = isMouseOver(mouseX, mouseY, buttonX, buttonY, buttonWidth, buttonHeight);

                // Afficher la couleur actuelle
                context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, currentColor.getRGB());
                drawBorder(context, buttonX, buttonY, buttonWidth, buttonHeight);

                // Texte avec couleur contrastante
                int textColor = (currentColor.getRed() + currentColor.getGreen() + currentColor.getBlue()) > 400 ?
                        Color.BLACK.getRGB() : Color.WHITE.getRGB();
                context.drawText(textRenderer, "Color", buttonX + 5, buttonY + 5, textColor, false);

                // Enregistrer la position si ce paramètre est le ColorPicker actif
                if (openColorPicker == s) {
                    colorPickerX = buttonX;
                    colorPickerY = buttonY + buttonHeight;
                }
            }
        }

        if (contentH > areaH) {
            float ratio = (float) areaH / contentH;
            int thumbH = Math.max(20, (int)(areaH * ratio));
            int thumbY = top + (int)((areaH - thumbH) * (configScroll / Math.max(0, contentH - areaH)));
            context.fill(p.x + p.width - 15, top, p.x + p.width - 10, top + areaH, new Color(50, 50, 55).getRGB());
            context.fill(p.x + p.width - 15, thumbY, p.x + p.width - 10, thumbY + thumbH, ACCENT);
        }

        if (openColorPicker != null && colorPickerX != -1 && colorPickerY != -1) {
            renderColorPicker(context, colorPickerX, colorPickerY, mouseX, mouseY);
        }

        context.disableScissor();
    }

    private void renderColorPicker(DrawContext context, int x, int y, int mouseX, int mouseY) {
        int pickerWidth = 200;
        int pickerHeight = 150;

        // Calculer la position pour que le picker reste dans les limites du panel
        PanelBounds configPanel = calcConfigPanel();
        int pickerX = x;
        int pickerY = y;

        // Ajuster X si le picker dépasse à droite
        if (pickerX + pickerWidth > configPanel.x + configPanel.width - 10) {
            pickerX = configPanel.x + configPanel.width - pickerWidth - 10;
        }

        // Ajuster Y si le picker dépasse en bas, le placer au-dessus du bouton
        if (pickerY + pickerHeight > configPanel.y + configPanel.height - 10) {
            pickerY = y - pickerHeight - 5; // Au-dessus du bouton
        }

        // S'assurer que le picker ne dépasse pas en haut
        if (pickerY < configPanel.y + 40) {
            pickerY = configPanel.y + 40;
        }

        // Fond du picker (SUPPRESSION DU DOUBLE RENDU)
        context.fill(pickerX, pickerY, pickerX + pickerWidth, pickerY + pickerHeight, new Color(40, 40, 45, 255).getRGB());
        drawBorder(context, pickerX, pickerY, pickerWidth, pickerHeight);

        // Zone de couleur principale (HSB)
        int colorAreaWidth = 150;
        int brightnessBarHeight = 100;
        int colorAreaX = pickerX + 10;
        int colorAreaY = pickerY + 10;

        // Dessiner le gradient de couleur
        for (int i = 0; i < colorAreaWidth; i++) {
            for (int j = 0; j < brightnessBarHeight; j++) {
                float hue = (float) i / colorAreaWidth;
                float saturation = 1.0f - (float) j / brightnessBarHeight;
                float brightness = 1.0f;

                int rgb = Color.HSBtoRGB(hue, saturation, brightness);
                context.fill(colorAreaX + i, colorAreaY + j, colorAreaX + i + 1, colorAreaY + j + 1, rgb);
            }
        }
        drawBorder(context, colorAreaX, colorAreaY, colorAreaWidth, brightnessBarHeight);

        int brightnessBarX = pickerX + colorAreaWidth + 20;
        int brightnessBarY = pickerY + 10;
        int brightnessBarWidth = 20;

        for (int j = 0; j < brightnessBarHeight; j++) {
            float brightness = 1.0f - (float) j / brightnessBarHeight;
            int rgb = Color.HSBtoRGB(0.0f, 0.0f, brightness);
            context.fill(brightnessBarX, brightnessBarY + j, brightnessBarX + brightnessBarWidth, brightnessBarY + j + 1, rgb);
        }
        drawBorder(context, brightnessBarX, brightnessBarY, brightnessBarWidth, brightnessBarHeight);

        int buttonY = pickerY + pickerHeight - 30;

        boolean okHover = isMouseOver(mouseX, mouseY, pickerX + 10, buttonY, 50, 20);
        context.fill(pickerX + 10, buttonY, pickerX + 60, buttonY + 20, okHover ? ACCENT_HOVER : ACCENT);
        context.drawText(textRenderer, "OK", pickerX + 30, buttonY + 5, Color.WHITE.getRGB(), false);

        boolean cancelHover = isMouseOver(mouseX, mouseY, pickerX + 70, buttonY, 50, 20);
        context.fill(pickerX + 70, buttonY, pickerX + 120, buttonY + 20,
                cancelHover ? new Color(200, 80, 80).getRGB() : new Color(150, 60, 60).getRGB());
        context.drawText(textRenderer, "Cancel", pickerX + 85, buttonY + 5, Color.WHITE.getRGB(), false);

        if (openColorPicker != null) {
            Color previewColor = openColorPicker.getColorValue();
            context.fill(pickerX + 130, buttonY, pickerX + 190, buttonY + 20, previewColor.getRGB());
            drawBorder(context, pickerX + 130, buttonY, 60, 20);
        }
    }

    private PanelBounds calcConfigPanel() {
        int w = Math.min(width - 40, 300), h = Math.min(height - 100, 400);
        int x = (width - w) / 2 + configOffsetX;
        int y = 82 + configOffsetY;

        x = MathHelper.clamp(x, 10, width - w - 10);
        y = MathHelper.clamp(y, 10, height - h - 10);

        return new PanelBounds(x, y, w, h);
    }

    private void drawBorder(DrawContext context, int x, int y, int w, int h) {
        context.fill(x - 1, y - 1, x + w + 1, y, OUTLINE);
        context.fill(x - 1, y + h, x + w + 1, y + h + 1, OUTLINE);
        context.fill(x - 1, y, x, y + h, OUTLINE);
        context.fill(x + w, y, x + w + 1, y + h, OUTLINE);
    }

    private boolean isMouseOver(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (animProgress < 1.0f) return false;
        if (configModule != null && configAnim > 0.0f) {
            PanelBounds p = calcConfigPanel();
            if (isMouseOver((int)mx, (int)my, p.x, p.y, p.width, p.height)) {
                if (isMouseOver((int)mx, (int)my, p.x + p.width - 25, p.y + 5, 20, 20)) {
                    configModule = null;
                    configScroll = 0.0f;
                    draggedSetting = null;
                    openDropdown = null;
                    return true;
                }
                if (isMouseOver((int)mx, (int)my, p.x, p.y, p.width, 30)) {
                    configDragging = true; configDragX = (int)mx; configDragY = (int)my;
                    return true;
                }
                List<ModuleSettings> settings = configModule.settings;
                int top = p.y + 40, setH = 40, sp = 5;
                for (int i = 0; i < settings.size(); i++) {
                    ModuleSettings s = settings.get(i);
                    int setY = top + i * (setH + sp) - (int)configScroll;
                    if (setY + setH < top || setY > top + p.height - 50) continue;
                    if (s.getType() == ModuleSettings.SettingType.BOOLEAN &&
                            isMouseOver((int)mx, (int)my, p.x + p.width - 60, setY + 10, 40, 20)) {
                        s.setBooleanValue(!s.getBooleanValue());
                        ((ConfigurableModule)configModule.module).onSettingChanged(s);
                        return true;
                    } else if (s.getType() == ModuleSettings.SettingType.DOUBLE && s.hasRange() &&
                            isMouseOver((int)mx, (int)my, p.x + p.width - 100, setY + 10, 80, 10)) {
                        draggedSetting = s;
                        double min = s.getMinValue().doubleValue(), max = s.getMaxValue().doubleValue();
                        double normalized = (mx - (p.x + p.width - 100)) / 80.0;
                        double rawValue = min + (max - min) * normalized;
                        rawValue = MathHelper.clamp(rawValue, min, max);
                        s.setDoubleValue(rawValue);
                        ((ConfigurableModule)configModule.module).onSettingChanged(s);
                        return true;
                    } else if (s.getType() == ModuleSettings.SettingType.ENUM) {
                        int buttonX = p.x + p.width - 100;
                        int buttonY = setY + 10;
                        int buttonWidth = 80;
                        int buttonHeight = 20;

                        if (isMouseOver((int)mx, (int)my, buttonX, buttonY, buttonWidth, buttonHeight)) {
                            openDropdown = (openDropdown == s) ? null : s;
                            return true;
                        }

                        if (openDropdown == s) {
                            Enum<?> currentValue = s.getEnumValue();
                            Enum<?>[] enumValues = currentValue.getDeclaringClass().getEnumConstants();
                            int optionY = buttonY + buttonHeight;
                            for (Enum<?> enumValue : enumValues) {
                                if (isMouseOver((int)mx, (int)my, buttonX, optionY, buttonWidth, 20)) {
                                    s.setEnumValue(enumValue);
                                    ((ConfigurableModule)configModule.module).onSettingChanged(s);
                                    openDropdown = null;
                                    return true;
                                }
                                optionY += 20;
                            }
                        }
                    } else if (s.getType() == ModuleSettings.SettingType.COLOR) {
                        int buttonX = p.x + p.width - 100;
                        int buttonY = setY + 10;
                        int buttonWidth = 80;
                        int buttonHeight = 20;

                        if (isMouseOver((int)mx, (int)my, buttonX, buttonY, buttonWidth, buttonHeight)) {
                            openColorPicker = (openColorPicker == s) ? null : s;
                            return true;
                        }

                        // Gérer les clics dans le color picker
                        if (openColorPicker == s) {
                            PanelBounds configPanel = calcConfigPanel();
                            int pickerX = buttonX;
                            int pickerY = buttonY + buttonHeight;
                            int pickerWidth = 200;
                            int pickerHeight = 150;

                            // Ajuster la position (même logique que renderColorPicker)
                            if (pickerX + pickerWidth > configPanel.x + configPanel.width - 10) {
                                pickerX = configPanel.x + configPanel.width - pickerWidth - 10;
                            }
                            if (pickerY + pickerHeight > configPanel.y + configPanel.height - 10) {
                                pickerY = buttonY - pickerHeight - 5;
                            }
                            if (pickerY < configPanel.y + 40) {
                                pickerY = configPanel.y + 40;
                            }

                            // Clic dans la zone du picker
                            if (isMouseOver((int)mx, (int)my, pickerX, pickerY, pickerWidth, pickerHeight)) {
                                // Zone de couleur principale
                                int colorAreaX = pickerX + 10;
                                int colorAreaY = pickerY + 10;
                                int colorAreaWidth = 150;
                                int colorAreaHeight = 100;

                                if (isMouseOver((int)mx, (int)my, colorAreaX, colorAreaY, colorAreaWidth, colorAreaHeight)) {
                                    float hue = (float) (mx - colorAreaX) / colorAreaWidth;
                                    float saturation = 1.0f - (float) (my - colorAreaY) / colorAreaHeight;
                                    float brightness = 1.0f;

                                    hue = Math.max(0, Math.min(1, hue));
                                    saturation = Math.max(0, Math.min(1, saturation));

                                    int rgb = Color.HSBtoRGB(hue, saturation, brightness);
                                    s.setColorValue(new Color(rgb));
                                    ((ConfigurableModule)configModule.module).onSettingChanged(s);
                                    return true;
                                }

                                // Barre de luminosité
                                int brightnessBarX = pickerX + colorAreaWidth + 20;
                                int brightnessBarY = pickerY + 10;
                                int brightnessBarWidth = 20;
                                int brightnessBarHeight = colorAreaHeight;

                                if (isMouseOver((int)mx, (int)my, brightnessBarX, brightnessBarY, brightnessBarWidth, brightnessBarHeight)) {
                                    float brightness = 1.0f - (float) (my - brightnessBarY) / brightnessBarHeight;
                                    brightness = Math.max(0, Math.min(1, brightness));

                                    Color currentColor = s.getColorValue();
                                    float[] hsb = Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null);

                                    int rgb = Color.HSBtoRGB(hsb[0], hsb[1], brightness);
                                    s.setColorValue(new Color(rgb));
                                    ((ConfigurableModule)configModule.module).onSettingChanged(s);
                                    return true;
                                }

                                // Bouton OK
                                if (isMouseOver((int)mx, (int)my, pickerX + 10, pickerY + pickerHeight - 30, 50, 20)) {
                                    openColorPicker = null;
                                    return true;
                                }

                                // Bouton Cancel
                                if (isMouseOver((int)mx, (int)my, pickerX + 70, pickerY + pickerHeight - 30, 50, 20)) {
                                    openColorPicker = null;
                                    return true;
                                }

                                return true; // Consommer le clic dans le picker
                            } else {
                                // Clic à l'extérieur du picker, le fermer
                                openColorPicker = null;
                            }
                        }
                    }
                }
                if (isMouseOver((int)mx, (int)my, p.x + p.width - 15, top, 5, p.height - 50)) {
                    config.isDragging = true; config.dragStartY = (int)my; config.dragStartOffset = configScroll;
                    return true;
                }
                return true;
            }
        }
        if (super.mouseClicked(mx, my, button)) return true;

        PanelBounds p = calcPanel();
        int catH = 40, sp = 5, totalH = categories.size() * (catH + sp) - sp, startY = p.y + (p.height - totalH) / 2;
        for (int i = 0; i < categories.size(); i++) {
            if (isMouseOver((int)mx, (int)my, p.x + 10, startY + i * (catH + sp), 130, catH)) {
                selectedCat = i; mainScroll = 0;
                return true;
            }
        }
        if (selectedCat < 0 || selectedCat >= categories.size()) return false;
        List<ModuleWrapper> mods = categories.get(selectedCat).modules;
        int modX = p.x + 160, modY = p.y + 30, modW = p.width - 190, modH = 30;
        int moduleAreaWidth = p.width - 160;

        if (categories.get(selectedCat).name.equalsIgnoreCase("COMBAT")) {
            modY += 15;
        }
        for (int i = 0; i < mods.size(); i++) {
            ModuleWrapper m = mods.get(i);
            int y = modY + i * (modH + sp) - (int)mainScroll;
            if (y + modH < modY || y > modY + p.height - 40) continue;
            if (m.isConfigurable) {
                if (button == 1 && isMouseOver((int)mx, (int)my, modX, y, modW, modH)) {
                    openConfigPanel(m); return true;
                }
                if (button == 0 && isMouseOver((int)mx, (int)my, modX + moduleAreaWidth - 50, y + 7, 10, 10)) {
                    openConfigPanel(m); return true;
                }
            }
            if (isMouseOver((int)mx, (int)my, modX + moduleAreaWidth - 24, y + 5, 18, 18)) {
                clickedModules.clear(); clickedModules.add(m); clickTime = System.currentTimeMillis();
                ModuleManager.getInstance().toggleModule(m.module);
                return true;
            }
        }
        if (isMouseOver((int)mx, (int)my, modX + modW - 20, modY, 10, p.height - 40)) {
            main.isDragging = true; main.dragStartY = (int)my; main.dragStartOffset = mainScroll;
            return true;
        }
        clickedModules.clear();
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        main.isDragging = config.isDragging = configDragging = false;
        draggedSetting = null;
        clickedModules.clear();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (configDragging && configModule != null) {
            configOffsetX += (int)mx - configDragX; configOffsetY += (int)my - configDragY;
            configOffsetX = MathHelper.clamp(configOffsetX, -width + 50, width - 50);
            configOffsetY = MathHelper.clamp(configOffsetY, -Math.min(height - 100, 400) + 30, height - 30);
            configDragX = (int)mx; configDragY = (int)my;
            return true;
        }
        if (draggedSetting != null && draggedSetting.getType() == ModuleSettings.SettingType.DOUBLE) {
            PanelBounds p = calcConfigPanel();
            for (int i = 0; i < configModule.settings.size(); i++) {
                if (configModule.settings.get(i) != draggedSetting) continue;
                double min = draggedSetting.getMinValue().doubleValue(), max = draggedSetting.getMaxValue().doubleValue();
                double normalized = (mx - (p.x + p.width - 100)) / 80.0;
                double rawValue = min + (max - min) * normalized;
                rawValue = MathHelper.clamp(rawValue, min, max);
                draggedSetting.setDoubleValue(rawValue);
                ((ConfigurableModule)configModule.module).onSettingChanged(draggedSetting);
                return true;
            }
        }
        if (config.isDragging && configModule != null) {
            int h = Math.min(height - 100, 400), areaH = h - 50;
            int contentH = configModule.settings.size() * 45 - 5;
            configScroll = config.dragStartOffset + ((int)my - config.dragStartY) * (float)Math.max(0, contentH - areaH) / (areaH - 20);
            configScroll = MathHelper.clamp(configScroll, 0, Math.max(0, contentH - areaH));
            return true;
        }
        if (main.isDragging && selectedCat >= 0 && selectedCat < categories.size()) {
            PanelBounds p = calcPanel();
            int areaH = p.height - 40, contentH = categories.get(selectedCat).modules.size() * 35 - 5;
            mainScroll = main.dragStartOffset + ((int)my - main.dragStartY) * (float)Math.max(0, contentH - areaH) / (areaH - 20);
            mainScroll = MathHelper.clamp(mainScroll, 0, Math.max(0, contentH - areaH));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (configModule != null && configAnim > 0.0f) {
            PanelBounds p = calcConfigPanel();
            if (isMouseOver((int)mx, (int)my, p.x, p.y, p.width, p.height)) {
                int contentH = configModule.settings.size() * 45 - 5, areaH = p.height - 50;
                configScroll = (float) MathHelper.clamp(configScroll - scrollY * 15, 0, Math.max(0, contentH - areaH));
                return true;
            }
        }
        if (selectedCat >= 0 && selectedCat < categories.size()) {
            PanelBounds p = calcPanel();
            int contentH = categories.get(selectedCat).modules.size() * 35 - 5, areaH = p.height - 40;
            mainScroll = (float) MathHelper.clamp(mainScroll - scrollY * 15, 0, Math.max(0, contentH - areaH));
        } else mainScroll = 0;
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int mods) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (configModule != null) { configModule = null; configScroll = 0.0f; draggedSetting = null; return true; }
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, mods);
    }

    @Override
    public void close() {
        configModule = null;
        configScroll = 0.0f;
        draggedSetting = null;
        openDropdown = null;
        openColorPicker = null;
        super.close();
    }

    private void openConfigPanel(ModuleWrapper m) {
        if (m.isConfigurable) {
            configModule = m;
            configAnim = 0.0f;
            configScroll = 0.0f;
            configOffsetX = 0;
            configOffsetY = 0;
        }
    }

    private record Category(String name, List<ModuleWrapper> modules) { }

    private static class ModuleWrapper {
        final Module module;
        final String name, desc;
        final boolean isConfigurable;
        final List<ModuleSettings> settings;
        ModuleWrapper(Module m) {
            module = m; name = m.getName(); desc = m.getDescription();
            isConfigurable = m instanceof ConfigurableModule;
            settings = isConfigurable ? ((ConfigurableModule)m).getSettings() : List.of();
        }
        boolean isEnabled() { return module.isEnabled(); }
    }

    private boolean isAnimationsDisabled() {
        return ModuleManager.getInstance().getModules().stream()
                .anyMatch(m -> m instanceof NoAnimations && m.isEnabled());
    }

    @Override
    public void renderBackground(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);
    }
}
