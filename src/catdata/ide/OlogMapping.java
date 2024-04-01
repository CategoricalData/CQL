package catdata.ide;

import java.util.Map;

import catdata.ide.Olog.OlogEntity;
import catdata.ide.Olog.OlogFkExp;
import catdata.ide.Olog.OlogGenExp;
import catdata.ide.Olog.OlogMappingName;
import catdata.ide.Olog.OlogName;
import catdata.ide.Olog.OlogSkExp;
import catdata.ide.Olog.OlogTermEn;
import catdata.ide.Olog.OlogTermTy;

public abstract class OlogMapping {

	public abstract OlogMappingPresentation toPresentation();

	public static record OlogMappingPresentation(Map<OlogEntity, OlogEntity> objectMapping,
			Map<OlogFkExp, OlogTermEn> arrowMapping, Map<OlogFkExp, OlogTermTy> attMapping, Map<OlogGenExp, OlogTermEn> genMapping,
			Map<OlogSkExp, OlogTermTy> skMapping) {
	}

	public static record OlogColimitPresentation(Map<String, OlogName> m1, Map<String, OlogMappingName> m2,
			Map<String, OlogName> dom, Map<String, OlogName> cod) {
	}
	
	public static class OlogMappingLiteral extends OlogMapping {
		public final OlogMappingPresentation presentation;

		public OlogMappingLiteral(OlogMappingPresentation s) {
			presentation = s;
		}

		@Override
		public OlogMappingPresentation toPresentation() {
			return presentation;
		}
	}

	public static class OlogMappingCompose extends OlogMapping {
		public final OlogMapping first, second;

		public OlogMappingCompose(OlogMapping m1, OlogMapping m2) {
			this.first = m1;
			this.second = m2; // TODO
		}

		@Override
		public OlogMappingPresentation toPresentation() {
			// TODO Auto-generated method stub
			return null;
		}

	}
}