package chihalu.stackplus.client;

import chihalu.stackplus.StackLimitConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public final class StackPlusFontSupport {
    private static final Identifier DEFAULT_FONT_RESOURCE =
            Identifier.of("minecraft", "font/default.json");
    private static final long EXTERNAL_FONT_CHECK_INTERVAL_NANOS = 1_000_000_000L;
    private static long nextExternalFontCheck;
    private static boolean externalDefaultFontPresent;

    private StackPlusFontSupport() {
    }

    public static Text apply(Text component, StackLimitConfig.CountFont font) {
        return apply(component, font, true);
    }

    public static Text applyPreview(Text component, StackLimitConfig.CountFont font) {
        return apply(component, font, false);
    }

    private static Text apply(Text component, StackLimitConfig.CountFont font, boolean honorPriority) {
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
                && (!StyleSpriteSource.DEFAULT.equals(component.getStyle().getFont()) || hasExternalDefaultFont())) {
            return component;
        }
        Identifier fontId = Identifier.of("stackplus", fontPath);
        return component.copy().styled(style -> style.withFont(new StyleSpriteSource.Font(fontId)));
    }

    private static boolean hasExternalDefaultFont() {
        long now = System.nanoTime();
        if (now < nextExternalFontCheck) {
            return externalDefaultFontPresent;
        }
        List<Resource> resources = MinecraftClient.getInstance().getResourceManager().getAllResources(DEFAULT_FONT_RESOURCE);
        externalDefaultFontPresent = resources.size() > 1;
        nextExternalFontCheck = now + EXTERNAL_FONT_CHECK_INTERVAL_NANOS;
        return externalDefaultFontPresent;
    }
}
