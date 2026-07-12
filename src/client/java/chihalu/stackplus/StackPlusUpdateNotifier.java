package chihalu.stackplus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StackPlusUpdateNotifier {
    private static final String MODRINTH_SLUG = "stackplus";
    private static final String MODRINTH_PROJECT_URL = "https://modrinth.com/mod/" + MODRINTH_SLUG;
    private static final String MODRINTH_VERSIONS_URL = "https://api.modrinth.com/v2/project/" + MODRINTH_SLUG + "/version";
    private static final String FABRIC_LOADER = "fabric";
    private static final int MAX_CHANGELOG_LINES = 5;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private static final AtomicBoolean checkStartedThisSession = new AtomicBoolean();

    private StackPlusUpdateNotifier() {
    }

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> checkOnJoin(client));
    }

    private static void checkOnJoin(MinecraftClient client) {
        if (!StackLimitConfig.isUpdateNotificationsEnabled() || !checkStartedThisSession.compareAndSet(false, true)) {
            return;
        }

        String currentModVersion = getCurrentModVersion();
        String gameVersion = SharedConstants.getGameVersion().name();
        HttpRequest request = HttpRequest.newBuilder(buildVersionsUri(gameVersion))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("User-Agent", "StackPlus/" + currentModVersion + " (https://github.com/ChihaluCoding/StackPlus)")
                .GET()
                .build();

        CompletableFuture.supplyAsync(() -> fetchLatestUpdate(request, currentModVersion))
                .thenAccept(update -> update.ifPresent(value -> client.execute(() -> sendUpdateMessage(client, value))))
                .exceptionally(exception -> {
                    StackPlus.LOGGER.warn("StackPlus の更新確認に失敗しました", exception);
                    return null;
                });
    }

    private static URI buildVersionsUri(String gameVersion) {
        String loaders = encodeJsonArray(FABRIC_LOADER);
        String gameVersions = encodeJsonArray(gameVersion);
        return URI.create(MODRINTH_VERSIONS_URL + "?loaders=" + loaders + "&game_versions=" + gameVersions);
    }

    private static String encodeJsonArray(String value) {
        return URLEncoder.encode("[\"" + value + "\"]", StandardCharsets.UTF_8);
    }

    private static Optional<ModrinthUpdate> fetchLatestUpdate(HttpRequest request, String currentModVersion) {
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                StackPlus.LOGGER.warn("Modrinth API がエラーを返しました: HTTP {}", response.statusCode());
                return Optional.empty();
            }

            JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement element : versions) {
                JsonObject version = element.getAsJsonObject();
                if (!"listed".equals(getString(version, "status")) || compareVersions(getString(version, "version_number"), currentModVersion) <= 0) {
                    continue;
                }
                return Optional.of(new ModrinthUpdate(getString(version, "version_number"), getString(version, "changelog")));
            }
        } catch (IOException | InterruptedException | IllegalStateException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            StackPlus.LOGGER.warn("Modrinth API から更新情報を取得できませんでした", exception);
        }
        return Optional.empty();
    }

    private static String getCurrentModVersion() {
        return FabricLoader.getInstance()
                .getModContainer("stackplus")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");
    }

    private static void sendUpdateMessage(MinecraftClient client, ModrinthUpdate update) {
        ClientPlayerEntity player = client.player;
        if (player == null || !StackLimitConfig.isUpdateNotificationsEnabled()) {
            return;
        }

        MutableText versionLink = Text.literal("Ver" + update.versionNumber())
                .styled(style -> style.withColor(Formatting.AQUA)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(MODRINTH_PROJECT_URL))));
        player.sendMessage(versionLink.append(Text.translatable("chat.stackplus.update.released")), false);
        player.sendMessage(Text.translatable("chat.stackplus.update.changelog_title"), false);

        List<String> changelogLines = extractChangelogLines(update.changelog());
        if (changelogLines.isEmpty()) {
            player.sendMessage(Text.translatable("chat.stackplus.update.changelog_empty"), false);
        } else {
            for (String changelogLine : changelogLines) {
                player.sendMessage(Text.literal("  - " + changelogLine).formatted(Formatting.GRAY), false);
            }
        }

        player.sendMessage(Text.translatable("chat.stackplus.update.disable_hint").formatted(Formatting.YELLOW), false);
    }

    private static List<String> extractChangelogLines(String changelog) {
        List<String> lines = new ArrayList<>();
        if (changelog == null || changelog.isBlank()) {
            return lines;
        }

        for (String rawLine : changelog.split("\\R")) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            line = stripMarkdownBullet(line);
            if (!line.isBlank()) {
                lines.add(line);
            }
            if (lines.size() >= MAX_CHANGELOG_LINES) {
                break;
            }
        }
        return lines;
    }

    private static String stripMarkdownBullet(String line) {
        if (line.startsWith("- ") || line.startsWith("* ")) {
            return line.substring(2).strip();
        }
        return line;
    }

    private static String getString(JsonObject object, String key) {
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

    private record ModrinthUpdate(String versionNumber, String changelog) {
    }
}
