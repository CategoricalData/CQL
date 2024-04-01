package catdata.cql.exp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;

public class Ian {

	public static void main(String[] args) {
		String infile = "/Users/ryan/Downloads/filtered_youtube_comments_with_stance.csv";
		String outfile = "/Users/ryan/Downloads/newian" + System.currentTimeMillis() + "/";
		int last = 0;

		try {
			Character sepChar = ',';
			Character quoteChar = '\"';
			Character escapeChar = '\\';

			final CSVParser parser = new CSVParserBuilder().withSeparator(sepChar).withQuoteChar(quoteChar)
					.withEscapeChar(escapeChar).withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS).build();

			Set<Map<String, String>> authors = new HashSet<>();
			Set<Map<String, String>> videos = new HashSet<>();
			Set<Map<String, String>> channels = new HashSet<>();
			Set<Map<String, String>> comments = new HashSet<>();

			Reader r = new FileReader(infile);

			final CSVReader reader = new CSVReaderBuilder(r).withCSVParser(parser)
					.withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS).build();

			List<String[]> rows = reader.readAll();
			boolean first = true;
			for (String[] row : rows) {
				last++;
				if (first) {
					first = false;
					continue;
				}
				String authorName = row[0].replace("'", "").replace(",", "").replace("\n", "").replace("\r", "");
				String channelId = row[1].replace("'", "").replace(" ", "");
				String videoId = row[2].replace("'", "").replace(" ", "");
				String commentId = row[3].replace("'", "").replace(" ", "");
				String parentId = row[4].replace("'", "").replace(" ", "");
				String text = row[5];
				String like_count = row[6];
				String authorId = row[7];
				String user_stance = row[8];
				Map<String, String> author = new HashMap<>();
				author.put("author_id", authorId);
				author.put("name", authorName);
				// author.put("like_count", like_count);
				author.put("user_stance", user_stance);
				authors.add(author);

				String[] videoIds = videoId.substring(1, videoId.length() - 1).split(",");
				String[] commentIds = commentId.substring(1, commentId.length() - 1).split(",");
				String[] parentIds = parentId.substring(1, parentId.length() - 1).split(",");
				String[] channelIds = channelId.substring(1, channelId.length() - 1).split(",");
				if (videoIds.length == commentIds.length && parentIds.length == channelIds.length
						&& videoIds.length == channelIds.length) {

				} else {
					throw new Exception("On row " + row + "\nthere is a length mismatch");
				}

				for (int i = 0; i < videoIds.length; i++) {
					Map<String, String> comment = new HashMap<>();
					Map<String, String> video = new HashMap<>();
					Map<String, String> channel = new HashMap<>();

					video.put("video_id", videoIds[i]);
					video.put("channel_id", channelIds[i]);

					comment.put("comment_id", commentIds[i].replace(parentIds[i] + ".", ""));
					comment.put("parent_id", parentIds[i]);
					comment.put("author_id", authorId);
					comment.put("video_id", videoIds[i]);
					comment.put("channel_id", channelIds[i]);

					// author.put("text", text);
					channel.put("channel_id", channelIds[i]);

					videos.add(video);
					comments.add(comment);
					channels.add(channel);
				}

			}

			File dir = new File(outfile);
			dir.mkdir();
			File v = new File(outfile + "Video.csv");
			v.createNewFile();
			File co = new File(outfile + "Comment.csv");
			co.createNewFile();
			File ch = new File(outfile + "Channel.csv");
			ch.createNewFile();
			File a = new File(outfile + "Author.csv");
			a.createNewFile();

			write(outfile + "Video.csv", videos, "video_id", "channel_id");
			write(outfile + "Comment.csv", comments, "comment_id", "parent_id", "author_id", "video_id", "channel_id");
			write(outfile + "Channel.csv", channels, "channel_id");
			write(outfile + "Author.csv", authors, "author_id", "name", "user_stance");
			
