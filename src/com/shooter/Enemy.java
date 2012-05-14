package com.shooter;

import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import android.util.Log;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

/**
 * 
 * @author John Linford
 * 
 *	Enemy class that will 'attack' the player
 *	and player can destroy them by hitting them (thus taking dmg)
 *	or by shooting them..
 *
 */
public class Enemy extends AnimatedSprite implements GameObject
{
	private int HEALTH;
	private int DAMAGE;
	private int TYPE;
	private int VELOCITYX;
	private double SHOOT_RATE;

	private static double totalGameTime = 0;
	private double previousShotTime = 0;

	private ShooterActivity activity;
	private Body body;

	public Enemy(final float pX, final float pY, final TiledTextureRegion pTextureRegion, final VertexBufferObjectManager pVertexBufferObjectManager,
			final ShooterActivity a, final int vx, final int h, final int d, final double r, final int t)
	{
		super(pX, pY, pTextureRegion, pVertexBufferObjectManager);
		activity = a;

		HEALTH = h;
		DAMAGE = d;
		TYPE = t;
		VELOCITYX = vx;
		SHOOT_RATE = r;

		final FixtureDef objectFixtureDef = PhysicsFactory.createFixtureDef(1, 0f, 0f, true);
		body = PhysicsFactory.createBoxBody(activity.getPhysicsWorld(), this, BodyType.KinematicBody, objectFixtureDef);

		// rotate body to face north
		body.setTransform(body.getWorldCenter(), (float)(3.14 / 2));                                        
		this.setRotation((float)(3.14 / 2));

		activity.getPhysicsWorld().registerPhysicsConnector(new PhysicsConnector(this, body, true, true));

		setUserData(body);

		body.setLinearVelocity(vx, 0);
	}

	// update function, called every game loop
	@Override
	public void onManagedUpdate(final float pSecondsElapsed) 
	{
		// if we go out of bounds set health to -1 so it is removed next game loop without explosion
		if (this.getX() > activity.CAMERA_WIDTH)
		{
			this.dealDamage(HEALTH + 1);
			return;
		}

		// check for collisions with player, set health to zero so it is removed with explosion
		if (this.collidesWith(activity.getPlayer()) && !activity.getPlayer().getImmune())
		{
			Log.i("Shooter", "Player-Enemy collision");
			activity.getPlayer().dealDamage(DAMAGE);
			this.dealDamage(HEALTH);
			return;
		}

		// if we are ship2, then follow the player in x-dir @ velocity 1
		if (TYPE == 2)
		{
			// if we are to the left, move right
			if (this.getY() < activity.getPlayer().getY())
				body.setLinearVelocity(VELOCITYX, 1);
			// if we are right, move left..
			else if (this.getY() > activity.getPlayer().getY())
				body.setLinearVelocity(VELOCITYX, -1);
			// else do nothing
			else if (this.getY() == activity.getPlayer().getY())
				body.setLinearVelocity(VELOCITYX, 0);
		}

		// update totalgame time
		totalGameTime += pSecondsElapsed;

		// fire a shot if we are in time bounds
		if (totalGameTime - previousShotTime > SHOOT_RATE)
		{
			previousShotTime = totalGameTime;
			shoot();
		}
	}

	
	// fire a shot
	public void shoot()
	{
		//Log.i("Shooter", "Enemy shot fired!");
		float x = this.getX();
		float y = this.getY();

		Projectile proj = new Projectile(x, y, activity.enemyProjRegion, activity.getVertexBufferObjectManager(), activity, 7, 0, false);
		activity.getScene().attachChild(proj);
	}


	//---------------------//
	// Getters and Setters //
	//---------------------//
	public void dealDamage(int damage)
	{
		HEALTH -= damage;
	}
	public int getDamage()
	{
		return DAMAGE;
	}
	public int getHealth() 
	{
		return HEALTH;
	}

	public Body getBody() 
	{
		return body;
	}
}
