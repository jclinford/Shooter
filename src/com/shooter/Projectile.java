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
 * Projectile (laser beams) class that damages enemies
 * or the player on contact
 *
 */
public class Projectile extends AnimatedSprite implements GameObject
{
	private int HEALTH = 10;
	private static final int DAMAGE = 10;

	private ShooterActivity activity;
	private Body body;
	private boolean isPlayerProjectile;

	public Projectile(final float pX, final float pY, final TiledTextureRegion pTextureRegion, final VertexBufferObjectManager pVertexBufferObjectManager,
			final ShooterActivity a, final int vx, final int vy, final boolean playerProjectile)
	{
		super(pX, pY, pTextureRegion, pVertexBufferObjectManager);
		activity = a;
		isPlayerProjectile = playerProjectile;

		final FixtureDef objectFixtureDef = PhysicsFactory.createFixtureDef(1, 0f, 0f, true);
		body = PhysicsFactory.createBoxBody(activity.getPhysicsWorld(), this, BodyType.KinematicBody, objectFixtureDef);

		// rotate body to face up and down
		body.setTransform(body.getWorldCenter(), (float)(3.14 / 2));                                        
		this.setRotation((float)(3.14 / 2));

		activity.getPhysicsWorld().registerPhysicsConnector(new PhysicsConnector(this, body, true, true));

		setUserData(body);

		body.setLinearVelocity(vx, vy);
	}

	@Override
	public void onManagedUpdate(final float pSecondsElapsed) 
	{
		// if we go out of bounds set health to -1 so it is removed next game loop without explosion
		if (this.getX() > activity.CAMERA_WIDTH || this.getX() < 0)
		{
			this.dealDamage(HEALTH + 1);
			return;
		}

		// if its the player's projectile, check for collisions with enemies
		if (isPlayerProjectile)
		{
			// if we have boss only need to check collisions w/ boss
			if (activity.hasBoss)
			{
				Boss boss = activity.boss;
				if (boss.collidesWith(this))
				{
					Log.i("Shooter", "Proj-Boss collision");
					boss.dealDamage(DAMAGE);
					this.dealDamage(HEALTH + 1);
				}
			}
			else
			{
				// otherwise, check if it collides with all active enemies
				for (int i = 0; i < activity.getScene().getChildCount(); i++)
				{
					try
					{
						Enemy obj = (Enemy) activity.getScene().getChild(i);

						// if we collide, deal it's damage
						if (obj.collidesWith(this))
						{
							Log.i("Shooter", "Proj-Enemy collision");
							obj.dealDamage(DAMAGE);

							// set laser beam's health to -1 so it is removed on next game loop without explsn
							this.dealDamage(HEALTH + 1);
						}
					}
					catch (Exception e){}
				}
			}
		}

		// Otherwise it is the enemy's projectile, so check for collisions with player
		else if (this.collidesWith(activity.getPlayer()) && !activity.getPlayer().getImmune())
		{
			Log.i("Shooter", "Player-Proj collision");

			// deal half dmg to player
			activity.getPlayer().dealDamage(DAMAGE / 2);

			// set health to -1 so it is removed on next game loop
			HEALTH = -1;
		}
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
