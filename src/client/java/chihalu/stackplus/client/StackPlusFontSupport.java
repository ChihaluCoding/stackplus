package chihalu.stackplus.client;

import chihalu.stackplus.StackLimitConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.util.List;

public final class StackPlusFontSupport {
    private static final Identifier DEFAULT_FONT_RESOURCE =
            Identifier.fromNamespaceAndPath("minecraft", "font/default.json");
    private static final long EXTERNAL_FONT_CHECK_INTERVAL_NANOS = 1_000_000_000L;
    private static long nextExternalFontCheck;
    private static boolean externalDefaultFontPresent;

    private StackPlusFontSupport() {
    }

    public static Component apply(Component component, StackLimitConfig.CountFont font) {
        return apply(component, font, true);
    }

    public static Component applyPreview(Component component, StackLimitConfig.CountFont font) {
        return apply(component, font, false);
    }

    private static Component apply(Component component, StackLimitConfig.CountFont font, boolean honorPriority) {
        String fontPath = switch (font) {
            case DEFAULT -> null;
            case JETBRAINS_MONO_EXTRA_BOLD -> "jetbrains_mono_extra_bold";
            case ROBOTO_CONDENSED_BLACK -> "roboto_condensed_black";
            case BUNGEE -> "bungee";
            case BLACK_OPS_ONE -> "black_ops_one";
            case LILITA_ONE -> "lilita_one";
            case PRESS_START_2P -> "press_start_2p";
            case CUSTOM -> "custom";
        };
        if (fontPath == null) {
            return component;
        }
        if (honorPriority && StackLimitConfig.getFontPriority() == StackLimitConfig.FontPriority.LOW
                && (!FontDescription.DEFAULT.equals(component.getStyle().getFont()) || hasExternalDefaultFont())) {
            return component;
        }
        FontDescription description = new FontDescription.Resource(
                Identifier.fromNamespaceAndPath("stackplus", fontPath));
        return component.copy().withStyle(style -> style.withFont(description));
    }

    private static boolean hasExternalDefaultFont() {
        long now = System.nanoTime();
        if (now < nextExternalFontCheck) {
            return externalDefaultFontPresent;
        }
        List<Resource> resources = Minecraft.getInstance().getResourceManager().getResourceStack(DEFAULT_FONT_RESOURCE);
        externalDefaultFontPresent = resources.size() > 1;
        nextExternalFontCheck = now + EXTERNAL_FONT_CHECK_INTERVAL_NANOS;
        return externalDefaultFontPresent;
    }
}
