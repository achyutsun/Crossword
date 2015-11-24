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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.ScaleGestureDetector;
import android.graphics.PointF;
import android.util.Log;
import android.util.DisplayMetrics;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
//import android.util.Log;

public class GridView extends View {

	private static final String TAG="uk.org.downiesoft.crossword.GridView";

	public interface GridViewListener {
		public void onGridClueSelected(Clue aClue, int aCursorX, int aCursorY, int aCursorDirection);
		public void onGridClueLongPress(Clue aClue, int aCursorX, int aCursorY, int aCursorDirection);
	}

	private Context iContext;
	private Paint iPaint;
	private Paint mLetterPaint;
	private Paint mNumberPaint;
	private int iWidth;
	private int iHeight;
	private int iVSize;
	private int iHSize;
	private int iHMargin;
	private int iVMargin;
	private int iDirection = CrosswordModel.CLUE_ACROSS;
	private Point iCursor;
	private Point iAnchor;
	private PointF iTouchAnchor;
	private Rect iExtent;
	private Clue iClue;
	private int iPressedDirection;
	private Rect iPressedExtent;
	private Clue iPressedClue;
	private CrosswordModel iCrossword;
	private GridViewListener iObserver;
	private Rect r = new Rect();
	private Rect iNumberBounds = new Rect();
	private Rect iLetterBounds = new Rect();
	private float mTextSize;
	private float mNumberSize;
	private Bitmap mBackBmp;
	private Canvas mBackCanvas;
	private Matrix mMatrix;
	private int mHighlight;
	private int mHighlightAlt;
	private int mPressed;
	private int mPressedAlt;
	private GestureDetector mGestureDetector;
	private boolean mLongPressTriggered;
	
