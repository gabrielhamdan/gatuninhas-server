package org.gatuninhas.api.model;

public enum Sex {
	
	MALE("M", "masculino"),
	FEMALE("F", "feminino"),
	INTERSX("I", "intersexo");
	
	public final String CODE;
	public final String DESCRIPTION;
	
	Sex(String code, String description) {
        CODE = code;
        DESCRIPTION = description;
    }

}
