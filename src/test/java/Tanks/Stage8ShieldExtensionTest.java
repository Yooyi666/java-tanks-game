package Tanks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import processing.event.KeyEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 8 shield extension regression tests. These exercise the production
 * purchase, blast damage, HUD and drawing paths without a GUI window.
 */
public class Stage8ShieldExtensionTest {
    private Recorder app;
    private App.Tanks a;
    private App.Tanks b;

    private static class Recorder extends App {
        int bubbles;
        float lastBubbleX, lastBubbleY, lastBubbleWidth, lastBubbleHeight;
        boolean blueTransparentFill;
        final List<String> labels = new ArrayList<String>();

        @Override public void noStroke() {}
        @Override public void noFill() {}
        @Override public void strokeWeight(float weight) {}
        @Override public void stroke(int rgb) {}
        @Override public void stroke(int rgb, float alpha) {}
        public void stroke(float r, float g, float b) {}
        @Override public void fill(int rgb) {}
        @Override public void fill(int rgb, float alpha) {}
        public void fill(float r, float g, float b) {}
        public void fill(float r, float g, float b, float alpha) {
            if (r == 60 && g == 145 && b == 255 && alpha == 80) blueTransparentFill = true;
        }
        @Override public void ellipse(float x, float y, float w, float h) {
            bubbles++;
            lastBubbleX = x; lastBubbleY = y;
            lastBubbleWidth = w; lastBubbleHeight = h;
        }
        @Override public void rect(float x, float y, float w, float h) {}
        @Override public void line(float x1, float y1, float x2, float y2) {}
        @Override public void textSize(float size) {}
        @Override public void text(String label, float x, float y) { labels.add(label); }
        @Override public void text(int value, float x, float y) {}
        @Override public void image(processing.core.PImage image, float x, float y, float w, float h) {}
        @Override public void textAlign(int alignment) {}

        @Override public void Initialise() {
            // Build a minimal new level without loading Processing assets.
            App.Tanks player = new Tanks('A');
            player.x = 100;
            App.tanks.add(player);
        }
    }

    @BeforeEach public void prepare() {
        App.tanks.clear();
        App.projectiles.clear();
        App.trees.clear();
        App.playerscolor.clear();
        App.playerscolor.put("A", "0,0,255");
        App.playerscolor.put("B", "255,0,0");
        App.pturn = 0;
        App.tturn = 0;
        App.wspeed = 0;
        App.TerrainList = new double[28];
        App.newTerrainList = new double[28 * 32];
        Arrays.fill(App.newTerrainList, 400);
        app = new Recorder();
        a = app.new Tanks('A');
        b = app.new Tanks('B');
        a.x = 100; b.x = 300;
        a.y = b.y = 400;
        App.tanks.add(a); App.tanks.add(b);
    }

    private void blast(App.Tanks shooter, int x, double y) {
        App.Projectiles p = app.new Projectiles(shooter, 90);
        p.x = x;
        p.preciseX = x;
        p.y = y;
        app.hit(p, 6, 15, 30);
    }

    private KeyEvent press(int code) {
        return new KeyEvent(null, 0, KeyEvent.PRESS, 0, (char) code, code);
    }

    @Test public void newTankBeginsWithoutShield() {
        assertFalse(a.shieldActive);
        assertFalse(b.shieldActive);
        assertEquals(20, App.SHIELD_COST);
    }

    @Test public void purchaseCostsExactlyTwentyPoints() {
        a.scores = 35;
        assertTrue(app.purchaseShield(a));
        assertEquals(15, a.scores);
        assertTrue(a.shieldActive);
    }

    @Test public void insufficientScoreCannotPurchaseShield() {
        a.scores = 19;
        assertFalse(app.purchaseShield(a));
        assertEquals(19, a.scores);
        assertFalse(a.shieldActive);
    }

    @Test public void duplicatePurchaseDoesNotSpendAgain() {
        a.scores = 100;
        assertTrue(app.purchaseShield(a));
        assertFalse(app.purchaseShield(a));
        assertEquals(80, a.scores);
    }

    @Test public void destroyedTankCannotBuyShield() {
        a.health = 0;
        a.scores = 50;
        assertFalse(app.purchaseShield(a));
        assertEquals(50, a.scores);
    }

