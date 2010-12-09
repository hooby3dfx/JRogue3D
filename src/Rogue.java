/* Rogue.java -- Rogue game for java 149 152 155*/
/*% javac Rogue.java */
/*% jar vcf Rogue.jar *.class */
/*% cabarc n Rogue.cab *.class */
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.net.*;
import java.applet.*;
import java.util.*;
import java.io.*;

public class Rogue extends Applet implements Runnable, Header, Serializable{
	private static final long serialVersionUID= 3L;
	Thread gamer;
	boolean rundone= false;
	Level level;
	View view= null;
	Vector flashers= new Vector();
	Randomx rand;
	String keybuf= "";
	long starttime;
	Option optsav= new Option();

	int pointsize= 12;
	String scorepagename;
	boolean interrupted;

	int treepct;		// parameter
	int gorepct;		// parameter

	static int ident= 1;
	int myident;
	Rogue3d game3d;
	public Rogue(){
		super();
		myident= ident++;
	}
	public String toString(){
		return super.toString() + " ID="+Integer.toString(myident);
	}
	int intparam(String s, int defvalue){
		int i;
		try{
			i= Integer.parseInt(getParameter(s));
		}catch(NumberFormatException e){
			i= defvalue;
		}
		return i;
	}
	public void init(){
		pointsize= intparam("pointsize", 12);
		rand= new Randomx(intparam("srand", (int)System.currentTimeMillis()));
	
		scorepagename= getParameter("score");
		setBackground(Color.black);
		Monster m= new Monster();	// Force static definitions
		String s= getParameter("style");
		if(s!=null && s.equals("old"))
			Monster.setoldstyle(true);
		treepct= intparam("treepct",0);
		gorepct= intparam("gorepct",0);
		game3d = new Rogue3d();
	}
	public void start(){
		gamer= new Thread(this);
		gamer.start();
		repaint(30);
	}
	public void stop(){
		gamer= null;
	}
	void begin_game(){
		// check for saved game
		if(view==null){
			add(view= new View(this, pointsize, 25, 80));
			// show centered with a blank line at the top
			Dimension s= getSize();
			Dimension d= view.getSize();
			Point pt= new Point((s.width-d.width)/2, (int)view.ch);
			view.setLocation(pt);
			view.man= new Man(this, view);
			if(0==view.man.option.nick_name.compareToIgnoreCase("rogue"))
				view.man.option= optsav;
			
			game3d.setView(view);
			
		}else{
			if(view.man!=null)
				optsav= view.man.option;
			view.empty();
			view.man= new Man(this, view);
			view.man.option= optsav;
		}
		if(0==view.man.option.nick_name.compareToIgnoreCase("rogue")
		|| !get_game()){
			Id.list_items();
			Id.mix_colors(rand);
			Id.make_scroll_titles(rand);
			view.man.new_level_message= "Welcome, "+ view.man.option.nick_name;
		}
		System.gc();
		starttime= (new Date()).getTime();
	}
	public void run(){
		int lastchar= 0;
		gamer.setPriority(Thread.MIN_PRIORITY);
		level= null;
		view= null;
		begin_game();
		while(gamer==Thread.currentThread()){
			if(level==null || level.rooms==null){
				interrupted= false;
				if(level==null || rand.percent(99) || level.cur_level<BigRoom.MIN_LEVEL)
					level= new NineRoom(25, 80, this);
				else
					level= new BigRoom(25, 80, this);
				level.put_monsters();
				view.man.level= level;
				if(view.man.pack==null)
					view.man.player_init();
				if(!view.man.has_amulet() && (level.cur_level >= Level.AMULET_LEVEL)){
					Rowcol pt= level.gr_row_col(Level.FLOOR|Level.TUNNEL, null);
					if(pt!=null){
						Toy amulet= new Toy(level, Id.AMULET);
						amulet.place_at(pt.row, pt.col, TOY);
					}
				}
				view.level= level;
				view.empty();
				view.man.pack.relevel(level);
				level.put_player(view.man);
				view.man.init_seen();
				if(0 != (((Room)level.rooms.elementAt(0)).is_room&Room.R_TREE))
					view.man.tell("you enter the Tulgey Wood");
			}
			view.man.print_stat();
			view.refresh();
			repaint();

			lastchar= view.man.play_level();
			md_slurp();

			if(lastchar=='S'){
				if(save_game())
					view.man.killed_by(null, Monster.SAVE);	// sets game_over
			}
			///System.out.println("off level " + level.cur_level + (view.man.game_over? "dead":"alive"));
			if(view.man.game_over){
				level= null;
				begin_game();
			}else if(lastchar==Man.RELOAD){
				if(!get_game())
					view.msg.message("Welcome, "+ view.man.option.nick_name);
			}else if(lastchar!='f' && lastchar!='S')
				level.rooms= null;	// mark level completed
		}
		rundone= true;
	}
	public void destroy(){
		while(!rundone)
			md_sleep(100);
		if(view!=null){
			view.removeMouseListener(view);
			view.removeKeyListener(view);
		}
	}
	public void paint(Graphics g){
		if(view!=null){
			view.repaint();
		}
	}
	public String getAppletInfo() {
		return "My Rogue";
	}
	synchronized void pushkey(char key){
		keybuf= keybuf + key;
		notify();
	}
	synchronized int popkey(){
		int key= 0;
		if(keybuf.length() > 0){
			key= (int)keybuf.charAt(0);
			keybuf= keybuf.substring(1);
		}
		return key;
	}
	public void keyact(int key) {
		if(key=='\033')
			interrupted= true;
		if(!gamer.isAlive()){
			if(key==' ')
				start();
		}else if(key!=Man.RELOAD)
			pushkey((char)key);
	}
	synchronized void md_sleep(int mseconds){
		if(mseconds>0){
			try {
				Thread.sleep(mseconds);
			} catch (InterruptedException e){};
		}
		keybuf= "";
	}
	synchronized void md_slurp(){
		keybuf= "";
	}
	synchronized int rgetchar(){
		while(keybuf.length()==0){
			try{
				 wait();
			}catch(InterruptedException e){
				interrupted= true;
				return '\033';
			}
		}
		return popkey();
	}
	void wait_for_ack(){
		int c;
		do c= rgetchar();
		while(c!=' ' && c!='\033');
	}
	public boolean igoto(String obit){
		Man man= view.man;
		view.empty();
		view.addch(12, 22, "Graveyard not available.");
		view.addch(23, 5, "Press SPACE to start a new game");
		view.refresh();
		return true;
	}
	void flashadd(int row, int col, int color){
		int ia[]= new int[3];
		ia[0]= row; ia[1]= col; ia[2]= color;
		flashers.addElement(ia);
	}
	void xflash(){
		if(flashers.size()>0){
			boolean bseen= false;
			Vector chsave= new Vector(flashers.size());
			if(view!=null){
				Enumeration f= flashers.elements();
				boolean vseen= false;
				while(f.hasMoreElements()){
					int ia[]= (int [])f.nextElement();
					if(view.in_sight(ia[0], ia[1])){
						int ch= view.terminal[ia[0]][ia[1]];
						if(ch==0)
							ch= view.buffer[ia[0]][ia[1]];
						chsave.addElement(new Character((char)ch));
						ch &= 255;
						if(ch=='.')
							ch= '*';
						ch= (ch&255)|ia[2];
						view.addch(ia[0], ia[1], (char)ch);
						vseen= true;
					}
				}
				if(vseen){
					view.refresh();
					bseen= true;
					md_sleep(120);
				}
			}
			if(bseen && view!=null){
				Enumeration f= flashers.elements();
				Enumeration c= chsave.elements();
				while(f.hasMoreElements()){
					int ia[]= (int [])f.nextElement();
					if(view.in_sight(ia[0], ia[1]))
						view.addch(ia[0], ia[1], ((Character)c.nextElement()).charValue());
					view.mark(ia[0], ia[1]);
				}
			}
			flashers= new Vector();
		}
	}
	void vflash(int r, int c, char ch){
		if(view!=null && view.in_sight(r, c)){
			view.addch(r, c, ch);
			refresh();
			md_sleep(50);
			view.addch(r, c, level.get_char(r, c));
		}
	}
	void tell(Persona p, String s, boolean bintr){
		if(view!=null && view.man==p){
			String ss= whoify(p, s);
			view.msg.message(ss, bintr);
			xflash();
		}
	}
	boolean describe(Rowcol rc, String s, boolean bintr){
		if(view!=null && view.in_sight(rc.row, rc.col)){
			String ss= whoify(view.man, s);
			view.msg.message(ss, bintr);
			return true;
		}
		xflash();
		return false;
	}
	void check_message(Persona p){
		if(view!=null && view.man==p)
			view.msg.check_message();
	}
	void refresh(){
		if(view!=null)
			view.refresh();
	}
	void vset(int r, int c){
		if(view!=null)
			view.addch(r,c,view.charat(r,c));
	}
	void mark(int r, int c){
		if(view!=null)
			view.mark(r,c);
	}
	void markall(){
		if(view!=null)
			view.markall();
	}
	String whoify(Persona p, String src){
		String dst= "";
		int i= 0;
		int j;
		try{
			while((j= src.indexOf('@', i)) >= 0){
				dst += src.substring(i, j);
				boolean hasverb= src.charAt(++j) == '>';
				i= j+1;
				j= src.indexOf(hasverb? '+' : '>', i);
				boolean byou= false;
				String name= src.substring(i, j);
				if(name.equals(p.name())){
					dst += "you";
					byou= true;
				}else
					dst += "the " + name;
				if(hasverb){
					i= j+1;
					dst += " ";
					j= src.indexOf('+', i);
					if(byou){
						dst += src.substring(i, j);
						i= src.indexOf('<', j);
					}else{
						i= src.indexOf('<', j);
						dst += src.substring(j+1, i);
					}
				}else
					i= j;
				++i;
			}
		} catch(Exception e){
			System.out.println("whoify error on " + p.name());
			System.out.println(src+"\n"+dst);
		}
		dst += src.substring(i);
		return dst;
	}
	public boolean read_game(String name){
		if(name==null || 0==name.compareToIgnoreCase("rogue"))
			return false;
		name= name.replace(' ', '_');
		view.msg= new Message(view);
		md_slurp();
		try{
			URL doc= getCodeBase();
			URL url= new URL(doc.getProtocol(), doc.getHost(), "cgi");
			URLConnection connection = url.openConnection();
			connection.connect();
			ObjectInput in= new ObjectInputStream(connection.getInputStream());

			String s= (String)in.readObject();
			//System.out.println("Read from stream:"+s);
			starttime= (new Date()).getTime() - in.readLong();
			Level l= (Level)in.readObject();
			try{
				ro((ObjectInputStream)in);
			}catch(Exception eo){
				System.out.println("EO "+eo);
			}
			Man m= (Man)l.level_men.elementAt(0);
			View v= m.view;

			l.self= this;
			m.self= this;
			m.view= view;

			view.level= level= l;
			view.man= m;
			view.buffer= v.buffer;
			level.unserialfix();
			flashers= new Vector();

			in.close();
			String wiz= view.man.wizzed? " the wizard ":" ";
			view.msg.message("Restored" + wiz + view.man.option.nick_name + " to level "+level.cur_level);
		}catch(Exception e){
			System.out.println("Restore failed "+ e);
			Option o= view.man!=null? view.man.option : optsav;
			return false;
		}
		view.repaint(30);
		return true;
	}
	public boolean send_game(String name){
		name= name.replace(' ', '_');
		URL url= null;
		String sret= null;
		try{
			view.msg.message(" ");
			view.msg.check_message();

			ByteArrayOutputStream baos= new ByteArrayOutputStream();
			ObjectOutput out= new ObjectOutputStream(baos);

			out.writeObject("SAVED DUNGEON FOR " + view.man.option.nick_name);
			out.writeLong((new Date()).getTime() - starttime);
			out.writeObject(level);
			wo((ObjectOutputStream)out);

			out.flush();
			out.close();

			/*
			URL doc= getCodeBase();
			url = new URL(doc.getProtocol(), doc.getHost(), "cgi");
			URLConnection connection = url.openConnection();
			connection.setDoInput (true);
			connection.setDoOutput(true);
			connection.setUseCaches (false);
			connection.setRequestProperty("Content-Length", Integer.toString(baos.size()+name.length()+1));
			connection.connect();
			*/

			//DataOutputStream printout= new DataOutputStream (connection.getOutputStream ());
			File outfile = new File("save");
			FileOutputStream printout= new FileOutputStream (outfile);
			
			//printout.writeBytes(name+"\n");
			String ss = name+"\n";
			printout.write(ss.getBytes());
			printout.write(baos.toByteArray(), 0, baos.size());
			printout.flush();
			printout.close();

			/*
			BufferedReader dis= new BufferedReader(new InputStreamReader(connection.getInputStream()));
			for(int w= 0; w<12; w++){
				sret= dis.readLine();
				if(sret==null)
					break;
				//System.out.println("*** "+sret);
				if(sret.length() > 2)
					break;
			}
			dis.close();
			*/
			
			/*
			
			if(sret==null || sret.length()<2)
				sret= "NG  No response from save";
			if(sret.substring(0,2).equals("OK"))
				view.man.tell("Saved " + baos.size() + " bytes");
			else{
				view.man.tell("Save failed " + sret.substring(2));
				sret= null;
			}
			*/
		}catch(Exception e){
			e.printStackTrace();
			view.man.tell("Save failed. Sorry.");
			return false;
		}
		//return sret != null;
		return true;
	}
	boolean get_game(){
		return read_game(view.man.option.nick_name+","+view.man.option.fruit);
	}
	boolean save_game(){
		return send_game(view.man.option.nick_name+","+view.man.option.fruit);
	}	
	private void writeObject(ObjectOutputStream out) throws IOException {}
	private void wo(ObjectOutputStream out) throws IOException {
		out.writeObject(Id.id_potions);
		out.writeObject(Id.id_scrolls);
		out.writeObject(Id.id_weapons);
		out.writeObject(Id.id_armors);
		out.writeObject(Id.id_wands);
		out.writeObject(Id.id_rings);
	}
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {}
	private void ro(ObjectInputStream in) throws IOException, ClassNotFoundException {
		Id.id_potions=	(Id [])in.readObject();
		Id.id_scrolls=	(Id [])in.readObject();
		Id.id_weapons=	(Id [])in.readObject();
		Id.id_armors=	(Id [])in.readObject();
		Id.id_wands=	(Id [])in.readObject();
		Id.id_rings=	(Id [])in.readObject();
	}
}

