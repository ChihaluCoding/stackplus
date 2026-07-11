package chihalu.customstacklimit.network;

import chihalu.customstacklimit.StackLimitConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class StackRulesClientNetworking {
    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!client.hasSingleplayerServer()) {
                StackLimitConfig.beginRemoteSession();
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(StackRulesNetworking.TYPE, (payload, context) -> {
            if (!context.client().hasSingleplayerServer()) {
                StackLimitConfig.applyServerStackRules(payload.rules());
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> StackLimitConfig.endRemoteSession());
    }

    private StackRulesClientNetworking() {
    }
}
