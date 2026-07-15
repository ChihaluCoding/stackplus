package chihalu.stackplus.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Optional;

public final class StackPlusCustomFont {
    public static final Identifier BITMAP_RESOURCE_ID = Identifier.of("stackplus", "textures/font/custom.png");
    private static final String SUPPORTED_CHARACTERS = "0123456789KMBkmbxX+.,-";
    private static final String SIZE_REFERENCE_CHARACTERS = "0123456789KMBkmbxX";
    private static final int CELL_SIZE = 32;
    private static final int TARGET_GLYPH_HEIGHT = 29;
    private static final int GLYPH_BOTTOM = 31;
    private static final Path DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve("stackplus").resolve("fonts");
    private static final Path CUSTOM_TTF_FILE = DIRECTORY.resolve("custom.ttf");
    private static final Path CUSTOM_OTF_FILE = DIRECTORY.resolve("custom.otf");
    private static final Path CUSTOM_BITMAP_FILE = DIRECTORY.resolve("custom.png");

    private StackPlusCustomFont() {
    }

    public static Path getDirectory() {
        try {
            Files.createDirectories(DIRECTORY);
        } catch (IOException ignored) {
        }
        return DIRECTORY;
    }

    public static Optional<Path> findFontFile() {
        Path directory = getDirectory();
        if (Files.isRegularFile(CUSTOM_TTF_FILE)) {
            return Optional.of(CUSTOM_TTF_FILE);
        }
        if (Files.isRegularFile(CUSTOM_OTF_FILE)) {
            return Optional.of(CUSTOM_OTF_FILE);
        }
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.endsWith(".ttf") || name.endsWith(".otf");
                    })
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .findFirst();
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    public static void openDirectory() {
        Util.getOperatingSystem().open(getDirectory());
    }

    public static boolean importFont(String dialogTitle) {
        getDirectory();
        String selectedPath;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.ttf;*.otf")).flip();
            selectedPath = TinyFileDialogs.tinyfd_openFileDialog(
                    dialogTitle, DIRECTORY.toString(), filters, "Font files (*.ttf, *.otf)", false);
        }
        if (selectedPath == null) {
            return false;
        }
        try {
            Path source = Path.of(selectedPath);
            boolean otf = source.getFileName().toString().toLowerCase().endsWith(".otf");
            Path destination = otf ? CUSTOM_OTF_FILE : CUSTOM_TTF_FILE;
            Files.deleteIfExists(otf ? CUSTOM_TTF_FILE : CUSTOM_OTF_FILE);
            if (!source.toAbsolutePath().normalize().equals(destination.toAbsolutePath().normalize())) {
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return prepareFont();
        } catch (IOException exception) {
            return false;
        }
    }

    public static boolean prepareFont() {
        Optional<Path> fontFile = findFontFile();
        if (fontFile.isEmpty()) {
            return false;
        }
        try {
            Font sourceFont;
            try (InputStream input = Files.newInputStream(fontFile.get())) {
                sourceFont = Font.createFont(Font.TRUETYPE_FONT, input);
            }
            BufferedImage image = new BufferedImage(CELL_SIZE * SUPPORTED_CHARACTERS.length(), CELL_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            FontRenderContext context = graphics.getFontRenderContext();
            Font measuringFont = sourceFont.deriveFont(Font.PLAIN, 64.0F);
            Rectangle measuringBounds = getGlyphBounds(measuringFont, context, SIZE_REFERENCE_CHARACTERS);
            float normalizedSize = 64.0F * TARGET_GLYPH_HEIGHT / Math.max(1, measuringBounds.height);
            Font font = sourceFont.deriveFont(Font.PLAIN, normalizedSize);
            Rectangle glyphBounds = getGlyphBounds(font, context, SUPPORTED_CHARACTERS);
            int baseline = GLYPH_BOTTOM - glyphBounds.y - glyphBounds.height;
            graphics.setFont(font);
            graphics.setColor(Color.WHITE);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            for (int index = 0; index < SUPPORTED_CHARACTERS.length(); index++) {
                String character = String.valueOf(SUPPORTED_CHARACTERS.charAt(index));
                Rectangle bounds = font.createGlyphVector(context, character).getPixelBounds(context, 0.0F, 0.0F);
                int x = index * CELL_SIZE + (CELL_SIZE - bounds.width) / 2 - bounds.x;
                graphics.drawString(character, x, baseline);
            }
            graphics.dispose();
            ImageIO.write(image, "png", CUSTOM_BITMAP_FILE.toFile());
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public static Optional<Path> getBitmapFile() {
        if (findFontFile().isPresent()) {
            prepareFont();
        }
        return Files.isRegularFile(CUSTOM_BITMAP_FILE) ? Optional.of(CUSTOM_BITMAP_FILE) : Optional.empty();
    }

    private static Rectangle getGlyphBounds(Font font, FontRenderContext context, String characters) {
        Rectangle combined = null;
        for (int index = 0; index < characters.length(); index++) {
            GlyphVector glyph = font.createGlyphVector(context, String.valueOf(characters.charAt(index)));
            Rectangle bounds = glyph.getPixelBounds(context, 0.0F, 0.0F);
            combined = combined == null ? bounds : combined.union(bounds);
        }
        return combined == null ? new Rectangle(0, 0, 1, 1) : combined;
    }
}
