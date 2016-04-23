package uk.org.downiesoft.crossword;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.ByteArrayInputStream;

import uk.org.downiesoft.crossword.WebInfoList.WebInfoListListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class WebViewFragment extends Fragment implements WebInfoListListener
{
	
	public static final String TAG = WebViewFragment.class.getName();

	public interface WebViewListener {
		void onLogin(int aLoginStatus, int aMode);
		void onRetryLogin();
	}	
	
	public static final String ARG_MODE = "arg_mode";
	public static final String ARG_LOGIN = "arg_login";
	public static final String ARG_STATE = "arg_state";
	
	public static final int MODE_NULL = 0;
	public static final int MODE_PUZZLE = 1;
	public static final int MODE_SOLUTION = 2;

	public static final int LOGIN_FAILED = -1;
	public static final int LOGIN_UNDEFINED = 0;
	public static final int LOGIN_SUCCESSFUL = 1;

	private static final int STATE_NULL = 0;
	private static final int STATE_INDEX = 1;
	private static final int STATE_IMPORT = 2;
	private static final int STATE_SOLUTION = 3;

	private static final String URL_CRYPTIC = "http://puzzles.telegraph.co.uk/site/crossword_puzzles_cryptic";
	private static final String URL_SOLUTION = "http://puzzles.telegraph.co.uk/site/print_crossword?id=%s&action=solution";
	private static final String URL_PUZZLE = "http://puzzles.telegraph.co.uk/site/print_crossword?id=%s";
		
	private String mCrosswordId;
	private WebView mWebView;
	private WebManager mWebManager;
	private String mUrl;
	private int mState = STATE_NULL;
	private int mMode = MODE_PUZZLE;
	private int mLoginStatus = LOGIN_UNDEFINED;
	private WebViewListener mListener;
	private final Handler mWebViewClientHandler = new Handler();
	private final Runnable mWebViewClientSetter = new Runnable()
	{
		@Override
		public void run()
		{
			mWebView.setWebViewClient(iDefaultWebViewClient);
		}
	};

	private final Runnable mWebViewLogin = new Runnable()
	{
		@Override
		public void run()
		{
			mListener.onLogin(mLoginStatus, mMode);
			if (mLoginStatus==LOGIN_SUCCESSFUL && mMode == MODE_SOLUTION) {
				int id = CrosswordModel.getInstance().getCrosswordId();
				int searchId = mWebManager.getCrossword(id).searchId();
				mWebView.setWebViewClient(iWebViewClient);
				mState = STATE_SOLUTION;
				mUrl = String.format(URL_SOLUTION,searchId);
				mWebView.loadUrl(mUrl);
			} else {
				mState = STATE_NULL;
			}
		}
	};

	private class MyJavaScriptInterface
	{

		@JavascriptInterface
		public void saveHTML(String html) {
			MainActivity.debug(1, TAG, String.format("saveHTML: mState=%s", mState));
			mWebViewClientHandler.post(mWebViewClientSetter);
			switch (mState) {
				case STATE_IMPORT:
					{
						Intent result = new Intent();
						result.putExtra("html", html);
						getActivity().setResult(Activity.RESULT_OK, result);
						getActivity().finish();
						mState = STATE_NULL;
						break;
					}
				case STATE_INDEX:
					{
						getCrosswordInfo(html);
						mWebViewClientHandler.post(mWebViewLogin);
						break;
					}
				case STATE_SOLUTION:
					{
						Intent result = new Intent();
						result.putExtra("html", html);
						getActivity().setResult(Activity.RESULT_OK, result);
						getActivity().finish();
						mState = STATE_NULL;
						break;
					}
			}
		}
	}
	
	private final WebViewClient iDefaultWebViewClient = new WebViewClient();

	private final WebViewClient iWebViewClient = new WebViewClient()
	{
		@Override
		public void onPageFinished(WebView view, String url)
		{
			MainActivity.debug(1, TAG, String.format("onPageFinished: URL=%s", url));
			view.loadUrl("javascript:window.HTMLOUT.saveHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
		}

		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
		{
			MainActivity.debug(1, TAG, String.format("onReceivedError: %s, URL %s", description, failingUrl));
		}
	};

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (WebViewListener)activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(String.format("Activity %s must implement %s",activity.getClass().getName(),WebViewListener.class.getName()));
		}
	}

	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mUrl = URL_CRYPTIC;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		MainActivity.debug(1, TAG, String.format("onCreateView: %s", savedInstanceState));
		View v = inflater.inflate(R.layout.web_view, container, false);
		setHasOptionsMenu(true);
		mWebView = (WebView) v.findViewById(R.id.webView);
		mWebManager = WebManager.getInstance();
		Bundle args =  getArguments();
		if (args != null) {
			mMode = args.getInt(ARG_MODE,MODE_PUZZLE);
			MainActivity.debug(1, TAG, String.format("onCreateView: mMode=%s", mMode));
		}
		if (savedInstanceState != null) {
			mState = savedInstanceState.getInt(ARG_STATE, STATE_NULL);
			mMode = savedInstanceState.getInt(ARG_MODE, MODE_PUZZLE);
			mLoginStatus = savedInstanceState.getInt(ARG_LOGIN, LOGIN_UNDEFINED);
			MainActivity.debug(1, TAG, String.format("onCreateView: %s", mLoginStatus));
		}
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");
		mWebView.setWebViewClient(iWebViewClient);
		mState = STATE_INDEX;
		mWebView.loadUrl(mUrl);
		return v;
	}

	@Override
	public void onResume()
	{
		super.onResume();
	}

	@Override
	public void onPause()
	{
		super.onPause();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(ARG_MODE, mMode);
		outState.putInt(ARG_STATE, mState);
		outState.putInt(ARG_LOGIN, mLoginStatus);
		super.onSaveInstanceState(outState);
	}

	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.web_view_fragment,menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MainActivity.debug(1, TAG, String.format("onPrepareOptionsMenu mState=%s, mLoggedIn=%s", mState, mLoginStatus));
		if (mState != STATE_NULL || mLoginStatus != LOGIN_FAILED) {
			menu.removeItem(R.id.action_refresh);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_refresh:
				mWebView.setWebViewClient(iWebViewClient);
				mUrl = URL_CRYPTIC;
				mState = STATE_INDEX;
				mWebView.loadUrl(mUrl);
				mListener.onRetryLogin();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public int getLoginStatus() {
		return mLoginStatus;
	}
	
	private void getCrosswordInfo(String html)
	{
		final String SEARCH_ID = "search_puzzle_number?id=";
		final String CROSSWORD_ID = "</span> - No. ";
		int crosswordId;
		int searchId;
		
		String date;
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(html.getBytes())));			
			String line = reader.readLine();
			while (line != null)
			{
				if (line.contains("welcome_message")) {
					mLoginStatus = (line.contains("Welcome back"))? LOGIN_SUCCESSFUL: LOGIN_FAILED;
				} else if (line.contains("CRYPTIC")) {
					line = line.substring(line.indexOf(SEARCH_ID) + SEARCH_ID.length());
					searchId = Integer.parseInt(line.substring(0, line.indexOf('"')));
					line = line.substring(line.indexOf(CROSSWORD_ID) + CROSSWORD_ID.length());
					line = line.substring(0, line.indexOf('<'));
					MainActivity.debug(2, TAG,String.format("line='%s'",line));
					crosswordId = Integer.parseInt(line.replace(",", ""));
					line = reader.readLine();
					line = line.substring(line.indexOf(SEARCH_ID) + SEARCH_ID.length());
					line = line.substring(line.indexOf('>') + 1);
					date = line.substring(0, line.indexOf('<'));
					if (mWebManager != null)
						mWebManager.insert(new WebInfo(crosswordId, searchId, date));
				}
				line = reader.readLine();
			}
		}
		catch (Exception e)
		{
			MainActivity.debug(1, "uk.org.downiesoft.crossword.getCrosswordInfo", "exception " + e.toString() + " " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void onWebInfoListItemSelected(final WebInfo aWebInfo)
	{
		WebViewFragment.this.mCrosswordId = Integer.toString(aWebInfo.crosswordId());
		String searchId = Integer.toString(aWebInfo.searchId());
		String message = getActivity().getString(R.string.text_importing);
		Toast.makeText(getActivity(), String.format(message, mCrosswordId), Toast.LENGTH_LONG).show();
		mState = STATE_IMPORT;
		mWebView.setWebViewClient(iWebViewClient);
		mUrl = String.format(URL_PUZZLE,searchId);
		mWebView.loadUrl(mUrl);
	}
		
}
