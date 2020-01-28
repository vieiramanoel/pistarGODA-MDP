package br.unb.cic.goda.rtgoretoprism.paramformula;

import java.util.Map;

public class SymbolicParamGenerator {

	public StringBuilder getAndReliability(String[] nodes, Map<String, String> ctxInformation) {

		StringBuilder formula = new StringBuilder();

		formula.append("( ");

		for (String id : nodes) {
			// Children is context-dependent
			if (ctxInformation.containsKey(id)) {
				formula.append(" CTX_" + id + " * " + id + " * ");
			}
			// Children is context-free
			else {
				formula.append(id + " * ");
			}
		}
		formula.delete(formula.lastIndexOf("*"), formula.length() - 1);
		formula.append(")");
		return formula;
	}

	public StringBuilder getSequentialAndCost(String[] ids, String rootId, Map<String, String> ctxInformation,
			boolean isParam) {

		StringBuilder formula = new StringBuilder();

		if (isParam) {
			formula.append("(");
			for (String id : ids) {
				if (ctxInformation.containsKey(id)) {
					formula.append(" CTX_" + id + " * " + id + " +");
				} else {
					formula.append(" " + id + " +");
				}
			}
			formula.deleteCharAt(formula.length() - 1);
			formula.append(" ) * R_" + rootId + " ");
		} else {
			formula.append("(");
			StringBuilder reliability = new StringBuilder();
			for (String id : ids) {
				if (ctxInformation.containsKey(id)) {
					if (reliability.length() == 0) {
						reliability.append("R_" + id);
						formula.append(" CTX_" + id + " * " + id + " + ");
					} else {
						formula.append(" ( " + reliability.toString() + " ) * CTX_" + id + " * " + id + " +");
						reliability.append(" * R_" + id);
					}
				} else {
					if (reliability.length() == 0) {
						reliability.append("R_" + id);
						formula.append(" " + id + " + ");
					} else {
						formula.append(" ( " + reliability.toString() + " ) * " + id + " +");
						reliability.append(" * R_" + id);
					}
				}
			}
			formula.deleteCharAt(formula.length() - 1);
			formula.append(" ) ");
		}

		return formula;
	}

	public StringBuilder getParallelAndCost(String[] ids, String rootId, Map<String, String> ctxInformation,
			boolean isParam) {

		StringBuilder formula = new StringBuilder();
		formula.append("(");

		if (isParam) {
			int numNodes = ids.length;

			if ((numNodes % 2) != 0) {
				numNodes = (numNodes + 1) / 2;
				formula.append(" (" + numNodes + " ) * ");
			} else {
				numNodes++;
				formula.append(" (" + numNodes + "/2) * ");
			}
			formula.append(getSequentialAndCost(ids, rootId, ctxInformation, isParam));
		} else {
			for (String id : ids) {
				if (ctxInformation.containsKey(id)) {
					if (id.equals(ids[0])) {
						formula.append(" CTX_" + id + " * " + id);
					} else {
						formula.append(" + CTX_" + id + " * " + id);
					}
				} else {
					if (id.equals(ids[0])) {
						formula.append(" " + id);
					} else {
						formula.append(" + " + id);
					}
				}

			}
		}
		formula.append(" )");
		return formula;
	}

	public StringBuilder getOrReliability(String[] ids, Map<String, String> ctxInformation) {

		StringBuilder formula = new StringBuilder();

		formula.append("( 1 - ");
		for (String id : ids) {
			if (ctxInformation.containsKey(id)) {
				formula.append(" ( 1 - CTX_" + id + " * " + id + " ) * ");
			} else {
				formula.append(" ( 1 - " + id + " ) * ");
			}
		}
		formula.delete(formula.lastIndexOf("*"), formula.length() - 1);
		formula.append(")");

		return formula;
	}

	public StringBuilder getSequentialOrCost(String[] ids, String rootId, Map<String, String> ctxInformation,
			boolean isParam) {

		StringBuilder formula = new StringBuilder();

		if (isParam) {

			StringBuilder reliability = new StringBuilder();
			String lastNode = new String();
			String lastFormula = new String();

			formula.append("(");
			reliability.append("( 1 - ");

			for (String id : ids) {

				if (id.equals(ids[ids.length - 1])) {
					StringBuilder reliabilityPrev = new StringBuilder(reliability);
					reliabilityPrev.delete(reliabilityPrev.lastIndexOf("*"), reliabilityPrev.length() - 1);
					reliabilityPrev.append(")");
					lastNode = id;
					lastFormula = reliabilityPrev.toString();
				}

				if (ctxInformation.containsKey(id)) {
					reliability.append(" ( 1 - CTX_" + id + " * R_" + id + " ) * ");
					formula.append(" CTX_" + id + " * " + id + " +");
				} else {
					reliability.append(" ( 1 - R_" + id + " ) * ");
					formula.append(" " + id + " +");
				}
			}
			reliability.delete(reliability.lastIndexOf("*"), reliability.length() - 1);
			reliability.append(")");

			formula.deleteCharAt(formula.length() - 1);
			formula.append(" ) * " + reliability.toString() + " - ( " + lastFormula + " * " + lastNode + " )");
		} else {
			formula.append("(");
			StringBuilder reliability = new StringBuilder();

			for (String id : ids) {

				if (ctxInformation.containsKey(id)) {
					if (reliability.length() == 0) {
						reliability.append("(1 - R_" + id + " )");
						formula.append(" CTX_" + id + " * " + id + " + ");
					} else {
						formula.append(" ( " + reliability.toString() + " ) * CTX_" + id + " * " + id + " +");
						reliability.append(" * (1 - R_" + id + " )");
					}
				} else {
					if (reliability.length() == 0) {
						reliability.append("(1 - R_" + id + " )");
						formula.append(" " + id + " + ");
					} else {
						formula.append(" ( " + reliability.toString() + " ) * " + id + " +");
						reliability.append(" * (1 - R_" + id + " )");
					}
				}
			}
			formula.deleteCharAt(formula.length() - 1);
			formula.append(" ) ");
		}

		return formula;
	}

