package org.span.service.vanetsex.pingpong;

public enum E_VANETPingPongDistance {
	DISTANCE_50, DISTANCE_100, DISTANCE_150, DISTANCE_200, DISTANCE_250;
	
	private static String[] names = null;
	
	public static String[] names() {
		if(names == null) {
			E_VANETPingPongDistance[] vs = values();
		    names = new String[vs.length];

		    for (int i = 0; i < vs.length; i++) {
		        names[i] = vs[i].name();
		    }
		}
	    return names;
	}
}
