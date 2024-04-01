package catdata.ide;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import catdata.Util;
import catdata.ide.Olog.OlogLiteral;
import catdata.ide.Olog.OlogMappingName;
import catdata.ide.Olog.OlogName;
import catdata.ide.Olog.OlogPresentation;
import catdata.ide.OlogMapping.OlogColimitPresentation;
import catdata.ide.OlogMapping.OlogMappingLiteral;
import catdata.ide.OlogMapping.OlogMappingPresentation;

// todo: validate deletes, etc
class OlogStoreImpl implements OlogStore {
	public Map<OlogName, Olog> ologs;
	public Map<OlogMappingName, OlogMapping> mappings;
	String file;
	boolean dirty;

	public OlogStoreImpl() {
		clear();
	}

	@Override
	public void clear() {
		ologs = Util.mk();
		mappings = Util.mk();
		dirty = false;
		file = null;
	}

	@Override
	public void save() {
		if (file == null)
			throw new RuntimeException("File not set");
		ObjectMapper mapper = new ObjectMapper();
		try {
			mapper.writeValue(Paths.get(file).toFile(), this);
			dirty = false;
		} catch (Exception e1) {
			throw new RuntimeException(e1); 
		}
		
	}

	@Override
	public void open(String file) {
		clear();
		this.file = file;
		// TODO Auto-generated method stub
	}

	@Override
	public void saveAs(String file) {
		this.file = file;
		save();
	}

	@Override
	public String file() {
		return file;
	}

	@Override
	public boolean dirty() {
		return dirty;
	}

	@Override
	public Set<OlogName> listOlogs() {
		return ologs.keySet();
	}

	@Override
	public Olog getOlog(OlogName name) {
		return ologs.get(name);
	}

	@Override
	public void deleteOlog(OlogName name) {
		ologs.remove(name);
	}

	@Override
	public void addLiteralOlog(OlogName name, OlogPresentation code) {
		ologs.put(name, new OlogLiteral(code));
		dirty = true;
	}

	@Override
	public Set<OlogMappingName> listMappings() {
		return mappings.keySet();
	}

	@Override
	public OlogMapping getMapping(OlogMappingName name) {
		return mappings.get(name);
	}

	@Override
	public void deleteMapping(OlogMappingName name) {
		mappings.remove(name);
		dirty = true;
	}

	@Override
	public void addLiteralMapping(OlogMappingName name, OlogMappingPresentation code) {
		mappings.put(name, new OlogMappingLiteral(code));
		dirty = true;
	}

	@Override
	public void addComposedOlog(OlogName name, OlogColimitPresentation code) {
		// ologs.put(name, new OlogCompose(code));
		dirty = true;
	}

	@Override
	public void addComposedMapping(OlogMappingName name, List<OlogMappingName> ms) {
		// mappings.put(name, new OlogMappingCompose(getMapping(code1),
		// getMapping(code2)));
		dirty = true;

	}

	

}