package Tanks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Deterministic regression checks for the two pending manual acceptance cases. */
public class Stage3PendingVerificationTest {
    private App app;
    private App.Tanks shooter;
    private App.Tanks target;

    @BeforeEach
    public void setup() {
        App.tanks.clear();
        App.projectiles.clear();
        App.trees.clear();
        App.playerscolor.clear();
        App.playerscolor.put("A", "0,0,255");
        App.playerscolor.put("B", "255,0,0");
        App.newTerrainList = new double[App.WIDTH];
        for (int x = 0; x < App.newTerrainList.length; x++) {
            App.newTerrainList[x] = 400;
        }
        App.wspeed = 0;
        App.tturn = 0;
        app = new App();
        shooter = app.new Tanks('A');
        target = app.new Tanks('B');
        shooter.x = 100;
        target.x = 300;
        shooter.y = target.y = 400;
        App.tanks.add(shooter);
        App.tanks.add(target);
    }

    @Test
    public void fatalHitCreditsOnlyRemainingTwelveHP() {
        target.health = 12;
        shooter.scores = 9;
        App.Projectiles shot = app.new Projectiles(shooter, 90);
        shot.x = target.x;
        shot.y = target.y;
        app.hit(shot, 6, 15, 30);
        assertEquals(0, target.health, "Target must be destroyed");
        assertEquals(21, shooter.scores, "Award exactly 12 actual HP, not theoretical 60");
        assertEquals(1, app.livingTankCount(), "Destroyed tank must leave active play");
    }

    @Test
    public void halfRadiusDamageIsThirtyHPAndCannotOverCredit() {
        target.health = 18;
        App.Projectiles shot = app.new Projectiles(shooter, 90);
        shot.x = target.x - 15;
        shot.y = target.y;
        app.hit(shot, 6, 15, 30);
        assertEquals(0, target.health);
        assertEquals(18, shooter.scores);
    }

    @Test
    public void firingAtSelfDoesNotIncreaseScore() {
        shooter.health = 7;
        App.Projectiles shot = app.new Projectiles(shooter, 90);
        shot.x = shooter.x;
        shot.y = shooter.y;
        app.hit(shot, 6, 15, 30);
        assertEquals(0, shooter.health);
        assertEquals(0, shooter.scores);
    }

    private App.Projectiles projectileAtWind(int wind) {
        App.wspeed = wind;
        App.Projectiles projectile = app.new Projectiles(shooter, 90);
        return projectile;
    }

    @Test
    public void rightWindProducesMoreRightwardDisplacementThanCalmOrLeftWind() {
        App.Projectiles right = projectileAtWind(35);
        App.Projectiles calm = projectileAtWind(0);
        App.Projectiles left = projectileAtWind(-35);
        // All shots share the same origin, angle, and power.
        for (int frame = 0; frame < 90; frame++) {
            right.p_position();
            calm.p_position();
            left.p_position();
        }
        assertTrue(right.preciseX > calm.preciseX, "Positive wind must push farther right");
        assertTrue(calm.preciseX > left.preciseX, "Negative wind must push farther left");
        assertEquals(calm.y, right.y, 1e-7, "Wind must not affect vertical acceleration");
    }

    @Test
    public void windIsCapturedAtLaunchRatherThanChangedMidFlight() {
        App.Projectiles right = projectileAtWind(25);
        App.wspeed = -25;
        double initialVx = right.vx;
        right.p_position();
        assertTrue(right.vx > initialVx);
        assertEquals(25, right.windAtLaunch);
    }
}
