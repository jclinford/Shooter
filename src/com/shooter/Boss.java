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

public class Boss extends AnimatedSprite implements GameObject
{
	private int HEALTH = 500;
	private static final int DAMAGE = 10;
	private static final int VELOCITY = 2;
	private static final int SHOOT_RATE = 1;

	private static double totalGameTime = 0;
	private double previousShotTime = 0;

	private ShooterActivity activity;
	private Body body;

	public Boss(final float pX, final float pY, final TiledTextureRegion pTextureRegion, final VertexBufferObjectManager pVertexBufferObjectManager,
			final ShooterActivity a)
	{
		super(pX, pY, pTextureRegion, pVertexBufferObjectManager);
		activity = a;

		final FixtureDef objectFixtureDef = PhysicsFactory.createFixtureDef(1, 0f, 0f, true);
		body = PhysicsFactory.createBoxBody(activity.getPhysicsWorld(), this, BodyType.KinematicBody, objectFixtureDef);

		// rotate body to face north
		body.setTransform(body.getWorldCenter(), (float)(3.14 / 2));                                        
		this.setRotation((float)(3.14 / 2));

		activity.getPhysicsWorld().registerPhysicsConnector(new PhysicsConnector(this, body, true, true));

		setUserData(body);	
		
		body.setLinearVelocity(0, VELOCITY);
	}

	@Override
	public void onManagedUpdate(final float pSecondsElapsed) 
	{
		// check for collisions with player
		if (this.collidesWith(activity.getPlayer()) && !activity.getPlayer().getImmune())
		{
			Log.i("Shooter", "Player-Enemy collision");
			activity.getPlayer().dealDamage(DAMAGE);
		}

		// the boss movement is up and down the horizontal
		if (this.getY() + this.getHeight() / 2 <= 0)
			body.setLinearVelocity(0, VELOCITY);
		else if (this.getY() + this.getHeight() / 2 > activity.CAMERA_HEIGHT)
			body.setLinearVelocity(0, -VELOCITY);

		// shoot at player at rate SHOOTRATE
		totalGameTime += pSecondsElapsed;

		// fire a shot if we are in time bounds
		if (totalGameTime - previousShotTime > SHOOT_RATE)
		{
			previousShotTime = totalGameTime;
			shoot();
		}
	}

	// boss fires 2 shots at a time
	public void shoot()
	{
		int offset = 25;
		float x = this.getX() + this.getHeight();
		float y = this.getY() + (this.getWidth() / 2 + offset);
		float y2 = this.getY() + (this.getWidth() / 2 - offset);

		Projectile proj = new Projectile(x, y, activity.enemyProjRegion, activity.getVertexBufferObjectManager(), activity, 6, 0, false);
		Projectile proj2 = new Projectile(x, y2, activity.enemyProjRegion, activity.getVertexBufferObjectManager(), activity, 6, 0, false);
		activity.getScene().attachChild(proj);
		activity.getScene().attachChild(proj2);
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
