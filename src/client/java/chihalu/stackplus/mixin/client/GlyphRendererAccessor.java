package chihalu.stackplus.mixin.client;

import net.minecraft.client.font.BakedGlyph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BakedGlyph.class)
public interface GlyphRendererAccessor {
    @Accessor("minX")
    float stackplus$getMinX();

    @Accessor("maxX")
    float stackplus$getMaxX();

    @Accessor("minY")
    float stackplus$getMinY();

    @Accessor("maxY")
    float stackplus$getMaxY();
}
