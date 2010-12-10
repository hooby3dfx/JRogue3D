

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import javax.swing.JFrame;
import com.sun.opengl.util.*;
import java.nio.ByteBuffer;

public class Rogue3d extends JFrame implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, ActionListener{

	
	// mouse control variables
	private final GLCanvas canvas;
	private int winW = 800, winH = 600;
	private int mouseX, mouseY;
	private int mouseButton;
	
	private boolean shiftKeyDown = false;
	

	// gl shading/transformation variables
	private float tx = 0.0f, ty = 0.0f;
	private float scale = 1.0f;
	private float angle = 0.0f;
	private float xangle = 0.0f;
	private boolean drawWireframe = false;
	
	
	private float xpos = 0, ypos = 0, zpos = 0;
	private float centerx, centery, centerz;
	private float roth = 0, rotv = 0;
	private float znear, zfar;
	private float xmin = -1f, ymin = -1f, zmin = -1f;
	private float xmax = 1f, ymax = 1f, zmax = 1f;	
	
	
	

	// gl context/variables
	private GL gl;
	private final GLU glu = new GLU();
	private final GLUT glut = new GLUT();
	
	private boolean minimap = true;

	public static void main(String args[]) {
		new Rogue3d();
	}

	// constructor
	public Rogue3d() {
		super("JRogue3d");
		canvas = new GLCanvas();
		canvas.addGLEventListener(this);
		canvas.addKeyListener(this);
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
		getContentPane().add(canvas);
		setSize(winW, winH);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
		canvas.requestFocus();
		
		//load all the Rogue3d stuff!
		initRogue3dStuff();
		
		//need this to use tab key
		canvas.setFocusTraversalKeysEnabled(false);
		
	}
	
