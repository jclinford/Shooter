package com.shooter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Random;

import org.andengine.audio.music.Music;
import org.andengine.audio.music.MusicFactory;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.AutoParallaxBackground;
import org.andengine.entity.scene.background.ParallaxBackground.ParallaxEntity;
import org.andengine.entity.scene.menu.MenuScene;
import org.andengine.entity.scene.menu.item.SpriteMenuItem;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.HorizontalAlign;
import org.andengine.util.debug.Debug;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.SensorManager;
import android.opengl.GLES20;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Toast;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;

/**
 *
 *	@author John Linford
 *  Shooter game for android
 *	Main driver class
 *
 */

public class ShooterActivity extends SimpleBaseGameActivity
{
	public static final int CAMERA_WIDTH = 360;
	public static final int CAMERA_HEIGHT = 240;
	protected static final int MENU_RESET = 0;
	protected static final int MENU_QUIT = MENU_RESET + 1;
	protected static final int MENU_OK = MENU_QUIT + 1;
	protected static final int MENU_NEXT_LEVEL = MENU_OK + 1;

	private MenuScene menuScene;					// shown when 'menu' button is pressed on phone
	private MenuScene nextMenu;						// shown when level complete
	private BitmapTextureAtlas menuTexture;
	private ITextureRegion menuResetTextureRegion;
	private ITextureRegion menuQuitTextureRegion;
	private	ITextureRegion menuOkayTextureRegion;

	// texture atlas's for texture and background, and parralax for background
	private BitmapTextureAtlas bitmapTextureAtlas;
	private BitmapTextureAtlas mAutoParallaxBackgroundTexture;
	private ITextureRegion mParallaxLayer1;			// background for level 1
	private ITextureRegion mParallaxLayer2;			// background for level 2
	private ITextureRegion mParallaxStars;			// stars that parallax on both levels

	// textures
	private TiledTextureRegion playerTextureRegion;	// player.png (player controlled char)
	public TiledTextureRegion bossTextureRegion;	// boss texture
	public TiledTextureRegion enemyTextureRegion;	// ship1.png
	public TiledTextureRegion enemy2TextureRegion;	// ship2.png
	public TiledTextureRegion enemy3TextureRegion;	// ship3.png
	public TiledTextureRegion power1TextureRegion;	// kill all enemies
	public TiledTextureRegion power2TextureRegion;	// heal
	public TiledTextureRegion power3TextureRegion;	// invisible
	public TiledTextureRegion playerProjRegion;		// player laser beam
	public TiledTextureRegion enemyProjRegion;		// enemy laser beam
	public TiledTextureRegion explosionTextureRegion; // explosion, 12 134x134 images (1602 x 134)

	private Player player;
	public Boss boss;
	public boolean hasBoss = false;					// if boss is out or not

	// world, level 1, and level 2
	private PhysicsWorld physicsWorld;
	private Scene scene;
	private Scene scene2;
	public int level;								// our current level
	public boolean isGameOver = false;

	// music and sound effects
	private Music level1Music;
	private Music level2Music;
	private Music crashSound1;
	private Music crashSound2;
	private Music crashSound3;

	// for HUD
	private Font smallFont;
	private Font largeFont;
	private Text healthText;
	private Text scoreText;
	private int score = 0;

	// for reporting high score
	private String serverIP;
	private static final String PORT = "7890";
	private Socket socket;
	private BufferedWriter writer;
	private BufferedReader reader;

	private float xVelocity;
	private float yVelocity;

	private Camera camera;
	private Controls controls;
	public Updater spawner;


