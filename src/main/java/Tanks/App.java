
package Tanks;


import processing.core.PApplet;
import processing.core.PImage;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.event.KeyEvent;
import processing.event.MouseEvent;


import java.io.*;
import java.util.*;




/**
 * Entry point and Processing renderer for a local, turn-based artillery game.
 *
 * <p>The class currently coordinates gameplay, rendering, map resources and
 * cumulative scores. Gameplay entities are implemented as non-static inner
 * classes to preserve compatibility with the verified game and test suite.
 * State management is still coupled to the renderer; see docs/OOP_Audit_CN.md.
 */
public class App extends PApplet {

    public static final int CELLSIZE = 32; //8;
    public static final int CELLHEIGHT = 32;

    public static final int CELLAVG = 32;
    public static final int TOPBAR = 0;
    public static int WIDTH = 864; //CELLSIZE*BOARD_WIDTH;
    public static int HEIGHT = 640; //BOARD_HEIGHT*CELLSIZE+TOPBAR;
    public static final int BOARD_WIDTH = WIDTH/CELLSIZE;
    public static final int BOARD_HEIGHT = 20;

    public static final int INITIAL_PARACHUTES = 1;

    public static final int FPS = 30;//means it runs 30 frames every second//每秒帧数
    //run draw() 30 times per second

    public String configPath;
    
    //public static JSONArray levels;
    public static JSONObject json;
    public static String[][] path;//2D array
    //public static int num = 0;
    public static double[] TerrainList = new double[28];
    public static double[] newTerrainList = new double[28*32];//32 means the height of the cell
    public static ArrayList<Tanks> tanks = new ArrayList<>();
    public static ArrayList<Projectiles> projectiles = new ArrayList<>();
    public static ArrayList<Tree> trees = new ArrayList<>();
    public static HashMap<String, String> playerscolor = new HashMap<>();
    public static int pturn = 0;
    public static int tturn = 0;
    public static PImage treeImage = null;
    public static PImage background = null;

    public static Random wrandom = new Random();
    public static int wspeed = wrandom.nextInt(71) - 35;//nextInt(71) means 0-70, -35 means -35 to 35;
    public static int counter = 0;
    public static boolean dead = false;
    // Round state is based on the number of live tanks, not a fixed kill count.
    public boolean gameOver = false;
    public boolean levelComplete = false;
    public int levelCompleteFrame = -1;
    public int gameOverFrame = -1;
    public boolean shotInFlight = false;
    public final Map<String, Integer> cumulativeScores = new HashMap<String, Integer>();
    /** Preserve actual player colours across levels, including randomly assigned colours. */
    public final Map<String, Integer> playerDisplayColors = new HashMap<String, Integer>();
    private PImage fuelIcon;
    private PImage windRightIcon;
    private PImage windLeftIcon;
    /** Key state is processed once per animation frame, independently of OS key repeat. */
    private final Set<Integer> heldControlKeys = new HashSet<Integer>();
    private int lastControlFrame = Integer.MIN_VALUE;
    public int turnStartFrame = 0;
    /** Keep the turn indicator visible until the active player performs a valid action. */
    public boolean turnArrowDismissed = false;
    /** Destroyed tanks may produce a six-frame explosion, but never more than once. */
    public final List<DeathExplosion> deathExplosions = new ArrayList<DeathExplosion>();
    private final Set<Tanks> explodedTanks = new HashSet<Tanks>();
    public static final int EXPLOSION_FRAMES = 6; // 0.2 seconds at 30 FPS.
    public static final int SHIELD_COST = 20; // Extension: absorb the next non-zero blast hit.

	PImage img; // Declare a variable of type PImage
    PImage pimg;

	// Feel free to add any additional methods or attributes you want. Please put classes in different files.
    /** Mutable per-player tank state, aiming, fuel-limited movement and firing. */
    public class Tanks {
        public String name;
        public int Color;
        public int fuel;
        public int parachute;
        public int health;
        public int power;
        public int scores;
        /** One-use blast shield, purchased with score using H. */
        public boolean shieldActive;
        public int x;
        public double y;
        public boolean ground = true;
        /** Tank whose projectile removed support under this tank, if any. */
        public Tanks fallCausedBy;
        /** A parachute is committed when a fall starts, not during a later frame. */
        public boolean fallWithParachute;
        public double currentAngle = 90; // Degrees: 90 points straight up.
        private double precisePower = 50.0; // Preserve the 0.2 fractions each frame.

        /** Synchronise after external code changes the legacy integer power field. */
        private void syncPrecisePower() {
            if (power != (int) Math.round(precisePower)) precisePower = power;
            precisePower = Math.min(Math.max(0.0, precisePower), Math.max(0, health));
            power = (int) Math.round(precisePower);
        }
        public double firingPower() {
            syncPrecisePower();
            return precisePower;
        }

