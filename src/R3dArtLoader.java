import java.util.*;
import javax.vecmath.Color3f;


public class R3dArtLoader {
	
	
	public R3dArtLoader(){
		
	}
	
	
	public void load(HashMap<Character,R3dBasicObject> objects){
		
		String path = "assets/models/";
		
		//can use this to set colors/textures...
		
		//rogue
		R3dBasicObject thing = new R3dobject(path+"knight.obj");
		thing.setCol(  new Color3f(.2f,.3f,.6f));
		objects.put('@', thing);
		
		//wall
		thing = new R3dobject(path+"box.obj");
		objects.put('|', thing); objects.put('-', thing);
		
		//floor
		thing = new R3dobject(path+"floor.obj");
		objects.put('.', thing);
		
		//+ door
		thing = new R3dobject(path+"floor.obj");
		thing.setCol( new Color3f(.6f,.2f,.6f));
		objects.put('+', thing);
		
		//path
		//#
		thing = new R3dobject(path+"floor.obj");
		objects.put('#', thing);
		
		//% stairs down
		thing = new R3dobject(path+"floor.obj");
		thing.setCol(  new Color3f(.4f,.5f,.2f));
		objects.put('%', thing);

		//gold
		thing = new R3dobject(path+"carrot.obj");
		thing.setCol(  new Color3f(.7f,.7f,.1f));
		objects.put('*', thing);
		
		//food
		thing = new R3dobject(path+"carrot.obj");
		thing.setCol(  new Color3f(.7f,.4f,.1f));
		objects.put(':', thing);
		
		//hobgolin
		thing = new R3dobject(path+"goblin.obj");
		thing.setCol(  new Color3f(.2f,.7f,.1f));
		objects.put('H', thing);
		
		
		//E emu
		thing = new R3dobject(path+"antlion.obj");
		thing.setCol( new Color3f(.2f,.7f,.1f));
		objects.put('E', thing);
		
		//S snake
		thing = new R3dobject(path+"frog.obj");
		thing.setCol(  new Color3f(.2f,.7f,.1f));
		objects.put('S', thing);
		
		
		//K kestrel
		thing = new R3dobject(path+"hellpig.obj");
		thing.setCol(  new Color3f(.2f,.7f,.1f));
		objects.put('K', thing);
		
		//I icemonster
		thing = new R3dobject(path+"ratspike.obj");
		thing.setCol(  new Color3f(.9f,.9f,.9f));
		objects.put('I', thing);

		//O orc
		thing = new R3dobject(path+"ogro.obj");
		thing.setCol( new Color3f(.2f,.6f,.1f));
		objects.put('O', thing);
		
		
		//need:

		//? scroll
		//! potion

		//B bat
		//= ring
		// ) arrow
		//new gold
		//^ trap
		//R rattlesnake
		//] armor
		// / wand
		//Q quagga
		//C
		//Z zombie
		
		//arrow
		thing = new R3dglobj();
		thing.setCol(  new Color3f(.1f,.1f,.1f));
		objects.put(')', thing);
		
		
		
		
		
	}
	
	
	
	

}
