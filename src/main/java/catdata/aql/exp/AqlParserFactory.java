package catdata.aql.exp;


public abstract class AqlParserFactory {

	public enum Mode {
		COMBINATOR, ANTLR4
	}

	public static Mode mode =
			// Mode.ANTLR4;
			Mode.COMBINATOR;

	protected AqlParserFactory() {
	}

	public static IAqlParser getParser() {
		/*
		 * if(AqlParserFactory.mode == Mode.COMBINATOR) { return new CombinatorParser();
		 * } else if(AqlParserFactory.mode == Mode.ANTLR4) { return new Antlr4Parser();
		 * } else {
		 */
		return new CombinatorParser();
		// }
	}

	public static IAqlParser getParser(final Mode mode) {
		// if(mode == Mode.COMBINATOR) {
		return new CombinatorParser();
		// }
		// else if(mode == Mode.ANTLR4) {
		// return new Antlr4Parser();
		// }
		// else {
		// return new CombinatorParser();
		// }
	}

}
