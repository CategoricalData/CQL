package catdata.ide;



import java.awt.Color;
import java.util.Collection;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.text.Element;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;

import catdata.Unit;
import gnu.trove.set.hash.THashSet;


class SpellChecker extends AbstractParser {
	
	private static Collection<String> words0;

	private boolean spPrev = true;
	
	private synchronized static Collection<String> getWords() {
		if (words0 != null) {
			return words0;
		}
		words0 = new THashSet<>();
		return words0;
		/*
		//SpellChecker.class.getR
		InputStream in = SpellChecker.class.getResourceAsStream("/words.txt"); 
		if (in == null) {
			System.err.println("Warning: no words for spellchecker found.  If you are building from source, make sure words.txt is on the classpath.");
			return words0;
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String line;
			while ((line = reader.readLine()) != null) {
				words0.add(line.toLowerCase().trim());
			}				
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return words0;
		*/
	}
	
	private final DefaultParseResult result;
	private RSyntaxDocument doc;
	private final Function<Unit, Collection<String>> local;
	JCheckBox box;
	
	public SpellChecker(Function<Unit, Collection<String>> local, JCheckBox box) {
		result = new DefaultParseResult(this);
		this.local = local;
		this.box = box;
	}


	private int getLineOfOffset(int offs) {
		return doc.getDefaultRootElement().getElementIndex(offs);
	}

	private static final Pattern pattern = Pattern.compile("\\S+");
	
	
	@Override
	public ParseResult parse(RSyntaxDocument doc1, String style) {
		boolean spNow = box.isSelected();
		if (spNow != spPrev) {
			spPrev = spNow;
			result.clearNotices();
			return parse(doc1, style);
		}
		spPrev = spNow;
        doc = doc1;
		Element root = doc.getDefaultRootElement();
		int lineCount = root.getElementCount();
		result.clearNotices();
		result.setParsedLines(0, lineCount-1);
		if (!spNow) {
			return result;
		}
		//try {
		for (Token t : doc) {
			if (t.isComment()) {
				int startOffs = t.getOffset();
				String comment = t.getLexeme();
								
				Matcher matcher = pattern.matcher(comment); 
				while (matcher.find()) {
					String word = matcher.group();
					String word2= word.toLowerCase().replaceAll("[^a-z ]", "");
					
					if (word2.length() > 0 && !getWords().contains(word2) && !local.apply(Unit.unit).contains(word.replace("`", "").replaceAll("\"", "").replaceAll(".", "").replaceAll(",", "").replaceAll(":", "")))  {
						spellingError(word, startOffs + matcher.start());
					} 

				}

				
			
				
			}
		}
		return result;

	}


	
	private void spellingError(String word, int off) {
		//int offs = startOffs + off;
		int line = getLineOfOffset(off);
		String text = word; //noticePrefix + word + noticeSuffix;
		SpellingParserNotice notice =
			new SpellingParserNotice(this, text, line, off, word.length());
		result.addNotice(notice);
		
	}


	private static class SpellingParserNotice extends DefaultParserNotice {
	
		private SpellingParserNotice(SpellChecker parser, String msg,
									int line, int offs, int len
									) {
			super(parser, msg, line, offs, len);
			setLevel(Level.WARNING);
		}

		@Override
		public Color getColor() {
			return Color.orange;
		}

	}

}
