package com.shooter;

import java.util.Random;

import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.entity.IEntity;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;


import android.util.Log;

/**
 * 
 * @author John Linford
 * 
 *	Used to spawn enemies, items and boss
 */
public class Updater implements IUpdateHandler
{
	private static final int SHIP1_ENEMY_SPAWN_TIME = 7;
	private static final int SHIP2_ENEMY_SPAWN_TIME = 4;
	private static final int SHIP3_ENEMY_SPAWN_TIME = 10;
	private static final int POWERUP_SPAWN_TIME = 13;
	private static final int LEVEL_COMPLETE_SCORE = 800;
	private static final int BOSS_SPAWN_SCORE = 500;

	// the y positions of lanes, used for spawning
	private static final int LANE1 = 165;
	private static final int LANE2 = 115;
	private static final int LANE3 = 58;
	private static final int LANE4 = 10;

	private static double spawnModifier = 1;
	private static double totalGameTime = 0;
	private static double previousShip1SpawnTime = 0;
	private static double previousShip2SpawnTime = 0;
	private static double previousShip3SpawnTime = 0;
	private static double previousPowerSpawnTime = 0;

	private Random rand = new Random();
	private ShooterActivity activity;
	private Scene scene;

	public Updater(ShooterActivity a, Scene s)
	{
		scene = s;
		activity = a;

		// scale spawn times based on level
		if (activity.level == 1)
			spawnModifier = 1;
		else 
			spawnModifier = .65;
	}

	@Override
	public void onUpdate(float pSecondsElapsed) 
	{
		totalGameTime += pSecondsElapsed;

		// only spawn blue enemy every 3 seconds.. etc
		if (totalGameTime - previousShip1SpawnTime > (int)(SHIP1_ENEMY_SPAWN_TIME * spawnModifier) && !activity.hasBoss && !activity.isGameOver)
		{
			previousShip1SpawnTime = totalGameTime;
			addShip1();
		}
		else if (totalGameTime - previousShip2SpawnTime > (int)(SHIP2_ENEMY_SPAWN_TIME * spawnModifier) && !activity.hasBoss && !activity.isGameOver)
		{
			previousShip2SpawnTime = totalGameTime;
			addShip2();
		}
		else if (totalGameTime - previousShip3SpawnTime > (int)(SHIP3_ENEMY_SPAWN_TIME * spawnModifier) && !activity.hasBoss && !activity.isGameOver)
		{
			previousShip3SpawnTime = totalGameTime;
			addShip3();
		}
		else if (totalGameTime - previousPowerSpawnTime > POWERUP_SPAWN_TIME && !activity.isGameOver)
		{
			previousPowerSpawnTime = totalGameTime;
			addPowerUp();
		}

		// check for game over
		if (activity.getPlayer().getHealth() <= 0 && !activity.isGameOver)
		{
			activity.runOnUiThread(new Runnable()
			{
				public void run() 
				{
					activity.gameOver();
				}
			});
		}

		// at bossspawnscore start boss mode, we only have boss show up in level 1
		if (activity.getScore() == BOSS_SPAWN_SCORE && activity.level == 1 && !activity.hasBoss)
		{
			activity.startBoss();
		}

		// at levelcompletescore start the next level
		if (activity.getScore() >= LEVEL_COMPLETE_SCORE && activity.level == 1)
		{
			activity.levelComplete();
		}

		// remove spawned enemies if they go off screen or die
		for (int i = 0; i < scene.getChildCount(); i++)
		{
			// check if off screen and remove
			if (scene.getChild(i).getX() > activity.CAMERA_WIDTH)
			{
				activity.getScene().detachChild(scene.getChild(i));
				return;
			}
			try
			{
				GameObject obj = (GameObject) scene.getChild(i);

				// if health is zero then we have ran into them
				if (obj.getHealth() == 0)
				{
					// if its the boss, reset boss and give player extra points
					if (obj.getClass() == Boss.class)
					{
						activity.resetBoss();
						activity.incScore(LEVEL_COMPLETE_SCORE - BOSS_SPAWN_SCORE);

						// sound effects
						activity.setCrashSound();

						//visual
						addExplosion((IEntity) obj);
					}
					else
					{
						// play a random crash sound
						activity.setCrashSound();

						// increment player's score for killing an enemy
						activity.incScore(10);

						// spawn an explosion at the entity's point
						addExplosion(scene.getChild(i));

						// remove entity from scene
						Log.i("Shooter", "Detaching enemy (health): " + scene.getChildCount());
						activity.getScene().detachChild((IEntity) obj);
					}
				}
				// if health is <0, then it just needs to be removed with no dmg dealth
				else if (obj.getHealth() < 0)
				{
					activity.getScene().detachChild((IEntity) obj);
				}
			}
			catch (Exception e){}
		}
	}

	// do nothing
	@Override
	public void reset() {}

