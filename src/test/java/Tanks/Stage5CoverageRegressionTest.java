package Tanks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import processing.core.PImage;
import processing.event.KeyEvent;
import java.util.Arrays;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 5: state transitions, keyboard paths, terrain and collision edges.
 * Every test is deterministic and requires no Processing window.
 * This suite supplements rather than replaces the 39 prior tests.
 */
public class Stage5CoverageRegressionTest {
    private Harness app;
    private App.Tanks a;
    private App.Tanks b;

    private static class Harness extends App {
        @Override public String[] loadStrings(String file) {
            if ("missing.txt".equals(file)) return null;
            return new String[] {"  B A       ", "XXXXXXXXXXXX"};
        }
        @Override public PImage loadImage(String file) { return new PImage(2, 2); }
    }

    @BeforeEach
    public void prepare() {
        App.tanks.clear(); App.projectiles.clear(); App.trees.clear();
        App.playerscolor.clear();
        App.playerscolor.put("A", "0,0,255");
        App.playerscolor.put("B", "255,0,0");
        App.TerrainList = new double[28];
        Arrays.fill(App.TerrainList, 400);
        App.newTerrainList = new double[28 * 32];
        Arrays.fill(App.newTerrainList, 400);
        App.path = new String[][] {
            {"first.txt", "snow.png", "255,255,255", "tree1.png"},
            {"second.txt", "desert.png", "234,221,181", null}
        };
        App.pturn = 0; App.tturn = 0; App.wspeed = 0;
        App.wrandom = new Random(12);
        app = new Harness();
        a = app.new Tanks('A'); b = app.new Tanks('B');
        a.x = 100; b.x = 300; a.y = b.y = 400;
        App.tanks.add(a); App.tanks.add(b);
    }

    private void press(int key) {
        app.keyPressed(new KeyEvent(null, 0, KeyEvent.PRESS, 0, (char) key, key));
    }

