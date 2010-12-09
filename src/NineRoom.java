import java.util.Vector;
import java.io.*;
import java.util.Enumeration;

class NineRoom extends Level  implements Serializable {
	private static final long serialVersionUID= 3L;
	static final Room PASSAGE= new Room();
	static {PASSAGE.rn= -1;};

	static final int GOLD_PERCENT= 46;
	static final int MAXROOMS= 9;

	NineRoom(int nrow, int ncol, Rogue self){
		super(nrow, ncol, self);
		int i, j;
		boolean bfinished= false;

		if(cur_level < LAST_map)
			cur_level++;
		if(cur_level > max_level)
			max_level= cur_level;
		while(!bfinished)try{
			int must_1= 0, must_2= 0, must_3= 0;
			//Room r= null;

			rooms= new Vector(MAXROOMS);

			// Required rooms (need one full row or one full column)
			switch(self.rand.get(5)){
			case 0:	must_1= 0; must_2= 1; must_3= 2; break;
			case 1:	must_1= 3; must_2= 4; must_3= 5; break;
			case 2:	must_1= 6; must_2= 7; must_3= 8; break;
			case 3: must_1= 0; must_2= 3; must_3= 6; break;
			case 4: must_1= 1; must_2= 4; must_3= 7; break;
			case 5: must_1= 2; must_2= 5; must_3= 8; break;
			}
			for(i= 0; i < MAXROOMS; i++){
				int top_row= MIN_ROW + (i/3)*(nrow/3);
				int left_col= (i%3)*(ncol/3)+1;
				int height= self.rand.get(4, (nrow/3)-1);
				int width= self.rand.get(7, (ncol/3)-4);

				top_row += self.rand.get((nrow/3) - height - 1);
				left_col+= self.rand.get((ncol/3) - width - 1);
				boolean isnothing= i!=must_1 && i!=must_2 && i!=must_3 && self.rand.percent(40);
				rooms.addElement((Object)(new Room(i, left_col, top_row,
					width, height, this, isnothing)));
			}
			//int nx= 0; for(j= 0; j<MAXROOMS; j++)if(((Room)rooms.elementAt(j)).is_room==Room.R_NOTHING)++nx;
			//System.out.println("Missing " + nx);
			add_mazes();
			int perm[]= self.rand.permute(MAXROOMS);

			for(j= 0; j < MAXROOMS; j++){
				Room r= (Room)rooms.elementAt(perm[j]);
				connect(r, 1);
				connect(r, 3);
				if(r.rn + 2<rooms.size()
				&& ((Room)rooms.elementAt(r.rn+1)).is_room==Room.R_NOTHING){
					if(connect(r, 2)){
						Room ra= (Room)rooms.elementAt(r.rn+1);
						ra.is_room= Room.R_CROSS;
					}
				}
				if(r.rn + 6<rooms.size()
				&& ((Room)rooms.elementAt(r.rn+3)).is_room==Room.R_NOTHING){
					if(connect(r, 6)){
						Room ra= (Room)rooms.elementAt(r.rn+3);
						ra.is_room= Room.R_CROSS;
					}
				}
				if(is_all_connected())
					break;
			}
			fill_out_level();
			if(!is_all_connected())
				throw new Exception("Disconnected");
			if(cur_level>Level.TREE_MINLEVEL && self.rand.percent(self.treepct%100)){
				Rowcol px= gr_row_col(FLOOR, null);
				if(px!=null)
					room_at(px.row, px.col).drop_rox(self.rand.get(4,32));
			}
			// put_stairs here, but not in a maze room
			stairlevel(GOLD_PERCENT, cur_level>1 && self.rand.percent(8));

			bfinished= true;
		}catch(Exception e){
			System.out.println("Something failed--redo level " + cur_level);
			super.init();
		}
	}
	private void add_mazes(){
		if(cur_level <= 1)
			return;
		int maze_percent= (cur_level * 5) / 4;
		if(cur_level > 15)
			maze_percent += cur_level;
		int i= rooms.size();
		while(--i>=0){
			Room rj= (Room)rooms.elementAt(i);
			if(self.rand.percent(maze_percent))
				rj.mazeroom();
		}
	}
	boolean is_all_connected(){
		Room root= null;
		int i;
		for(i= 0; i<rooms.size(); i++){
			Room r= (Room)rooms.elementAt(i);
			r.is_room &= ~Room.R_VISIT;
			if(0 != (r.is_room & (Room.R_ROOM | Room.R_MAZE)))
				root= r;
		}
		if(root!=null){
			root.visit_rooms();
			for(i= 0; i<rooms.size(); i++){
					Room r= (Room)rooms.elementAt(i);
					if(0 != (r.is_room & (Room.R_ROOM | Room.R_MAZE))
					&& 0 == (r.is_room & Room.R_VISIT))
						return false;
			}
		}
		return true;
	}
	void fill_out_level(){
		int i= rooms.size();
		int perm[]= self.rand.permute(i);

		Room.endroom= null;
		while(--i>=0){
			Room r= (Room)rooms.elementAt(perm[i]);
			if(r != null
			&& (0 != (r.is_room & Room.R_NOTHING)
			 ||(0 != (r.is_room & Room.R_CROSS) && self.rand.coin())))
				r.fill_it(true);
		}
		if(Room.endroom != null)
			Room.endroom.fill_it(false);
	}
	boolean connect(Room rfr, int n){
		Rowcol p1= null, p2= null;
		int dir= 0;

		if(rfr.rn+n>=rooms.size())
			return false;
		Room rto= (Room)rooms.elementAt(rfr.rn+n);
		if(0==(rfr.is_room & Room.R_ROOMISH)
		|| 0==(rto.is_room & Room.R_ROOMISH))
			return false;

		if(same_row(rfr, rto)){
			if(rfr.rn%3 > rto.rn%3){
			//if(rfr.left_col > rto.right_col){
				p1= rfr.put_door(Id.LEFT);
				p2= rto.put_door(Id.RIGHT);
				dir= Id.LEFT;
			}else if(rto.rn%3 > rfr.rn%3){
			//}else if(rto.left_col > rfr.right_col){
				p1= rfr.put_door(Id.RIGHT);
				p2= rto.put_door(Id.LEFT);
				dir= Id.RIGHT;
			}
		} else if(same_col(rfr, rto)){
			if(rfr.rn/3 > rto.rn/3){
			//if(rfr.top_row > rto.bottom_row){
				p1= rfr.put_door(Id.UPWARD);
				p2= rto.put_door(Id.DOWN);
				dir= Id.UPWARD;
			} else if(rto.rn/3 > rfr.rn/3){
			//} else if(rto.top_row > rfr.bottom_row){
				p1= rfr.put_door(Id.DOWN);
				p2= rto.put_door(Id.UPWARD);
				dir= Id.DOWN;
			}
		} else
			return false;
		do
			rfr.draw_simple_passage(p1.row, p1.col, p2.row, p2.col, dir);
		while(self.rand.percent(4));
		rfr.doors[dir/2]= new Door(this, p1.row, p1.col, p2.row, p2.col);
		rto.doors[((dir+4)%Id.DIRS)/2]= new Door(this, p2.row, p2.col, p1.row, p1.col);
		rfr.doors[dir/2].connect(rto.doors[((dir+4)%Id.DIRS)/2]);
		return true;
	}
	boolean same_row(Room rfr, Room rto){
		return rfr.rn/3==rto.rn/3;
	}
	boolean same_col(Room rfr, Room rto){
		return rfr.rn%3==rto.rn%3;
	}
	private Room nth_room(int n){
		return n>=0 && n<rooms.size()? (Room)rooms.elementAt(n) : null;
	}
	Room nabes(Room r)[]{
		Room ra[]= new Room[4];
		ra[0]= nth_room(r.rn-3);
		ra[1]= nth_room(r.rn+1);
		ra[2]= nth_room(r.rn+3);
		ra[3]= nth_room(r.rn-1);
		return ra;
	}
}
