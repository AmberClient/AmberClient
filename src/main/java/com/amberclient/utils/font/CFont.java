package com.amberclient.utils.font;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class CFont {
    private static final float IMG_SIZE = 512f;
    private final CharData[] charData = new CharData[256];
    private Font font;
    private boolean antiAlias, fractionalMetrics;
    private int fontHeight = -1;
    private NativeImageBackedTexture texture;
    private Identifier textureId;

    public CFont(Font font, boolean antiAlias, boolean fractionalMetrics) {
        this.font = font;
        this.antiAlias = antiAlias;
        this.fractionalMetrics = fractionalMetrics;
        this.setupTexture();
    }

    private void setupTexture() {
        try {
            BufferedImage bufferedImage = generateFontImage();
            NativeImage nativeImage = convertBufferedImageToNativeImage(bufferedImage);

            if (this.texture != null) {
                this.texture.close();
            }

            this.texture = new NativeImageBackedTexture(nativeImage);

            MinecraftClient client = MinecraftClient.getInstance();
            if (this.textureId != null) {
                client.getTextureManager().destroyTexture(this.textureId);
            }

            this.textureId = Identifier.of("cfont", "font_texture_" + System.currentTimeMillis());
            client.getTextureManager().registerTexture(this.textureId, this.texture);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BufferedImage generateFontImage() {
        int imgSize = (int) IMG_SIZE;
        BufferedImage bufferedImage = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = (Graphics2D) bufferedImage.getGraphics();

        graphics.setFont(font);
        graphics.setColor(new Color(255, 255, 255, 0));
        graphics.fillRect(0, 0, imgSize, imgSize);
        graphics.setColor(Color.WHITE);

        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        FontMetrics fontMetrics = graphics.getFontMetrics();
        int charHeight = 0;
        int positionX = 0;
        int positionY = 1;

        for (int index = 0; index < charData.length; index++) {
            char c = (char) index;
            CharData charData = new CharData();
            Rectangle2D dimensions = fontMetrics.getStringBounds(String.valueOf(c), graphics);

            charData.width = dimensions.getBounds().width + 8;
            charData.height = dimensions.getBounds().height;

            if (positionX + charData.width >= imgSize) {
                positionX = 0;
                positionY += charHeight;
                charHeight = 0;
            }

            if (charData.height > charHeight) {
                charHeight = charData.height;
            }

            charData.storedX = positionX;
            charData.storedY = positionY;

            if (charData.height > this.fontHeight) {
                this.fontHeight = charData.height;
            }

            this.charData[index] = charData;
            graphics.drawString(String.valueOf(c), positionX + 2, positionY + fontMetrics.getAscent());
            positionX += charData.width;
        }

        graphics.dispose();
        return bufferedImage;
    }

    private NativeImage convertBufferedImageToNativeImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        NativeImage nativeImage = new NativeImage(width, height, false);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = bufferedImage.getRGB(x, y);
                // Fix 2: Use setPixelColor instead of setColor (which is private)
                nativeImage.setColorArgb(x, y, rgb);
            }
        }

        return nativeImage;
    }

    public void drawChar(char c, float x, float y) {
        if (c >= 0 && c < charData.length && charData[c] != null) {
            bindTexture();
            drawQuad(x, y, charData[c].width, charData[c].height,
                    charData[c].storedX, charData[c].storedY,
                    charData[c].width, charData[c].height);
        }
    }

    private void bindTexture() {
        if (textureId != null) {
            RenderSystem.setShaderTexture(0, textureId);
        }
    }

    private void drawQuad(float x, float y, float width, float height,
                          float srcX, float srcY, float srcWidth, float srcHeight) {
        float renderSRCX = srcX / IMG_SIZE;
        float renderSRCY = srcY / IMG_SIZE;
        float renderSRCWidth = srcWidth / IMG_SIZE;
        float renderSRCHeight = srcHeight / IMG_SIZE;

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY);
        GL11.glVertex2f(x + width, y);
        GL11.glTexCoord2f(renderSRCX, renderSRCY);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(renderSRCX, renderSRCY + renderSRCHeight);
        GL11.glVertex2f(x, y + height);
        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY + renderSRCHeight);
        GL11.glVertex2f(x + width, y + height);
        GL11.glEnd();
    }

    public void setAntiAlias(boolean antiAlias) {
        if (this.antiAlias != antiAlias) {
            this.antiAlias = antiAlias;
            this.setupTexture();
        }
    }

    public boolean isAntiAlias() {
        return antiAlias;
    }

    public void setFractionalMetrics(boolean fractionalMetrics) {
        if (this.fractionalMetrics != fractionalMetrics) {
            this.fractionalMetrics = fractionalMetrics;
            this.setupTexture();
        }
    }

    public boolean isFractionalMetrics() {
        return fractionalMetrics;
    }

    public void setFont(Font font) {
        this.font = font;
        this.setupTexture();
    }

    public Font getFont() {
        return font;
    }

    public int getFontHeight() {
        return fontHeight;
    }

    public void cleanup() {
        if (texture != null) {
            texture.close();
        }
        if (textureId != null && MinecraftClient.getInstance() != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(textureId);
        }
    }

    protected static class CharData {
        public int width, height, storedX, storedY;

        protected CharData() {}
    }
}