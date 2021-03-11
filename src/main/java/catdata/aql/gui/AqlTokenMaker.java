package catdata.aql.gui;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

import catdata.aql.Kind;
import catdata.aql.AqlProver.ProverName;
import catdata.aql.exp.IAqlParser;

public class AqlTokenMaker extends AbstractTokenMaker {

	@Override
	public synchronized void addToken(Segment segment, int start, int end, int tokenType, int startOffset) {
		// This assumes all keywords, etc. were parsed as "identifiers."
		// if (tokenType==Token.IDENTIFIER) {
		int value = wordsToHighlight.get(segment, start, end);
		if (value != -1) {
			tokenType = value;
		}
		// }
		super.addToken(segment, start, end, tokenType, startOffset);
	}

	int currentTokenStart = -1;
	int currentTokenType = -1;

	@Override
	public synchronized Token getTokenList(Segment text, int startTokenType, int startOffset) {
		resetTokenList();
		char[] array = text.array;
		int offset = text.offset;
		int count = text.count;
		int end = offset + count;
		int newStartOffset = startOffset - offset;

		currentTokenStart = offset;
		currentTokenType = startTokenType;

		outer: for (int i = offset; i < end; i++) {
			char c = array[i];
			if (currentTokenType == Token.WHITESPACE || currentTokenType == Token.IDENTIFIER
					|| currentTokenType == Token.OPERATOR || currentTokenType == Token.NULL) {
				for (char z : IAqlParser.opsC) {
					if (z == c) {
						if (currentTokenType != Token.OPERATOR && currentTokenType != Token.NULL) {
							addToken(text, currentTokenStart, i - 1, currentTokenType, newStartOffset + currentTokenStart);
						}
						currentTokenStart = i;
						currentTokenType = Token.NULL;
						addToken(text, currentTokenStart, i, Token.OPERATOR, newStartOffset + currentTokenStart);

						continue outer;
					}
				}
			}
			if (currentTokenType == Token.WHITESPACE || currentTokenType == Token.IDENTIFIER
					|| currentTokenType == Token.OPERATOR || currentTokenType == Token.NULL) {

				if (c == '{' || c == '}') {
					if (currentTokenType != Token.OPERATOR && currentTokenType != Token.NULL) {
						addToken(text, currentTokenStart, i - 1, currentTokenType, newStartOffset + currentTokenStart);
					}
					currentTokenStart = i;
					currentTokenType = Token.NULL;
					addToken(text, currentTokenStart, i, Token.SEPARATOR, newStartOffset + currentTokenStart);

					continue outer;
				}

			}

			switch (currentTokenType) {
				// case Token.SEPARATOR:
				// addToken(text, currentTokenStart, i - 1, currentTokenType, newStartOffset +
				// currentTokenStart);
				// currentTokenStart = i;
				// currentTokenType = Token.SEPARATOR;
				// break;

//                                        addToken(text, currentTokenStart, i, Token.OPERATOR,
				// newStartOffset + currentTokenStart);

				case Token.NULL:
					currentTokenStart = i; // Starting a new token here.
					start(c);
					break;
				case Token.WHITESPACE:
					whitespace(text, newStartOffset, i, c);
					break;
				case Token.LITERAL_NUMBER_DECIMAL_INT:
					i = literalNumber(text, newStartOffset, i, c);
					break;
				case Token.COMMENT_EOL:
					i = end - 1;
					addToken(text, currentTokenStart, i, currentTokenType, newStartOffset + currentTokenStart);
					currentTokenType = Token.NULL;
					break;
				case Token.LITERAL_BACKQUOTE:
					currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
					break;
				case Token.LITERAL_STRING_DOUBLE_QUOTE:
					if (c == '\\') {
						currentTokenType = Token.LITERAL_BACKQUOTE;
					} else if (c == '"') {
						addToken(text, currentTokenStart, i, Token.LITERAL_STRING_DOUBLE_QUOTE,
								newStartOffset + currentTokenStart);
						currentTokenType = Token.NULL;
					}
					break;
				default:
				case Token.IDENTIFIER:
					identifier(text, newStartOffset, i, c);
					break;

			} // End of switch (currentTokenType).

		} // End of for (int i=offset; i<end; i++).

		switch (currentTokenType) {
			// Remember what token type to begin the next line with.
			case Token.LITERAL_STRING_DOUBLE_QUOTE:
				addToken(text, currentTokenStart, end - 1, currentTokenType, newStartOffset + currentTokenStart);
				break;

			// Do nothing if everything was okay.
			case Token.NULL:
				addNullToken();
				break;

			case Token.SEPARATOR:
				addNullToken();
				break;

			// All other token types don't continue to the next line...
			default:
				addToken(text, currentTokenStart, end - 1, currentTokenType, newStartOffset + currentTokenStart);
				addNullToken();

		}
		return firstToken;
	}

