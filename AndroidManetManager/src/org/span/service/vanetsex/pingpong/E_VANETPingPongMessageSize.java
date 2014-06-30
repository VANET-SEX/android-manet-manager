package org.span.service.vanetsex.pingpong;


public enum E_VANETPingPongMessageSize {
	SMALL(100), MEDIUM(32 * 1024), LARGE(64 * 1024 - 200), RANDOM(-1);
	
	private int value;
	private static String[] names = null;

	private E_VANETPingPongMessageSize(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	public static String[] names() {
		if(names == null) {
			E_VANETPingPongMessageSize[] vs = values();
		    names = new String[vs.length];

		    for (int i = 0; i < vs.length; i++) {
		        names[i] = vs[i].name();
		    }
		}
	    return names;
	}
}