	// gl display function
	public void display(GLAutoDrawable drawable) {
		
		// if mouse is clicked, we need to detect whether it's clicked on the shape
		/*
		if (mouseClick) {
			ByteBuffer pixel = ByteBuffer.allocateDirect(1);

			gl.glClear( GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT );
			gl.glColor3f(1.0f, 1.0f, 1.0f);
			gl.glDisable( GL.GL_LIGHTING );
			drawShape();
			gl.glReadPixels(mouseX, (winH-1-mouseY), 1, 1, GL.GL_RED, GL.GL_UNSIGNED_BYTE, pixel);
			
			if (pixel.get(0) == (byte)255) {
				// mouse clicked on the shape, set clickedOnShape to true
				clickedOnShape = true;
			}
			// set mouseClick to false to avoid detecting again
			mouseClick = false;
		}
		*/

		// shade the current shape
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		
		//first text
		gl.glDisable(GL.GL_LIGHTING);
		gl.glLoadIdentity();
		gl.glTranslated(0, 0, -4f);
		gl.glColor3f(0, 1f, 0);
		if(minimap){
			
			double bx = -2;
			double by = 1;
			if(myBuffer!=null){
				for(int i=0; i<myBuffer.length; i++){
					
					gl.glRasterPos2d(bx, by);
					glut.glutBitmapString(GLUT.BITMAP_8_BY_13, new String(myBuffer[i]));
					by-=.1;
	
				}
			}
		
		}else{
			//just show status messages
			
			double bx = -2;
			double by = 1.2;
			if(myBuffer!=null){
				gl.glRasterPos2d(bx, by);
				glut.glutBitmapString(GLUT.BITMAP_8_BY_13, new String(myBuffer[0]));
				by-=2.7;
				gl.glRasterPos2d(bx, by);
				glut.glutBitmapString(GLUT.BITMAP_8_BY_13, new String(myBuffer[myBuffer.length-1]));

			}
			
		}
		gl.glEnable(GL.GL_LIGHTING);
		gl.glPolygonMode(GL.GL_FRONT_AND_BACK, drawWireframe ? GL.GL_LINE : GL.GL_FILL);
		
		loadDefaultColor();

		gl.glLoadIdentity();

		gl.glTranslatef(-xpos, -ypos, -zpos);
		gl.glTranslatef(centerx, centery, centerz);
		gl.glRotatef(360.f - roth, 0, 1f, 0);
		//gl.glRotatef(rotv, spin1, 0, 0);
		gl.glRotatef(rotv, spin1, 0, spin2);
		gl.glTranslatef(-centerx, -centery, -centerz);	
		
		if(focusGo){
			focusCam();
			//focusGo=false;
		}
		
		if(myBuffer!=null)
			drawRogue();
		gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);

	}
	
	private float spin1 = 1f;
	private float spin2 = 0f;
	
	private void swapSpin(){
		System.out.println("spin swap");
		float t = spin1;
		spin1=spin2;
		spin2=t;
	}

	private void loadDefaultColor(){
		gl.glColor3f(.6f, 0.1f, 0.01f);
	}
	private boolean focusGo=false;
	
	//backups
	private float xpos1, ypos1, zpos1, centerx1, centery1, centerz1, roth1, rotv1;
	
	//to change from follow view to fixed cam
	private void swapCamVars(){
		float t1,t2,t3,t4,t5,t6,t7,t8;
		
		t1=xpos;
		t2=ypos;
		t3=zpos;
		t4=centerx;
		t5=centery;
		t6=centerz;
		t7=roth;
		t8=rotv;
		
		
		xpos=xpos1;
		ypos=ypos1;
		zpos=zpos1;
		centerx=centerx1;
		centery=centery1;
		centerz=centerz1;
		roth=roth1;
		rotv=rotv1;
		
		xpos1=t1;
		ypos1=t2;
		zpos1=t3;
		centerx1=t4;
		centery1=t5;
		centerz1=t6;
		roth1=t7;
		rotv1=t8;
		
	}
	
	private void focusCam(){
		
		gl.glTranslated(-roguex,-1,roguey);
		
		System.out.println("focused on rogue");


	}
	
	private void setRogueSpot(int x,int y){
		roguex=x;
		roguey=y;
	}
	private int roguex, roguey;
	
	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
		// TODO Auto-generated method stub
	}
	
	public void initRogue3dStuff(){
		
		R3dArtLoader loader = new R3dArtLoader();
		
		objects = new HashMap<Character,R3dBasicObject>();
		//load em up!
		
		loader.load(objects);
		
		//maybe keep these in a text file of assets
		
		
	}
	
	public void updateRogue(char[][] buffer){
		
		System.out.println("Rogue update called!");
		myBuffer = buffer;
		//System.out.println(buffer[0]);

		for(int i=0; i<myBuffer.length; i++){
			for(int j=0; j<myBuffer[i].length; j++){
				myBuffer[i][j]=(char)(byte)(myBuffer[i][j]&127);

			}
		}

		canvas.display();
		
	}
	private char[][] myBuffer = null;
	
	private void drawRogue(){
		for(int i=1; i<myBuffer.length-1; i++){
			
			gl.glTranslated(1,0,0);
			gl.glPushMatrix();
			for(int j=0; j<myBuffer[i].length; j++){
				
				//calculate matrix!
				
				gl.glTranslated(0,0,-1);
				drawObject(myBuffer[i][j]);
				
				if(myBuffer[i][j]=='@'){
					//save 'cam' position based on this
					setRogueSpot(i,j);
				}
				
			}

			gl.glPopMatrix();
		}
	}
	
	private HashMap<Character,R3dBasicObject> objects;
	
	private void drawObject(char code){
		
		//lookup char 
		R3dBasicObject obj = objects.get(code);
		
		if(obj!=null){
			//System.out.println("trying to draw: "+code);
			obj.Draw(this.gl);
			
			//set default color
			loadDefaultColor();

			//always draw floor?
			obj = objects.get('#');
			obj.Draw(this.gl);
		
		}
		
		loadDefaultColor();

		
	}
	
	
	

	// initialization
	public void init(GLAutoDrawable drawable) {
		// original:
		/*
		gl = drawable.getGL();
    		
		gl.setSwapInterval(1);
    		
    	initViewParameters();


		gl.glColorMaterial(GL.GL_FRONT, GL.GL_DIFFUSE);
		gl.glEnable( GL.GL_COLOR_MATERIAL ) ;
		gl.glEnable(GL.GL_LIGHT0);
		gl.glEnable(GL.GL_NORMALIZE);
		gl.glShadeModel(GL.GL_SMOOTH);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LESS);
		gl.glCullFace(GL.GL_BACK);
		gl.glEnable(GL.GL_CULL_FACE);

		// set clear color: this determines the background color (which is dark gray)
		gl.glClearColor(.3f, .3f, .3f, 1f);
		gl.glClearDepth(1.0f);
		*/
		
		//from hierarchical:
		gl = drawable.getGL();

		initViewParameters();
		
		//check
		gl.glColorMaterial(GL.GL_FRONT, GL.GL_DIFFUSE);
		gl.glEnable( GL.GL_COLOR_MATERIAL ) ;
		
		
		gl.glClearColor(.1f, .1f, .1f, 1f);
		gl.glClearDepth(1.0f);

	    // white light at the eye
	    float light0_position[] = { 0, 0, 1, 0 };
	    float light0_diffuse[] = { 1, 1, 1, 1 };
	    float light0_specular[] = { 1, 1, 1, 1 };
	    gl.glLightfv( GL.GL_LIGHT0, GL.GL_POSITION, light0_position, 0);
	    gl.glLightfv( GL.GL_LIGHT0, GL.GL_DIFFUSE, light0_diffuse, 0);
	    gl.glLightfv( GL.GL_LIGHT0, GL.GL_SPECULAR, light0_specular, 0);

	    //red light
	    float light1_position[] = { -.1f, 1, 0, 0 };
	    float light1_diffuse[] = { .6f, .05f, .05f, 1 };
	    float light1_specular[] = { .6f, .05f, .05f, 1 };
	    gl.glLightfv( GL.GL_LIGHT1, GL.GL_POSITION, light1_position, 0);
	    gl.glLightfv( GL.GL_LIGHT1, GL.GL_DIFFUSE, light1_diffuse, 0);
	    gl.glLightfv( GL.GL_LIGHT1, GL.GL_SPECULAR, light1_specular, 0);

	    //blue light
	    float light2_position[] = { .1f, 1, 0, 0 };
	    float light2_diffuse[] = { .05f, .05f, .6f, 1 };
	    float light2_specular[] = { .05f, .05f, .6f, 1 };
	    gl.glLightfv( GL.GL_LIGHT2, GL.GL_POSITION, light2_position, 0);
	    gl.glLightfv( GL.GL_LIGHT2, GL.GL_DIFFUSE, light2_diffuse, 0);
	    gl.glLightfv( GL.GL_LIGHT2, GL.GL_SPECULAR, light2_specular, 0);

	    //material
	    /*
	    float mat_ambient[] = { 0, 0, 0, 1 };
	    float mat_specular[] = { .8f, .8f, .8f, 1 };
	    float mat_diffuse[] = { .4f, .4f, .4f, 1 };
	    float mat_shininess[] = { 128 };
	    gl.glMaterialfv( GL.GL_FRONT, GL.GL_AMBIENT, mat_ambient, 0);
	    gl.glMaterialfv( GL.GL_FRONT, GL.GL_SPECULAR, mat_specular, 0);
	    gl.glMaterialfv( GL.GL_FRONT, GL.GL_DIFFUSE, mat_diffuse, 0);
	    gl.glMaterialfv( GL.GL_FRONT, GL.GL_SHININESS, mat_shininess, 0);

	    float bmat_ambient[] = { 0, 0, 0, 1 };
	    float bmat_specular[] = { 0, .8f, .8f, 1 };
	    float bmat_diffuse[] = { 0, .4f, .4f, 1 };
	    float bmat_shininess[] = { 128 };
	    gl.glMaterialfv( GL.GL_BACK, GL.GL_AMBIENT, bmat_ambient, 0);
	    gl.glMaterialfv( GL.GL_BACK, GL.GL_SPECULAR, bmat_specular, 0);
	    gl.glMaterialfv( GL.GL_BACK, GL.GL_DIFFUSE, bmat_diffuse, 0);
	    gl.glMaterialfv( GL.GL_BACK, GL.GL_SHININESS, bmat_shininess, 0);
	    */

	    float lmodel_ambient[] = { .5f, .5f, .5f, 1 };
	    gl.glLightModelfv( GL.GL_LIGHT_MODEL_AMBIENT, lmodel_ambient, 0);
	    gl.glLightModeli( GL.GL_LIGHT_MODEL_TWO_SIDE, 1 );

	    gl.glEnable( GL.GL_NORMALIZE );
	    gl.glEnable( GL.GL_LIGHTING );
	    gl.glEnable( GL.GL_LIGHT0 );
	    gl.glEnable( GL.GL_LIGHT1 );
	    gl.glEnable( GL.GL_LIGHT2 );

	    gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LESS);
		gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
		gl.glCullFace(GL.GL_BACK);
		gl.glEnable(GL.GL_CULL_FACE);
		gl.glShadeModel(GL.GL_SMOOTH);
	}
	
	void initViewParameters()
	{
		roth = rotv = 0;

		float ball_r = (float) Math.sqrt((xmax-xmin)*(xmax-xmin)
							+ (ymax-ymin)*(ymax-ymin)
							+ (zmax-zmin)*(zmax-zmin)) * 0.307f;//0.707f;

		centerx = (xmax+xmin)/2.f;
		centery = (ymax+ymin)/2.f;
		centerz = (zmax+zmin)/2.f;
		xpos = centerx;
		ypos = centery;
		zpos = ball_r/(float) Math.sin(45.f*Math.PI/180.f)+centerz;

		znear = 0.01f;
		zfar  = 1000.f;

		//motionSpeed = 0.002f * ball_r;
		//rotateSpeed = 0.1f;

	}

	// reshape callback function: called when the size of the window changes
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		// TODO Auto-generated method stub
		winW = width;
		winH = height;
		/*
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(30.0f, (float) width / (float) height, 0.01f, 100.0f);
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL.GL_MODELVIEW);
		*/
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(45.f, (float)width/(float)height, znear, zfar);
		gl.glMatrixMode(GL.GL_MODELVIEW);
	}

	// mouse pressed even callback function
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		//mouseClick = true;
		mouseX = e.getX();
		mouseY = e.getY();
		mouseButton = e.getButton();
		canvas.display();
	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		//clickedOnShape = false;
		mouseButton = MouseEvent.NOBUTTON;

		canvas.display();
	}

	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub
		/*
		if (!clickedOnShape)	return;

		int x = e.getX();
		int y = e.getY();
		if (mouseButton == MouseEvent.BUTTON3) {
			// right button scales
			scale += (y - mouseY) * 0.01f;
		}
		else if (mouseButton == MouseEvent.BUTTON2) {
			// middle button translates
			tx += (x - mouseX) * 0.01f;
			ty -= (y - mouseY) * 0.01f;
		}
		else if (mouseButton == MouseEvent.BUTTON1 && shiftKeyDown){
			xangle += (x - mouseX);
		}
		else if (mouseButton == MouseEvent.BUTTON1) {
			// left button rotates
			angle += (y - mouseY);
		}
		mouseX = x;
		mouseY = y;
		canvas.display(); */
		int x = e.getX();
		int y = e.getY();
		if (mouseButton == MouseEvent.BUTTON3 ) {
			zpos -= (y - mouseY) ;
			mouseX = x;
			mouseY = y;
			canvas.display();
		} else if (mouseButton == MouseEvent.BUTTON2 || shiftKeyDown) {
			xpos -= (x - mouseX) ;
			ypos += (y - mouseY) ;
			mouseX = x;
			mouseY = y;
			canvas.display();
		} else if (mouseButton == MouseEvent.BUTTON1) {
			roth -= (x - mouseX) ;
			rotv += (y - mouseY) ;
			mouseX = x;
			mouseY = y;
			canvas.display();
		}
		
	}
	public void keyPressed(KeyEvent e) {

		switch(e.getKeyCode()) {
		
		//use this to set cam on rogue
		case KeyEvent.VK_TAB:
			minimap=!minimap;
			System.out.println("tab toggle");
			canvas.display();	
			return;
		case KeyEvent.VK_SHIFT:
			shiftKeyDown = true;
			break;
		
		case KeyEvent.VK_CONTROL:

			swapCamVars();
			
			focusGo=!focusGo;
			canvas.display();
			break;
		case KeyEvent.VK_ALT:
			System.out.println("alt toggle");
			swapSpin();
			break;

		
		//need to wrap other keys into rogue applet
			
			
		}
		
		myView.keyPressed(e);
		
		//canvas.display();		
	}
	
	private View myView;
	public void setView(View v){
		myView=v;
	}

	// these event functions are not used for this assignment
	// but may be useful in the future
	public void keyTyped(KeyEvent e) { 
		//do this to prevent key from going to applet
		//most keys are needed, so use sparingly
		if(e.getKeyChar()=='\t'){
			System.out.println("tab skipping");
			return;
		}
		//System.out.println(e.getKeyChar());

		/*
		if(e.getKeyChar()=='x'){
			System.out.println("x skipping");
			return;
		}
		*/

		myView.keyTyped(e);
	}
	public void keyReleased(KeyEvent e) { 
		switch(e.getKeyCode()) {
		case KeyEvent.VK_SHIFT:
			shiftKeyDown = false;
			//System.out.println("shift up");
			break;
		
		}
		
	}
	public void mouseMoved(MouseEvent e) { }
	public void actionPerformed(ActionEvent e) { }
	public void mouseClicked(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) {	}

}