	public GridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		iContext = context;
		iPaint = new Paint();
		mLetterPaint = new Paint();
		mNumberPaint = new Paint();
		iCursor = new Point();
		iExtent = new Rect();
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
//		if (h > w) {
//			iWidth = w;
//			iHeight = iWidth;
//		} else {
//			iWidth = w;
//			iHeight = h;
//		}

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
		canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);
		canvas.drawBitmap(mBackBmp, mMatrix, iPaint);
		//MainActivity.debug(1, TAG,String.format("onDraw finish %d millisecs",System.currentTimeMillis()-start));
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
			highlightCurrentClue(true, false, mBackCanvas);		
		}
	}

	public Clue getCurrentClue() {
		return iClue;
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
		return iCursor.x;
	}

	public int getCursorY() {		
		return iCursor.y;
	}

	public int getCursorDirection() {		
		return iDirection;
	}

	public void setCursor(int aX, int aY, int aDirection, boolean aNotifyListener) {
		if (iCursor.x == aX && iCursor.y == aY && iDirection == aDirection && iClue != null) {
			iExtent = iCrossword.getClueExtent(iCursor, iDirection);
			highlightCurrentClue(true, false, mBackCanvas);
		} else {
			highlightCurrentClue(false, false, mBackCanvas);
			iCursor.set(aX, aY);
			iDirection = aDirection;
			Point delta = new Point(1 - aDirection, aDirection);
			while (iCrossword.withinGrid(iCursor) && iCrossword.isBlank(iCursor)) {
				iCursor.x += delta.x;
				iCursor.y += delta.y;
			}
			if (!iCrossword.withinGrid(iCursor)) {
				do
				{
					iCursor.x -= delta.x;
					iCursor.y -= delta.y;
				} while (iCrossword.withinGrid(iCursor) && iCrossword.isBlank(iCursor));
			}
			iClue = iCrossword.clueAt(iCursor, iDirection);
			iExtent = iCrossword.getClueExtent(iCursor, iDirection);
			if (iExtent.width() == 0 && iExtent.height() == 0) {
				iDirection = 1 - iDirection;
				iExtent = iCrossword.getClueExtent(iCursor, iDirection);
			}
			if (iClue != null && iObserver != null && aNotifyListener) {
				iObserver.onGridClueSelected(iClue, iCursor.x, iCursor.y, iDirection);
			}
			highlightCurrentClue(true, false, mBackCanvas);
		}
		redraw();
		MainActivity.debug(1, TAG, String.format("<setCursor(%s,%s,%s) %s", iCursor.x, iCursor.y, iDirection, iClue));

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
		int direction = iDirection;
		boolean invalidated = false;
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				iAnchor = new Point(pos);
				MainActivity.debug(1, TAG, String.format(">onTouchEvent: ACTION_DOWN %s %s %s)",pos, iCrossword.withinGrid(iAnchor), !iCrossword.isBlank(pos)));
				iTouchAnchor = new PointF(event.getX(), event.getY());
				mLongPressTriggered = false;
				if (iCrossword.withinGrid(iAnchor) && !iCrossword.isBlank(pos)) {
					highlightCurrentClue(false, false, mBackCanvas);
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
						iPressedDirection = direction;
						iPressedExtent = new Rect(extent);
						iPressedClue = iCrossword.clueAt(pos, direction);
						highlightClue(iAnchor, direction, iPressedExtent, iPressedClue, true, true, mBackCanvas);
						invalidate();					
					} else {
						iPressedClue = null;
						iPressedExtent = null;
						highlightCurrentClue(true);
					}
				}
				MainActivity.debug(1, TAG, String.format("<onTouchEvent: ACTION_DOWN %s %s %s)",pos, iCrossword.withinGrid(iAnchor), !iCrossword.isBlank(pos)));
				return true;
			case MotionEvent.ACTION_MOVE:
				return false;
			case MotionEvent.ACTION_UP:
				MainActivity.debug(1, TAG, String.format("onTouchEvent: ACTION_UP %s)",pos));
				if (iPressedClue != null) {
					highlightClue(iAnchor, iPressedDirection, iPressedExtent, iPressedClue, false, true, mBackCanvas);
					iPressedClue = null;
				}
				extent = iCrossword.getClueExtent(pos, direction);
				if (!mLongPressTriggered && iCrossword.withinGrid(pos) && !iCrossword.isBlank(pos)) {
					if (iCursor.equals(pos) && iAnchor.equals(iCursor)) {
						extent = iCrossword.getClueExtent(pos, 1 - iDirection);
						if (extent.width() > 0 || extent.height() > 0) {
							highlightCurrentClue(false, false, mBackCanvas);
							iDirection = 1 - iDirection;
							invalidated = true;
						} else {
							extent = iCrossword.getClueExtent(pos, iDirection);							
						}
					} else {
						highlightCurrentClue(false, false, mBackCanvas);
						if (Math.abs(pos.x - iAnchor.x) > 5 * Math.abs(pos.y - iAnchor.y))
							direction = CrosswordModel.CLUE_ACROSS;
						else if (5 * Math.abs(pos.x - iAnchor.x) < Math.abs(pos.y - iAnchor.y))
							direction = CrosswordModel.CLUE_DOWN;
						MainActivity.debug(1, TAG, String.format("onTouchEvent: %s %s)",extent, direction));
						switch (direction) {
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
						MainActivity.debug(1, TAG, String.format("onTouchEvent: %s %s)",extent, direction));
						iDirection = direction;
					}
					iCursor = pos;
					iExtent.set(extent);
					iClue = iCrossword.clueAt(pos, iDirection);
					highlightCurrentClue(true, false, mBackCanvas);
					if (iObserver != null) {
						iObserver.onGridClueSelected(iClue, iCursor.x, iCursor.y, iDirection);
					}
					if (invalidated)
						invalidate();
				} else if (mLongPressTriggered) {
					iCursor.set(iAnchor.x, iAnchor.y);
					iExtent.set(iPressedExtent);
					iDirection = iPressedDirection;
					if (!iClue.equals(iPressedClue)) {
						iClue = iPressedClue;
						if (iObserver != null) {
							iObserver.onGridClueSelected(iClue, iCursor.x, iCursor.y, iDirection);
						}
					}
					highlightCurrentClue(true);
					invalidate();
				} else {
					highlightCurrentClue(true);
					invalidate();
				}
				return true;
		}
		return false;
	}

	public void highlightCurrentClue(boolean aOn) {
		highlightCurrentClue(aOn, false, mBackCanvas);
		invalidate();
	}

	private void highlightCurrentClue(boolean aOn, boolean aPressed, Canvas aCanvas) {
		highlightClue(iCursor, iDirection, iExtent, iClue, aOn, aPressed, aCanvas);
	}
	
	private void highlightClue(Point aCursor, int aDirection, Rect aExtent, Clue aClue, boolean aOn, boolean aPressed, Canvas aCanvas) {
		MainActivity.debug(1,TAG, String.format("highlightClue(%s, %s, %s, %s, %s, %s)", aCursor, aDirection, aExtent, aOn, aPressed, aClue));
		if (aClue == null || aCanvas == null)
			return;
		Point delta = new Point(1 - aDirection, aDirection);
		int pos = aCursor.x - aExtent.left + aCursor.y - aExtent.top;
		int len = aExtent.right - aExtent.left + aExtent.bottom - aExtent.top + 1;
		int word = 0;
		int wordlen = aClue.wordLength(word);
		if (wordlen == -1)
			wordlen = len;
		while (pos >= wordlen) {
			word++;
			wordlen += aClue.wordLength(word);
		}
		int colour = 0;
		word = 0;
		wordlen = aClue.wordLength(word);
		if (wordlen == -1)
			wordlen = len;
		Point sq = new Point(aExtent.left, aExtent.top);
		int hi = aPressed?mPressed:mHighlight;
		int alt = aPressed?mPressedAlt:mHighlightAlt;
		for (int i = 0; i < len; i++) {
			drawSquare(sq.x, sq.y, aOn ? (colour != 0 ? alt : hi) : Color.WHITE, aCanvas);
			drawLetter(sq.x, sq.y, aCanvas);
			drawNumber(sq.x, sq.y, aCanvas);
			sq.x += delta.x;
			sq.y += delta.y;
			if (--wordlen == 0) {
				word++;
				wordlen = aClue.wordLength(word);
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
				iCursor.set(iAnchor.x, iAnchor.y);
				iExtent.set(iPressedExtent);
				iDirection = iPressedDirection;
				if (!iClue.equals(iPressedClue)) {
					iClue = iPressedClue;
					if (iObserver != null) {
						iObserver.onGridClueSelected(iClue, iCursor.x, iCursor.y, iDirection);
					}
				}
				highlightCurrentClue(true);
				invalidate();
				iObserver.onGridClueLongPress(iPressedClue, iAnchor.x, iAnchor.y, iPressedDirection);
				mLongPressTriggered = true;
			}

		}
	}
}
