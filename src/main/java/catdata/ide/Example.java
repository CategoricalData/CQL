package catdata.ide;

public abstract class Example implements Comparable<Example> {
	
	public abstract String getName(); 
	
	public abstract String getText();
	
	@SuppressWarnings("static-method")
	public Language lang() {
		return null;
	}

	@Override 
	public String toString() {
		String pre = lang() == null ? "" : lang().prefix();
		return (pre + "  " + getName()).trim();
	}
	
	@Override 
	public int compareTo(Example e) {
		return toString().compareTo(e.toString());
	}




}
