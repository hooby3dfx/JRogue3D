import java.util.Vector;
import java.io.*;
import java.util.Enumeration;

class Level implements Header, Serializable {
	private static final long serialVersionUID= 3L;
	static final int TREE_MINLEVEL= 4;
	Rogue self;
	Item dummy;
	ItemVector level_men;
	ItemVector level_toys;
	ItemVector level_monsters;
	ItemVector level_traps;
	ItemVector level_doors;
	Vector rooms;
	int map[][];
	int ncol;		// Width
	int nrow;		// Height
	int cur_level= 0;		// Level number
	int max_level= 0;
	int foods= 0;		// Higher levels get more food

	Level(int nrow, int ncol, Rogue self){
		this.self= self;
		this.nrow= nrow;
		this.ncol= ncol;
		init();
		if(self.level!=null){
			cur_level= self.level.cur_level;
			max_level= self.level.max_level;
			foods= self.level.foods;
		}
	}
	void init(){
		map= new int[nrow][ncol];

		for(int r= 0; r<nrow; r++)
			for(int c= 0; c<ncol; c++)
				map[r][c]= 0;

		level_monsters= new ItemVector(6);	// Start with a few monsters
		level_doors= new ItemVector(8);		// and a few doors too
		level_men= new ItemVector();
		level_toys= new ItemVector(6);
		level_traps= new ItemVector(3);
	}
	void mark(int r, int c){
		int i= level_men.size();
		while(--i>=0)
			((Man)level_men.elementAt(i)).view.mark(r,c);
	}
	char get_char(int row, int col){
		if(row<0 || row>=nrow || col<0 || col>=ncol)
			return (char)0;
		int mask= map[row][col];
		if(0!=(mask & TOY)){
			Toy toy= (Toy)level_toys.item_at(row, col);
			return toy==null? ';' : (char)Id.get_mask_char(toy.kind);
		}
		char ch= get_bgchar(row, col);
		if(0!=(mask&GORE))
			ch= (char)(ch|uWeak);
		return ch;
	}
	char get_bgchar(int row, int col){
		/* Not allowing hidden stairs */
		int mask= map[row][col];

		if(0!=(mask & STAIRS))
			return '%';
		if(0!=(mask & (TUNNEL|HORWALL|VERTWALL|FLOOR|DOOR|BLOCK))){
			if(TUNNEL==(mask & (HIDDEN|TUNNEL)))	return '#';
			if(0!=(mask & HORWALL))		return '-';
			if(0!=(mask & VERTWALL))	return '|';
			if(BLOCK==(mask&(HIDDEN|BLOCK)))	return 'o';
			if(0!=(mask & FLOOR))
				return (mask & (HIDDEN|TRAP)) == TRAP ? '^' : '.';
			if(0!=(mask & DOOR)){
				if(0==(mask & HIDDEN))	return '+';

				/* Hidden door: */
				if(0!=(mask & TUNNEL))
					return ' ';//FOOP

				if(col<=0||col>=ncol-1)	return '|';
				if(0!=(map[row][col-1] & HORWALL)
				|| 0!=(map[row][col+1] & HORWALL))
					return '-';
				return '|';
			}
		}
		return ' ';
	}
	boolean is_passable(int row, int col){
		if(row<MIN_ROW || row>(nrow- 2) || col<0 ||	col>(ncol-1))
			return false;
		int mask= map[row][col];
		if(0!=(mask&BLOCK))
			return false;
		if(0!=(mask & HIDDEN))
			return 0 != (mask & TRAP);
		return 0 != (mask & (FLOOR|TUNNEL|DOOR|STAIRS|TRAP));
	}
	boolean can_turn(int row, int col){
		return 0!=(TUNNEL & map[row][col]) && is_passable(row, col);
	}
	boolean can_move(int row1, int col1, int row2, int col2){
		if(!is_passable(row2, col2))
			return false;
		if(row1 != row2 && col1 != col2){
			// you can always move diagonally from floor to floor
			if(0 != (map[row1][col1]&map[row2][col2]&FLOOR))
				return true;
			if(0 != ((map[row1][col1]|map[row2][col2]) & DOOR))
				return false;
			if(0==map[row1][col2] || 0==map[row2][col1])
				return false;
		}
		return true;
	}
	// Get a point in a random room--avoid itm
	Rowcol gr_row_col(int mask, Item itm){
		int r= 0, c= 0;
		mask |= HOLDER|DARK;
		int ntry= 2400;
		do {
			if(--ntry>0){
				r= self.rand.get(MIN_ROW, nrow-2);
				c= self.rand.get(ncol-1);
			}else if(ntry==0){
				r= nrow-2;
				c= ncol-1;
			}else if(--c<0){
				c= ncol-1;
				if(--r<0)
					return null;
			}
		} while(0==(map[r][c] & mask)
		     || 0!=(map[r][c] & (~mask))
			 || 0==(map[r][c] & HOLDER)
		     || (itm!=null && r==itm.row && c==itm.col));
		return new Rowcol(r,c);
	}
	void plant_gold(int row, int col, int gold){
		Toy obj= new Toy(this, Id.GOLD);
		obj.quantity= gold;
		obj.place_at(row, col, TOY);
	}
	Scroll gr_scroll(){
		Scroll t= new Scroll(this);
		t.kind= Id.gr_which_scroll(self.rand);
		return t;
	}
	Potion gr_potion(){
		Potion t= new Potion(this);
		t.kind= Id.gr_which_potion(self.rand);
		return t;
	}
	Toy gr_weapon(int assign_wk){
		if(assign_wk<0)
			assign_wk= self.rand.get(Id.id_weapons.length-1);
		return new Toy(this, Id.WEAPON|assign_wk);
	}
	Toy gr_armor(){
		return new Toy(this, Id.ARMOR + self.rand.get(Id.id_armors.length-1));
	}
	Toy gr_wand(){
		int n= Monster.isold? 1:2;	// last wand is for old only
		return new Toy(this, Id.WAND + self.rand.get(Id.id_wands.length-n));
	}
	Toy gr_ring(int assign_wk){
		if(assign_wk<0)
			assign_wk= self.rand.get(Id.id_rings.length-1);
		return new Toy(this, Id.RING + assign_wk);
	}
	Toy get_food(boolean force_ration){
		return new Toy(this, force_ration || self.rand.percent(80)? Id.RATION : Id.FRUIT);
	}
	Toy gr_toy(){
		int k;
		if(foods < cur_level/2){
			k= Id.FOOD;
			foods++;
		}else{
			k= Id.gr_species(self.rand);

			// Stop the reloaders!!! (no armor or weapons on the first level)
			if(cur_level==1 && (k==Id.ARMOR || k==Id.WEAPON))
				return null;
		}
		switch(k){
		case Id.SCROLL:		return (Toy)gr_scroll();
		case Id.POTION:		return (Toy)gr_potion();
		case Id.WEAPON:		return (Toy)gr_weapon(-1);
		case Id.ARMOR:		return (Toy)gr_armor();
		case Id.WAND:		return (Toy)gr_wand();
		case Id.FOOD:		return (Toy)get_food(false);
		case Id.RING:		return (Toy)gr_ring(-1);
		}
		return null;
	}
	Toy wiztoy(Man man, int ch){
		Toy t= null;
		int max= 0;
		String buf= "0";
		switch(ch){
		case '!':
			t= (Toy)gr_potion();
			max= Id.id_potions.length;
			break;
		case '?':
			t= (Toy)gr_scroll();
			max= Id.id_scrolls.length;
			break;
		case ',':
			t= new Toy(this, Id.AMULET);
			break;
		case ':':
			t= (Toy)get_food(false);
			break;
		case ')':
			max= Id.id_weapons.length;
			break;
		case ']':
			t= (Toy)gr_armor();
			max= Id.id_armors.length;
			break;
		case '/':
			t= (Toy)gr_wand();
			max= Id.id_wands.length;
			break;
		case '=':
			max= Id.id_rings.length;
			break;
		}
		--max;
		if(ch == ',' || ch == ':')
			return t;
		if(max<0)
			return null;
		if(man != null){
			buf= man.view.msg.get_input_line("which kind of "+((char)ch) + " (0 to "+max+")?", "","", false, true);
			man.view.msg.check_message();
		}
		if(buf!=null){
			int wk= -1;
			try {
				wk= Integer.parseInt(buf);
			}catch(NumberFormatException e){
			}
			if(wk >= 0 && wk <= max){
				if(ch == '=')
					t= gr_ring(wk);
				else if(ch == ')')
					t= gr_weapon(wk);
				else if(t!=null)
					t.kind= (t.kind & Id.ALL_TOYS) + wk;
				return t;
			}
		}
		return null;
	}
	void put_toys(){
		if(cur_level < max_level)
			return;
		int n= self.rand.get(2,4);
		if(self.rand.coin())
			n++;
		while(self.rand.percent(33))
			n++;
		for(int i= 0; i < n; i++){
			Rowcol pt= gr_row_col(FLOOR|TUNNEL, null);
			if(pt!=null){
				Toy t= gr_toy();
				if(t!=null)
					t.place_at(pt.row, pt.col, TOY);
			}
		}
	}
	Rowcol get_dir_rc(int dir, int row, int col, boolean allow_off_screen){
		switch(dir){
		case Id.UPLEFT:	--row;
		case Id.LEFT: 	--col; break;
		case Id.DOWNLEFT:	--col;
		case Id.DOWN:		++row; break;
		case Id.UPRIGHT:	++col;
		case Id.UPWARD:	--row; break;
		case Id.DOWNRIGHT:++row;
		case Id.RIGHT:	++col; break;
		}
		if(allow_off_screen || (row>MIN_ROW && row<nrow-2 && col>0 && col<ncol-1))
			return new Rowcol(row, col);
		return null;
	}
	void put_player(Man man){
		/* try not to put where he can see his current position */
		int misses= 2;
		Rowcol pt;
		do{
			pt= gr_row_col(FLOOR|TUNNEL|TOY|STAIRS, man);
			if(pt==null)
				return;
		}while(--misses>=0 && sees(pt.row, pt.col, man.row, man.col));
		man.place_at(pt.row, pt.col, MAN);
		wake_room(man, true, pt.row, pt.col);
		if(man.new_level_message!=null){
			self.tell(man, man.new_level_message, false);
			man.new_level_message= null;
		}
	}
	void draw_magic_map(Man man){
		for(int i= 0; i < nrow; i++)
		for(int j= 0; j < ncol; j++){
			int s= map[i][j];
			if(0 !=(s & (HORWALL|VERTWALL|DOOR|TUNNEL|TRAP|STAIRS))){
				map[i][j]|= MAPPED;
				if(0!=(s&STAIRS))
					map[i][j] &= ~DARK;
				man.view.mark(i, j);
			}
		}
	}
	void unhide(){
		for(int i= 0; i < nrow; i++)
		for(int j= 0; j < ncol; j++)
		if(0!=(map[i][j] & HIDDEN)){
			map[i][j] &= ~HIDDEN;
			mark(i,j);
		}
	}
	Monster get_zapped_monster(int dir, int row, int col){
		for(;;){
			int ocol= col;
			int orow= row;
			Rowcol pt= get_dir_rc(dir, row, col, false);
			row= pt.row;
			col= pt.col;
			if((row==orow && col==ocol)
			|| 0!=(map[row][col] & (HORWALL|VERTWALL))
			|| 0==(map[row][col] & SOMETHING))
				break;
			if(0 != (map[row][col] & MONSTER)){
				Monster monster= (Monster)(level_monsters.item_at(row, col));
				if(!imitating(row, col))
					return monster;
			}
		}
		return null;
	}
	void wdrain_life(Man man, Monster monster){
		if(man.hp_current<3){
			man.tell("you are too weak to use it");
			return;
		}
		int hp= man.hp_current / 3;
		man.hp_current= (man.hp_current + 1) / 2;

		Enumeration e= level_monsters.elements();
		while(e.hasMoreElements()){
			Monster lmon= (Monster)e.nextElement();
			if(sees(lmon.row, lmon.col, man.row, man.col)){
				lmon.wake_up();
				lmon.damage(man, hp, 0);
				monster= null;
			}
		}
		if(monster != null){
			monster.wake_up();
			monster.damage(man, hp, 0);
		}
		man.print_stat();
		man.view.markall();	// relight
	}
	static int btime;
	void bounce(Toy obj, int dir, int row, int col, int r){
		boolean fiery= obj.kind==Id.FIRE;
		int orow, ocol;
		int i, ch, new_dir= -1;
		boolean doend= true;
		Persona owner= obj.owner;

		if(r==1)
			return;
		if(r==0)
			r= self.rand.get(3, 6)+1;
		String s= fiery? "fire" : "ice";
		//if(r > 1)
		//	self.view.msg.message("the "+s+" bounces", true);
		self.md_sleep(100);
		orow= row;
		ocol= col;
		Rowcol pt;
		do {
			self.flashadd(orow, ocol, uBrite);
			pt= get_dir_rc(dir, orow, ocol, true);
			orow= pt.row; ocol= pt.col;
		} while(!(ocol <= 0
			 || ocol >= ncol-1
			 || 0 == (map[orow][ocol] & SOMETHING)
			 || 0 != (map[orow][ocol] & MONSTER )
			 || 0 != (map[orow][ocol] & (HORWALL|VERTWALL))
			 || (orow == owner.row  && ocol == owner.col)));
		self.xflash();
		do {
			orow= row;
			ocol= col;
			//self.vset(row, col);
			self.mark(row,col);
			pt= get_dir_rc(dir, row, col, true);
			row= pt.row; col= pt.col;
		} while(!(col <= 0
			 || col >= ncol-1
			 || 0==(map[row][col] & SOMETHING)
			 || 0 != (map[row][col] & MONSTER)
			 || 0 != (map[row][col] & (HORWALL|VERTWALL))
			 || (row == owner.row && col == owner.col)));

		if(0 != (map[row][col] & MONSTER)){
			Monster monster= (Monster)level_monsters.item_at(row, col);
			if(monster!=null)
				doend= monster.zapt(obj);
		} else{
			Man man= self.view.man;
			if(row==man.row && col==man.col)
				doend= man.zapt(obj);
		}
		if(doend){
			int nr, nc;
			for(i= 0; i < 10; i++){
				dir= self.rand.get(Id.DIRS-1);
				nr= orow;
				nc= ocol;
				pt= get_dir_rc(dir, nr, nc, true);
				nr= pt.row; nc= pt.col;
				if((nc>=0 && nc<=ncol-1)
				 &&	0!=(map[nr][nc] & SOMETHING)
				 && 0==(map[nr][nc] & (VERTWALL|HORWALL))){
					new_dir= dir;
					break;
				}
			}
			if(new_dir != -1)
				bounce(obj, new_dir, orow, ocol, --r);
		}
	}
	void put_monsters(){
		int n= self.rand.get(4, 6);

		for(int i= 0; i < n; i++){
			Rowcol pt= gr_row_col(FLOOR|TUNNEL|STAIRS|TOY, null);
			if(pt==null)
				continue;
			Monster monster= gr_monster();
			if(0!=(monster.m_flags & Monster.WANDERS) && self.rand.coin())
				monster.wake_up();
			monster.put_m_at(pt.row, pt.col);
			//System.out.println("Monster " + monster);
		}
	}
	Monster gr_monster(){
		int mn;

		for(;;){
			mn= self.rand.get(Monster.MONSTERS-1);
			if((cur_level >= Monster.mon_tab[mn].first_level)
			&& (cur_level <= Monster.mon_tab[mn].last_level))
				break;
		}
		Monster monster= new Monster(this, mn);
		if(0!=(monster.m_flags & Monster.IMITATES))
			monster.disguise= Id.gr_obj_char(self.rand);
		if(cur_level > AMULET_LEVEL+2)
			monster.m_flags |= Monster.HASTED;
		monster.trow= -1;
		return monster;
	}
	int gmc_row_col(Man man, int row, int col){
		Monster monster= (Monster)level_monsters.item_at(row, col);
		return monster!=null? monster.gmc(man) : '&';
	}
	void mv_mons(Man man){	// Move all the monsters
		if(0!=(man.haste_self %2))
			return;
		Enumeration e= level_monsters.elements();
		while(e.hasMoreElements() && !man.game_over){
			Monster monster= (Monster)e.nextElement();
			monster.dstrow= man.row;
			monster.dstcol= man.col;
			if(0!=(monster.m_flags & Monster.HASTED)){
				monster.mv_monster();
				if(!level_monsters.contains(monster))
					continue;
			} else if(0!=(monster.m_flags & Monster.SLOWED)){
				monster.slowed_toggle= !monster.slowed_toggle;
				if(monster.slowed_toggle)
					continue;
			}
			if(0!=(monster.m_flags & Monster.CONFUSED) && monster.move_confused())
				continue;
			boolean flew= false;
			if(	0!=(monster.m_flags & Monster.FLIES)
			&&  0==(monster.m_flags & Monster.NAPPING)
			&&  (monster.ihate==null || !monster.mon_can_go(monster.ihate.row, monster.ihate.col))){
				flew= true;
				monster.mv_monster();
				if(!level_monsters.contains(monster))
					continue;
			}
			if(!(flew && (monster.ihate==null || monster.mon_can_go(monster.ihate.row, monster.ihate.col)))){
				monster.mv_monster();
			}
		}
	}
	void wanderer(){
		for(int i= 0; i<15; i++){
			Monster monster= gr_monster();
			if(0!=(monster.m_flags & (Monster.WAKENS|Monster.WANDERS))){
				monster.wake_up();
				for(i= 0; i < 25; i++){
					Rowcol pt= gr_row_col(FLOOR|TUNNEL|STAIRS|TOY, null);
					if(pt!=null){
						int j= level_men.size();
						while(--j>=0){
							Man m= (Man)level_men.elementAt(j);
							if(m.can_see(pt.row, pt.col)) break;
						}
						if(j<0){
							monster.put_m_at(pt.row, pt.col);
							return;
						}
					}
				}
				return;
			}
		}
	}
	void mv_aquatars(Persona man){
		/* aquatars get to hit early if man removes his armor */
		Enumeration e= level_monsters.elements();
		while(e.hasMoreElements()){
			Monster monster= (Monster)e.nextElement();
			if((monster.ichar == 'A') &&
			monster.mon_can_go(man.row, man.col)){
				monster.ihate= man;
				monster.mv_monster();
				monster.m_flags |= Monster.ALREADY_MOVED;
			}
		}
	}
	boolean imitating(int r, int c){
		if(0!=(map[r][c] & MONSTER)){
			Monster monster= (Monster)level_monsters.item_at(r, c);
			return monster != null && 0!=(monster.m_flags & Monster.IMITATES);
		}
		return false;
	}
	boolean show_monsters(Man man){
		boolean found= false;
		man.detect_monster= true;
		if(man.blind>0)
			return false;
		Enumeration e= level_monsters.elements();
		while(e.hasMoreElements()){
			Monster monster= (Monster)e.nextElement();
			man.view.addch(monster.row, monster.col, monster.ichar);
			if(0!=(monster.m_flags & Monster.IMITATES)){
				monster.m_flags &= ~Monster.IMITATES;
				monster.m_flags |= Monster.WAKENS;
			}
			found= true;
		}
		return found;
	}
	void show_toys(Man man){
		if(man.blind>0)
			return;
		Enumeration e= level_toys.elements();
		while(e.hasMoreElements()){
			Toy t= (Toy)e.nextElement();
			man.view.addch(t.row, t.col, (char)t.ichar);
		}
	}
	void show_traps(Man man){
		Enumeration e= level_traps.elements();
		while(e.hasMoreElements()){
			Item t= (Item)e.nextElement();
			man.view.addch(t.row, t.col, '^');

		}
	}		
	boolean seek_gold(Monster monster){
		Enumeration e= level_toys.elements();
		while(e.hasMoreElements()){
			Toy gold= (Toy)e.nextElement();
			if(gold.kind!=Id.GOLD
			|| 0!=(map[gold.row][gold.col] & MONSTER)
			|| !sees(monster.row, monster.col, gold.row, gold.col))
				continue;
			monster.m_flags |= Monster.CAN_FLIT;
			if(monster.mon_can_go(gold.row, gold.col)){
				monster.m_flags &= ~Monster.CAN_FLIT;
				monster.move_mon_to(gold.row, gold.col);
				monster.m_flags |= Monster.ASLEEP;
				monster.m_flags &= ~(Monster.WAKENS|Monster.SEEKS_GOLD);
			}else{
				monster.m_flags &= ~Monster.SEEKS_GOLD;
				monster.m_flags |= Monster.CAN_FLIT;
				monster.mv_to(gold.row, gold.col);
				monster.m_flags &= ~Monster.CAN_FLIT;
				monster.m_flags |= Monster.SEEKS_GOLD;
			}
			return true;
		}
		return false;
	}
	boolean sees(int r, int c, int r1, int c1){	// Is r,c visible from r1,c1?
		int ri, ci, dr, dc;
	
		if(r1>r){
			dr= r1-r; ri= 1;
		}else{
			dr= r-r1; ri= -1;
			if(dr==0)ri= 0;
		}
		if(c1>c){
			dc= c1-c; ci= 1;
		}else{
			dc= c-c1; ci= -1;
			if(dc==0)ci= 0;
		}
		// Tunnel case
		if(dr<=1 && dc<=1 && 0!=(map[r1][c1] & TUNNEL))
			return 0!=(map[r][c] & (TUNNEL|DOOR|FLOOR));
		if(dr>dc){
			// If the first point (typically the non-critter point)
			// is not floor, then offset it a little so walls and
			// doors are more visible
			//
			// Note that r,c and r1,c1 are not checked for floorishness--
			// only the interior points of the line joining them are
			// so checked
			int sum= dr>>1;
			if(0==(map[r][c] & (BLOCK|FLOOR))){
				c += ci;
				sum -= dr;
			}
			do{
				r += ri;
				sum += dc;
				if(sum>=dr){
					c += ci;
					sum -= dr;
				}
				if(r==r1 && c==c1)
					return true;
			}while(0!=(map[r][c] & FLOOR));
		}else if(dc>0){
			int sum= dc>>1;
			if(0==(map[r][c] & (BLOCK|FLOOR))){
				r += ri;
				sum -= dc;
			}
			do{
				c += ci;
				sum += dr;
				if(sum>=dc){
					r += ri;
					sum -= dc;
				}
				if(r==r1 && c==c1)
					return true;
			}while(0!=(map[r][c] & FLOOR));
		}
		return false;
	}
/*
	// Places near a door...
	Rowcol porch(int r, int c){
		if(0!=(TUNNEL & map[r+1][c]))return new Rowcol(r+1,c);
		if(0!=(TUNNEL & map[r-1][c]))return new Rowcol(r-1,c);
		if(0!=(TUNNEL & map[r][c+1]))return new Rowcol(r,c+1);
		if(0!=(TUNNEL & map[r][c-1]))return new Rowcol(r,c-1);
		return null;
	}
*/
	Rowcol foyer(int r, int c){
		if(0!=(HOLDER & map[r+1][c]))return new Rowcol(r+1,c);
		if(0!=(HOLDER & map[r-1][c]))return new Rowcol(r-1,c);
		if(0!=(HOLDER & map[r][c+1]))return new Rowcol(r,c+1);
		if(0!=(HOLDER & map[r][c-1]))return new Rowcol(r,c-1);
		return null;
	}
	void wake_room(Man man, boolean entering, int row, int col){
		ItemVector v= new ItemVector(4);
		Room r= room_at(row, col);
		boolean treed= r!=null && (r.is_room & Room.R_TREE)!=0;

		// List the monsters in the room that can be seen and are asleep
		Enumeration e= level_monsters.elements();
		while(e.hasMoreElements()){
			Monster monster= (Monster)e.nextElement();
			if(0!=(monster.m_flags & Monster.ASLEEP)
			&& (treed || sees(row, col, monster.row, monster.col)))
				v.addElement(monster);
		}
		// It's a party if there are more than 4 sleepy monsters
		int wake_percent= v.size()>4 ? Monster.PARTY_WAKE_PERCENT : Monster.WAKE_PERCENT;
		if(man.stealthy > 0)
			wake_percent /= (Monster.STEALTH_FACTOR + man.stealthy);

		e= v.elements();
		while(e.hasMoreElements()){
			Monster monster= (Monster)e.nextElement();
			if(entering)
				monster.trow= -1;
			else {
				monster.trow= row;
				monster.tcol= col;
			}
			if(0!=(monster.m_flags & Monster.WAKENS)
			&& self.rand.percent(wake_percent))
				monster.wake_up();
		}
	}
	boolean same_row(Room rfr, Room rto){
		return false;
	}
	boolean same_col(Room rfr, Room rto){
		return false;
	}
	Room nabes(Room r)[]{
		Room ra[]= new Room[1];
		ra[0]= null;
		return ra;
	}
	String maps(int r, int c){
		return new Rowcol(r,c) + Integer.toString(map[r][c],16) + ' ';
	}
	void init_seen(){
		int si= ~SEEMAP;
		for(int r= 0; r<nrow; r++)
		for(int c= 0; c<ncol; c++)
			map[r][c] &= si;
	}
	void setseen(int r, int c){
		map[r][c]= (map[r][c]&~SEEMAP)|SEEN;
	}
	boolean try_to_cough(int r, int c, Toy obj){
		if(r<MIN_ROW || r>nrow-2 || c<0 || c>ncol-1)
			return false;
		if(0==(map[r][c] & (TOY | STAIRS | TRAP))
		&& 0!=(map[r][c] & (TUNNEL | FLOOR | DOOR))){
			obj.place_at(r, c, TOY);
			if(0==(map[r][c] & (MONSTER|MAN)))
				self.mark(r,c);
			return true;
		}
		return false;
	}
	void stairlevel(int goldpct, boolean partyroom){
		// put_stairs here, but not in a maze room
		Rowcol p= gr_row_col(FLOOR, null);
		// (may return null--then redo this level after error)
		map[p.row][p.col] |= STAIRS|FLOOR;

		add_traps();
		if(cur_level >= max_level){
			if(partyroom)
				make_party();
			else
				put_toys();
		}
		// Add gold (one lump per room)
		if(level_toys != null)
		for(int i= 0; i<rooms.size(); i++)
			((Room)rooms.elementAt(i)).put_gold(goldpct);
	}
	Room gr_room(){
		int perm[]= self.rand.permute(rooms.size());
		for(int i= 0; i<rooms.size(); i++){
			Room rm= (Room)rooms.elementAt(perm[i]);
			if(0!=(rm.is_room & Room.R_ROOMISH))
				return rm;
		}
		return null;
	}
	void make_party(){
		Room party_room= gr_room();
		if(party_room!=null){	
			int tries= 0;
			int n= self.rand.percent(99) ? party_room.party_toys() : 11;
			Rowcol rc= null;
			if(self.rand.percent(99))
				party_room.party_monsters(n);

			/* Put a trap in the party room */
			do {
				rc= party_room.randomrc(1);
				tries++;
			}while(0!=(map[rc.row][rc.col] & (TOY|STAIRS|TRAP|TUNNEL))
			    || (0==(map[rc.row][rc.col] & SOMETHING) && tries < 15));
			if(tries<15){
				Trap trap= new Trap(this, rc.row, rc.col, self.rand.get(Trap.TRAPS-1));
				map[rc.row][rc.col] |= HIDDEN;
			}
		}
	}
	void add_traps(){
		int i, n= 0, tries= 0;

		if(cur_level>2 && cur_level<=7)
			n= self.rand.get(2);
		else if(cur_level<=11)
			n= self.rand.get(1, 2);
		else if(cur_level<=16)
			n= self.rand.get(2, 3);
		else if(cur_level<=21)
			n= self.rand.get(2, 4);
		else if(cur_level<=AMULET_LEVEL+2)
			n= self.rand.get(3, 5);
		else
			n= self.rand.get(5, 10);	// Maximum number of traps
		for(i= 0; i < n; i++){
			Rowcol pt= gr_row_col(FLOOR | MONSTER, null);
			if(pt!=null){
				Trap trap= new Trap(this, pt.row, pt.col, self.rand.get(Trap.TRAPS-1));
				map[pt.row][pt.col] |= HIDDEN;
				//System.out.println("Trap at " + pt +  " (" + trap.m_flags + ")");
			}
		}
	}
	Room room_at(int row, int col){
		Enumeration e= rooms.elements();
		while(e.hasMoreElements()){
			Room r= (Room)e.nextElement();
			if(r.in_room(row, col))
				return r;
		}
		return null;
	}
	void gorefill(){
		Vector justfilled= new Vector(nrow+ncol);
		Rowcol rc= gr_row_col(FLOOR|TUNNEL, null);
		map[rc.row][rc.col] |= GORE;
		justfilled.addElement(rc);
		while(justfilled.size() > 0){
			Vector found= new Vector(nrow+ncol);
			Enumeration en= justfilled.elements();
			while(en.hasMoreElements()){
				rc= (Rowcol)en.nextElement();
				int di= 0 != (map[rc.row][rc.col]&(FLOOR|DOOR))? 1 : 2;
				for(int i= 0; i<8; i += di){
					try{
						Rowcol rt= new Rowcol(rc.row+Id.ytab[i], rc.col+Id.xtab[i]);
						int m= map[rt.row][rt.col];
						if(0 != (m&(FLOOR|DOOR|STAIRS|TUNNEL))){
							if(0==(m&GORE)){
								map[rt.row][rt.col]= m|GORE;
								found.addElement(rt);
							}
						}
					}catch(ArrayIndexOutOfBoundsException e){
					}
				}
				justfilled= found;
			}
		}
	}				
	boolean gory(){
		for(int r= 0; r<nrow; r++)for(int c= 0; c<ncol; c++){
			int m= map[r][c];
			if(0 != (m&(FLOOR|DOOR|STAIRS|TUNNEL)) && 0 == (m&GORE)){
				System.out.println("GORELESS "+new Rowcol(r,c));
				for(int i= 0; i<rooms.size(); i++){
					try{
					Room ro= (Room)rooms.elementAt(i);
					if(0 != (ro.is_room & (Room.R_ROOM | Room.R_MAZE))){
						System.out.println("Room"  + i + " " + ro.toString());
/*
						for(int j= 0; j<4; j++)if(ro.doors[j]!=null){
							Rowcol rc= (Rowcol)ro.doors[j];
							System.out.println("  "+rc+" to "+ro.doors[j].oth + " "+ro.doors[j]);
						}
*/
					}
					}catch(Exception e){
						e.printStackTrace();
					}
				}
				return false;
			}
		}
		return true;
	}
	boolean somethinginteresting(int row, int col, int dirch){
		int d= Id.is_direction(dirch) + (Id.DIRS-2);
		for(int i= 0; i<3; i++, d+= 2){
			Rowcol rc= get_dir_rc(d%Id.DIRS, row, col, false);
			if(rc!=null){
				int ch= get_char(rc.row, rc.col)&(~uWeak);
				if(ch>0 && 0 > " #-|.".indexOf(ch))
					return true;
			}
		}
		return false;
	}
	void unserialfix(){///Man man){
		Enumeration e= level_monsters.elements();
		while(e.hasMoreElements()){
			Monster lmon= (Monster)e.nextElement();
			lmon.self= self;
		}
//		if(level_men.size() > 0){
//			System.out.println("Man removed " + level_men.elementAt(0).toString());
//			level_men.removeElement(level_men.elementAt(0));
//		}
//		level_men.addElement(man);
	}
}
