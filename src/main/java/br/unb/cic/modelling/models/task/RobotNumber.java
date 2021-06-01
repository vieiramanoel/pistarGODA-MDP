package br.unb.cic.modelling.models.task;

import br.unb.cic.modelling.enums.TypesAttributesEnum;
import br.unb.cic.modelling.models.PropertyModel;

public class RobotNumber extends PropertyModel{

	public RobotNumber() {
		this.setName("RobotNumber");
		this.setType(TypesAttributesEnum.TEXT);
	}
}
