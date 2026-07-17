package chihalu.stackplus;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class StackPlusIssueReportCommand {
    private static final String REPORT_DIRECTORY_NAME = "stackplus";
    private static final String REPORT_FILE_NAME = "stackplus-issue-report.txt";
    private static final String REPORT_DISPLAY_PATH = REPORT_DIRECTORY_NAME + "/" + REPORT_FILE_NAME;
    private static final String FABRIC_MODULE_ID_PREFIX = "fabric-";
    private static final String C2ME_MODULE_ID_PREFIX = "c2me-";

    private StackPlusIssueReportCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("stackplus")
                        .then(ClientCommandManager.literal("issue")
                                .then(ClientCommandManager.literal("report")
                                        .executes(context -> exportIssueReport(context.getSource()))))));
    }

    private static int exportIssueReport(FabricClientCommandSource source) {
        try {
            Path reportPath = writeIssueReport();
            sendMessage(source, "StackPlus issue report exported.");
            sendMessage(source, "Saved to: " + REPORT_DISPLAY_PATH);
            sendMessage(source, "Please attach it to your GitHub issue.");
            StackPlus.LOGGER.info("StackPlus issue report exported: {}", reportPath);
            return 1;
        } catch (IOException exception) {
            StackPlus.LOGGER.warn("Failed to export the StackPlus issue report", exception);
            sendMessage(source, "Failed to export the StackPlus issue report. Check the log for details.");
            return 0;
        }
    }

    private static Path writeIssueReport() throws IOException {
        Path reportPath = getReportPath(FabricLoader.getInstance().getGameDir());
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, buildIssueReportText(), StandardCharsets.UTF_8);
        return reportPath;
    }

    // Keep issue reports easy to find and separate from user configuration.
    static Path getReportPath(Path gameDirectory) {
        return gameDirectory.resolve(REPORT_DIRECTORY_NAME).resolve(REPORT_FILE_NAME);
    }

    private static String buildIssueReportText() {
        List<ModContainer> mods = new ArrayList<>(FabricLoader.getInstance().getAllMods());
        mods.sort(Comparator.comparing(mod -> mod.getMetadata().getId()));
        List<ModContainer> fabricModules = mods.stream()
                .filter(mod -> isFabricModule(mod.getMetadata()))
                .toList();
        List<ModContainer> c2meModules = mods.stream()
                .filter(mod -> isC2meModule(mod.getMetadata()))
                .toList();
        List<ModContainer> otherMods = mods.stream()
                .filter(mod -> !isFabricModule(mod.getMetadata()) && !isC2meModule(mod.getMetadata()))
                .toList();

        StringBuilder builder = new StringBuilder();
        builder.append("[Environment Info]").append(System.lineSeparator());
        builder.append("Fabric Loader Version: ").append(getModVersion("fabricloader")).append(System.lineSeparator());
        builder.append("Fabric API Version: ").append(getModVersion("fabric-api")).append(System.lineSeparator());
        builder.append("StackPlus Version: ").append(getModVersion("stackplus")).append(System.lineSeparator());
        builder.append("Java Version: ").append(System.getProperty("java.version", "Unknown")).append(System.lineSeparator());
        builder.append("Mod Count: ").append(mods.size()).append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("[Loaded Mods]").append(System.lineSeparator());

        appendModList(builder, otherMods);
        builder.append(System.lineSeparator());
        builder.append("[Fabric Modules]").append(System.lineSeparator());
        appendModList(builder, fabricModules);
        builder.append(System.lineSeparator());
        builder.append("[C2ME Modules]").append(System.lineSeparator());
        appendModList(builder, c2meModules);

        return builder.toString();
    }

    // Keep Fabric API components and C2ME internal modules separate from regular mods.
    private static boolean isFabricModule(ModMetadata metadata) {
        return metadata.getId().startsWith(FABRIC_MODULE_ID_PREFIX);
    }

    private static boolean isC2meModule(ModMetadata metadata) {
        return metadata.getId().startsWith(C2ME_MODULE_ID_PREFIX);
    }

    private static void appendModList(StringBuilder builder, List<ModContainer> mods) {
        for (ModContainer mod : mods) {
            ModMetadata metadata = mod.getMetadata();
            builder.append("- ")
                    .append(metadata.getName())
                    .append(" (")
                    .append(metadata.getId())
                    .append(") ")
                    .append(metadata.getVersion().getFriendlyString())
                    .append(System.lineSeparator());
        }
    }

    private static String getModVersion(String modId) {
        return FabricLoader.getInstance()
                .getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("Not installed");
    }

    private static void sendMessage(FabricClientCommandSource source, String message) {
        source.sendFeedback(Text.literal(message).formatted(Formatting.YELLOW));
    }
}
