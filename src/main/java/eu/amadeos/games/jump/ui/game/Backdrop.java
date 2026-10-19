package eu.amadeos.games.jump.ui.game;

import eu.amadeos.gameengine.camera.Parallax;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.gameengine.sprite.Sprite;
import eu.amadeos.games.jump.domain.Field;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * The backgrounds behind the levels, one for each theme a map names: hills under a blue sky for the meadow,
 * rock pillars in the dark for the cave, clouds for the heights. Each is a {@link Parallax} of pictures
 * drawn here, pixel by pixel at one pixel per logical unit, so they look like the tiles in front of them:
 * pixel art, scaled with hard edges.
 *
 * <p>Pictures are as wide as the screen and repeat; a layer that also moves up and down is tall enough to
 * cover the screen however far up the camera goes.
 */
final class Backdrop {
    private Backdrop() {
    }

    // --8<-- [start:backdrop]
    /** The background of a theme for a world {@code worldHeight} units high. */
    static Parallax of(String theme, double worldHeight) {
        double climb = Math.max(0, worldHeight - Field.HEIGHT);
        Parallax back = new Parallax(Field.WIDTH, Field.HEIGHT);
        switch (theme) {
            case "cave" -> {
                add(back, sky(0x0E0C1A, 0x241E3A), 0, 0);
                add(back, pillars(0x2E2848, 0x3A3358, 3, 0.2, climb), 0.2, 0.2);
                add(back, pillars(0x1A1628, 0x221D34, 4, 0.45, climb), 0.45, 0.45);
            }
            case "sky" -> {
                add(back, sky(0x2E7FD8, 0xBDE4FF), 0, 0);
                // Paler than the white cloud platforms in front: the eye must not try to stand on them.
                add(back, clouds(0x8EC0EE, 5, 0.1, climb), 0.1, 0.1);
                add(back, clouds(0xB4D6F6, 6, 0.3, climb), 0.3, 0.3);
            }
            default -> {
                add(back, sky(0x5AA8F0, 0xCFEBFF), 0, 0);
                add(back, clouds(0xFFFFFF, 1, 0.05, climb), 0.05, 0.05);
                add(back, hills(0x8FB0D8, 0x7FA0CA, 160, 70, 2), 0.15, 0);
                add(back, hills(0x5DA050, 0x4A8A42, 240, 50, 7), 0.4, 0);
            }
        }
        return back;
    }

    /**
     * Adds a picture as bands of rows: rows with nothing in them are left out, rows that are solid all the
     * way across become a picture of their own, and the rest, the rows with see-through pixels, another.
     * Copying a solid picture needs no test of each pixel for transparency, and an empty row costs nothing:
     * the hills cost a third of a full-screen picture, the stalactites half.
     */
    private static void add(Parallax back, BufferedImage image, double factor, double factorY) {
        int row = 0;
        while (row < image.getHeight()) {
            Rows kind = rows(image, row);
            int end = row + 1;
            while (end < image.getHeight() && same(image, kind, end)) end++;
            if (kind != Rows.EMPTY) {
                back.layer(picture(image.getSubimage(0, row, image.getWidth(), end - row)), factor, factorY, row);
            }
            row = end;
        }
    }

    private enum Rows { EMPTY, SOLID, SEE_THROUGH }

    /**
     * Whether a row belongs to the band of {@code kind}. A see-through band takes in short gaps of empty
     * rows, and solid rows above it: one band more costs more than copying a few rows for nothing.
     */
    private static boolean same(BufferedImage image, Rows kind, int row) {
        Rows here = rows(image, row);
        if (here == kind) return true;
        if (kind != Rows.SEE_THROUGH) return false;
        if (here == Rows.SOLID) return false;
        int gap = row;
        while (gap < image.getHeight() && gap < row + GAP && rows(image, gap) == Rows.EMPTY) gap++;
        return gap < image.getHeight() && gap < row + GAP;
    }

    /** Empty rows shorter than this between see-through rows are copied with them. */
    private static final int GAP = 24;

    private static Rows rows(BufferedImage image, int row) {
        boolean seen = false;
        boolean hidden = false;
        for (int x = 0; x < image.getWidth(); x++) {
            int alpha = image.getRGB(x, row) >>> 24;
            seen |= alpha != 0;
            hidden |= alpha != 255;
        }
        return !seen ? Rows.EMPTY : hidden ? Rows.SEE_THROUGH : Rows.SOLID;
    }
    // --8<-- [end:backdrop]