	public StringBuilder getParallelOrCost(String[] ids, String rootId, Map<String, String> ctxInformation,
			boolean isParam) {

		StringBuilder formula = new StringBuilder();

		if (isParam) {
			throw new IllegalStateException("Parrallel OR cost formula for PARAM is not implemented");
		} else {
			formula = getParallelAndCost(ids, rootId, ctxInformation, false);
		}

		return formula;
	}

	public StringBuilder getDMReliability(String[] ids, Map<String, String> ctxInformation, boolean isParam) {

		StringBuilder formula = new StringBuilder();

		if (isParam) {
			throw new IllegalStateException("DM parametric formula for PARAM is not implemented");
		} else {
			formula.append("(");
			for (String id : ids) {
				// possible invalid situation, throw exception ?
				if (!ctxInformation.containsKey(id)) {
					System.out.println("[WARN] Node '" + id + "' has no context information and is in a DM");
				}

				formula.append(" CTX_" + id + " * " + id + " +");

			}
			formula.deleteCharAt(formula.length() - 1);
			formula.append(" ) ");
		}

		return formula;
	}

	public StringBuilder getRetryReliability(String[] ids, String rootId, Map<String, String> ctxInformation,
			boolean isParam, int retryNum) {

		StringBuilder formula = new StringBuilder();

		if (isParam) {
			formula.append("( 1 - ( 1 - ");
			if (ctxInformation.containsKey(ids[0])) {
				formula.append("CTX_" + ids[0] + " * " + ids[0] + " )^" + retryNum + " )");
			} else {
				formula.append(ids[0] + " )^" + retryNum + " )");
			}
		} else {
			if (ctxInformation.containsKey(ids[0])) {
				formula.append("( CTX_" + ids[0] + " * " + ids[0]);
			} else {
				formula.append("( " + ids[0]);
			}

			for (int i = 1; i <= retryNum; i++) {
				if (ctxInformation.containsKey(ids[0])) {
					formula.append(
							" + CTX_" + ids[0] + " * " + ids[0] + " * (1 - CTX_" + ids[0] + " * " + ids[0] + " )^" + i);
				} else {
					formula.append(" + " + ids[0] + " * (1 - " + ids[0] + " )^" + i);
				}
			}
			formula.append(" )");
		}

		return formula;
	}

	public StringBuilder getRetryCost(String[] ids, String rootId, Map<String, String> ctxInformation, boolean isParam,
			int retryNum) {
		StringBuilder formula = new StringBuilder();

		if (isParam) {
			throw new IllegalStateException("RETRY parametric formula for PARAM is not implemented");
		} else {
			if (ctxInformation.containsKey(ids[0])) {
				formula.append("( CTX_" + ids[0] + " * " + ids[0]);
			} else {
				formula.append("( " + ids[0]);
			}

			for (int i = 1; i <= retryNum; i++) {
				if (ctxInformation.containsKey(ids[0])) {
					formula.append(" + CTX_" + ids[0] + " * " + ids[0] + " * (1 - CTX_" + ids[0] + " * R_" + ids[0]
							+ " )^" + i);
				} else {
					formula.append(" + " + ids[0] + " * (1 - R_" + ids[0] + " )^" + i);
				}
			}
			formula.append(" )");
		}

		return formula;
	}

	public StringBuilder getTryReliability(String[] ids, Map<String, String> ctxInformation, boolean isParam) {
		StringBuilder formula = new StringBuilder();

		if (ids.length != 3) {
			throw new IllegalStateException("TRY should have 3 parameters, and received: " + ids.toString());
		}

		String p1 = ctxInformation.containsKey(ids[0]) ? "CTX_" + ids[0] : "";
		String p2 = ctxInformation.containsKey(ids[1]) ? "CTX_" + ids[1] : "";
		String p3 = ctxInformation.containsKey(ids[2]) ? "CTX_" + ids[2] : "";

		p1 = p1 + " * " + ids[0];
		p2 = p2 + " * " + ids[1];
		p3 = p3 + " * " + ids[2];
		
		formula.append("( " + p1 + " * " + p2 + " - " + p1 + " * " + p3 + " + " + p3 + ")");

		return formula;
	}
	
	public StringBuilder getTryCost(String[] ids, Map<String, String> ctxInformation, boolean isParam) {
		StringBuilder formula = new StringBuilder();

		if (ids.length != 3) {
			throw new IllegalStateException("TRY should have 3 parameters, and received: " + ids.toString());
		}
		
		if (isParam) {
			throw new IllegalStateException("TRY parametric formula for PARAM is not implemented");
		}

		String r1 = ctxInformation.containsKey(ids[0]) ? "CTX_" + ids[0] : "";
		r1 = r1 + " * R_" + ids[0];

		formula.append("( " + ids[0] + " + " + r1 + " * " +  ids[1] + "+ (1 - " + r1 + " ) * " + ids[2] + ")");

		return formula;
	}
}
