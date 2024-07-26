import game2D.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Student ID: 2932358

@SuppressWarnings("serial")
public class Game extends GameCore {
    // game constants
    static int screenWidth = 512;
    static int screenHeight = 384;
    float gravity = 0.0003f;
    float moveSpeed = 0.3f;
    float bouncingSpeed = -0.25f;
    long total;
    long lastSoundTime = 0;
    int health = 100;
    int level = 1;

    // game state flags
    boolean jump = false;
    boolean moveRight = false;
    boolean moveLeft = false;
    boolean debug = false;
    boolean hasCollidedWithTop = false;
    boolean hasCollidedWithBottom = false;
    boolean hurt;
    boolean playFallingNoise = false;
    boolean displayStartingMenu = true;
    boolean displayGame = false;
    boolean displayGameOverMenu = false;
    boolean isGamePaused = true;
    boolean firing;

    // Game resources
    Image bgImage = loadImage("images/background.png");
    Image lookLeft = loadImage("images/lookLeft.png");
    Image lookRight = loadImage("images/lookRight.png");
    Sprite girl = null;
    Sprite obstacle = null;
    Sprite brokenPlatform = null;
    Sprite heart = null;
    Sprite door = null;
    Sprite notebook = null;
    Sprite bullet = null;
    Sprite menuButton = null;
    Sprite playButton = null;
    Sprite playAgainButton = null;
    Sprite quitButton = null;
    Sprite gameOverSprite = null;
    ArrayList<Sprite> obstacles = new ArrayList<Sprite>();
    ArrayList<Sprite> brokenPlatforms = new ArrayList<Sprite>();
    ArrayList<Sprite> hearts = new ArrayList<Sprite>();
    ArrayList<Tile> collidedTiles = new ArrayList<Tile>();
    ArrayList<Sprite> activeBullets = new ArrayList<Sprite>();
    ArrayList<Sprite> notebooks = new ArrayList<Sprite>();
    ArrayList<Sprite> menuButtons = new ArrayList<Sprite>();
    TileMap tmap = new TileMap();    // Our tile map, note that we load it in init()
    String animationNames[] = {"idling","jumping", "flying", "falling", "dying", "shooting"};
    String FOLDER_PREFIX = "/";
    String IMAGE_SUFFIX = ".png";
    // map of animation names to durations
    Map<String, long[]> durationMap = new HashMap<>();
    Map<String, Animation> animationMap = new HashMap<>();
    Sound bounceSound;
    Sound fallingSound;
    Sound heartCollectionSound;
    Sound obstacleCollisionSound;

    /**
     * The obligatory main method that creates
     * an instance of our class and starts it running
     *
     * @param args The list of parameters this program might use (ignored)
     */
    public static void main(String[] args) throws Exception {
        PlayMIDI player = new PlayMIDI();
        player.play("sounds/Never-Gonna-Give-You-Up.mid");

        Game gct = new Game();
        gct.init("menu.txt");
        gct.setWindowLayout();
        gct.run(false, screenWidth, screenHeight);
    }

    /**
     * Initialise the class, e.g. set up variables, load images,
     * create animations, register event handlers.
     * <p>
     * This shows you the general principles but you should create specific
     * methods for setting up your game that can be called again when you wish to
     * restart the game (for example you may only want to load animations once
     * but you could reset the positions of sprites each time you restart the game).
     */
    public void init(String mapName) {
        if (debug) System.out.println("this is init at level " + level);
        tmap.loadMap("maps",mapName);
        addDurations();

        // create animations for the girl
        loopAnimationFrom(0, 1, "images");

        // create animations for the menu buttons
        createAnimation("playButton", 0, 0, "images", 0);
        createAnimation("playAgainButton", 0, 0, "images", 0);
        createAnimation("quitButton", 0, 0, "images", 0);
        createAnimation("gameOverSprite", 0, 0, "images", 0);

        // create animation for the other elements of the game, adding them to the animationMap
        createAnimation("idlingPlatform", 0, 0, "platform", 200);
        createAnimation("breakingPlatform", 0, 5, "platform", 200);
        createAnimation("floatingObstacle", 0, 5, "images", 6000);
        createAnimation("spinningHeart",0, 3, "images", 3000);
        createAnimation("openingDoor", 0, 1, "images", 200);

        // creating sprites for the elements of the game, initialised on a specific animation
        girl = new Sprite(animationMap.get("idling"));
        obstacle = new Sprite(animationMap.get("floatingObstacle"));
        brokenPlatform = new Sprite(animationMap.get("idlingPlatform"));
        heart = new Sprite(animationMap.get("spinningHeart"));
        door = new Sprite(animationMap.get("openingDoor"));
        animationMap.get("openingDoor").setLoop(false);
        animationMap.get("openingDoor").setAnimationFrame(0);

        playAgainButton = new Sprite(animationMap.get("playAgainButton"));
        playButton = new Sprite(animationMap.get("playButton"));
        quitButton = new Sprite(animationMap.get("quitButton"));
        gameOverSprite = new Sprite(animationMap.get("gameOverSprite"));

        initialiseGame();
    }


