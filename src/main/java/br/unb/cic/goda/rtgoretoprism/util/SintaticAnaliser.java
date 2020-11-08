package br.unb.cic.goda.rtgoretoprism.util;

import java.util.regex.*;

import br.unb.cic.goda.model.ModelTypeEnum;
import br.unb.cic.goda.rtgoretoprism.generator.goda.producer.RTGoreProducer;

public class SintaticAnaliser {

	public static void verifyModel(String rtRegex, RTGoreProducer rtgp) {
		if (rtRegex != null) {
			if (isMDP(rtgp.getTypeModel())) {
				// Verifica se o modulo NonDeterminism existe pra ser gerado
				if (rtRegex.contains("DM")) {
					String reg1 = "^(DM\\().*?(\\))";
					String reg2 = "[^0-9A-Za-z\\(\\,\\)]";

					Matcher matcherDM1 = Pattern.compile(reg1).matcher(rtRegex);
					Matcher matcherDM2 = Pattern.compile(reg2).matcher(rtRegex);
					if (!matcherDM1.find() || matcherDM2.find()) {
						throw new RuntimeException("Invalid sintax '" + rtRegex + "' in ' " + matcherDM2.group() + " '");
					}
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
