package br.unb.cic.goda.rtgoretoprism.util;

import java.util.List;

import br.unb.cic.goda.model.ModelTypeEnum;

public class SintaticAnaliser {

	public static void verifyModel(String rtRegex, List<String> rtDMGoals, String typeModel) {
		if(rtRegex != null) {
			if(isMDP(typeModel)) {
				//Verifica se o modulo NonDeterminism existe pra ser gerado
				if(rtDMGoals.isEmpty()) {
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
