package com.king.ritelock;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class GalaxyText {
	private GalaxyText() {}

	// ungu -> biru -> magenta -> pink
	private static final int[] STOPS = { 0x7F00FF, 0x4B6CFF, 0xB100FF, 0xFF4FD8 };

	public static MutableComponent gradient(String text) {
		MutableComponent out = Component.empty();
		int n = text.length();
		for (int i = 0; i < n; i++) {
			float t = n <= 1 ? 0f : i / (float) (n - 1);
			Style style = Style.EMPTY.withColor(TextColor.fromRgb(sample(t))).withBold(true);
			out.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(style));
		}
		return out;
	}

	private static int sample(float t) {
		float scaled = t * (STOPS.length - 1);
		int idx = Math.min((int) scaled, STOPS.length - 2);
		float local = scaled - idx;
		int a = STOPS[idx], b = STOPS[idx + 1];
		int r = lerp((a >> 16) & 0xFF, (b >> 16) & 0xFF, local);
		int g = lerp((a >> 8) & 0xFF, (b >> 8) & 0xFF, local);
		int bl = lerp(a & 0xFF, b & 0xFF, local);
		return (r << 16) | (g << 8) | bl;
	}

	private static int lerp(int a, int b, float t) {
		return Math.round(a + (b - a) * t);
	}
}