    /**
     * Check if a sprite has been clicked
     * @param e the mouse event
     */
    public void click(MouseEvent e) {
        int mouseX = e.getX();
        int mouseY = e.getY();

        if (isSpriteClicked(playButton, mouseX, mouseY)) {
            displayStartingMenu = false;
            displayGameOverMenu = false;
            displayGame = true;
            initialiseGame();
        }

        if (isSpriteClicked(quitButton, mouseX, mouseY)) {
            level = 1;
            System.exit(0);
        }

        if (isSpriteClicked(playAgainButton, mouseX, mouseY)) {
            // you can't see this, it should be for hovering not clicking
            displayStartingMenu = false;
            displayGameOverMenu = false;
            displayGame = true;
            level = 1;
            initialiseGame();
        }
        e.consume();
    }

    /**
     * Resets variables and sets the tile maps and position of sprites depending on the flags
     */
    public void initialiseGame() {
        total = 0;
        health = 100;

        if (displayStartingMenu) {
            if (debug) System.out.println(tmap);
            playButton.setPosition(screenWidth/2+40, screenHeight/2);
            quitButton.setPosition(screenWidth/2+40, screenHeight/2+100);
        }

        if (displayGame) {
            if (debug) System.out.println("this is displayGame at level " + level);
            if (level == 1) {
                if (debug) { System.out.print("1"); }

                // BACKGROUND NOTEBOOK
                Animation notebookAnim = new Animation();
                notebookAnim.addFrame(loadImage("images/background.png"), 1000); // has just one image, so no need to add more frames
                setNotebooksPosition(notebookAnim);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                tmap.loadMap("maps", "map.txt");

                // OBSTACLES
                // placing them at different levels, increasing their presence when we go up
                placeCharacters('x', 5, tmap, 60, 50);
                placeCharacters('x', 25, tmap, 50, 20);
                placeCharacters('x', 20, tmap, 20, 1);
                obstacles = placeTileSprite(tmap, 'x', animationMap.get("floatingObstacle"), 0.01f, 0.00f, true);
                // change the velocity of the obstacles at a given threshold
                modifyVelocity(obstacles, 40, 0.1f);
                modifyVelocity(obstacles, 20, 0.2f);

                // BROKEN PLATFORMS
                placeCharacters('y', 15, tmap, 60, 1);
                brokenPlatforms = placeTileSprite(tmap, 'y', animationMap.get("idlingPlatform"), 0.02f, 0.00f, true);

                // HEART
                placeCharacters('h', 10, tmap, 60, 1);
                hearts = placeTileSprite(tmap, 'h', animationMap.get("spinningHeart"), 0.00f, 0.00f, true);

                // BULLET
                Animation bulletAnim = new Animation();
                bulletAnim.addFrame(loadImage("images/bullet.png"), 1000); // has just one image, so no need to add more frames
                bullet = new Sprite(bulletAnim);

                // PORTAL TO NEXT LEVEL
                ArrayList<Sprite> doors = placeTileSprite(tmap, 'p', animationMap.get("openingDoor"), 0.00f, 0.00f, false);
                if (doors.size() > 0) {
                    door = doors.get(0);
                }
                door.setY(door.getY() - 40);

                // GIRL
                ArrayList<Sprite> girls = placeTileSprite(tmap, 'a', animationMap.get("idling"), 0.00f, 0.00f, false);
                if (girls.size() > 0) {
                    girl = girls.get(0);
                }
                int someSpace = 250; // some space above the tile to position the girl at the beginning
                girl.setY(girl.getY() - someSpace);
                girl.setVelocity(0, 0);
                girl.show();
            }
            if (level == 2)  {
                if (debug) System.out.print("2");

                // BACKGROUND NOTEBOOK
                Animation notebookAnim = new Animation();
                notebookAnim.addFrame(loadImage("images/background.png"), 1000); // has just one image, so no need to add more frames
                setNotebooksPosition(notebookAnim);

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                tmap.loadMap("maps", "map2.txt");

                // OBSTACLES
                // placing them at different levels, increasing their presence when we go up
                placeCharacters('x', 10, tmap, 55, 50);
                placeCharacters('x', 35, tmap, 50, 20);
                placeCharacters('x', 20, tmap, 20, 1);
                obstacles = placeTileSprite(tmap, 'x', animationMap.get("floatingObstacle"), 0.01f, 0.00f, true);
                // change the velocity of the obstacles at a given threshold
                modifyVelocity(obstacles, 40, 0.2f);
                modifyVelocity(obstacles, 20, 0.3f);

                // BROKEN PLATFORMS
                placeCharacters('y', 15, tmap, 60, 1);
                brokenPlatforms = placeTileSprite(tmap, 'y', animationMap.get("idlingPlatform"), 0.02f, 0.00f, true);

                // HEART
                placeCharacters('h', 10, tmap, 60, 1);
                hearts = placeTileSprite(tmap, 'h', animationMap.get("spinningHeart"), 0.00f, 0.00f, true);

                // BULLET
                Animation bulletAnim = new Animation();
                bulletAnim.addFrame(loadImage("images/bullet.png"), 1000); // has just one image, so no need to add more frames
                bullet = new Sprite(bulletAnim);

                // PORTAL TO NEXT LEVEL
                ArrayList<Sprite> doors = placeTileSprite(tmap, 'p', animationMap.get("openingDoor"), 0.00f, 0.00f, false);
                if (doors.size() > 0) {
                    door = doors.get(0);
                }

                // GIRL
                // set girl position above the tile with the character 'f', which is going to serve as "starting tile"
                ArrayList<Sprite> girls = placeTileSprite(tmap, 'f', animationMap.get("idling"), 0.00f, 0.00f, false);
                if (girls.size() > 0) girl = girls.get(0);
                int someSpace = 250; // some space above the tile to position the girl at the beginning
                girl.setY(girl.getY() - someSpace);
                girl.setVelocity(0, 0);
                girl.show();
            }
        }

        if (displayGameOverMenu) {
            tmap.loadMap("maps", "menu.txt");
            if (debug) System.out.println(tmap);
            gameOverSprite.setPosition(screenWidth/2+40, screenHeight/2);
            playAgainButton.setPosition(screenWidth/2+40, screenHeight/2 + 100);
            quitButton.setPosition(screenWidth/2+40, screenHeight/2+150);
        }
    }

