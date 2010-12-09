import java.io.*;
class Rowcol  implements Serializable {
	private static final long serialVersionUID= 3L;
	int row;
	int col;
	Rowcol(int r, int c){
		row= r;
		col= c;
	}
	Rowcol(){
		this(0,0);
	}
	public String toString(){
		return '[' + Integer.toString(row) + ' ' + Integer.toString(col) + ']';
	}
}