        public Tanks(char name){//only define the tank class
            this.name = String.valueOf(name);//transfer
            fuel = 250;
            parachute = 3;
            health = 100;//health >= power, power!<= 0 increase power decrease power
            power = 50;
            scores = 0;
            shieldActive = false;

            Integer rememberedColor = playerDisplayColors.get(this.name);
            if (rememberedColor != null) {
                this.Color = rememberedColor;
            } else {
                String configuredColor = playerscolor.get(this.name);
                if (configuredColor == null || "random".equals(configuredColor)) {
                    configuredColor = (int) random(256) + "," + (int) random(256) + "," + (int) random(256);
                }
                String[] colors = configuredColor.split(",");
                this.Color = color(Integer.parseInt(colors[0]), Integer.parseInt(colors[1]), Integer.parseInt(colors[2]));
                playerDisplayColors.put(this.name, this.Color);
            }
        }
        public void tankmoveleft() {
            int distance = Math.min(Math.min(2, Math.max(0, fuel)), Math.max(0, x));
            x -= distance;
            fuel -= distance;
        }
        public void tankmoveright() {
            int distance = Math.min(Math.min(2, Math.max(0, fuel)), Math.max(0, WIDTH - 1 - x));
            x += distance;
            fuel -= distance;
        }
        public void turretright() {
            currentAngle = Math.max(0.0, currentAngle - Math.toDegrees(3.0 / FPS));
        }
        public void turretleft() {
            currentAngle = Math.min(180.0, currentAngle + Math.toDegrees(3.0 / FPS));
        }
        public void increasepower() {
            syncPrecisePower();
            precisePower = Math.min(Math.max(0, health), precisePower + 36.0 / FPS);
            power = (int) Math.round(precisePower);
        }
        public void decreasepower() {
            syncPrecisePower();
            precisePower = Math.max(0.0, precisePower - 36.0 / FPS);
            power = (int) Math.round(precisePower);
        }
        public void fire(){
            Projectiles p = new Projectiles(this, this.currentAngle);//to create another class in this class
            projectiles.add(p);//add the projectile into the arraylist
            //System.out.println('2');
        }

        
    }
    public App() {
        this.configPath = "config.json";
    }
    /** Projectile positions use doubles so small per-frame movements are not lost. */
    /** One active projectile, including launch ownership and flight state. */
    public class Projectiles {
        public double speed;
        public int x; // Existing drawing / collision code expects an integer x.
        public double preciseX;
        public double y;
        public double theta;
        public double vx; // Pixels per frame.
        public double vy; // Pixels per frame, positive y is downward.
        public double gravity = 3.6 / FPS; // Applied each frame, as in the assignment.
        public final int windAtLaunch;
        public int explosion_phrase = 0;
        public boolean explosion = false;
        public Tanks player;

        public Projectiles(Tanks player, double theta) {
            this.player = player;
            this.theta = Math.toRadians(-theta);
            this.preciseX = player.x;
            this.x = player.x;
            this.y = newTerrainList[player.x] - 12;
            this.speed = 60 + 4.8 * Math.min(100, player.firingPower());
            this.vx = speed * Math.cos(this.theta) / FPS;
            this.vy = speed * Math.sin(this.theta) / FPS;
            this.windAtLaunch = wspeed;
        }

        /** Advance one simulation frame under gravity and horizontal wind. */
        public void p_position() {
            preciseX += vx;
            y += vy;
            x = (int) Math.round(preciseX);
            vx += windAtLaunch * 0.03 / FPS;
            vy += gravity;
        }

        public void remove(int i) {
            projectiles.remove(i);
        }
    }
    /** Delayed 0.2-second visual blast caused by a tank's destruction. */
    /** Timed visual blast generated when a tank dies or exits the map. */
    public class DeathExplosion {
        public final Tanks victim;
        public final Tanks owner;
        public final int x;
        public final double y;
        public final int radius;
        public int frame;

        public DeathExplosion(Tanks victim, Tanks owner, int radius) {
            this.victim = victim;
            this.owner = owner;
            this.radius = radius;
            this.x = victim.x;
            this.y = Math.min(HEIGHT - 1, victim.y);
        }
    }

    /** Tree position; the renderer anchors each tree to changing terrain. */
    public class Tree{
        public int x;
        public float y;
        public Tree(){
            //this.x = ;
        }
    }
    /**
     * Initialise the setting of the window size.
     */
	@Override
    public void settings() {
        size(WIDTH, HEIGHT);
    }

