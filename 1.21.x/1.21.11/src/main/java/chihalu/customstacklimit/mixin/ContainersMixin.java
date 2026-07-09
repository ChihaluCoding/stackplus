package chihalu.customstacklimit.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 巨大スタックを持つコンテナ破壊時に、バニラの小分けドロップで大量エンティティ化するのを防ぎます。
 */
@Mixin(Containers.class)
public class ContainersMixin {
    private static final int VANILLA_MAX_STACK_SIZE = 64;
    private static final double ITEM_ENTITY_WIDTH = 0.25D;

    @Inject(method = "dropItemStack", at = @At("HEAD"), cancellable = true, require = 1)
    private static void dropLargeStackAsSingleEntity(Level level, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        if (stack.isEmpty() || stack.getCount() <= VANILLA_MAX_STACK_SIZE) {
            return;
        }

        ItemStack droppedStack = chihalu.customstacklimit.StackLimitConfig.clampStackCount(stack).copy();
        if (spawnSingleLargeStack(level, x, y, z, droppedStack)) {
            stack.setCount(0);
            ci.cancel();
        }
    }

    private static boolean spawnSingleLargeStack(Level level, double x, double y, double z, ItemStack stack) {
        double positionRange = 1.0D - ITEM_ENTITY_WIDTH;
        double positionOffset = ITEM_ENTITY_WIDTH / 2.0D;
        RandomSource random = level.getRandom();
        double spawnX = Math.floor(x) + random.nextDouble() * positionRange + positionOffset;
        double spawnY = Math.floor(y) + random.nextDouble() * positionRange;
        double spawnZ = Math.floor(z) + random.nextDouble() * positionRange + positionOffset;

        ItemEntity itemEntity = new ItemEntity(level, spawnX, spawnY, spawnZ, stack);
        return level.addFreshEntity(itemEntity);
    }
}
