package br.unb.cic.modelling.models.goal;

import br.unb.cic.modelling.enums.TypesAttributesEnum;
import br.unb.cic.modelling.models.PropertyModel;

public class Perform extends PropertyModel {
	
	public Perform() {
		this.setType(TypesAttributesEnum.CHECKBOX);
		this.setName("Perform");
	}

}
