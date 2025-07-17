package com.amberclient.mixins.features;

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

import java.net.URI;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    private static final Identifier DISCORD_ICON = Identifier.of("amber-client", "textures/gui/discord_icon.png");
    private static final Identifier GITHUB_ICON = Identifier.of("amber-client", "textures/gui/github_icon.png");

    private static final String DISCORD_URL = "https://discord.gg/jwgSKxWqrn";
    private static final String GITHUB_URL = "https://github.com/gqdThinky/AmberClient";

    private static final int BUTTON_SIZE = 32;
    private static final int BUTTON_SPACING = 8;
    private static final int MARGIN_TOP = 10;
    private static final int MARGIN_RIGHT = 10;

    public TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        renderSocialButtons(context, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {
        if (button == 0) { // Clic gauche
            if (handleSocialButtonClick(mouseX, mouseY)) {
                info.setReturnValue(true);
            }
        }
    }

    @Unique
    private void renderSocialButtons(DrawContext context, int mouseX, int mouseY) {
        int screenWidth = this.width;

        int discordX = screenWidth - MARGIN_RIGHT - BUTTON_SIZE;
        int discordY = MARGIN_TOP;

        int githubX = discordX - BUTTON_SIZE - BUTTON_SPACING;
        int githubY = MARGIN_TOP;

        context.drawTexture(RenderLayer::getGuiTextured, DISCORD_ICON, discordX, discordY, 0, 0, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);
        context.drawTexture(RenderLayer::getGuiTextured, GITHUB_ICON, githubX, githubY, 0, 0, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);

        if (isMouseOver(mouseX, mouseY, discordX, discordY, BUTTON_SIZE, BUTTON_SIZE)) {
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, Text.of("Discord"), (int)mouseX, (int)mouseY);
        } else if (isMouseOver(mouseX, mouseY, githubX, githubY, BUTTON_SIZE, BUTTON_SIZE)) {
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, Text.of("GitHub"), (int)mouseX, (int)mouseY);
        }
    }

    @Unique
    private boolean handleSocialButtonClick(double mouseX, double mouseY) {
        int screenWidth = this.width;

        int discordX = screenWidth - MARGIN_RIGHT - BUTTON_SIZE;
        int discordY = MARGIN_TOP;

        int githubX = discordX - BUTTON_SIZE - BUTTON_SPACING;
        int githubY = MARGIN_TOP;

        if (isMouseOver(mouseX, mouseY, discordX, discordY, BUTTON_SIZE, BUTTON_SIZE)) {
            openURL(DISCORD_URL);
            return true;
        } else if (isMouseOver(mouseX, mouseY, githubX, githubY, BUTTON_SIZE, BUTTON_SIZE)) {
            openURL(GITHUB_URL);
            return true;
        }

        return false;
    }

    @Unique
    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
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