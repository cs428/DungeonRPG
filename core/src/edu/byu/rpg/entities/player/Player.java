package edu.byu.rpg.entities.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import edu.byu.rpg.RpgGame;
import edu.byu.rpg.audio.AudioManager;
import edu.byu.rpg.entities.base.Actor;
import edu.byu.rpg.entities.effects.Shadow;
import edu.byu.rpg.entities.player.weapons.base.PlayerWeapon;
import edu.byu.rpg.entities.player.weapons.basic.BasicWeapon;
import edu.byu.rpg.graphics.AnimationManager;
import edu.byu.rpg.input.InputManager;
import edu.byu.rpg.physics.Body;
import edu.byu.rpg.physics.Collideable;
import edu.byu.rpg.physics.World;
import java.lang.Math;

import static java.lang.Math.abs;

/**
 * The player-controlled character.
 */
public class Player extends Actor implements Collideable {

    // animations and graphics
    private AnimationManager torsoAnims;
    private AnimationManager legsAnims;
    private Shadow shadow;

    // character stats
    private float health;
    private float maxHealth;
    private float hunger;
    private final float maxHunger = 200;
    private int playerExperience;
    private int playerLevel;
    private int luck;

    // audio
    private String walkingSound = "footstep";

    // physics constants
    private final float ACCEL_CONST = 2.5f;

    // taking damage, flashing, being invincible, etc.
    private final float invincibleTime = 1.5f;
    private float invincibleClock;
    private final float flashTime = 0.05f;
    private float flashClock;
    private boolean flashing;

    // player weapon
    private PlayerWeapon weapon;

    public Player(RpgGame game, World world, int x, int y) {
        super(game, world, new Body(x, y, 8, 0, 16, 16));
        // add to player collision group
        world.add(World.Type.PLAYER, this);

        // character stats
        maxHealth = health = 3;
        hunger = 0;
        playerExperience = 0;
        playerLevel = 1;
        luck = 1;

        // damage variables
        invincibleClock = 0;
        flashClock = 0;
        flashing = false;

        // equip basic weapon
        equipWeapon(new BasicWeapon(game, world));

        // init animations
        legsAnims = new AnimationManager(game);
        torsoAnims = new AnimationManager(game);

        legsAnims.add("player/legs_stand_down", 1, 1, 0);
        legsAnims.add("player/legs_walk_down", 1, 8, 10);

        torsoAnims.add("player/body_stand_down", 1, 1, 0);
        torsoAnims.add("player/body_walk_down", 1, 8, 10);

        // create a shadow
        shadow = new Shadow(game, game.assets.getTexture("player/shadow"), body);
    }

    @Override
    public void update(float delta) {
        // get input and update velocity, then position
        // TODO: Integrate speedBoost when temporary powerups are implemented
        body.acceleration.x = ACCEL_CONST * InputManager.getLeftXAxis();
        body.acceleration.y = ACCEL_CONST * InputManager.getLeftYAxis();

        // TODO: Check for collisions with weapons, boosts, and recovery items
        // check for collisions with enemies, traps, etc.
        handleEnemyCollisions();

        // play animations
        if (body.velocity.len() > 0) {
            game.audio.playSound(walkingSound);
            legsAnims.play("player/legs_walk_down", true);
            torsoAnims.play("player/body_walk_down", true);
        } else {
            legsAnims.play("player/legs_stand_down", true);
            torsoAnims.play("player/body_stand_down", true);
        }

        // right stick = bullets
        float rightXAxis = InputManager.getRightXAxis();
        float rightYAxis = InputManager.getRightYAxis();
        if (abs(rightXAxis) > 0 || abs(rightYAxis) > 0) {
            // fireBullets bullet in the direction of the input
            fireWeapon(rightXAxis, rightYAxis);
        }

        float prevX = body.getCenterX();
        float prevY = body.getCenterY();

        super.update(delta);

        hunger += (abs(prevX - body.getCenterX()) + abs(prevY - body.getCenterY())) / 100.0;

        if (hunger >= maxHunger){
            die();
        }
    }

    /**
     * Subroutine of update, used to fire a weapon.
     * @param xInput The x-axis input (0-1).
     * @param yInput The y-axis input (0-1).
     */
    private void fireWeapon(float xInput, float yInput) {
        if (weapon == null) return;

        // get bullet direction and influence by player velocity
        float xDir = xInput;
        float yDir = yInput;

        // get center of hitbox
        float x = body.getCenterX();
        float y = body.position.y + body.size.y;

        // fire weapon
        weapon.fire(x, y, xDir, yDir);
    }

