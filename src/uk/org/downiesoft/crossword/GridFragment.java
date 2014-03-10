package uk.org.downiesoft.crossword;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class GridFragment extends Fragment implements GridView.GridObserver, WordEntryDialog.WordEntryListener
{
	public static final String TAG="uk.org.downiesoft.crossword.GridFragment";
	
	private View iView;
	private RelativeLayout iGridLayout;
	private RelativeLayout iEmptyLayout;
	private GridView iGridView;
	private TextView iTextView;
	private Button iEnterButton;
	private Button iEraseButton;
	private Button iCluesButton;
	private CrosswordModel iCrossword;
	private WordEntryDialog iWordEntry;
	private int iCursorX=0;
	private int iCursorY=0;
	private int iCursorDirection=CrosswordModel.CLUE_ACROSS;
	private boolean iGuardBackKey=false;
	private long iBackTimestampMillis=0;
	private final int iBackTimeoutMillis=3000;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		iView = inflater.inflate(R.layout.crossword_grid, container, false);
		iGridLayout = (RelativeLayout) iView.findViewById(R.id.gridLayout);
		iEmptyLayout = (RelativeLayout) iView.findViewById(R.id.emptyLayout);
		iGridView = (GridView) iView.findViewById(R.id.gridView);
		iTextView = (TextView) iView.findViewById(R.id.clueText);
		iEnterButton = (Button) iView.findViewById(R.id.gridEnterButton);
		iEraseButton = (Button) iView.findViewById(R.id.gridEraseButton);
		iCluesButton = (Button) iView.findViewById(R.id.gridCluesButton);
		iCrossword=((MainActivity)getActivity()).getCrossword();
		
		if (iCrossword == null || !iCrossword.isValid())
		{
			iGridLayout.setVisibility(View.GONE);
			iEmptyLayout.setVisibility(View.VISIBLE);
		}
		else
		{
			iGridLayout.setVisibility(View.VISIBLE);
			iEmptyLayout.setVisibility(View.GONE);
		}
		if (iGridView != null)
		{
			iGridView.setObserver(this);
			iGridView.setCrossword(iCrossword);
		}
		iEnterButton.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View arg0)
			{
				String hint = iCrossword.getCluePattern(new Point(iCursorX,iCursorY),iCursorDirection);
				FragmentManager fm = getActivity().getSupportFragmentManager();
				iWordEntry = WordEntryDialog.getInstance(iGridView.getCurrentClue(), "", hint, GridFragment.this);
				FragmentTransaction ft = fm.beginTransaction();
				ft.replace(R.id.fragmentContainer, iWordEntry, "word_entry");
				ft.addToBackStack("word_entry");
				ft.commit();
			}
		});
		iEraseButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				iCrossword.eraseWord(new Point(iCursorX,iCursorY), iCursorDirection);
				iGridView.invalidate();
			}
		});
		iCluesButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				Clue clue=iCrossword.clueAt(new Point(iCursorX,iCursorY),iCursorDirection);
				Fragment cluesFragment=new CluesFragment();
				Bundle args=new Bundle();
				args.putInt("direction",clue.iType);
				args.putInt("number", clue.iNumber);
				cluesFragment.setArguments(args);
				FragmentManager fm = getActivity().getSupportFragmentManager();
				FragmentTransaction ft = fm.beginTransaction();
				ft.replace(R.id.fragmentContainer, cluesFragment, "clues_fragment");
				ft.addToBackStack("clues_fragment");
				ft.commit();
			}
		});
		return iView;
	}

	@Override 
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState!=null)
		{
			iCursorX=savedInstanceState.getInt("cursorX",0);
			iCursorY=savedInstanceState.getInt("cursorY",0);
			iCursorDirection=savedInstanceState.getInt("cursorDirection",CrosswordModel.CLUE_ACROSS);
		}
		else
		{
			getSavedState();
		}
		if (iCrossword!=null && iCrossword.isValid())
			iGridView.setCursor(iCursorX, iCursorY, iCursorDirection);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		iGuardBackKey=true;
		iGridView.setObserver(this);
		iGridView.setCrossword(iCrossword);
		if (iCrossword!=null && iCrossword.isValid())
			iGridView.setCursor(iCursorX, iCursorY, iCursorDirection);
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		if (iGridView != null)
		{
			iCursorX = iGridView.getCursorX();
			iCursorY = iGridView.getCursorY();
			iCursorDirection = iGridView.getCursorDirection();
			outState.putInt("cursorX", iCursorX);
			outState.putInt("cursorY", iCursorY);
			outState.putInt("cursorDirection", iCursorDirection);
		}
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		saveState();
		iGuardBackKey=false;
	}

	public void getSavedState()
	{
		SharedPreferences settings=getActivity().getSharedPreferences("CROSSWORD_SETTING", Context.MODE_PRIVATE);
		iCursorX=settings.getInt("cursorX", 0);
		iCursorY=settings.getInt("cursorY", 0);
		iCursorDirection=settings.getInt("cursorDirection", CrosswordModel.CLUE_ACROSS);		
	}

	public void saveState()
	{
		SharedPreferences settings=getActivity().getSharedPreferences("CROSSWORD_SETTING", Context.MODE_PRIVATE);
		Editor editor=settings.edit();
		editor.putInt("cursorX", iCursorX);
		editor.putInt("cursorY", iCursorY);
		editor.putInt("cursorDirection", iCursorDirection);
		editor.commit();
	}
	
	public void setCrossword(CrosswordModel aCrossword)
	{
		iCrossword = aCrossword;
		if (iGridView != null)
		{
			iGridView.setCrossword(iCrossword);
		}
		if (iCrossword == null || !iCrossword.isValid())
		{
			iGridLayout.setVisibility(View.GONE);
			iEmptyLayout.setVisibility(View.VISIBLE);
		}
		else
		{
			iGridLayout.setVisibility(View.VISIBLE);
			iEmptyLayout.setVisibility(View.GONE);
		}
	}

	public void resetClue()
	{
		for (int row=0; row<CrosswordModel.GRID_SIZE; row++)
			for (int col=0; col<CrosswordModel.GRID_SIZE; col++)
				if (!iCrossword.isBlank(col,row))
				{
					Point pos=new Point(col,row);
					for (int dir=CrosswordModel.CLUE_ACROSS; dir<=CrosswordModel.CLUE_DOWN; dir++)
					{
						Clue clue=iCrossword.clueAt(pos, dir);
						if (clue!=null)
						{
							iGridView.setCursor(pos.x, pos.y, dir);
							iTextView.setText(clue.toString());
							return;
						}
					}
				}
	}
	
	@Override
	public void onClueSelected(Clue aClue, int aCursorX, int aCursorY, int aCursorDirection)
	{
		if (aClue != null)
			iTextView.setText(aClue.toString());
		iCursorX = aCursorX;
		iCursorY = aCursorY;
		iCursorDirection = aCursorDirection;
		saveState();
	}

	@Override
	public void onWordEntered(String aWord)
	{
		enterWord(aWord);
		((MainActivity)getActivity()).synch();
		FragmentManager fm = getActivity().getSupportFragmentManager();
		fm.popBackStack("word_entry", FragmentManager.POP_BACK_STACK_INCLUSIVE);
		if (iCrossword.crosswordCompleted())
		{
			Toast.makeText(getActivity(), R.string.text_congrats, Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onWordEntryCancelled()
	{
		FragmentManager fm = getActivity().getSupportFragmentManager();
		fm.popBackStack("word_entry", FragmentManager.POP_BACK_STACK_INCLUSIVE);
		Fragment frag=fm.findFragmentByTag("word_entry");
		if (frag!=null)
			fm.beginTransaction().remove(frag).commit();
	}

	public void onClueClicked(int aDirection, int aNum)
	{
		Point pos=iCrossword.locateClue(aDirection, aNum);
		if (pos!=null)
		{
			iGridView.setCursor(pos.x, pos.y, aDirection);
			FragmentManager fm = getActivity().getSupportFragmentManager();
			fm.popBackStack("clues_fragment", FragmentManager.POP_BACK_STACK_INCLUSIVE);
			Fragment frag=fm.findFragmentByTag("clues_fragment");
			if (frag!=null)
				fm.beginTransaction().remove(frag).commit();
		}
	}

	public boolean offerBackKeyEvent()
	{
		if (iGuardBackKey)
		{
	    	long now=System.currentTimeMillis();
	    	if (now-iBackTimestampMillis>iBackTimeoutMillis)
	    	{
	    		Toast.makeText(getActivity(),R.string.text_press_back_again, Toast.LENGTH_SHORT).show();
	    		iBackTimestampMillis=now;
		        return true;
	    	}
		}
		return false;
	}
	
	public void update()
	{
		if (iGridView!=null)
			iGridView.invalidate();
	}

	public String getClueText()
	{
		return iCrossword.clueAt(new Point(iCursorX,iCursorY), iCursorDirection).iText;
	}
	
	public String getCluePattern()
	{
		return iCrossword.getCluePattern(new Point(iCursorX,iCursorY),iCursorDirection).replace('*','.');
	}
	
	public void enterWord(String aWord)
	{
		if (iCrossword.enterWord(new Point(iCursorX,iCursorY), iCursorDirection, aWord))
		{
			iGridView.invalidate();
		}
	}

}