    /**
     * Load all resources such as images. Initialise the elements such as the player and map elements.
     */
	@Override
    public void setup() {
        frameRate(FPS);
		//See PApplet javadoc:
		json = loadJSONObject(configPath);
        JSONArray levels = json.getJSONArray("levels");
        getpath(levels);
        Initialise(); // Load level assets once, not in the render loop.
        newTerrainList();
        avgTerrain();
        avgTerrain();
        for (Tanks tank : tanks) {
            tank.y = newTerrainList[Math.max(0, Math.min(tank.x, newTerrainList.length - 1))];
        }
        //wind();
        //changewind();
        
        //JSONObject level = levels.getJSONObject(0);
        //String backgroundpath = level.getString("background");
        //String backgroundImagePath = jasono.getString("background");
        
		//loadImage(this.getClass().getResource(filename).getPath().toLowerCase(Locale.ROOT).replace("%20", " "));
    }
    
    public void getpath(JSONArray levels){
        if (levels == null){
            return;
        }
        JSONObject PlayerColor = json.getJSONObject("player_colours");
        Set<String> names = PlayerColor.keys();
        for (String key : names){
            playerscolor.put(key, PlayerColor.getString(key));//put the key and value into the hashmap
        }

        path = new String[levels.size()][4];//4 means the number of the elements in the array
        for(int i = 0; i < levels.size(); i++){
            JSONObject level = levels.getJSONObject(i);
            String layoutpath = level.getString("layout");
            String backgroundpath = level.getString("background");
            String fcpath = level.getString("foreground-colour");
            String treepath = level.hasKey("trees") ? level.getString("trees") : null;
            path[i][0] = layoutpath;
            path[i][1] = backgroundpath;//image
            path[i][2] = fcpath;
            path[i][3] = treepath;//image
            
        }
    }
    /** Render preloaded images. Kept as a legacy wrapper for earlier call sites. */
    public void loadImage() {
        if (background != null) image(background, 0, 0, WIDTH, HEIGHT);
        if (fuelIcon != null) image(fuelIcon, 160, 6, 25, 25);
        if (pimg != null) image(pimg, 160, 35, 25, 25);
    }

    /** Reset the level and load its map, retaining cumulative score by player name. */
    public void reset(){
        for (Tanks tank : tanks) {
            cumulativeScores.put(tank.name, tank.scores);
        }
        tanks.clear();
        projectiles.clear();
        trees.clear();
        TerrainList = new double[28];
        newTerrainList = new double[28 * 32];
        tturn = 0;
        counter = 0;
        shotInFlight = false;
        heldControlKeys.clear();
        lastControlFrame = Integer.MIN_VALUE;
        deathExplosions.clear();
        explodedTanks.clear();
        turnStartFrame = frameCount;
        turnArrowDismissed = false;
        levelComplete = false;
        levelCompleteFrame = -1;
        Initialise();
        newTerrainList();
        avgTerrain();
        avgTerrain();
        for (Tanks tank : tanks) {
            tank.scores = cumulativeScores.containsKey(tank.name) ? cumulativeScores.get(tank.name) : 0;
            tank.y = newTerrainList[Math.max(0, Math.min(tank.x, newTerrainList.length - 1))];
        }
    }

    public int livingTankCount() {
        int alive = 0;
        for (Tanks tank : tanks) if (tank.health > 0) alive++;
        return alive;
    }

    /** Returns the live tank whose turn it is, or null when none remain. */
    public Tanks currentPlayer() {
        if (tanks.isEmpty()) return null;
        for (int offset = 0; offset < tanks.size(); offset++) {
            int index = (tturn + offset) % tanks.size();
            if (tanks.get(index).health > 0) {
                tturn = index;
                return tanks.get(index);
            }
        }
        return null;
    }

    public void nextTurn() {
        if (tanks.isEmpty()) return;
        tturn = (tturn + 1) % tanks.size();
        currentPlayer();
        turnStartFrame = frameCount;
        turnArrowDismissed = false;
        heldControlKeys.clear();
        changewind();
    }

    /** Process terminal state only after the current projectile has resolved. */
    public void updateRoundState() {
        if (gameOver) return;
        if (!projectiles.isEmpty() || !deathExplosions.isEmpty()) return;
        if (shotInFlight) {
            // The next player's two-second indicator starts after the shot resolves,
            // not while the previous player's projectile is still flying.
            shotInFlight = false;
            turnStartFrame = frameCount;
        }
        int alive = livingTankCount();
        if (alive <= 1 && !levelComplete) {
            levelComplete = true;
            levelCompleteFrame = frameCount;
        }
        if (levelComplete) {
            if (frameCount - levelCompleteFrame >= FPS) {
                if (pturn + 1 < path.length) {
                    pturn++;
                    reset();
                } else {
                    for (Tanks tank : tanks) cumulativeScores.put(tank.name, tank.scores);
                    gameOver = true;
                    gameOverFrame = frameCount;
                }
            }
        }
    }

