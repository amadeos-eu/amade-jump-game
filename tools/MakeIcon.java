import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Draws the program icon of Jump from the game's own pixel art: the player in mid-jump over a strip of
 * grass, a coin beside them, under a blue sky. The picture is 24 × 24 pixels, enlarged by whole factors
 * so its pixels stay square: twice for 48, ten times (and centred) for 256.
 *
 * <pre>
 * java tools/MakeIcon.java 48 src/main/resources/icons/amade-jump-game.png
 * java tools/MakeIcon.java 256 build/icon-256.png
 * python3 -c "from PIL import Image; Image.open('build/icon-256.png').save('packaging/windows/amade-jump-game.ico',
 *     sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])"
 * </pre>
 *
 * Run it in the repository's folder, after {@code tools/MakeSprites.java}. The files are part of the
 * repository; run this again only to change the icon.
 */
public class MakeIcon {
    static final String ART = "src/main/resources/eu/amadeos/games/jump/";
    static final int CELL = 16;

    public static void main(String[] args) throws IOException {
        int size = Integer.parseInt(args[0]);
        File file = new File(args[1]);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();
        BufferedImage small = picture();
        int scale = Math.max(1, size / small.getWidth());
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int margin = (size - scale * small.getWidth()) / 2;
        g.drawImage(small, margin, margin, scale * small.getWidth(), scale * small.getHeight(), null);
        g.dispose();
        ImageIO.write(image, "png", file);
    }

    /** The icon at one pixel per pixel of the art. */
    static BufferedImage picture() throws IOException {
        BufferedImage sprites = ImageIO.read(new File(ART + "sprites.png"));
        BufferedImage tiles = ImageIO.read(new File(ART + "levels/tiles.png"));
        BufferedImage icon = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        // Sky in bands, a dark frame around it.
        Color[] sky = {new Color(0x5AA8F0), new Color(0x78BAF4), new Color(0x96CCF8), new Color(0xB4DDFC)};
        for (int i = 0; i < sky.length; i++) {
            g.setColor(sky[i]);
            g.fillRect(1, 1 + i * 4, 22, 4);
        }
        g.fillRect(1, 17, 22, 6);
        // Grass: the top rows of the grass tile, twice across.
        BufferedImage grass = tiles.getSubimage(0, 0, CELL, CELL);
        g.drawImage(grass, 1, 17, 17, 23, 0, 0, CELL, 6, null);
        g.drawImage(grass, 17, 17, 23, 23, 0, 0, 6, 6, null);
        // A small coin, pixel by pixel, and the player jumping (row 0, column 4 of the sprites).
        String[] coin = {".yyy.", "yYYYy", "yYWYy", "yYYYy", "yYYYy", ".yyy."};
        for (int y = 0; y < coin.length; y++) {
            for (int x = 0; x < coin[y].length(); x++) {
                char c = coin[y].charAt(x);
                if (c == '.') continue;
                g.setColor(c == 'y' ? new Color(0xC89020) : c == 'Y' ? new Color(0xFFD84A) : Color.WHITE);
                g.fillRect(16 + x, 4 + y, 1, 1);
            }
        }
        g.drawImage(sprites.getSubimage(4 * CELL, 0, CELL, CELL), 2, 3, null);
        g.setColor(new Color(0x1C1A2C));
        g.drawRect(0, 0, 23, 23);
        g.dispose();
        return icon;
    }
}
