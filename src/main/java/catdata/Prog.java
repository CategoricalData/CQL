package catdata;

import java.util.Collection;

public interface Prog {

	int getLine(String s);

	Collection<String> keySet();

	@SuppressWarnings("unused")
	default String kind(String s) {
		return "";
	}

	default long timeout() {
		return 30;
	}

}