    public void restartGame() {
        cumulativeScores.clear();
        pturn = 0;
        playerDisplayColors.clear();
        gameOver = false;
        gameOverFrame = -1;
        heldControlKeys.clear();
        // A new game must not re-save the outgoing level's scores inside reset().
        tanks.clear();
        reset();
        wspeed = wrandom.nextInt(71) - 35;
    }

    public void Initialise(){
        String[] ls = loadStrings(path[pturn][0]);//load the file
        treeImage = path[pturn][3] == null ? null : loadImage("src/main/resources/Tanks/" + path[pturn][3]);
        background = loadImage("src/main/resources/Tanks/" + path[pturn][1]);
        if (fuelIcon == null) fuelIcon = loadImage("src/main/resources/Tanks/fuel.png");
        if (pimg == null) pimg = loadImage("src/main/resources/Tanks/parachute.png");
        if (windRightIcon == null) windRightIcon = loadImage("src/main/resources/Tanks/wind.png");
        if (windLeftIcon == null) windLeftIcon = loadImage("src/main/resources/Tanks/wind-1.png");

        //tankscolor
        if (ls == null){
            return;
        }
        //fill(255);
        //image(background, 0,0, WIDTH, HEIGHT);
        //image(img,0,0, WIDTH, HEIGHT);

        for (int i = 0; i < ls.length; i++){//y height
            String lss = ls[i];
            for (int j = 0; j < Math.min(lss.length(), TerrainList.length); j++){//x
                if(lss.charAt(j) == 'X'){
                    TerrainList[j] = i*CELLSIZE;//y height when x = i
                }
                if(lss.charAt(j) == 'T'){
                    if(treeImage != null){
                        Tree tree = new Tree();
                        tree.x = j*CELLSIZE;
                        trees.add(tree);
                    }
                    //drawtree(j*CELLSIZE, (float)TerrainList[j]);//image x y width length// change double(64 bit) to float(32 bit)
        

                }
                if(lss.charAt(j) == 'A' || lss.charAt(j) == 'B' || lss.charAt(j) == 'C' || lss.charAt(j) == 'D'){
                    Tanks player = new Tanks(lss.charAt(j));//parameter is the tanks name// we have to create the class tanks
                    player.x = j*CELLSIZE;//define x//cannot draw here, tanks cannot move//this line is to define the location of x of the tank 
                    player.scores = cumulativeScores.containsKey(player.name) ? cumulativeScores.get(player.name) : 0;
                    tanks.add(player);//add the tank into the arraylist
                    //Collections.sort(tanks);
                }
            }
        }
    //sort the tanks
    Collections.sort(tanks, new Comparator<Tanks>() {
        @Override
        public int compare(Tanks t1, Tanks t2) {
            // Players must take turns alphabetically, not by terrain position.
            return t1.name.compareTo(t2.name);
        }
    });
}
    
    public void drawtree(){
        //loadImage();
        for (int i = 0; i < trees.size(); i++){
            Tree tree = trees.get(i);
            image(treeImage, tree.x-15, (float)newTerrainList[tree.x]-30, CELLSIZE, CELLSIZE);
        }
    }
    public void drawterrain(){
        loadImage();//if we didn't add loadimage, it will show several tanks.
        String fcolor = path[pturn][2];
        String[] color = fcolor.split(",");
        int r = Integer.parseInt(color[0]);
        int g = Integer.parseInt(color[1]);
        int b = Integer.parseInt(color[2]);
        
        for(int i = 0; i < newTerrainList.length; i++){  
            stroke(r, g, b);//笔划颜色
            strokeWeight(1);//笔划宽度
            line((float) i,(float) (newTerrainList[i]),(float) i,(float)HEIGHT);//draw the line
        } 
    }
    /**
     * Update tank falls without invoking Processing drawing operations.
     * Every downward pixel without a parachute costs 1 HP; the player who
     * caused the ground collapse receives only the HP actually removed.
     */
    public void updateTankFalls() {
        for (Tanks tank : tanks) {
            if (tank.health <= 0) continue;
            if (tank.x < 0 || tank.x >= newTerrainList.length) continue;
            double groundY = newTerrainList[tank.x];
            if (tank.ground) {
                tank.y = groundY;
                if (tank.y >= HEIGHT) {
                    tank.health = 0;
                    queueDeathExplosion(tank, tank.fallCausedBy, 30);
                }
                continue;
            }
            double speed = tank.fallWithParachute ? 60.0 / FPS : 120.0 / FPS;
            double destination = Math.min(groundY, tank.y + speed);
            double travelled = Math.max(0.0, destination - tank.y);
            tank.y = destination;
            if (!tank.fallWithParachute && travelled > 0.0) {
                int actualLoss = Math.min(tank.health, (int) Math.round(travelled));
                tank.health -= actualLoss;
                if (tank.fallCausedBy != null && tank.fallCausedBy != tank) {
                    tank.fallCausedBy.scores += actualLoss;
                }
            }
            if (tank.y >= HEIGHT) {
                tank.health = 0;
                queueDeathExplosion(tank, tank.fallCausedBy, 30);
                tank.ground = true;
                tank.fallCausedBy = null;
            } else if (tank.health <= 0) {
                queueDeathExplosion(tank, tank.fallCausedBy, 15);
                tank.ground = true;
                tank.fallCausedBy = null;
            } else if (tank.y >= groundY - 0.0001) {
                tank.y = groundY;
                tank.ground = true;
                if (tank.fallWithParachute) tank.parachute = Math.max(0, tank.parachute - 1);
                tank.fallWithParachute = false;
                tank.fallCausedBy = null;
            }
        }
    }

