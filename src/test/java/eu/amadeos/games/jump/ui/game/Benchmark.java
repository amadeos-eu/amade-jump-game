package eu.amadeos.games.jump.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.camera.Parallax;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.save.SaveFile;
import eu.amadeos.games.jump.bootstrap.JumpModule;
import eu.amadeos.games.jump.domain.Field;
import eu.amadeos.games.jump.domain.Input;
import eu.amadeos.games.jump.domain.Level;
import eu.amadeos.games.jump.repository.JumpSettings;
import eu.amadeos.games.jump.repository.Progress;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * How long a frame of each level takes to draw, and how much of it the background is, at two window sizes:
 * {@code ./gradlew benchmark}. The player runs and jumps to the right, so the camera scrolls. The frames
 * are drawn into a compatible image, as the canvas draws, but without waiting for the screen.
 */
public final class Benchmark {
    static final int WARM_UP = 60;
    static final int FRAMES = 300;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("jump");
        Messages msg = Messages.load(JumpModule.class, JumpModule.MESSAGES, Locale.ENGLISH);
        JumpGame game = new JumpGame(new Keyboard(), Gamepads.none(), msg, new JumpSettings(dir.resolve("jump.properties")),
                new Progress(new SaveFile(dir.resolve("progress.save"))), new Mixer());
        GraphicsConfiguration gc = GraphicsEnvironment.isHeadless() ? null
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        System.out.println("ms per frame          whole frame   background");
        for (int scale : new int[]{2, 3}) {
            for (int number = 1; number <= Level.FILES.size(); number++) {
                JumpGame.PlayScene play = game.play(number);
                Parallax back = Backdrop.of(play.game().level().theme(), play.game().level().height() * Field.TILE);
                int[] step = {0};
                double frame = measure(gc, scale, g -> {
                    boolean jump = step[0]++ % 90 == 0;
                    play.step(JumpGame.STEP, new Input(1, jump, true, false));
                    game.render(g, 0.5);
                    game.renderOverlay(g, 0.5);
                });
                double background = measure(gc, scale, g -> back.draw(g, play.camera().x(), play.camera().y()));
                System.out.printf(Locale.ROOT, "level %d at %4d x %4d   %6.2f        %6.2f%n", number,
                        (int) Field.WIDTH * scale, (int) Field.HEIGHT * scale, frame, background);
            }
        }
        System.exit(0);
    }

    /** Milliseconds per frame, on average, after a warm-up for the caches and the compiler. */
    private static double measure(GraphicsConfiguration gc, int scale, Consumer<Graphics2D> draw) {
        int w = (int) Field.WIDTH * scale;
        int h = (int) Field.HEIGHT * scale;
        BufferedImage image = gc != null ? gc.createCompatibleImage(w, h) : new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        long total = 0;
        for (int i = 0; i < WARM_UP + FRAMES; i++) {
            Graphics2D g = image.createGraphics();
            g.scale(scale, scale);
            long start = System.nanoTime();
            draw.accept(g);
            if (i >= WARM_UP) total += System.nanoTime() - start;
            g.dispose();
        }
        return total / 1e6 / FRAMES;
    }
}
