package org.farook.towerofhanoi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.grantoo.lib.propeller.PropellerSDK;
import org.grantoo.lib.propeller.PropellerSDKBroadcastReceiver;
import org.grantoo.lib.propeller.PropellerSDKListener;
import org.grantoo.lib.propeller.gcm.PropellerSDKGCM;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.widget.Toast;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Request;
import com.facebook.Request.GraphUserCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.NewPermissionsRequest;
import com.facebook.Session.OpenRequest;
import com.facebook.SessionDefaultAudience;
import com.facebook.SessionLoginBehavior;
import com.facebook.SessionState;
import com.facebook.SharedPreferencesTokenCachingStrategy;
import com.facebook.TokenCachingStrategy;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;

/*******************************************************************************
 * Bridge class used to communicate between the host activity and the Propeller
 * SDK.
 */
public class PropellerSDKBridge extends PropellerSDKListener {

	/**
	 * Broadcast intent challenge count changed action.
	 */
	private static final String INTENT_ACTION_CHALLENGE_COUNT_CHANGED = "PropellerSDKChallengeCountChanged";

	/**
	 * Broadcast intent tournament info action.
	 */
	private static final String INTENT_ACTION_TOURNAMENT_INFO = "PropellerSDKTournamentInfo";

	/**
	 * Host activity.
	 */
	private TowerOfHanoiActivity mActivity;

	/**
	 * Challenge match id.
	 */
	private String mMatchId;

	/**
	 * Challenge tournament id.
	 */
	private String mTournamentId;

	/**
	 * Challenge count intent filter.
	 */
	private IntentFilter mIntentFilter;

	/**
	 * Challenge count broadcast receiver.
	 */
	private BroadcastReceiver mBroadcastReceiver;

	/**
	 * Facebook launching context.
	 */
	private Context mContext;

	/**
	 * Facebook graph user callback.
	 */
	private GraphUserCallback mGraphUserCallback;

	/**
	 * Facebook session status callback.
	 */
	private Session.StatusCallback mSessionStatusCallback;

	/**
	 * Facebook UI lifecycle helper.
	 */
	private UiLifecycleHelper mUiLifecycleHelper;

	/***************************************************************************
	 * Propeller SDK processing for the onCreate() Activity lifecycle.
	 * 
	 * @param activity Host activity.
	 * @param savedInstanceState If the activity is being re-initialized after
	 *        previously being shut down then this Bundle contains the data it
	 *        most recently supplied in onSaveInstanceState(Bundle). Note:
	 *        Otherwise it is null.
	 */
	public void onCreate(TowerOfHanoiActivity activity, Bundle savedInstanceState) {
		mActivity = activity;

		PropellerSDK.onCreate(activity);

		boolean gameHasLogin = true;
		boolean gameHasInvite = true;
		boolean gameHasShare = true;

		if (BuildConfig.DEBUG) {
			// use the sandbox GCM sender ID
			PropellerSDKGCM.onCreate(activity, "709454243316");
			// this is for test startup
			PropellerSDK.useSandbox();
			// use the sandbox key/secret pair
			PropellerSDK.initialize(
				"51145f0fdce0751836000028",
				"841f983c-e97a-190b-62bf-ccc2ec29cde3",
				gameHasLogin,
				gameHasInvite,
				gameHasShare);
		} else {
			// use the production GCM sender ID
			PropellerSDKGCM.onCreate(activity, "");
			// use the production game/secret pair
			PropellerSDK.initialize(
				"",
				"",
				gameHasLogin,
				gameHasInvite,
				gameHasShare);
		}

		PropellerSDK.instance().setOrientation("landscape");

		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(INTENT_ACTION_CHALLENGE_COUNT_CHANGED);
		mIntentFilter.addAction(INTENT_ACTION_TOURNAMENT_INFO);

		mBroadcastReceiver = getBroadcastReceiver();
	}

