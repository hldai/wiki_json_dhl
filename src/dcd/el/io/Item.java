package dcd.el.io;


// e.g.
// TITLE 1
// Autism
public class Item {
	public void fixNumLines() {
		if (value == null || value.equals("")) {
			numLines = 0;
			return;
		}
		
		numLines = 1;
		for (int i = 0; i < value.length(); ++i) {
			if (value.charAt(i) == '\n') {
				++numLines;
			}
		}
	}
	
	public String key = null;
	public String value = null;
	public int numLines = 0;
}
