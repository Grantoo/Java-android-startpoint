package org.farook.towerofhanoi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Stack;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.modifier.RotationModifier;
import org.andengine.entity.modifier.ScaleModifier;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.bitmap.BitmapTexture;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.HorizontalAlign;
import org.andengine.util.adt.io.in.IInputStreamOpener;
import org.andengine.util.debug.Debug;
import android.content.Intent;
import android.graphics.Color;
import android.opengl.GLES20;
import android.os.Bundle;

public class TowerOfHanoiActivity extends SimpleBaseGameActivity {

	private static int CAMERA_WIDTH = 800;

	private static int CAMERA_HEIGHT = 480;

	private static float GAME_OVER_ANIMATION_DURATION = 2.0f;

	private ITextureRegion
		mBackgroundTextureRegion,
		mTowerTextureRegion,
		mRing0,
		mRing1,
		mRing2,
		mRing3;

	private Sprite
		mTower1,
		mTower2,
		mTower3;

	private Stack<Ring>
		mStack1,
		mStack2,
		mStack3;

	private Ring
		mRingObject0,
		mRingObject1,
		mRingObject2,
		mRingObject3;

	private Font mFont;

	private Text
		mMoves,
		mTime,
		mScore,
		mGameOverText,
		mChallengeButtonText,
		mChallengeCountText;

	private Scene mScene;

	private boolean
		mGameRunning,
		mMatchIsChallenge;

	private Rectangle
		mPlayButton,
		mChallengeButton;

	private int
		mMovesValue,
		mTimeValue,
		mScoreValue;

	private PropellerSDKBridge mPropellerSDKBridge;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPropellerSDKBridge = new PropellerSDKBridge();
		mPropellerSDKBridge.onCreate(this, savedInstanceState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		mPropellerSDKBridge.onActivityResult(this, requestCode, resultCode, data);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mPropellerSDKBridge.onResume(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mPropellerSDKBridge.onPause(this);
	}

	@Override
	public EngineOptions onCreateEngineOptions() {
		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera);
	}

	@Override
	protected void onCreateResources() {
		try {
			// set up bitmap textures
			ITexture backgroundTexture = new BitmapTexture(
				getTextureManager(),
				new IInputStreamOpener() {

					@Override
					public InputStream open() throws IOException {
						return getAssets().open("gfx/background.png");
					}

				});

			ITexture towerTexture = new BitmapTexture(
				getTextureManager(),
				new IInputStreamOpener() {

					@Override
					public InputStream open() throws IOException {
						return getAssets().open("gfx/tower.png");
					}

				});

			ITexture ring0 = new BitmapTexture(
				getTextureManager(),
				new IInputStreamOpener() {

					@Override
					public InputStream open() throws IOException {
						return getAssets().open("gfx/ring0.png");
					}

				});

			ITexture ring1 = new BitmapTexture(
				getTextureManager(),
				new IInputStreamOpener() {

					@Override
					public InputStream open() throws IOException {
						return getAssets().open("gfx/ring1.png");
					}

				});

			ITexture ring2 = new BitmapTexture(
				getTextureManager(),
				new IInputStreamOpener() {

					@Override
					public InputStream open() throws IOException {
						return getAssets().open("gfx/ring2.png");
					}

				});

			ITexture ring3 = new BitmapTexture(
				getTextureManager(),
				new IInputStreamOpener() {

					@Override
					public InputStream open() throws IOException {
						return getAssets().open("gfx/ring3.png");
					}

				});

			// load bitmap textures into VRAM
			backgroundTexture.load();
			towerTexture.load();
			ring0.load();
			ring1.load();
			ring2.load();
			ring3.load();

			// set up texture regions
			mBackgroundTextureRegion = TextureRegionFactory.extractFromTexture(backgroundTexture);
			mTowerTextureRegion = TextureRegionFactory.extractFromTexture(towerTexture);
			mRing0 = TextureRegionFactory.extractFromTexture(ring0);
			mRing1 = TextureRegionFactory.extractFromTexture(ring1);
			mRing2 = TextureRegionFactory.extractFromTexture(ring2);
			mRing3 = TextureRegionFactory.extractFromTexture(ring3);

			// create the stacks
			mStack1 = new Stack<Ring>();
			mStack2 = new Stack<Ring>();
			mStack3 = new Stack<Ring>();

			// load the font we are going to use.
			FontFactory.setAssetBasePath("font/");

			mFont = FontFactory.createFromAsset(
				getFontManager(),
				getTextureManager(),
				512, 512,
				TextureOptions.BILINEAR,
				getAssets(),
				"Droid.ttf", 32, true, Color.WHITE);

			mFont.load();
		} catch (IOException e) {
			Debug.e(e);
		}
	}