    public void drawtank(){
        for (int i = 0; i < tanks.size(); i++){//tanks.size() means the number of tanks
            Tanks player = tanks.get(i);
            if (player.health > 0){
                // A semi-transparent blue bubble visibly indicates an equipped shield.
                if (player.shieldActive) {
                    noStroke();
                    fill(60, 145, 255, 80);
                    ellipse((float) player.x, (float) player.y - 9f, 40f, 38f);
                }
                //player.sort(tanks);
                fill(tanks.get(i).Color);

                //draw the tank
                noStroke();//no stroke
                strokeWeight(1);
                rect(player.x-10, (float)player.y-5, 20, 5);//draw the tank
                rect(player.x-7, (float)player.y-10, 15, 5);
                //rect(x, y, width, height)
                

                //draw the turret
                float linex = (float)player.x;
                float liney = (float)player.y-10;
                float r = 15;//the length of the line

                float theta = (float)player.currentAngle;
                float radians = radians(-theta);
                float x1 = linex + r * cos(radians);
                float y1 = liney + r * sin(radians);

                stroke(0);
                strokeWeight(3);
                line(linex, liney, x1, y1);//draw the line

                if (!player.ground && player.fallWithParachute && pimg != null) {
                    image(pimg, (float)player.x - 32, (float)player.y - 64, 64, 64);
                }
            }     
        }
    }

