package catdata.provers;

import java.util.Map;

public abstract class DPKB<T,C,V> {

	public KBTheory<T, C, V> kb ;
	
	public DPKB(KBTheory<T, C, V> kb) {
		this.kb=kb;
	}

	public abstract boolean eq(Map<V, T> ctx, KBExp<C,V> lhs, KBExp<C,V> rhs);
		
	public abstract void add(C c, T t);

				
}
