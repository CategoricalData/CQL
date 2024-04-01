package catdata.cql.gui;

import catdata.cql.gui.WarehouseUI.Example;

public class NISTExample extends Example {

	public String getOptions() {
		return """
				options
						require_consistency=false
						allow_sql_import_all_unsafe=true
						sql_constraints_simple=true
						simplify_names=false
						allow_aggregation_unsafe=true
						jdbc_quote_char="`"
						""";
	}
	
	@Override
	public String getName() {
		return "NIST";
	}
	public boolean push() {
		return false;
	}
	@Override
	public String getSources() {
		return """
				schema S1 = import_jdbc_all "jdbc:mysql://localhost:3306/portala?user=root&password=password&useSSL=false"
instance S1Instance = import_jdbc_direct "jdbc:mysql://localhost:3306/portala?user=root&password=password&useSSL=false"
 "ROW_NUMBER() OVER (Order by CQL_ZERO)" : S1  
constraints S1Constraints =  from_mysql "jdbc:mysql://localhost:3306/portala?user=root&password=password&useSSL=false"
 : S1  
command c1 = check S1Constraints S1Instance

schema S2 = import_jdbc_all "jdbc:mysql://localhost:3306/portalb?user=root&password=password&useSSL=false"
instance S2Instance = import_jdbc_direct "jdbc:mysql://localhost:3306/portalb?user=root&password=password&useSSL=false"
 "ROW_NUMBER() OVER (Order by CQL_ZERO)" : S2  
constraints S2Constraints =  from_mysql "jdbc:mysql://localhost:3306/portalb?user=root&password=password&useSSL=false"
 : S2  
command c2 = check S2Constraints S2Instance
				""";
	}

	@Override
	public String getLinks() {
		return """
				entity_isomorphisms
		LinkMaterial : S1.Material -> S2.Material
		LinkIndustry : S1.Industry -> S2.Industry
		LinkSupplier : S1.Supplier -> S2.Supplier
		LinkMold : S1.MoldTypes -> S2.MoldOrProcessType
		LinkEDMCapability : S1.Capability -> S2.EDMCapability
		LinkMachiningCapability : S1.Capability -> S2.MachiningCapability
		#LinkCapability : S0.CapabilitiesMaster -> S1.Capability
		
	equations
	
	eq1: forall x:S1.Capability, x.Capability_Name = x.LinkEDMCapability.EDMCapabilitiesName
	eq2: forall x:S1.Capability, x.Capability_Name = x.LinkMachiningCapability.MachiningCapabilityName
	eq3: forall x:S1.Material, x.Material_Name = x.LinkMaterial.MaterialName
	eq4: forall x:S1.Industry, x.Industry_Name = x.LinkIndustry.IndustryName
	eq5: forall x:S1.MoldTypes, x.MoldTypes_Name = x.LinkMold.MoldOrProcessTypeName
	eq6: forall x:S1.Supplier, x.Source = x.LinkSupplier.Source
				
				constraints
r1: forall s1m:S1_Material s2m:S2_Material
	where s1m.Material_Name = s2m.MaterialName -> where s1m.LinkMaterial = s2m

	r2: forall s1i:S1_Industry s2i:S2_Industry
	where s1i.Industry_Name = s2i.IndustryName -> where s1i.LinkIndustry = s2i

	r3: forall s1s:S1_Supplier s2s:S2_Supplier
	where s1s.Source = s2s.Source -> where s1s.LinkSupplier = s2s

	r4: forall s1m:S1_MoldTypes s2m:S2_MoldOrProcessType
	where s1m.MoldTypes_Name = s2m.MoldOrProcessTypeName -> where s1m.LinkMold = s2m

	r5: forall s0c:S1_Capability s2c:S2_EDMCapability
	where s0c.Capability_Name = s2c.EDMCapabilitiesName -> where s0c.LinkEDMCapability = s2c
	
	r6: forall s0c:S1_Capability s2c:S2_MachiningCapability
	where s0c.Capability_Name = s2c.MachiningCapabilityName -> where s0c.LinkMachiningCapability = s2c
				
				""";
	
	}

	@Override
	public String getTargets() {
		return "";
	}

	
}
