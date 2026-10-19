package Tanks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import processing.core.PImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** Headless Stage 4 acceptance tests: no human gameplay or graphical window needed. */
public class Stage4AutomatedAcceptanceTest {
    private FakeResourcesApp app;

    /** A deterministic resource loader avoids Processing GUI and filesystem dependencies. */
    public static class FakeResourcesApp extends App {
        public final Map<String, Integer> imageCalls = new HashMap<String, Integer>();

        @Override
        public String[] loadStrings(String filename) {
            // Deliberately put B to the LEFT of A to expose incorrect x-based sorting.
            return "first.txt".equals(filename)
                    ? new String[] {"B       A"}
                    : new String[] {"A B"};
        }

        @Override
        public PImage loadImage(String filename) {
            Integer oldCount = imageCalls.get(filename);
            imageCalls.put(filename, oldCount == null ? 1 : oldCount + 1);
            return new PImage(2, 2);
        }

        public int imageCount(String filename) {
            Integer count = imageCalls.get(filename);
            return count == null ? 0 : count;
        }
    }

    @BeforeEach
    public void setUp() {
        App.tanks.clear();
        App.projectiles.clear();
        App.trees.clear();
        App.playerscolor.clear();
        App.playerscolor.put("A", "0,0,255");
        App.playerscolor.put("B", "255,0,0");
        App.TerrainList = new double[28];
        Arrays.fill(App.TerrainList, 400);
        App.newTerrainList = new double[28 * 32];
        Arrays.fill(App.newTerrainList, 400);
        App.path = new String[][] {
                {"first.txt", "snow.png", "255,255,255", "tree2.png"},
                {"second.txt", "desert.png", "234,221,181", null}
        };
        App.pturn = 0;
        App.tturn = 0;
        app = new FakeResourcesApp();
    }

    private App.Tanks player(String name) {
        for (App.Tanks tank : App.tanks) if (name.equals(tank.name)) return tank;
        fail("Missing player " + name);
        return null;
    }

    @Test
    public void restartFromNonZeroScoresStartsFresh() {
        app.Initialise();
        player("A").scores = 78;
        player("B").scores = 21;
        app.cumulativeScores.put("A", 78);
        app.cumulativeScores.put("B", 21);
        App.pturn = 1; // Simulate reaching the last level.
        app.gameOver = true;
        app.restartGame();

        assertFalse(app.gameOver);
        assertEquals(0, App.pturn);
        assertEquals(0, player("A").scores, "Old scores must not survive restart");
        assertEquals(0, player("B").scores);
        assertTrue(app.cumulativeScores.isEmpty(), "Old cumulative scores must be cleared");
    }

    @Test
    public void turnsAreAlphabeticalEvenWhenBStartsLeftOfA() {
        app.Initialise();
        assertEquals("A", App.tanks.get(0).name);
        assertEquals("B", App.tanks.get(1).name);
        assertTrue(player("B").x < player("A").x, "Fixture must put B to A's left");
        assertEquals("A", app.currentPlayer().name);
    }

    @Test
    public void commonHudIconsAreLoadedOnlyOnceAcrossLevels() {
        app.Initialise();
        App.pturn = 1;
        app.reset();

        final String dir = "src/main/resources/Tanks/";
        for (String icon : new String[] {
                "fuel.png", "parachute.png", "wind.png", "wind-1.png"}) {
            assertEquals(1, app.imageCount(dir + icon), "Repeated HUD image load: " + icon);
        }
        assertEquals(1, app.imageCount(dir + "snow.png"));
        assertEquals(1, app.imageCount(dir + "desert.png"));
        assertEquals(0, app.imageCount(dir + "null"), "Optional trees must not load a null path");
    }

    @Test
    public void actualCraterTriggersFallDamageAndCreditsItsShooter() {
        app.Initialise();
        Arrays.fill(App.newTerrainList, 400);
        App.Tanks shooter = player("A");
        App.Tanks victim = player("B");
        shooter.y = victim.y = 400;
        victim.parachute = 0;
        App.Projectiles shot = app.new Projectiles(shooter, 90);
        shot.x = victim.x;
        shot.preciseX = victim.x;
        shot.y = 400;
        app.changeterrain(shot);
        app.onGround(shot);

        assertFalse(victim.ground, "Crater should remove supporting terrain");
        assertEquals(430.0, App.newTerrainList[victim.x], 0.01);
        app.updateTankFalls();
        assertEquals(404.0, victim.y, 0.01);
        assertEquals(96, victim.health);
        assertEquals(4, shooter.scores);
    }
}