	@Override
	protected Scene onCreateScene() {
		mGameRunning = false;

		// create new scene
		final Scene scene = new Scene();
		Sprite backgroundSprite = new Sprite(0, 0, mBackgroundTextureRegion, getVertexBufferObjectManager());
		scene.attachChild(backgroundSprite);

		// add the towers
		mTower1 = new Sprite(192, 63, mTowerTextureRegion, getVertexBufferObjectManager());
		mTower2 = new Sprite(400, 63, mTowerTextureRegion, getVertexBufferObjectManager());
		mTower3 = new Sprite(604, 63, mTowerTextureRegion, getVertexBufferObjectManager());
		scene.attachChild(mTower1);
		scene.attachChild(mTower2);
		scene.attachChild(mTower3);

		// create the rings
		mRingObject0 = new Ring(1, 154, 138, mRing0, getVertexBufferObjectManager()) {

			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
				if (getStack().peek().getWeight() != getWeight()) {
					return false;
				}

				setPosition(pSceneTouchEvent.getX() - getWidth() / 2, pSceneTouchEvent.getY() - getHeight() / 2);

				if (pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP) {
					checkForCollisionsWithTowers(this);
				}

				return true;
			}

		};

		mRingObject1 = new Ring(2, 139, 174, mRing1, getVertexBufferObjectManager()) {

			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
				if (getStack().peek().getWeight() != getWeight()) {
					return false;
				}

				setPosition(pSceneTouchEvent.getX() - getWidth() / 2, pSceneTouchEvent.getY() - getHeight() / 2);

				if (pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP) {
					checkForCollisionsWithTowers(this);
				}

				return true;
			}

		};

		mRingObject2 = new Ring(3, 118, 212, mRing2, getVertexBufferObjectManager()) {

			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
				if (getStack().peek().getWeight() != getWeight()) {
					return false;
				}

				setPosition(pSceneTouchEvent.getX() - getWidth() / 2, pSceneTouchEvent.getY() - getHeight() / 2);

				if (pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP) {
					checkForCollisionsWithTowers(this);
				}

				return true;
			}

		};

		mRingObject3 = new Ring(4, 97, 255, mRing3, getVertexBufferObjectManager()) {

			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
				if (getStack().peek().getWeight() != getWeight()) {
					return false;
				}

				setPosition(pSceneTouchEvent.getX() - getWidth() / 2, pSceneTouchEvent.getY() - getHeight() / 2);

				if (pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP) {
					checkForCollisionsWithTowers(this);
				}

				return true;
			}

		};

		scene.attachChild(mRingObject0);
		scene.attachChild(mRingObject1);
		scene.attachChild(mRingObject2);
		scene.attachChild(mRingObject3);

		// add touch handlers
		scene.setTouchAreaBindingOnActionDownEnabled(true);

		// add scores
		int moveWidth = "Moves: XXXX".length();
		mMoves = new Text(5, 5, mFont, "Moves: 0", moveWidth, getVertexBufferObjectManager());
		mMoves.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		mMoves.setAlpha(0.5f);
		scene.attachChild(mMoves);

		int scoreWidth = "Score: XXXX".length();
		mScore = new Text(320, 10, mFont, "Score: 0", scoreWidth, getVertexBufferObjectManager());
		mScore.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		mScore.setAlpha(0.5f);
		mScore.setScale(2.0f);

		int timeWidth = "Time: XXXX".length();
		mTime = new Text(650, 5, mFont, "Time: 0", timeWidth, getVertexBufferObjectManager());
		mTime.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		mTime.setAlpha(0.5f);
		scene.attachChild(mTime);

		// the game-over text.
		mGameOverText = new Text(0, 0, mFont, "Game\nOver", new TextOptions(HorizontalAlign.CENTER), getVertexBufferObjectManager());
		mGameOverText.setPosition((CAMERA_WIDTH - mGameOverText.getWidth()) * 0.5f, (CAMERA_HEIGHT - mGameOverText.getHeight()) * 0.5f);

		mPlayButton = new Rectangle(50, 100, 300, 150, getVertexBufferObjectManager()) {

			@Override
			public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
				if (pSceneTouchEvent.isActionUp()) {
					playPressed();
				}

				return true;
			}

		};

		mPlayButton.setColor(0.8f, 0.0f, 0.0f, 0.8f);
		mPlayButton.attachChild(new Text(50, 75, mFont, "Play", new TextOptions(HorizontalAlign.CENTER), getVertexBufferObjectManager()));

		mChallengeButton = new Rectangle(450, 100, 300, 150, getVertexBufferObjectManager()) {

			@Override
			public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
				if (pSceneTouchEvent.isActionUp()) {
					challengePressed();
				}

				return true;
			}

		};

		mChallengeButton.setColor(0.8f, 0.0f, 0.0f, 0.8f);
		mChallengeButtonText = new Text(50, 75, mFont, "Challenge", new TextOptions(HorizontalAlign.CENTER), getVertexBufferObjectManager());
		mChallengeCountText = new Text(100, 10, mFont, "     ", new TextOptions(HorizontalAlign.CENTER), getVertexBufferObjectManager());
		mChallengeButton.attachChild(mChallengeButtonText);
		mChallengeButton.attachChild(mChallengeCountText);

		scene.registerUpdateHandler(new TimerHandler(1.0f, true, new ITimerCallback() {

			@Override
			public void onTimePassed(final TimerHandler pTimerHandler) {
				if (mGameRunning && mTimeValue > 0) {
					mTimeValue -= 1;
					setTextValues();
				}
			}

		}));

		mScene = null;
		mScene = scene;

		showButtons();

		return scene;
	}

	private void playPressed() {
		startGame(false);
	}

	private void challengePressed() {
		mPropellerSDKBridge.launch();
	}

	public void showButtons() {
		if (!mPlayButton.hasParent()) {
			mScene.attachChild(mPlayButton);
			mScene.registerTouchArea(mPlayButton);
		}

		if (!mChallengeButton.hasParent()) {
			mScene.attachChild(mChallengeButton);
			mScene.registerTouchArea(mChallengeButton);
		}

		mPropellerSDKBridge.syncChallengeCounts();
		mPropellerSDKBridge.syncTournamentInfo();
	}

	private void hideButtons() {
		if (mPlayButton.hasParent()) {
			mScene.unregisterTouchArea(mPlayButton);
			mScene.detachChild(mPlayButton);
		}

		if (mChallengeButton.hasParent()) {
			mScene.unregisterTouchArea(mChallengeButton);
			mScene.detachChild(mChallengeButton);
		}
	}

	public void startGame(boolean matchIsChallenge) {
		mMatchIsChallenge = matchIsChallenge;
		hideButtons();
		resetScene();
		mGameRunning = true;
	}

	private void stopGame() {
		mGameRunning = false;

		// add touch handlers
		mScene.unregisterTouchArea(mRingObject0);
		mScene.unregisterTouchArea(mRingObject1);
		mScene.unregisterTouchArea(mRingObject2);
		mScene.unregisterTouchArea(mRingObject3);

		mScoreValue = (mTimeValue * 5) + (100 * (40 - mMovesValue));

		if (mScoreValue < 0) {
			mScoreValue = 0;
		}

		mScore.setText("Score: " + mScoreValue);

		mScene.attachChild(mScore);
		mScene.attachChild(mGameOverText);

		// wait to show buttons or post match results
		mScene.registerUpdateHandler(
			new TimerHandler(
				GAME_OVER_ANIMATION_DURATION * 2.0f,
				new ITimerCallback() {

					@Override
					public void onTimePassed(TimerHandler pTimerHandler) {
						mScene.unregisterUpdateHandler(pTimerHandler);

						if (mMatchIsChallenge) {
							mPropellerSDKBridge.launchWithMatchResult(mScoreValue);
						} else {
							showButtons();
						}
					}

				}));
	}

	public void resetScene() {
		mScene.detachChild(mScore);
		mScene.detachChild(mGameOverText);

		setGameOverAnimation();

		// remove from an existing stack if there
		if (mRingObject0.getStack() != null)
			mRingObject0.getStack().remove(mRingObject0);
		if (mRingObject1.getStack() != null)
			mRingObject1.getStack().remove(mRingObject1);
		if (mRingObject2.getStack() != null)
			mRingObject2.getStack().remove(mRingObject2);
		if (mRingObject3.getStack() != null)
			mRingObject3.getStack().remove(mRingObject3);

		mRingObject0.setPosition(154, 138);
		mRingObject1.setPosition(139, 174);
		mRingObject2.setPosition(118, 212);
		mRingObject3.setPosition(97, 255);

		// add all rings to stack one
		mStack1.add(mRingObject3);
		mStack1.add(mRingObject2);
		mStack1.add(mRingObject1);
		mStack1.add(mRingObject0);

		// initialize starting position for each ring
		mRingObject0.setStack(mStack1);
		mRingObject1.setStack(mStack1);
		mRingObject2.setStack(mStack1);
		mRingObject3.setStack(mStack1);
		mRingObject0.setTower(mTower1);
		mRingObject1.setTower(mTower1);
		mRingObject2.setTower(mTower1);
		mRingObject3.setTower(mTower1);

		mScene.registerTouchArea(mRingObject0);
		mScene.registerTouchArea(mRingObject1);
		mScene.registerTouchArea(mRingObject2);
		mScene.registerTouchArea(mRingObject3);

		mMovesValue = 0;
		mTimeValue = 100;
		mScoreValue = 0;

		setTextValues();
	}

	private void checkForCollisionsWithTowers(Ring ring) {
		Stack<Ring> stack = null;
		Sprite tower = null;
		boolean moved = true;

		if (ring.collidesWith(mTower1) && ((mStack1.size() == 0) || (ring.getWeight() < mStack1.peek().getWeight()))) {
			stack = mStack1;
			tower = mTower1;
		} else if (ring.collidesWith(mTower2) && ((mStack2.size() == 0) || (ring.getWeight() < mStack2.peek().getWeight()))) {
			stack = mStack2;
			tower = mTower2;
		} else if (ring.collidesWith(mTower3) && ((mStack3.size() == 0) || (ring.getWeight() < mStack3.peek().getWeight()))) {
			stack = mStack3;
			tower = mTower3;
		} else {
			stack = ring.getStack();
			tower = ring.getTower();
			moved = false;
		}

		ring.getStack().remove(ring);

		if ((stack != null) && (tower != null) && (stack.size() == 0)) {
			ring.setPosition(tower.getX() + tower.getWidth() / 2 - ring.getWidth() / 2, tower.getY() + tower.getHeight() - ring.getHeight());
		} else if ((stack != null) && (tower != null) && (stack.size() > 0)) {
			ring.setPosition(tower.getX() + tower.getWidth() / 2 - ring.getWidth() / 2, stack.peek().getY() - ring.getHeight());
		}

		stack.add(ring);

		ring.setStack(stack);
		ring.setTower(tower);

		if (moved == true) {
			mMovesValue += 1;
			setTextValues();
		}

		// check for game over condition
		if ((stack == mStack3) && (stack.size() == 4)) {
			// we have moved all the objects over
			stopGame();
		}
	}

	private void setGameOverAnimation() {
		mGameOverText.registerEntityModifier(new ScaleModifier(GAME_OVER_ANIMATION_DURATION, 0.1f, 3.0f));
		mGameOverText.registerEntityModifier(new RotationModifier(GAME_OVER_ANIMATION_DURATION, 0, 1440));
	}

	private void setTextValues() {
		mMoves.setText("Moves: " + mMovesValue);
		mTime.setText("Time: " + mTimeValue);
	}

	public void updateChallengeCount(int count) {
		if (count > 0) {
			mChallengeCountText.setText("" + count);
		} else {
			mChallengeCountText.setText("");
		}
	}

	public void updateTournamentInfo(Map<String, Object> tournamentInfo) {
		if ((tournamentInfo == null) || tournamentInfo.isEmpty()) {
			mChallengeButtonText.setText("Challenge ");
		} else {
			mChallengeButtonText.setText("Tournament");
		}
	}

}
