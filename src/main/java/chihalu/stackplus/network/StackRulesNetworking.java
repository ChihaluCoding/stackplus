package chihalu.stackplus.network;

import chihalu.stackplus.StackLimitConfig;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class StackRulesNetworking {
    public static final CustomPacketPayload.Type<StackRulesPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("stackplus", "stack_rules"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StackRulesPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.stringUtf8(1_048_576), StackRulesPayload::rules, StackRulesPayload::new);

    public static void registerServer() {
        PayloadTypeRegistry.playS2C().register(TYPE, CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!ServerPlayNetworking.canSend(handler.player, TYPE)) {
                handler.disconnect(Component.literal("StackPlus is required on both client and server."));
                return;
            }
            ServerPlayNetworking.send(handler.player, new StackRulesPayload(StackLimitConfig.exportStackRules()));
        });
    }

    public record StackRulesPayload(String rules) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private StackRulesNetworking() {
    }
}
