package chihalu.stackplus.mixin.client;

import chihalu.stackplus.StackCountFormatter;
import chihalu.stackplus.StackLimitConfig;
import chihalu.stackplus.client.StackPlusFontSupport;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ホットバー選択時に表示されるアイテム名の下へ、正確なスタック数を追加します。
 */
@Mixin(InGameHud.class)
public class InGameHudSelectedItemNameMixin {
    private static final int STACKPLUS_SELECTED_ITEM_NAME_TICKS = 40;
    private static final int STACKPLUS_BELOW_LINE_OFFSET = 24;

    @Shadow
    private ItemStack currentStack;

    @Shadow
    private int heldItemTooltipFade;

    private ItemStack stackplus$lastSelectedStack = ItemStack.EMPTY;
    private int stackplus$lastSelectedStackCount = -1;

    @Inject(method = "renderHeldItemTooltip", at = @At("HEAD"))
    private void stackplus$keepSelectedItemCountVisible(DrawContext context, CallbackInfo callbackInfo) {
        StackLimitConfig.SelectedItemCountMode selectedItemCountMode = StackLimitConfig.getSelectedItemCountMode();
        if (!selectedItemCountMode.appendsCountToItemName()) {
            stackplus$lastSelectedStack = ItemStack.EMPTY;
            stackplus$lastSelectedStackCount = -1;
            return;
        }

        ItemStack selectedStack = getSelectedStack();
        if (selectedStack.isEmpty()) {
            stackplus$lastSelectedStack = ItemStack.EMPTY;
            stackplus$lastSelectedStackCount = -1;
            return;
        }

        boolean changedSelectedStack = !ItemStack.areItemsEqual(selectedStack, stackplus$lastSelectedStack);
        stackplus$lastSelectedStack = selectedStack;
        stackplus$lastSelectedStackCount = selectedStack.getCount();

        if (changedSelectedStack && selectedStack.getCount() > 1) {
            currentStack = selectedStack.copy();
            heldItemTooltipFade = Math.max(heldItemTooltipFade, STACKPLUS_SELECTED_ITEM_NAME_TICKS);
            return;
        }

    }

    private static ItemStack getSelectedStack() {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft.player == null) {
            return ItemStack.EMPTY;
        }
        return minecraft.player.getMainHandStack();
    }

    @Inject(method = "renderHeldItemTooltip", at = @At("TAIL"))
    private void stackplus$renderPersistentItemCount(DrawContext context, CallbackInfo callbackInfo) {
        StackLimitConfig.SelectedItemCountMode mode = StackLimitConfig.getSelectedItemCountMode();
        ItemStack selectedStack = stackplus$lastSelectedStack;
        if (!mode.appendsCountToItemName()
                || selectedStack.isEmpty()
                || selectedStack.getCount() <= 1
                || (heldItemTooltipFade <= 0 && !mode.keepsVisible())) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int y = client.getWindow().getScaledHeight() - 59 + STACKPLUS_BELOW_LINE_OFFSET;
        if (client.interactionManager != null && !client.interactionManager.getCurrentGameMode().isCreative()) {
            y -= 13;
            
        }

        int alpha = mode.keepsVisible() ? 255 : (heldItemTooltipFade > 15 ? 255 : heldItemTooltipFade * 17);
        if (alpha < 0) alpha = 0;
        int color = (alpha << 24) | StackLimitConfig.getSelectedItemCountColorRgb();
        Text countText = StackPlusFontSupport.apply(
                Text.literal("x" + StackCountFormatter.formatExact(selectedStack.getCount())),
                StackLimitConfig.getSelectedItemCountFont());
        context.drawCenteredTextWithShadow(client.textRenderer, countText, screenWidth / 2, y, color);
    }
}
