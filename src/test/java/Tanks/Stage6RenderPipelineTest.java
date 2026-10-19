package Tanks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import processing.core.PImage;
import processing.event.KeyEvent;
import processing.data.JSONObject;
import processing.data.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 6: execute production Processing rendering paths with a recording renderer.
 * No OS window or manual gameplay is needed. These tests inspect draw calls,
 * state changes and timing; they cannot validate pixel-perfect visual fidelity.
 */
public class Stage6RenderPipelineTest {
    private RecordingApp app;
    private App.Tanks a;
    private App.Tanks b;

    /** Substitutes only the external Processing drawing and resource-loading boundary. */
    private static class RecordingApp extends App {
        int lines, rects, ellipses, images, textCalls, loadCalls, triangles;
        int lastRed, lastGreen, lastBlue, lastAlpha;
        int lastSolidFill = -1, lastSolidStroke = -1;
        float arrowTipX, arrowTipY;
        final List<String> labels = new ArrayList<String>();
        final List<String> imagePaths = new ArrayList<String>();
        final List<float[]> circles = new ArrayList<float[]>();
        JSONObject fixture;

        @Override public JSONObject loadJSONObject(String file) { return fixture; }
        @Override public void frameRate(float fps) {}
        @Override public String[] loadStrings(String file) {
            return new String[] {"B       A       T", "XXXXXXXXXXXXXXXXX"};
        }
        @Override public PImage loadImage(String file) {
            loadCalls++;
            imagePaths.add(file);
            return new PImage(2, 2);
        }
        @Override public void image(PImage image, float x, float y, float w, float h) { images++; }
        @Override public void line(float x1, float y1, float x2, float y2) { lines++; }
        @Override public void rect(float x, float y, float w, float h) { rects++; }
        @Override public void ellipse(float x, float y, float w, float h) {
            ellipses++;
            circles.add(new float[] { x, y, w, h });
        }
        @Override public void triangle(float x1, float y1, float x2, float y2, float x3, float y3) {
            triangles++;
            arrowTipX = x3;
            arrowTipY = y3;
        }
        @Override public void text(String text, float x, float y) {
            textCalls++; labels.add(text);
        }
        @Override public void text(int number, float x, float y) { textCalls++; }
        @Override public void text(float number, float x, float y) { textCalls++; }
        @Override public void textSize(float size) {}
        @Override public void textAlign(int align) {}
        @Override public void strokeWeight(float weight) {}
        @Override public void noStroke() {}
        @Override public void noFill() {}
        @Override public void stroke(int rgb) { lastSolidStroke = rgb; }
        @Override public void stroke(int rgb, float alpha) {}
        public void stroke(float gray) {}
        public void stroke(float gray, float alpha) {}
        public void stroke(float red, float green, float blue) {}
        public void stroke(float red, float green, float blue, float alpha) {}
        @Override public void fill(int rgb) { lastSolidFill = rgb; }
        @Override public void fill(int rgb, float alpha) {}
        public void fill(float gray) {}
        public void fill(float gray, float alpha) {}
        public void fill(float red, float green, float blue) {}
        public void fill(float red, float green, float blue, float alpha) {
            lastRed = Math.round(red); lastGreen = Math.round(green);
            lastBlue = Math.round(blue); lastAlpha = Math.round(alpha);
        }
    }

    @BeforeEach
    public void prepare() {
        App.tanks.clear(); App.trees.clear(); App.projectiles.clear();
        App.playerscolor.clear();
        App.playerscolor.put("A", "0,0,255");
        App.playerscolor.put("B", "255,0,0");
        App.TerrainList = new double[28];
        Arrays.fill(App.TerrainList, 400);
        App.newTerrainList = new double[28 * 32];
        Arrays.fill(App.newTerrainList, 400);
        App.path = new String[][] {{"one.txt", "snow.png", "255,255,255", "tree1.png"},
                                    {"two.txt", "desert.png", "234,221,181", null}};
        App.pturn = 0; App.tturn = 0; App.wspeed = 0;
        app = new RecordingApp();
        a = app.new Tanks('A'); b = app.new Tanks('B');
        a.x = 96; b.x = 288; a.y = b.y = 400;
        App.tanks.add(a); App.tanks.add(b);
    }

    private static JSONObject configFixture() {
        JSONObject root = new JSONObject();
        JSONArray levels = new JSONArray();
        JSONObject first = new JSONObject();
        first.setString("layout", "one.txt");
        first.setString("background", "snow.png");
        first.setString("foreground-colour", "255,255,255");
        first.setString("trees", "tree1.png");
        levels.append(first);
        JSONObject second = new JSONObject();
        second.setString("layout", "two.txt");
        second.setString("background", "desert.png");
        second.setString("foreground-colour", "234,221,181");
        levels.append(second);
        JSONObject colours = new JSONObject();
        colours.setString("A", "0,0,255");
        colours.setString("B", "255,0,0");
        root.setJSONArray("levels", levels);
        root.setJSONObject("player_colours", colours);
        return root;
    }

