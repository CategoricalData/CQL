package catdata;

public class Unit {

	public static final Unit unit = new Unit();
	
	private Unit() {}

	@Override
	public String toString() {
		return "()";
	}
	
	@Override
	public boolean equals(Object o) {
        return o.getClass() == Unit.class;
    }
	
	@Override
	public int hashCode() {
		return 0;
	}

}
