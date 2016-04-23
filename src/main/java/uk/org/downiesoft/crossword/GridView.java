package uk.org.downiesoft.crossword;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
//import android.util.Log;

public class GridView extends View {

	private static final String TAG="uk.org.downiesoft.crossword.GridView";

	public interface GridViewListener {
		public void onGridClueSelected(GridClueContext aClueContext);
		public void onGridClueLongPress(GridClueContext aClueContext);
	}

	private final Context iContext;
	private final Paint iPaint;
	private final Paint mLetterPaint;
	private final Paint mNumberPaint;
	private int iWidth;
	private int iHeight;
	private int iVSize;
	private int iHSize;
	private int iHMargin;
	private int iVMargin;
	private final GridClueContext mCursor;
	private GridClueContext mPressedCursor;
	private Point iAnchor;
	private PointF iTouchAnchor;
	private CrosswordModel iCrossword;
	private GridViewListener iObserver;
	private final Rect r = new Rect();
	private final Rect iNumberBounds = new Rect();
	private final Rect iLetterBounds = new Rect();
	private float mTextSize;
	private float mNumberSize;
	private Bitmap mBackBmp;
	private Canvas mBackCanvas;
	private Matrix mMatrix;
	private final int mHighlight;
	private final int mHighlightAlt;
	private final int mPressed;
	private final int mPressedAlt;
	private final GestureDetector mGestureDetector;
	private boolean mLongPressTriggered;
	private boolean mDoubleTapTriggered;
	private final CluePressRunnable mCluePressRunnable = new CluePressRunnable();

	private class CluePressRunnable implements Runnable {

		@Override
		public void run() {
			GridView.this.getHandler().removeCallbacks(this);
			MainActivity.debug(1,TAG, String.format(">CluePressRunnable: %s %s", mPressedCursor, mCursor));
			if (mPressedCursor != null) {
				highlightClue(mPressedCursor,mPressedCursor.equals(mCursor),false);
				highlightCurrentClue(true);
			}
		}
	
	}
	
