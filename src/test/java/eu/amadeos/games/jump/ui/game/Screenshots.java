package eu.amadeos.games.jump.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.camera.Camera;
import eu.amadeos.gameengine.camera.Parallax;
import eu.amadeos.gameengine.collision.Box;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.physics.Body;
import eu.amadeos.gameengine.save.SaveFile;
import eu.amadeos.gameengine.sprite.SpriteSheet;
import eu.amadeos.games.jump.bootstrap.JumpModule;
import eu.amadeos.games.jump.domain.Field;
import eu.amadeos.games.jump.domain.Input;
import eu.amadeos.games.jump.domain.Level;
import eu.amadeos.games.jump.domain.LevelResult;
import eu.amadeos.games.jump.domain.Stage;
import eu.amadeos.games.jump.repository.JumpSettings;
import eu.amadeos.games.jump.repository.Progress;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The screenshots of chapter 27, drawn without a screen: the title, the choice of a level, the three
 * levels, the camera's dead zone, the goal, the layers of a background and the gaps between tiles that
 * {@code drawSeamless} closes. {@code ./gradlew screenshots -Pshots=<folder>}.
 */
public final class Screenshots {
    private final long[] now = {0};
    private final Keyboard keyboard = new Keyboard(() -> now[0]);
    private final JumpGame game;

    private Screenshots() throws Exception {
        Messages msg = Messages.load(JumpModule.class, JumpModule.MESSAGES, Locale.ENGLISH);
        Path dir = Files.createTempDirectory("jump");
        Progress progress = new Progress(new SaveFile(dir.resolve("progress.save")));
        // Two levels finished, so the choice shows times, coins and one lock.
        progress.finished(new LevelResult(1, 71.4, 37, 41, 2), 3);
        progress.finished(new LevelResult(2, 98.2, 30, 38, 5), 3);
        game = new JumpGame(keyboard, Gamepads.none(), msg, new JumpSettings(dir.resolve("jump.properties")),
                progress, new Mixer());
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "screenshots");
        dir.mkdirs();
        Screenshots shots = new Screenshots();
        shots.steps(14 * 120);
        shots.save(new File(dir, "title.png"), true);
        shots.tap(KeyEvent.VK_SPACE);
        shots.tap(KeyEvent.VK_RIGHT);
        shots.steps(60);
        shots.save(new File(dir, "choice.png"), true);

        // The meadow: a jump over the first beetle.
        JumpGame.PlayScene meadow = shots.play(1, 560, 320, Field.RUN_SPEED);
        shots.jump(meadow, 1, 4, 26);
        shots.save(new File(dir, "meadow.png"), true);
        shots.camera(new File(dir, "camera.png"), meadow);

        // The caves: three beetles after the second checkpoint.
        JumpGame.PlayScene caves = shots.play(2, 0, 0, 0);
        shots.before(caves, 1600, 64);
        shots.jump(caves, 1, 4, 26);
        shots.save(new File(dir, "caves.png"), true);

        // The heights: the camera has climbed with the player.
        JumpGame.PlayScene heights = shots.play(3, 88, 832, 0);
        shots.steps(130);
        shots.jump(heights, 1, 40, 30);
        shots.save(new File(dir, "heights.png"), true);

        // The goal of the meadow.
        JumpGame.PlayScene goal = shots.play(1, 0, 0, 0);
        Box flag = goal.game().level().goal();
        Stage.put(goal.game(), flag.x() - 30, flag.bottom(), 0);
        goal.camera().jumpTo(flag.x(), flag.y());
        for (int i = 0; i < 40; i++) goal.step(JumpGame.STEP, Input.run(1));
        shots.steps((int) (JumpGame.CHEER_SECONDS * 120) + 60);
        shots.save(new File(dir, "cleared.png"), true);