    @Test public void nullTankCannotBuyShield() {
        assertFalse(app.purchaseShield(null));
    }

    @Test public void keyboardHBuysShieldOnTheCurrentTurn() {
        a.scores = 25;
        app.keyPressed(press('H'));
        assertEquals(5, a.scores);
        assertTrue(a.shieldActive);
        assertFalse(b.shieldActive);
    }

    @Test public void keyboardHDoesNotBuyWhileProjectileInFlight() {
        a.scores = 25;
        app.shotInFlight = true;
        app.keyPressed(press('H'));
        assertEquals(25, a.scores);
        assertFalse(a.shieldActive);
    }

    @Test public void shieldAbsorbsFirstEnemyBlastWithoutAwardingScore() {
        a.scores = 40;
        assertTrue(app.purchaseShield(a));
        int before = a.scores;
        b.scores = 7;
        blast(b, a.x, a.y);
        assertEquals(100, a.health);
        assertEquals(before, a.scores);
        assertEquals(7, b.scores);
        assertFalse(a.shieldActive);
    }

    @Test public void nextBlastDamagesAfterShieldHasBeenConsumed() {
        a.scores = 20;
        app.purchaseShield(a);
        blast(b, a.x, a.y);
        blast(b, a.x, a.y);
        assertEquals(40, a.health);
        assertEquals(60, b.scores);
    }

    @Test public void distantBlastDoesNotConsumeShield() {
        a.scores = 20;
        app.purchaseShield(a);
        blast(b, a.x + 30, a.y);
        assertEquals(100, a.health);
        assertTrue(a.shieldActive);
    }

    @Test public void nonDamagingNearRadiusBlastDoesNotConsumeShield() {
        a.scores = 20;
        app.purchaseShield(a);
        // Distance 29.9 -> damage truncates to zero HP.
        blast(b, a.x, a.y + 29.9);
        assertTrue(a.shieldActive);
    }

    @Test public void ownProjectileConsumesShieldButDoesNotGrantScore() {
        a.scores = 20;
        app.purchaseShield(a);
        blast(a, a.x, a.y);
        assertEquals(100, a.health);
        assertFalse(a.shieldActive);
        assertEquals(0, a.scores);
    }

    @Test public void shieldOnlyProtectsTheEquippedTank() {
        a.scores = 20;
        app.purchaseShield(a);
        blast(a, b.x, b.y);
        assertEquals(40, b.health);
        assertEquals(60, a.scores);
        assertTrue(a.shieldActive);
    }

    @Test public void shieldDoesNotAlterFallDamageRules() {
        b.scores = 20;
        app.purchaseShield(b);
        b.ground = false;
        b.fallWithParachute = false;
        b.parachute = 0;
        b.y = 390;
        App.newTerrainList[b.x] = 420;
        app.updateTankFalls();
        assertEquals(96, b.health);
        assertTrue(b.shieldActive);
    }

    @Test public void shieldBubbleUsesBlueTransparentEllipse() {
        a.scores = 20;
        app.purchaseShield(a);
        app.drawtank();
        assertEquals(1, app.bubbles);
        assertTrue(app.blueTransparentFill);
        assertEquals(40.0, app.lastBubbleWidth, 0.001);
        assertEquals(38.0, app.lastBubbleHeight, 0.001);
        assertEquals(a.x, app.lastBubbleX, 0.001);
    }

    @Test public void unshieldedTanksDoNotRenderBubbles() {
        app.drawtank();
        assertEquals(0, app.bubbles);
    }

    @Test public void scoreboardShowsEquippedShield() {
        a.scores = 20;
        app.purchaseShield(a);
        app.drawscoreboard();
        assertTrue(app.labels.contains("Shield ON"));
    }

    @Test public void scoreboardOmitsShieldIndicatorWhenNotEquipped() {
        app.drawscoreboard();
        assertFalse(app.labels.contains("Shield ON"));
    }

    @Test public void shieldDoesNotPersistAcrossLevelReset() {
        a.scores = 50;
        app.purchaseShield(a);
        app.reset();
        assertEquals(1, App.tanks.size());
        assertFalse(App.tanks.get(0).shieldActive);
        assertEquals(30, App.tanks.get(0).scores);
    }
}
