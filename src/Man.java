import java.util.Vector;
import java.io.*;
import java.util.Date;
import java.awt.*;
class Man extends Persona implements Serializable {
	private static final long serialVersionUID= 3L;
	View view;
	Option option;
	ItemVector pack= null;
	int exp_points= 0;		// Experience points
	int moves_left= 1250;	// Food counter
	int less_hp= 0;
	int m_moves= 0;		// General move counter

	boolean sustain_strength= false;
	boolean detect_monster= false;
	//boolean passgo= true;//false;
	boolean r_teleport= false;
	boolean jump= false;
	boolean see_invisible;
	boolean game_over= false;
	boolean wizzed= false;

	int	regeneration= 0;
	boolean	r_see_invisible= false;
	boolean	maintain_armor= false;

	static final int R_TELE_PERCENT= 8;

	int auto_search= 0;		// Number of times to auto-search each turn
	int searchcount= 0;		// half-move counter for searching

	String new_level_message;
	String hunger_str= "";
	boolean trap_door;
	
	static final int SAVELEVEL= 1;	// lowest level that Save can be done 
	static final char RELOAD='\034';
	static final char WELCOME='\035';
	
	static final int MOVED= 0;
	static final int MOVE_FAILED= -1;
	static final int STOPPED_ON_SOMETHING= -2;

	static final int HUNGRY= 300;
	static final int WEAK=   150;
	static final int FAINT=   20;
	static final int STARVE=   0;

	static final int MAX_EXP_LEVEL=	21;
	static final int MAX_EXP=  10000001;
	static final int MAX_GOLD= 999999;
	static final int MAX_ARMOR= 99;
	static final int MAX_HP= 999;
	static final int MAX_STRENGTH= 99;