    /** A sky from {@code top} to {@code bottom} colour, in bands eight units high, as old games did it. */
    private static BufferedImage sky(int top, int bottom) {
        BufferedImage image = image((int) Field.HEIGHT);
        int bands = (int) Field.HEIGHT / 8;
        for (int y = 0; y < image.getHeight(); y++) {
            int band = Math.min(bands - 1, y / 8);
            int colour = mix(top, bottom, band / (double) (bands - 1));
            for (int x = 0; x < image.getWidth(); x++) image.setRGB(x, y, colour);
        }
        return image;
    }

    /** Hills whose tops rise and fall around {@code base}, with a lighter rim; seeded, so always the same. */
    private static BufferedImage hills(int fill, int rim, int base, int height, long seed) {
        BufferedImage image = image((int) Field.HEIGHT);
        int width = image.getWidth();
        SeededRandom random = new SeededRandom(seed);
        // A few waves whose lengths divide the width: the picture repeats without a seam.
        double[] amplitude = new double[3];
        double[] phase = new double[3];
        for (int i = 0; i < 3; i++) {
            amplitude[i] = height * (0.5 - i * 0.15) * (0.6 + random.nextDouble() * 0.4);
            phase[i] = random.nextDouble() * 2 * Math.PI;
        }
        for (int x = 0; x < width; x++) {
            double top = base;
            for (int i = 0; i < 3; i++) top -= amplitude[i] * Math.sin(2 * Math.PI * (i + 1) * x / width + phase[i]);
            int surface = (int) Math.round(top);
            for (int y = Math.max(0, surface); y < image.getHeight(); y++) {
                image.setRGB(x, y, 0xFF000000 | (y - surface < 2 ? rim : fill));
            }
        }
        return image;
    }

    /** Pillars and hanging stalactites in the dark, for the cave. */
    private static BufferedImage pillars(int fill, int rim, long seed, double factor, double climb) {
        int height = (int) Math.ceil(Field.HEIGHT + climb * factor);
        BufferedImage image = image(height);
        SeededRandom random = new SeededRandom(seed);
        int width = image.getWidth();
        int x = 0;
        while (x < width - 40) {
            int w = 18 + random.nextInt(30);
            int hang = 40 + random.nextInt(140);
            int stand = 30 + random.nextInt(120);
            spike(image, x, w, 0, hang, fill, rim, true);
            spike(image, x + random.nextInt(20), w, height - stand, stand, fill, rim, false);
            x += w + 20 + random.nextInt(60);
        }
        return image;
    }

    /** A rock narrowing to a point: down from the ceiling, or up from the floor. */
    private static void spike(BufferedImage image, int left, int width, int top, int length, int fill, int rim,
                              boolean hanging) {
        for (int y = 0; y < length; y++) {
            double along = hanging ? y / (double) length : 1 - y / (double) length;
            int half = (int) Math.round(width / 2.0 * (1 - along * along));
            int middle = left + width / 2;
            for (int x = middle - half; x <= middle + half; x++) {
                if (x < 0 || x >= image.getWidth() || top + y >= image.getHeight()) continue;
                image.setRGB(x, top + y, 0xFF000000 | (x - (middle - half) < 2 ? rim : fill));
            }
        }
    }

    /** Clouds of round blobs, scattered over a picture tall enough for the climb. */
    private static BufferedImage clouds(int colour, long seed, double factor, double climb) {
        int height = (int) Math.ceil(Field.HEIGHT + climb * factor);
        BufferedImage image = image(height);
        SeededRandom random = new SeededRandom(seed);
        int count = 5 + height / 90;
        for (int i = 0; i < count; i++) {
            int cx = random.nextInt(image.getWidth());
            int cy = 20 + random.nextInt(Math.max(1, height - 140));
            int blobs = 3 + random.nextInt(3);
            for (int b = 0; b < blobs; b++) {
                int r = 8 + random.nextInt(10);
                disc(image, cx + b * 14 - blobs * 7, cy - random.nextInt(8), r, colour);
            }
        }
        return image;
    }

    /** A filled circle that wraps around the left and right edges, so the picture tiles. */
    private static void disc(BufferedImage image, int cx, int cy, int r, int colour) {
        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                if (x * x + y * y > r * r || cy + y < 0 || cy + y >= image.getHeight()) continue;
                int px = Math.floorMod(cx + x, image.getWidth());
                image.setRGB(px, cy + y, 0xFF000000 | colour);
            }
        }
    }

    private static BufferedImage image(int height) {
        return new BufferedImage((int) Field.WIDTH, height, BufferedImage.TYPE_INT_ARGB);
    }

    private static Sprite picture(BufferedImage image) {
        return Sprite.of(image, image.getWidth(), image.getHeight()).hardEdges();
    }

    private static int mix(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }
}
