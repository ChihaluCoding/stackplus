package chihalu.stackplus.mixin.client;

import chihalu.stackplus.StackCountFormatter;
import chihalu.stackplus.StackLimitConfig;
import chihalu.stackplus.client.StackPlusFontSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
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
 * ホットバー選択時に表示されるアイテム名の下へ、正確なスタック数を追加します。
 */
@Mixin(Hud.class)
public class HudSelectedItemNameMixin {
    private static final int STACKPLUS_SELECTED_ITEM_NAME_TICKS = 40;
    private static final int STACKPLUS_BELOW_LINE_OFFSET = 24;

    @Shadow
    private ItemStack lastToolHighlight;

    @Shadow
    private int toolHighlightTimer;

    private ItemStack stackplus$lastSelectedStack = ItemStack.EMPTY;
    private int stackplus$lastSelectedStackCount = -1;


    @Inject(method = "extractSelectedItemName", at = @At("HEAD"))
    private void stackplus$keepSelectedItemCountVisible(GuiGraphicsExtractor graphics, CallbackInfo callbackInfo) {

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

    @Inject(method = "extractSelectedItemName", at = @At("TAIL"))
    private void stackplus$renderPersistentItemCount(GuiGraphicsExtractor graphics, CallbackInfo callbackInfo) {
        StackLimitConfig.SelectedItemCountMode mode = StackLimitConfig.getSelectedItemCountMode();
        ItemStack selectedStack = stackplus$lastSelectedStack;
        if (!mode.appendsCountToItemName()
                || selectedStack.isEmpty()
                || selectedStack.getCount() <= 1
                || (toolHighlightTimer <= 0 && !mode.keepsVisible())) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int y = client.getWindow().getGuiScaledHeight() - 59 + STACKPLUS_BELOW_LINE_OFFSET;
        if (client.gameMode != null && !client.gameMode.getPlayerMode().isCreative()) {
            y -= 13;
            
        }

        int alpha = mode.keepsVisible() ? 255 : (toolHighlightTimer > 15 ? 255 : toolHighlightTimer * 17);
        if (alpha < 0) alpha = 0;
        int color = (alpha << 24) | StackLimitConfig.getSelectedItemCountColorRgb();
        Component countText = StackPlusFontSupport.apply(
                Component.literal("x" + StackCountFormatter.formatExact(selectedStack.getCount())),
                StackLimitConfig.getSelectedItemCountFont());
        graphics.centeredText(client.font, countText, screenWidth / 2, y, color);
    }
}
