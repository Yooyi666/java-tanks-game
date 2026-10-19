package Tanks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Basic deterministic unit tests for the supplied Tanks implementation. */
public class SampleTest {
    private App app;
    private App.Tanks tank;

    @BeforeEach
    public void setUp() {
        App.playerscolor.clear();
        App.playerscolor.put("A", "0,0,255");
        App.tanks.clear();
        App.projectiles.clear();
        App.trees.clear();
        App.wspeed = 0;
        App.newTerrainList = new double[App.WIDTH];
        app = new App();
        tank = app.new Tanks('A');
        tank.x = 100;
    }

    @Test
    public void tankStartsWithDocumentedStats() {
        assertEquals("A", tank.name);
        assertEquals(250, tank.fuel);
        assertEquals(3, tank.parachute);
        assertEquals(100, tank.health);
        assertEquals(50, tank.power);
        assertEquals(0, tank.scores);
    }

    @Test
    public void movingLeftConsumesFuel() {
        tank.tankmoveleft();
        assertEquals(98, tank.x);
        assertEquals(248, tank.fuel);
    }

    @Test
    public void movingRightConsumesFuel() {
        tank.tankmoveright();
        assertEquals(102, tank.x);
        assertEquals(248, tank.fuel);
    }

    @Test
    public void tankCannotMoveWithNoFuel() {
        tank.fuel = 0;
        tank.tankmoveleft();
        tank.tankmoveright();
        assertEquals(100, tank.x);
        assertEquals(0, tank.fuel);
    }

    @Test
    public void turretAngleClampsAtBoundaries() {
        tank.currentAngle = 0;
        tank.turretright();
        assertEquals(0.0, tank.currentAngle, 0.001);
        tank.currentAngle = 180;
        tank.turretleft();
        assertEquals(180.0, tank.currentAngle, 0.001);
    }

    @Test
    public void powerCannotExceedHealthOrGoBelowZero() {
        tank.health = 55;
        tank.power = 55;
        tank.increasepower();
        assertEquals(55, tank.power);
        tank.power = 0;
        tank.decreasepower();
        assertEquals(0, tank.power);
    }

    @Test
    public void firingCreatesProjectileOwnedByTank() {
        tank.fire();
        assertEquals(1, App.projectiles.size());
        assertSame(tank, App.projectiles.get(0).player);
    }

    @Test
    public void projectileUpdateAdvancesPosition() {
        App.Projectiles p = app.new Projectiles(tank, 45);
        int oldX = p.x;
        double oldY = p.y;
        p.p_position();
        assertNotEquals(oldX, p.x);
        assertNotEquals(oldY, p.y);
    }

    @Test
    public void projectileRemoveUpdatesSharedCollection() {
        App.Projectiles p = app.new Projectiles(tank, 45);
        App.projectiles.add(p);
        p.remove(0);
        assertTrue(App.projectiles.isEmpty());
    }
    @Test
    public void zeroPowerStartsAtMinimumSpeed() {
        tank.power = 0;
        App.Projectiles p = app.new Projectiles(tank, 90);
        assertEquals(60.0, p.speed, 1e-9);
        assertEquals(-2.0, p.vy, 1e-9);
    }

    @Test
    public void fullPowerStartsAtMaximumSpeed() {
        tank.power = 100;
        App.Projectiles p = app.new Projectiles(tank, 90);
        assertEquals(540.0, p.speed, 1e-9);
    }

    @Test
    public void projectilePowerIsLimitedByHealth() {
        tank.power = 90;
        tank.health = 20;
        App.Projectiles p = app.new Projectiles(tank, 90);
        assertEquals(156.0, p.speed, 1e-9);
    }

    @Test
    public void gravityIncreasesDownwardVelocityEachFrame() {
        App.Projectiles p = app.new Projectiles(tank, 90);
        double initial = p.vy;
        p.p_position();
        assertEquals(initial + 3.6 / App.FPS, p.vy, 1e-9);
    }

    @Test
    public void positiveWindAcceleratesProjectileRight() {
        App.wspeed = 30;
        App.Projectiles p = app.new Projectiles(tank, 90);
        double initial = p.vx;
        p.p_position();
        assertTrue(p.vx > initial);
    }

    @Test
    public void negativeWindAcceleratesProjectileLeft() {
        App.wspeed = -30;
        App.Projectiles p = app.new Projectiles(tank, 90);
        double initial = p.vx;
        p.p_position();
        assertTrue(p.vx < initial);
    }

    @Test
    public void projectileKeepsWindFromTimeOfFiring() {
        App.wspeed = 20;
        App.Projectiles p = app.new Projectiles(tank, 90);
        App.wspeed = -20;
        double initial = p.vx;
        p.p_position();
        assertTrue(p.vx > initial);
    }

    @Test
    public void fractionalHorizontalMovementIsPreserved() {
        App.Projectiles p = app.new Projectiles(tank, 89);
        double initial = p.preciseX;
        p.p_position();
        assertTrue(p.preciseX > initial);
        assertTrue(p.preciseX - initial < 1);
    }
}