	/***************************************************************************
	 * Propeller SDK processing for the onResume() Activity lifecycle.
	 * 
	 * @param activity Host activity.
	 * @param requestCode The integer request code originally supplied to
	 *        startActivityForResult(), allowing you to identify who this result
	 *        came from.
	 * @param resultCode The integer result code returned by the child activity
	 *        through its setResult().
	 * @param data An Intent, which can return result data to the caller
	 *        (various data can be attached to Intent "extras").
	 */
	public void onActivityResult(TowerOfHanoiActivity activity, int requestCode, int resultCode, Intent data) {
		mActivity = activity;

		PropellerSDK.onActivityResult(
			activity,
			requestCode,
			resultCode,
			data);
	}

	/***************************************************************************
	 * Propeller SDK processing for the onResume() Activity lifecycle.
	 * 
	 * @param activity Host activity.
	 * @param requestCode The integer request code originally supplied to
	 *        startActivityForResult(), allowing you to identify who this result
	 *        came from.
	 * @param resultCode The integer result code returned by the child activity
	 *        through its setResult().
	 * @param data An Intent, which can return result data to the caller
	 *        (various data can be attached to Intent "extras").
	 */
	protected void onResume(TowerOfHanoiActivity activity) {
		mActivity = activity;

		PropellerSDK.onResume(activity);

		LocalBroadcastManager
			.getInstance(activity)
			.registerReceiver(
				mBroadcastReceiver,
				mIntentFilter);
	}

	/***************************************************************************
	 * Propeller SDK processing for the onPause() Activity lifecycle.
	 * 
	 * @param activity Host activity.
	 */
	public void onPause(TowerOfHanoiActivity activity) {
		mActivity = activity;

		LocalBroadcastManager
			.getInstance(activity)
			.unregisterReceiver(mBroadcastReceiver);
	}

	/***************************************************************************
	 * Retrieves the challenge count broadcast receiver.
	 * 
	 * @return The challenge count broadcast receiver.
	 */
	private BroadcastReceiver getBroadcastReceiver() {
		return new PropellerSDKBroadcastReceiver() {

			@Override
			public void onReceive(Context context, String action, Map<String, Object> data) {
				if (action.equals(INTENT_ACTION_CHALLENGE_COUNT_CHANGED)) {
					int count = 0;
					Object countObject = data.get("count");

					if ((countObject != null) &&
						(countObject instanceof Integer)) {
						count = ((Integer) countObject).intValue();
					}

					mActivity.updateChallengeCount(count);
				} else if (action.equals(INTENT_ACTION_TOURNAMENT_INFO)) {
					mActivity.updateTournamentInfo(data);
				}
			}

		};
	}

	/***************************************************************************
	 * Launches the Propeller SDK.
	 */
	public void launch() {
		PropellerSDK.instance().launch(this);
	}

	/***************************************************************************
	 * Launches the Propeller SDK after obtaining a match score.
	 * 
	 * @param score Match score to launch with.
	 */
	public void launchWithMatchResult(long score) {
		Map<String, Object> matchResult = new HashMap<String, Object>();
		matchResult.put("tournamentID", mTournamentId);
		matchResult.put("matchID", mMatchId);
		matchResult.put("score", Long.valueOf(score));

		PropellerSDK.instance().launchWithMatchResult(matchResult, this);
	}

	/***************************************************************************
	 * Request synchronization of challenge counts.
	 */
	public void syncChallengeCounts() {
		PropellerSDK.instance().syncChallengeCounts();
	}

	/***************************************************************************
	 * Request synchronization of tournament information.
	 */
	public void syncTournamentInfo() {
		PropellerSDK.instance().syncTournamentInfo();
	}

	/***************************************************************************
	 * Called when the Propeller SDK completed with exit.
	 */
	@Override
	public void sdkCompletedWithExit() {
		mActivity.resetScene();
		mActivity.showButtons();
	}

