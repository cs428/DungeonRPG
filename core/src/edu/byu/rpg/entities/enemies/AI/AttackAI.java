package edu.byu.rpg.entities.enemies.AI;

import edu.byu.rpg.physics.Body;

/**
 * The basis for an object that represents specifically the AI of how an enemy attacks.
 * An object that implements {@link AttackAI) will be able to calculate attacks and launch them from the {@link Body}
 * of an object
 */
public interface AttackAI extends EnemyAI {

    /**
     * causes an attack to origionate from the Body specified
     * @param enemyBody The {@link Body} that corresponds to where the enemy is at the moment move is called.
     * @param playerBody The {@Link Body} that corresponds to where the player is at the moment move is called.
     */
    public void attack(Body enemyBody, Body playerBody);
}