    @Override
    public void draw(float delta, SpriteBatch batch) {
        // if invincible, set flashing variable
        if (invincibleClock > 0) {
            invincibleClock -= delta;
            if (flashClock > 0) {
                flashClock -= delta;
            } else {
                flashClock = flashTime;
                flashing = !flashing;
            }
        } else {
            flashing = false;
        }

        if (!flashing) {
            // draw legs first, then torso
            legsAnims.draw(delta, body.position.x, body.position.y);
            torsoAnims.draw(delta, body.position.x, body.position.y);
        }
    }

    /**
     * Handles collisions with enemies, traps, etc.  Subroutine of {@link Player#update(float)}.
     */
    private void handleEnemyCollisions() {
        // if invincible, return
        if (invincibleClock > 0) return;

        // check for collisions with enemies, and get hurt if hit.
        Collideable enemy = world.collide(World.Type.ENEMY, body);
        if (enemy != null) {
            takeDamage(enemy.getDamage());
        }
    }

    /**
     * Sets the current weapon object, destroying any existing equipped weapons.
     * Useful when the Player walks over a powerup, or buys a new weapon from a shop.
     * @param newWeapon The new weapon to equip.
     */
    public void equipWeapon(PlayerWeapon newWeapon) {
        if (weapon != null) {
            weapon.destroy();
        }
        this.weapon = newWeapon;
        Gdx.app.debug("Player", "New weapon!");
    }

    /**
     * By default, player doesn't deal damage to things by touching them. (Possible upgrade idea for later???)
     * @return 0, because player doesn't hurt stuff by default.
     */
    @Override
    public float getDamage() {
        return 0;
    }

    /**
     * Reduce health by specified damage amount, and become invincible for a bit.
     * @param damage The amount of damage to inflict on this object.
     */
    @Override
    public void takeDamage(float damage) {
        // take damage
        health -= damage;

        // "bounce" off the enemy
        body.velocity.scl(-1f);

        // die
        if (health <= 0) {
            die();
        }
        // go invincible
        invincibleClock = invincibleTime;
        flashing = true;
    }

    private void heal(float recover) {
        health += recover;
        if (health > maxHealth) { health = maxHealth; }
        Gdx.app.debug("Player", String.format("Healed! Health is now %1$d", health));
    }

    private void eat(float recover){
        hunger -= recover;
        if (hunger < 0) { hunger = 0; };
        Gdx.app.debug("Player", String.format("Ate! Hunger is now %1$d", hunger));
    }

    // Levels are calculated using a square root, rounded down. Currently, there is no level cap.
    private int experienceNeededToLevelUp() { return (int) Math.floor(40 * Math.sqrt((float) playerLevel - 1)); }

    private void addExperience(int experience){
        playerExperience += experience;
        if (playerExperience >= experienceNeededToLevelUp()){
            levelUp();
        }
    }

    private void levelUp() {
        /*
        TODO: Trigger events (animation, stat rolls, etc.)
        TODO: Return Array or Map of changes for UI to display (or trigger here)
        */
        playerExperience -= experienceNeededToLevelUp();
        ++playerLevel;

        Gdx.app.debug("Player", String.format("Level up! Now at: %1$d", playerLevel));
        Gdx.app.debug("Player", String.format("XP needed for next level: %1$d", experienceNeededToLevelUp()));
        
        // Luck cannot increase more than 2 per level
        int luckIncrease = 0;
        for (int i = 0; i < luck; i++) {
            if (randomRollAverage() >= Math.random()) {
                ++luckIncrease;
            }
        }
        luck += luckIncrease;

        double healthIncrease = 0;
        for (int i = 0; i < luck; i++) {
            double roll = randomRollAverage() + randomRollAverage() + randomRollAverage();
            if (healthIncrease < roll) { healthIncrease = roll; }
        }
        maxHealth += healthIncrease;
        health += healthIncrease;
        Gdx.app.debug("Player", String.format("Stats are now: Luck = %1$d, Max health = %2$d", luck, maxHealth));
    }

    private double randomRollAverage(){ return (Math.random() + Math.random()) / 2; }

    /**
     * Kills the player.  Called when player's health is below or equal to 0
     */
    private void die() {
        // TODO: create some death sequence/animation, don't call destroy until after it's complete.
        destroy();
    }

    @Override
    public void destroy() {
        super.destroy();
        world.remove(this);
        shadow.destroy();
    }
}