	/***************************************************************************
	 * Called when the Propeller SDK completed with a match.
	 * 
	 * @param data Match data returned by the Propeller SDK. The data contains a
	 *        'tournamentID' (String), 'matchID' (String), and 'params'
	 *        (Map<String, Object>) which is a map of game context match
	 *        parameters. The match parameters, at a minimum, will contain a
	 *        match 'seed' (Long) and a match 'round' (Integer) which can be
	 *        used for setting up deterministic properties of the game. If
	 *        configured, the match parameters will also contain match 'options'
	 *        (Map<String, Object>) which is the set of game options selected by
	 *        the match creator.
	 */
	@Override
	public void sdkCompletedWithMatch(Map<String, Object> data) {
		mTournamentId = (String) data.get("tournamentID");
		mMatchId = (String) data.get("matchID");
		mActivity.startGame(true);
	}

	/***************************************************************************
	 * Called when the Propeller SDK failed.
	 * 
	 * @param message Failure message.
	 * @param result Failed result returned by the Propeller SDK.
	 */
	@Override
	public void sdkFailed(String message, Map<String, Object> result) {
		mActivity.resetScene();
		mActivity.showButtons();
	}

	/***************************************************************************
	 * Called when the Propeller SDK wants to perform a social login.
	 * 
	 * @param context Calling context.
	 * @param allowCache True if cached login credentials can be used, false
	 *        otherwise.
	 * @return True if the social login was performed, false otherwise.
	 */
	@Override
	public boolean sdkSocialLogin(Context context, boolean allowCache) {
		mContext = context;

		Session session = Session.getActiveSession();

		if ((session != null) &&
			session.isOpened() &&
			((session.getState() == SessionState.OPENED) ||
			(session.getState() == SessionState.OPENED_TOKEN_UPDATED))) {
			return false;
		}

		OpenRequest openRequest =
			new Session.OpenRequest((Activity) context)
				.setLoginBehavior(SessionLoginBehavior.SSO_WITH_FALLBACK)
				.setDefaultAudience(SessionDefaultAudience.EVERYONE)
				.setPermissions(Arrays.asList("email"));

		if (session == null) {
			openRequest.setCallback(mSessionStatusCallback);

			TokenCachingStrategy tokenCachingStrategy =
				new SharedPreferencesTokenCachingStrategy(context);

			session = new Session.Builder(context)
				.setApplicationId(context.getString(R.string.app_id))
				.setTokenCachingStrategy(tokenCachingStrategy)
				.build();
		}

		session.openForRead(openRequest);

		return true;
	}

	/***************************************************************************
	 * Called when the Propeller SDK wants to perform a social invite.
	 * 
	 * @param context Calling context.
	 * @param subject The subject string for the invite.
	 * @param longMessage The message to invite with (long version).
	 * @param shortMessage The message to invite with (short version).
	 * @param linkUrl The URL for where the game can be obtained.
	 * @return True if the social invite was performed, false otherwise.
	 */
	@Override
	public boolean sdkSocialInvite(Context context, String subject, String longMessage, String shortMessage, String linkUrl) {
		return showFeedDialog(context, subject, shortMessage, longMessage, linkUrl, null, true);
	}

	/***************************************************************************
	 * Called when the Propeller SDK wants to perform a social share.
	 * 
	 * @param context Calling context.
	 * @param subject The subject string for the share.
	 * @param longMessage The message to share with (long version).
	 * @param shortMessage The message to share with (short version).
	 * @param linkUrl The URL for where the game can be obtained.
	 * @return True if the social share was performed, false otherwise.
	 */
	public boolean sdkSocialShare(Context context, String subject, String longMessage, String shortMessage, String linkUrl) {
		return showFeedDialog(context, subject, shortMessage, longMessage, linkUrl, null, false);
	}

	/***************************************************************************
	 * Called on the Propeller SDK Activity OnCreate() life cycle phase.
	 * 
	 * @param context Calling context.
	 * @param savedInstanceState If the activity is being re-initialized after
	 *        previously being shut down then this Bundle contains the data it
	 *        most recently supplied in onSaveInstanceState(Bundle). Note:
	 *        Otherwise it is null.
	 */
	@Override
	public void sdkOnCreate(Context context, Bundle savedInstanceState) {
		mContext = context;

		mGraphUserCallback = getGraphUserCallback();

		mSessionStatusCallback = getSessionStatusCallback();

		mUiLifecycleHelper = new UiLifecycleHelper(
			(Activity) context,
			mSessionStatusCallback);

		mUiLifecycleHelper.onCreate(savedInstanceState);
	}

