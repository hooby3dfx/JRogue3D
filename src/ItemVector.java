import java.util.Vector;
import java.io.*;
import java.util.Enumeration;

class ItemVector extends Vector  implements Serializable {
	private static final long serialVersionUID= 3L;
	static final int PACKSIZE=26;
	ItemVector(){
		super();
	}
	ItemVector(int n){
		super(n);
	}
	void relevel(Level level){
		Enumeration e= elements();
		while(e.hasMoreElements())
			((Item)e.nextElement()).level= level;
	}
	Item item_at(int row, int col){
		int i= size();
		while(--i>=0){
			Item p= (Item)elementAt(i);
			if(p.row==row && p.col==col)
				return p;
		}
		return null;
	}
	Item get_letter_toy(int ch){	// Call on the rogue's pack
		int i= size();
		while(--i>=0){
			Item p= (Item)elementAt(i);
				if(p.ichar == ch)
					return p;
		}
		return null;
	}
	int inventory(int mask, Message msg, boolean ask){
		int i= size();
		String descs[];
		descs= new String[1];
		descs[0]= "--";

		if(i==0){
			msg.message("your pack is empty");
			return '\033';
		}
		int n= 0;
		Enumeration e= elements();
		while(e.hasMoreElements()){
			Toy obj= (Toy)e.nextElement();
			if(0 != (obj.kind & mask))
				++n;
		}
		if(n>0){
			descs= new String[n];
			n= 0;
			e= elements();
			while(e.hasMoreElements()){
				Toy obj= (Toy)e.nextElement();
				if(0 != (obj.kind & mask)){
					int k= obj.ichar>='a' && obj.ichar<='z'? obj.ichar:n;
					descs[n++]= single_inv(k);
				}
			}
		}
		if(n==0){
 			descs= new String[1];
			descs[0]= "--nothing appropriate--";
		}
		return msg.rightlist(descs, ask);
	}
	String single_inv(int ch){
		if(ch<'a')
			ch += 'a';
		Enumeration e= elements();
		Toy obj= null;
		while(e.hasMoreElements()){
			obj= (Toy)e.nextElement();
			if(obj.ichar==ch)
				break;
		}
		if(obj==null)
			return "";
		String sep= ") ";
		if(0 !=(obj.kind & Id.ARMOR) && obj.is_protected)
			sep= "} ";
		return " " + (char)obj.ichar + sep + obj.get_desc();
	}
	boolean mask_pack(int mask){
		int i= size();
		while(--i>=0){
			Toy t= (Toy)elementAt(i);
			if(0 != (t.kind & mask))
				return true;
		}
		return false;
	}
	char next_avail_ichar(){
		int i;
		boolean ichars[]= new boolean[PACKSIZE];

		for(i= 0; i < PACKSIZE; i++)
			ichars[i]= false;
		i= size();
		while(--i>=0)try{
			Toy obj= (Toy)elementAt(i);
			ichars[obj.ichar - 'a']= true;
		}catch(ArrayIndexOutOfBoundsException e){
		}
		for(i= 0; i < PACKSIZE; i++)
			if(!ichars[i])
				return (char)(i + 'a');
		return '?';
	}
	/*
	void uncurse_all(){
		Enumeration e= elements();
		while(e.hasMoreElements())
			((Toy)e.nextElement()).is_cursed= false;
	}
	*/
	int sell_all(){
		Enumeration e= elements();
		int gold= 0;
		while(e.hasMoreElements()){
			Toy t= (Toy)e.nextElement();
			gold += t.quantity * t.value();
		}
		return gold;
	}
}