	public GridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		iContext = context;
		iPaint = new Paint();
		mLetterPaint = new Paint();
		mNumberPaint = new Paint();
		mCursor = new GridClueContext();
		mHighlight = context.getResources().getColor(R.color.grid_highlight);
		mHighlightAlt = context.getResources().getColor(R.color.grid_highlight_alt);
		mPressed = context.getResources().getColor(R.color.grid_pressed);
		mPressedAlt = context.getResources().getColor(R.color.grid_pressed_alt);
		mGestureDetector = new GestureDetector(context, new GridViewOnGestureListener());
	}

	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		mTextSize = (3 * w) / (CrosswordModel.GRID_SIZE * 4);
		mNumberSize = (3 * w) / (CrosswordModel.GRID_SIZE * 11);
		if (mTextSize < 50 ) {
			mTextSize = (5 * w) / (CrosswordModel.GRID_SIZE * 6);			
			mNumberSize = 0;
		}
		Rect textBounds = new Rect();
		iPaint.getTextBounds("W", 0, 1, textBounds);
		iWidth = w;
		iHeight = h;

		iHSize = iWidth / CrosswordModel.GRID_SIZE;
		iVSize = iHeight / CrosswordModel.GRID_SIZE;
		if (iHSize*4 > iVSize*5) {
			iHSize = iVSize * 5 / 4;
		}
		if (iVSize*4 > iHSize*5) {
			iVSize = iHSize * 5 / 4;
		}
		iHMargin = (iWidth - iHSize * CrosswordModel.GRID_SIZE) / 2;
		iVMargin = (iHeight - iVSize * CrosswordModel.GRID_SIZE) / 2;
		mBackBmp = Bitmap.createBitmap(iHSize*CrosswordModel.GRID_SIZE, iVSize*CrosswordModel.GRID_SIZE, Bitmap.Config.ARGB_8888);
		mBackCanvas = new Canvas(mBackBmp);
		mMatrix = new Matrix();
		mMatrix.postTranslate(iHMargin, iVMargin);

		mLetterPaint.setTypeface(Typeface.DEFAULT_BOLD);
		mLetterPaint.setTextSize(mTextSize);
		mLetterPaint.setTextAlign(Align.CENTER);
		mLetterPaint.getTextBounds("W", 0, 1, iLetterBounds);
		mLetterPaint.setStyle(Style.STROKE);
		mLetterPaint.setAntiAlias(true);
		mLetterPaint.setLinearText(true);
		mLetterPaint.setSubpixelText(true);

		if (mNumberSize>0) {
			mNumberPaint.setTypeface(Typeface.DEFAULT_BOLD);
			mNumberPaint.setTextSize(mNumberSize);
			mNumberPaint.setTextAlign(Align.LEFT);
			mNumberPaint.getTextBounds("88", 0, 2, iNumberBounds);
			mNumberPaint.setColor(Color.BLACK);
			mNumberPaint.setAntiAlias(true);
			mNumberPaint.setLinearText(true);
			mNumberPaint.setSubpixelText(true);
		}
		MainActivity.debug(1, TAG,String.format("onSizeChanged(%s,%s) = (%s,%s) (%s,%s) (%s,%s) %s %s", w, h, iWidth, iHeight, iHSize, iVSize, iHMargin, iVMargin, mTextSize, mNumberSize));
		redraw();
		invalidate();
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int wmode = MeasureSpec.getMode(widthMeasureSpec);
		int hmode = MeasureSpec.getMode(heightMeasureSpec);
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);
		switch (wmode) {
			case MeasureSpec.EXACTLY:
				switch (hmode) {
					case MeasureSpec.EXACTLY:
						break;
					case MeasureSpec.UNSPECIFIED:
					case MeasureSpec.AT_MOST:
						break;
				}
				break;
			case MeasureSpec.UNSPECIFIED:
				switch (hmode) {
					case MeasureSpec.EXACTLY:
						width = height;
						break;
					case MeasureSpec.UNSPECIFIED:
						break;
					case MeasureSpec.AT_MOST:
						width = height;
				}
				break;
			case MeasureSpec.AT_MOST:
				switch (hmode) {
					case MeasureSpec.EXACTLY:

						break;
					case MeasureSpec.UNSPECIFIED:
						height = width;
						break;
					case MeasureSpec.AT_MOST:
						break;
				}
				break;
		}
		setMeasuredDimension(width, height);			
		MainActivity.debug(1,TAG, String.format("onMeasure(%s,%s)=(%s,%s)",MeasureSpec.toString(widthMeasureSpec),MeasureSpec.toString(heightMeasureSpec),width,height));
	}

	@Override
	public int getMinimumWidth() {
		return iWidth;
	}

	@Override
	public int getMinimumHeight() {
		return iWidth;
	}

	private void drawNumber(int col, int row, Canvas canvas) {
		if (mNumberSize > 0) {
			Point pos=new Point(col, row);
			boolean across=(col == 0 || iCrossword.isBlank(col - 1, row))
				&& col < CrosswordModel.GRID_SIZE - 1 && !iCrossword.isBlank(col + 1, row);
			boolean down=(row == 0 || iCrossword.isBlank(col, row - 1))
				&& row < CrosswordModel.GRID_SIZE - 1 && !iCrossword.isBlank(col, row + 1);
			int direction=across ?CrosswordModel.CLUE_ACROSS: (down ?CrosswordModel.CLUE_DOWN: -1);
			if (direction != -1) {
				r.set(col * iHSize, row * iVSize, (col + 1) * iHSize, (row + 1) * iVSize);
				Clue clue=iCrossword.clueAt(pos, direction);
				if (clue != null) {
					canvas.drawText(Integer.toString(clue.number()), r.left + 1, r.top + 1 + iNumberBounds.height(), mNumberPaint);
				}
			}
		}
	}

	private void drawNumbers(Canvas canvas) {
		for (int col=0; col < CrosswordModel.GRID_SIZE; col++) {
			for (int row=0; row < CrosswordModel.GRID_SIZE; row++) {
				drawNumber(col, row, canvas);
			}
		}
	}

	private void drawLetter(int col, int row, Canvas canvas) {
		if (!iCrossword.isBlank(col, row)) {
			r.set(col * iHSize, row * iVSize, (col + 1) * iHSize, (row + 1) * iVSize);
			int value = iCrossword.value(col, row);
			int solution = iCrossword.solution(col, row);
			String letter=" ";
			if (value != CrosswordModel.SQUARE_NONBLANK) {
				if (solution == CrosswordModel.SQUARE_BLANK || solution == value)
					mLetterPaint.setColor(Color.BLACK);
				else
					mLetterPaint.setColor(Color.RED);
				letter = Character.toString((char)value);
			} else if (solution != CrosswordModel.SQUARE_NONBLANK) {
				mLetterPaint.setColor(Color.GREEN);
				letter = Character.toString((char)solution);
			}
			mLetterPaint.setStyle(Paint.Style.FILL_AND_STROKE);
			canvas.drawText(letter, r.centerX(), r.centerY() + iLetterBounds.height() / 2f, mLetterPaint);
		}		
	}

	private void drawLetters(Canvas canvas) {
		for (int col=0; col < CrosswordModel.GRID_SIZE; col++) {
			for (int row=0; row < CrosswordModel.GRID_SIZE; row++) {
				drawLetter(col, row, canvas);
			}
		}		
	}

	private void drawSquare(int col, int row, int color, Canvas canvas) {
		r.set(col * iHSize, row * iVSize, (col + 1) * iHSize, (row + 1) * iVSize);
		iPaint.setStrokeWidth(0);
		iPaint.setStyle(Style.FILL);
		iPaint.setColor(iCrossword.isBlank(col, row) ? Color.BLACK : color);
		canvas.drawRect(r, iPaint);
		iPaint.setStyle(Style.STROKE);
		iPaint.setColor(Color.BLACK);
		canvas.drawRect(r, iPaint);
		iPaint.setStyle(Style.FILL_AND_STROKE);		
	}

	private void drawSquare(int col, int row, Canvas canvas) {
		int color=iCrossword.isBlank(col, row) ? Color.BLACK : Color.WHITE;
		drawSquare(col, row, color, canvas);
	}

	private void drawSquares(Canvas canvas) {
		for (int col=0; col < CrosswordModel.GRID_SIZE; col++) {
			for (int row=0; row < CrosswordModel.GRID_SIZE; row++) {
				if (col >= 0 && row >= 0 && col < CrosswordModel.GRID_SIZE && row < CrosswordModel.GRID_SIZE) {
					drawSquare(col, row, canvas);
				}
			}
		}
	}

	@Override
	public void onDraw(Canvas canvas) {
		iPaint.reset();
		canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);
		canvas.drawBitmap(mBackBmp, mMatrix, iPaint);
	}

	public void redraw() {
		MainActivity.debug(1, TAG,String.format("redraw start %s",iCrossword));
		if (mBackCanvas != null) {
			iPaint.reset();
			if (iCrossword == null) {
				String noFiles=iContext.getString(R.string.text_no_crossword);
				mBackCanvas.drawColor(Color.BLACK);
				iPaint.setTypeface(Typeface.DEFAULT);
				iPaint.setTextSize(50f);
				iPaint.setTextAlign(Align.CENTER);
				Rect bounds=new Rect();
				iPaint.getTextBounds(noFiles, 0, 1, bounds);
				mBackCanvas.drawText(noFiles, iWidth / 2, (iHeight - bounds.height()) / 2, iPaint);
			} else {
				drawSquares(mBackCanvas);
				drawNumbers(mBackCanvas);
				drawLetters(mBackCanvas);
			}
			highlightCurrentClue(true, false);		
		} else {
			MainActivity.debug(1, TAG, "redraw - no back canvas");
		}
	}

	public Clue getCurrentClue() {
		return mCursor.getClue();
	}

	public void setObserver(GridViewListener aObserver) {
		iObserver = aObserver;
	}

	public void setCrossword(CrosswordModel aCrossword) {
		MainActivity.debug(1, TAG,String.format("setCrossword(%s): %s",aCrossword, this));
		iCrossword = aCrossword;
		redraw();
		invalidate();
	}

	public int getCursorX() {		
		return mCursor.getPosition().x;
	}

	public int getCursorY() {		
		return mCursor.getPosition().y;
	}

	public int getCursorDirection() {		
		return mCursor.getDirection();
	}

	public void setCursor(int aX, int aY, int aDirection, boolean aNotifyListener) {
		if (mCursor.getPosition().x == aX && mCursor.getPosition().y == aY && mCursor.getDirection() == aDirection && mCursor.getClue() != null) {
			mCursor.setExtent(iCrossword.getClueExtent(mCursor.getPosition(), mCursor.getDirection()));
			highlightCurrentClue(true, false);
		} else {
			highlightCurrentClue(false, false);
			mCursor.setPosition(aX, aY);
			mCursor.setDirection(aDirection);
			Point delta = new Point(1 - aDirection, aDirection);
			while (iCrossword.withinGrid(mCursor.getPosition()) && iCrossword.isBlank(mCursor.getPosition())) {
				mCursor.getPosition().offset(delta.x,delta.y);
			}
			if (!iCrossword.withinGrid(mCursor.getPosition())) {
				do
				{
					mCursor.getPosition().offset(-delta.x,-delta.y);
				} while (iCrossword.withinGrid(mCursor.getPosition()) && iCrossword.isBlank(mCursor.getPosition()));
			}
			mCursor.setClue(iCrossword.clueAt(mCursor.getPosition(), mCursor.getDirection()));
			mCursor.setExtent(iCrossword.getClueExtent(mCursor.getPosition(), mCursor.getDirection()));
			if (mCursor.getExtent().width() == 0 && mCursor.getExtent().height() == 0) {
				mCursor.setDirection(1 - mCursor.getDirection());
				mCursor.setExtent(iCrossword.getClueExtent(mCursor.getPosition(), mCursor.getDirection()));
			}
			if (mCursor.getClue() != null && iObserver != null && aNotifyListener) {
				iObserver.onGridClueSelected(mCursor);
			}
			highlightCurrentClue(true, false);
		}
		redraw();
		MainActivity.debug(1, TAG, String.format("<setCursor(%s,%s,%s) %s", mCursor.getPosition().x, mCursor.getPosition().y, mCursor.getDirection(), mCursor.getClue()));

		invalidate();
	}

	private Point getEventCoords(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();
		float[] points={ x, y };
		Matrix inverse=new Matrix();
		mMatrix.invert(inverse);
		inverse.mapPoints(points);
		Point pos = new Point((int) (points[0] / (float) iHSize), (int) (points[1] / (float) iVSize));
		MainActivity.debug(2, TAG, String.format("getEventCoords: (%3.1f,%3.1f),(%3.1f,%3.1f),%s,%s", x, y, points[0], points[1], pos, mLongPressTriggered));
		return pos;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mGestureDetector.onTouchEvent(event)) {
			return true;
		}
		Point pos = getEventCoords(event);
		Rect extent;
		int direction = mCursor.getDirection();
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				iAnchor = new Point(pos);
				MainActivity.debug(1, TAG, String.format(">onTouchEvent: ACTION_DOWN %s %s %s)",pos, iCrossword.withinGrid(iAnchor), !iCrossword.isBlank(pos)));
				iTouchAnchor = new PointF(event.getX(), event.getY());
				mLongPressTriggered = false;
				mDoubleTapTriggered = false;
				if (iCrossword.withinGrid(iAnchor) && !iCrossword.isBlank(pos)) {
					highlightCurrentClue(false, false);
					extent = iCrossword.getClueExtent(pos, direction);
					switch (direction) {
						case CrosswordModel.CLUE_ACROSS:
							if (extent.width() == 0)
								direction = CrosswordModel.CLUE_DOWN;
							break;
						case CrosswordModel.CLUE_DOWN:
							if (extent.height() == 0)
								direction = CrosswordModel.CLUE_ACROSS;
							break;
						default:
							break;
					}
					extent = iCrossword.getClueExtent(pos, direction);
					if (extent.width() > 0 || extent.height() > 0) {
						mPressedCursor = new GridClueContext(iAnchor, direction, extent, iCrossword.clueAt(pos, direction));
						highlightClue(mPressedCursor, true, true);
						invalidate();					
					} else {
						mPressedCursor = null;
						highlightCurrentClue(true);
						invalidate();
					}
				}
				MainActivity.debug(1, TAG, String.format("<onTouchEvent: ACTION_DOWN %s %s %s)",pos, iCrossword.withinGrid(iAnchor), !iCrossword.isBlank(pos)));
				return true;
			case MotionEvent.ACTION_MOVE:
				return false;
			case MotionEvent.ACTION_UP:
				MainActivity.debug(1, TAG, String.format(">onTouchEvent: ACTION_UP %s, %s",pos,mCursor));
				if (mPressedCursor != null) {
					getHandler().removeCallbacks(mCluePressRunnable);
					getHandler().postDelayed(mCluePressRunnable,150);
				}
				if (!mLongPressTriggered && !mDoubleTapTriggered && iCrossword.withinGrid(pos) && !iCrossword.isBlank(pos)) {
					if (Math.abs(pos.x - iAnchor.x) > 5 * Math.abs(pos.y - iAnchor.y)) {
						direction = CrosswordModel.CLUE_ACROSS;
					} else if (5 * Math.abs(pos.x - iAnchor.x) < Math.abs(pos.y - iAnchor.y)) {
						direction = CrosswordModel.CLUE_DOWN;
					}
					extent = iCrossword.getClueExtent(pos, direction);
					MainActivity.debug(1, TAG, String.format("onTouchEvent: %s %s)", extent, direction));
					switch (direction) {
						case CrosswordModel.CLUE_ACROSS:
							if (extent.width() == 0)
								direction = CrosswordModel.CLUE_DOWN;
							break;
						case CrosswordModel.CLUE_DOWN:
							if (extent.height() == 0)
								direction = CrosswordModel.CLUE_ACROSS;
							break;
						default:
							break;
					}
					extent = iCrossword.getClueExtent(pos, direction);
					if (direction != mCursor.getDirection()) {
						// change of direction
						mCursor.setCursor(pos, direction);
						mCursor.setExtent(extent);
						mCursor.setClue(iCrossword.clueAt(pos, mCursor.getDirection()));
						if (mPressedCursor != null) {
							highlightClue(mPressedCursor,false,false);
							highlightCurrentClue(true,true);
							GridView.this.getHandler().removeCallbacks(mCluePressRunnable);
							mPressedCursor.setContext(mCursor);
							GridView.this.getHandler().postDelayed(mCluePressRunnable,150);
						}
						invalidate();						
					} else if (mPressedCursor == null || mPressedCursor.getExtent().equals(extent)) {
						mCursor.setPosition(pos);
						mCursor.setExtent(extent);
						mCursor.setClue(iCrossword.clueAt(pos, mCursor.getDirection()));
						invalidate();						
					} else {
						mCursor.setContext(mPressedCursor);
						invalidate();						
					}
					if (iObserver != null) {
						iObserver.onGridClueSelected(mCursor);
					}
				} else if (mLongPressTriggered) {
					if (mPressedCursor != null && !mCursor.equals(mPressedCursor)) {
						mCursor.setContext(mPressedCursor);
						if (iObserver != null) {
							iObserver.onGridClueSelected(mCursor);
						}
					}
					highlightCurrentClue(true);
					invalidate();
				} else {
					highlightCurrentClue(true);
					invalidate();
				}
				MainActivity.debug(1, TAG, String.format("<onTouchEvent: ACTION_UP %s, %s",pos,mCursor));
				return true;
		}
		return false;
	}

	public void highlightCurrentClue(boolean aOn) {
		highlightCurrentClue(aOn, false);
		invalidate();
	}

	private void highlightCurrentClue(boolean aOn, boolean aPressed) {
		highlightClue(mCursor, aOn, aPressed);
	}
	
	private void highlightClue(GridClueContext aCursor, boolean aOn, boolean aPressed) {
		MainActivity.debug(1,TAG, String.format("highlightClue(%s, %s, %s)", aCursor, aOn, aPressed));
		if (aCursor.getClue() == null || mBackCanvas == null)
			return;
		Point delta = new Point(1 - aCursor.getDirection(), aCursor.getDirection());
		int pos = aCursor.getPosition().x - aCursor.getExtent().left + aCursor.getPosition().y - aCursor.getExtent().top;
		int len = aCursor.getExtent().right - aCursor.getExtent().left + aCursor.getExtent().bottom - aCursor.getExtent().top + 1;
		int word = 0;
		int wordlen = aCursor.getClue().wordLength(word);
		if (wordlen == -1)
			wordlen = len;
		while (pos >= wordlen) {
			word++;
			wordlen += aCursor.getClue().wordLength(word);
		}
		int colour = 0;
		word = 0;
		wordlen = aCursor.getClue().wordLength(word);
		if (wordlen == -1)
			wordlen = len;
		Point sq = new Point(aCursor.getExtent().left, aCursor.getExtent().top);
		int hi = aPressed?mPressed:mHighlight;
		int alt = aPressed?mPressedAlt:mHighlightAlt;
		for (int i = 0; i < len; i++) {
			drawSquare(sq.x, sq.y, aOn ? (colour != 0 ? alt : hi) : Color.WHITE, mBackCanvas);
			drawLetter(sq.x, sq.y, mBackCanvas);
			drawNumber(sq.x, sq.y, mBackCanvas);
			sq.x += delta.x;
			sq.y += delta.y;
			if (--wordlen == 0) {
				word++;
				wordlen = aCursor.getClue().wordLength(word);
				colour ^= 1;
			}
		}

	}

	public void dumpGrid() {
		iCrossword.dumpGrid(TAG);
	}

	private class GridViewOnGestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public void onLongPress(MotionEvent e) {
			Point pos = getEventCoords(e);
			if (iCrossword.withinGrid(pos) && !iCrossword.isBlank(pos)) {
				if (mPressedCursor != null && !mCursor.equals(mPressedCursor)) {
					mCursor.setContext(mPressedCursor);
					if (iObserver != null) {
						iObserver.onGridClueSelected(mCursor);
					}
				}
				highlightCurrentClue(true);
				invalidate();
				iObserver.onGridClueLongPress(mPressedCursor);
				mLongPressTriggered = true;
			}

		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			Point pos = getEventCoords(e);
			MainActivity.debug(1,TAG, String.format(">onDoubleTap(%s): %s %s",pos, mCursor, iAnchor));
			if (iCrossword.withinGrid(pos) && !iCrossword.isBlank(pos) && pos.equals(iAnchor)) {
				Clue clue = iCrossword.clueAt(pos, 1 - mCursor.getDirection());
				Rect extent = iCrossword.getClueExtent(pos, 1 - mCursor.getDirection());
				if (extent.width() > 0 || extent.height() > 0) {
					mCursor.setCursor(pos, 1 - mCursor.getDirection());
					mCursor.setExtent(extent);
					mCursor.setClue(clue);
					if (mPressedCursor != null) {
						highlightClue(mPressedCursor,false,false);
						highlightCurrentClue(true,true);
						GridView.this.getHandler().removeCallbacks(mCluePressRunnable);
						mPressedCursor.setContext(mCursor);
						GridView.this.getHandler().postDelayed(mCluePressRunnable,150);
						if (iObserver != null) {
							iObserver.onGridClueSelected(mCursor);
						}
					}
					invalidate();
					mDoubleTapTriggered = true;
				}
				MainActivity.debug(1,TAG, String.format("<onDoubleTap(%s): %s %s",pos, mCursor, iAnchor));
				return true;
			} 
			return false;
		}
		
		
	}
}
