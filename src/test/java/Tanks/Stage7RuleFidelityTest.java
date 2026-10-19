package Tanks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import processing.event.KeyEvent;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

/** Stage 7: time-based controls, 0.2-second radius-correct blasts and death explosions. */
public class Stage7RuleFidelityTest {
    private Recorder app;
    private App.Tanks a;
    private App.Tanks b;

    private static class Recorder extends App {
        int ellipses;
        float firstWidth, lastWidth;
        @Override public void noStroke() {}
        public void fill(float r, float g, float b) {}
        @Override public void ellipse(float x, float y, float w, float h) {
            if (ellipses == 0) firstWidth = w;
            lastWidth = w;
            ellipses++;
        }
    }

    @BeforeEach
    public void setUp() {
        App.tanks.clear(); App.projectiles.clear(); App.trees.clear();
        App.playerscolor.clear();
        App.playerscolor.put("A", "0,0,255");
        App.playerscolor.put("B", "255,0,0");
        App.TerrainList = new double[28];
        App.newTerrainList = new double[28 * 32];
        Arrays.fill(App.newTerrainList, 400);
        App.pturn = 0; App.tturn = 0; App.wspeed = 0;
        app = new Recorder();
        a = app.new Tanks('A');
        b = app.new Tanks('B');
        a.x = 100; b.x = 300;
        a.y = b.y = 400;
        App.tanks.add(a); App.tanks.add(b);
    }

    private KeyEvent key(int action, int code) {
        return new KeyEvent(null, 0, action, 0, (char) code, code);
    }

    @Test public void turretRotatesThreeRadiansDuringThirtyFrameSteps() {
        a.currentAngle = 0;
        for (int frame = 0; frame < App.FPS; frame++) a.turretleft();
        assertEquals(Math.toDegrees(3.0), a.currentAngle, 1e-9);
    }

    @Test public void powerChangesByExactlyThirtySixAcrossThirtyFrames() {
        for (int i = 0; i < App.FPS; i++) a.increasepower();
        assertEquals(86, a.power);
        assertEquals(86.0, a.firingPower(), 1e-9);
        for (int i = 0; i < App.FPS; i++) a.decreasepower();
        assertEquals(50, a.power);
        assertEquals(50.0, a.firingPower(), 1e-9);
    }

    @Test public void projectileUsesFractionalFiringPowerNotRoundedHudValue() {
        a.increasepower(); // 51.2 exact, 51 displayed.
        assertEquals(51, a.power);
        App.Projectiles shot = app.new Projectiles(a, 90);
        assertEquals(60 + 4.8 * 51.2, shot.speed, 1e-9);
    }

    @Test public void heldRightMovesOncePerFrameAndStopsWhenReleased() {
        app.frameCount = 10;
        app.keyPressed(key(KeyEvent.PRESS, App.RIGHT));
        assertEquals(102, a.x);
        app.keyPressed(key(KeyEvent.PRESS, App.RIGHT)); // OS repeat ignored.
        assertEquals(102, a.x);
        app.frameCount = 11;
        app.updateHeldControls();
        assertEquals(104, a.x);
        app.updateHeldControls();
        assertEquals(104, a.x);
        app.keyReleased(key(KeyEvent.RELEASE, App.RIGHT));
        app.frameCount = 12;
        app.updateHeldControls();
        assertEquals(104, a.x);
        assertEquals(246, a.fuel);
    }

    @Test public void lastUnitOfFuelMovesExactlyOnePixel() {
        a.fuel = 1;
        a.tankmoveright();
        assertEquals(101, a.x);
        assertEquals(0, a.fuel);
    }

    @Test public void explosionUsesSixFramesAndFullThirtyPixelRadius() {
        App.Projectiles shot = app.new Projectiles(a, 90);
        shot.x = 200; shot.preciseX = 200; shot.y = 400;
        App.projectiles.add(shot);
        app.drawexplosions(shot, 0);
        assertEquals(10.0, app.firstWidth, 1e-6); // red DIAMETER at 1/6 progress
        assertEquals(1, shot.explosion_phrase);
        assertEquals(1, App.projectiles.size());
        for (int i = 1; i < App.EXPLOSION_FRAMES; i++) app.drawexplosions(shot, 0);
        assertEquals(App.EXPLOSION_FRAMES, shot.explosion_phrase);
        assertEquals(0, App.projectiles.size());
        assertEquals(18, app.ellipses);
    }

    @Test public void explosionFinalRedCircleHasSixtyPixelDiameter() {
        app.paintExplosion(200, 300, 30, App.EXPLOSION_FRAMES - 1);
        assertEquals(60.0, app.firstWidth, 1e-6);
        assertEquals(3, app.ellipses);
    }

    @Test public void fatalProjectileSchedulesExactlyOneSmallBlast() {
        b.health = 12;
        App.Projectiles shot = app.new Projectiles(a, 90);
        shot.x = b.x; shot.y = b.y;
        app.hit(shot, 6, 15, 30);
        assertEquals(0, b.health);
        assertEquals(12, a.scores);
        assertEquals(1, app.deathExplosions.size());
        assertEquals(15, app.deathExplosions.get(0).radius);
        app.queueDeathExplosion(b, a, 15);
        assertEquals(1, app.deathExplosions.size());
    }

    @Test public void offscreenFallSchedulesThirtyPixelBlast() {
        b.y = App.HEIGHT - 2;
        b.ground = false;
        b.parachute = 0;
        b.fallWithParachute = false;
        App.newTerrainList[b.x] = App.HEIGHT + 40;
        app.updateTankFalls();
        assertEquals(0, b.health);
        assertEquals(1, app.deathExplosions.size());
        assertEquals(30, app.deathExplosions.get(0).radius);
    }

    @Test public void roundWaitsUntilDeathAnimationCompletes() {
        b.health = 0;
        app.queueDeathExplosion(b, a, 15);
        app.frameCount = 100;
        app.updateRoundState();
        assertFalse(app.levelComplete);
        for (int i = 0; i < App.EXPLOSION_FRAMES; i++) app.drawDeathExplosions();
        assertEquals(0, app.deathExplosions.size());
        app.updateRoundState();
        assertTrue(app.levelComplete);
    }

    @Test public void terrainSmoothingDoesNotUseAlreadyUpdatedInput() {
        Arrays.fill(App.newTerrainList, 0);
        App.newTerrainList[31] = 320;
        app.avgTerrain();
        assertEquals(10.0, App.newTerrainList[0], 1e-9);
        assertEquals(10.0, App.newTerrainList[1], 1e-9); // old inplace averaging fails here
        assertEquals(0.0, App.newTerrainList[32], 1e-9);
    }
}
