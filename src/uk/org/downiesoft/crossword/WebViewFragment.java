package uk.org.downiesoft.crossword;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import uk.org.downiesoft.crossword.WebInfoList.WebInfoListListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class WebViewFragment extends Fragment implements WebInfoListListener
{
	
	public static final String TAG = WebViewFragment.class.getName();
	
	public static final int IMPORT_PUZZLE = 1;
	public static final int IMPORT_SOLUTION = 2;

	private final int STATE_NULL = 0;
	private final int STATE_INDEX = 1;
	private final int STATE_IMPORT = 2;
	private final int STATE_SOLUTION = 3;

	private String iCrosswordId;
	private WebView iWebView;
	private WebManager iWebManager;
	private String iUrl;
	private int iState = STATE_NULL;
	private Handler iWebViewClientHandler = new Handler();
	private Runnable iWebViewClientSetter = new Runnable()
	{
		@Override
		public void run()
		{
			iWebView.setWebViewClient(iDefaultWebViewClient);
		}
	};

	private class MyJavaScriptInterface
	{

		@JavascriptInterface
		public void saveHTML(String html)
		{
			iWebViewClientHandler.post(iWebViewClientSetter);
			switch (iState)
			{
				case STATE_IMPORT:
				{
					Intent result = new Intent();
					result.putExtra("import",IMPORT_PUZZLE);
					result.putExtra("html",html);
					getActivity().setResult(Activity.RESULT_OK,result);
					getActivity().finish();
				}
				break;
				case STATE_INDEX:
				{
					getCrosswordInfo(html);
					break;
				}
				case STATE_SOLUTION:
				{
					Intent result = new Intent();
					result.putExtra("import",IMPORT_SOLUTION);
					result.putExtra("html",html);
					getActivity().setResult(Activity.RESULT_OK,result);
					getActivity().finish();
					break;
				}
			}
			iState = STATE_NULL;
		}
	}

	private WebViewClient iDefaultWebViewClient = new WebViewClient();

	private WebViewClient iWebViewClient = new WebViewClient()
	{
		@Override
		public void onPageFinished(WebView view, String url)
		{
			view.loadUrl("javascript:window.HTMLOUT.saveHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
		}

		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
		{
			MainActivity.debug(1, "uk.org.downiesoft.crossword.WebViewClient", String.format("Description: %s, URL %s", description, failingUrl));
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		iUrl = "http://puzzles.telegraph.co.uk/site/crossword_puzzles_cryptic";
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.web_view, container, false);
		setHasOptionsMenu(true);
		iWebView = (WebView) v.findViewById(R.id.webView);
		iWebManager = WebManager.getInstance();
		iWebView.getSettings().setJavaScriptEnabled(true);
		iWebView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");
		iWebView.setWebViewClient(iWebViewClient);
		iState = STATE_INDEX;
		iWebView.loadUrl(iUrl);
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.web_menu,menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_import:
				FragmentManager fm = getActivity().getSupportFragmentManager();
				FragmentTransaction ft = fm.beginTransaction();
				WebInfoList infoList = new WebInfoList();
				infoList.setListener(WebViewFragment.this);
				ft.replace(R.id.webContainer, infoList, "web_info_list");
				ft.addToBackStack("web_view_fragment");
				ft.commit();
				return true;
			case R.id.action_solution:
				int id = CrosswordModel.getInstance().getCrosswordId();
				int searchId = iWebManager.getCrossword(id).searchId();
				iWebView.setWebViewClient(iWebViewClient);
				iState = STATE_SOLUTION;
				iUrl = "http://puzzles.telegraph.co.uk/site/print_crossword?id=" + Integer.toString(searchId) + "&action=solution";
				iWebView.loadUrl(iUrl);
				return true;
		}
		return super.onOptionsItemSelected(item);
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
				if (line.contains("CRYPTIC"))
				{
					line = line.substring(line.indexOf(SEARCH_ID) + SEARCH_ID.length());
					searchId = Integer.parseInt(line.substring(0, line.indexOf('"')));
					line = line.substring(line.indexOf(CROSSWORD_ID) + CROSSWORD_ID.length());
					line = line.substring(0, line.indexOf('<'));
					MainActivity.debug(1, TAG,String.format("line='%s'",line));
					crosswordId = Integer.parseInt(line.replace(",", ""));
					line = reader.readLine();
					line = line.substring(line.indexOf(SEARCH_ID) + SEARCH_ID.length());
					line = line.substring(line.indexOf('>') + 1);
					date = line.substring(0, line.indexOf('<'));
					if (iWebManager != null)
						iWebManager.insert(new WebInfo(crosswordId, searchId, date));
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
		WebViewFragment.this.iCrosswordId = Integer.toString(aWebInfo.crosswordId());
		String searchId = Integer.toString(aWebInfo.searchId());
		String message = getActivity().getString(R.string.text_importing);
		Toast.makeText(getActivity(), String.format(message, iCrosswordId), Toast.LENGTH_LONG).show();
		iState = STATE_IMPORT;
		iWebView.setWebViewClient(iWebViewClient);
		iUrl = "http://puzzles.telegraph.co.uk/site/print_crossword?id=" + searchId;
		iWebView.loadUrl(iUrl);
	}
		
}
