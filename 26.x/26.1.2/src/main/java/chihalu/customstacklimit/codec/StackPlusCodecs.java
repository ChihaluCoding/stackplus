package chihalu.customstacklimit.codec;

import chihalu.customstacklimit.StackLimitConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * vanillaの保存用ItemStack CODECに残る99個制限を、必要な保存経路だけで拡張します。
 */
public final class StackPlusCodecs {
    public static final MapCodec<ItemStack> ITEM_STACK_MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Item.CODEC_WITH_BOUND_COMPONENTS.fieldOf("id").forGetter(ItemStack::typeHolder),
            ExtraCodecs.intRange(1, StackLimitConfig.MAX_STACK_LIMIT).optionalFieldOf("count", 1)
                    .forGetter(ItemStack::getCount),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                    .forGetter(ItemStack::getComponentsPatch)
    ).apply(instance, ItemStack::new));

    public static final Codec<ItemStack> ITEM_STACK_CODEC = Codec.lazyInitialized(ITEM_STACK_MAP_CODEC::codec);

    public static final Codec<ItemStackWithSlot> ITEM_STACK_WITH_SLOT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.UNSIGNED_BYTE.optionalFieldOf("Slot", 0)
                    .forGetter(ItemStackWithSlot::slot),
            ITEM_STACK_MAP_CODEC.forGetter(ItemStackWithSlot::stack)
    ).apply(instance, ItemStackWithSlot::new));

    private StackPlusCodecs() {
    }
}
