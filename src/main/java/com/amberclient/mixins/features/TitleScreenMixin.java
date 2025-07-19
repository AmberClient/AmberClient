package com.amberclient.mixins.features;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Util;

import java.awt.Color;
import java.net.URI;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Unique private static final Identifier DISCORD_ICON = Identifier.of("amber-client", "textures/gui/discord_icon.png");
    @Unique private static final Identifier GITHUB_ICON = Identifier.of("amber-client", "textures/gui/github_icon.png");

    @Unique private static final String DISCORD_URL = "https://discord.gg/jwgSKxWqrn";
    @Unique private static final String GITHUB_URL = "https://github.com/gqdThinky/AmberClient";

    @Unique private static final int PANEL_BG = new Color(30, 30, 35, 255).getRGB();
    @Unique private static final int ACCENT = new Color(255, 165, 0).getRGB();
    @Unique private static final int ACCENT_HOVER = new Color(255, 190, 50).getRGB();
    @Unique private static final int TEXT = new Color(220, 220, 220).getRGB();
    @Unique private static final int OUTLINE = new Color(255, 255, 255, 180).getRGB();

    @Unique private static final int BUTTON_SIZE = 28;
    @Unique private static final int BUTTON_SPACING = 8;
    @Unique private static final int MARGIN_TOP = 12;
    @Unique private static final int MARGIN_RIGHT = 20;
    @Unique private static final int PADDING = 4;

    public TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        renderSocialButtons(context, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {
        if (button == 0) {
            if (handleSocialButtonClick(mouseX, mouseY)) {
                info.setReturnValue(true);
            }
        }
    }

    @Unique
    private void renderSocialButtons(DrawContext context, int mouseX, int mouseY) {
        int screenWidth = this.width;

        int discordX = screenWidth - MARGIN_RIGHT - BUTTON_SIZE;

        int githubX = discordX - BUTTON_SIZE - BUTTON_SPACING;

        renderSocialButton(context, mouseX, mouseY, discordX, DISCORD_ICON, "Discord");
        renderSocialButton(context, mouseX, mouseY, githubX, GITHUB_ICON, "GitHub");
    }

    @Unique
    private void renderSocialButton(DrawContext context, int mouseX, int mouseY, int x, Identifier icon, String tooltip) {
        boolean isHovered = isMouseOver(mouseX, mouseY, x);

        int backgroundColor = isHovered ? ACCENT_HOVER : ACCENT;
        int borderColor = isHovered ? OUTLINE : new Color(255, 255, 255, 120).getRGB();

        int buttonSize = isHovered ? BUTTON_SIZE + 1 : BUTTON_SIZE;
        int adjustedX = isHovered ? x - 1 : x;
        int adjustedY = isHovered ? TitleScreenMixin.MARGIN_TOP - 1 : TitleScreenMixin.MARGIN_TOP;

        context.fill(adjustedX + 2, adjustedY + 2, adjustedX + buttonSize + 2, adjustedY + buttonSize + 2,
                new Color(0, 0, 0, 60).getRGB());

        context.fill(adjustedX, adjustedY, adjustedX + buttonSize, adjustedY + buttonSize, backgroundColor);

        context.fill(adjustedX, adjustedY, adjustedX + buttonSize, adjustedY + 2,
                new Color(255, 255, 255, 30).getRGB());

        drawBorder(context, adjustedX, adjustedY, buttonSize, borderColor);

        int iconSize = buttonSize - (PADDING * 2);
        int iconX = adjustedX + PADDING;
        int iconY = adjustedY + PADDING;

        if (isHovered) { RenderSystem.setShaderColor(1.2f, 1.2f, 1.2f, 1.0f); }

        context.drawTexture(RenderLayer::getGuiTextured, icon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        if (isHovered) { renderTooltip(context, tooltip, x); }
    }

    @Unique
    private void drawBorder(DrawContext context, int x, int y, int size, int color) {
        context.fill(x, y, x + size, y + 1, color); // Top
        context.fill(x, y + size - 1, x + size, y + size, color); // Bottom
        context.fill(x, y, x + 1, y + size, color); // Left
        context.fill(x + size - 1, y, x + size, y + size, color); // Right
    }

    @Unique
    private void renderTooltip(DrawContext context, String text, int buttonX) {
        int tooltipWidth = MinecraftClient.getInstance().textRenderer.getWidth(text) + 8;
        int tooltipHeight = 16;

        int tooltipX = buttonX + (BUTTON_SIZE / 2) - (tooltipWidth / 2);

        int tooltipY = MARGIN_TOP + BUTTON_SIZE + 8;

        if (tooltipX < 4) tooltipX = 4;
        if (tooltipX + tooltipWidth > this.width - 4) tooltipX = this.width - tooltipWidth - 4;

        context.fill(tooltipX + 2, tooltipY + 2, tooltipX + tooltipWidth + 2, tooltipY + tooltipHeight + 2,
                new Color(0, 0, 0, 100).getRGB());

        context.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, PANEL_BG);

        context.drawText(MinecraftClient.getInstance().textRenderer, text,
                tooltipX + 4, tooltipY + 4, TEXT, true);
    }

    @Unique
    private boolean handleSocialButtonClick(double mouseX, double mouseY) {
        int screenWidth = this.width;

        int discordX = screenWidth - MARGIN_RIGHT - BUTTON_SIZE;
        int githubX = discordX - BUTTON_SIZE - BUTTON_SPACING;

        if (isMouseOver(mouseX, mouseY, discordX)) {
            openURL(DISCORD_URL);
            return true;
        } else if (isMouseOver(mouseX, mouseY, githubX)) {
            openURL(GITHUB_URL);
            return true;
        }

        return false;
    }

    @Unique
    private boolean isMouseOver(double mouseX, double mouseY, int x) {
        return mouseX >= x && mouseX <= x + BUTTON_SIZE && mouseY >= TitleScreenMixin.MARGIN_TOP && mouseY <= TitleScreenMixin.MARGIN_TOP + BUTTON_SIZE;
    }

    @Unique
    private void openURL(String url) {
        try {
            Util.getOperatingSystem().open(URI.create(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}