    @Test public void getpathRejectsNullLevelsWithoutThrowing() {
        app.getpath(null);
        assertEquals(2, App.path.length);
    }
    @Test public void getpathReadsLevelStylesAndOptionalTree() {
        app.fixture = configFixture();
        App.json = app.fixture;
        app.getpath(app.fixture.getJSONArray("levels"));
        assertEquals("snow.png", App.path[0][1]);
        assertEquals("tree1.png", App.path[0][3]);
        assertEquals("234,221,181", App.path[1][2]);
        assertNull(App.path[1][3]);
        assertEquals("0,0,255", App.playerscolor.get("A"));
    }
    @Test public void setupReadsConfigAndInitialisesLevelWithoutWindow() {
        App.tanks.clear(); App.trees.clear();
        app.fixture = configFixture();
        app.setup();
        assertEquals(2, App.tanks.size());
        assertEquals("A", App.tanks.get(0).name);
        assertTrue(app.loadCalls >= 5);
        assertEquals(App.newTerrainList[App.tanks.get(0).x], App.tanks.get(0).y, 0.001);
    }
    @Test public void setupCanSwitchToOptionalTreeFreeLevelOnReset() {
        App.tanks.clear(); App.trees.clear();
        app.fixture = configFixture();
        app.setup();
        App.pturn = 1;
        app.reset();
        assertEquals(0, App.trees.size());
        assertEquals("desert.png", App.path[1][1]);
    }

    private boolean contains(String needle) {
        for (String text : app.labels) if (text.contains(needle)) return true;
        return false;
    }
    private App.Projectiles shotAt(double x, double y) {
        App.Projectiles shot = app.new Projectiles(a, 90);
        shot.x = (int) x; shot.preciseX = x; shot.y = y;
        return shot;
    }

