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
//import android.util.Log;

public class GridView extends View {

	private static final String TAG="uk.org.downiesoft.crossword.GridView";
	private static final int HIGHLIGHT = 0xffffa060;
	private static final int HIGHLIGHT_ALT = 0xffa0c8ff;

	public interface GridViewListener {
		public void onClueSelected(Clue aClue, int aCursorX, int aCursorY, int aCursorDirection);
	}

	private Context iContext;
	private Paint iPaint;
	private Paint mLetterPaint;
	private Paint mNumberPaint;
	private int iWidth;
	private int iHeight;
	private int iSize;
	private int iMargin;
	private int iDirection = CrosswordModel.CLUE_ACROSS;
	private Point iCursor;
	private Point iAnchor;
	private PointF iTouchAnchor;
	private boolean mDragInProgress;
	private Rect iExtent;
	private Clue iClue;
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
	private float mImageScale;
	private ScaleGestureDetector mScaleDetector;

	public GridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		iContext = context;
		iPaint = new Paint();
		mLetterPaint = new Paint();
		mNumberPaint = new Paint();
		iCursor = new Point();
		iExtent = new Rect();
		mScaleDetector = new ScaleGestureDetector(iContext, new ScaleListener());
	}

	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		mTextSize = (3 * w) / (CrosswordModel.GRID_SIZE * 4);
		mNumberSize = (5 * w) / (CrosswordModel.GRID_SIZE * 16);
		Rect textBounds = new Rect();
		iPaint.getTextBounds("W", 0, 1, textBounds);
		if (h > w) {
			iWidth = w;
			iHeight = iWidth;
		} else {
			iWidth = h * h / w;
			iHeight = h;
		}
		iSize = iWidth / CrosswordModel.GRID_SIZE;
		iMargin = (iWidth - iSize * CrosswordModel.GRID_SIZE) / 2;
		mBackBmp = Bitmap.createBitmap(iWidth, iHeight, Bitmap.Config.ARGB_8888);
		mBackCanvas = new Canvas(mBackBmp);
		mMatrix = new Matrix();
		mImageScale = 1f;
		mMatrix.postTranslate(iMargin, iMargin);
		
		mLetterPaint.setTypeface(Typeface.DEFAULT_BOLD);
		mLetterPaint.setTextSize(mTextSize);
		mLetterPaint.setTextAlign(Align.CENTER);
		mLetterPaint.getTextBounds("W", 0, 1, iLetterBounds);
		mLetterPaint.setStyle(Style.STROKE);

		mNumberPaint.setTypeface(Typeface.DEFAULT_BOLD);
		mNumberPaint.setTextSize(mNumberSize);
		mNumberPaint.setTextAlign(Align.LEFT);
		mNumberPaint.getTextBounds("88", 0, 2, iNumberBounds);
		mNumberPaint.setColor(Color.BLACK);

		redraw();
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int iWidth = MeasureSpec.getSize(widthMeasureSpec);
		int iHeight = MeasureSpec.getSize(heightMeasureSpec);
		int min=Math.min(iWidth, iHeight);
		setMeasuredDimension(min, min);
		iSize = min / CrosswordModel.GRID_SIZE;
		iMargin = (iWidth - iSize * CrosswordModel.GRID_SIZE) / 2;
		iWidth = min;
		iHeight = min;
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
		Point pos=new Point(col, row);
		boolean across=(col == 0 || iCrossword.isBlank(col - 1, row))
			&& col < CrosswordModel.GRID_SIZE - 1 && !iCrossword.isBlank(col + 1, row);
		boolean down=(row == 0 || iCrossword.isBlank(col, row - 1))
			&& row < CrosswordModel.GRID_SIZE - 1 && !iCrossword.isBlank(col, row + 1);
		int direction=across ?CrosswordModel.CLUE_ACROSS: (down ?CrosswordModel.CLUE_DOWN: -1);
		if (direction != -1) {
			r.set(col * iSize, row * iSize, (col + 1) * iSize, (row + 1) * iSize);
			Clue clue=iCrossword.clueAt(pos, direction);
			if (clue != null) {
				canvas.drawText(Integer.toString(clue.number()), r.left + 1, r.top + 1 + iNumberBounds.height(), mNumberPaint);
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
			r.set(col * iSize, row * iSize, (col + 1) * iSize, (row + 1) * iSize);
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
		r.set(col * iSize, row * iSize, (col + 1) * iSize, (row + 1) * iSize);
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
					drawSquare(col, row, color, canvas);
				}
			}
		}
	}

	@Override
	public void onDraw(Canvas canvas) {
		canvas.drawBitmap(mBackBmp, mMatrix, iPaint);
		//Log.d(TAG,String.format("onDraw finish %d millisecs",System.currentTimeMillis()-start));
	}

	public void redraw() {
		//Log.d(TAG,String.format("onDraw start"));
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
		highlightCurrentClue(true, mBackCanvas);		
	}

	public Clue getCurrentClue() {
		return iClue;
	}

	public void setObserver(GridViewListener aObserver) {
		iObserver = aObserver;
	}

	public void setCrossword(CrosswordModel aCrossword) {
		iCrossword = aCrossword;
		//if (iCrossword!=null && iCrossword.isValid())
		//	setCursor(0,0,CrosswordModel.CLUE_ACROSS);
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
		if (iCursor.x == aX && iCursor.y == aY && iDirection == aDirection) {
			return;
		}
		highlightCurrentClue(false, mBackCanvas);
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
			iObserver.onClueSelected(iClue, iCursor.x, iCursor.y, iDirection);
		}
		highlightCurrentClue(true, mBackCanvas);
		invalidate();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();
		float[] points={ x, y };
		Matrix inverse=new Matrix();
		mMatrix.invert(inverse);
		inverse.mapPoints(points);
		int col = (int) (points[0] / (float) iSize);
		int row = (int) (points[1] / (float) iSize);
		Log.d(TAG,String.format("onTouchEvent: (%3.1f,%3.1f),(%3.1f,%3.1f),(%d,%d)",x,y,points[0],points[1],col,row));
		Point pos = new Point(col, row);
		Rect extent;
		boolean invalidated = false;
		mScaleDetector.onTouchEvent(event);
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mDragInProgress=false;
				if (!mScaleDetector.isInProgress()) {
					iAnchor = new Point(col, row);
					iTouchAnchor = new PointF(x, y);
					return true;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if (!mScaleDetector.isInProgress() && mImageScale > 1) {
					if (iAnchor.x == -1 && iAnchor.y == -1)
						break;

					PointF raw=new PointF(x, y);
					double distance=Math.sqrt(Math.pow(raw.x - iTouchAnchor.x, 2) + Math.pow((raw.y - iTouchAnchor.y), 2));
					float time=event.getEventTime() - event.getDownTime();
					float velocity=(float)distance / time;
					if (time > 100 && velocity < 1) {
						mDragInProgress=true;
						moveBitmap(raw.x - iTouchAnchor.x, raw.y - iTouchAnchor.y);
						invalidate();
						iTouchAnchor.set(raw);
						return true;
					}
					return true;
				}
				break;
			case MotionEvent.ACTION_UP:
				if (!mScaleDetector.isInProgress() && !mDragInProgress) {
					if (iCrossword.withinGrid(pos) && !iCrossword.isBlank(pos)) {
						if (iCursor.equals(col, row)) {
							extent = iCrossword.getClueExtent(pos, 1 - iDirection);
							if (extent.width() > 0 || extent.height() > 0) {
								highlightCurrentClue(false, mBackCanvas);
								iDirection = 1 - iDirection;
								invalidated = true;
							} else
								return true;
						} else {
							int direction=iDirection;
							highlightCurrentClue(false, mBackCanvas);
							if (Math.abs(col - iAnchor.x) > Math.abs(row - iAnchor.y))
								direction = CrosswordModel.CLUE_ACROSS;
							else if (Math.abs(col - iAnchor.x) < Math.abs(row - iAnchor.y))
								direction = CrosswordModel.CLUE_DOWN;
							extent = iCrossword.getClueExtent(pos, direction);
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
							iDirection = direction;
						}
						iCursor = pos;
						iExtent.set(extent);
						iClue = iCrossword.clueAt(pos, iDirection);
						highlightCurrentClue(true, mBackCanvas);
						if (iObserver != null) {
							iObserver.onClueSelected(iClue, iCursor.x, iCursor.y, iDirection);
						}
						if (invalidated)
							invalidate();
					}
					return true;
				}
				mDragInProgress=false;
				break;
			default:
				return false;
		}
		return false;
	}

	private void highlightCurrentClue(boolean aOn, Canvas aCanvas) {
		if (iClue == null || aCanvas == null)
			return;
		Point delta = new Point(1 - iDirection, iDirection);
		int pos = iCursor.x - iExtent.left + iCursor.y - iExtent.top;
		int len = iExtent.right - iExtent.left + iExtent.bottom - iExtent.top + 1;
		int word = 0;
		int wordlen = iClue.wordLength(word);
		if (wordlen == -1)
			wordlen = len;
		while (pos >= wordlen) {
			word++;
			wordlen += iClue.wordLength(word);
		}
		int colour = 0;
		word = 0;
		wordlen = iClue.wordLength(word);
		if (wordlen == -1)
			wordlen = len;
		Point sq = new Point(iExtent.left, iExtent.top);
		for (int i = 0; i < len; i++) {
			drawSquare(sq.x, sq.y, aOn ? (colour != 0 ? HIGHLIGHT_ALT : HIGHLIGHT) : Color.WHITE, aCanvas);
			drawLetter(sq.x, sq.y, aCanvas);
			drawNumber(sq.x, sq.y, aCanvas);
			sq.x += delta.x;
			sq.y += delta.y;
			if (--wordlen == 0) {
				word++;
				wordlen = iClue.wordLength(word);
				colour ^= 1;
			}
		}

	}

	public void dumpGrid() {
		iCrossword.dumpGrid(TAG);
	}

	private class ScaleListener extends SimpleOnScaleGestureListener {

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			return true;
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			float scaleFactor = detector.getScaleFactor();
			scaleBitmap(scaleFactor, detector.getFocusX(), detector.getFocusY());
			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			centreImage(mMatrix);
			invalidate();
		}

	}

	private void getRotatedCoords(PointF aBmpSize, PointF aOffset) {
		float[] matrixValues = new float[9];
		mMatrix.getValues(matrixValues);
		aOffset.set(matrixValues[Matrix.MTRANS_X], matrixValues[Matrix.MTRANS_Y]);
		//Log.d(TAG, String.format("getRotatedCoords: offset=(%.1f, %.1f)", aOffset.x, aOffset.y));
		//Log.d(TAG, String.format("getRotatedCoords: (%.1f, %.1f)", aOffset.x, aOffset.y));
		aBmpSize.set(iWidth * mImageScale, iHeight * mImageScale);
		//Log.d(TAG, String.format("getRotatedCoords: offset=(%.1f, %.1f)", aOffset.x, aOffset.y));
	}


	private void centreImage(Matrix matrix) {
		PointF bmpSize=new PointF();
		PointF offset=new PointF();
		int dispW = iWidth;
		int dispH = iHeight;
		getRotatedCoords(bmpSize, offset);
		//Log.d(TAG, String.format("centreImage: bmpSize=(%.1f, %.1f)", bmpSize.x, bmpSize.y));
		//Log.d(TAG, String.format("centreImage: displaySize=(%d, %d)", mDisplayParams.mDisplaySize.x, mDisplayParams.mDisplaySize.y));
		PointF margins=new PointF();
		margins.x = bmpSize.x >= dispW ? 0 : (dispW - bmpSize.x) / 2;
		margins.y = bmpSize.y >= dispH ? 0 : (dispH - bmpSize.y) / 2;
		//Log.d(TAG, String.format("centreImage: margins=(%.1f, %.1f)", margins.x, margins.y));
		float dx = 0;
		float dy = 0;
		if (offset.x > margins.x) 
			dx = margins.x - offset.x;
		if (offset.x + dx + bmpSize.x < dispW - margins.x)
			dx = dispW - margins.x - bmpSize.x - offset.x - dx;
		if (offset.y > margins.y)
			dy = margins.y - offset.y;
		if (offset.y + dy + bmpSize.y < dispH - margins.y)
			dy = dispH - margins.y - bmpSize.y - offset.y - dy;
		//Log.d(TAG, String.format("centreImage: dx=%f, dy=%f", dx, dy));
		if (dx != 0 || dy != 0) {
			matrix.postTranslate(dx, dy);
		}

	}

	private void moveBitmap(float aDx, float aDy) {
		float[] matrixValues = new float[9];
		mMatrix.getValues(matrixValues);
		mMatrix.postTranslate(aDx, aDy);
		centreImage(mMatrix);
		invalidate();
	}

	private void scaleBitmap(float aScaleFactor, float aFocusX, float aFocusY) {
		//Log.d(TAG,String.format("scaleBitmap(%3.2f,%3.2f,%3.2f)", aScaleFactor, aFocusX, aFocusY));
		//Log.d(TAG,String.format("mImageScale=%3.2f, mImageParams.mScale=%3.2f, mMinimumScale=%3.2f", mImageScale, mImageParams.mScale, mMinimumScale));
		if (mImageScale * aScaleFactor < 1)
			aScaleFactor = 1 / mImageScale;
		if (mImageScale * aScaleFactor > 2)
			aScaleFactor = 2 / mImageScale;
		mMatrix.postScale(aScaleFactor, aScaleFactor, aFocusX, aFocusY);
		mImageScale *= aScaleFactor;
		centreImage(mMatrix);
		invalidate();
	}


}
