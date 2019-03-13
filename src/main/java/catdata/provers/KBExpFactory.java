package catdata.provers;

import java.util.List;

public interface KBExpFactory<T,C,V> {

	public KBExp<C, V> KBApp(C c, List<KBExp<C, V>> in);
	
	public KBExp<C, V> KBVar(V v);

	public KBExp<C, V> freshConst(T ty, int n);
	
}
