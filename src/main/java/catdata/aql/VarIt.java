package catdata.aql;

import java.util.Iterator;

public class VarIt implements Iterator<Var>  {

	private static int index = 0;
	
	private VarIt() { }
	
	public static Iterator<Var> it() {
			return new VarIt();
		}

	private
	Var fresh() {
		return Var.Var("v" + index++);
	}
	
	@Override
	public boolean hasNext() {
		return true;
	}

	@Override
	public Var next() {
		return fresh();
	}
	
}