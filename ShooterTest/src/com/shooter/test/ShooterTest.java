package com.shooter.test;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.shooter.ShooterActivity;


/**
 * 
 * @author John Linford
 * 
 * Some test cases I created for Shooter game.
 * 
 * Note these tests are rather limited because its much much easier to
 * simply use log statements to test what is happening, since most of the
 * problems occur in the game loop and/or most problems are visual bugs,
 * both of which are hard/impossible to test for. So its much quicker, easier
 * and more effective to simply use log statements while playtesting the game.
 *
 */
public class ShooterTest extends AndroidTestCase
{
	private static ShooterActivity activity;
	private static Context context;

	public ShooterTest(String name) 
	{
		super();
		setName(name);
	}

	protected void setUp() throws Exception 
	{
		super.setUp();
		
		context = getContext();

		// Set our main activity, and get the text boxes used in addWordView
		activity = new ShooterActivity();
	}

	protected void tearDown() throws Exception 
	{
		super.tearDown();
		
		//activity.onPauseGame();
		//activity.onDestroyResources();
		//activity.onDestroy();
	}


	// Test that the engine is setup
	@SmallTest
	public void testEngineOptions()
	{
		activity.onCreateEngineOptions();
		assertNotNull(activity);
		assertNotNull(activity.getEngine());
	}
	
	// Test that our textures and resources are loaded properly
	@SmallTest
	public void testOnCreateResources()
	{
		activity.onCreateResources();
		
		// after create resources, textures should be loaded
		assertNotNull(activity.enemyTextureRegion);
		assertNotNull(activity.explosionTextureRegion);
		assertNotNull(activity.enemy2TextureRegion);
		assertNotNull(activity.enemy3TextureRegion);
		assertNotNull(activity.enemyProjRegion);
		assertNotNull(activity.playerProjRegion);
	}
	
	@SmallTest
	public void testOnCreateScene()
	{
		activity.onCreateScene();
		
		// physics engine should be loaded
		assertNotNull(activity.getPhysicsWorld());
		// scene should be loaded
		assertNotNull(activity.getScene());
		// there should be 6 children in the scene (walls, player)
		assertEquals(6, activity.getScene().getChildCount());
		// player should be loaded
		assertNotNull(activity.getPlayer());
	}

}