	/***************************************************************************
	 * Called on the Propeller SDK Activity OnActivityResult() life cycle phase.
	 * 
	 * @param context Calling context.
	 * @param requestCode The integer request code originally supplied to
	 *        startActivityForResult(), allowing you to identify who this result
	 *        came from.
	 * @param resultCode The integer result code returned by the child activity
	 *        through its setResult().
	 * @param data An Intent, which can return result data to the caller
	 *        (various data can be attached to Intent "extras").
	 */
	@Override
	public void sdkOnActivityResult(Context context, int requestCode, int resultCode, Intent data) {
		mContext = context;
		mUiLifecycleHelper.onActivityResult(requestCode, resultCode, data);
	}

	/***************************************************************************
	 * Called on the Propeller SDK Activity OnResume() life cycle phase.
	 * 
	 * @param context Calling context.
	 */
	@Override
	public void sdkOnResume(Context context) {
		mContext = context;
		mUiLifecycleHelper.onResume();
	}

	/***************************************************************************
	 * Called on the Propeller SDK Activity OnPause() life cycle phase.
	 * 
	 * @param context Calling context.
	 */
	@Override
	public void sdkOnPause(Context context) {
		mContext = context;
		mUiLifecycleHelper.onPause();
	}

	/***************************************************************************
	 * Called on the Propeller SDK Activity OnSaveInstanceState() life cycle
	 * phase.
	 * 
	 * @param context Calling context.
	 * @param outState Bundle in which to place your saved state.
	 */
	@Override
	public void sdkOnSaveInstanceState(Context context, Bundle outState) {
		mContext = context;
		mUiLifecycleHelper.onSaveInstanceState(outState);
	}

	/***************************************************************************
	 * Called on the Propeller SDK Activity OnDestroy() life cycle phase.
	 * 
	 * @param context Calling context.
	 */
	@Override
	public void sdkOnDestroy(Context context) {
		mContext = context;
		mUiLifecycleHelper.onDestroy();
	}

	/***************************************************************************
	 * Retrieves the Facebook graph user callback.
	 * 
	 * @return The Facebook graph user callback.
	 */
	private GraphUserCallback getGraphUserCallback() {
		return new GraphUserCallback() {

			@Override
			public void onCompleted(GraphUser user, Response response) {
				String result = null;

				if (user == null) {
					try {
						JSONObject json = new JSONObject();
						json.put("failed", response.getError().getErrorMessage());
						result = json.toString();
					} catch (JSONException jsonException) {
						jsonException.printStackTrace();
					}
				} else {
					String id = user.getId();
					String nickname = user.getName();
					String email = (String) user.getProperty("email");
					String token = Session.getActiveSession().getAccessToken();

					try {
						JSONObject json = new JSONObject();
						json.put("provider", "facebook");
						json.put("id", id);
						json.put("nickname", nickname);
						json.put("email", email);
						json.put("token", token);
						result = json.toString();
					} catch (JSONException jsonException) {
						jsonException.printStackTrace();
					}
				}

				PropellerSDK.instance().sdkSocialLoginCompleted(result);
			}

		};
	}

