package uk.org.downiesoft.crossword;

import android.graphics.Point;
import android.graphics.Rect;

public class GridClueContext {

	private int mDirection;
	private Point mPosition;
	private Rect mExtent;
	private Clue mClue;

	GridClueContext() {
		mPosition = new Point(0,0);
		mDirection = CrosswordModel.CLUE_ACROSS;
	}

	GridClueContext(int aCol, int aRow, int aDirection) {
		this.mPosition = new Point(aCol, aRow);
		this.mDirection = aDirection;
	}

	GridClueContext(int aCol, int aRow, int aDirection, Rect aExtent, Clue aClue) {
		this.mPosition = new Point(aCol, aRow);
		this.mDirection = aDirection;
		this.mExtent = new Rect(aExtent);
		this.mClue = aClue;
	}

	GridClueContext(Point aPos, int aDirection, Rect aExtent, Clue aClue) {
		this.mPosition = new Point(aPos.x, aPos.y);
		this.mDirection = aDirection;
		this.mExtent = new Rect(aExtent);
		this.mClue = aClue;
	}

	GridClueContext(GridClueContext aClueContext) {
		this.mPosition = new Point(aClueContext.mPosition);
		this.mDirection = aClueContext.mDirection;
		this.mExtent = new Rect(aClueContext.mExtent);
		this.mClue = aClueContext.mClue;
	}

	public void setClue(Clue aClue) {
		this.mClue = aClue;
	}

	public Clue getClue() {
		return mClue;
	}

	public void setExtent(Rect aExtent) {
		this.mExtent = new Rect(aExtent);
	}

	public Rect getExtent() {
		return mExtent;
	}

	public void setPosition(int aX, int aY) {
		this.mPosition.set(aX, aY);			
	}

	public void setPosition(Point aPosition) {
		setPosition(aPosition.x, aPosition.y);
	}

	public Point getPosition() {
		return mPosition;
	}

	public void setDirection(int aDirection) {
		this.mDirection = aDirection;
	}

	public int getDirection() {
		return mDirection;
	}

	public void setCursor(int aCol, int aRow, int aDirection) {
		mPosition.set(aCol, aRow);
		mDirection = aDirection;
	}

	public void setCursor(Point aPos, int aDirection) {
		mPosition.set(aPos.x, aPos.y);
		mDirection = aDirection;
	}

	public void setContext(GridClueContext aClueContext) {
		this.mPosition.set(aClueContext.mPosition.x,aClueContext.mPosition.y);
		this.mDirection = aClueContext.mDirection;
		this.mExtent = new Rect(aClueContext.mExtent);
		this.mClue = aClueContext.mClue;
	}

	@Override
	public boolean equals(Object o) {
		try {
			GridClueContext oc = (GridClueContext)o;
			boolean equal = oc != null;
			equal = equal && this.mPosition.equals(oc.mPosition);
			equal = equal && this.mDirection == oc.mDirection;
			equal = equal && this.mExtent.equals(oc.mExtent);
			equal = equal && this.mClue.equals(oc.mClue);
			return equal;
		} catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	public String toString() {
		return 	String.format("CursorContext{%s, %s, %s}", mPosition, mDirection, mExtent);			
	}

}
	

