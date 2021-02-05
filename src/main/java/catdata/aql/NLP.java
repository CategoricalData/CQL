package catdata.aql;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import catdata.Pair;
import catdata.Quad;
import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;

public class NLP {

	StanfordCoreNLP pipeline;

	public NLP() {
		Properties props = PropertiesUtils.asProperties("annotators",
				"tokenize,ssplit,pos,lemma,depparse,natlog,openie");
		pipeline = new StanfordCoreNLP(props);
	}

	public List<Pair<String, List<Quad<Double, String, String, String>>>> main(String text) {
		Annotation doc = new Annotation(text);
		pipeline.annotate(doc);

		List<Pair<String, List<Quad<Double, String, String, String>>>> ret = new LinkedList<>();
		for (CoreMap sentence : doc.get(CoreAnnotations.SentencesAnnotation.class)) {
			String sen = sentence.get(CoreAnnotations.TextAnnotation.class);
			List<Quad<Double, String, String, String>> list = new LinkedList<>();

			Collection<RelationTriple> triples = sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);

			for (RelationTriple triple : triples) {
				list.add(new Quad<>(triple.confidence, triple.subjectGloss(), triple.relationGloss(),
						triple.objectGloss()));
			}

			ret.add(new Pair<>(sen, list));
		}

		return ret;
	}

}