	/***************************************************************************
	 * Retrieves the Facebook session status callback.
	 * 
	 * @return The Facebook session status callback.
	 */
	private Session.StatusCallback getSessionStatusCallback() {
		return new Session.StatusCallback() {

			private boolean mExplicitLogin;

			@SuppressWarnings("deprecation")
			@Override
			public void call(Session session, SessionState state, Exception exception) {
				switch (state) {
					case OPENING:
						mExplicitLogin = true;

						Session.setActiveSession(session);
						break;
					case OPENED:
						Session.setActiveSession(session);

						if (!session.getPermissions().contains("publish_actions")) {
							session.requestNewPublishPermissions(
								new NewPermissionsRequest(
									(Activity) mContext,
									Arrays.asList("publish_actions")));
							break;
						}

					case OPENED_TOKEN_UPDATED:
						if (mExplicitLogin) {
							mExplicitLogin = false;
						} else {
							break;
						}

						Request.executeMeRequestAsync(session, mGraphUserCallback);
						break;
					case CLOSED_LOGIN_FAILED:
						mExplicitLogin = false;

						session.removeCallback(mSessionStatusCallback);
						Session.setActiveSession(null);

						String failedResult = null;

						try {
							JSONObject json = new JSONObject();
							json.put("failed", exception.getMessage());
							failedResult = json.toString();
						} catch (JSONException jsonException) {
							jsonException.printStackTrace();
						}

						PropellerSDK.instance().sdkSocialLoginCompleted(failedResult);
						break;
					case CLOSED:
						mExplicitLogin = false;

						session.removeCallback(mSessionStatusCallback);
						Session.setActiveSession(null);

						PropellerSDK.instance().sdkSocialLoginCompleted(null);
						break;
					default:
				}
			}

		};
	}

	/***************************************************************************
	 * Retrieves the Facebook feed dialog completion listener.
	 * 
	 * @param invite Flags whether or not the listener is for an invite post or
	 *        a share post.
	 * @return The Facebook feed dialog completion listener.
	 */
	private OnCompleteListener getOnCompleteListener(final boolean invite) {
		return new OnCompleteListener() {

			@Override
			public void onComplete(Bundle values, FacebookException error) {
				if (error == null) {
					String postId = values.getString("post_id");

					if (postId != null) {
						Toast.makeText(
							mContext,
							"Posted story, id: " + postId,
							Toast.LENGTH_SHORT).show();
					} else {
						// User clicked the Cancel button
						Toast.makeText(
							mContext.getApplicationContext(),
							"Publish cancelled",
							Toast.LENGTH_SHORT).show();
					}

					if (invite) {
						PropellerSDK.instance().sdkSocialInviteCompleted();
					} else {
						PropellerSDK.instance().sdkSocialShareCompleted();
					}

					return;
				}

				String failedResult = null;

				if (error instanceof FacebookOperationCanceledException) {
					failedResult = "Publish cancelled";
				} else {
					failedResult = "Error posting story";
				}

				// User clicked the "x" button
				Toast.makeText(
					mContext.getApplicationContext(),
					failedResult,
					Toast.LENGTH_SHORT).show();

				if (invite) {
					PropellerSDK.instance().sdkSocialInviteCompleted();
				} else {
					PropellerSDK.instance().sdkSocialShareCompleted();
				}
			}

		};
	}

	/***************************************************************************
	 * Shows the feed dialog.
	 * 
	 * @param context Calling context.
	 * @param name Name of the feed.
	 * @param caption Caption of the feed.
	 * @param description Description of the feed.
	 * @param link URL for the name link.
	 * @param picture URL for the feed image.
	 * @return True if the request was made, false otherwise.
	 */
	private boolean showFeedDialog(Context context, String name, String caption, String description, String link, String picture, boolean invite) {
		mContext = context;

		Session session = Session.getActiveSession();

		if ((session == null) ||
			session.isClosed() ||
			(session.getState() != SessionState.OPENED) &&
			(session.getState() != SessionState.OPENED_TOKEN_UPDATED)) {
			return false;
		}

		Bundle params = new Bundle();

		if (!TextUtils.isEmpty(name)) {
			params.putString("name", name);
		}

		if (!TextUtils.isEmpty(caption)) {
			params.putString("caption", caption);
		}

		if (!TextUtils.isEmpty(description)) {
			params.putString("description", description);
		}

		if (!TextUtils.isEmpty(link)) {
			params.putString("link", link);
		}

		if (!TextUtils.isEmpty(picture)) {
			params.putString("picture", picture);
		}

		WebDialog feedDialog = new WebDialog.FeedDialogBuilder(
			context,
			session,
			params).build();
		feedDialog.setOnCompleteListener(getOnCompleteListener(invite));
		feedDialog.show();

		return true;
	}

}
