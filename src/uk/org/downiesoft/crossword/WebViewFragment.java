package uk.org.downiesoft.crossword;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import uk.org.downiesoft.crossword.WebInfoList.WebInfoListListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;
import android.webkit.JavascriptInterface;

public class WebViewFragment extends Fragment implements WebInfoListListener
{
	
	public static final String TAG = WebViewFragment.class.getName();
	
	public interface WebViewFragmentListener
	{
		void importPuzzle(String html);

		void importSolution(String html);
	}

	private final int STATE_NULL = 0;
	private final int STATE_INDEX = 1;
	private final int STATE_IMPORT = 2;
	private final int STATE_SOLUTION = 3;

	private String iCrosswordId;
	private WebView iWebView;
	private WebManager iWebManager;
	private Button iImportButton;
	private Button iSolutionButton;
	private String iUrl;
	private int iState = STATE_NULL;
	private WebViewFragmentListener iListener;
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
					if (iListener != null)
						iListener.importPuzzle(html);
				}
				break;
				case STATE_INDEX:
				{
					getCrosswordInfo(html);
					break;
				}
				case STATE_SOLUTION:
				{
					if (iListener != null)
						iListener.importSolution(html);
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
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		try
		{
			iListener = (WebViewFragmentListener) activity;
		}
		catch (ClassCastException e)
		{
			throw new ClassCastException(activity.toString() + " must implement WebViewFragmentListListener");
		}
	}

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
		iWebView = (WebView) v.findViewById(R.id.webView);
		iImportButton = (Button) v.findViewById(R.id.webImportButton);
		iSolutionButton = (Button) v.findViewById(R.id.webSolutionButton);
		iWebManager = WebManager.getInstance();
		iWebView.getSettings().setJavaScriptEnabled(true);
		iWebView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");
		iWebView.setWebViewClient(iWebViewClient);
		iState = STATE_INDEX;
		iWebView.loadUrl(iUrl);
		iImportButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				FragmentManager fm = getActivity().getSupportFragmentManager();
				FragmentTransaction ft = fm.beginTransaction();
				WebInfoList infoList = new WebInfoList();
				infoList.setListener(WebViewFragment.this);
				ft.replace(R.id.fragmentContainer, infoList, "web_info_list");
				ft.addToBackStack("web_view_fragment");
				ft.commit();

			}
		});
		iSolutionButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				int id = CrosswordModel.getInstance().crosswordId();
				int searchId = iWebManager.getCrossword(id).searchId();
				iWebView.setWebViewClient(iWebViewClient);
				iState = STATE_SOLUTION;
				iUrl = "http://puzzles.telegraph.co.uk/site/print_crossword?id=" + Integer.toString(searchId) + "&action=solution";
				iWebView.loadUrl(iUrl);
			}
		});
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

	@SuppressWarnings("deprecation")
	private void getCrosswordInfo(String html)
	{
		final String SEARCH_ID = "search_puzzle_number?id=";
		final String CROSSWORD_ID = "</span> - No. ";
		int crosswordId;
		int searchId;
		String date;
		try
		{
			DataInputStream is = new DataInputStream(new ByteArrayInputStream(html.getBytes()));
			String line = is.readLine();
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
					line = is.readLine();
					line = line.substring(line.indexOf(SEARCH_ID) + SEARCH_ID.length());
					line = line.substring(line.indexOf('>') + 1);
					date = line.substring(0, line.indexOf('<'));
					if (iWebManager != null)
						iWebManager.insert(new WebInfo(crosswordId, searchId, date));
				}
				line = is.readLine();
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
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setCancelable(true);
		builder.setTitle(R.string.action_import);
		builder.setMessage(R.string.text_query_continue);
		builder.setPositiveButton(R.string.text_ok, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				WebViewFragment.this.iCrosswordId = Integer.toString(aWebInfo.crosswordId());
				String searchId = Integer.toString(aWebInfo.searchId());
				String message = getActivity().getString(R.string.text_importing);
				Toast.makeText(getActivity(), String.format(message, iCrosswordId), Toast.LENGTH_LONG).show();
				iState = STATE_IMPORT;
				iWebView.setWebViewClient(iWebViewClient);
				iUrl = "http://puzzles.telegraph.co.uk/site/print_crossword?id=" + searchId;
				iWebView.loadUrl(iUrl);
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.text_cancel, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();				
	}
		
}
