package Tanks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Regression tests for terrain-collapse falls and final-result state, without rendering. */
public class Stage4FallingAndResultsTest {
    private App app;
    private App.Tanks shooter;
    private App.Tanks victim;

    @BeforeEach
    public void setup() {
        App.tanks.clear();
        App.projectiles.clear();
        App.trees.clear();
        App.playerscolor.clear();
        App.playerscolor.put("A", "0,0,255");
        App.playerscolor.put("B", "255,0,0");
        App.newTerrainList = new double[App.WIDTH];
        for (int i = 0; i < App.newTerrainList.length; i++) App.newTerrainList[i] = 400;
        App.tturn = 0;
        app = new App();
        shooter = app.new Tanks('A');
        victim = app.new Tanks('B');
        shooter.x = 100;
        victim.x = 300;
        shooter.y = victim.y = 400;
        App.tanks.add(shooter);
        App.tanks.add(victim);
    }

    private void collapseBelowVictim(int craterDepth) {
        App.newTerrainList[victim.x] = 400 + craterDepth;
        App.Projectiles source = app.new Projectiles(shooter, 90);
        app.onGround(source);
        assertFalse(victim.ground);
    }

    @Test
    public void noParachuteCostsOneHpForEveryFourPixelFrame() {
        victim.parachute = 0;
        collapseBelowVictim(20);
        assertSame(shooter, victim.fallCausedBy);
        app.updateTankFalls();
        assertEquals(404.0, victim.y, 0.001);
        assertEquals(96, victim.health);
        assertEquals(4, shooter.scores);
        for (int frame = 0; frame < 4; frame++) app.updateTankFalls();
        assertTrue(victim.ground);
        assertEquals(80, victim.health);
        assertEquals(20, shooter.scores);
    }

    @Test
    public void parachuteSlowsFallPreventsDamageAndIsUsedOnce() {
        victim.parachute = 3;
        collapseBelowVictim(20);
        assertTrue(victim.fallWithParachute);
        app.updateTankFalls();
        assertEquals(402.0, victim.y, 0.001);
        assertEquals(100, victim.health);
        for (int frame = 0; frame < 9; frame++) app.updateTankFalls();
        assertTrue(victim.ground);
        assertEquals(2, victim.parachute);
        assertEquals(100, victim.health);
        assertEquals(0, shooter.scores);
        app.updateTankFalls();
        assertEquals(2, victim.parachute, "Landing consumes one parachute, not one per frame");
    }

    @Test
    public void fatalFallAwardsAtMostActualRemainingHp() {
        victim.parachute = 0;
        victim.health = 3;
        collapseBelowVictim(24);
        app.updateTankFalls();
        assertEquals(0, victim.health);
        assertEquals(3, shooter.scores);
        assertEquals(1, app.livingTankCount());
    }

    @Test
    public void selfCausedFallDoesNotAwardScore() {
        victim.parachute = 0;
        App.newTerrainList[victim.x] = 420;
        App.Projectiles selfShot = app.new Projectiles(victim, 90);
        app.onGround(selfShot);
        app.updateTankFalls();
        assertEquals(96, victim.health);
        assertEquals(0, victim.scores);
    }

    @Test
    public void winnerColourMatchesOriginalPlayerColour() {
        assertEquals(shooter.Color, app.playerResultColor("A"));
        assertEquals(victim.Color, app.playerResultColor("B"));
    }

    @Test
    public void resultsAppearEveryTwentyOneFrames() {
        app.gameOverFrame = 100;
        app.frameCount = 100;
        assertTrue(app.isFinalScoreVisible(0));
        assertFalse(app.isFinalScoreVisible(1));
        app.frameCount = 121;
        assertTrue(app.isFinalScoreVisible(1));
        assertFalse(app.isFinalScoreVisible(2));
        app.frameCount = 142;
        assertTrue(app.isFinalScoreVisible(2));
    }
}