	Man(Rogue self, View view){
		super(self);
		mt= Monster.mon_tab[Monster.MONSTERS-1];
		ichar= (char)(mt.ichar | uRogue);
		this.self= self;
		this.option= new Option();
		this.view= view;
		hp_max= hp_current= mt.hp_current;
		str_max= str_current= 16;
		exp= 1;
	}
	void rest(int count){
		self.interrupted= false;
		do{
			reg_move();
			showcount(count);
		}while(--count>0 && !self.interrupted);
	}
	private void showcount(int n){
		String s= "   ";
		if(n>0){
			String t= Integer.toString(n);
			s= "";
			for(int i= 0; i<t.length(); i++)
				s= s+(char)(t.charAt(i)+uGray);
			if(n<100){
				s= " "+s;
				if(n<10)s= " "+s;
			}
		}
		view.addch(24, 76, s);
		view.refresh();
	}
	private int follow_passage(int dir){
		int dircase= dir&0xe0;
		boolean ok[]= new boolean[Id.DIRS];
		int ans= dir&0x1f;
		int idir= Id.is_direction(ans|0x60);
		//int flag= 0;
		if(idir>=0){
			for(int i= 0; i<Id.DIRS; i++){
				Rowcol rc= level.get_dir_rc((idir+i)%Id.DIRS, row, col, false);
				ok[i]= rc!=null && level.is_passable(rc.row,rc.col);
		//		flag <<= 1;
		//		if(ok[i])flag+= 1;
			}
			if(!ok[0]){
				String dirvector= "kulnjbhykulnjbhy";
				if(ok[2]&&!ok[6]&&!(ok[1]&&ok[3]))ans= (int)(dirvector.charAt(idir+2))&0x1f;
				if(ok[6]&&!ok[2]&&!(ok[5]&&ok[6]))ans= (int)(dirvector.charAt(idir+6))&0x1f;
			}else if((ok[2]||ok[6]) && !(ok[1]||ok[7]))
				self.interrupted= true;
		}
		ans|= dircase;
		//System.out.println("Flag="+Integer.toString(flag,16)+this+((char)(ans|0x40)));
		return ans;
	}
	/*
	private void turn_passage(int dir, boolean fast){
		int crow= row, ccol= col, turns= 0;
		int ndir= 0;

		if((dir != 'h') && level.can_turn(crow, ccol + 1)){
			turns++;
			ndir= 'l';
		}
		if((dir != 'l') && level.can_turn(crow, ccol - 1)){
			turns++;
			ndir= 'h';
		}
		if((dir != 'k') && level.can_turn(crow + 1, ccol)){
			turns++;
			ndir= 'j';
		}
		if((dir != 'j') && level.can_turn(crow - 1, ccol)){
			turns++;
			ndir= 'k';
		}
		if(turns == 1 && dir!=ndir){
			play_move(ndir - (fast ? 32 : 96),  1);
		}
	}
	*/
	boolean search(int n, boolean is_auto){
		int found= 0, shown= 0;
		self.interrupted= false;
		for(int r= row-1; r<=row+1; r++)if(r>=MIN_ROW && r<level.nrow-1)
			for(int c= col-1; c<=col+1; c++)if(c>=0 && c<level.ncol)
				if(0!=(level.map[r][c]&HIDDEN))
					++found;
		do{
			for(int r= row-1; r<=row+1; r++)if(r>=MIN_ROW && r<level.nrow-1)
			for(int c= col-1; c<=col+1; c++)if(c>=0 && c<level.ncol)
			if(0!=(level.map[r][c]&HIDDEN) && self.rand.percent(17 + exp+ring_exp)){
				level.map[r][c] &= ~HIDDEN;
				if(blind==0 && (r!=row || c!=col))
					view.mark(r, c);
				shown++;
				if(0!=(level.map[r][c]&TRAP)){
					Trap t= (Trap)level.level_traps.item_at(r, c);
					if(t!=null && t.kind<Trap.name.length)
						tell(Trap.name[t.kind], true);
					else
						System.out.println("Err in search=flag="+r+" " +c);
				}
				if(shown==found && found>0)
					return true;
			}
			/* A search is half a move */
			if(!is_auto && 0==((++searchcount)&1))
				reg_move();
			showcount(n);
		}while(--n>0  && !self.interrupted);
		return shown>0;
	}
	int play_move(int ch, int count){
		int r, c, m;
		switch(ch){
		case '.':
		case '-':
			rest(count);
			break;
		case 's':
			search(count, false);
			break;
		case 'i':
			tell("Inventory for "+option.nick_name);
			pack.inventory(Id.ALL_TOYS, view.msg, false);
			view.msg.check_message();
			break;

		case Man.WELCOME:
			tell("Welcome, " + option.nick_name);
			break;
		case 'f':
			break;
		//	fight(0);
		//	break;
		case 'F':
		//	fight(1);
			break;
 		case Event.HOME: case Event.END: case Event.PGUP: case Event.PGDN:
 			if(ch==Event.HOME)ch= 'y';
 			if(ch==Event.END)ch= 'b';
 			if(ch==Event.PGUP)ch= 'u';
 			if(ch==Event.PGDN)ch= 'n';
		case Event.DOWN: case Event.UP: case Event.RIGHT: case Event.LEFT:
			if(ch==Event.DOWN)ch= 'j';
			if(ch==Event.UP)ch= 'k';
			if(ch==Event.RIGHT)ch= 'l';
			if(ch==Event.LEFT)ch= 'h';
		case 'h':	case 'j':	case 'k':	case 'l':
		case 'y':	case 'u':	case 'n':	case 'b':
			one_move_rogue(ch, true);
			break;
		case 'H':	case 'J':	case 'K':	case 'L':
		case 'B':	case 'Y':	case 'U':	case 'N':
			while(!self.interrupted && one_move_rogue((ch + 32), true)==MOVED){
				if(option.passgo && 0!=(level.map[row][col]&TUNNEL))
					ch= follow_passage(ch);
			}
			break;
		case 'H'-'@':	case 'J'-'@':	case 'K'-'@':	case 'L'-'@':
		case 'B'-'@':	case 'Y'-'@':	case 'U'-'@':	case 'N'-'@':
			r= row; c= col;
			while(!self.interrupted	&& one_move_rogue((ch + 96), true)==MOVED){
				if(next_to_something(r,c))
					break;
				r= row; c= col;
				if(option.passgo && 0!=(level.map[row][col]&TUNNEL))
					ch= follow_passage(ch);
			}
			break;
/*
			do {
				r= row;
				c= col;
				m= one_move_rogue(ch + 96, true);
				if(m==MOVE_FAILED || m==STOPPED_ON_SOMETHING || self.interrupted)
					break;
			} while(!next_to_something(r, c));
			if(!self.interrupted && option.passgo && m==MOVE_FAILED && 0!=(level.map[row][ col]&TUNNEL))
				turn_passage(ch + 96, false);
			break;
*/
		case 'e':
			eat();
			break;
		case 'q':
			quaff();
			break;
		case 'r':
			read_scroll();
			break;
		case 'm':
			move_onto();
			break;
		case ',':
			kick_into_pack();
			break;
		case 'd':
			drop();
			break;
		case 'P':
			put_on_ring();
			break;
		case 'R':
			remove_ring();
			break;
		case 'P'-'@':	/* Print old messages */
			do {
				view.msg.remessage(count++);
				ch= self.rgetchar();
			} while(ch == 'P'-'@');
			view.msg.check_message();
			count= play_move(ch, 0);
			break;
		case 'W'-'@':
			tell((wizard= !wizard)? "Welcome, wizard!" : "not wizard anymore");
			wizzed= true;
			Id.wizard_identify();
			break;
		case 'R'-'@':
			view.repaint(30);
			break;
		case '>':
			if(wizard)
				return -1;
			if(0!=(level.map[row][ col]&STAIRS)){
				if(levitate!=0)
					tell("you're floating in the air!");
				else
					return -1;
			}
			return 0;
		case '<':
			if(!wizard){
				if(0==(level.map[row][col]&STAIRS)){
					tell("I see no way up");
					return 0;
				}
				if(!has_amulet()){
					tell("your way is magically blocked");
					return 0;
				}
			}
			new_level_message= "you feel a wrenching sensation in your gut";
			if(level.cur_level == 1)
				win();
			else
				level.cur_level -= 2;
			return -1;
		case ')':
			tell(weapon==null? "not wielding anything":pack.single_inv(weapon.ichar));
			break;
		case ']':
			tell(armor==null? "not wearing anything":pack.single_inv(armor.ichar));
			break;
		case '=':
			if(left_ring==null && right_ring==null)
				tell("not wearing any rings");
			if(left_ring!=null)
				tell(pack.single_inv(left_ring.ichar));
			if(right_ring!=null)
				tell(pack.single_inv(right_ring.ichar));
			break;
		case '^':
			id_trap();
			break;
		case '/':
			Id.id_type(this);
			break;
		case '?':
			//if(wizard)
			//	wiz_cmds();
			//else
				id_com();
			break;
		case '!':
		//	do_shell();
			break;
		case 'o':
			Option o= option.edit_opts(this);
			if(o != null){
				if(!option.nick_name.equals(o.nick_name)
				|| !option.fruit.equals(o.fruit)){
					//self.setcookie(o);
					self.pushkey(Man.RELOAD);
				}
				option= self.optsav= o;
			}else
				tell("Options were not modified.");
			///option.edit_opts(this);
			break;
		case 'I':
		//	single_inv(0);
			break;
		case 'T':
			take_off();
			break;
		case 'W':
			wear();
			break;
		case 'w':
			wield();
			break;
		case 'c':
			call_it();
			break;
		case 'z':
			zapp();
			break;
		case 't':
			throw_missile();
			break;
		case 'v':
			tell("rogue-clone: in java", false);
			break;
		case 'Q':
			//if(option.ask_quit){
				if(!view.msg.yes_or_no("Really quit?"))
					break;
			//}
			killed_by(null, Monster.QUIT);
			break;
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
			view.refresh();
			do {
				count= (10*count + ch - '0')%1000;
				showcount(count);
				ch= self.rgetchar();
			} while('0'<=ch && ch<='9');
			if(ch != '\033')
				count= play_move(ch, count);
			showcount(0);
			break;
		case ' ':
			break;
		case '\001':
		//	show_average_hp();
			break;
		case 'S':
			if(level.max_level<SAVELEVEL)
				view.msg.message("You must be past level "+Integer.toString(SAVELEVEL-1)+" to save", false);
			else if(option.nick_name==null || 0==option.nick_name.compareToIgnoreCase("rogue"))
				view.msg.message("Please choose another name for yourself. Rogue is so COMMON.", false);
			else if(view.msg.yes_or_no("Really save?"))
				return -1;
			break;
		case 'g':
			//printseen();
			break;
		case 'G':
			//if(view.msg.yes_or_no("Restore the saved dungeon for "+option.nick_name+ "?"))
			//	return -1;
			break;
		case RELOAD:
			return -1;
		default:
			if(!wizard)
				ch= 0;
			switch(ch){
			case '@':
				System.out.println(this.toString());
				for(int i= row-1; i<=row+1; i++)try{
					for(int j= col-1; j<=col+1; j++)
						System.out.print(Integer.toString(07000000+level.map[i][j],8) + view.buffer[i][j] + " ");
					for(int j= col-1; j<=col+1; j++)
						System.out.print(Integer.toString(level.map[i][j]&3));
					System.out.println("");
				}catch(Exception e){
					System.out.println("-------? ");
				}
				view.msg.message("At " + ((Rowcol)this) + " Food="+moves_left+" blind="+blind+ " regen="+regeneration, false);
				break;
			// PRW HJKLYUNM control characters are used in the game
			case 'A'-'@':
				System.out.println("Room "+level.room_at(row,col));
				break;
			case 'Z'-'@':
				if(count>1)
					level.show_toys(this);
				else{
					System.out.println("Toys:");
					for(int i= 0; i<level.level_toys.size(); i++)
						System.out.println(level.level_toys.elementAt(i));
					///level.level_toys.inventory(Id.ALL_TOYS, view.msg, false);
				}
				break;
			case 'C'-'@':
				c_toy_for_wizard(count); break;
			case 'D'-'@':
				if(count>1)
					level.show_monsters(this);
				else{
					System.out.println("Monsters:");
					for(int i= 0; i<level.level_monsters.size(); i++)
						System.out.println(level.level_monsters.elementAt(i));
				}
				break;
			case 'E'-'@':
				if(count>1)
					level.show_traps(this);
				else{
					System.out.println("Traps:");
					for(int i= 0; i<level.level_traps.size(); i++)
						System.out.println(level.level_traps.elementAt(i));
				}
				break;
			case 'F'-'@':
				level.cur_level+= 19; return -1;	// plummet
			case 'G'-'@':	// nothing here
				break;
			case 'I'-'@':
				level.unhide(); break;
			case 'O'-'@':
				level.wanderer(); break;
			case 'Q'-'@':
				break;
			case 'S'-'@':
				level.draw_magic_map(this); break;
			case 'T'-'@':
				tele(); break;
			case 'X'-'@':
				monster_for_wizard(); break;
			default:
				tell("unknown_command"); break;
			}
		}
		return count;
	}
	int play_level(){
		int ch= 0;
		int count= 0;
		showmap();
		do{
			try{
				self.interrupted= false;
				if(hit_message.length()>1){
					tell(hit_message);
					hit_message= "";
				}
				if(trap_door){
					trap_door= false;
					break;
				}
				showmap();
				view.refresh();

				ch= self.rgetchar();
				if(!game_over)
					view.msg.check_message();
				count= play_move(ch, 0);
			}catch(Exception e){
				if(e.getMessage()!=null)
					System.out.println(e.getMessage());
				e.printStackTrace();
			}
			System.gc();
		}while(count>=0 && !game_over);
		return ch;	// the last command character
	}
 	char showrc(int r, int c){	// What man sees at this position
		char ch= 0;

		if(r==row && c==col)
			return ichar;
		int mask= level.map[r][c];
		if(can_see(r,c)){
			if(0!=(mask&DARK)){
				// this should be omitted
				if(r<row-1 || r>row+1 || c<col-1 || c>col+1)
					return ' ';
			}
			if(0!=(mask&MONSTER)){
				Monster monster= (Monster)level.level_monsters.item_at(r, c);
				ch= (char)(monster!=null? monster.gmc(this) : '$');
			}else if(0!=(mask&TOY)){
				if(halluc==0){
					Toy t= (Toy)level.level_toys.item_at(r,c);
					if(t==null)
						System.out.println("See no toy at " + new Rowcol(r,c));
					else
						ch= t.ichar;
				}else
					ch= (char)Id.gr_obj_char(self.rand);
			}else
				ch= level.get_char(r, c);
		}else if(detect_monster && 0!=(mask&MONSTER) && blind==0){
			Monster monster= (Monster)level.level_monsters.item_at(r, c);
			ch= (char)(monster!=null? monster.gmc(this) : '$');
			if(self.rand.percent(30))
				detect_monster= false;
		}else if(0!=(level.map[r][c]&SEEMAP)){
			/* displaying from the mapped stuff or when blind */
			ch= level.get_bgchar(r,c);
			if((DOOR|HIDDEN)==(mask&(DOOR|HIDDEN))){
				/* if a 'seen' wall is near a hidden door, mark it as a wall
				   otherwise it is hidden (a space)
				 */
				try{
					if((ch==HORWALL && 0==(SEEMAP&level.map[r][c-1]) && 0==(SEEMAP&level.map[r][c+1]))
					|| (ch==VERTWALL&& 0==(SEEMAP&level.map[r-1][c]) && 0==(SEEMAP&level.map[r+1][c])))
						ch= ' ';
				}catch(ArrayIndexOutOfBoundsException e){
					ch= ' ';
				}
			}
			if(blind>0 && FLOOR==(level.map[r][c]&(STAIRS|FLOOR)))
				ch= ' ';
		}else
			ch= ' ';
		return ch;
	}
	void showmap(){
		Rowcol pt;
		preshow();
		view.setmarked(this);
	}
	boolean check_hunger(boolean msg_only){
		int i, n;
		boolean fainted= false;

		if(moves_left == HUNGRY){
			hunger_str= "hungry";
			tell("you feel "+hunger_str);
			print_stat();
		}
		if(moves_left == WEAK){
			hunger_str= "weak  ";
			tell("you feel "+hunger_str, true);
			print_stat();
		}
		if(moves_left <= FAINT){
			if(moves_left == FAINT){
				hunger_str= "faint ";
				tell("you "+hunger_str, true);
				print_stat();
			}
			n= self.rand.get(FAINT - moves_left);
			if(n > 0){
				fainted= true;
				if(self.rand.percent(40))
					moves_left++;
				tell("you faint", true);
				for(i= 0; i < n; i++)
					if(self.rand.coin())
						level.mv_mons(this);
				tell("you can move again", true);
			}
		}
		if(msg_only)
			return fainted;
		if(moves_left <= STARVE){
			killed_by(null, Monster.STARVATION);
			return false;
		}
		switch(e_rings){
		case -1:
			moves_left -= (moves_left % 2);
			break;
		case 0:
			moves_left--;
			break;
		case 1:
			moves_left--;
			check_hunger(true);
			moves_left -= (moves_left % 2);
			break;
		case 2:
			moves_left--;
			check_hunger(true);
			moves_left--;
			break;
		}
		return fainted;
	}
	boolean reg_move(){
		boolean fainted= false;

		if((moves_left <= HUNGRY) || level.cur_level >= level.max_level)
			fainted= check_hunger(false);
		level.mv_mons(this);

		if(++m_moves >= 120){
			m_moves= 0;
			level.wanderer();
		}
		super.reg_move();
		heal();

		if(auto_search > 0)
			search(auto_search, true);
		return fainted;
	}
	void move_onto(){
		int ch;
		if(-2 == Id.is_direction(ch= self.rgetchar())){
			tell("direction? ");
			ch= self.rgetchar();
		}
		view.msg.check_message();
		if(ch != '\033')
			one_move_rogue(ch, false);
	}
	int one_move_rogue(int dirch, boolean pickup){
		if(confused != 0)
			dirch= mov_confused();
		int d= Id.is_direction(dirch);
		Rowcol pto= level.get_dir_rc(d, row, col, true);

		if(!level.can_move(row, col, pto.row, pto.col)){
			if(blind>0)try{
				level.setseen(pto.row, pto.col);
				view.mark(pto.row,pto.col);
			}catch(ArrayIndexOutOfBoundsException e){
			}
			return MOVE_FAILED;
		}
		if(being_held || bear_trap>0){
			if(0==(level.map[pto.row][pto.col]&MONSTER)){
				if(being_held)
					tell("you are being held", true);
				else{
					tell("you are still stuck in the bear trap");
					reg_move();
				}
				return MOVE_FAILED;
			}
		}
		if(r_teleport){
			if(self.rand.percent(R_TELE_PERCENT)){
				tele();
				return STOPPED_ON_SOMETHING;
			}
		}
		if(0!=(level.map[pto.row][pto.col]&MONSTER)){
			Monster monster= (Monster)level.level_monsters.item_at(pto.row, pto.col);
			if(monster!=null)
				rogue_hit(monster, false);
			reg_move();
			return MOVE_FAILED;
		}
		if(0!=(level.map[pto.row][pto.col]&DOOR)
		&& 0!=(level.map[row][col]&TUNNEL))
			level.wake_room(this, true, pto.row, pto.col);
		else if(0!=(level.map[pto.row][ pto.col]&TUNNEL)
		&& 0!=(level.map[row][col]&DOOR))
			level.wake_room(this, false, row, col);
//////////////////////////////////////////////////
		if(blind==0){	// Basic tunnel view
			for(int r= row-1; r<=row+1; r++)
			for(int c= col-1; c<=col+1; c++)
////			if(0!=(level.map[r][c]&TUNNEL) && 0==(level.map[r][c]&HIDDEN))
					view.mark(r,c);
		}
		place_at(pto.row, pto.col, MAN);	// Note--sets row,col to pto
		if(blind==0){	// Basic tunnel view
			for(int r= row-1; r<=row+1; r++)
			for(int c= col-1; c<=col+1; c++){
////			if(0!=(level.map[r][c]&TUNNEL) && 0==(level.map[r][c]&HIDDEN))
					view.mark(r,c);
				if(0!=(level.map[r][c]&STAIRS))
					level.map[r][c] &= ~DARK;
			}
		}
		if(!jump){
			showmap();
			view.refresh();
		}
//////////////////////////////////////////////////
		Toy obj= null;
		boolean sos= false;	// Stopped on something
		if(0!=(level.map[row][ col]&TOY)){
			if(levitate>0 && pickup)
				return STOPPED_ON_SOMETHING;
			if(pickup && 0==levitate)
				obj= pick_up();
			if(obj==null){
				obj= (Toy)level.level_toys.item_at(row, col);
				if(obj!=null)
					tell("moved onto " + obj.get_desc());
			}else if(obj.ichar==1)	// Not a dusted scroll
				return STOPPED_ON_SOMETHING;
			sos= true;
		}
		if(0 != (level.map[row][col] & (level.DOOR | level.STAIRS | level.TRAP))){
			if(levitate==0 && 0 != (level.map[row][col] & level.TRAP))
				trap_player();
			sos= true;
			self.interrupted= true;
		}
		return (reg_move()		/* fainted from hunger */
			|| sos				/* already on something */
			|| confused != 0)
			? STOPPED_ON_SOMETHING : MOVED;
	}
	private boolean next_to_something(int drow, int dcol){
		int pass_count= 0;
		int s;

		if(confused != 0)
			return true;
		if(blind>0)
			return false;
		int i_end= (row < (level.nrow-2)) ? 1 : 0;
		int j_end= (col < (level.ncol-1)) ? 1 : 0;

		for(int i= row > MIN_ROW ? -1 : 0; i <= i_end; i++){
			for(int j= col > 0 ? -1 : 0; j <= j_end; j++){
				if(i == 0 && j == 0)
					continue;
				if(row+i==drow && col+j==dcol)
					continue;
				int r= row + i;
				int c= col + j;
				s= level.map[r][c];
				if(0 != (s & HIDDEN))
					continue;

				/* If the rogue used to be right, up, left, down, or right of
				 * r,c, and now isn't, then don't stop */
				if(0 != (s & (MONSTER | TOY | STAIRS))){
					if((r == drow || c == dcol)	&& !(r==row || c==col))
						continue;
					return true;
				}
				if(0!= (s & TRAP)){
					if((r == drow || c==dcol) && !(r==row || c==col))
						continue;
					return true;
				}
				if((i-j==1 || i-j==-1) && 0!=(s & TUNNEL)){
					if(++pass_count > 1)
						return true;
				}
				if(0!=(s & DOOR) && (i==0||j==0))
						return true;
			}
		}
		return false;
	}
	Trap trap_player(){	// Call with the trap list
		level.map[row][col] &= ~HIDDEN;
		if(self.rand.percent(exp+ring_exp)){
			tell("the trap failed", true);
			return null;
		}
		Trap t= (Trap)level.level_traps.item_at(row, col);
		if(t==null)
			return null;
		switch(t.kind){
		case Trap.BEAR_TRAP:
			tell(t.trap_message(this), true);
			bear_trap= self.rand.get(4, 7);
			t= null;
			break;
		case Trap.TRAP_DOOR:
			trap_door= true;
			new_level_message= t.trap_message(this);
			break;
		case Trap.TELE_TRAP:
			view.mark(row, col);
			tele();
			break;
		case Trap.DART_TRAP:
			tell(t.trap_message(this), true);
			hp_current -= Id.get_damage("1d6", self.rand);
			if(hp_current <= 0)
				hp_current= 0;
			if(!sustain_strength && self.rand.percent(40) &&	str_current >= 3)
				str_current--;
			print_stat();
			if(hp_current <= 0)
				killed_by(null, Monster.POISON_DART);
			break;
		case Trap.SLEEPING_GAS_TRAP:
			tell(t.trap_message(this), true);
			take_a_nap();
			break;
		case Trap.RUST_TRAP:
			tell(t.trap_message(this), true);
			rust(null);
			break;
		}
		return t;
	}
	void take_a_nap(){
		int i= self.rand.get(2, 5);
		self.md_sleep(1000);
		while(--i>=0){
			level.mv_mons(this);
		}
		self.md_sleep(1000);
		tell("you can move again");
	}
	// Format an integer (print in minwidth spaces)
	static String ifmt(int i, int minwidth){
		String s= new Integer(i).toString();
		while(s.length()<minwidth)
			s= new String(" ").concat(s);
		return s;
	}
/*
Level: 99 Gold: 999999 Hp: 999(999) Str: 99(99) Arm: 99 Exp: 21/10000000 Hungry
0    5    1    5    2    5    3    5    4    5    5    5    6    5    7    5
*/
	String stat_string(){
		if(hp_max > MAX_HP){
			hp_current -= hp_max - MAX_HP;
			hp_max= MAX_HP;
		}
		if(str_max > MAX_STRENGTH){
			str_current -= str_max - MAX_STRENGTH;
			str_max= MAX_STRENGTH;
		}
		int armorclass= 0;
		if(armor!=null){
			if(armor.d_enchant>MAX_ARMOR)
				armor.d_enchant= MAX_ARMOR;
			armorclass= armor.get_armor_class();
		}
		if(exp_points > MAX_EXP)
			exp_points= MAX_EXP;
		if(exp > MAX_EXP_LEVEL)
			exp= MAX_EXP_LEVEL;
		return "Level: " + ifmt(level.cur_level,2)
		 + " Gold: " + ifmt(gold, 6)
		 + " Hp: " + ifmt(hp_current,3) + '(' + ifmt(hp_max,3)
		 + ") Str: " + ifmt(str_current,2) + '(' + ifmt(str_max,2)
		 + ") Arm: " + ifmt(armorclass, 2)
		 + " Exp: " + ifmt(exp,2) + '/' + ifmt(exp_points,8)
		 + " " + hunger_str;
	}
	void print_stat(){
		view.addch(level.nrow-1, 0, stat_string());
	}
	void drop(){
		Toy obj;
		int ch;

		if(0 != (level.map[row][col] & (TOY | STAIRS | TRAP))){
			tell("there's already something there");
			return;
		}
		if(null==pack){
			tell("you have nothing to drop");
			return;
		}
		ch= pack_letter("drop what?", Id.ALL_TOYS);
		if(ch=='\033')
			return;
		obj= (Toy)pack.get_letter_toy(ch);
		if(obj==null){
			tell("no such item.");
			return;
		}
		if(obj.kind == Id.SCARE_MONSTER)
			obj.picked_up= true;
		obj.drop();
	}
	private int is_pack_letter(int c){
		switch(c){
		case '?':	return Id.SCROLL;
		case '!':	return Id.POTION;
		case ':':	return Id.FOOD;
		case ')':	return Id.WEAPON;
		case ']':	return Id.ARMOR;
		case '/':	return Id.WAND;
		case '=':	return Id.RING;
		case ',':	return Id.AMULET;
		default:	break;
		}
		return 0;
	}
	int pack_letter(String prompt, int mask){
		int ch;

		if(!pack.mask_pack(mask)){
			tell("nothing appropriate");
			return '\033';
		}
		tell(prompt);
		ch= self.rgetchar();
		while((ch<'a' || ch>'z') && ch!='\033'){
			int m= mask;
			if(ch=='*' || m==0)
				m= Id.ALL_TOYS;
			ch= pack.inventory(m, view.msg, true);
		}
		view.msg.check_message();
		return ch;
	}
	void take_off(){
		if(armor!=null){
			if(armor.is_cursed){
				tell(Toy.curse_message);
			} else {
				level.mv_aquatars(this);
				Toy obj= armor;
				unwear();
				tell("was wearing " + obj.get_desc());
				print_stat();
				reg_move();
			}
		} else {
			tell("not wearing any");
		}
	}
	void wear(){
		if(armor!=null){
			tell("your already wearing some");
			return;
		}
		int ch= pack_letter("wear what?", Id.ARMOR);
		if(ch == '\033')
			return;
		Toy obj= (Toy)pack.get_letter_toy(ch);
		if(null == obj){
			tell("no such item.");
			return;
		}
		if(0==(obj.kind & Id.ARMOR)){
			tell("you can't wear that");
			return;
		}
		obj.identified= true;
		tell("wearing " + obj.get_desc());
		do_wear(obj);
		print_stat();
		reg_move();
	}
	void wield(){
		if(weapon!=null && weapon.is_cursed){
			tell(Toy.curse_message);
			return;
		}
		int ch= pack_letter("wield what?", Id.WEAPON);
		if(ch == '\033')
			return;
		Toy obj= (Toy)pack.get_letter_toy(ch);
		if(obj == null){
			if(ch=='-' && weapon!=null)
				unwield();
			else
				tell("No such item.");
			return;
		}
		if(0 != (obj.kind & (Id.ARMOR | Id.RING))){
			tell("you can't wield " + (0!=(obj.kind & Id.ARMOR) ? "armor" : "rings"));
			return;
		}
		if(0 != (obj.in_use_flags & Id.BEING_WIELDED)){
			tell("in use");
		} else {
			unwield();
			tell("wielding " + obj.get_desc());
			do_wield(obj);
			reg_move();
		}
	}
	Toy find(int mask, String prompt, String fail){
		int ch= pack_letter(prompt, mask);
		if(ch=='\033')
			return null;
		view.msg.check_message();
		Toy t= (Toy)pack.get_letter_toy(ch);
		if(t==null){
			tell("no such item.");
			return null;
		}
		if(0==(t.kind & mask)){
			tell(fail);
			return null;
		}
		return t;
	}
	void call_it(){
		Toy obj= find(Id.SCROLL | Id.POTION | Id.WAND | Id.RING,
			"call what?","surely you already know what that's called");
		if(obj==null)
			return;
		Id id_table[]= Id.get_id_table(obj);
		String buf= view.msg.get_input_line("call it:","",id_table[obj.kind&255].title,true,true);
		if(buf != null){
			id_table[obj.kind&255].id_status= Id.CALLED;
			id_table[obj.kind&255].title= buf;
		}
	}
	void kick_into_pack(){
		if(0==(level.map[row][col] & TOY)){
			tell("nothing here");
		}else{
			Toy obj= pick_up();
			if(obj!=null && obj.ichar!=1)	// Not a dusted scroll
				reg_move();
		}
	}
	void monster_for_wizard(){
		tell("type of monster? ");
		int ch= self.rgetchar();
		view.msg.check_message();
		if(ch == '\033' || ch<'A' || ch>'Z')
			return;
		Monster m= new Monster(level, ch-'A');
		int r= row, c= col-2;
		if(0 != (level.map[r][c]&(Level.FLOOR|Level.TUNNEL)))
			m.put_m_at(row, col-2);
		else
			tell("cannot put monster there!");
	}
	void c_toy_for_wizard(int count){
		if(count<=0)
			count= 1;
		tell("type of object? ");
		int ch= self.rgetchar();
		view.msg.check_message();
		if(ch == '\033')
			return;
		Toy obj= null;
		do{
			obj= level.wiztoy(count==1? this:null, ch);
			if(null==obj || null==(obj= obj.add_to_pack(this)))
				break;
		}while(--count>0);
		tell(null != obj? "Wizard got "+obj.get_desc() : "Wizard failed");
	}
	Toy pick_up(){
		Toy obj= (Toy)level.level_toys.item_at(row, col);
		if(obj==null){
			tell("pick_up(): inconsistent", true);
			return null;
		}
		if(levitate>0){
			tell("you're floating in the air!");
			return null;
		}
		if(!obj.packcanhold(pack)){
			tell("pack too full", true);
			return null;
		}
		level.map[row][col] &= ~TOY;
		level.level_toys.removeElement(obj);

		if(obj.kind == Id.SCARE_MONSTER	&&	obj.picked_up){
			tell("the scroll turns to dust as you pick it up");
			if(Id.id_scrolls[Id.SCARE_MONSTER&255].id_status == Id.UNIDENTIFIED)
				Id.id_scrolls[Id.SCARE_MONSTER&255].id_status= Id.IDENTIFIED;
			obj.ichar= 1;	// Flag the dusted scroll
			return obj;
		}
		if(obj.kind == Id.GOLD){
			gold += obj.quantity;
			tell(obj.get_desc(), true);
			print_stat();
		}else{
			obj= obj.add_to_pack(this);
			if(obj != null){
				obj.picked_up= true;
				tell(obj.get_desc()+" ("+((char)obj.ichar)+")", true);
			}
		}
		return obj;
	}
	static final int level_points[]= {
	  10, 20, 40, 80, 160, 320, 640, 1300, 2600, 5200, 10000,
       20000, 40000, 80000, 160000, 320000, 1000000, 3333333,
     6666666, MAX_EXP, 99900000};