    @Test public void terrainRenderDrawsOneColumnForEachPixel() {
        app.drawterrain();
        assertEquals(App.newTerrainList.length, app.lines);
    }
    @Test public void terrainRenderUsesPreloadedBackgroundAndHudIcons() {
        app.Initialise();
        int loaded = app.loadCalls;
        app.drawterrain(); app.drawterrain();
        assertEquals(loaded, app.loadCalls);
        assertTrue(app.images >= 6);
    }
    @Test public void missingOptionalTreeSpriteIsNotLoaded() {
        App.pturn = 1;
        app.Initialise();
        assertFalse(app.imagePaths.contains("src/main/resources/Tanks/null"));
    }
    @Test public void treeRenderUsesOneImagePerTree() {
        app.Initialise();
        App.trees.clear();
        App.Tree first = app.new Tree(); first.x = 45;
        App.Tree second = app.new Tree(); second.x = 70;
        App.trees.add(first); App.trees.add(second);
        int baseline = app.images;
        app.drawtree();
        assertEquals(2, app.images - baseline);
    }
    @Test public void emptyTreeListDrawsNothing() {
        app.drawtree();
        assertEquals(0, app.images);
    }
    @Test public void drawTankPaintsBothAliveTanks() {
        app.drawtank();
        assertEquals(4, app.rects);
        assertEquals(2, app.lines);
    }
    @Test public void destroyedTankHasNoTankGeometry() {
        b.health = 0;
        app.drawtank();
        assertEquals(2, app.rects);
        assertEquals(1, app.lines);
    }
    @Test public void parachutingTankHasExtraImage() {
        app.Initialise();
        a.ground = false; a.fallWithParachute = true;
        int baseline = app.images;
        app.drawtank();
        assertEquals(1, app.images - baseline);
    }
    @Test public void fallingWithoutParachuteDoesNotDrawCanopy() {
        app.Initialise();
        a.ground = false; a.fallWithParachute = false;
        int baseline = app.images;
        app.drawtank();
        assertEquals(0, app.images - baseline);
    }
    @Test public void scoreboardContainsBothPlayersAlphabetically() {
        a.scores = 12; b.scores = 31;
        app.drawscoreboard();
        assertTrue(app.labels.indexOf("Player A") < app.labels.indexOf("Player B"));
        assertTrue(contains("Player A's turn"));
    }
    @Test public void scoreboardSkipsHudForAllDeadTanks() {
        a.health = 0; b.health = 0;
        app.drawscoreboard();
        assertTrue(contains("Scores"));
        assertFalse(contains("'s turn"));
    }
    @Test public void scoreboardSkipsDeadPlayersAsActivePlayer() {
        a.health = 0;
        app.drawscoreboard();
        assertTrue(contains("Player B's turn"));
    }
    @Test public void scoreboardShowsWindIconAfterAssetsLoaded() {
        app.Initialise();
        int baseline = app.images;
        App.wspeed = -12;
        app.drawscoreboard();
        assertTrue(app.images > baseline);
    }
    @Test public void scoreboardWorksWithNoTanks() {
        App.tanks.clear();
        app.drawscoreboard();
        assertTrue(contains("Scores"));
    }
    @Test public void drawTurnArrowHighlightsCurrentTank() {
        app.drawTurnArrow();
        assertEquals(1, app.triangles, "A downward arrow head should be drawn for the active tank");
        assertTrue(app.lines >= 1, "A vertical arrow stem should be rendered");
        assertEquals(a.x, app.arrowTipX, 0.01f);
        assertEquals(a.y - 55, app.arrowTipY, 0.01f);
        assertEquals(0, app.lastSolidStroke, "Arrow stem must be black");
        assertEquals(0, app.lastSolidFill, "Arrow head must be black");
        assertTrue(app.arrowTipY <= a.y - 50, "Leave an unobstructed gap above the tank");
        App.tturn = 1;
        app.drawTurnArrow();
        assertEquals(2, app.triangles);
        assertEquals(b.x, app.arrowTipX, 0.01f, "The next turn arrow should point to B");
    }
    @Test public void drawTurnArrowDoesNotRenderDuringGameOver() {
        app.gameOver = true;
        app.drawTurnArrow();
        assertEquals(0, app.triangles);
    }
    @Test public void turnArrowPersistsUntilPlayerActs() {
        app.turnStartFrame = 30;
        app.frameCount = 90;
        assertTrue(app.shouldShowTurnArrow(), "Two seconds must not dismiss the arrow");
        app.frameCount = 10000;
        assertTrue(app.shouldShowTurnArrow(), "Arrow stays visible for an idle player");
    }
    @Test public void arrowHidesOnMovementAndResetsForNextPlayer() {
        assertTrue(app.shouldShowTurnArrow());
        press(App.RIGHT);
        assertFalse(app.shouldShowTurnArrow());
        app.nextTurn();
        assertTrue(app.shouldShowTurnArrow(), "New player must get a fresh arrow");
        press(App.UP);
        assertFalse(app.shouldShowTurnArrow(), "Aiming is the first player action");
    }
    @Test public void arrowHidesWhenPowerAdjustedAndStaysHiddenAfterRelease() {
        assertTrue(app.shouldShowTurnArrow());
        press('W');
        assertFalse(app.shouldShowTurnArrow());
        app.keyReleased(key(KeyEvent.RELEASE, 'W'));
        app.frameCount += 1000;
        assertFalse(app.shouldShowTurnArrow(), "Hiding lasts until the next turn");
    }
    @Test public void arrowHidesAfterPurchaseButNotOnInvalidInput() {
        press('Z');
        assertTrue(app.shouldShowTurnArrow());
        press('H'); // Not enough score: no action taken.
        assertTrue(app.shouldShowTurnArrow());
        a.scores = 25;
        press('H');
        assertFalse(app.shouldShowTurnArrow(), "Successful power-up is a player action");
    }
    @Test public void arrowResetsOnShotButWaitsForProjectileToFinish() {
        press(' ');
        assertFalse(app.shouldShowTurnArrow());
        assertTrue(app.shotInFlight);
        App.projectiles.clear();
        app.updateRoundState();
        assertTrue(app.shouldShowTurnArrow(), "Next player's arrow waits until the shot resolves");
    }
    private KeyEvent key(int action, int code) {
        return new KeyEvent(null, 0, action, 0, (char) code, code);
    }
    private void press(int code) { app.keyPressed(key(KeyEvent.PRESS, code)); }
    @Test public void turnArrowWaitsUntilProjectileResolves() {
        app.frameCount = 70;
        app.turnStartFrame = 0;
        app.shotInFlight = true;
        assertFalse(app.shouldShowTurnArrow());
        app.updateRoundState();
        assertEquals(70, app.turnStartFrame);
        assertTrue(app.shouldShowTurnArrow());
    }
    @Test public void finalResultIdentifiesHighestScoringPlayer() {
        app.cumulativeScores.put("A", 10);
        app.cumulativeScores.put("B", 42);
        app.gameOverFrame = 100; app.frameCount = 200;
        app.drawFinalResults();
        assertTrue(contains("Player B wins!"));
        assertTrue(contains("Player B  -  42"));
    }
    @Test public void finalResultDelaysSecondScoreUntilTwentyOneFrames() {
        app.cumulativeScores.put("A", 30);
        app.cumulativeScores.put("B", 20);
        app.gameOverFrame = 100; app.frameCount = 100;
        app.drawFinalResults();
        assertTrue(contains("Player A  -  30"));
        assertFalse(contains("Player B  -  20"));
        app.frameCount = 121;
        app.drawFinalResults();
        assertTrue(contains("Player B  -  20"));
    }
    @Test public void finalResultHandlesNoScoreEntries() {
        app.drawFinalResults();
        assertTrue(contains("Press R to restart"));
        assertFalse(contains("wins!"));
    }
    @Test public void finalResultLightensWinnerColourWithTransparency() {
        app.cumulativeScores.put("A", 4);
        app.gameOverFrame = 50; app.frameCount = 100;
        app.drawFinalResults();
        assertEquals(55, app.lastRed);
        assertEquals(55, app.lastGreen);
        assertEquals(255, app.lastBlue);
        assertEquals(185, app.lastAlpha);
    }
    @Test public void drawGameOverUsesFinalResultsNotLiveScoreboard() {
        app.gameOver = true;
        app.cumulativeScores.put("A", 20);
        app.gameOverFrame = 0;
        app.draw();
        assertTrue(contains("Player A wins!"));
        assertFalse(contains("Scores"));
    }
    @Test public void drawActiveGameShowsScoreboardAndTerrain() {
        app.draw();
        // One additional line is the active tank's turn-arrow stem.
        assertEquals(App.newTerrainList.length + 4, app.lines);
        assertTrue(contains("Scores"));
    }
    @Test public void drawActiveGameAdvancesRoundAfterLastTankDies() {
        b.health = 0;
        app.draw();
        assertTrue(app.levelComplete);
    }
    @Test public void movingProjectileCreatesEllipseAndAdvancesPosition() {
        App.Projectiles p = shotAt(120, 300);
        p.vx = 2.0; p.vy = -1.0;
        App.projectiles.add(p);
        app.drawprojectiles();
        assertTrue(app.ellipses >= 1);
        assertEquals(122.0, p.preciseX, 0.001);
        assertEquals(1, App.projectiles.size());
    }
    @Test public void projectileBeyondRightMapEdgeIsRemoved() {
        App.Projectiles p = shotAt(App.newTerrainList.length - 1, 100);
        p.vx = 20; p.vy = 0;
        App.projectiles.add(p);
        app.drawprojectiles();
        assertTrue(App.projectiles.isEmpty());
    }
    @Test public void projectileBeyondBottomIsRemoved() {
        Arrays.fill(App.newTerrainList, App.HEIGHT + 100);
        App.Projectiles p = shotAt(100, App.HEIGHT - 2);
        p.vx = 0; p.vy = 10;
        App.projectiles.add(p);
        app.drawprojectiles();
        assertTrue(App.projectiles.isEmpty());
    }
    @Test public void projectileBeyondLeftMapEdgeIsRemoved() {
        App.Projectiles p = shotAt(1, 100);
        p.vx = -10; p.vy = 0;
        App.projectiles.add(p);
        app.drawprojectiles();
        assertTrue(App.projectiles.isEmpty());
    }
    @Test public void projectileContactTriggersFirstExplosionFrame() {
        App.Projectiles p = shotAt(150, 399);
        p.vx = 0; p.vy = 2;
        App.projectiles.add(p);
        app.drawprojectiles();
        assertTrue(p.explosion);
        assertEquals(1, p.explosion_phrase);
        assertEquals(1, App.projectiles.size());
    }
    @Test public void explosionGrowsForSixFramesAndRemovesProjectile() {
        App.Projectiles p = shotAt(200, 400);
        App.projectiles.add(p);
        for (int frame = 1; frame < App.EXPLOSION_FRAMES; frame++) {
            app.drawexplosions(p, 0);
            assertEquals(frame, p.explosion_phrase);
            assertEquals(1, App.projectiles.size());
        }
        app.drawexplosions(p, 0);
        assertTrue(App.projectiles.isEmpty());
        assertTrue(app.ellipses >= 18);
    }
    @Test public void existingExplosionContinuesOnRenderStep() {
        App.Projectiles p = shotAt(200, 400);
        p.explosion = true;
        p.explosion_phrase = 1;
        App.projectiles.add(p);
        app.drawprojectiles();
        assertEquals(2, p.explosion_phrase);
    }
    @Test public void finishedExplosionChangesTerrainAtImpact() {
        App.Projectiles p = shotAt(200, 400);
        p.explosion_phrase = App.EXPLOSION_FRAMES - 1; p.explosion = true;
        App.projectiles.add(p);
        app.drawexplosions(p, 0);
        assertTrue(App.newTerrainList[200] > 400);
    }
    @Test public void fullDrawAfterImageInitialisationDoesNotReloadAssets() {
        app.Initialise();
        int loaded = app.loadCalls;
        app.draw(); app.draw();
        assertEquals(loaded, app.loadCalls);
    }
}
