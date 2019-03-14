package catdata.ide;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import catdata.aql.gui.AqlCodeEditor;

public enum Language {

	EASIK, SKETCH,

//	FPQL,
//	OPL,
//	MPL,
	CQL, CQL_ALT;

	public static Language getDefault() {
		return CQL;
	}

	// non easik ones
	public static Language[] values0() {
		List<Language> l = new LinkedList<>(Arrays.asList(values()));
		// l.remove(EASIK);
		l.remove(SKETCH);
		return l.toArray(new Language[0]);
	}

	@Override
	public String toString() {
		switch (this) {
		// case MPL: return "MPL";
		case CQL:
			return "CQL";
		case CQL_ALT:
			return "CQL ALT";
		case EASIK:
			return "EASIK";
		case SKETCH:
			return "Sketch";
		default:
			break;
		}
		throw new RuntimeException("Anomaly - please report");
	}

	public String prefix() {
		switch (this) {
		// case MPL: return "M";
		case CQL:
			return " ";
		case CQL_ALT:
			return " ";
		case EASIK:
			return "E";
		case SKETCH:
			return "S";
		default:
			break;
		}
		throw new RuntimeException("Anomaly - please report");
	}

	public String fileExtension() {
		switch (this) {
		case CQL:
			return "cql";
		case CQL_ALT:
			return "cql";
		case EASIK:
			return "easik";
		case SKETCH:
			return "sketch";
		default:
			throw new RuntimeException("Anomaly - please report");
		}

	}

	public String filePath() {
		switch (this) {
		case CQL:
			return "cql";
		case CQL_ALT:
			return "cql_extra";
		case EASIK:
			return "easik";
		case SKETCH:
			return "sketch";
		default:
			throw new RuntimeException("Anomaly - please report");
		}

	}

	@SuppressWarnings({ "rawtypes" })
	public CodeEditor createEditor(String title, int id, String content) {
		switch (this) {
		case CQL:
			return new AqlCodeEditor(title, id, content);
		case CQL_ALT:
			return new AqlCodeEditor(title, id, content);
		case EASIK:
		case SKETCH:
		default:
			throw new RuntimeException("Anomaly - please report");
		}

	}

	public List<Example> getExamples() {
		switch (this) {
		case CQL:
			return Examples.getExamples(Language.CQL);
		case CQL_ALT:
			return Examples.getExamples(Language.CQL_ALT);
		case EASIK:
			return Examples.getExamples(Language.EASIK);
		case SKETCH:
			return Examples.getExamples(Language.SKETCH);
		default:
			throw new RuntimeException("Anomaly - please report");
		}

	}
}
