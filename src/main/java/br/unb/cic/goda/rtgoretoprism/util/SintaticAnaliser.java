package br.unb.cic.goda.rtgoretoprism.util;

import br.unb.cic.goda.model.ModelTypeEnum;
import br.unb.cic.goda.rtgoretoprism.generator.goda.producer.RTGoreProducer;

public class SintaticAnaliser {

	public static void verifyModel(String rtRegex, RTGoreProducer rtgp) {
		if(rtRegex != null) {
			if(isMDP(rtgp.getTypeModel())) {
				//Verifica se o modulo NonDeterminism existe pra ser gerado
				if(rtgp.getRtDMGoals().isEmpty() && rtRegex.contains("[DM")) {
					throw new RuntimeException("sintaxe inválida em '" + rtRegex + "'");
				}
			}
		}
	}
	
	private static boolean isMDP(String typeModel) {
		ModelTypeEnum enumn = ModelTypeEnum.valueOf(typeModel);
		return enumn.equals(ModelTypeEnum.MDP);
	}
	
	private static boolean isDTMC(String typeModel) {
		ModelTypeEnum enumn = ModelTypeEnum.valueOf(typeModel);
		return enumn.equals(ModelTypeEnum.DTMC);
	}
}
