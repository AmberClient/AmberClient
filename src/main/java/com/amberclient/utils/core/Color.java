package com.amberclient.utils.core;

public class Color {
    private int red;
    private int green;
    private int blue;
    private int alpha;

    public Color(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    // Getters
    public int red() { return red; }
    public int green() { return green; }
    public int blue() { return blue; }
    public int alpha() { return alpha; }

    // Setters
    public void setRed(int red) { this.red = red; }
    public void setGreen(int green) { this.green = green; }
    public void setBlue(int blue) { this.blue = blue; }
    public void setAlpha(int alpha) { this.alpha = alpha; }

    // Set method that your EntityUtils code expects
    public void set(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    @Override
    public String toString() {
        return "Color[red=" + red + ", green=" + green + ", blue=" + blue + ", alpha=" + alpha + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Color color = (Color) o;
        return red == color.red && green == color.green && blue == color.blue && alpha == color.alpha;
    }

    @Override
    public int hashCode() {
        int result = red;
        result = 31 * result + green;
        result = 31 * result + blue;
        result = 31 * result + alpha;
        return result;
    }
}