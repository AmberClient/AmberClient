package com.amberclient.utils.module;

import java.awt.Color;
import java.util.Set;

public class ModuleSettings {
    public enum SettingType {
        BOOLEAN,
        INTEGER,
        DOUBLE,
        STRING,
        ENUM,
        SET,
        COLOR
    }

    private final String name;
    private final String description;
    private final SettingType type;
    private Object value;
    private Object defaultValue;
    private Number minValue;
    private Number maxValue;
    private Number stepValue;

    public ModuleSettings(String name, String description, boolean defaultValue) {
        this.name = name;
        this.description = description;
        this.type = SettingType.BOOLEAN;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public ModuleSettings(String name, String description, int defaultValue, int minValue, int maxValue) {
        this.name = name;
        this.description = description;
        this.type = SettingType.INTEGER;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.stepValue = 1;
    }

    public ModuleSettings(String name, String description, int defaultValue) {
        this(name, description, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public ModuleSettings(String name, String description, double defaultValue, double minValue, double maxValue, double step) {
        this.name = name;
        this.description = description;
        this.type = SettingType.DOUBLE;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.stepValue = step;
    }

    public ModuleSettings(String name, String description, double defaultValue, double minValue, double maxValue) {
        this(name, description, defaultValue, minValue, maxValue, 0.1);
    }

    public ModuleSettings(String name, String description, double defaultValue) {
        this(name, description, defaultValue, Double.MIN_VALUE, Double.MAX_VALUE, 0.1);
    }

    public ModuleSettings(String name, String description, String defaultValue) {
        this.name = name;
        this.description = description;
        this.type = SettingType.STRING;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public ModuleSettings(String name, String description, Enum<?> defaultValue) {
        this.name = name;
        this.description = description;
        this.type = SettingType.ENUM;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public ModuleSettings(String name, String description, Set<?> defaultValue) {
        this.name = name;
        this.description = description;
        this.type = SettingType.SET;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    // Color constructor with Color object
    public ModuleSettings(String name, String description, Color defaultValue) {
        this.name = name;
        this.description = description;
        this.type = SettingType.COLOR;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    // Static factory methods for color creation to avoid constructor conflicts
    public static ModuleSettings createColorSetting(String name, String description, int red, int green, int blue) {
        return new ModuleSettings(name, description, new Color(red, green, blue));
    }

    public static ModuleSettings createColorSetting(String name, String description, int red, int green, int blue, int alpha) {
        return new ModuleSettings(name, description, new Color(red, green, blue, alpha));
    }

    public static ModuleSettings createColorSettingFromHex(String name, String description, int hexColor) {
        return new ModuleSettings(name, description, new Color(hexColor));
    }

    public static ModuleSettings createColorSettingFromHex(String name, String description, int hexColor, boolean hasAlpha) {
        return new ModuleSettings(name, description, new Color(hexColor, hasAlpha));
    }

    public boolean isEnabled() {
        if (type == SettingType.BOOLEAN) {
            return (boolean) value;
        }
        throw new IllegalStateException("Setting is not a boolean and cannot be checked as enabled/disabled");
    }

    public void setBooleanValue(boolean value) {
        if (type == SettingType.BOOLEAN) {
            this.value = value;
        } else {
            throw new IllegalStateException("Setting is not a boolean");
        }
    }

    // GETTERS
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public SettingType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Number getStepValue() {
        return stepValue;
    }

    public boolean getBooleanValue() {
        if (type == SettingType.BOOLEAN) {
            return (boolean) value;
        }
        throw new IllegalStateException("Setting is not a boolean");
    }

    public int getIntegerValue() {
        if (type == SettingType.INTEGER) {
            return (int) value;
        }
        throw new IllegalStateException("Setting is not an integer");
    }

    public double getDoubleValue() {
        if (type == SettingType.DOUBLE) {
            return (double) value;
        }
        throw new IllegalStateException("Setting is not a double");
    }

    public String getStringValue() {
        if (type == SettingType.STRING) {
            return (String) value;
        }
        throw new IllegalStateException("Setting is not a string");
    }

    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> T getEnumValue() {
        if (type == SettingType.ENUM) {
            return (T) value;
        }
        throw new IllegalStateException("Setting is not an enum");
    }

    @SuppressWarnings("unchecked")
    public <T> Set<T> getSetValue() {
        if (type == SettingType.SET) {
            return (Set<T>) value;
        }
        throw new IllegalStateException("Setting is not a set");
    }

    public Color getColorValue() {
        if (type == SettingType.COLOR) {
            return (Color) value;
        }
        throw new IllegalStateException("Setting is not a color");
    }

    // Get color as RGB integer (without alpha)
    public int getColorRGB() {
        if (type == SettingType.COLOR) {
            Color color = (Color) value;
            return color.getRGB() & 0x00FFFFFF; // Remove alpha bits
        }
        throw new IllegalStateException("Setting is not a color");
    }

    // Get color as ARGB integer (with alpha)
    public int getColorARGB() {
        if (type == SettingType.COLOR) {
            Color color = (Color) value;
            return color.getRGB(); // Includes alpha
        }
        throw new IllegalStateException("Setting is not a color");
    }

    // Get individual color components
    public int getColorRed() {
        return getColorValue().getRed();
    }

    public int getColorGreen() {
        return getColorValue().getGreen();
    }

    public int getColorBlue() {
        return getColorValue().getBlue();
    }

    public int getColorAlpha() {
        return getColorValue().getAlpha();
    }

    // Get color as hex string
    public String getColorHex() {
        if (type == SettingType.COLOR) {
            Color color = (Color) value;
            return String.format("#%06X", color.getRGB() & 0x00FFFFFF);
        }
        throw new IllegalStateException("Setting is not a color");
    }

    // Get color as hex string with alpha
    public String getColorHexAlpha() {
        if (type == SettingType.COLOR) {
            Color color = (Color) value;
            return String.format("#%08X", color.getRGB());
        }
        throw new IllegalStateException("Setting is not a color");
    }

    // SETTERS

    public void setIntegerValue(int value) {
        if (type == SettingType.INTEGER) {
            if (minValue != null && value < minValue.intValue()) {
                value = minValue.intValue();
            }
            if (maxValue != null && value > maxValue.intValue()) {
                value = maxValue.intValue();
            }

            if (stepValue != null && stepValue.intValue() > 0) {
                int step = stepValue.intValue();
                int min = minValue != null ? minValue.intValue() : 0;

                int stepsFromMin = (value - min) / step;
                int roundedSteps = Math.round((float)(value - min) / step);

                value = min + (roundedSteps * step);

                if (minValue != null && value < minValue.intValue()) {
                    value = minValue.intValue();
                }
                if (maxValue != null && value > maxValue.intValue()) {
                    value = maxValue.intValue();
                }
            }

            this.value = value;
        } else {
            throw new IllegalStateException("Setting is not an integer");
        }
    }

    public void setDoubleValue(double value) {
        if (type == SettingType.DOUBLE) {
            if (minValue != null && value < minValue.doubleValue()) {
                value = minValue.doubleValue();
            }
            if (maxValue != null && value > maxValue.doubleValue()) {
                value = maxValue.doubleValue();
            }

            if (stepValue != null && stepValue.doubleValue() > 0) {
                double step = stepValue.doubleValue();
                double min = minValue != null ? minValue.doubleValue() : 0.0;

                double stepsFromMin = (value - min) / step;
                double roundedSteps = Math.round(stepsFromMin);

                value = min + (roundedSteps * step);

                if (minValue != null && value < minValue.doubleValue()) {
                    value = minValue.doubleValue();
                }
                if (maxValue != null && value > maxValue.doubleValue()) {
                    value = maxValue.doubleValue();
                }
            }

            this.value = value;
        } else {
            throw new IllegalStateException("Setting is not a double");
        }
    }

    public void setStringValue(String value) {
        if (type == SettingType.STRING) {
            this.value = value != null ? value : "";
        } else {
            throw new IllegalStateException("Setting is not a string");
        }
    }

    public void setEnumValue(Enum<?> value) {
        if (type == SettingType.ENUM) {
            this.value = value;
        } else {
            throw new IllegalStateException("Setting is not an enum");
        }
    }

    public <T> void setSetValue(Set<T> value) {
        if (type == SettingType.SET) {
            this.value = value;
        } else {
            throw new IllegalStateException("Setting is not a set");
        }
    }

    public void setColorValue(Color color) {
        if (type == SettingType.COLOR) {
            this.value = color != null ? color : Color.WHITE;
        } else {
            throw new IllegalStateException("Setting is not a color");
        }
    }

    // Set color from RGB values
    public void setColorValue(int red, int green, int blue) {
        setColorValue(new Color(
                Math.max(0, Math.min(255, red)),
                Math.max(0, Math.min(255, green)),
                Math.max(0, Math.min(255, blue))
        ));
    }

    // Set color from RGBA values
    public void setColorValue(int red, int green, int blue, int alpha) {
        setColorValue(new Color(
                Math.max(0, Math.min(255, red)),
                Math.max(0, Math.min(255, green)),
                Math.max(0, Math.min(255, blue)),
                Math.max(0, Math.min(255, alpha))
        ));
    }

    // Set color from hex integer
    public void setColorValue(int hexColor) {
        setColorValue(new Color(hexColor));
    }

    // Set color from hex integer with alpha
    public void setColorValue(int hexColor, boolean hasAlpha) {
        setColorValue(new Color(hexColor, hasAlpha));
    }

    // Set color from hex string
    public void setColorValueFromHex(String hexColor) {
        if (type == SettingType.COLOR) {
            try {
                // Remove # if present
                if (hexColor.startsWith("#")) {
                    hexColor = hexColor.substring(1);
                }

                // Parse hex string
                if (hexColor.length() == 6) {
                    // RGB format
                    int rgb = Integer.parseInt(hexColor, 16);
                    setColorValue(new Color(rgb));
                } else if (hexColor.length() == 8) {
                    // ARGB format
                    long argb = Long.parseLong(hexColor, 16);
                    setColorValue(new Color((int) argb, true));
                } else {
                    throw new IllegalArgumentException("Invalid hex color format");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid hex color: " + hexColor);
            }
        } else {
            throw new IllegalStateException("Setting is not a color");
        }
    }

    public void setValue(Object value) {
        switch (type) {
            case BOOLEAN:
                if (value instanceof Boolean) {
                    setBooleanValue((Boolean) value);
                }
                break;
            case INTEGER:
                if (value instanceof Integer) {
                    setIntegerValue((Integer) value);
                } else if (value instanceof Number) {
                    setIntegerValue(((Number) value).intValue());
                }
                break;
            case DOUBLE:
                if (value instanceof Double) {
                    setDoubleValue((Double) value);
                } else if (value instanceof Number) {
                    setDoubleValue(((Number) value).doubleValue());
                }
                break;
            case STRING:
                if (value instanceof String) {
                    setStringValue((String) value);
                } else if (value != null) {
                    setStringValue(value.toString());
                }
                break;
            case ENUM:
                if (value instanceof Enum<?>) {
                    setEnumValue((Enum<?>) value);
                }
                break;
            case SET:
                if (value instanceof Set<?>) {
                    setSetValue((Set<?>) value);
                }
                break;
            case COLOR:
                if (value instanceof Color) {
                    setColorValue((Color) value);
                } else if (value instanceof Integer) {
                    setColorValue((Integer) value);
                } else if (value instanceof String) {
                    setColorValueFromHex((String) value);
                }
                break;
        }
    }

    public void resetToDefault() {
        this.value = this.defaultValue;
    }

    public int getIntValue() {
        if (type == SettingType.INTEGER) {
            return (int) value;
        }
        throw new IllegalStateException("Setting is not an integer");
    }

    public Number getMinValue() {
        return minValue;
    }

    public Number getMaxValue() {
        return maxValue;
    }

    public boolean hasRange() {
        return minValue != null && maxValue != null;
    }
}