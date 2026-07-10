package chihalu.stackplus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StackPlusUpdateNotifier {
    private static final String MODRINTH_VERSIONS_URL =
            "https://api.modrinth.com/v2/project/stackplus/version?game_versions=[\"%s\"]&loaders=[\"fabric\"]";
    private static final String MODRINTH_PAGE_URL = "https://modrinth.com/mod/stackplus";
    private static final String RELEASE_NOTE_URL = "https://chihalucoding.github.io/stackplus-release-note/";
    private static final AtomicBoolean checkStartedThisSession = new AtomicBoolean();
    private static volatile UpdateNotice pendingUpdate;

    private StackPlusUpdateNotifier() {
    }

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> checkOnJoin(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> pendingUpdate = null);
        ClientTickEvents.END_CLIENT_TICK.register(StackPlusUpdateNotifier::flushPendingUpdate);
    }

    private static void checkOnJoin(Minecraft client) {
        if (!StackLimitConfig.isUpdateNotificationsEnabled() || !checkStartedThisSession.compareAndSet(false, true)) {
            return;
        }

        String currentModVersion = getCurrentModVersion();
        String currentGameVersion = getCurrentGameVersion();
        CompletableFuture.supplyAsync(() -> fetchLatestVersion(currentModVersion, currentGameVersion))
                .thenAccept(update -> update.ifPresent(value -> client.execute(() -> pendingUpdate = value)))
                .exceptionally(exception -> {
                    CustomStackLimit.LOGGER.warn("StackPlus の更新通知取得に失敗しました", exception);
                    return null;
                });
    }

    private static Optional<UpdateNotice> fetchLatestVersion(String currentModVersion, String currentGameVersion) {
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String gameVersions = java.net.URLEncoder.encode("[\"" + currentGameVersion + "\"]", java.nio.charset.StandardCharsets.UTF_8);
            String loaders = java.net.URLEncoder.encode("[\"fabric\"]", java.nio.charset.StandardCharsets.UTF_8);
            String apiUrl = MODRINTH_VERSIONS_URL + "?game_versions=" + gameVersions + "&loaders=" + loaders;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("User-Agent", "StackPlus/" + currentModVersion + " (github:ChihaluCoding)")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                CustomStackLimit.LOGGER.warn("StackPlus 更新通知: Modrinth API がステータス {} を返しました", response.statusCode());
                return Optional.empty();
            }

            JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
            if (versions.isEmpty()) {
                return Optional.empty();
            }

            JsonObject latest = versions.get(0).getAsJsonObject();
            String latestVersion = getString(latest, "version_number");
            if (latestVersion.isBlank() || compareVersions(latestVersion, currentModVersion) <= 0) {
                return Optional.empty();
            }

            String lastNotified = StackLimitConfig.getLastNotifiedReleaseVersion(currentGameVersion);
            if (latestVersion.equalsIgnoreCase(lastNotified)) {
                return Optional.empty();
            }

            return Optional.of(new UpdateNotice(latestVersion));
        } catch (IOException | IllegalStateException | InterruptedException exception) {
            CustomStackLimit.LOGGER.warn("StackPlus の更新通知データ取得に失敗しました", exception);
            return Optional.empty();
        }
    }

    private static String getCurrentModVersion() {
        return FabricLoader.getInstance()
                .getModContainer("stackplus")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");
    }

    private static String getCurrentGameVersion() {
        return SharedConstants.getCurrentVersion().name();
    }

    private static void flushPendingUpdate(Minecraft client) {
        UpdateNotice update = pendingUpdate;
        if (update == null || client.player == null || client.gui == null || !StackLimitConfig.isUpdateNotificationsEnabled()) {
            return;
        }

        pendingUpdate = null;
        sendUpdateMessage(client, update);
        StackLimitConfig.setLastNotifiedReleaseVersion(getCurrentGameVersion(), update.releaseVersion());
    }

    private static void sendUpdateMessage(Minecraft client, UpdateNotice update) {
        MutableComponent message = Component.translatable("chat.stackplus.update.prefix")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.translatable("chat.stackplus.update.version", update.releaseVersion())
                        .withStyle(ChatFormatting.GOLD))
                .append(Component.translatable("chat.stackplus.update.released")
                        .withStyle(ChatFormatting.GOLD));

        addChatMessage(client, message);

        MutableComponent links = Component.empty();
        links.append(Component.translatable("chat.stackplus.update.download")
                .withStyle(style -> style.withColor(ChatFormatting.YELLOW)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(MODRINTH_PAGE_URL)))));
        links.append(Component.literal(" / "));
        links.append(Component.translatable("chat.stackplus.update.release_note")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(RELEASE_NOTE_URL)))));
        addChatMessage(client, links);

        addChatMessage(client, Component.translatable("chat.stackplus.update.disable_hint")
                .withStyle(ChatFormatting.GRAY));
    }

    private static void addChatMessage(Minecraft client, Component message) {
        client.gui.getChat().addMessage(message, null, null);
    }

    private static String getString(JsonObject object, String key) {
        if (object == null) {
            return "";
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return "";
        }
        return element.getAsString();
    }

    private static int compareVersions(String left, String right) {
        String[] leftParts = normalizeVersion(left).split("\\.");
        String[] rightParts = normalizeVersion(right).split("\\.");
        int maxLength = Math.max(leftParts.length, rightParts.length);
        for (int index = 0; index < maxLength; index++) {
            int leftValue = index < leftParts.length ? parseVersionPart(leftParts[index]) : 0;
            int rightValue = index < rightParts.length ? parseVersionPart(rightParts[index]) : 0;
            if (leftValue != rightValue) {
                return Integer.compare(leftValue, rightValue);
            }
        }
        return left.compareToIgnoreCase(right);
    }

    private static String normalizeVersion(String version) {
        String normalized = version.strip();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            return normalized.substring(1);
        }
        return normalized;
    }

    private static int parseVersionPart(String part) {
        StringBuilder digits = new StringBuilder();
        for (int index = 0; index < part.length(); index++) {
            char character = part.charAt(index);
            if (!Character.isDigit(character)) {
                break;
            }
            digits.append(character);
        }
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }

    private record UpdateNotice(String releaseVersion) {
    }
}
