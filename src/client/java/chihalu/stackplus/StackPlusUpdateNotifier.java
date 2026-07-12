package chihalu.stackplus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StackPlusUpdateNotifier {
    private static final String MODRINTH_VERSIONS_URL = "https://api.modrinth.com/v2/project/stackplus/version";
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

    private static void checkOnJoin(MinecraftClient client) {
        if (!StackLimitConfig.isUpdateNotificationsEnabled() || !checkStartedThisSession.compareAndSet(false, true)) {
            return;
        }

        String currentModVersion = getCurrentModVersion();
        String currentGameVersion = getCurrentGameVersion();
        CompletableFuture.supplyAsync(() -> fetchLatestVersion(currentModVersion, currentGameVersion))
                .thenAccept(update -> update.ifPresent(value -> client.execute(() -> pendingUpdate = value)))
                .exceptionally(exception -> {
                    StackPlus.LOGGER.warn("StackPlus の更新通知取得に失敗しました", exception);
                    return null;
                });
    }

    private static Optional<UpdateNotice> fetchLatestVersion(String currentModVersion, String currentGameVersion) {
        try {
            HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            String gameVersions = URLEncoder.encode("[\"" + currentGameVersion + "\"]", StandardCharsets.UTF_8);
            String loaders = URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MODRINTH_VERSIONS_URL + "?game_versions=" + gameVersions + "&loaders=" + loaders))
                    .header("User-Agent", "StackPlus/" + currentModVersion + " (github:ChihaluCoding)")
                    .timeout(Duration.ofSeconds(15)).GET().build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                StackPlus.LOGGER.warn("StackPlus 更新通知: Modrinth API がステータス {} を返しました", response.statusCode());
                return Optional.empty();
            }

            JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
            if (versions.isEmpty()) {
                return Optional.empty();
            }
            JsonObject latest = versions.get(0).getAsJsonObject();
            String latestVersion = getString(latest, "version_number");
            if (latestVersion.isBlank() || compareVersions(latestVersion, currentModVersion) <= 0
                    || latestVersion.equalsIgnoreCase(StackLimitConfig.getLastNotifiedReleaseVersion(currentGameVersion))) {
                return Optional.empty();
            }
            return Optional.of(new UpdateNotice(latestVersion));
        } catch (IOException | IllegalStateException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            StackPlus.LOGGER.warn("StackPlus の更新通知データ取得に失敗しました", exception);
            return Optional.empty();
        }
    }

    private static String getCurrentModVersion() {
        return FabricLoader.getInstance().getModContainer("stackplus")
                .map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("0.0.0");
    }

    private static String getCurrentGameVersion() {
        return SharedConstants.getGameVersion().name();
    }

    private static void flushPendingUpdate(MinecraftClient client) {
        UpdateNotice update = pendingUpdate;
        if (update == null || client.player == null || !StackLimitConfig.isUpdateNotificationsEnabled()) {
            return;
        }
        pendingUpdate = null;
        sendUpdateMessage(client, update);
        StackLimitConfig.setLastNotifiedReleaseVersion(getCurrentGameVersion(), update.releaseVersion());
    }

    private static void sendUpdateMessage(MinecraftClient client, UpdateNotice update) {
        MutableText message = Text.translatable("chat.stackplus.update.prefix").formatted(Formatting.GOLD)
                .append(Text.translatable("chat.stackplus.update.version", update.releaseVersion()).formatted(Formatting.GOLD))
                .append(Text.translatable("chat.stackplus.update.released").formatted(Formatting.GOLD));
        addChatMessage(client, message);

        MutableText links = Text.empty();
        links.append(Text.translatable("chat.stackplus.update.download").styled(style -> style.withColor(Formatting.YELLOW)
                .withUnderline(true).withClickEvent(new ClickEvent.OpenUrl(URI.create(MODRINTH_PAGE_URL)))));
        links.append(Text.literal(" / "));
        links.append(Text.translatable("chat.stackplus.update.release_note").styled(style -> style.withColor(Formatting.AQUA)
                .withUnderline(true).withClickEvent(new ClickEvent.OpenUrl(URI.create(RELEASE_NOTE_URL)))));
        addChatMessage(client, links);
        addChatMessage(client, Text.translatable("chat.stackplus.update.disable_hint").formatted(Formatting.GRAY));
    }

    private static void addChatMessage(MinecraftClient client, Text message) {
        if (client.player != null) {
            client.player.sendMessage(message, false);
        }
    }

    private static String getString(JsonObject object, String key) {
        JsonElement element = object == null ? null : object.get(key);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
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
        return normalized.startsWith("v") || normalized.startsWith("V") ? normalized.substring(1) : normalized;
    }

    private static int parseVersionPart(String part) {
        StringBuilder digits = new StringBuilder();
        for (int index = 0; index < part.length() && Character.isDigit(part.charAt(index)); index++) {
            digits.append(part.charAt(index));
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
