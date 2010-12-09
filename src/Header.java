interface Header {
	static final int MIN_ROW	= 1;
	static final int AMULET_LEVEL= 26;
	static final int LAST_map = 99;

	// Values for the level map:
	static final int SEEN		=     01;
	static final int MAPPED		=     02;
	static final int SEEMAP		=     03;

	static final int STAIRS		=     04;
	static final int HORWALL	=    010;
	static final int VERTWALL	=    020;
	static final int DOOR		=    040;
	static final int FLOOR		=   0100;
	static final int TUNNEL		=   0200;
	static final int TRAP		=   0400;
	static final int TOY		=  01000;
	static final int MONSTER	=  02000;
	static final int HOLDER     =  04000;	// May contain trap/toy/monster
	static final int DARK		= 010000;	// Dark place
	static final int BLOCK		= 020000;	// rox (oak tree)
	static final int GORE		= 040000;	// gore
	static final int HIDDEN		=0100000;
	static final int MAN		=0200000;	// The rogue is here


	static final int DROPHERE	= (SEEMAP| DOOR|FLOOR|TUNNEL|MAN|HOLDER|MONSTER);
	static final int SOMETHING  =  03774;

	static final int uNormal	= 0x000;	// Light gray
	static final int uWeak		= 0x100;	// Dark red (gore)
	static final int uBlack		= 0x200;	// Black is invisible
	static final int uBrite		= 0x300;	// White
	static final int uRed		= 0x400;
	static final int uRogue		= 0x500;
	static final int uGray		= 0x600;	// Gray
	static final int uGreen		= 0x700;
}