    /**
     * Set the window layout.
     */
    public void setWindowLayout() {
        setSize(tmap.getPixelWidth(), tmap.getPixelHeight() / 4);
        setVisible(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                level = 1;
                System.exit(0);
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                click(e);
                e.consume();
            }
        });
    }

    /**
     * Add the durations to the animations of the girl.
     * Add to durationMap each animation name with its correspondent array of frames durations.
     */
    public void addDurations() {
        durationMap.put("idling", new long[]{150, 250});
        durationMap.put("jumping", new long[]{250, 250});
        durationMap.put("flying", new long[]{250, 250});
        durationMap.put("falling", new long[]{150, 150});
        durationMap.put("dying", new long[]{250, 250});
        durationMap.put("shooting", new long[]{250, 250});
    }


    /**
     * Loop through the array of animation names and create an animation for each one.
     * Then add all the frames and relative durations to that animation.
     * @param startNum the number of the first frame
     * @param endNum the number of the last frame
     * @param folderName the name of the folder where the images are stored
     * @return
     */
    public Animation loopAnimationFrom(int startNum, int endNum, String folderName) {
        Animation a = null;
        for (int j = 0; j < animationNames.length; j++) {
            a = new Animation();
            animationMap.put(animationNames[j], a); // the animation will be empty for now
            for (int i = startNum; i <= endNum; i++) {
                String imagePath = folderName + FOLDER_PREFIX + animationNames[j] + FOLDER_PREFIX + animationNames[j] + i + IMAGE_SUFFIX;
                a.addFrame(loadImage(imagePath), durationMap.get(animationNames[j])[i]);
                // ex.: "images/idling/idling0.png", duration of "idling" (pos j) with duration at index i
            }
        }
        return a;
    }

    /**
     * Creates an animation for a given name and adds it to the animationMap.
     * @param startNum the number of the first frame
     * @param endNum the number of the last frame
     * @param folderName the name of the folder where the images are stored
     * @param animationName the name of the animation
     * @param duration the duration of each frame of the animation (they must have all the same duration)
     */
    public Animation createAnimation(String animationName, int startNum, int endNum, String folderName, long duration) {
        Animation a = new Animation();
        for (int i = startNum; i <= endNum; i++) {
            String imagePath = folderName + FOLDER_PREFIX + animationName + FOLDER_PREFIX + animationName + i + IMAGE_SUFFIX; // platform/breakingPlatform + 0 +.png
            //ex.: platform/idlingPlatform/idlingPlatform0.png
            a.addFrame(loadImage(imagePath), duration);
        }
        animationMap.put(animationName, a);
        return a;
    }

    ////// PLACING ELEMENTS ON THE TILE MAP //////
    /**
     * Set the position of some notebooks on the background one under the other
     * @param notebookAnim
     */
    public void setNotebooksPosition (Animation notebookAnim) {
        for(int i = -1; i<2; i++) {
            Sprite n = new Sprite(notebookAnim);
            n.setY(i * notebookAnim.getImage().getHeight(null));
            notebooks.add(n);
        }
    }

    /**
     * Place a character on the tile map
     * @param c the character to place
     * @param occurences the number of times to place the character
     * @param tm the tile map
     * @param yhigh the highest y-pixel-coordinate to place the character (The highest the yhigh value, the lower in the screen the character will be placed)
     * @param ylow the lowest y-pixel-coordinate to place the character (The lowest the ylow value, the higher in the screen the character will be placed)
     */
    public void placeCharacters (char c, int occurences, TileMap tm, int yhigh, int ylow) {
        int placed = 0;
        int range = ylow - yhigh;
        while (placed < occurences) {
            // calculate the coordinates of where the tile will be placed
            int xt = (int) (Math.random() * (float)tm.getTileWidth());
            int yt = yhigh + (int)(Math.random() * (float)range);
            if (tm.getTileChar(xt, yt) != '.') continue;

            tm.setTileChar(c, xt, yt);
            placed++;
        }
    }

    /**
     * Place a sprite at a given character on tile map
     * @param tmap the tile map
     * @param c the character to place the sprite at
     * @param a the sprite's animation
     * @param dx the x-velocity of the sprite
     * @param dy the y-velocity of the sprite
     * @param clear if true, clear the tile after placing the sprite
     * @return an array list of sprites
     */
    public ArrayList<Sprite> placeTileSprite(TileMap tmap, char c, Animation a, float dx, float dy, boolean clear) {
        ArrayList<Sprite> sprites = new ArrayList<Sprite>();
        for (int y=0; y < tmap.getMapHeight(); y++) {
            for (int x = 0; x < tmap.getMapWidth(); x++) {
                if (tmap.getTileChar(x,y) == c) {
                    Tile t = tmap.getTile(x, y);
                    Sprite s = new Sprite(a);
                    s.setPosition(t.getXC(), t.getYC());
                    s.setVelocity(dx,dy);
                    sprites.add(s);

                    if (clear) tmap.setTileChar('.', x, y);
                }
            }
        }
        return sprites;
    }

    /**
     * Modify velocity of the sprites when they reach a certain threshold
     * @param sprites the sprites to modify the position of
     * @param threshold the threshold over which we want to modify the velocity (in tiles)
     * @param velocity the velocity to set
     */
    public void modifyVelocity(ArrayList<Sprite> sprites, int threshold, float velocity) {
        for (Sprite s : sprites) {
            if (s.getY() < tmap.getTileHeight() * threshold) {
                s.setVelocityX(velocity);
            }
        }
    }

    /**
     * Make the girl fire a bullet. Change the girl animation, make a bullet appear at girl position
     * and add the bullet to the active bullets list
     */
    public void fireBullet() {
        girl.setAnimationTo("shooting", animationMap);
        bullet.setPosition(girl.getX(), girl.getY());
        bullet.setVelocityY(-0.4f);
        activeBullets.add(bullet);
        // flag to draw bullet
        firing = true;
    }

    /**
     * Update any sprites and check for collisions
     *
     * @param elapsed The elapsed time between this call and the previous call of elapsed
     */
    public void update(long elapsed) {
        if (isGamePaused || displayStartingMenu) return;

        if (displayGame) {
            girl.setVelocityY(girl.getVelocityY() + (gravity * elapsed));
            girl.setAnimationSpeed(1.0f);

            if (debug && jump) girl.setVelocityY(-0.5f);

            if (moveRight) {
                girl.setVelocityX(moveSpeed);
                girl.setAnimation(createChangedirectionAnimation("right"));
            } else if (moveLeft) {
                girl.setVelocityX(-moveSpeed);
                girl.setAnimation(createChangedirectionAnimation("left"));
            } else {
                girl.setVelocityX(0);
            }

            girl.update(elapsed);

            // check for any collisions
            checkScreenEdge(girl);
            bounceBack(obstacles);
            checkTileCollision(girl, tmap);
            checkObstacleCollision(elapsed);
            checkHeartCollision(elapsed);
            checkBrokenPlatformCollision(elapsed);
            checkBulletCollision(elapsed);
            checkPortalCollision(elapsed);
            manageBackground(elapsed);

            if (girl.getVelocityY() > 0.7f) {
                if (debug) System.out.println("you're falling!");
            }

            checkGameOver();

            if (girl.getVelocityY() > 0 && !hurt) {
                girl.setAnimationTo("falling", animationMap);
            }

            if (girl.getVelocityY() > 0 && hurt) {
                girl.setAnimationTo("dying", animationMap);
                girl.setVelocityY(-0.1f);
            }

            if (girl.getAnimation().hasLooped()) {
                hurt = false;
            }
        }
    }

    /**
     *  Create the illusion of depth with a moving background
     *  by making the notebooks move at a different speed
     *  and make background notebooks successive
     * @param elapsed
     */
    public void manageBackground(long elapsed) {
        for (Sprite s : notebooks) {
            s.setVelocityY(girl.getVelocityY() * -0.08f);
            s.update(elapsed);
        }
        // make background notebooks successive
        for (int i = 0; i < notebooks.size(); i++) {
            Sprite n = notebooks.get(i);
            float diff = n.getY() - girl.getY();
            if (diff < (-200 - n.getHeight()) && girl.getVelocityY() > 0) {
                n.setY(n.getY() + (n.getHeight() * 2));
            } else if (diff > (-200 + n.getHeight()) && girl.getVelocityY() < 0) {
                n.setY(n.getY() - (n.getHeight() * 2));
            }
        }
    }

    /**
     * Check if the girl is colliding with the portal to the next level
     * @param elapsed
     */
    public void checkPortalCollision (long elapsed) {
        door.update(elapsed);
        if (boundingCircleCollision(girl, door)) {
            if (debug) System.out.println("colliding with portal");
            door.setAnimationFrame(1);
            if (level == 1) {
                level++;
                initialiseGame();
            }
            else {
                displayGame = false;
                displayStartingMenu = false;
                displayGameOverMenu = true;
                isGamePaused = true;
                initialiseGame();
            }
            //openingDoor.show();
        }
    }

    /**
     * Check if the girl is colliding with any of the hearts
     * @param elapsed
     */
    public void checkHeartCollision (long elapsed) {
        for (int i = 0; i < hearts.size(); i++) {
            hearts.get(i).update(elapsed);
            Sprite tempHeart = hearts.get(i);
            tempHeart.update(elapsed);
            if (boundingCircleCollision(girl, tempHeart)) {
                hearts.remove(tempHeart);
                if (health < 100) health += 15;
                heartCollectionSound = new Sound("sounds/coin.wav");
                heartCollectionSound.start();
            }
        }
    }

    /**
     * Check if a bullet is colliding with any of the obstacles.
     * If so, remove the bullet and the obstacle
     * Then, check if the bullet is offscreen, if so, remove it.
     * @param elapsed
     */
    public void checkBulletCollision (long elapsed) {
        for (int i = 0; i < activeBullets.size(); i++) {
            Sprite tempBullet = activeBullets.get(i);
            tempBullet.update(elapsed);
            float diffX = tempBullet.getX() - girl.getX();
            float diffY = tempBullet.getY() - girl.getY();
            // check if bullet hits one of the obstacles
            for (int j = 0; j < obstacles.size(); j++) {
                if (boundingCircleCollision(tempBullet, obstacles.get(j))) {
                    activeBullets.remove(i);
                    obstacles.remove(j);
                    break;
                }
            }
            // check if the bullet is offscreen, if so, remove it
            if (diffY < -300 || diffY > 300 || diffX < -300 || diffX > 300) {
                for (int j = 0; j < activeBullets.size(); j++) {
                    if (activeBullets.get(j).equals(tempBullet)) {
                        activeBullets.remove(j);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Check if the girl is colliding with one of the obstacles
     * @param elapsed
     */
    public void checkObstacleCollision (long elapsed) {
        for (int i = 0; i < obstacles.size(); i++) {
            Sprite tempObstacle = obstacles.get(i);
            tempObstacle.update(elapsed);

              if (boundingCircleCollision(girl, tempObstacle)) {
                if (debug) System.out.println("collision with obstacle");
                obstacles.remove(tempObstacle);
                girl.setAnimationTo("dying", animationMap);
                obstacleCollisionSound = new Sound("sounds/obstacle1.wav");
                obstacleCollisionSound.start();
                hurt = true;
                health -= 20;
            }
        }
    }

    /**
     * Check if the girl is colliding with any of the broken platforms.
     * If so, change both sprites' animations, make the platform fall and play a sound
     * @param elapsed
     */
    public void checkBrokenPlatformCollision (long elapsed) {
        // check collision with the broken platforms
        for (int i = 0; i < brokenPlatforms.size(); i++) {
            brokenPlatforms.get(i).update(elapsed);
            if (boundingBoxCollision(girl, brokenPlatforms.get(i))) {
                if (System.currentTimeMillis() - lastSoundTime >= 500) { // Check if 100ms have passed since the last sound played
                    playFallingNoise = true;
                    fallingSound = new Sound("sounds/breaking.wav");
                    fallingSound.start();
                    lastSoundTime = System.currentTimeMillis(); // Update the last sound time
                }
                girl.setAnimationTo("dying", animationMap);
                animationMap.get("breakingPlatform").setLoop(false);
                brokenPlatforms.get(i).setAnimationTo("breakingPlatform", animationMap);
                // remove them if they are offscreen
                brokenPlatforms.get(i).setVelocityY(0.1f);
                if (brokenPlatforms.get(i).getY() < 300) {
                    brokenPlatforms.remove(i);
                }
            }
        }
    }

    /**
     * Check if the game is over
     */
    public void checkGameOver() {
        if (health <= 0 || girl.getY()+ girl.getHeight() > tmap.getPixelHeight()) {
            displayGame = false;
            displayStartingMenu = false;
            displayGameOverMenu = true;
            isGamePaused = true;
            initialiseGame();
        }
    }

    /**
     * Check if the sprite is facing left or right, and change animation accordingly
     * @param direction "left" or "right"
     */
    public Animation createChangedirectionAnimation(String direction) { //"left" or "right"
        Animation a = new Animation();
        if (direction.equals("left")) {
            animationMap.put("changingDirectionLeft", a);
            a.addFrame(lookLeft, 250);
        } else if (direction.equals("right")) {
            animationMap.put("changingDirectionRight", a);
            a.addFrame(lookRight, 250);
        }
        return a;
    }


    ///// CHECKING EDGES /////

    /**
     * Checks if the sprite has gone off the edge of the screen and wrap it around
     * to the other side.
     *
     * @param s The Sprite to check
     */
    public void checkScreenEdge(Sprite s) {
        if (s.getX() > getWidth()) {
            s.setX(0);
        }
        if (s.getX() < 0) {
            s.setX(getWidth());
        }
    }

    /**
     * Checks if the sprite has collided with the edge of the screen. If it has,
     * then the sprite is moved back and the velocity is reversed.
     *
     * @param sprites The Sprites to check
     */
    public void bounceBack(ArrayList<Sprite> sprites) {
        for (Sprite s : sprites) {
            if (s.getX() + s.getWidth() > getWidth()) {
                s.setVelocityX(-s.getVelocityX());
            }
            if (s.getX() < 0) {
                s.setVelocityX(-s.getVelocityX());
            }
        }
    }

    /**
     * Use the sample code in the lecture notes to properly detect
     * a bounding box collision between sprites s1 and s2.
     *
     * @return true if a collision may have occurred, false if it has not.
     */
    public boolean boundingBoxCollision(Sprite s1, Sprite s2) {
        return ((s1.getX() + s1.getImage().getWidth(null) > s2.getX()) &&
                (s1.getX() < (s2.getX() + s2.getImage().getWidth(null))) &&
                ((s1.getY() + s1.getImage().getHeight(null) > s2.getY()) &&
                        (s1.getY() < s2.getY() + s2.getImage().getHeight(null))));
    }

    /**
     *  Collision detection between two sprites using bounding circles.
     *  The collision is detected if the distance between the
     *  centers of the two bounding circles is less than the sum of the
     *  radii of the two bounding circles.
     * @param s1 The first sprite
     * @param s2 The second sprite
     * @return true if a collision has occurred, false if it has not.
     */
    public boolean boundingCircleCollision(Sprite s1, Sprite s2)
    {
        float dx,dy,minimum;
        dx = s1.getX() - s2.getX();
        dy = s1.getY() - s2.getY();
        minimum = s1.getImage().getWidth(null) / 2 + s2.getImage().getWidth(null) / 2;
        return (((dx * dx) + (dy * dy)) < (minimum * minimum));
    }

    /**
     * Check and handles collisions with a tile map for the
     * sprite 'girl'.
     * @param s    The Sprite to check collisions for
     * @param tmap The tile map to check
     */
    public void checkTileCollision(Sprite s, TileMap tmap) {
        collidedTiles.clear();
        float sx = s.getX();
        float sy = s.getY();
        float tileWidth = tmap.getTileWidth();
        float tileHeight = tmap.getTileHeight();

        // check top left corner
        int tlXtile = (int) (sx / tileWidth);
        int tlYtile = (int) (sy / tileHeight);
        Tile tl = tmap.getTile(tlXtile, tlYtile);
        if (tl != null && tl.getCharacter() != '.') {
            hasCollidedWithTop = true;
        }

        // check top right corner
        int trXtile = (int) ((sx + s.getWidth()) / tileWidth);
        int trYtile = (int) (sy / tileHeight);
        Tile tr = tmap.getTile(trXtile, trYtile);
        if (tr != null && tr.getCharacter() != '.') {
            hasCollidedWithTop = true;
        }

        // check bottom left
        int blXtile = (int) (sx / tileWidth);
        int blYtile = (int) ((sy + s.getHeight()) / tileHeight);
        Tile bl = tmap.getTile(blXtile, blYtile);
        if (bl != null && bl.getCharacter() != '.' && girl.getVelocityY()>=0) {
            hasCollidedWithBottom = true;
            hasCollidedWithTop = false;
            collidedTiles.add(bl);
        }

        // check bottom right
        int brXtile = (int) ((sx + s.getWidth()) / tileWidth);
        int brYtile = (int) ((sy + s.getHeight()) / tileHeight);
        Tile br = tmap.getTile(brXtile, brYtile);
        if (br != null && br.getCharacter() != '.'&& girl.getVelocityY()>=0) {
            hasCollidedWithBottom = true;
            hasCollidedWithTop = false;
            collidedTiles.add(br);
        }

        if (hasCollidedWithBottom) {
            s.setAnimationTo("jumping", animationMap);
            animationMap.get("jumping").setLoop(false);
            s.setVelocityY(bouncingSpeed); // Reverse velocity
            bounceSound = new Sound("sounds/bounce.wav");
            bounceSound.start();
            hasCollidedWithBottom = false;

            // if the tile is a pink platform, bounce higher
            if (br.getCharacter() == 'c' || br.getCharacter() == 'd' || br.getCharacter() == 'e' ) {
                float extraBouncingSpeed = -0.7f;
                s.setVelocityY(extraBouncingSpeed); // Reverse velocity
            }
        }
    }

    ///// KEY EVENTS /////

    /**
     * Override of the keyPressed event defined in GameCore to catch our
     * own events
     *
     * @param e The event that has been generated
     */
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        switch (key) {
            case KeyEvent.VK_UP:
                jump = true;
                break;
            case KeyEvent.VK_RIGHT:
                moveRight = true;
                break;
            case KeyEvent.VK_LEFT:
                moveLeft = true;
                break;
            case KeyEvent.VK_S:
                Sound s = new Sound("sounds/caw.wav");
                s.start();
                break;
            case KeyEvent.VK_ESCAPE:
                stop();
                break;
            case KeyEvent.VK_B:
                debug = !debug;
                break;
            case KeyEvent.VK_SPACE:
                isGamePaused = !isGamePaused;
                break;
            case KeyEvent.VK_F:
                Sound f = new Sound("sounds/shotgun.wav");
                f.start();
                fireBullet();
                break;
            default:
                break;
        }
    }

    public void keyReleased(KeyEvent e) {

        int key = e.getKeyCode();

        switch (key) {
            case KeyEvent.VK_ESCAPE:
                stop();
                break;
            case KeyEvent.VK_UP:
                jump = false;
                break;
            case KeyEvent.VK_RIGHT:
                moveRight = false;
                girl.setAnimationTo("idling", animationMap);
                //girl.setAnimation("idling");
                break;
            case KeyEvent.VK_LEFT:
                moveLeft = false;
                girl.setAnimationTo("idling", animationMap);
                break;

            default:
                break;
        }
    }

    ///// MOUSE EVENTS /////

    /**
     * Check if a sprite has been clicked,
     * based on the mouse position and the sprite's position.
     * @param s the sprite to check
     * @param mouseX mouse position x
     * @param mouseY mouse position y
     * @return true if the sprite has been clicked, false otherwise
     */
    public boolean isSpriteClicked(Sprite s, int mouseX, int mouseY) {
        int spriteWidth = s.getImage().getWidth(null);
        int spriteHeight = s.getImage().getHeight(null);
        // Check if the mouse position is within the bounds of the sprite
        if (mouseX >= s.getX() && mouseX < s.getX() + spriteWidth &&
                mouseY >= s.getY() && mouseY < s.getY() + spriteHeight) {
            return true;
        } else {
            return false;
        }
    }

    ////// DRAWING METHODS /////

    /**
     * Draw the current state of the game. Note the sample use of
     * debugging output that is drawn directly to the game screen.
     */
    public void draw(Graphics2D g) {
        if (displayStartingMenu && !displayGame) {
            drawBackground(g);
            drawSprite(playButton, 0, 0, g);
            drawSprite(quitButton, 0, 0, g);
        }
        if (displayGameOverMenu) {
            //drawBackground(g);
            drawSprite(playAgainButton, 0, 0, g);
            drawSprite(quitButton, 0, 0, g);
            drawSprite(gameOverSprite, 0, 0, g);
        }
        if (displayGame && displayStartingMenu==false) {
            int xo = 0;
            int yo = -(int) girl.getY() + 200;
            drawSprites(notebooks, xo, yo, g);
            drawTileMap(g, xo, yo);
            drawSprites(hearts, xo, yo, g);
            drawSprites(obstacles, xo, yo, g);
            drawSprites(brokenPlatforms, xo, yo, g);
            drawSprite(girl, xo, yo, g);
            drawSprites(activeBullets, xo, yo, g);
            drawSprite(door, xo, yo, g);
            drawHealth(g);

            if (isGamePaused) drawPauseMenu(g);
            if (debug) {
                tmap.drawBorder(g, xo, yo, Color.black);
                g.setColor(Color.red);
                g.setFont(new Font("Arial", Font.BOLD, 15));
                girl.drawBoundingCircle(g);
                for (Sprite s : obstacles) {
                    s.drawBoundingCircle(g);
                }
                g.drawString(String.format("girl: %.0f,%.0f", girl.getX(), girl.getY()),
                        getWidth() - 100, 70);
                drawCollidedTiles(g, tmap, xo, yo);
            }
        }
    }

    /**
     * Draw all the sprites in a given list
     * @param sprites
     * @param xo
     * @param yo
     * @param g
     */
    public void drawSprites(ArrayList<Sprite> sprites, int xo, int yo, Graphics2D g) {
        for (Sprite s : sprites) {
            s.setOffsets(xo, yo);
            s.draw(g);
        }
    }

    /**
     * Draw a single sprite
     * @param s
     * @param xo
     * @param yo
     * @param g
     */
    public void drawSprite(Sprite s, int xo, int yo, Graphics2D g) {
        s.setOffsets(xo, yo);
        s.draw(g);
    }

    /**
     * Draw the tile map
     * @param g
     * @param xo
     * @param yo
     */
    public void drawTileMap(Graphics2D g, int xo, int yo) {
        tmap.draw(g, xo, yo);
    }

    /**
     * Draw the tiles that the topbar and the health of the player
     * @param g
     */
    public void drawHealth(Graphics2D g) {
        Image topBarImage = loadImage("images/topBar.png");
        g.drawImage(topBarImage, 0, 20, null);
        String msg = String.format("Health: %d", health);
        g.setColor(Color.darkGray);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.drawString(msg, getWidth() - 100, 50);
    }

    /**
     * Draw the background image
     * @param g
     */
    public void drawBackground(Graphics2D g) {
        g.setColor(Color.yellow);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /**
     * Draw the pause menu
     * @param g
     */
    public void drawPauseMenu(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Game Paused", 100, 100);
        g.drawString("Press Space to continue", 100, 150);
    }

    //// DEBUGGING METHODS /////
    /**
     * A simple method to print out the contents of a map for debugging.
     * @param map
     */
    public void showMapContents(Map<String, Animation> map) {
        for (String key : map.keySet()) {
            System.out.println("Key: " + key + ", Value: " + map.get(key));
        }
    }

    /**
     * Draw the tiles that the player has collided with
     * @param g
     * @param map
     * @param xOffset
     * @param yOffset
     */
    public void drawCollidedTiles(Graphics2D g, TileMap map, int xOffset, int yOffset) {
        if (collidedTiles.size() > 0) {
            int tileWidth = map.getTileWidth();
            int tileHeight = map.getTileHeight();

            g.setColor(Color.blue);
            for (Tile t : collidedTiles) {
                g.drawRect(t.getXC() + xOffset, t.getYC() + yOffset, tileWidth, tileHeight);
                g.drawString(String.format("%c",t.getCharacter()),t.getXC() + xOffset+8, t.getYC() + yOffset+8);
            }
        }
    }

}