			System.out.println(comments.size());
			var x = hashJoin(comments, "author_id", authors, "author_id");
			System.out.println(x.size());
			x = join(x, "video_id", videos, "video_id");
			System.out.println(x.size());
			x = join(x, "channel_id", channels, "channel_id");
			System.out.println(x.size());
			
			write(outfile + "All.csv", x, "video_id", "channel_id", "comment_id", "parent_id", "author_id", "name", "user_stance");
	
			
			var ret = join(comments, "video_id", comments, "video_id");
			System.out.println("dd" + ret.size());
			write(outfile + "Link.csv", ret, "comment_id", "comment_id_2");

		} catch (Exception ex) {
			System.out.println("Row number " + last);
			ex.printStackTrace();
		}
	}

	private static Map<String, Set<Map<String, String>>> makeIndex(Set<Map<String, String>> r, String c) {
		Map<String, Set<Map<String, String>>> i = new HashMap<>();
		for (Map<String, String> row : r) {
			String value = row.get(c);
			if (!i.containsKey(value)) {
				i.put(value, new HashSet<>());
			}
			i.get(value).add(row);
		}
		return i;
	}

	private static Set<Map<String, String>> join(Set<Map<String, String>> r1, String c1,
			Set<Map<String, String>> r2, String c2) {
		Set<Map<String, String>> ret = new HashSet<>();

		int i = 0;
		for (Map<String, String> row1 : r1) {
				for (Map<String, String> row2 : r2) {
					if (!row1.get(c1).equals(row2.get(c2))) {
						continue;
					}
					Map<String, String> m = new HashMap<>();

					for (var k : row1.entrySet()) {
						m.put(k.getKey(), k.getValue());						
					}
					for (var k : row2.entrySet()) {
						if (m.containsKey(k.getKey())) {
							m.put(k.getKey() + "_2", k.getValue());													
						} else {
							m.put(k.getKey(), k.getValue());						
						}
					}
					if (!m.isEmpty()) {
						i++;
						ret.add(m);					
					}

				}
			}
System.out.println("+++ " + i);
		return ret;
	}
	
	private static Set<Map<String, String>> hashJoin(Set<Map<String, String>> r1, String c1,
			Set<Map<String, String>> r2, String c2) {
		Set<Map<String, String>> ret = new HashSet<>();

		Map<String, Set<Map<String, String>>> i2 = makeIndex(r2, c2);
		int i = 0;
		for (Map<String, String> row1 : r1) {
			Set<Map<String, String>> set = i2.get(row1.get(c1));
			if (set != null) {
				Map<String, String> m = new HashMap<>();
				for (Map<String, String> row2 : set) {
				
					for (var k : row1.entrySet()) {
						m.put(k.getKey(), k.getValue());						
					}
					for (var k : row2.entrySet()) {
						if (m.containsKey(k.getKey())) {
							m.put(k.getKey() + "_2", k.getValue());													
						} else {
							m.put(k.getKey(), k.getValue());						
						}
					}
				}
				if (!m.isEmpty()) {
					i++;
					ret.add(m);					
				}
			}
		}
System.out.println("*** " + i);
		return ret;
	}

	private static void write(String ff, Set<Map<String, String>> rows, String... cols) throws IOException {
		File f = new File(ff);
		f.createNewFile();
		BufferedWriter p = new BufferedWriter(new FileWriter(f));
		boolean first = true;
		StringBuffer sb = new StringBuffer();

		for (String col : cols) {
			if (!first) {
				sb.append(",");
			}
			first = false;
			sb.append(col);
		}
		p.write(sb.toString());
		p.newLine();

		for (Map<String, String> row : rows) {
			sb = new StringBuffer();
			first = true;
			for (String col : cols) {
				if (!first) {
					sb.append(",");
				}
				first = false;
				String s = row.get(col);
				if (s != null) {
					sb.append(s);
				}
			}
			p.write(sb.toString());
			p.newLine();
		}
		p.flush();
		p.close();
	}

}
