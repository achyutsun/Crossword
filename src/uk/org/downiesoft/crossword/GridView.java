package uk.org.downiesoft.crossword;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
//import android.util.Log;

public class GridView extends View
{

	private static final String TAG="uk.org.downiesoft.crossword.GridView";
	private static final int HIGHLIGHT = 0xffffa060;
	private static final int HIGHLIGHT_ALT = 0xffa0c8ff;

	public interface GridViewListener
	{
		public void onClueSelected(Clue aClue, int aCursorX, int aCursorY, int aCursorDirection);
	}

	private Context iContext;
	private Paint iPaint;
	private int iWidth;
	private int iHeight;
	private int iSize;
	private int iMargin;
	private int iDirection = CrosswordModel.CLUE_ACROSS;
	private Point iCursor;
	private Point iAnchor;
	private Rect iExtent;
	private Clue iClue;
	private CrosswordModel iCrossword;
	private GridViewListener iObserver;
	private Rect r = new Rect();
	private Rect iBounds = new Rect();
	private int mTextSize;
	private int mNumberSize;

	public GridView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		iContext=context;
		iPaint = new Paint();
		iCursor = new Point();
		iExtent = new Rect();
	}

	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		mTextSize=(3*w)/(CrosswordModel.GRID_SIZE*4);
		mNumberSize=(5*w)/(CrosswordModel.GRID_SIZE*16);
		Rect textBounds = new Rect();
		iPaint.getTextBounds("W", 0, 1, textBounds);
		iSize = textBounds.width() + 5;
		if (h>w)
		{
			iWidth = w;
			iHeight = iWidth;
		}
		else
		{
			iWidth = h * h / w;
			iHeight = h;
		}
		iSize = iWidth / CrosswordModel.GRID_SIZE;
		iMargin = (iWidth - iSize * CrosswordModel.GRID_SIZE) / 2;
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int iWidth = MeasureSpec.getSize(widthMeasureSpec);
		int iHeight = MeasureSpec.getSize(heightMeasureSpec);
		int min=Math.min(iWidth,iHeight);
		setMeasuredDimension(min, min);
		iSize = min / CrosswordModel.GRID_SIZE;
		iMargin = (iWidth - iSize * CrosswordModel.GRID_SIZE) / 2;
		iWidth=min;
		iHeight=min;
	}

	@Override
	public int getMinimumWidth()
	{
		return iWidth;
	}

	@Override
	public int getMinimumHeight()
	{
		return iWidth;
	}

	private void drawNumber(int col, int row, Canvas canvas) {
		Point pos=new Point(col, row);
		boolean across=(col == 0 || iCrossword.isBlank(col - 1, row))
			&& col < CrosswordModel.GRID_SIZE - 1 && !iCrossword.isBlank(col + 1, row);
		boolean down=(row == 0 || iCrossword.isBlank(col, row - 1))
			&& row < CrosswordModel.GRID_SIZE - 1 && !iCrossword.isBlank(col, row + 1);
		int direction=across ?CrosswordModel.CLUE_ACROSS: (down ?CrosswordModel.CLUE_DOWN: -1);
		if (direction != -1) {
			r.set(iMargin + col * iSize, iMargin + row * iSize, iMargin + (col + 1) * iSize, iMargin + (row + 1) * iSize);
			Clue clue=iCrossword.clueAt(pos, direction);
			if (clue!=null) {
				canvas.drawText(Integer.toString(clue.number()), r.left + 1, r.top + 1 + iBounds.height(), iPaint);
			}
		}
		
	}
	
	private void drawNumbers(Canvas canvas) {
		iPaint.setTypeface(Typeface.DEFAULT_BOLD);
		iPaint.setTextSize(mNumberSize);
		iPaint.setTextAlign(Align.LEFT);
		iPaint.getTextBounds("88", 0, 2, iBounds);
		iPaint.setColor(Color.BLACK);
		for (int col=0; col < CrosswordModel.GRID_SIZE; col++) {
			for (int row=0; row < CrosswordModel.GRID_SIZE; row++) {
				drawNumber(col,row,canvas);
			}
		}
	}

	private void drawLetter(int col, int row, Canvas canvas) {
		if (!iCrossword.isBlank(col, row))
		{
			r.set(iMargin + col * iSize, iMargin + row * iSize, iMargin + (col + 1) * iSize, iMargin + (row + 1) * iSize);
			int value = iCrossword.value(col, row);
			int solution = iCrossword.solution(col, row);
			String letter=" ";
			if (value!=CrosswordModel.SQUARE_NONBLANK)
			{
				if (solution==CrosswordModel.SQUARE_BLANK || solution==value)
					iPaint.setColor(Color.BLACK);
				else
					iPaint.setColor(Color.RED);
				letter=Character.toString((char)value);
			}
			else if (solution!=CrosswordModel.SQUARE_NONBLANK)
			{
				iPaint.setColor(Color.GREEN);
				letter=Character.toString((char)solution);
			}
			canvas.drawText(letter, r.centerX(), r.centerY() + iBounds.height()/2f, iPaint);
		}		
	}
	
	private void drawLetters(Canvas canvas) {
		iPaint.setTypeface(Typeface.DEFAULT_BOLD);
		iPaint.setTextSize(mTextSize);
		iPaint.setTextAlign(Align.CENTER);
		iPaint.getTextBounds("W", 0, 1, iBounds);
		iPaint.setStyle(Style.STROKE);
		for (int col=0; col < CrosswordModel.GRID_SIZE; col++) {
			for (int row=0; row < CrosswordModel.GRID_SIZE; row++) {
				drawLetter(col,row,canvas);
			}
		}		
	}
	
	private void drawSquare(int col, int row, int color, Canvas canvas) {
		r.set(iMargin + col * iSize, iMargin + row * iSize, iMargin + (col + 1) * iSize, iMargin + (row + 1) * iSize);
		iPaint.setStrokeWidth(0);
		iPaint.setStyle(Style.FILL);
		iPaint.setColor(iCrossword.isBlank(col, row) ? Color.BLACK : color);
		canvas.drawRect(r, iPaint);
		iPaint.setStyle(Style.STROKE);
		iPaint.setColor(Color.BLACK);
		canvas.drawRect(r, iPaint);
		iPaint.setStyle(Style.FILL_AND_STROKE);		
	}
	
	private void drawSquares(Canvas canvas) {
		for (int col=0; col < CrosswordModel.GRID_SIZE; col++) {
			for (int row=0; row < CrosswordModel.GRID_SIZE; row++) {
				if (col >= 0 && row >= 0 && col < CrosswordModel.GRID_SIZE && row < CrosswordModel.GRID_SIZE) {
					int color=iCrossword.isBlank(col, row) ? Color.BLACK : Color.WHITE;
					drawSquare(col,row,color,canvas);
				}
			}
		}
	}
	
	@Override
	public void onDraw(Canvas canvas)
	{
		long start=System.currentTimeMillis();
		//Log.d(TAG,String.format("onDraw start"));
		iPaint.reset();
		if (iCrossword == null)
		{
			String noFiles=iContext.getString(R.string.text_no_crossword);
			canvas.drawColor(Color.BLACK);
			iPaint.setTypeface(Typeface.DEFAULT);
			iPaint.setTextSize(50f);
			iPaint.setTextAlign(Align.CENTER);
			iPaint.getTextBounds(noFiles, 0, 1, iBounds);
			canvas.drawText(noFiles, iWidth / 2, (iHeight - iBounds.height()) / 2, iPaint);
		}
		else
		{
			drawSquares(canvas);
			drawNumbers(canvas);
			drawLetters(canvas);
		}
		highlightCurrentClue(true, canvas);
		//Log.d(TAG,String.format("onDraw finish %d millisecs",System.currentTimeMillis()-start));
	}

	public Clue getCurrentClue()
	{
		return iClue;
	}
	
	public void setObserver(GridViewListener aObserver)
	{
		iObserver = aObserver;
	}

	public void setCrossword(CrosswordModel aCrossword)
	{
		iCrossword = aCrossword;
		//if (iCrossword!=null && iCrossword.isValid())
		//	setCursor(0,0,CrosswordModel.CLUE_ACROSS);
		invalidate();
	}

	public int getCursorX()
	{		
		return iCursor.x;
	}
	
	public int getCursorY()
	{		
		return iCursor.y;
	}
	
	public int getCursorDirection()
	{		
		return iDirection;
	}
	
	public void setCursor(int aX, int aY, int aDirection, boolean aNotifyListener)
	{
		iCursor.set(aX, aY);
		iDirection = aDirection;
		Point delta = new Point(1 - aDirection, aDirection);
		while (iCrossword.withinGrid(iCursor) && iCrossword.isBlank(iCursor))
		{
			iCursor.x += delta.x;
			iCursor.y += delta.y;
		}
		if (!iCrossword.withinGrid(iCursor))
		{
			do
			{
				iCursor.x -= delta.x;
				iCursor.y -= delta.y;
			} while (iCrossword.withinGrid(iCursor) && iCrossword.isBlank(iCursor));
		}
		iClue = iCrossword.clueAt(iCursor, iDirection);
		iExtent = iCrossword.getClueExtent(iCursor, iDirection);
		if (iExtent.width() ==0 && iExtent.height() ==0)
		{
			iDirection = 1 - iDirection;
			iExtent = iCrossword.getClueExtent(iCursor, iDirection);
		}
		if (iClue!=null && iObserver!=null && aNotifyListener)
		{
			iObserver.onClueSelected(iClue,iCursor.x,iCursor.y,iDirection);
		}
		invalidate();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		float x = event.getX();
		float y = event.getY();
		int col = (int) ((x - (float) iMargin) / (float) iSize);
		int row = (int) ((y - (float) iMargin) / (float) iSize);
		Point pos = new Point(col, row);
		Rect extent;
		boolean invalidated = false;
		switch (event.getAction())
		{
			case MotionEvent.ACTION_DOWN:
			{
				iAnchor=pos;
				return true;
			}
			case MotionEvent.ACTION_UP:
			{
				if (iCrossword.withinGrid(pos) && !iCrossword.isBlank(pos))
				{
					if (iCursor.equals(col, row))
					{
						extent = iCrossword.getClueExtent(pos, 1 - iDirection);
						if (extent.width() > 0 || extent.height() > 0)
						{
							iDirection = 1 - iDirection;
							invalidated = true;
						}
						else
							return true;
					}
					else 
					{
						int direction=iDirection;
						if (Math.abs(col-iAnchor.x)>Math.abs(row-iAnchor.y))
							direction=CrosswordModel.CLUE_ACROSS;
						else if (Math.abs(col-iAnchor.x)<Math.abs(row-iAnchor.y))
							direction=CrosswordModel.CLUE_DOWN;
						extent = iCrossword.getClueExtent(pos, direction);
						switch (direction)
						{
							case CrosswordModel.CLUE_ACROSS:
								if (extent.width() == 0)
									direction = CrosswordModel.CLUE_DOWN;
								invalidated = true;
								break;
							case CrosswordModel.CLUE_DOWN:
								if (extent.height() == 0)
									direction = CrosswordModel.CLUE_ACROSS;
								invalidated = true;
								break;
							default:
								break;
						}
						extent = iCrossword.getClueExtent(pos, direction);
						iDirection = direction;
					}
					iCursor = pos;
					iExtent.set(extent);
					iClue = iCrossword.clueAt(pos, iDirection);
					if (iObserver != null)
					{
						iObserver.onClueSelected(iClue,iCursor.x,iCursor.y,iDirection);
					}
					if (invalidated)
						invalidate();
				}
				return true;
			}
			default:
				return true;
		}
	}

	private void highlightCurrentClue(boolean aOn, Canvas aCanvas)
	{
		if (iClue == null)
			return;
		iPaint.setTypeface(Typeface.DEFAULT_BOLD);
		iPaint.setTextSize(mTextSize);
		iPaint.setTextAlign(Align.CENTER);
		iPaint.getTextBounds("W", 0, 1, iBounds);
		Point delta = new Point(1 - iDirection, iDirection);
		int pos = iCursor.x - iExtent.left + iCursor.y - iExtent.top;
		int len = iExtent.right - iExtent.left + iExtent.bottom - iExtent.top + 1;
		int word = 0;
		int wordlen = iClue.wordLength(word);
		if (wordlen == -1)
			wordlen = len;
		while (pos >= wordlen)
		{
			word++;
			wordlen += iClue.wordLength(word);
		}
		int colour = 0;
		word = 0;
		wordlen = iClue.wordLength(word);
		if (wordlen == -1)
			wordlen = len;
		Point sq = new Point(iExtent.left, iExtent.top);
		for (int i = 0; i < len; i++)
		{
			drawSquare(sq.x, sq.y, aOn ? (colour != 0 ? HIGHLIGHT_ALT : HIGHLIGHT) : Color.WHITE, aCanvas);
			iPaint.setTypeface(Typeface.DEFAULT_BOLD);
			iPaint.setTextSize(mTextSize);
			iPaint.setTextAlign(Align.CENTER);
			iPaint.getTextBounds("W", 0, 1, iBounds);
			iPaint.setStyle(Style.STROKE);
			drawLetter(sq.x, sq.y, aCanvas);
			iPaint.setTypeface(Typeface.DEFAULT_BOLD);
			iPaint.setTextSize(mNumberSize);
			iPaint.setTextAlign(Align.LEFT);
			iPaint.getTextBounds("88", 0, 2, iBounds);
			iPaint.setColor(Color.BLACK);
			drawNumber(sq.x,sq.y,aCanvas);
			sq.x += delta.x;
			sq.y += delta.y;
			if (--wordlen == 0)
			{
				word++;
				wordlen = iClue.wordLength(word);
				colour ^= 1;
			}
		}
		
	}

	public void dumpGrid()
	{
		iCrossword.dumpGrid(TAG);
	}
}