	public ShooterActivity()
	{
		// allow us to network on android 4.0 phones
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy); 
	}
	//---------------------------------------//
	// SuperClass Overridden Methods	     //
	//--------------------------------------//
	@Override
	public EngineOptions onCreateEngineOptions() 
	{
		camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

		final EngineOptions engineOptions = new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera);
		engineOptions.getAudioOptions().setNeedsMusic(true);

		return engineOptions;
	}

	// loading assets
	@Override
	public void onCreateResources() 
	{
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

		// set up player/enemy textures
		bitmapTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 100, 72 * 18, TextureOptions.BILINEAR);
		playerTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(bitmapTextureAtlas, this, "player.png", 0, 0, 1, 1); // 36x47
		enemyTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(bitmapTextureAtlas, this, "ship1.png", 0, 47, 1, 1); // 36x60
		enemy2TextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(bitmapTextureAtlas, this, "ship2.png", 0, 107, 1, 1); // 36x45
		enemy3TextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(bitmapTextureAtlas, this, "ship3.png", 0, 152, 1, 1); // 36x50
		power1TextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(bitmapTextureAtlas, this, "kill.png", 0, 202, 1, 1); // 36x36
		power2TextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(bitmapTextureAtlas, this, "heal.png", 0, 238, 1, 1);	// 36 x 36
		power3TextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(bitmapTextureAtlas, this, "shield.png", 0, 274, 1, 1);	// 36x36
		playerProjRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(bitmapTextureAtlas, this, "laser.png", 0, 310, 1, 1); // 16 x 46
		enemyProjRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(bitmapTextureAtlas, this, "enemy_laser.png", 0, 356, 1, 1); // 16 x 46
		bossTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(bitmapTextureAtlas, this, "boss.png", 0, 402, 1, 1);	// 80 x 151
		explosionTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(bitmapTextureAtlas, this, "explosion.png", 0, 553, 1, 12);	// 36 x something
		bitmapTextureAtlas.load();

		// background textures
		mAutoParallaxBackgroundTexture = new BitmapTextureAtlas(this.getTextureManager(), 823, 235 * 3);
		mParallaxLayer1 = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mAutoParallaxBackgroundTexture, this, "mainBackground1.png", 0, 0);
		mParallaxLayer2 = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mAutoParallaxBackgroundTexture, this, "mainBackground2.png", 0, 235);
		mParallaxStars = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mAutoParallaxBackgroundTexture, this, "parallaxstars.png", 0, 470);
		mAutoParallaxBackgroundTexture.load();

		// menu textures
		menuTexture = new BitmapTextureAtlas(this.getTextureManager(), 256, 256, TextureOptions.BILINEAR);
		menuResetTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(menuTexture, this, "menu_reset.png", 0, 0);
		menuQuitTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(menuTexture, this, "menu_quit.png", 0, 50);
		menuOkayTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(menuTexture, this, "menu_ok.png", 0, 100);
		menuTexture.load();

		// load music
		MusicFactory.setAssetBasePath("sfx/");
		try 
		{
			// music
			level1Music = MusicFactory.createMusicFromAsset(this.mEngine.getMusicManager(), this, "level1.mp3");
			level2Music = MusicFactory.createMusicFromAsset(this.mEngine.getMusicManager(), this, "level2.mp3");
			level1Music.setLooping(true);
			level2Music.setLooping(true);
			level1Music.setVolume(.25f);
			level2Music.setVolume(.25f);

			// crash sound effect
			crashSound1 = MusicFactory.createMusicFromAsset(this.mEngine.getMusicManager(), this, "explosion1.wav");
			crashSound1.setVolume(.15f);
			crashSound2 = MusicFactory.createMusicFromAsset(this.mEngine.getMusicManager(), this, "explosion2.wav");
			crashSound2.setVolume(.15f);
			crashSound3 = MusicFactory.createMusicFromAsset(this.mEngine.getMusicManager(), this, "explosion3.wav");
			crashSound3.setVolume(.15f);
		} 
		catch (final IOException e) 
		{
			Debug.e(e);
		}

		// load our font we will use throughout the program
		FontFactory.setAssetBasePath("font/");
		smallFont = FontFactory.createFromAsset(this.getFontManager(), this.getTextureManager(), 512, 512, TextureOptions.BILINEAR, this.getAssets(), "font.ttf", 20, true, Color.CYAN);
		largeFont = FontFactory.createFromAsset(this.getFontManager(), this.getTextureManager(), 512, 512, TextureOptions.BILINEAR, this.getAssets(), "font.ttf", 40, true, Color.CYAN);
		smallFont.load();
		largeFont.load();
	}

	@Override
	public Scene onCreateScene() 
	{		
		mEngine.registerUpdateHandler(new FPSLogger());

		hasBoss = false;
		level = 1;
		physicsWorld = new PhysicsWorld(new Vector2(0, SensorManager.GRAVITY_MOON), false);

		// create menus
		createMenuScene();
		//		createGameOverMenu();
		createNextMenu();

		// game scene
		scene = new Scene();		
		controls = new Controls(this);
		spawner = new Updater(this, scene);

		// set touch listeners to scene and menu
		scene.setOnSceneTouchListener(controls);
		scene.setOnAreaTouchListener(controls);
		menuScene.setOnMenuItemClickListener(controls);
		nextMenu.setOnMenuItemClickListener(controls);

		// create boundaries / walls
		final VertexBufferObjectManager vertexBufferObjectManager = getVertexBufferObjectManager();
		final Rectangle ground = new Rectangle(0, CAMERA_HEIGHT - 2, CAMERA_WIDTH, 2, vertexBufferObjectManager);
		final Rectangle roof = new Rectangle(0, 0, CAMERA_WIDTH, 2, vertexBufferObjectManager);
		final Rectangle left = new Rectangle(0, 0, 2, CAMERA_HEIGHT, vertexBufferObjectManager);
		final Rectangle right = new Rectangle(CAMERA_WIDTH - 2, 0, 2, CAMERA_HEIGHT, vertexBufferObjectManager);
		ground.setColor(0, 0, 0, 0);
		roof.setColor(0, 0, 0, 0);
		right.setColor(0, 0, 0, 0);
		left.setColor(0, 0, 0, 0);

		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0f, 0f, 0f);
		PhysicsFactory.createBoxBody(physicsWorld, ground, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(physicsWorld, roof, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(physicsWorld, left, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(physicsWorld, right, BodyType.StaticBody, wallFixtureDef);

		// add walls to scene
		scene.attachChild(ground);
		scene.attachChild(roof);
		scene.attachChild(left);
		scene.attachChild(right);

		// create background
		final AutoParallaxBackground autoParallaxBackground = new AutoParallaxBackground(0, 0, 0, 5);
		Sprite backSprite = new Sprite(0, CAMERA_HEIGHT - this.mParallaxLayer1.getHeight(), this.mParallaxLayer1, vertexBufferObjectManager);
		autoParallaxBackground.attachParallaxEntity(new ParallaxEntity(10.0f, backSprite));
		autoParallaxBackground.attachParallaxEntity(new ParallaxEntity(15.0f, new Sprite(0, CAMERA_HEIGHT - mParallaxStars.getHeight(), mParallaxStars, vertexBufferObjectManager)));
		scene.setBackground(autoParallaxBackground);

		// register physicsWorld and spawner as update handlers to be called every update
		scene.registerUpdateHandler(this.physicsWorld);
		scene.registerUpdateHandler(spawner);

		// add player
		player = new Player(CAMERA_WIDTH/2, CAMERA_HEIGHT/2, playerTextureRegion, getVertexBufferObjectManager(), this);

		// start music
		level1Music.play();

		// place HUD on screen
		healthText = new Text(0, 0, this.smallFont, "Health: " + player.getHealth(), new TextOptions(HorizontalAlign.CENTER), vertexBufferObjectManager);
		scoreText = new Text(0, 20, this.smallFont, "Score: 0000", new TextOptions(HorizontalAlign.CENTER), vertexBufferObjectManager);
		healthText.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		healthText.setAlpha(0.5f);
		scoreText.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		scoreText.setAlpha(0.5f);
		scene.attachChild(healthText);
		scene.attachChild(scoreText);

		return this.scene;
	}

	// show the menu scene (to quit/reset) if the android menu button is pressed
	@Override
	public boolean onKeyDown(final int pKeyCode, final KeyEvent pEvent) 
	{
		if(pKeyCode == KeyEvent.KEYCODE_MENU && pEvent.getAction() == KeyEvent.ACTION_DOWN) 
		{
			if(scene.hasChildScene()) 
			{
				// Remove the menu and reset it.
				menuScene.back();
			} 
			else 
			{
				// Attach the menu.
				if (level == 1)
					scene.setChildScene(menuScene, false, true, true);
				else if (level == 3)
					scene2.setChildScene(menuScene, false, true, true);
			}
			return true;
		} 
		else
		{
			return super.onKeyDown(pKeyCode, pEvent);
		}
	}

	// resume game and music
	@Override
	public void onResumeGame() 
	{
		super.onResumeGame();
		enableAccelerationSensor(controls);

		if (level == 1)
			level1Music.play();
		else
			level2Music.play();
	}

	// when game is paused (ie minimized, on another screen etc), pause game and music
	@Override
	public void onPauseGame() 
	{
		super.onPauseGame();
		disableAccelerationSensor();

		// stop music
		level1Music.pause();
		level2Music.pause();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		
		try
		{
			level1Music.release();
			level2Music.release();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	// called when we loose, gives option to connect to server for high score
	@Override
	public Dialog onCreateDialog(final int pID)
	{
		switch(pID)
		{
		// first came over case, shows reset game or connect to server options
		case 0:
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle("Game Over!")
			.setCancelable(false)
			.setPositiveButton("Restart", new OnClickListener() 
			{
				// restart the game from level 1
				@Override
				public void onClick(final DialogInterface pDialog, final int pWhich) 
				{
					ShooterActivity.this.getScene().reset();
					ShooterActivity.this.getScene().clearChildScene();
					ShooterActivity.this.reset();
					ShooterActivity.this.removeDialog(0);
				}
			})
			.setNegativeButton("View High Score", new OnClickListener() 
			{
				// bring up a new dialog to ask for server ip
				@Override
				public void onClick(final DialogInterface pDialog, final int pWhich) 
				{
					ShooterActivity.this.showDialog(1);
				}
			})
			.create();

			// create a dialog with a server ip entry box that we will connect to 
		case 1:
			final EditText ipEditText = new EditText(this);
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle("Enter Score-Server IP..")
			.setCancelable(false)
			.setView(ipEditText)
			.setPositiveButton("Submit", new OnClickListener() 
			{
				@Override
				public void onClick(final DialogInterface pDialog, final int pWhich) 
				{
					ShooterActivity.this.serverIP = ipEditText.getText().toString();
					ShooterActivity.this.submitScores();
					ShooterActivity.this.removeDialog(1);
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.create();

		default:
			return null;//super.onCreateDialog(pID);
		}
	}


	//----------------//
	// Class Methods
	//---------------//
	protected void createMenuScene() 
	{
		menuScene = new MenuScene(camera);

		final SpriteMenuItem resetMenuItem = new SpriteMenuItem(MENU_RESET, menuResetTextureRegion, getVertexBufferObjectManager());
		resetMenuItem.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		menuScene.addMenuItem(resetMenuItem);

		final SpriteMenuItem quitMenuItem = new SpriteMenuItem(MENU_QUIT, menuQuitTextureRegion, getVertexBufferObjectManager());
		quitMenuItem.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		menuScene.addMenuItem(quitMenuItem);

		menuScene.buildAnimations();

		menuScene.setBackgroundEnabled(false);
	}

	// menu for next level
	protected void createNextMenu()
	{
		nextMenu = new MenuScene(camera);

		final Text nextText = new Text(10, 10, largeFont, "Level Complete!", new TextOptions(HorizontalAlign.CENTER), getVertexBufferObjectManager());
		nextMenu.attachChild(nextText);

		final SpriteMenuItem nextMenuItem = new SpriteMenuItem(MENU_NEXT_LEVEL, menuOkayTextureRegion, getVertexBufferObjectManager());
		nextMenuItem.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		nextMenu.addMenuItem(nextMenuItem);

		nextMenu.buildAnimations();
		nextMenu.setBackgroundEnabled(false);
	}

	// display game over menu
	protected void gameOver()
	{
		isGameOver = true;

		this.showDialog(0);
	}

	// submits scores to the server @ serverIP
	protected void submitScores()
	{
		// try to setup the connection
		try
		{
			socket = new Socket(serverIP, Integer.parseInt(PORT));
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

			// if its our server, it will reply with "Score Server", so we read this line
			String connection = reader.readLine().trim();
			Log.i("Shooter", "Connection established: " + connection);
			
			// if it is our server, send our score
			if (connection.contains("Score Server"))
			{
				// score = score * level
				writer.write("Score:" + this.score * this.level + "\n");
				writer.flush();

				// should read the rank if server recieved it
				String rank = reader.readLine();
				Log.i("Shooter", rank);

				// show a toast with the users rank
				String text = "Congrats! Your rank is: " + rank;
				Toast toast = Toast.makeText(this.getApplicationContext(), text, Toast.LENGTH_LONG);
				toast.show();
				
				this.showDialog(0);
			}

			Log.i("Shooter", "ScoreSubmit success");
		}
		catch(Exception e)
		{
			// if we fail, show a toast to user
			String text = "Connection to server failed!!";
			Toast toast = Toast.makeText(this.getApplicationContext(), text, Toast.LENGTH_SHORT);
			toast.show();

			Log.i("Shooter", "Failed to connect");
			e.printStackTrace();

			this.showDialog(0);
		}
		finally
		{
			try
			{
				socket.close();
				reader.close();
				writer.close();
				
				this.showDialog(0);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	// display next level
	protected void levelComplete()
	{
		scene.setChildScene(nextMenu, false, true, true);
	}

	// load level 2
	protected void loadNextLevel()
	{
		// update attributes
		isGameOver = false;
		hasBoss = false;
		level = 3;
		score = 0;
		getPlayer().HEALTH = 150;

		physicsWorld = new PhysicsWorld(new Vector2(0, SensorManager.GRAVITY_EARTH), false);

		// game scene
		scene2 = new Scene();
		spawner = new Updater(this, scene2);

		// set touch listeners to scene and menu
		scene2.setOnSceneTouchListener(controls);
		scene2.setOnAreaTouchListener(controls);

		// create boundaries / walls
		final VertexBufferObjectManager vertexBufferObjectManager = getVertexBufferObjectManager();
		final Rectangle ground = new Rectangle(0, CAMERA_HEIGHT - 2, CAMERA_WIDTH, 2, vertexBufferObjectManager);
		final Rectangle roof = new Rectangle(0, 0, CAMERA_WIDTH, 2, vertexBufferObjectManager);
		final Rectangle left = new Rectangle(0, 0, 2, CAMERA_HEIGHT, vertexBufferObjectManager);
		final Rectangle right = new Rectangle(CAMERA_WIDTH - 2, 0, 2, CAMERA_HEIGHT, vertexBufferObjectManager);
		ground.setColor(0, 0, 0, 0);
		roof.setColor(0, 0, 0, 0);
		right.setColor(0, 0, 0, 0);
		left.setColor(0, 0, 0, 0);

		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0f, 0f, 0f);
		PhysicsFactory.createBoxBody(physicsWorld, ground, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(physicsWorld, roof, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(physicsWorld, left, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(physicsWorld, right, BodyType.StaticBody, wallFixtureDef);

		// add walls to scene
		scene2.detachChildren();
		scene2.attachChild(ground);
		scene2.attachChild(roof);
		scene2.attachChild(left);
		scene2.attachChild(right);

		// create background
		final AutoParallaxBackground autoParallaxBackground = new AutoParallaxBackground(0, 0, 0, 5);
		Sprite backSprite = new Sprite(0, CAMERA_HEIGHT - this.mParallaxLayer2.getHeight(), this.mParallaxLayer2, vertexBufferObjectManager);
		autoParallaxBackground.attachParallaxEntity(new ParallaxEntity(10.0f, backSprite));
		autoParallaxBackground.attachParallaxEntity(new ParallaxEntity(18.0f, new Sprite(0, CAMERA_HEIGHT - mParallaxStars.getHeight(), mParallaxStars, vertexBufferObjectManager)));
		scene2.setBackground(autoParallaxBackground);

		// register physicsWorld and spawner as update handlers to be called every update
		scene2.registerUpdateHandler(this.physicsWorld);
		scene2.registerUpdateHandler(spawner);

		// add player
		player = new Player(CAMERA_WIDTH/2, CAMERA_HEIGHT/2, playerTextureRegion, getVertexBufferObjectManager(), this);

		// start music
		setMusic(2);

		// place HUD on screen
		healthText = new Text(0, 0, this.smallFont, "Health: " + player.getHealth(), new TextOptions(HorizontalAlign.CENTER), vertexBufferObjectManager);
		scoreText = new Text(0, 20, this.smallFont, "Score: 0000", new TextOptions(HorizontalAlign.CENTER), vertexBufferObjectManager);
		healthText.setColor(1, 1, 1, 1);
		scoreText.setColor(1, 1, 1, 1);
		scene2.attachChild(healthText);
		scene2.attachChild(scoreText);

		// remove menu and reset it to new level
		getScene().reset();
		getScene().clearChildScene();
		nextMenu.reset();
		nextMenu.clearChildScene();
		nextMenu.closeMenuScene();

		this.getEngine().setScene(scene2);
	}

	// reset to beginning level and state
	public void reset()
	{
		// update attributes
		isGameOver = false;
		hasBoss = false;
		level = 1;
		score = 0;
		getPlayer().HEALTH = 100;

		physicsWorld = new PhysicsWorld(new Vector2(0, SensorManager.GRAVITY_EARTH), false);

		// game scene
		scene = new Scene();
		spawner = new Updater(this, scene);

		// set touch listeners to scene and menu
		scene.setOnSceneTouchListener(controls);
		scene.setOnAreaTouchListener(controls);

		// create boundaries / walls
		final VertexBufferObjectManager vertexBufferObjectManager = getVertexBufferObjectManager();
		final Rectangle ground = new Rectangle(0, CAMERA_HEIGHT - 2, CAMERA_WIDTH, 2, vertexBufferObjectManager);
		final Rectangle roof = new Rectangle(0, 0, CAMERA_WIDTH, 2, vertexBufferObjectManager);
		final Rectangle left = new Rectangle(0, 0, 2, CAMERA_HEIGHT, vertexBufferObjectManager);
		final Rectangle right = new Rectangle(CAMERA_WIDTH - 2, 0, 2, CAMERA_HEIGHT, vertexBufferObjectManager);
		ground.setColor(0, 0, 0, 0);
		roof.setColor(0, 0, 0, 0);
		right.setColor(0, 0, 0, 0);
		left.setColor(0, 0, 0, 0);

		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0f, 0f, 0f);
		PhysicsFactory.createBoxBody(physicsWorld, ground, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(physicsWorld, roof, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(physicsWorld, left, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(physicsWorld, right, BodyType.StaticBody, wallFixtureDef);

		// add walls to scene
		scene.detachChildren();
		scene.attachChild(ground);
		scene.attachChild(roof);
		scene.attachChild(left);
		scene.attachChild(right);

		// create parallax background
		final AutoParallaxBackground autoParallaxBackground = new AutoParallaxBackground(0, 0, 0, 5);
		Sprite backSprite = new Sprite(0, CAMERA_HEIGHT - this.mParallaxLayer1.getHeight(), this.mParallaxLayer1, vertexBufferObjectManager);
		autoParallaxBackground.attachParallaxEntity(new ParallaxEntity(10.0f, backSprite));
		autoParallaxBackground.attachParallaxEntity(new ParallaxEntity(15.0f, new Sprite(0, CAMERA_HEIGHT - mParallaxStars.getHeight(), mParallaxStars, vertexBufferObjectManager)));
		scene.setBackground(autoParallaxBackground);

		// register physicsWorld and spawner as update handlers to be called every update
		scene.registerUpdateHandler(this.physicsWorld);
		scene.registerUpdateHandler(spawner);

		// add player
		player = new Player(CAMERA_WIDTH/2, CAMERA_HEIGHT/2, playerTextureRegion, getVertexBufferObjectManager(), this);

		// start music
		setMusic(1);

		// place HUD on screen
		healthText = new Text(0, 0, this.smallFont, "Health: " + player.getHealth(), new TextOptions(HorizontalAlign.CENTER), vertexBufferObjectManager);
		scoreText = new Text(0, 20, this.smallFont, "Score: 0000", new TextOptions(HorizontalAlign.CENTER), vertexBufferObjectManager);
		healthText.setColor(1, 1, 1, 1);
		scoreText.setColor(1, 1, 1, 1);
		scene.attachChild(healthText);
		scene.attachChild(scoreText);

		// remove menu and reset it to new level
		getScene().reset();
		getScene().clearChildScene();

		this.getEngine().setScene(scene);
	}

	// setup boss 
	public void startBoss()
	{
		hasBoss = true;
		boss = new Boss(0, 0, bossTextureRegion, getVertexBufferObjectManager(), this);
		getScene().attachChild(boss);
	}

	// tear down boss on defeat
	public void resetBoss()
	{
		hasBoss = false;

		if (boss != null)
		{
			getScene().detachChild(boss);
			boss = null;
		}
	}


	//---------------------//
	// Getters and Setters //
	//---------------------//
	public void setMusic(int l)
	{
		// start level 1 music, pause level 2
		if (l == 1)
		{
			level1Music.play();
			level2Music.pause();
		}
		// start l2, pause l1
		else if (l == 2)
		{
			level1Music.pause();
			level2Music.play();
		}
	}
	public void setCrashSound()
	{
		// play a random explosion sound
		Random rand = new Random();
		int crashNum = rand.nextInt(3);

		if (crashNum == 0)
			crashSound1.play();
		else if (crashNum == 1)
			crashSound2.play();
		else if (crashNum == 2)
			crashSound3.play();
	}

	public void setHealth(int h)
	{
		// update health on HUD
		healthText.setText("Health: " + h);
	}
	public void incScore(int s)
	{
		// update score on HUD
		score += s;
		scoreText.setText("Score: " + score);
	}

	public void setVelocity(float x, float y)
	{
		this.xVelocity = x;
		this.yVelocity = y;
	}
	public float getXGravity()
	{
		return this.xVelocity;
	}
	public float getYGravity()
	{
		return this.yVelocity;
	}

	public PhysicsWorld getPhysicsWorld()
	{
		return physicsWorld;
	}
	public Scene getScene()
	{
		if (level == 1)
			return scene;
		else 
			return scene2;
	}
	public MenuScene getMenuScene()
	{
		return menuScene;
	}

	public Player getPlayer()
	{
		return player;
	}

	public int getScore()
	{
		return score;
	}
	
	public ShooterActivity getActivity()
	{
		return this;
	}
}
