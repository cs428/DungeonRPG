package edu.byu.rpg.entities.player.weapons.base;

import com.badlogic.gdx.utils.Pool;
import edu.byu.rpg.RpgGame;
import edu.byu.rpg.entities.base.Actor;
import edu.byu.rpg.physics.Body;
import edu.byu.rpg.physics.Collideable;
import edu.byu.rpg.physics.World;
import edu.byu.rpg.tools.Utils;

/**
 * Base class for all player bullets.  Implements {@link com.badlogic.gdx.utils.Pool.Poolable},
 * so that the player weapon classes can efficiently activate new bullets when firing.
 *
 * @see <a href="https://github.com/libgdx/libgdx/wiki/Memory-management">LibGDX Memory Management</a>
 */
public abstract class PlayerBullet extends Actor implements Pool.Poolable, Collideable {

    /**
     * Local instance of bullet pool, used to deactivate this bullet on collision.
     */
    private Pool<PlayerBullet> pool;

    /** The amount of damage this bullet inflicts. */
    private float damage;

    /**
     * Calls {@link Actor}'s constructor, initializes the body.
     * @param game Our game class.
     * @param world The physics world.
     * @param body A physics {@link Body} that defines this bullet's hitbox.
     * @param pool The bullet pool that this bullet belongs to.
     */
    public PlayerBullet(RpgGame game, World world, Body body, Pool<PlayerBullet> pool) {
        super(game, world, body);
        world.add(World.Type.PLAYER_BULLET, this);
        this.pool = pool;
        body.alive = false;
        body.maxSpeed = 9.5f;
        body.friction = 0;
        damage = 1;
    }

    /**
     * Initializes the position and velocity of the bullet, sets the alive flag on its hitbox,
     * and re-registers it with {@link RpgGame#engine}.
     * Call this method after getting a bullet from the pool.
     * @param x The new x-position of the bullet.
     * @param y The new y-position of the bullet.
     * @param xDir The x-component of the direction of the bullet (0 to 1)
     * @param yDir The y-component of the direction of the bullet (0 to 1);
     * @param damage The amount of damage this bullet should do.
     */
    public void init(float x, float y, float xDir, float yDir, float damage) {
        body.alive = true;
        body.position.set(x, y);
        body.velocity.set(xDir * body.maxSpeed, yDir * body.maxSpeed);
        game.engine.addEntity(this);
        this.damage = damage;
    }

    /**
     * This function is called whenever the bullet collides with something that should destroy it
     * (enemies, walls, etc.).  It can be overridden to deal with any post-collision logic such as
     * creating a dust ball animation, making a harmful explosion, spawning more bullets... Anything you want to
     * do once a bullet hits an enemy or a wall, etc.
     */
    public void pop() {
        pool.free(this);
    }

    /**
     * Callback method for when the object is "freed", or removed from the game to go back to the {@link Pool}.
     * This is called automatically by {@link Pool#free(Object)} within {@link PlayerBullet#pop()}.  It simply
     * sets velocity to 0 and then removes the bullet from {@link RpgGame#engine}.
     */
    @Override
    public void reset() {
        body.velocity.set(0, 0);
        body.alive = false;
        game.engine.removeEntity(this);
    }

    /**
     * Moves the bullet, but also checks for collisions with enemies or solids.  If collisions happen, then the
     * bullet moves back outside of the collision overlap space, and its {@link PlayerBullet#pop()} method is called.
     * @param delta The time since the last frame.
     */
    @Override
    public void update(float delta) {
        body.updateVelocity(delta);

        float prevX = body.position.x;
        float prevY = body.position.y;
        body.updatePosition(delta);

        // check for collisions
        if (collideCheck()) {

            // damage enemies if hit
            Collideable enemy = world.collide(World.Type.ENEMY, body);
            if (enemy != null) {
                enemy.takeDamage(damage);
            }

            // step backwards as long as bullet is still colliding
            while(collideCheck()) {
                body.position.x = Utils.approach(body.position.x, prevX, 1);
                body.position.y = Utils.approach(body.position.y, prevY, 1);
            }

            // destroy this bullet
            pop();
        }
    }

    /**
     * Helper method, wraps collision logic into a single method for use by {@link PlayerBullet#update(float)}
     * @return <tt>true</tt> if this bullet is overlapping enemies or solids, <tt>false</tt> if no.
     */
    private boolean collideCheck() {
        return (world.collideCheck(World.Type.ENEMY, body)
                || world.collideCheck(World.Type.SOLID, body));
    }

    /**
     * Bullets handle their own collision logic in {@link PlayerBullet#update(float)}, so this function doesn't
     * do anything.
     * @param damage (nothing)
     */
    @Override
    public void takeDamage(float damage) {}
}