	// add ship1 to scene, basic slow moving ship, only moves in x direction, high health, high dmg
	public void addShip1()
	{
		int health = 70;
		int damage = 20;
		int velocity = 2;
		int rate = 3;
		int randY;

		// spawn a ship in a random lane 
		randY = rand.nextInt(4);
		if (randY == 0)
			randY = LANE1;
		else if (randY == 1)
			randY = LANE2;
		else if (randY == 2)
			randY = LANE3;
		else if (randY == 3)
			randY = LANE4;

		Log.i("Shooter", "Adding ship1");
		Enemy enemy = new Enemy(0, randY, activity.enemyTextureRegion, activity.getVertexBufferObjectManager(), activity, velocity, health, damage, rate, 1);
		this.scene.attachChild(enemy);
	}

	// adding ship2, follows the player, low health, low dmg
	public void addShip2()
	{
		int health = 30;
		int damage = 5;
		int velocity = 2;
		int rate = 4;
		int randY;

		randY = rand.nextInt(4);
		if (randY == 0)
			randY = LANE1;
		else if (randY == 1)
			randY = LANE2;
		else if (randY == 2)
			randY = LANE3;
		else if (randY == 3)
			randY = LANE4;

		Log.i("Shooter", "Adding ship2");
		Enemy enemy = new Enemy(0, randY, activity.enemy2TextureRegion, activity.getVertexBufferObjectManager(), activity, velocity, health, damage, rate, 2);
		this.scene.attachChild(enemy);
	}

	// adding ship3, faster moving ship in x direction, low health/dmg
	public void addShip3()
	{
		int health = 20;
		int damage = 10;
		int velocity = 4;
		int rate = 2;
		int randY;

		randY = rand.nextInt(4);
		if (randY == 0)
			randY = LANE1;
		else if (randY == 1)
			randY = LANE2;
		else if (randY == 2)
			randY = LANE3;
		else if (randY == 3)
			randY = LANE4;

		Log.i("Shooter", "Adding ship3");
		Enemy enemy = new Enemy(0, randY, activity.enemy3TextureRegion, activity.getVertexBufferObjectManager(), activity, velocity, health, damage, rate, 3);
		this.scene.attachChild(enemy);
	}

	// add a random powerup
	public void addPowerUp()
	{
		PowerUp power = null;

		// remove immunity if it is active before spawning another
		if (activity.getPlayer().getImmune())
			activity.getPlayer().setImmune(false);

		int randY = rand.nextInt(4);
		if (randY == 0)
			randY = LANE1;
		else if (randY == 1)
			randY = LANE2;
		else if (randY == 2)
			randY = LANE3;
		else if (randY == 3)
			randY = LANE4;

		// get a random type of power up to add
		int randType = rand.nextInt(3);

		if (randType == 0)
			power = new PowerUp(0, randY, activity.power1TextureRegion, activity.getVertexBufferObjectManager(), activity, 2, 0, randType);
		else if (randType == 1)
			power = new PowerUp(0, randY, activity.power2TextureRegion, activity.getVertexBufferObjectManager(), activity, 2, 0, randType);
		else if (randType == 2)
			power = new PowerUp(0, randY, activity.power3TextureRegion, activity.getVertexBufferObjectManager(), activity, 2, 0, randType);

		Log.i("Shooter", "Adding PowerUp");
		this.scene.attachChild(power);
	}

	// add explosion animation
	public void addExplosion(IEntity obj)
	{
		// get obj's position
		float xPos = obj.getX();
		float yPos = obj.getY();

		// add a couple explosions
		final FixtureDef objectFixtureDef = PhysicsFactory.createFixtureDef(1f, 0f, 0f, true);
		AnimatedSprite explosion = new AnimatedSprite(xPos, yPos, activity.explosionTextureRegion, activity.getVertexBufferObjectManager());
		Body b = PhysicsFactory.createBoxBody(activity.getPhysicsWorld(), explosion, BodyType.KinematicBody, objectFixtureDef);
		explosion.setUserData(b);
		explosion.animate(30);
		activity.getPhysicsWorld().registerPhysicsConnector(new PhysicsConnector(explosion, b, true, true));
		b.setLinearVelocity(8, 0);
		activity.getScene().attachChild(explosion);

		// add another explosion a little offset to make it look more real
		AnimatedSprite explosion2 = new AnimatedSprite(xPos + 15, yPos, activity.explosionTextureRegion, activity.getVertexBufferObjectManager());
		Body b2 = PhysicsFactory.createBoxBody(activity.getPhysicsWorld(), explosion2, BodyType.KinematicBody, objectFixtureDef);
		explosion2.setUserData(b2);
		explosion2.animate(40);
		activity.getPhysicsWorld().registerPhysicsConnector(new PhysicsConnector(explosion2, b2, true, true));
		b2.setLinearVelocity(8, 0);
		activity.getScene().attachChild(explosion2);
	}
}
