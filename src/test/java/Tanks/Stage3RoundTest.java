package Tanks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Game-state tests which do not require opening a Processing window. */
public class Stage3RoundTest {
    private App app;
    private App.Tanks a;
    private App.Tanks b;
    private App.Tanks c;

    @BeforeEach
    public void prepare() {
        App.tanks.clear();
        App.projectiles.clear();
        App.trees.clear();
        App.playerscolor.clear();
        for (String name : new String[] {"A", "B", "C"}) {
            App.playerscolor.put(name, "0,0,255");
        }
        App.newTerrainList = new double[App.WIDTH];
        for (int i = 0; i < App.newTerrainList.length; i++) App.newTerrainList[i] = 400;
        App.tturn = 0;
        App.pturn = 0;
        app = new App();
        a = app.new Tanks('A');
        b = app.new Tanks('B');
        c = app.new Tanks('C');
        a.x = 100; b.x = 200; c.x = 300;
        a.y = b.y = c.y = 400;
        App.tanks.add(a); App.tanks.add(b); App.tanks.add(c);
    }

    @Test
    public void activeTankCountExcludesDestroyedTanks() {
        assertEquals(3, app.livingTankCount());
        b.health = 0;
        assertEquals(2, app.livingTankCount());
    }

    @Test
    public void nextTurnSkipsDestroyedPlayers() {
        b.health = 0;
        app.nextTurn();
        assertSame(c, app.currentPlayer());
        app.nextTurn();
        assertSame(a, app.currentPlayer());
    }

    @Test
    public void fatalShotAwardsOnlyRemainingEnemyHealth() {
        b.health = 12;
        App.Projectiles p = app.new Projectiles(a, 90);
        p.x = 200;
        p.y = 400;
        app.hit(p, 6, 15, 30);
        assertEquals(0, b.health);
        assertEquals(12, a.scores);
    }

    @Test
    public void selfDamageNeverAwardsScore() {
        a.health = 10;
        App.Projectiles p = app.new Projectiles(a, 90);
        p.x = 100;
        p.y = 400;
        app.hit(p, 6, 15, 30);
        assertEquals(0, a.health);
        assertEquals(0, a.scores);
    }

    @Test
    public void fallingOrAliveTanksAreNotImplicitlyCountedAsKills() {
        b.ground = false;
        assertEquals(3, app.livingTankCount());
    }

    @Test
    public void allDeadHasNoCurrentPlayer() {
        a.health = b.health = c.health = 0;
        assertNull(app.currentPlayer());
    }

    @Test
    public void scoreTrackerKeepsPerPlayerIdentity() {
        a.scores = 47;
        b.scores = 12;
        app.cumulativeScores.put(a.name, a.scores);
        app.cumulativeScores.put(b.name, b.scores);
        assertEquals(47, app.cumulativeScores.get("A").intValue());
        assertEquals(12, app.cumulativeScores.get("B").intValue());
    }
}
