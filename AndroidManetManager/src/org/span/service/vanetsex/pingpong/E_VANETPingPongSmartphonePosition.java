package org.span.service.vanetsex.pingpong;

public enum E_VANETPingPongSmartphonePosition {
	BOARD_BOARD, BOARD_SEAT, SEAT_SEAT;
	
	private static String[] names = null;
	
	public static String[] names() {
		if(names == null) {
			E_VANETPingPongSmartphonePosition[] vs = values();
		    names = new String[vs.length];

		    for (int i = 0; i < vs.length; i++) {
		        names[i] = vs[i].name();
		    }
		}
	    return names;
	}
}
