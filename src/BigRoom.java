import java.util.Vector;
import java.io.*;
import java.util.Enumeration;

class BigRoom extends Level  implements Serializable {
	private static final long serialVersionUID= 3L;
	Room theroom;
	static final int BIG_ROOM= 10;
	static final int MIN_LEVEL= 3;

	BigRoom(int nrow, int ncol, Rogue self){
		super(nrow, ncol, self);
		int i, j;
		boolean bfinished= false;

		if(cur_level < LAST_map)
			cur_level++;
		if(cur_level > max_level)
			max_level= cur_level;
		while(!bfinished)try{
////		Room r;
			rooms= new Vector(1);

			int top_row= self.rand.get(MIN_ROW, MIN_ROW+5);
			int height= self.rand.get(nrow-7, nrow-2)-top_row;
			int left_col= self.rand.get(1,10);;
			int width= self.rand.get(ncol-11, ncol-1)-left_col;
			theroom= new Room(BIG_ROOM, left_col, top_row, width, height, this, false);
			rooms.addElement(theroom);
			if(cur_level>=Level.TREE_MINLEVEL && self.rand.percent(50))
				theroom.drop_rox(self.rand.get(24,250));

			stairlevel(46, true);
			bfinished= true;
		}catch(Exception e){
			System.out.println("Something failed--redo level " + cur_level);
			super.init();
		}
	}
}

