package uk.org.downiesoft.crossword;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import android.os.Bundle;

public class Clue {

	int iType;
	int iNumber;
	String iText;
	boolean iSelected;

	Clue() {
		iText = new String();
	};

	Clue(Bundle args) {
		iType = args.getInt("type");
		iNumber = args.getInt("number");
		iText = args.getString("text");
	}

	Clue(Clue aClue) {
		iType = aClue.iType;
		iNumber = aClue.iNumber;
		iText = new String(aClue.iText);
	}

	public int type() {
		return iType;
	}

	public int number() {
		return iNumber;
	}

	public String text() {
		return iText;
	};

	public void setType(int aType) {
		iType = aType;
	}

	public void setNumber(int aNumber) {
		iNumber = aNumber;
	};

	public void setText(String aText) {
		iText = new String(aText);
	};

	public void setSelected(boolean aSelected) {
		iSelected = aSelected;	
	}

	public boolean isSelected() {
		return iSelected;
	}

	public int length() {
		int bra = iText.lastIndexOf('(');
		if (bra < 0)
			return -1;
		int ket = iText.lastIndexOf(')');
		if (ket < 0 || bra >= ket || ket - bra - 1 > 16)
			return -1;
		String value = iText.substring(bra + 1, ket);		
		int i = 0;
		int len=0;
		while (i < value.length()) {
			int j = i;
			while (j < value.length() && Character.isDigit(value.charAt(j)))
				j++;
			len += Integer.parseInt(value.substring(i, j));
			i = j + 1;
		}
		return len;
	}

	@Override
	public boolean equals(Object o) {
		if (o.getClass().equals(Clue.class)) {
			Clue clue = (Clue)o;
			return clue.iNumber == this.iNumber && clue.iType == this.iType && this.iText.equals(clue.iText);
		}
		return false;
	}

	
	public int wordLength(int aWordIndex) {
		int bra = iText.lastIndexOf('(');
		if (bra < 0)
			return -1;
		int ket = iText.lastIndexOf(')');
		if (ket < 0 || bra >= ket || ket - bra - 1 > 16)
			return -1;
		String value = iText.substring(bra + 1, ket);

		int i = 0;
		while (aWordIndex-- > 0) {
			while (i < value.length() && Character.isDigit(value.charAt(i)))
				i++;
			i++;
			if (i >= value.length())
				return 0;
		}
		while (i < value.length() && value.charAt(i) == ' ')
			i++;
		int j = i;
		while (j < value.length() && Character.isDigit(value.charAt(j)))
			j++;
		if (j == i)
			return 0;
		return Integer.parseInt(value.substring(i, j));
	}

	public void externalize(DataOutputStream aStream) throws IOException {
		aStream.writeInt(iType);
		aStream.writeInt(iNumber);
		aStream.writeUTF(iText);
	}

	public void internalize(DataInputStream aStream) throws IOException {
		iType = aStream.readInt();
		iNumber = aStream.readInt();
		iText = aStream.readUTF();
	}

	public String toString() {
		return Integer.toString(iNumber) + Character.toString((char)(65 + 3 * iType)) + ":\t" + iText;
	}

	public Bundle toArgs() {
		Bundle args=new Bundle();
		args.putInt("type", iType);
		args.putInt("number", iNumber);
		args.putString("text", iText);
		return args;
	}
}