	int hp_raise(){
		return wizard ? 10 : self.rand.get(3, 10);
	}
	static int get_exp_level(int e){
		int i;
		for(i= 0; i < level_points.length; i++)
			if(level_points[i] > e)
				break;
		return i+1;
	}
	void add_exp(int e, boolean promotion){
		exp_points += e;
		if(exp_points >= level_points[exp-1]){
			int new_exp= get_exp_level(exp_points);
			if(exp_points > MAX_EXP)
				exp_points= MAX_EXP + 1;
			for(int i= exp+1; i <= new_exp; i++){
				tell("welcome to level " + i);
				if(promotion){
					int hp= hp_raise();
					hp_current += hp;
					hp_max += hp;
				}
				exp= i;
				print_stat();
			}
		} else
			print_stat();
	}
	void eat(){
		Toy obj= find(Id.FOOD, "eat what?","you can't eat that");
		if(obj != null){
			obj.eatenby();
			reg_move();
		}
	}
	void quaff(){
		Potion obj= (Potion)find(Id.POTION, "quaff what?", "you can't drink that");
		if(obj != null){
			obj.quaffby();
			reg_move();
		}
	}
	void read_scroll(){
		Scroll obj= (Scroll)find(Id.SCROLL, "read what?", "you can't read that");
		if(obj != null){
			obj.readby();
			if(obj.kind != Id.SLEEP)
				reg_move();
		}
	}
	void put_on_ring(){
		if(left_ring!=null && right_ring!=null){
			tell("wearing two rings already");
			return;
		}
		Toy obj= (Toy)find(Id.RING, "put on what?", "that's not a ring");
		if(obj==left_ring || obj==right_ring){
			tell("that ring is already being worn");
			return;
		}
		if(obj != null){
			int ch= 'r';
			if(left_ring==null){
				ch= 'l';
				if(right_ring==null){
					ch= view.msg.left_or_right();
					if(ch==0){
						view.msg.check_message();
						return;
					}
				}
			}
			if(ch=='l'){
				obj.in_use_flags |= Id.ON_LEFT_HAND;
				left_ring= obj;
			}else{
				obj.in_use_flags |= Id.ON_RIGHT_HAND;
				right_ring= obj;
			}
			ring_stats(true);
			view.msg.check_message();
			tell(obj.get_desc());
			reg_move();
		}
	}
	void remove_ring(){
		Toy obj= right_ring;
		if(left_ring!=null && right_ring!=null){
			int ch= view.msg.left_or_right();
			if(ch==0){
				view.msg.check_message();
				return;
			}
			if(ch=='l')
				obj= left_ring;
		}else if(left_ring!=null)
			obj= left_ring;
		else if(right_ring==null){
			tell("there's no ring on that hand");
			return;
		}
		if(obj.is_cursed){
			tell(Toy.curse_message);
		}else{
			obj.un_put_on();
			tell("removed " + obj.get_desc());
			ring_stats(true);
		}
	}
	void tele(){
		level.put_player(this);
		being_held= false;
		bear_trap= 0;
	}
	void ring_stats(boolean pr){
		stealthy= 0;
		r_rings= 0;
		e_rings= 0;
		ring_exp= 0;
		r_teleport= false;
		sustain_strength= false;
		add_strength= 0;
		regeneration= 0;
		r_see_invisible= false;
		maintain_armor= false;
		auto_search= 0;

		for(int i= 0; i < 2; i++){
			Toy ring= null;
			if(i==0 && left_ring!=null)
				ring= left_ring;
			else if(i==1 && right_ring!=null)
				ring= right_ring;
			else continue;
			r_rings++;
			e_rings++;
			switch(ring.kind){
			case Id.STEALTH:			stealthy++; break;
			case Id.R_TELEPORT:			r_teleport= true; break;
			case Id.REGENERATION:		regeneration++; break;
			case Id.SLOW_DIGEST:		e_rings -= 2; break;
			case Id.ADD_STRENGTH:		add_strength += ring.klass; break;
			case Id.SUSTAIN_STRENGTH:	sustain_strength= true; break;
			case Id.DEXTERITY:			ring_exp += ring.klass; break;
			case Id.ADORNMENT:			break;
			case Id.R_SEE_INVISIBLE:	r_see_invisible= true; break;
			case Id.MAINTAIN_ARMOR:		maintain_armor= true; break;
			case Id.SEARCHING:			auto_search += 2; break;
			}
		}
		if(pr){
			print_stat();
			view.markall();	// relight
		}
	}
	static final int healtab[]= {2,20,18,17,14,13,10,9,8,7,4,3,2};
	int c_heal= 0;
	boolean b_heal_alt= false;
	void heal(){
		if(hp_current == hp_max){
			c_heal= 0;
			return;
		}
		int n= exp < healtab.length? healtab[exp] : 2;
		if(++c_heal >= n){
			hp_current++;
			if(b_heal_alt= !b_heal_alt)
				hp_current++;
			c_heal= 0;
			hp_current += regeneration;
			if(hp_current > hp_max)
				hp_current= hp_max;
			print_stat();
		}
	}
	void zapp(){
		int d= view.msg.kbd_direction();
		if(d<0)
			return;
		Toy wand= find(Id.WAND, "zap with what?", "you can't zap with that");
		if(wand == null)
			return;
		if(wand.klass <= 0){
			tell("nothing happens");
		} else {
			wand.klass--;
			if((wand.kind == Id.LIGHTSTICK)){
				if(blind==0){
					Scroll s= new Scroll(level);
					s.owner= this;
					s.kind= Id.LIGHT;
					s.readby();
				}
			}else if((wand.kind == Id.COLD) || (wand.kind == Id.FIRE)){
				level.bounce(wand, d, row, col, 0);
				view.markall();	// relight
			} else {
				Monster monster= level.get_zapped_monster(d, row, col);
				if(monster != null){
					if(wand.kind == Id.DRAIN_LIFE){
						level.wdrain_life(this, monster);
					} else if(monster!=null){
						monster.wake_up();
						monster.s_con_mon(this);
						monster.zap_monster(this, wand.kind);
						view.markall();	// relight
					}
				}
			}
		}
		reg_move();
	}
	void wiz_cmds(){
		String desc[]= new String[14];
		desc[0]= "Wizard Commands";
		desc[1]= " @  Location/State";
		desc[2]= "^A  Current Room";
		desc[3]= "^C  Get Toy for Wizard";
		desc[4]= "^D  Level Monsters (2=show)";
		desc[5]= "^E  Level Traps (2=show)";
		desc[6]= "^F  Drop 20 Levels";
		desc[7]= "^G  Unas.";
		desc[8]= "^I  Unhide Level";
		desc[9]= "^O  Launch Wanderer";
		desc[10]= "^S  Show Map";
		desc[11]= "^T  Teleport";
		desc[12]= "^X  Monster for Wizard";
		desc[13]= "^Z  Level Toys (2=show)";
		view.msg.rightlist(desc, false);
	}
	void id_com(){
		view.msg.check_message();
		tell("Character you want help for(* for all):");
		int ch= self.rgetchar();
		view.msg.check_message();
		Identifychar.cmds_list((char)ch, view.msg);
		view.markall();	// relight
	}
	boolean has_amulet(){
		return pack.mask_pack(Id.AMULET);
	}
	void player_init(){
		pack= new ItemVector(26);
		level.get_food(true).add_to_pack(this);

		Toy obj= level.gr_armor();
		obj.kind= Id.RINGMAIL;
		obj.klass= (Id.RINGMAIL&255)+2;
		obj.d_enchant= 1;
		obj.is_cursed= false;
		obj.identified= true;
		obj.add_to_pack(this);
		do_wear(obj);

		obj= level.gr_weapon(Id.MACE);
		obj.hit_enchant= obj.d_enchant= 1;
		obj.identified= true;
		obj.add_to_pack(this);
		obj.is_cursed= false;
		do_wield(obj);

		obj= level.gr_weapon(Id.BOW);
		obj.damage= "1d2";
		obj.hit_enchant= 1;
		obj.d_enchant= 0;
		obj.identified= true;
		obj.is_cursed= false;
		obj.add_to_pack(this);

	   	obj= level.gr_weapon(Id.ARROW);
		obj.quantity= self.rand.get(25, 35);
		obj.hit_enchant= 0;
		obj.d_enchant= 0;
		obj.identified= true;
		obj.is_cursed= false;
		obj.add_to_pack(this);
	}
	void id_trap(){
		view.msg.check_message();
		tell("direction? ");
		int d= Id.is_direction(self.rgetchar());
		view.msg.check_message();
		if(d<0)
			return;
		Rowcol pt= level.get_dir_rc(d, row, col, false);
		int r= pt.row;
		int c= pt.col;
		Trap t;
		if(0!=(level.map[r][c]&TRAP) && 0==(level.map[r][c]&HIDDEN)
		&& (t= (Trap)level.level_traps.item_at(r, c))!=null)
			tell(Trap.name[t.kind]);
		else
			tell("no trap there");
	}
	void throw_missile(){
		int dir= view.msg.kbd_direction();
		if(dir<0)
			return;
		int wch= pack_letter("throw what?", Id.WEAPON);
		if(wch=='\033')
			return;
		view.msg.check_message();
		Toy missile= (Toy)pack.get_letter_toy(wch);
		if(missile==null){
			tell("no such item.");
			return;
		}
		if(0!=(missile.in_use_flags & Id.BEING_USED) && missile.is_cursed){
			tell(Toy.curse_message);
			return;
		}
		missile.owner= this;
		missile.thrownby(dir);
		reg_move();
	}
	boolean rogue_is_around(int r, int c){
		r -= row;
		c -= col;
		return r>=-1 && r<=1 && c>=-1 && c<=1;
	}
	void rogue_hit(Monster monster, boolean force_hit){
		if(monster.check_imitator()){
			if(blind==0){
				view.msg.check_message();
				tell("wait, that's a " + monster.name() + '!');
			}
			return;
		}
		int hit_chance= 100;
		if(!force_hit)
			hit_chance= get_hit_chance(weapon);
		if(wizard)
			hit_chance *= 2;
		if(!self.rand.percent(hit_chance)){
			if(null==ihate)
				hit_message += who("miss","misses")+" ";
		}else{
			int dmg= get_weapon_damage(weapon);
			if(wizard)
				dmg *= 3;
			if(con_mon)
				monster.s_con_mon(this);
			if(monster.damage(this, dmg, 0)){	/* still alive? */
				if(null==ihate)
					hit_message += who("hit")+" ";
			}
		}
		monster.check_gold_seeker();
		monster.wake_up();
	}
	boolean damage(Persona monster, int d, int other){
		if(d >= hp_current){
			hp_current= 0;
			print_stat();
			killed_by(monster, other);
			return true;
		}
		if(d > 0){
			if(self.rand.percent(self.gorepct) && hp_current*8<hp_max)
				level.map[row][col] |= GORE;
			self.flashadd(row, col, uRed);
			hp_current -= d;
			print_stat();
			if(hp_current<=hp_max/8 && monster!=null)
				monster.gloat(this);
		}
		return false;
	}
	void fight(boolean to_the_death){
		int ch;
		if(-2 == Id.is_direction(ch= self.rgetchar())){
			tell("direction? ");
			ch= self.rgetchar();
		}
		view.msg.check_message();
		if(ch == '\033')
			return;
		int d= Id.is_direction(ch);
		Rowcol pt= level.get_dir_rc(d, row, col, false);

		int c= view.charat(pt.row, pt.col);
		if(c<'A' || c>'Z' || !level.can_move(row, col, pt.row, pt.col)){
			tell("I see no monster there");
			return;
		}
		ihate= (Monster)level.level_monsters.item_at(pt.row, pt.col);
		if(null==ihate)
			return;

		int possible_damage;	// Fight should really be more symmetrical
		if(0==(ihate.m_flags & Monster.STATIONARY))
			possible_damage= Id.get_damage(ihate.mt.m_damage, null)*2/3;
		else
			possible_damage= ((Monster)ihate).stationary_damage - 1;

		while(null != ihate){
			one_move_rogue(ch, false);
			if((!to_the_death && hp_current <= possible_damage)
			|| level.self.interrupted || 0==(level.map[pt.row][pt.col] & MONSTER)){
				ihate= null;
			} else {
				Monster monster= (Monster)level.level_monsters.item_at(pt.row, pt.col);
				if(monster != ihate)
					ihate= null;
			}
		}
	}
	void rust(Monster monster){
		if(null==armor || armor.get_armor_class()<=1
		|| armor.kind == Id.LEATHER)
			return;
		if(armor.is_protected || maintain_armor){
			if(monster!=null && 0==(monster.m_flags & Monster.RUST_VANISHED)){
				tell("the rust vanishes instantly");
				monster.m_flags |= Monster.RUST_VANISHED;
			}
		} else {
			armor.d_enchant--;
			tell("your armor weakens");
			print_stat();
		}
	}
	void freeze(Monster monster){
		int freeze_percent= 99;
		int i, n;

		if(self.rand.percent(12))
			return;
		freeze_percent -= str_current + str_current/2;
		freeze_percent -= (exp + ring_exp) * 4;
		if(armor!=null)
			freeze_percent -= armor.get_armor_class() * 5;
		freeze_percent -= hp_max/3;

		if(freeze_percent > 10){
			monster.m_flags |= Monster.FREEZING_ROGUE;
			tell("you are frozen", true);

			n= self.rand.get(4, 8);
			for(i= 0; i < n; i++)
				level.mv_mons(this);
			if(self.rand.percent(freeze_percent)){
				for(i= 0; i < 50; i++)
					level.mv_mons(this);
				killed_by(null, Monster.HYPOTHERMIA);
			}else
				tell("you_can_move_again", true);
			monster.m_flags &= ~Monster.FREEZING_ROGUE;
		}
	}
	void sting(Monster monster){
		int sting_chance= 35;
		if(str_current <= 3)
			return;
		if(armor!=null)
			sting_chance += 6*(6-armor.get_armor_class());

		if(exp+ring_exp > 8)
			sting_chance -= 6 * (exp + ring_exp - 8);

		if(self.rand.percent(sting_chance)){
			if(sustain_strength){
				tell("A sting momentarily weakens you");
			}else{
				tell("the " + monster.name() + "'s bite has weakened you");
				str_current--;
				print_stat();
			}
		}
	}
	void drop_level(){
		if(self.rand.percent(80) || exp <= 5)
			return;
		exp_points= level_points[exp-2] - self.rand.get(9, 29);
		exp -= 2;
		int hp= hp_raise();
		hp_current -= hp;
		if(hp_current <= 0)
			hp_current= 1;
		hp_max -= hp;
		if(hp_max <= 0)
			hp_max= 1;
		add_exp(1, false);
	}
	void drain_life(){
		if(self.rand.percent(60) || hp_max<=30 || hp_current<10)
			return;
		int n= self.rand.get(1, 3);		/* 1 Hp, 2 Str, 3 both */

		if(n!=2 || !sustain_strength)
			tell("you feel weaker");
		if(n!=2){
			hp_max--;
			hp_current--;
			less_hp++;
		}
		if(n!=1){
			if(str_current>3 && !sustain_strength){
				str_current--;
				if(self.rand.coin())
					str_max--;
			}
		}
		print_stat();
	}
	void win(){
		Date d= new Date();
		self.starttime= d.getTime() - self.starttime;
		tell("YOU WON!");
		col= -1;
		trap_door= true;
		game_over= true;
		gold += pack.sell_all();
		view.empty();
		view.msg.banner(1, 6, option.nick_name);
		view.addch(10, 11, "@   @  @@@   @   @      @  @  @   @@@   @   @   @");
		view.addch(11, 11, " @ @  @   @  @   @      @  @  @  @   @  @@  @   @");
		view.addch(12, 11, "  @   @   @  @   @      @  @  @  @   @  @ @ @   @");
		view.addch(13, 11, "  @   @   @  @   @      @  @  @  @   @  @  @@");
		view.addch(14, 11, "  @    @@@    @@@        @@ @@    @@@   @   @   @");
		view.addch(17, 11, "Congratulations  you have  been admitted  to  the");
		view.addch(18, 11, "Fighters' Guild.   You return home,  sell all your");
		view.addch(19, 11, "treasures at great profit and retire into comfort.");
		view.addch(21, 16, "You have " + gold + " in gold");
		view.addch(23, 11, "Press SPACE to see the hall of fame");
		view.refresh();
		self.wait_for_ack();
		if(!wizzed)
			if(self.igoto("--**- WINNER -**--"))
				self.wait_for_ack();
	}
	void killed_by(Persona monster, int other){
		Date d= new Date();
		self.starttime= d.getTime() - self.starttime;
		if(other != Monster.QUIT && other!= Monster.SAVE)
			gold= ((gold * 9) / 10);
		String obit= "";
		if(other!=0){
			switch(other){
			case Monster.HYPOTHERMIA:	obit= "Died of hypothermia"; break;
			case Monster.STARVATION:	obit= "Died of starvation";break;
			case Monster.POISON_DART:	obit= "Killed by a dart";break;
			case Monster.QUIT:			obit= "Quit the game";	break;
			case Monster.KFIRE:			obit= "Killed by fire";break;
			case Monster.SAVE:			break;
			}
		} else if(monster!=null){
			/* Took out the vowel lookup */
			//char i= monster.name().charAt(0);
			//if(i=='a'||i=='e'||i=='i'||i=='o'||i=='u')
			if(Id.is_vowel((int)monster.name().charAt(0)))
				obit= "Killed by an " + monster.name();
			else
				obit= "Killed by a " + monster.name();
		}
		if(game_over){
			///System.out.println(option.nick_name + " is already dead "+obit);
			return;
		}
		col= -1;
		game_over= true;
		trap_door= true;
		if(other==Monster.SAVE){
			view.empty();
			String s= option.nick_name + ", the dungeon is saved";
			view.centerch(4, 0, s);
			view.colorow(4, 0, 0, uGreen);
			view.centerch(8, 0, "Press SPACE to continue in the dungeon");
			view.centerch(9, 0, "or change your name to start a new one");
		}else{
		//if(!option.no_skull){
			String s= obit + " with " + gold + " gold";
			view.empty();
			view.addch(4, 32, "__---------__");
			view.addch(5, 30, "_~             ~_");
			view.addch(6, 29, "/                 \\");
			view.addch(7, 28, "~                   ~");
			view.addch(8, 27, "/                     \\");
			view.addch(9, 27, "|    XXXX     XXXX    |");
			view.addch(10, 27, "|    XXXX     XXXX    |");
			view.addch(11, 27, "|    XXX       XXX    |");
			view.addch(12, 28, "\\         @         /");
			view.addch(13, 29, "--\\     @@@     /--");
			view.addch(14, 30, "| |    @@@    | |");
			view.addch(15, 30, "| |           | |");
			view.addch(16, 30, "| vvVvvvvvvvVvv |");
			view.addch(17, 30, "|  ^^^^^^^^^^^  |");
			view.addch(18, 31, "\\_           _/");
			view.addch(19, 33, "~---------~");
			view.addch(21, 8, option.nick_name);
			view.colorow(21, 0, 0, uRogue);
			view.addch(22, 8, s);
			view.addch(23, 8, "Press SPACE to see the graveyard");
		//} else {
		//	tell(s);
		//	tell("Press SPACE to see the graveyard");
		}
		view.refresh();
		self.wait_for_ack();
		if(!wizzed && other!=Monster.SAVE)
			if(self.igoto(obit))
				self.wait_for_ack();
	}
	void init_seen(){
		level.init_seen();
	}
	void preshow(){
		for(int r= 0; r<level.nrow; r++)
		for(int c= 0; c<level.ncol; c++)
		if(0!=(SEEN&level.map[r][c])){
			view.mark(r,c);
			if(FLOOR==(level.map[r][c]&(STAIRS|FLOOR)))
				level.map[r][c] &= ~SEEMAP;
			else
				level.map[r][c]|= MAPPED;	// mapped location
		}
		if(blind==0){
			///boolean intunnel= 0!=(level.map[row][col]&(TUNNEL|DOOR));
			int test= TUNNEL|DOOR|FLOOR|STAIRS;
			if(0==(level.map[row][col]&TUNNEL))
				test |= HORWALL|VERTWALL;
			Room room= level.room_at(row, col);
			for(int k= 0; k<8; k++)try{
				int r= row + Id.xtab[k];
				int c= col + Id.ytab[k];
				int mask= level.map[r][c];
				if(0!=(mask&test)){
					if(0!=(mask&HIDDEN)){
						if(0!=(mask&TUNNEL))
							continue;
						if(0!=(mask&DOOR) && 0!=(level.map[row][col]&TUNNEL))
							continue;
					}
				///if((intunnel && 0!=(mask&(TUNNEL|DOOR|DARK)) && 0==(mask&HIDDEN))
				///||(!intunnel && 0!=(mask&(HORWALL|VERTWALL|STAIRS|DOOR|DARK)))){
					if(0 != (mask&DARK))
						room= null;
					level.setseen(r,c);
					view.mark(r,c);
				}
			}catch(ArrayIndexOutOfBoundsException e){
			}
			if(room != null){
				for(Rowcol rc= room.nextrc(null); rc!=null; rc= room.nextrc(rc)){
					if(level.sees(rc.row, rc.col, row, col)){
						level.setseen(rc.row, rc.col);
						view.mark(rc.row,rc.col);
					}
				}
			}
		}
		level.setseen(row,col);
	}
	boolean can_see(int r, int c){
		if(blind>0)
			return false;
		return SEEN==(level.map[r][c]&SEEMAP);
	}
	String name(){
		return option.nick_name;
	}
	boolean zapt(Toy obj){
		boolean doend= true;
		boolean fiery= obj.kind==Id.FIRE;
		String s= obj.kind==Id.FIRE? "fire" : "ice";

		int ac= armor==null? 0 : armor.get_armor_class();
		if(self.rand.percent(10 + (3 * ac))){
			self.describe(this, "the " + s + " misses", false);
		} else {
			doend= false;
			int mydamage= self.rand.get(3, (3 * exp));
			if(fiery){
				mydamage= (mydamage * 3) / 2;
				if(armor != null)
					mydamage -= armor.get_armor_class();
			}
			damage(null, mydamage, 
				fiery? Monster.KFIRE : Monster.HYPOTHERMIA);
			self.describe(this, "the " + s + " hits", false);
		}
		return doend;
	}
/*
	void printseen(){
		String let= ".^mM-";
		int cmax= view.ncol<78> view.ncol : 78;

		System.out.println(toString());
		for(int r= 0; r<view.nrow; r++){
			for(int c= 0; c<cmax; c++){
				byte s= seen[r][c];
				if(s<0 || s>3)s= 4;
				System.out.print(let.charAt(s));
			}
			System.out.print("\n");
		}
	}
*/
}
