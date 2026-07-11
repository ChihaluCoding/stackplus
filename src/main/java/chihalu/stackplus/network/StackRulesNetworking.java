package chihalu.stackplus.network;

import chihalu.stackplus.StackLimitConfig;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class StackRulesNetworking {
    public static final CustomPayload.Id<StackRulesPayload> ID =
            new CustomPayload.Id<>(Identifier.of("stackplus", "stack_rules"));
    public static final PacketCodec<RegistryByteBuf, StackRulesPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.string(1_048_576), StackRulesPayload::rules, StackRulesPayload::new);

    public static void registerServer() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!ServerPlayNetworking.canSend(handler.player, ID)) {
                handler.disconnect(Text.literal("StackPlus is required on both client and server."));
                return;
            }
            ServerPlayNetworking.send(handler.player, new StackRulesPayload(StackLimitConfig.exportStackRules()));
        });
    }

    public record StackRulesPayload(String rules) implements CustomPayload {
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    private StackRulesNetworking() {
    }
}

