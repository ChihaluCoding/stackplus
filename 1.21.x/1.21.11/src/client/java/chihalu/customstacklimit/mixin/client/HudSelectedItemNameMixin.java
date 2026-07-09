package chihalu.customstacklimit.mixin.client;

import chihalu.customstacklimit.StackCountFormatter;
import chihalu.customstacklimit.StackLimitConfig;
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
    private GuiGraphics stackplus$cachedGraphics;

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

        if (selectedItemCountMode.keepsVisible() && lastToolHighlight.getCount() > 1) {
            toolHighlightTimer = Math.max(toolHighlightTimer, 10);
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
                .append(Component.literal(" x" + StackCountFormatter.formatExact(lastToolHighlight.getCount())));
    }

    @Inject(method = "renderSelectedItemName", at = @At("TAIL"))
    private void stackplus$renderCountBelowItemName(GuiGraphics graphics, CallbackInfo callbackInfo) {
        if (!StackLimitConfig.getSelectedItemCountMode().appendsCountToItemName()
                || !StackLimitConfig.getSelectedItemCountPosition().isBelow()
                || lastToolHighlight.isEmpty()
                || lastToolHighlight.getCount() <= 1
                || (toolHighlightTimer <= 0 && !StackLimitConfig.getSelectedItemCountMode().keepsVisible())) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int y = client.getWindow().getGuiScaledHeight() - 59 + STACKPLUS_BELOW_LINE_OFFSET;
        if (client.gameMode != null && !client.gameMode.getPlayerMode().isCreative()) {
            y -= 13;
            
        }

        MutableComponent countText = Component.literal("x" + StackCountFormatter.formatExact(lastToolHighlight.getCount()))
                .withColor(0xFFFFFFFF);
        graphics.drawCenteredString(client.font, countText, screenWidth / 2, y, 0xFFFFFFFF);
    }
}
