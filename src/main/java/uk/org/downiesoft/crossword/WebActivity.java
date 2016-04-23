package uk.org.downiesoft.crossword;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.app.Activity;

public class WebActivity extends FragmentActivity implements WebViewFragment.WebViewListener {

	public static final String TAG=WebActivity.class.getName();

	private WebViewFragment mWebFragment;
	private WebInfoList mWebInfoList;
	private ProgressDialog mProgressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.web_activity);
		mWebFragment = new WebViewFragment();
		mWebFragment.setArguments(getIntent().getExtras());
		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction().replace(R.id.webViewContainer, mWebFragment, WebViewFragment.TAG).commit();
		mWebInfoList = new WebInfoList();
		fm.beginTransaction().replace(R.id.webListContainer, mWebInfoList, WebInfoList.TAG).commit();
		mWebInfoList.setListener(mWebFragment);
	}

	@Override
	protected void onResume() {
		super.onResume();
		int login = mWebFragment.getLoginStatus();
		MainActivity.debug(1, TAG, String.format("onResume %s", login));
		doLogin(login, WebViewFragment.MODE_NULL);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
	}
	
	@Override
	public void onLogin(int aLoginStatus, int aMode) {
		MainActivity.debug(1, TAG, String.format("onLogin(%s)", aLoginStatus));
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		doLogin(aLoginStatus, aMode);
		invalidateOptionsMenu();
	}
	
	public void doLogin(int aLoginStatus, int mMode) {
		View webview = findViewById(R.id.webViewContainer);
		View weblist = findViewById(R.id.webListContainer);
		switch (aLoginStatus) {
			case WebViewFragment.LOGIN_FAILED:
				switch (mMode) {
					case WebViewFragment.MODE_NULL:
						break;
					case WebViewFragment.MODE_SOLUTION:
					case WebViewFragment.MODE_PUZZLE: 
						if (webview != null) {
							webview.setVisibility(View.VISIBLE);
						}
						AlertDialog.Builder builder = new AlertDialog.Builder(this);
						builder.setTitle(R.string.text_login_failed);
						builder.setMessage(R.string.text_please_login);
						builder.setPositiveButton(R.string.text_ok, null);
						builder.create().show();
						break;
				}
				break;
			case WebViewFragment.LOGIN_SUCCESSFUL:
				switch (mMode) {
					case WebViewFragment.MODE_NULL:
						break;
					case WebViewFragment.MODE_PUZZLE: 
						if (webview != null) {
							webview.setVisibility(View.GONE);
						}
						if (weblist != null) {
							weblist.setVisibility(View.VISIBLE);
						}
						break;
					case WebViewFragment.MODE_SOLUTION:
						mProgressDialog = new ProgressDialog(this);
						mProgressDialog.setTitle(R.string.text_getting_solution);
						mProgressDialog.setMessage(getString(R.string.text_please_wait));
						mProgressDialog.setIndeterminate(true);
						mProgressDialog.setCancelable(true);
						mProgressDialog.show();
						break;
				}
				break;
			case WebViewFragment.LOGIN_UNDEFINED:
				startLoginDialog();
				break;
		}
	}

	@Override
	public void onRetryLogin() {
		startLoginDialog();
	}

	private void startLoginDialog() {
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setTitle(R.string.text_logging_in);
		mProgressDialog.setMessage(getString(R.string.text_please_wait));
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setCancelable(true);
		mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface p1) {
					mProgressDialog.dismiss();
					setResult(Activity.RESULT_CANCELED);
					finish();
				}
			});
		mProgressDialog.show();
	}

}
