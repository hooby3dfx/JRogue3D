import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.net.*;
import java.applet.*;
import java.util.*;
import java.io.*;

class View extends Canvas  implements Serializable , MouseListener, KeyListener {
	private static final long serialVersionUID= 3L;
	Rogue self;
	Message msg;
	Item dummy;
	int pointsize;
	Level level;
	char terminal[][];
	char buffer[][];		// Low byte=ascii character, High byte=color index
	boolean line_dirty[];
	boolean rc_dirty[][];
	int nrow;
	int ncol;
	int cw;	/* Character width */
	int ch; /* Character height */
	int ca; /* Ascent */
	int lead;	/* Font leading */
	Font ffixed;
	FontMetrics fm;
	Man man= null;	/* From whose point of view this is */

	public View(Rogue self, int pointsize, int nrow, int ncol){
		this.self= self;
		this.pointsize= pointsize;
		this.level= level;
		this.nrow= nrow;
		this.ncol= ncol;
		this.msg= new Message(this);
		terminal= new char[nrow][ncol];
		buffer= new char[nrow][ncol];
		line_dirty= new boolean[nrow];
		rc_dirty= new boolean[nrow][ncol];

		for(int k= 0; k<nrow; k++){
			line_dirty[k]= false;
			for(int c= 0; c<ncol; c++)
				terminal[k][c]= 0;
		}
		// Set up the view canvas
		setSize(getPreferredSize());

		addMouseListener(this);
		addKeyListener(this);
//		requestFocusInWindow();
	}
	public Dimension getPreferredSize(){
		Dimension d= self.getSize();
		++pointsize;
		if(d.height>0)
			pointsize+= pointsize/2;
		do{
			--pointsize;
			ffixed= new Font("Courier", Font.PLAIN, pointsize);
			FontMetrics fm= getFontMetrics(ffixed);
			cw= fm.charWidth('X');
			ch= fm.getHeight();
			ca= fm.getAscent();
		}while(d.height>0 && pointsize>8 && ((nrow+1)*ch>d.height || (ncol+1)*cw>d.width));
		///System.out.println("SIZE" + cw + " " + ch + " ascent="+ ca);
		///System.out.println("--");
		return new Dimension(ncol * cw, nrow * ch);
	}
	boolean in_sight(int row, int col){
		return man.can_see(row, col);
	}
	static Color cmap[]= new Color[8+1];
	static {
		cmap[0]= Color.lightGray;
		cmap[1]= new Color(204,51,51);	// Dark red
		cmap[2]= Color.black;
		cmap[3]= Color.white;
		cmap[4]= Color.red;
		cmap[5]= Color.yellow;
		cmap[6]= Color.gray;
		cmap[7]= new Color(51,204,51);	// Green
		cmap[8]= new Color(192,168,153);	// old white
	}
	
	
	private void inupdate(Graphics g){
		
		if(g != null){
			Font ft= g.getFont();
			g.setFont(ffixed);
			byte ba[]= new byte[2];
			for(int y= 0; y<nrow; y++)if(line_dirty[y]){
				line_dirty[y]= false;
				char ter[]= terminal[y];
				char buf[]= buffer[y];
				for(int x= 0; x<ncol; x++){//if(ter[x]!=buf[x]){
					ter[x]= buf[x];
					ba[0]= (byte)(buf[x]&127);
					int st= buf[x]>>8;
					if(st==0 && Monster.isold)
						st= 8;
					if(ba[0]=='_' || ba[0]==0 || st==2)
						ba[0]= 32;
					g.setColor(Color.black);
					g.fillRect(x*cw, y*ch, cw, ch);
					g.setColor(cmap[st]);
					g.drawBytes(ba, 0, 1, x*cw, y*ch+ca);
				}
			}
			g.setFont(ft);
		}
	}
	void mark(int r, int c){
		rc_dirty[r][c]= true;
		line_dirty[r]= true;
	}
	void markall(){
		for(int r= 0; r<nrow-1; r++){
			line_dirty[r]= true;
			for(int c= 0; c<ncol; c++)
				rc_dirty[r][c]= true;
		}
	}
	void setmarked(Man man){
		for(int r= 0; r<nrow; r++)for(int c= 0; c<ncol; c++)
		if(rc_dirty[r][c])
			addch(r, c, man.showrc(r,c));
	}
	public void update(Graphics g){
		
		synchronized (self.gamer){
			for(int r= 0; r<nrow; r++){
				line_dirty[r]= true;
				for(int c= 0; c<ncol; c++)
					terminal[r][c]= 0;
			}
			inupdate(g);
			
		}
	}
	public void paint(Graphics g) {
		update(g);
	}
	void refresh(){
		
		Graphics g= getGraphics();
		if(g!=null){
			inupdate(g);
			g.dispose();
			
			//call 3d update
			self.game3d.updateRogue(buffer);
		}
	}
	void addch(int row, int col, char ch){
		//if(row>=0 && row<nrow && col>=0 && col<ncol){
		try{
			if(ch!=buffer[row][col]){
				buffer[row][col]=ch;
				terminal[row][col]= 0;
				line_dirty[row]= true;
			}
			rc_dirty[row][col]= false;
		}catch(ArrayIndexOutOfBoundsException e){
		}
	}
	void addch(int row, int col, String s){
		if(row>=0 && row<nrow){
			int n= s.length();
			col += n;
			while(--n>=0){
				--col;
				if(col>=0 && col<ncol){
					rc_dirty[row][col]= false;
					buffer[row][col]= s.charAt(n);
				}
			}
			line_dirty[row]= true;
		}
	}
	void centerch(int row, int nc, String s){
		if(nc==0)
			nc= ncol;
		int col= (nc - s.length())/2;
		if(col<0)
				col= 0;
		addch(row, col, s);
	}
	char charat(int row, int col){
		return buffer[row][col];
	}
	void colorow(int row, int col, int n, int color){
		if(n==0)
			n= ncol-col;
		try{
			line_dirty[row]= true;
			while(--n>=0){
				addch(row, col, (char)((buffer[row][col]&255)|color));
				col++;
			}
		}catch(ArrayIndexOutOfBoundsException e){
		}
	}
	void empty(){
		for(int r= 0; r<nrow; r++){
			line_dirty[r]= true;
			for(int c= 0; c<ncol; c++){
				terminal[r][c]= 0;
				buffer[r][c]= ' ';
			}
		}
		refresh();
	}
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(buffer);
	}
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		buffer= (char[][])in.readObject();
	}
	public void mouseClicked(MouseEvent evt){}
	public void mouseEntered(MouseEvent evt){}
	public void mouseExited(MouseEvent evt){}
	public void mousePressed(MouseEvent evt){}
	public void mouseReleased(MouseEvent evt){}

	public void keyPressed(KeyEvent evt){
		char ch= ' ';
		switch(evt.getKeyCode()){
		case KeyEvent.VK_KP_LEFT:
		case KeyEvent.VK_LEFT:		ch='h'; break;
		case KeyEvent.VK_KP_RIGHT:
		case KeyEvent.VK_RIGHT:		ch='l'; break;
		case KeyEvent.VK_UP:
		case KeyEvent.VK_KP_UP:		ch= 'k'; break;
		case KeyEvent.VK_DOWN:
		case KeyEvent.VK_KP_DOWN:	ch= 'j'; break;
		case KeyEvent.VK_HOME:		ch= 'y'; break;
		case KeyEvent.VK_END:		ch= 'b'; break;
		case KeyEvent.VK_PAGE_UP:	ch= 'u'; break;
		case KeyEvent.VK_PAGE_DOWN:	ch= 'n'; break;
		case KeyEvent.VK_NUMPAD0:	ch= '0'; break;
		case KeyEvent.VK_NUMPAD1:	ch= '1'; break;
		case KeyEvent.VK_NUMPAD2:	ch= '2'; break;
		case KeyEvent.VK_NUMPAD3:	ch= '3'; break;
		case KeyEvent.VK_NUMPAD4:	ch= '4'; break;
		case KeyEvent.VK_NUMPAD5:	ch= '5'; break;
		case KeyEvent.VK_NUMPAD6:	ch= '6'; break;
		case KeyEvent.VK_NUMPAD7:	ch= '7'; break;
		case KeyEvent.VK_NUMPAD8:	ch= '8'; break;
		case KeyEvent.VK_NUMPAD9:	ch= '9'; break;
		default:	return;
		}
		self.keyact(ch);
	}
	public void keyReleased(KeyEvent evt){}
	public void keyTyped(KeyEvent evt){
		self.keyact(evt.getKeyChar());
	}
}
