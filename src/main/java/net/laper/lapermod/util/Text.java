package net.laper.lapermod.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/** Helper teks: gradasi warna, kode warna '&' dan format waktu. */
public final class Text {
    private Text() {}

    public static MutableComponent gradient(String text, int from, int to, boolean bold) {
        MutableComponent out = Component.empty();
        int len = text.length();
        for (int i = 0; i < len; i++) {
            float t = len <= 1 ? 0f : i / (float) (len - 1);
            int r = lerp((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
            int g = lerp((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
            int b = lerp(from & 0xFF, to & 0xFF, t);
            Style style = Style.EMPTY.withColor(TextColor.fromRgb((r << 16) | (g << 8) | b)).withBold(bold);
            out.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(style));
        }
        return out;
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    public static int parseColor(String hex, int fallback) {
        try {
            String s = hex == null ? "" : hex.trim();
            if (s.startsWith("#")) s = s.substring(1);
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Mendukung &0-&f, &k-&o, &r dan &#RRGGBB. */
    public static MutableComponent legacy(String in, boolean noItalic) {
        MutableComponent root = Component.empty();
        if (in == null) return root;
        Style base = noItalic ? Style.EMPTY.withItalic(false) : Style.EMPTY;
        Style style = base;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (c == '&' && i + 1 < in.length()) {
                char n = in.charAt(i + 1);
                if (n == '#' && i + 8 <= in.length() && isHex(in, i + 2, i + 8)) {
                    flush(root, buf, style);
                    style = style.withColor(TextColor.fromRgb(Integer.parseInt(in.substring(i + 2, i + 8), 16)));
                    i += 7;
                    continue;
                }
                ChatFormatting f = ChatFormatting.getByCode(Character.toLowerCase(n));
                if (f != null) {
                    flush(root, buf, style);
                    if (f == ChatFormatting.RESET) style = base;
                    else if (isLegacyColor(f)) style = base.withColor(TextColor.fromRgb(legacyColorRgb(f)));
                    else style = style.applyFormat(f);
                    i++;
                    continue;
                }
            }
            buf.append(c);
        }
        flush(root, buf, style);
        return root;
    }


    /**
     * Detect legacy palette colors without relying on ChatFormatting#getColor,
     * whose availability differs between Minecraft mapping versions.
     */
    private static boolean isLegacyColor(ChatFormatting formatting) {
        return switch (formatting) {
            case BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE,
                 GOLD, GRAY, DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE,
                 YELLOW, WHITE -> true;
            default -> false;
        };
    }

    private static int legacyColorRgb(ChatFormatting formatting) {
        return switch (formatting) {
            case BLACK -> 0x000000;
            case DARK_BLUE -> 0x0000AA;
            case DARK_GREEN -> 0x00AA00;
            case DARK_AQUA -> 0x00AAAA;
            case DARK_RED -> 0xAA0000;
            case DARK_PURPLE -> 0xAA00AA;
            case GOLD -> 0xFFAA00;
            case GRAY -> 0xAAAAAA;
            case DARK_GRAY -> 0x555555;
            case BLUE -> 0x5555FF;
            case GREEN -> 0x55FF55;
            case AQUA -> 0x55FFFF;
            case RED -> 0xFF5555;
            case LIGHT_PURPLE -> 0xFF55FF;
            case YELLOW -> 0xFFFF55;
            case WHITE -> 0xFFFFFF;
            default -> throw new IllegalArgumentException("Not a legacy color: " + formatting);
        };
    }

    private static boolean isHex(String s, int from, int to) {
        for (int i = from; i < to; i++) {
            if (Character.digit(s.charAt(i), 16) < 0) return false;
        }
        return true;
    }

    private static void flush(MutableComponent root, StringBuilder buf, Style style) {
        if (buf.length() > 0) {
            root.append(Component.literal(buf.toString()).withStyle(style));
            buf.setLength(0);
        }
    }

    public static String formatTime(long totalSeconds) {
        long s = Math.max(0, totalSeconds);
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }
}
