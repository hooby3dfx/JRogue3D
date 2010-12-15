import java.util.*;
import javax.vecmath.Color3f;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;


public class R3dArtLoader {
	
	
	public R3dArtLoader(){
		
	}
	
	
	public void load(HashMap<Character,R3dBasicObject> objects, GL gl, GLU glu){
				
		String path = "assets/models/";
		
		//can use this to set colors/textures...
		
		//rogue
		R3dBasicObject thing = new R3dobject(path+"knight.obj", true);
		//thing.setCol(  new Color3f(.2f,.3f,.6f));
		((R3dobject)thing).setTexture(path+"knight.png",gl, glu);
		objects.put('@', thing);
		
		//wall
		thing = new R3dobject(path+"box.obj",false);
		thing.setCol( new Color3f(.4f,.2f,.1f));
		objects.put('|', thing); objects.put('-', thing);
		
		//floor
		thing = new R3dobject(path+"floor.obj",true);	
		//thing.setCol( new Color3f(.3f, .2f, .1f));
		((R3dobject)thing).setTexture(path+"floor.png",gl, glu);
		objects.put('.', thing);
		
		//+ door
		thing = new R3dobject(path+"floor.obj",false);
		thing.setCol( new Color3f(.4f,.2f,.2f));
		objects.put('+', thing);
		
		//path
		//#
		thing = new R3dobject(path+"floor.obj",true);
		//thing.setCol( new Color3f(.3f, .2f, .1f));
		((R3dobject)thing).setTexture(path+"floor.png",gl, glu);
		objects.put('#', thing);
		
		//% stairs down
		thing = new R3dobject(path+"ladder.obj",true);
		//thing.setCol(  new Color3f(.4f,.5f,.2f));
		((R3dobject)thing).setTexture(path+"ladder.jpg",gl, glu);
		objects.put('%', thing);

		//gold
		thing = new R3dobject(path+"money.obj",true);
		//thing.setCol(  new Color3f(.7f,.7f,.1f));
		((R3dobject)thing).setTexture(path+"money.jpg",gl, glu);
		objects.put('*', thing);
		
		//food
		thing = new R3dobject(path+"carrot.obj",true);
		//thing.setCol(  new Color3f(.7f,.4f,.1f));
		((R3dobject)thing).setTexture(path+"carrot.jpg",gl, glu);
		objects.put(':', thing);
		
		//hobgolin
		thing = new R3dobject(path+"goblin.obj",true);
		//thing.setCol(  new Color3f(.2f,.7f,.1f));
		((R3dobject)thing).setTexture(path+"goblin.jpg",gl, glu);
		objects.put('H', thing);
		
		
		//E emu
		thing = new R3dobject(path+"antlion.obj",true);
		//thing.setCol( new Color3f(.2f,.7f,.1f));
		((R3dobject)thing).setTexture(path+"antlion.png",gl, glu);
		objects.put('E', thing);
		
		//S snake
		thing = new R3dobject(path+"frog.obj",true);
		//thing.setCol(  new Color3f(.2f,.7f,.1f));
		((R3dobject)thing).setTexture(path+"froggreen.png",gl, glu);
		objects.put('S', thing);
		
		
		//K kestrel
		thing = new R3dobject(path+"hellpig.obj",true);
		//thing.setCol(  new Color3f(.2f,.7f,.1f));
		((R3dobject)thing).setTexture(path+"hellpig.jpg",gl, glu);
		objects.put('K', thing);
		
		//I icemonster
		thing = new R3dobject(path+"ratspike.obj",false);
		thing.setCol(  new Color3f(.9f,.9f,.9f));
		objects.put('I', thing);

		//O orc
		thing = new R3dobject(path+"ogro.obj",true);
		//thing.setCol( new Color3f(.2f,.6f,.1f));
		((R3dobject)thing).setTexture(path+"ogro.jpg",gl, glu);
		objects.put('O', thing);		
		
		
		//! potion
		thing = new R3dobject(path+"bottle.obj",false);
		thing.setCol( new Color3f(.3f,.3f,.6f));
		objects.put('!', thing);

		//B bat or BUNNY
		thing = new R3dobject(path+"bunny.obj",false);
		thing.setCol( new Color3f(.3f,.3f,.1f));
		objects.put('B', thing);
		
		
		//^ trap
		thing = new R3dobject(path+"gibtrap.obj",true);
		((R3dobject)thing).setTexture(path+"gibtrap.jpg",gl, glu);
		//thing.setCol(  new Color3f(.6f,.3f,.1f));
		objects.put('^', thing);
		
		//R rattlesnake
		thing = new R3dobject(path+"frog.obj",true);
		//thing.setCol(  new Color3f(.6f,.3f,.1f));
		((R3dobject)thing).setTexture(path+"frogblue.png",gl, glu);
		objects.put('R', thing);
		
		//] armor
		thing = new R3dobject(path+"armor.obj",true);
		//thing.setCol(  new Color3f(.1f,.2f,.6f));
		((R3dobject)thing).setTexture(path+"armor.jpg",gl, glu);
		objects.put(']', thing);
		
		// ) arrow or weapon
		thing = new R3dglobj();
		thing.setCol(  new Color3f(.1f,.1f,.1f));
		objects.put(')', thing);
		
		//C=centaur
		thing = new R3dobject(path+"centaur.obj",true);
		//thing.setCol(  new Color3f(.1f,.1f,.1f));
		((R3dobject)thing).setTexture(path+"centaur.png",gl, glu);
		objects.put('C', thing);
		
		//L=leprochaun
		thing = new R3dobject(path+"cocomonk.obj",false);
		thing.setCol(  new Color3f(.2f,.7f,.1f));
		objects.put('L', thing);
		
		//Z zombie
		thing = new R3dobject(path+"slith.obj",true);
		//thing.setCol(  new Color3f(.3f,.5f,.1f));
		((R3dobject)thing).setTexture(path+"slith.jpg",gl, glu);
		objects.put('Z', thing);
	
		//= ring
		thing = new R3dglobj();
		thing.setCol(  new Color3f(.3f,.1f,.5f));
		((R3dglobj)thing).shape=R3dglobj.Torus;
		((R3dglobj)thing).size=.05f;
		objects.put('=', thing);
		
		// / wand
		thing = new R3dglobj();
		thing.setCol(  new Color3f(.3f,.1f,.5f));
		((R3dglobj)thing).shape=R3dglobj.Cone;
		objects.put('/', thing);
			
		//? scroll
		thing = new R3dglobj();
		thing.setCol(  new Color3f(.3f,.1f,.5f));
		((R3dglobj)thing).shape=R3dglobj.Cone;
		objects.put('?', thing);
		
		
		
		//Q quagga
		thing = new R3dobject(path+"rhino.obj",true);
		((R3dobject)thing).setTexture(path+"rhino.jpg",gl, glu);
		objects.put('Q', thing);
		
		//N nymph
		thing = new R3dobject(path+"pixie.obj",true);
		((R3dobject)thing).setTexture(path+"pixie.png",gl, glu);
		objects.put('N', thing);
		
		//pretty good through level 10
		//more?
		
		//Y yeti
		thing = new R3dobject(path+"bauul.obj",true);
		((R3dobject)thing).setTexture(path+"bauul.jpg",gl, glu);
		objects.put('Y', thing);
		
		//A aquator
		thing = new R3dobject(path+"ratspike.obj",true);
		((R3dobject)thing).setTexture(path+"ratspike.jpg",gl, glu);
		objects.put('A', thing);
		
		//W wraith
		
		
		
	}
	
	
	
	

}
