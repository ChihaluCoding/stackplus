package chihalu.customstacklimit.mixin;

import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * コンテナ破壊時などにバニラが大きなスタックを細切れにドロップするのを防ぎます。
 * これがないと、99個を超えるスタックが地面に散らばった後、拾っても99個に区切られてしまいます。
 */
@Mixin(Containers.class)
public class ContainersMixin {
    private static final double ITEM_ENTITY_WIDTH = 0.25D;

    @Inject(method = "dropItemStack", at = @At("HEAD"), cancellable = true)
    private static void dropLargeStackAsSingleEntity(Level level, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        if (stack.isEmpty() || stack.getCount() <= Item.ABSOLUTE_MAX_STACK_SIZE) {
            return;
        }

        spawnSingleLargeStack(level, x, y, z, stack.copy());
        stack.setCount(0);
        ci.cancel();
    }

    private static void spawnSingleLargeStack(Level level, double x, double y, double z, ItemStack stack) {
        double positionRange = 1.0D - ITEM_ENTITY_WIDTH;
        double positionOffset = ITEM_ENTITY_WIDTH / 2.0D;
        double spawnX = Math.floor(x) + level.getRandom().nextDouble() * positionRange + positionOffset;
        double spawnY = Math.floor(y) + level.getRandom().nextDouble() * positionRange;
        double spawnZ = Math.floor(z) + level.getRandom().nextDouble() * positionRange + positionOffset;

        ItemEntity itemEntity = new ItemEntity(level, spawnX, spawnY, spawnZ, stack);
        level.addFreshEntity(itemEntity);
    }
}
