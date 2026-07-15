package chihalu.stackplus.mixin.client;

import chihalu.stackplus.client.StackPlusCustomFont;
import net.minecraft.resource.LifecycledResourceManagerImpl;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

@Mixin(LifecycledResourceManagerImpl.class)
public abstract class MultiPackResourceManagerMixin {
    @Shadow
    @Final
    private List<ResourcePack> packs;

    @Inject(method = "getResource", at = @At("RETURN"), cancellable = true)
    private void stackplus$loadCustomFont(Identifier id, CallbackInfoReturnable<Optional<Resource>> cir) {
        if (!StackPlusCustomFont.BITMAP_RESOURCE_ID.equals(id) || packs.isEmpty()) {
            return;
        }
        StackPlusCustomFont.getBitmapFile().ifPresent(path ->
                cir.setReturnValue(Optional.of(new Resource(packs.getLast(), () -> Files.newInputStream(path)))));
    }
}
