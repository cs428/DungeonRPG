package edu.byu.rpg.entities.enemies.weapons.attacks;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Pool;
import edu.byu.rpg.RpgGame;
import edu.byu.rpg.entities.enemies.weapons.base.EnemyAttack;
import edu.byu.rpg.physics.Body;
import edu.byu.rpg.physics.World;

/**
 * Starter ranged attack for enemies.  A basic bullet with no special abilities.
 */
public class BasicEnemyAttack extends EnemyAttack {

    Texture bulletTexture;

    public BasicEnemyAttack(RpgGame game, World world, Pool<EnemyAttack> pool) {
        super(game, world, new Body(0, 0, 8, 8), pool);
        bulletTexture = game.assets.getTexture("basic_bullet");
    }

    @Override
    public void draw(float delta, SpriteBatch batch) {
        batch.draw(bulletTexture, body.position.x, body.position.y);
    }
}