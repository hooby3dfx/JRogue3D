import java.util.StringTokenizer;
import java.io.*;
import java.awt.Event;

class Identifychar {
	static char c_cmd[]= new char[48];
	static String c_desc[]= new String[48];
	static {
		c_cmd[ 0]='?';	c_desc[ 0]=	"?       prints help";
		c_cmd[ 1]='r';	c_desc[ 1]=	"r       read scroll";
		c_cmd[ 2]='/';	c_desc[ 2]=	"/       identify object";
		c_cmd[ 3]='e';	c_desc[ 3]=	"e       eat food";
		c_cmd[ 4]='h';	c_desc[ 4]=	"h       left ";
		c_cmd[ 5]='w';	c_desc[ 5]=	"w       wield a weapon";
		c_cmd[ 6]='j';	c_desc[ 6]=	"j       down";
		c_cmd[ 7]='W';	c_desc[ 7]=	"W       wear armor";
		c_cmd[ 8]='k';	c_desc[ 8]=	"k       up";
		c_cmd[ 9]='T';	c_desc[ 9]=	"T       take armor off";
		c_cmd[10]='l';	c_desc[10]=	"l       right";
		c_cmd[11]='P';	c_desc[11]=	"P       put on ring";
		c_cmd[12]='y';	c_desc[12]=	"y       up & left";
		c_cmd[13]='R';	c_desc[13]=	"R       remove ring";
		c_cmd[14]='u';	c_desc[14]=	"u       up & right";
		c_cmd[15]='d';	c_desc[15]=	"d       drop object";
		c_cmd[16]='b';	c_desc[16]=	"b       down & left";
		c_cmd[17]='c';	c_desc[17]=	"c       call object";
		c_cmd[18]='n';	c_desc[18]=	"n       down & right";
		c_cmd[19]= '\0';c_desc[19]=	"<SHIFT><dir>: run that way";
		c_cmd[20]=')';	c_desc[20]=	")       print current weapon";
		c_cmd[21]='\0';	c_desc[21]=	"<CTRL><dir>: run till adjacent";
		c_cmd[22]=']';	c_desc[22]=	"]       print current armor";
		c_cmd[23]='f';	c_desc[23]=	"f<dir>  fight till death or near death";
		c_cmd[24]='=';	c_desc[24]=	"=       print current rings";
		c_cmd[25]='t';	c_desc[25]=	"t<dir>  throw something";
		c_cmd[26]='\001';c_desc[26]="^A      print Hp-raise average";
		c_cmd[27]='m';	c_desc[27]=	"m<dir>  move onto without picking up";
		c_cmd[28]='z';	c_desc[28]=	"z<dir>  zap a wand in a direction";
		c_cmd[29]='o';	c_desc[29]=	"o       examine/set options";
		c_cmd[30]='^';	c_desc[30]=	"^<dir>  identify trap type";
		c_cmd[31]='\022';c_desc[31]="^R      redraw screen";
		//c_cmd[32]='&';	c_desc[32]=	"&       save screen into 'rogue.screen'";
		c_cmd[33]='s';	c_desc[33]=	"s       search for trap/secret door";
		c_cmd[34]='\020';c_desc[34]="^P      repeat last message";
		c_cmd[35]='>';	c_desc[35]=	">       go down a staircase";
		c_cmd[36]='\033';c_desc[36]="^[      cancel command";
		c_cmd[37]='<';	c_desc[37]=	"<       go up a staircase";
		c_cmd[38]='S';	c_desc[38]=	"S       save game";
		c_cmd[39]='.';	c_desc[39]=	".       rest for a turn";
		c_cmd[40]='Q';	c_desc[40]=	"Q       quit";
		c_cmd[41]=',';	c_desc[41]=	",       pick something up";
		//c_cmd[42]='!';	c_desc[42]=	"!       shell escape";
		c_cmd[43]='i';	c_desc[43]=	"i       inventory";
		c_cmd[44]='F';	c_desc[44]=	"F<dir>  fight till either of you dies";
		//c_cmd[45]='I';	c_desc[45]=	"I       inventory single item";
		c_cmd[46]='v';	c_desc[46]=	"v       print version number";
		c_cmd[47]='q';	c_desc[47]=	"q       quaff potion";
		c_desc[19]= "";
		c_desc[21]= "";
		c_desc[26]= "";
		c_desc[42]= "";
		c_desc[45]= "";
	}
	static void cmds_list(char ch, Message msg){
		if(ch=='*' || ch=='?')
			msg.rightlist(c_desc,false);
		else{
			String desc[]= new String[1];
			desc[0]= "No such command: " + ch;
			for(int k= 0; k<c_cmd.length; k++)
				if(ch==c_cmd[k]){
					desc[0]= c_desc[k];
					break;
				}
			msg.rightlist(desc,false);
		}
	}
}
