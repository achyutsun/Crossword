package uk.org.downiesoft.spell;

import java.io.IOException;
import java.util.ArrayList;

import uk.org.downiesoft.crossword.R;
import uk.org.downiesoft.spell.Lexicon.LexiconObserver;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.view.*;

@SuppressLint("DefaultLocale")
public class SpellActivity extends Activity implements LexiconObserver
{

	public static final int REQUEST_CROSSWORD = 1024;

	GridView iGridView;
	EditText iEditText;
	ArrayList<String> iWordList;
	ArrayList<Integer> iMetric;
	ArrayAdapter<String> iAdapter;
	Lexicon iLexicon;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.spell_activity);

		iGridView = (GridView) this.findViewById(R.id.listView);
		iEditText = (EditText) this.findViewById(R.id.textView);
		iEditText.clearFocus();
		iEditText.setOnEditorActionListener(new OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2)
			{
				String word = iEditText.getText().toString().toLowerCase();
				if (word.length() > 1)
				{
					iLexicon.check(word, Lexicon.DICT_ALL, Lexicon.DICT_ALL, false);
				}
				return true;
			}
		});
		Intent intent = getIntent();
		if (intent != null && intent.hasExtra("uk.org.downiesoft.crossword.wordPattern"))
		{
			setResult(Activity.RESULT_CANCELED);
			iEditText.setText(intent.getExtras().getString("uk.org.downiesoft.crossword.wordPattern", ""));
			iGridView.setOnItemClickListener(new OnItemClickListener()
			{

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id)
				{
					String word = iWordList.get(position);
					Intent intent = new Intent();
					intent.putExtra("uk.org.downiesoft.spell.wordResult", word);
					setResult(Activity.RESULT_OK, intent);
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(iEditText.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
					finish();
				}
			});
			if (intent.hasExtra("uk.org.downiesoft.crossword.clueText"))
			{
				TextView clueText = (TextView) findViewById(R.id.clueText);
				String text = intent.getExtras().getString("uk.org.downiesoft.crossword.clueText", "");
				if (text.length() > 0)
				{
					clueText.setText(text);
					clueText.setVisibility(View.VISIBLE);
				}
			}
		}

		try
		{
			iLexicon = new Lexicon(this, this);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Toast.makeText(this, R.string.text_no_lexicon, Toast.LENGTH_LONG).show();
			finish();
		}

		iWordList = new ArrayList<String>();
		iMetric = new ArrayList<Integer>();
		iAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, iWordList);
		iGridView.setEmptyView(findViewById(R.id.listEmpty));
		iGridView.setAdapter(iAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.spell_menu,menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		String word = iEditText.getText().toString().replace(" ", "");
		switch (item.getItemId()) {
			case R.id.action_check:
				if (word.length() > 2)
				{
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(iEditText.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
					iWordList.clear();
					iMetric.clear();
					iAdapter.notifyDataSetChanged();
					iLexicon.check(word, Lexicon.DICT_ALL, Lexicon.DICT_ALL, true);
				}
				return true;
			case R.id.action_anagram:
				if (word.length() > 2)
				{
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(iEditText.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
					iWordList.clear();
					iMetric.clear();
					iAdapter.notifyDataSetChanged();
					iLexicon.anagram(word, false, Lexicon.DICT_ALL, Lexicon.LEVEL_CHAMPIONSHIP);
				}
				return true;
			case R.id.action_crossword:
				if (word.length() > 2)
				{
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(iEditText.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
					iWordList.clear();
					iMetric.clear();
					iAdapter.notifyDataSetChanged();
					iLexicon.xWordMatch(word, true);
				}
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean reportMatch(String aMatch, int aDistance)
	{
		insertIsq(aMatch, aDistance);
		iAdapter.notifyDataSetChanged();
		return false;
	}

	@Override
	public void reportComplete()
	{
		// TODO Auto-generated method stub

	}

	private int insertIsq(String aWord, int aMetric)
	{
		int i = 0;
		while (i < iMetric.size())
		{
			if (iMetric.get(i).intValue() > aMetric)
			{
				iWordList.add(i, aWord);
				//iWordList.add(i, String.format("%s (%d)",aWord,aMetric));
				iMetric.add(i, aMetric);
				return i;
			}
			i++;
		}
		iWordList.add(i, aWord);
		//iWordList.add(i, String.format("%s (%d)",aWord,aMetric));
		iMetric.add(i, aMetric);
		return i;
	}
}
