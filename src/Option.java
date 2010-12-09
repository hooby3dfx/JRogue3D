import java.io.*;
class Option  implements Serializable, Cloneable {
	private static final long serialVersionUID= 3L;
//	boolean flush= true;
//	boolean jump= false;
	boolean passgo= false;
//	boolean no_skull= false;
//	boolean ask_quit= true;
	String nick_name= "Rogue";
	String fruit= "slime_mold";
	boolean colormonster= false;

	static String prompt[]= {
		"Flush typeahead during battle: ",
		"Show position only at end of run: ",
		"Follow turnings in passageways: ",
		"Don't print skull when killed: ",
		"Ask player before saying 'Okay, bye-bye!': ",
		"Name: ",
		"Fruit: ",
		"Color weak & sleeping monsters: "};

	Option(){
	}
	private boolean bool_opt(Message msg, String prompt, boolean b){
		char c;
		do{
			c= '\033';
			msg.check_message();
			String s= msg.get_input_line(prompt + '[' + (b? "true":"false") + ']', "", "", false, true);
			if(s!=null && s.length()>0){
				if(s.indexOf('\033')>=0)
					break;
				c= s.charAt(0);
				if(c>='a')c -= 'a'-'A';
			}
		}while(c!='\033' && c!='T' && c!='F');
		if(c=='T')return true;
		if(c=='F')return false;
		return b;
	}
	private String string_opt(Message msg, String prompt, String v){
		char c;
		msg.check_message();
		String s= msg.get_input_line(prompt + '[' + v + ']', "", "", false, true);
		if(s==null || s.length()==0 || s.indexOf('\033')>=0)
			return v;
		return s;
	}
	void edit_opts(Message msg){
		//flush= bool_opt(msg, prompt[0], flush);
		//jump= bool_opt(msg, prompt[1], jump);
		//no_skull= bool_opt(msg, prompt[3], no_skull);
		//ask_quit= bool_opt(msg, prompt[4], ask_quit);
		nick_name= string_opt(msg, prompt[5], nick_name);
		fruit= string_opt(msg, prompt[6], fruit);
		passgo= bool_opt(msg, prompt[2], passgo);
		colormonster= bool_opt(msg, prompt[7], colormonster);
	}
	Option edit_opts(Man man){
		Option o= option_factory(null,null);
		if(o==null)
			return o;
		o.edit_opts(man.view.msg);
		return o.option_factory(null, null);
	}
/*
		Message msg= man.view.msg;
		//flush= bool_opt(msg, prompt[0], flush);
		//jump= bool_opt(msg, prompt[1], jump);
		boolean ipassgo= bool_opt(msg, prompt[2], passgo);
		//no_skull= bool_opt(msg, prompt[3], no_skull);
		//ask_quit= bool_opt(msg, prompt[4], ask_quit);
		String inick_name= string_opt(msg, prompt[5], nick_name);
		String ifruit= string_opt(msg, prompt[6], fruit);
		boolean icolormonster= bool_opt(msg, prompt[7], colormonster);
		Option o= man.option.option_factory(inick_name, ifruit);
*/			
	static String kosher(String stg, int maxlen){
		String badchars= "/|,;:\"\t";
		String goodchars="-- --' ";

		stg.trim();
		if(stg.length() > maxlen)
			stg= stg.substring(0,maxlen);
		for(int i= 0; i<badchars.length(); i++)
			stg= stg.replace(badchars.charAt(i), goodchars.charAt(i));
		return stg;
	}
	Option option_factory(String iname, String ifruit){
		try{
			Option o= (Option)clone();
			o.nick_name= kosher(iname==null? nick_name : iname, 20);
			o.fruit= kosher(ifruit==null? fruit : ifruit, 20);
			if(o.nick_name.length() > 1 && o.fruit.length() > 1)
				return o;
		}catch(Exception e){
			System.out.println("Oops in setName: "+e.toString());
		}
		return null;
	}
}