    @Test public void currentPlayerIsNullWithoutAnyTanks() {
        App.tanks.clear(); assertNull(app.currentPlayer());
    }
    @Test public void currentPlayerSkipsDeadPlayerAndWraps() {
        App.tturn = 1; b.health = 0;
        assertSame(a, app.currentPlayer()); assertEquals(0, App.tturn);
    }
    @Test public void nextTurnWithNoPlayersDoesNotAlterWind() {
        App.tanks.clear(); App.wspeed = 13; app.nextTurn();
        assertEquals(13, App.wspeed);
    }
    @Test public void activeMultiplayerLevelIsNotMarkedComplete() {
        app.frameCount = 100; app.updateRoundState();
        assertFalse(app.levelComplete); assertFalse(app.gameOver);
    }
    @Test public void projectileInFlightPreventsPrematureLevelCompletion() {
        b.health = 0;
        App.projectiles.add(app.new Projectiles(a, 90));
        app.frameCount = 100; app.updateRoundState();
        assertFalse(app.levelComplete);
    }
    @Test public void lastSurvivorBeginsOneSecondCountdown() {
        b.health = 0; app.frameCount = 41;
        app.updateRoundState();
        assertTrue(app.levelComplete); assertEquals(41, app.levelCompleteFrame);
        app.frameCount = 41 + App.FPS - 1; app.updateRoundState();
        assertEquals(0, App.pturn);
    }
    @Test public void completedRoundLoadsNextLayoutAfterThirtyFrames() {
        a.scores = 13; b.health = 0; app.frameCount = 20;
        app.updateRoundState(); app.frameCount = 20 + App.FPS;
        app.updateRoundState();
        assertEquals(1, App.pturn); assertFalse(app.levelComplete);
        assertEquals(13, app.cumulativeScores.get("A").intValue());
    }
    @Test public void finalLevelProducesGameOverAndRecordsScore() {
        App.pturn = 1; a.scores = 29; b.health = 0;
        app.frameCount = 200; app.updateRoundState();
        app.frameCount = 200 + App.FPS; app.updateRoundState();
        assertTrue(app.gameOver); assertEquals(230, app.gameOverFrame);
        assertEquals(29, app.cumulativeScores.get("A").intValue());
    }
    @Test public void gameOverBlocksFurtherRoundUpdates() {
        app.gameOver = true; b.health = 0;
        app.frameCount = 400; app.updateRoundState();
        assertFalse(app.levelComplete);
    }
    @Test public void leftArrowMovesGroundedTankAndConsumesFuel() {
        press(App.LEFT); assertEquals(98, a.x); assertEquals(248, a.fuel);
    }
    @Test public void rightArrowMovesTankAtLeftBoundary() {
        a.x = 0; press(App.RIGHT);
        assertEquals(2, a.x); assertEquals(248, a.fuel);
    }
    @Test public void leftArrowDoesNotMoveTankPastLeftEdge() {
        a.x = 0; press(App.LEFT); assertEquals(0, a.x);
    }
    @Test public void arrowKeysAdjustTurretInCorrectDirections() {
        press(App.UP); assertEquals(90.0 + Math.toDegrees(3.0 / App.FPS), a.currentAngle, 0.001);
        press(App.DOWN); assertEquals(90.0, a.currentAngle, 0.001);
    }
    @Test public void powerKeysRespectHealthLimitAndLowerBound() {
        a.power = 100; press('W'); assertEquals(100, a.power);
        a.power = 0; press('S'); assertEquals(0, a.power);
    }
    @Test public void spaceFiresAndMovesToNextPlayer() {
        press(' '); assertEquals(1, App.projectiles.size());
        assertTrue(app.shotInFlight); assertSame(b, app.currentPlayer());
    }
    @Test public void keysCannotChangeTankDuringProjectileFlight() {
        app.shotInFlight = true; press(App.LEFT); press(' ');
        assertEquals(100, a.x); assertTrue(App.projectiles.isEmpty());
    }
    @Test public void controlsAreBlockedWhileTankFalls() {
        a.ground = false; press(App.RIGHT); press(' ');
        assertEquals(100, a.x); assertTrue(App.projectiles.isEmpty());
    }
    @Test public void inputIsIgnoredIfNoTankIsAlive() {
        a.health = b.health = 0; press(' ');
        assertTrue(App.projectiles.isEmpty());
    }
    @Test public void repairPurchasesExactlyTwentyHealthAndCostsTwentyPoints() {
        a.health = 54; a.scores = 30; press('R');
        assertEquals(74, a.health); assertEquals(10, a.scores);
    }
    @Test public void repairNeverExceedsMaximumHealth() {
        a.health = 95; a.scores = 20; press('R');
        assertEquals(100, a.health); assertEquals(0, a.scores);
    }
    @Test public void unaffordableRepairMakesNoChanges() {
        a.health = 55; a.scores = 19; press('R');
        assertEquals(55, a.health); assertEquals(19, a.scores);
    }
    @Test public void fuelPurchaseAddsTwoHundredAndCostsTen() {
        a.scores = 10; press('F');
        assertEquals(450, a.fuel); assertEquals(0, a.scores);
    }
    @Test public void unaffordableFuelPurchaseDoesNothing() {
        a.scores = 9; press('F');
        assertEquals(250, a.fuel); assertEquals(9, a.scores);
    }
    @Test public void parachutePurchaseAddsOneAndCostsFifteen() {
        a.scores = 15; press('P');
        assertEquals(4, a.parachute); assertEquals(0, a.scores);
    }
    @Test public void unaffordableParachutePurchaseDoesNothing() {
        a.scores = 14; press('P');
        assertEquals(3, a.parachute); assertEquals(14, a.scores);
    }
    @Test public void levelCompleteSpaceAdvancesWithoutWaiting() {
        a.scores = 12; app.levelComplete = true;
        press(' '); assertEquals(1, App.pturn); assertFalse(app.levelComplete);
        assertEquals(12, app.cumulativeScores.get("A").intValue());
    }
    @Test public void lastLevelSpaceFinishesGameAndPreservesScores() {
        App.pturn = 1; app.levelComplete = true; a.scores = 27; app.frameCount = 99;
        press(' '); assertTrue(app.gameOver);
        assertEquals(99, app.gameOverFrame);
        assertEquals(27, app.cumulativeScores.get("A").intValue());
    }
    @Test public void gameOverIgnoresNonRestartInput() {
        app.gameOver = true; press(App.RIGHT);
        assertTrue(app.gameOver); assertEquals(100, a.x);
    }
    @Test public void gameOverRRestartsAndClearsScores() {
        app.gameOver = true; a.scores = 65;
        press('R'); assertFalse(app.gameOver); assertEquals(0, App.pturn);
        assertTrue(app.cumulativeScores.isEmpty());
        for (App.Tanks tank : App.tanks) assertEquals(0, tank.scores);
    }
    @Test public void terrainExpansionRepeatsEachColumnThirtyTwoTimes() {
        App.TerrainList[0] = 128; App.TerrainList[1] = 256;
        app.newTerrainList();
        assertEquals(128.0, App.newTerrainList[0]);
        assertEquals(128.0, App.newTerrainList[31]);
        assertEquals(256.0, App.newTerrainList[32]);
    }
    @Test public void flatTerrainIsUnchangedByBothSmoothingPasses() {
        Arrays.fill(App.newTerrainList, 215);
        app.avgTerrain(); app.avgTerrain();
        for (double height : App.newTerrainList) assertEquals(215.0, height, 0.001);
    }
    @Test public void craterAltersOnlyTheLocalThirtyPixelNeighborhood() {
        App.Projectiles shot = app.new Projectiles(a, 90);
        shot.x = 100; shot.y = 400;
        app.changeterrain(shot);
        assertEquals(400.0, App.newTerrainList[69], 0.001);
        assertEquals(400.0, App.newTerrainList[131], 0.001);
        assertTrue(App.newTerrainList[100] > 400);
    }
    @Test public void craterNearLeftMapEdgeDoesNotAccessNegativeIndices() {
        App.Projectiles shot = app.new Projectiles(a, 90);
        shot.x = 0; shot.y = 400;
        app.changeterrain(shot);
        assertTrue(App.newTerrainList[0] >= 400);
    }
    @Test public void outOfBoundsImpactDoesNotChangeTerrain() {
        App.Projectiles shot = app.new Projectiles(a, 90);
        shot.x = -5; shot.y = 400;
        app.changeterrain(shot);
        assertEquals(400.0, App.newTerrainList[0]);
    }
    @Test public void blastAtExactRadiusDoesNotDamageTank() {
        App.Projectiles shot = app.new Projectiles(a, 90);
        shot.x = b.x - 30; shot.y = b.y;
        app.hit(shot, 6, 15, 30);
        assertEquals(100, b.health); assertEquals(0, a.scores);
    }
    @Test public void blastOutsideRadiusDoesNotDamageTank() {
        App.Projectiles shot = app.new Projectiles(a, 90);
        shot.x = b.x - 31; shot.y = b.y;
        app.hit(shot, 6, 15, 30);
        assertEquals(100, b.health);
    }
    @Test public void deadTankDoesNotReceiveFurtherDamageOrExtraScore() {
        b.health = 0;
        App.Projectiles shot = app.new Projectiles(a, 90);
        shot.x = b.x; shot.y = b.y;
        app.hit(shot, 6, 15, 30);
        assertEquals(0, b.health); assertEquals(0, a.scores);
    }
    @Test public void fallsIgnoreDestroyedTanks() {
        b.health = 0; b.ground = false; b.y = 385;
        app.updateTankFalls(); assertEquals(385.0, b.y, 0.001);
    }
    @Test public void fallsIgnoreOffMapTanks() {
        b.x = -1; b.ground = false; b.y = 385;
        app.updateTankFalls(); assertEquals(385.0, b.y, 0.001);
    }
    @Test public void groundedTankSnapsToGroundHeight() {
        b.y = 350; b.ground = true;
        app.updateTankFalls(); assertEquals(400.0, b.y, 0.001);
    }
    @Test public void unsupportedTankWithoutParachuteDamagesItself() {
        b.parachute = 0; b.ground = false; b.y = 396;
        b.fallWithParachute = false;
        app.updateTankFalls(); assertEquals(400.0, b.y, 0.001);
        assertEquals(96, b.health); assertTrue(b.ground);
    }
    @Test public void noNegativeIndexAcceptedForFinalResultRows() {
        app.gameOverFrame = 50; app.frameCount = 50;
        assertFalse(app.isFinalScoreVisible(-1));
        app.gameOverFrame = -1; assertFalse(app.isFinalScoreVisible(0));
    }
    @Test public void unknownPlayerResultColorFallsBackWithoutCrash() {
        assertNotEquals(0, app.playerResultColor("UNKNOWN"));
    }
}