    /** Draw and advance projectiles; sample the path to avoid skipping terrain. */
    public void drawprojectiles() {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectiles p = projectiles.get(i);
            noStroke();
            fill(p.player.Color);
            ellipse((float) p.preciseX, (float) p.y, 5, 5);

            if (p.explosion) {
                drawexplosions(p, i);
                continue;
            }

            double startX = p.preciseX;
            double startY = p.y;
            p.p_position();
            double endX = p.preciseX;
            double endY = p.y;
            int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(endX - startX), Math.abs(endY - startY))));
            boolean finished = false;
            for (int step = 1; step <= steps; step++) {
                double fraction = step / (double) steps;
                double sampleX = startX + (endX - startX) * fraction;
                double sampleY = startY + (endY - startY) * fraction;
                int column = (int) Math.round(sampleX);
                if (column < 0 || column >= newTerrainList.length || sampleY > HEIGHT) {
                    projectiles.remove(i);
                    finished = true;
                    break;
                }
                if (sampleY >= newTerrainList[column]) {
                    p.preciseX = sampleX;
                    p.x = column;
                    p.y = sampleY;
                    drawexplosions(p, i);
                    finished = true;
                    break;
                }
            }
            if (!finished && p.preciseX < 0) {
                projectiles.remove(i);
            }
        }
    }

    /** Render circular radii, not diameters: Processing ellipse() accepts a diameter. */
    public void paintExplosion(double x, double y, int radius, int frame) {
        double fraction = Math.min(1.0, (frame + 1.0) / EXPLOSION_FRAMES);
        noStroke();
        fill(255, 0, 0);
        ellipse((float) x, (float) y, (float) (2 * radius * fraction), (float) (2 * radius * fraction));
        fill(255, 165, 0);
        ellipse((float) x, (float) y, (float) (radius * fraction), (float) (radius * fraction));
        fill(255, 255, 0);
        ellipse((float) x, (float) y, (float) (0.4 * radius * fraction), (float) (0.4 * radius * fraction));
    }

    /** A projectile explosion lasts exactly six frames before dealing damage. */
    public void drawexplosions(Projectiles p, int index) {
        p.explosion = true;
        paintExplosion(p.x, p.y, 30, p.explosion_phrase);
        p.explosion_phrase++;
        if (p.explosion_phrase >= EXPLOSION_FRAMES) {
            hit(p, 6, 15, 30);
            changeterrain(p);
            onGround(p);
            projectiles.remove(p);
        }
    }

    /** Schedule one explosion per dead tank; never create endless chain explosions. */
    public void queueDeathExplosion(Tanks victim, Tanks cause, int radius) {
        if (explodedTanks.add(victim)) {
            deathExplosions.add(new DeathExplosion(victim, cause, radius));
        }
    }

    public void drawDeathExplosions() {
        for (int i = deathExplosions.size() - 1; i >= 0; i--) {
            DeathExplosion effect = deathExplosions.get(i);
            paintExplosion(effect.x, effect.y, effect.radius, effect.frame);
            effect.frame++;
            if (effect.frame >= EXPLOSION_FRAMES) {
                applyBlastDamage(effect.owner, effect.x, effect.y, effect.radius, effect.victim);
                carveCrater(effect.x, effect.y, effect.radius);
                checkUnsupportedTanks(effect.owner);
                deathExplosions.remove(effect);
            }
        }
    }

    public void onGround(Projectiles p) {
        checkUnsupportedTanks(p.player);
    }

    private void checkUnsupportedTanks(Tanks shooter) {
        for (Tanks tank : tanks) {
            if (tank.health > 0 && tank.x >= 0 && tank.x < newTerrainList.length
                    && tank.ground && tank.y < newTerrainList[tank.x] - 0.5) {
                tank.ground = false;
                tank.fallWithParachute = tank.parachute > 0;
                tank.fallCausedBy = shooter;
            }
        }
    }
    public void changewind(){//????????
        wspeed = wspeed + (wrandom.nextInt(11) - 5);//nextInt(11) means 0-10, -5 means -5 to 5
        if (wspeed > 35){
            wspeed = 35;
        }
        else if (wspeed < -35){
            wspeed = -35;
        }
    }
    public float d(float px, float py, float player_point, float newTerrainList){
        float distance = sqrt((float)Math.pow(player_point - px, 2) + (float)Math.pow(newTerrainList - py, 2));
        return distance;
    }
    /**
     * Shield extension: spend 20 points to protect against one damaging blast.
     * An already equipped shield cannot be purchased twice.
     */
    public boolean purchaseShield(Tanks buyer) {
        if (buyer == null || buyer.health <= 0 || buyer.shieldActive
                || buyer.scores < SHIELD_COST) return false;
        buyer.scores -= SHIELD_COST;
        buyer.shieldActive = true;
        return true;
    }

    /** Apply falloff once to each living tank; score only effective enemy damage. */
    public void hit(Projectiles p, int yellowRadius, int orangeRadius, int redRadius) {
        applyBlastDamage(p.player, p.x, p.y, redRadius, null);
    }

    private void applyBlastDamage(Tanks shooter, double x, double y, int radius, Tanks victim) {
        for (Tanks target : tanks) {
            if (target.health <= 0 || target == victim) continue;
            double distance = Math.hypot(target.x - x, target.y - y);
            if (distance >= radius) continue;
            int damage = Math.max(0, (int) (60.0 * (radius - distance) / radius));
            // Only a damaging blast consumes the single-use shield.
            // No HP is lost and the shooter earns no score for an absorbed hit.
            if (damage > 0 && target.shieldActive) {
                target.shieldActive = false;
                continue;
            }
            int actualDamage = Math.min(target.health, damage);
            target.health -= actualDamage;
            if (shooter != null && shooter != target) shooter.scores += actualDamage;
            if (target.health <= 0) queueDeathExplosion(target, shooter, 15);
        }
    }

    public void changeterrain(Projectiles p) {
        carveCrater(p.x, p.y, 30);
    }

    /** Deform columns within the given circular blast radius. */
    private void carveCrater(int x, double y, int radius) {
        if (x < 0 || x >= newTerrainList.length) return;
        for (int column = Math.max(0, x - radius);
             column <= Math.min(newTerrainList.length - 1, x + radius); column++) {
            double dy = Math.sqrt(Math.max(0.0, radius * radius - (column - x) * (column - x)));
            double upper = y - dy;
            double lower = y + dy;
            if (newTerrainList[column] < upper) {
                newTerrainList[column] += dy;
            } else if (newTerrainList[column] < lower) {
                newTerrainList[column] = lower;
            }
        }
    }

    /** Keep the turn indicator visible until the player first interacts with the tank. */
    public boolean shouldShowTurnArrow() {
        if (gameOver || levelComplete || shotInFlight || turnArrowDismissed) return false;
        Tanks current = currentPlayer();
        return current != null && current.health > 0;
    }

    /** Draw a small, entirely black arrow with a gap above the tank's turret. */
    public void drawTurnArrow() {
        if (!shouldShowTurnArrow()) return;
        Tanks current = currentPlayer();
        float centerX = (float) current.x;
        // The turret extends approximately 25 px above tank.y; this leaves a 30 px gap.
        float arrowTipY = (float) current.y - 55f;
        stroke(0);
        strokeWeight(2);
        line(centerX, arrowTipY - 26f, centerX, arrowTipY - 9f);
        noStroke();
        fill(0);
        triangle(centerX - 6f, arrowTipY - 11f, centerX + 6f, arrowTipY - 11f, centerX, arrowTipY);
        strokeWeight(1);
    }

    /** Draw scores for actual configured players and HUD for the living current tank. */
    public void drawscoreboard() {
        stroke(0);
        fill(255, 235);
        rect(708, 48, 143, Math.max(35, 20 + 20 * tanks.size()));
        fill(0);
        textSize(16);
        text("Scores", 714, 65);
        List<Tanks> ordered = new ArrayList<Tanks>(tanks);
        Collections.sort(ordered, new Comparator<Tanks>() {
            public int compare(Tanks a, Tanks b) { return a.name.compareTo(b.name); }
        });
        for (int i = 0; i < ordered.size(); i++) {
            Tanks tank = ordered.get(i);
            fill(tank.Color);
            text("Player " + tank.name, 714, 85 + 20 * i);
            text(tank.scores, 821, 85 + 20 * i);
        }
        Tanks current = currentPlayer();
        if (current == null) return;
        fill(0);
        text("Player " + current.name + "'s turn", 20, 25);
        text(current.fuel, 185, 25);
        text(current.parachute, 195, 55);
        text("Health:", 380, 25);
        text(current.health, 595, 25);
        text("Power:", 380, 55);
        text(current.power, 445, 55);
        if (current.shieldActive) {
            fill(20, 75, 185);
            text("Shield ON", 550, 55);
        }
        stroke(0);
        fill(255);
        rect(445, 10, 150, 20);
        noStroke();
        fill(current.Color);
        rect(445, 12, 150 * current.health / 100f, 17);
        noFill();
        stroke(150);
        strokeWeight(3);
        rect(445, 10, 150 * current.power / 100f, 20);
        stroke(255, 0, 0);
        line(445 + 150 * current.power / 100f, 3, 445 + 150 * current.power / 100f, 40);
        strokeWeight(1);
        fill(0);
        text(Math.abs(wspeed), 820, 30);
        PImage windImage = wspeed >= 0 ? windRightIcon : windLeftIcon;
        if (windImage != null) image(windImage, 760, 3, 50, 50);
        if (pimg != null) image(pimg, 160, 35, 25, 25);
    }

    /** Tests and UI use the same winner identity and timed score reveal. */
    public boolean isFinalScoreVisible(int index) {
        return index >= 0 && gameOverFrame >= 0
                && frameCount - gameOverFrame >= Math.round(index * 0.7f * FPS);
    }

    /** Return the actual colour assigned to a player (also handles random colours). */
    public int playerResultColor(String name) {
        Integer remembered = playerDisplayColors.get(name);
        if (remembered != null) return remembered;
        for (Tanks tank : tanks) if (tank.name.equals(name)) return tank.Color;
        return color(120, 120, 120);
    }

    /** Show ranked, staggered results after the last level ends. */
    public void drawFinalResults() {
        List<Map.Entry<String, Integer>> ranking = new ArrayList<Map.Entry<String, Integer>>(cumulativeScores.entrySet());
        Collections.sort(ranking, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                int scoreOrder = Integer.compare(b.getValue(), a.getValue());
                return scoreOrder != 0 ? scoreOrder : a.getKey().compareTo(b.getKey());
            }
        });
        int winnerColor = ranking.isEmpty() ? color(120, 120, 120)
                : playerResultColor(ranking.get(0).getKey());
        int lighterRed = Math.min(255, ((winnerColor >> 16) & 255) + 55);
        int lighterGreen = Math.min(255, ((winnerColor >> 8) & 255) + 55);
        int lighterBlue = Math.min(255, (winnerColor & 255) + 55);
        noStroke();
        fill(lighterRed, lighterGreen, lighterBlue, 185);
        rect(175, 155, 515, 335);
        textAlign(CENTER);
        textSize(27);
        fill(0);
        if (!ranking.isEmpty()) text("Player " + ranking.get(0).getKey() + " wins!", WIDTH / 2f, 215);
        textSize(20);
        for (int i = 0; i < ranking.size(); i++) {
            if (isFinalScoreVisible(i)) {
                Map.Entry<String, Integer> row = ranking.get(i);
                text("Player " + row.getKey() + "  -  " + row.getValue(), WIDTH / 2f, 265 + 34 * i);
            }
        }
        textSize(15);
        text("Press R to restart", WIDTH / 2f, 460);
        textAlign(LEFT);
    }

    public void newTerrainList(){
        for (int i = 0; i < TerrainList.length; i++) {//use two for loop to draw the line
            for (int j = 0; j < 32; j++){
                newTerrainList[i*32+j] = TerrainList[i];//32 means the height of the cell
                //896 newTerrainList.length //28 TerrainList.length
            }
        }

    }
    /** Independent 32-sample moving average; call twice for two full passes. */
    public void avgTerrain() {
        double[] source = Arrays.copyOf(newTerrainList, newTerrainList.length);
        for (int i = 0; i < newTerrainList.length; i++) {
            double sum = 0.0;
            for (int offset = 0; offset < CELLAVG; offset++) {
                sum += source[Math.min(source.length - 1, i + offset)];
            }
            newTerrainList[i] = sum / CELLAVG;
        }
    }

    

    /** Apply movement and aim at the fixed 30 FPS step. */
    private void applyControl(Tanks player, int code) {
        if (code == LEFT) player.tankmoveleft();
        else if (code == RIGHT) player.tankmoveright();
        else if (code == UP) player.turretleft();
        else if (code == DOWN) player.turretright();
        else if (code == 'W') player.increasepower();
        else if (code == 'S') player.decreasepower();
    }

    private boolean isHeldControl(int code) {
        return code == LEFT || code == RIGHT || code == UP || code == DOWN
                || code == 'W' || code == 'S';
    }

    /** Process held keys once per frame rather than relying on OS key-repeat speed. */
    public void updateHeldControls() {
        if (lastControlFrame == frameCount || gameOver || levelComplete || shotInFlight) return;
        Tanks player = currentPlayer();
        if (player == null || !player.ground) return;
        if (!heldControlKeys.isEmpty()) turnArrowDismissed = true;
        for (Integer code : heldControlKeys) applyControl(player, code);
        lastControlFrame = frameCount;
    }

    @Override
    public void keyPressed(KeyEvent event) {
        int code = event.getKeyCode();
        if (code >= 'a' && code <= 'z') code = Character.toUpperCase((char) code);
        if (gameOver) {
            if (code == 'R') restartGame();
            return;
        }
        if (levelComplete && code == ' ') {
            if (pturn + 1 < path.length) {
                pturn++;
                reset();
            } else {
                for (Tanks tank : tanks) cumulativeScores.put(tank.name, tank.scores);
                gameOver = true;
                gameOverFrame = frameCount;
            }
            return;
        }
        if (shotInFlight || levelComplete) return;
        Tanks player = currentPlayer();
        if (player == null || !player.ground) return;

        if (isHeldControl(code)) {
            if (heldControlKeys.add(code)) {
                turnArrowDismissed = true;
                applyControl(player, code);
                lastControlFrame = frameCount;
            }
            return;
        }
        if (code == ' ') {
            turnArrowDismissed = true;
            player.fire();
            shotInFlight = true;
            nextTurn();
        } else if (code == 'R' && player.scores >= 20) {
            turnArrowDismissed = true;
            player.scores -= 20;
            player.health = Math.min(100, player.health + 20);
        } else if (code == 'F' && player.scores >= 10) {
            turnArrowDismissed = true;
            player.fuel += 200;
            player.scores -= 10;
        } else if (code == 'P' && player.scores >= 15) {
            turnArrowDismissed = true;
            player.parachute++;
            player.scores -= 15;
        } else if (code == 'H') {
            if (purchaseShield(player)) turnArrowDismissed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        int code = event.getKeyCode();
        if (code >= 'a' && code <= 'z') code = Character.toUpperCase((char) code);
        heldControlKeys.remove(code);
    }

    @Override
    public void keyReleased() {
        heldControlKeys.remove(keyCode);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        //TODO - powerups, like repair and extra fuel and teleport


    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    /**
     * Draw all elements in the game by current frame.
     */
	@Override
    public void draw() {
        updateHeldControls();
        drawterrain();
        if (!gameOver) {
            updateTankFalls();
            drawtank();
            drawTurnArrow();
            drawprojectiles();
            drawDeathExplosions();
            drawtree();
            updateRoundState();
            drawscoreboard();
        } else {
            drawFinalResults();
        }
        

        //----------------------------------
        //display HUD:
        //----------------------------------
        //TODO

        //----------------------------------
        //display scoreboard:
        //----------------------------------
        //TODO
        
		//----------------------------------
        //----------------------------------

        //TODO: Check user action
    }


    public static void main(String[] args) {
        PApplet.main("Tanks.App");
    }
}

