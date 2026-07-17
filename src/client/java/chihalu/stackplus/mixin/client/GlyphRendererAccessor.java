package chihalu.stackplus.mixin.client;

import net.minecraft.client.font.GlyphRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlyphRenderer.class)
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
