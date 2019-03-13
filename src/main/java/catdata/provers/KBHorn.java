package catdata.provers;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


@SuppressWarnings({"rawtypes", "unchecked"})
public class KBHorn {

	public static final String _false = "_false";
	public static final String _true = "_true";
	public static final String _eq = "_eq";
	public static final String _not = "_not";
	public static final String _or = "_or";
	public static final String[] reserved = new String[] {_false, _true, _eq, _not, _or};
	
	private static KBExp falseAtom, trueAtom; 
	public static <C,V> KBExp<C,V> fals() {
		if (falseAtom != null) {
			return falseAtom;
		}
		falseAtom = KBExpFactoryOldImpl.factory.KBApp(_false, Collections.emptyList());
		return falseAtom;
	}
	public static <C,V> KBExp<C,V> tru() {
		if (trueAtom != null) {
			return trueAtom;
		}
		trueAtom = KBExpFactoryOldImpl.factory.KBApp(_true, Collections.emptyList());
		return trueAtom;
	}
	public static <C,V> KBExp<C,V> not(KBExp<C,V> e) {
		List l = new LinkedList<>();
		l.add(e);
		return KBExpFactoryOldImpl.factory.KBApp(_not, l);
	}
	public static <C,V> KBExp<C,V> or(KBExp<C,V> e1, KBExp<C,V> e2) {
		List l = new LinkedList();
		l.add(e1);
		l.add(e2);
		return KBExpFactoryOldImpl.factory.KBApp(_or, l);
	}
	public static <C,V> KBExp<C,V> eq(KBExp<C,V> e1, KBExp<C,V> e2) {
		List l = new LinkedList();
		l.add(e1);
		l.add(e2);
		return KBExpFactoryOldImpl.factory.KBApp(_eq, l);
	}

	public static <C,V> boolean isAtom(KBExp<C,V> e) {

        if (e.isVar()) {
            return false;
        }
        if (e.equals(tru())) {
            return true;
        }
        if (e.equals(fals())) {
            return true;
        }
        KBExp app = e;
        return app.f().equals(_or) || app.f().equals(_not) || (app.f().equals(_eq));
    }
}
