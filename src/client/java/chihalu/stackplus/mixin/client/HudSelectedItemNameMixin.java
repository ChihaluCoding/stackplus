package chihalu.stackplus.mixin.client;

import chihalu.stackplus.StackCountFormatter;
import chihalu.stackplus.StackLimitConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ホットバー選択時に表示されるアイテム名へ、正確なスタック数を追加します。
 * 設定でアイテム名の横（BESIDE）または下（BELOW）に表示を切り替えられます。
 */
@Mixin(Gui.class)
public class HudSelectedItemNameMixin {
    private static final int STACKPLUS_SELECTED_ITEM_NAME_TICKS = 40;
    private static final int STACKPLUS_BELOW_LINE_OFFSET = 24;

    @Shadow
    private ItemStack lastToolHighlight;

    @Shadow
    private int toolHighlightTimer;

    private ItemStack stackplus$lastSelectedStack = ItemStack.EMPTY;
    private int stackplus$lastSelectedStackCount = -1;


    @Inject(method = "renderSelectedItemName", at = @At("HEAD"))
    private void stackplus$keepSelectedItemCountVisible(GuiGraphics graphics, CallbackInfo callbackInfo) {

        StackLimitConfig.SelectedItemCountMode selectedItemCountMode = StackLimitConfig.getSelectedItemCountMode();
        if (!selectedItemCountMode.appendsCountToItemName()) {
            stackplus$lastSelectedStack = ItemStack.EMPTY;
            stackplus$lastSelectedStackCount = -1;
            return;
        }

        if (lastToolHighlight.isEmpty()) {
            stackplus$lastSelectedStack = ItemStack.EMPTY;
            stackplus$lastSelectedStackCount = -1;
            return;
        }

        boolean changedSelectedStack = !ItemStack.isSameItemSameComponents(lastToolHighlight, stackplus$lastSelectedStack);
        stackplus$lastSelectedStack = lastToolHighlight;
        stackplus$lastSelectedStackCount = lastToolHighlight.getCount();

        if (changedSelectedStack && lastToolHighlight.getCount() > 1) {
            toolHighlightTimer = Math.max(toolHighlightTimer, STACKPLUS_SELECTED_ITEM_NAME_TICKS);
            return;
        }

    }

    @ModifyArg(
            method = "renderSelectedItemName",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;append(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;"),
            index = 0
    )
    private Component stackplus$appendExactCountToSelectedItemName(Component itemName) {
        if (!StackLimitConfig.getSelectedItemCountMode().appendsCountToItemName()
                || lastToolHighlight.isEmpty()
                || lastToolHighlight.getCount() <= 1
                || StackLimitConfig.getSelectedItemCountPosition().isBelow()) {
            return itemName;
        }

        return Component.empty()
                .append(itemName)
                .append(Component.literal(" x" + StackCountFormatter.formatExact(lastToolHighlight.getCount()))
                        .withColor(StackLimitConfig.getSelectedItemCountColorRgb()));
    }

    @Inject(method = "renderSelectedItemName", at = @At("TAIL"))
    private void stackplus$renderPersistentItemCount(GuiGraphics graphics, CallbackInfo callbackInfo) {
        StackLimitConfig.SelectedItemCountMode mode = StackLimitConfig.getSelectedItemCountMode();
        ItemStack selectedStack = stackplus$lastSelectedStack;
        if (!mode.appendsCountToItemName()
                || selectedStack.isEmpty()
                || selectedStack.getCount() <= 1
                || (toolHighlightTimer <= 0 && !mode.keepsVisible())) {
            return;
        }

        boolean below = StackLimitConfig.getSelectedItemCountPosition().isBelow();
        if (!below && (!mode.keepsVisible() || toolHighlightTimer > 0)) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int y = client.getWindow().getGuiScaledHeight() - 59 + (below ? STACKPLUS_BELOW_LINE_OFFSET : 0);
        if (client.gameMode != null && !client.gameMode.getPlayerMode().isCreative()) {
            y -= 13;

        }

        int alpha = mode.keepsVisible() ? 255 : (toolHighlightTimer > 15 ? 255 : toolHighlightTimer * 17);
        if (alpha < 0) alpha = 0;
        int color = (alpha << 24) | StackLimitConfig.getSelectedItemCountColorRgb();
        MutableComponent countText = Component.literal("x" + StackCountFormatter.formatExact(selectedStack.getCount()));
        graphics.drawCenteredString(client.font, countText, screenWidth / 2, y, color);
    }
}