	private int literalNumber(Segment text, int newStartOffset, int i, char c) {
		switch (c) {

			/*
			 * case '{': addToken(text, currentTokenStart, i - 1,
			 * Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset + currentTokenStart);
			 * currentTokenType = Token.SEPARATOR; currentTokenStart = i; break; case '}':
			 * addToken(text, currentTokenStart, i - 1, Token.LITERAL_NUMBER_DECIMAL_INT,
			 * newStartOffset + currentTokenStart); currentTokenStart = i; currentTokenType
			 * = Token.SEPARATOR; break;
			 */
			case ' ':
			case '\t':
				addToken(text, currentTokenStart, i - 1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset + currentTokenStart);
				currentTokenStart = i;
				currentTokenType = Token.WHITESPACE;
				break;

			case '"':
				addToken(text, currentTokenStart, i - 1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset + currentTokenStart);
				currentTokenStart = i;
				currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
				break;

			default:

				if (RSyntaxUtilities.isDigit(c)) {
					break; // Still a literal number.
				}

				// Otherwise, remember this was a number and start over.
				addToken(text, currentTokenStart, i - 1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset + currentTokenStart);
				i--;
				currentTokenType = Token.NULL;

		} // End of switch (c).
		return i;
	}

	private void identifier(Segment text, int newStartOffset, int i, char c) {
		switch (c) {

			/*
			 * case '{': addToken(text, currentTokenStart, i - 1, Token.IDENTIFIER,
			 * newStartOffset + currentTokenStart); currentTokenType = Token.SEPARATOR;
			 * currentTokenStart = i; break; case '}': addToken(text, currentTokenStart, i -
			 * 1, Token.IDENTIFIER, newStartOffset + currentTokenStart); currentTokenStart =
			 * i; currentTokenType = Token.SEPARATOR; break;
			 */

			case '"':
				addToken(text, currentTokenStart, i - 1, Token.IDENTIFIER, newStartOffset + currentTokenStart);
				currentTokenStart = i;
				currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
				break;

			default:
				if (Character.isWhitespace(c)) {
					addToken(text, currentTokenStart, i - 1, Token.IDENTIFIER, newStartOffset + currentTokenStart);
					currentTokenStart = i;
					currentTokenType = Token.WHITESPACE;
				} else if (RSyntaxUtilities.isLetterOrDigit(c) || c == '/' || c == '_') {
					break; // Still an identifier of some type.
				}
				addToken(text, currentTokenStart, i - 1, Token.IDENTIFIER, newStartOffset + currentTokenStart);
				currentTokenStart = i;
				// start(c);

		}
	}

	private void whitespace(Segment text, int newStartOffset, int i, char c) {
		switch (c) {
			/*
			 * case '{': addToken(text, currentTokenStart, i - 1, Token.WHITESPACE,
			 * newStartOffset + currentTokenStart); currentTokenStart = i; currentTokenType
			 * = Token.SEPARATOR; break; case '}': addToken(text, currentTokenStart, i - 1,
			 * Token.WHITESPACE, newStartOffset + currentTokenStart); currentTokenStart = i;
			 * currentTokenType = Token.SEPARATOR; break;
			 */

			case '"':
				addToken(text, currentTokenStart, i - 1, Token.WHITESPACE, newStartOffset + currentTokenStart);
				currentTokenStart = i;
				currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
				break;

			case '#':
				addToken(text, currentTokenStart, i - 1, Token.WHITESPACE, newStartOffset + currentTokenStart);
				currentTokenStart = i;
				currentTokenType = Token.COMMENT_EOL;
				break;

			default: // Add the whitespace token and start anew.

				addToken(text, currentTokenStart, i - 1, Token.WHITESPACE, newStartOffset + currentTokenStart);
				currentTokenStart = i;

				if (RSyntaxUtilities.isDigit(c)) {
					currentTokenType = Token.LITERAL_NUMBER_DECIMAL_INT;
					break;
				} else if (Character.isWhitespace(c)) {
					currentTokenType = Token.WHITESPACE;
					break;
				}

				// Anything not currently handled - mark as identifier
				currentTokenType = Token.IDENTIFIER;

		} // End of switch (c).
	}

	private void start(char c) {

		switch (c) {

//                        case '{':
			// currentTokenType = Token.SEPARATOR;
			// break;
			// case '}':
			// currentTokenType = Token.SEPARATOR;
			// break;

			case '"':
				currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
				break;

			case '#':
				currentTokenType = Token.COMMENT_EOL;
				break;

			default:
				if (RSyntaxUtilities.isDigit(c)) {
					currentTokenType = Token.LITERAL_NUMBER_DECIMAL_INT;
					break;
				} else if (Character.isWhitespace(c)) {
					currentTokenType = Token.WHITESPACE;
					break;
				}

				// Anything not currently handled - mark as an identifier
				currentTokenType = Token.IDENTIFIER;
				break;

		} // End of switch (c).
	}

	@Override
	public synchronized TokenMap getWordsToHighlight() {
		TokenMap tokenMap = new TokenMap();

		for (Kind x : Kind.values()) {
			tokenMap.put(x.toString(), Token.RESERVED_WORD);
		}
		for (String x : IAqlParser.res) {
			tokenMap.put(x, Token.RESERVED_WORD_2);
		}
		for (String x : IAqlParser.opts) {
			tokenMap.put(x, Token.FUNCTION);
		}
		for (String x : IAqlParser.ops) {
			tokenMap.put(x, Token.OPERATOR);
		}

		return tokenMap;
	}

}
