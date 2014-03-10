package uk.org.downiesoft.crossword;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class WordEntryDialog extends DialogFragment
{

	public interface WordEntryListener
	{
		public void onWordEntered(String aWord);

		public void onWordEntryCancelled();
	}

	TextView iClueText;
	EditText iEditText;
	Button iOKButton;
	Button iCancelButton;
	Clue iClue;
	String iWord;
	String iHint;
	WordEntryListener iListener;

	public static WordEntryDialog getInstance(Clue aClue, String aWord, String aHint, WordEntryListener aListener)
	{
		WordEntryDialog self = new WordEntryDialog();
		self.iClue = aClue;
		self.iWord = aWord;
		self.iHint = aHint;
		self.iListener = aListener;
		return self;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.word_entry, container, false);
		iEditText = (EditText) view.findViewById(R.id.entryEditText);
		iClueText = (TextView) view.findViewById(R.id.entryClue);
		iOKButton = (Button) view.findViewById(R.id.entryOK);
		iCancelButton = (Button) view.findViewById(R.id.entryCancel);
		// Bundle args=getArguments();
		// iClue = new Clue(args.getBundle("clue"));
		// iHint = args.getString("hint");
		iClueText.setText(iClue.toString());
		iEditText.setText(iWord);
		iEditText.setHint(iHint);
		iOKButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				{
					String word = validate(iEditText.getText().toString());
					if (word != null)
					{
						InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(iEditText.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
						if (iListener != null)
							iListener.onWordEntered(word);
						Dialog dlg = getDialog();
						if (dlg != null)
							dlg.dismiss();
					}
					else
					{
						Toast toast = Toast.makeText(getActivity(), R.string.text_bad_fit, Toast.LENGTH_SHORT);
						toast.setGravity(Gravity.CENTER, 0, 0);
						toast.show();
					}
				}
			}
		});
		iCancelButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(iEditText.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
				Dialog dlg = getDialog();
				if (dlg != null)
					dlg.dismiss();
				if (iListener!=null)
					iListener.onWordEntryCancelled();
			}
		});
		iEditText.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2)
			{
				String word = validate(iEditText.getText().toString());
				if (word != null)
				{
					InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(iEditText.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
					if (iListener != null)
						iListener.onWordEntered(word);
					Dialog dlg = getDialog();
					if (dlg != null)
						dlg.dismiss();
				}
				else
				{
					Toast toast = Toast.makeText(getActivity(), R.string.text_bad_fit, Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
				}
				return true;
			}
		});
		Dialog dlg = getDialog();
		if (dlg != null)
		{
			dlg.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
			dlg.setTitle(R.string.text_enter_word);
		}
		return view;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		iEditText.requestFocus();
		InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.showSoftInput(iEditText, InputMethodManager.SHOW_FORCED);
	}

	private String validate(String aWord)
	{
		String word = aWord.replace(" ", "");
		if (word.length() == iHint.length())
		{
			for (int i = 0; i < iHint.length(); i++)
			{
				if (iHint.charAt(i) != '*' && word.charAt(i) != iHint.charAt(i))
					return null;
			}
		}
		else
			return null;
		return word;
	}
}