        parallax(new File(dir, "parallax.png"));
        seamless(new File(dir, "seamless.png"));
        System.exit(0);
    }

    /** Level {@code number} with the player put at {@code x}, {@code feet}, and the camera on it. */
    private JumpGame.PlayScene play(int number, double x, double feet, double vx) {
        JumpGame.PlayScene play = game.play(number);
        steps(280);                     // past the level's name at the start
        if (feet > 0) {
            Stage.put(play.game(), x, feet, vx);
            Body body = play.game().player().body();
            play.camera().jumpTo(body.centerX(), body.centerY());
        }
        return play;
    }

    /** Puts the player {@code distance} before the nearest enemy right of {@code from}, running towards it. */
    private void before(JumpGame.PlayScene play, double from, double distance) {
        Body beetle = play.game().enemies().stream().map(e -> e.body()).filter(b -> b.x > from)
                .min(java.util.Comparator.comparingDouble(b -> b.x)).orElseThrow();
        Stage.put(play.game(), beetle.x - distance, beetle.bottom(), Field.RUN_SPEED);
        Body body = play.game().player().body();
        play.camera().jumpTo(body.centerX(), body.centerY());
    }

    /** Runs {@code move} for {@code before} steps, then jumps and holds it for {@code steps} steps. */
    private void jump(JumpGame.PlayScene play, int move, int before, int steps) {
        for (int i = 0; i < before; i++) play.step(JumpGame.STEP, Input.run(move));
        play.step(JumpGame.STEP, new Input(move, true, true, false));
        for (int i = 0; i < steps; i++) play.step(JumpGame.STEP, new Input(move, false, true, false));
    }

    /** The meadow again, with the camera's dead zone and the middle of the view drawn over it. */
    private void camera(File file, JumpGame.PlayScene play) throws Exception {
        write(file, g -> {
            game.render(g, 1);
            Camera camera = play.camera();
            g.setColor(new Color(255, 255, 255, 60));
            double w = 48;
            double h = 96;
            g.fill(new Rectangle2D.Double((Field.WIDTH - w) / 2, (Field.HEIGHT - h) / 2, w, h));
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new Rectangle2D.Double((Field.WIDTH - w) / 2, (Field.HEIGHT - h) / 2, w, h));
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{4, 4}, 0));
            g.draw(new Line2D.Double(Field.WIDTH / 2, 0, Field.WIDTH / 2, Field.HEIGHT));
            g.draw(new Line2D.Double(0, Field.HEIGHT / 2, Field.WIDTH, Field.HEIGHT / 2));
            Body body = play.game().player().body();
            g.setColor(new Color(255, 80, 60));
            g.fill(new Rectangle2D.Double(body.centerX() - camera.x() - 2, body.centerY() - camera.y() - 2, 4, 4));
        });
    }

    /** The layers of the meadow's background by their factor, each picture on its own. */
    private static void parallax(File file) throws Exception {
        Parallax back = Backdrop.of("meadow", Field.HEIGHT);
        List<Double> factors = back.layers().stream().map(Parallax.Layer::factor).distinct().toList();
        int w = (int) Field.WIDTH;
        int h = (int) Field.HEIGHT;
        int gap = 12;
        BufferedImage image = new BufferedImage(2 * w + 3 * gap, 2 * (h + gap + 24) + gap, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x1C1C24));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        for (int i = 0; i < factors.size(); i++) {
            double factor = factors.get(i);
            int x = gap + (i % 2) * (w + gap);
            int y = gap + (i / 2) * (h + gap + 24);
            Graphics2D cell = (Graphics2D) g.create(x, y, w, h);
            cell.setColor(new Color(0x30343E));
            cell.fillRect(0, 0, w, h);
            Parallax alone = new Parallax(Field.WIDTH, Field.HEIGHT);
            int pictures = 0;
            for (Parallax.Layer layer : back.layers()) {
                if (layer.factor() != factor) continue;
                alone.layer(layer.picture(), layer.factor(), layer.factorY(), layer.y());
                pictures++;
            }
            alone.draw(cell, 0, 0);
            cell.dispose();
            g.setColor(Color.WHITE);
            g.drawString("factor " + factor + (pictures > 1 ? " · " + pictures + " pictures" : ""), x + 6, y + h + 22);
        }
        g.dispose();
        ImageIO.write(image, "png", file);
    }

    /**
     * A row of grass tiles at 2.7 screen pixels per unit, drawn with {@code draw} above and with
     * {@code drawSeamless} below, enlarged three times: the lines between the tiles above are the gaps.
     */
    private static void seamless(File file) throws Exception {
        SpriteSheet tiles = SpriteSheet.load(Screenshots.class, Level.FOLDER + "tiles.png", 8, 4, Field.TILE,
                Field.TILE).hardEdges();
        double scale = 2.7;
        int count = 12;
        BufferedImage strip = new BufferedImage((int) (count * Field.TILE * scale) + 20, (int) (Field.TILE * scale * 2) + 40,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = strip.createGraphics();
        g.setColor(new Color(0x101018));
        g.fillRect(0, 0, strip.getWidth(), strip.getHeight());
        g.translate(10.3, 10);
        g.scale(scale, scale);
        for (int i = 0; i < count; i++) tiles.frame(1).draw(g, i * Field.TILE, 0);
        for (int i = 0; i < count; i++) tiles.frame(1).drawSeamless(g, i * Field.TILE, Field.TILE + 4);
        g.dispose();
        int zoom = 3;
        int part = strip.getWidth();
        BufferedImage image = new BufferedImage(part * zoom, strip.getHeight() * zoom, BufferedImage.TYPE_INT_RGB);
        Graphics2D big = image.createGraphics();
        big.drawImage(strip, 0, 0, part * zoom, strip.getHeight() * zoom, 0, 0, part, strip.getHeight(), null);
        big.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        big.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        big.setColor(Color.WHITE);
        big.drawString("above: draw · below: drawSeamless", 40, strip.getHeight() * zoom - 20);
        big.dispose();
        ImageIO.write(image, "png", file);
    }

    private void tap(int key) {
        keyboard.pressed(key, now[0]);
        keyboard.released(key, now[0] + 1_000_000);
        steps(5);
    }

    private void steps(int n) {
        for (int i = 0; i < n; i++) {
            now[0] += (long) (JumpGame.STEP * 1e9);
            game.update(JumpGame.STEP);
        }
    }

    /** World and overlay at twice the logical size. */
    private void save(File file, boolean overlay) throws Exception {
        write(file, g -> {
            game.render(g, 1);
            if (overlay) game.renderOverlay(g, 1);
        });
    }

    /** At twice the logical size. */
    private static void write(File file, Consumer<Graphics2D> draw) throws Exception {
        int scale = 2;
        BufferedImage image = new BufferedImage((int) Field.WIDTH * scale, (int) Field.HEIGHT * scale,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.scale(scale, scale);
        draw.accept(g);
        g.dispose();
        ImageIO.write(image, "png", file);
    }

    static List<String> names() {
        return List.of("title", "choice", "meadow", "camera", "caves", "heights", "cleared", "parallax", "seamless");
    }
}
