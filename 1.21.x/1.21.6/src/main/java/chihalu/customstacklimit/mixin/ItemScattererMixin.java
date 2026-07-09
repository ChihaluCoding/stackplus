package chihalu.customstacklimit.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 巨大スタックを持つコンテナ破壊時に、バニラの小分けドロップで大量エンティティ化するのを防ぎます。
 */
@Mixin(ItemScatterer.class)
public class ItemScattererMixin {
    private static final int VANILLA_MAX_STACK_SIZE = 64;
    private static final double ITEM_ENTITY_SPREAD_VELOCITY = 0.11485000171139836D;

    @Inject(method = "spawn(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
    private static void spawnLargeStackAsSingleEntity(World world, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        if (stack.isEmpty() || stack.getCount() <= VANILLA_MAX_STACK_SIZE) {
            return;
        }

        spawnSingleLargeStack(world, x, y, z, stack.copy());
        stack.setCount(0);
        ci.cancel();
    }

    private static void spawnSingleLargeStack(World world, double x, double y, double z, ItemStack stack) {
        double entityWidth = EntityType.ITEM.getWidth();
        double positionRange = 1.0D - entityWidth;
        double positionOffset = entityWidth / 2.0D;
        Random random = world.random;
        double spawnX = Math.floor(x) + random.nextDouble() * positionRange + positionOffset;
        double spawnY = Math.floor(y) + random.nextDouble() * positionRange;
        double spawnZ = Math.floor(z) + random.nextDouble() * positionRange + positionOffset;

        ItemEntity itemEntity = new ItemEntity(world, spawnX, spawnY, spawnZ, stack);
        itemEntity.setVelocity(
                random.nextTriangular(0.0D, ITEM_ENTITY_SPREAD_VELOCITY),
                random.nextTriangular(0.2D, ITEM_ENTITY_SPREAD_VELOCITY),
                random.nextTriangular(0.0D, ITEM_ENTITY_SPREAD_VELOCITY)
        );
        world.spawnEntity(itemEntity);
    }
}
