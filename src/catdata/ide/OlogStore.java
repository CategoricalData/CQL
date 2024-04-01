package catdata.ide;

import java.util.List;
import java.util.Set;

import catdata.ide.Olog.OlogMappingName;
import catdata.ide.Olog.OlogName;
import catdata.ide.Olog.OlogPresentation;
import catdata.ide.OlogMapping.OlogColimitPresentation;
import catdata.ide.OlogMapping.OlogMappingPresentation;

public interface OlogStore {

	public void clear();

	public void save();
	
	public void saveAs(String file);

	public void open(String file);

	public String file();
	
	public boolean dirty();
	
	////////////////////////////////////////////////////////////////
	
	public Set<OlogName> listOlogs();

	public Olog getOlog(OlogName name);

	public void deleteOlog(OlogName name);

	
	//add node, delete node, add edge, delete edge, add gen/sk, delete gen/sk, add rule, delete rule 
	public void addLiteralOlog(OlogName name, OlogPresentation code);

	public void addComposedOlog(OlogName name, OlogColimitPresentation code);

	//////////
	
	public Set<OlogMappingName> listMappings();

	public OlogMapping getMapping(OlogMappingName name);

	public void deleteMapping(OlogMappingName name);

	public void addLiteralMapping(OlogMappingName name,  OlogMappingPresentation code);

	public void addComposedMapping(OlogMappingName name, List<OlogMappingName> ms);

	


}
