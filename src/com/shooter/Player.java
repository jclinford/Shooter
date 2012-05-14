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
 *	Player class that the user controls
 *	to move or shoot
 *
 */
public class Player extends AnimatedSprite implements GameObject
{
	public static int HEALTH = 100;
	private static final int DAMAGE = 10;

	private ShooterActivity activity;
	private final Body body;

	private boolean isImmune;


	public Player(final float pX, final float pY, final TiledTextureRegion pTextureRegion, final VertexBufferObjectManager pVertexBufferObjectManager,
			final ShooterActivity a) 
	{
		super(pX, pY, pTextureRegion, pVertexBufferObjectManager);
		activity = a;
		isImmune = false;
		//physicsHandler = new PhysicsHandler(this);
		//registerUpdateHandler(physicsHandler);

		final FixtureDef objectFixtureDef = PhysicsFactory.createFixtureDef(1, 0f, 0f);
		body = PhysicsFactory.createBoxBody(activity.getPhysicsWorld(), this, BodyType.DynamicBody, objectFixtureDef);

		// rotate the body so its facing north
		body.setTransform(body.getWorldCenter(), (float)(3.14 / 2 * 3));                                        
		this.setRotation((float)(3.14 / 2 * 3));

		activity.getPhysicsWorld().registerPhysicsConnector(new PhysicsConnector(this, body, true, true));

		setUserData(body);

		// attach our player to our scene
		activity.getScene().registerTouchArea(this);
		activity.getScene().attachChild(this);

	}

	@Override
	public void onManagedUpdate(final float pSecondsElapsed) 
	{
		super.onManagedUpdate(pSecondsElapsed);
	}


	//---------------------//
	// Getters and Setters //
	//---------------------//
	public void dealDamage(int damage)
	{
		// only deal damage if immunity is not active
		if (!isImmune)
		{
			HEALTH -= damage;
			activity.setHealth(HEALTH);
		}
	}
	public int getDamage()
	{
		return DAMAGE;
	}
	public int getHealth() 
	{
		return HEALTH;
	}

	// if we pick up the invisible buff, visibly change the player
	public void setImmune(boolean b)
	{
		isImmune = b;

		if (isImmune)
			this.setColor(.5f, .5f, .5f, .3f);
		else
			this.setColor(1f, 1f, 1f, 1f);
	}

	// if we are immune, returns true
	public boolean getImmune()
	{
		return isImmune;
	}

	// return the physics body of our player
	public Body getBody() 
	{
		return body;
	}

}
