package org.farook.towerofhanoi;

import java.util.HashMap;
import java.util.Map;
import org.grantoo.lib.propeller.PropellerSDK;
import org.grantoo.lib.propeller.PropellerSDKBroadcastReceiver;
import org.grantoo.lib.propeller.PropellerSDKListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

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

		if (BuildConfig.DEBUG) {
			// this is for test startup
			PropellerSDK.useSandbox();
			// use the sandbox key/secret pair
			PropellerSDK.initialize(
				"51145f0fdce0751836000028",
				"841f983c-e97a-190b-62bf-ccc2ec29cde3");
		} else {
			// use the production game/secret pair
			PropellerSDK.initialize(
				"",
				"");
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

}
