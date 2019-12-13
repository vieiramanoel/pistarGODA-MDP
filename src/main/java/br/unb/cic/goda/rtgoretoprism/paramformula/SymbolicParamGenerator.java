package br.unb.cic.goda.rtgoretoprism.paramformula;

import java.util.Map;

public class SymbolicParamGenerator {

	public StringBuilder getAndReliability(String[] nodes, Map<String, String> ctxInformation) {
		
		StringBuilder formula = new StringBuilder();
		
		formula.append("( ");
		
		for (String id : nodes) {
			//Children is context-dependent
			if (ctxInformation.containsKey(id)) {
				formula.append(" CTX_" + id + " * " + id + " * ");
			}
			//Children is context-free
			else {
				formula.append(id + " * ");
			}
		}
		formula.delete(formula.lastIndexOf("*"), formula.length()-1);
		formula.append(")");
		return formula;
	}

	public StringBuilder getSequentialAndCost (String[] nodes, String rootId, Map<String, String> ctxInformation, boolean isParam) {
		
		StringBuilder formula = new StringBuilder();
		
		if (isParam) {
			formula.append("(");
			for (String id : nodes) {
				if (ctxInformation.containsKey(id)) {
					formula.append(" CTX_" + id + " * " + id + " +");
				}
				else {
					formula.append(" " + id + " +");
				}
			}
			formula.deleteCharAt(formula.length()-1);
			formula.append(" ) * R_" + rootId + " ");
		}
		else {
			formula.append("(");
			StringBuilder reliability = new StringBuilder();
			for (String id : nodes) {
				if (ctxInformation.containsKey(nodes)) {
					if (reliability.length() == 0) {
						reliability.append("R_" + id);
						formula.append(" CTX_" + id + " * " + id + " + ");
					}
					else {
						formula.append(" ( " + reliability.toString() + " ) * CTX_" + id + " * " + id + " +");
						reliability.append(" * R_" + id);
					}
				}
				else {
					if (reliability.length() == 0) {
						reliability.append("R_" + id);
						formula.append(" " + id + " + ");
					}
					else {
						formula.append(" ( " + reliability.toString() + " ) * " + id + " +");
						reliability.append(" * R_" + id);
					}
				}	
			}
			formula.deleteCharAt(formula.length()-1);
			formula.append(" ) ");
		}
		
		return formula;
	}

	public StringBuilder getParallelAndCost(String[] nodes, String rootId, Map<String, String> ctxInformation, boolean isParam) {
	
		StringBuilder formula = new StringBuilder();
		formula.append("(");
		
		if (isParam) {
			int numNodes = nodes.length;
			
			if ((numNodes % 2) != 0) {
				numNodes = (numNodes+1)/2;
				formula.append(" (" + numNodes + " ) * ");
			}
			else {
				numNodes++;
				formula.append(" (" + numNodes + "/2) * ");
			}
			formula.append(getSequentialAndCost(nodes, rootId, ctxInformation, isParam));
		}
		else {
			for (String id: nodes) {
				if (ctxInformation.containsKey(nodes)) {
					if (id.equals(nodes[0])) {
						formula.append(" CTX_" + id + " * " + id);
					}
					else {
						formula.append(" + CTX_" + id + " * " + id);
					}
				}
				else {
					if (id.equals(nodes[0])) {
						formula.append(" " + id);
					}
					else {
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
			}
			else {
				formula.append(" ( 1 - " + id + " ) * ");
			}
		}
		formula.delete(formula.lastIndexOf("*"), formula.length()-1);
		formula.append(")");
		
		return formula;
	}

	public StringBuilder getSequentialOrCost(String[] nodes, String rootId, Map<String, String> ctxInformation,
			boolean isParam) {
		
		StringBuilder formula = new StringBuilder();
		
		if (isParam) {
			
			StringBuilder reliability = new StringBuilder();
			String lastNode = new String();
			String lastFormula = new String();
			
			formula.append("(");
			reliability.append("( 1 - ");
			
			for (String id : nodes) {
				
				if (id.equals(nodes[nodes.length-1])) {
					StringBuilder reliabilityPrev = new StringBuilder(reliability);
					reliabilityPrev.delete(reliabilityPrev.lastIndexOf("*"), reliabilityPrev.length()-1);
					reliabilityPrev.append(")");
					lastNode = id;
					lastFormula = reliabilityPrev.toString();
				}
				
				if (ctxInformation.containsKey(id)) {
					reliability.append(" ( 1 - CTX_" + id + " * R_" + id + " ) * ");
					formula.append(" CTX_" + id + " * " + id + " +");
				}
				else {
					reliability.append(" ( 1 - R_" + id + " ) * ");
					formula.append(" " + id + " +");
				}
			}
			reliability.delete(reliability.lastIndexOf("*"), reliability.length()-1);
			reliability.append(")");
			
			formula.deleteCharAt(formula.length()-1);
			formula.append(" ) * " + reliability.toString() + " - ( " + lastFormula + " * " + lastNode + " )");
		}
		else {
			formula.append("(");
			StringBuilder reliability = new StringBuilder();
			
			for (String id : nodes) {
				
				if (ctxInformation.containsKey(nodes)) {
					if (reliability.length() == 0) {
						reliability.append("(1 - R_" + id + " )");
						formula.append(" CTX_" + id + " * " + id + " + ");
					}
					else {
						formula.append(" ( " + reliability.toString() + " ) * CTX_" + id + " * " + id + " +");
						reliability.append(" * (1 - R_" + id + " )");
					}
				}
				else {
					if (reliability.length() == 0) {
						reliability.append("(1 - R_" + id + " )");
						formula.append(" " + id + " + ");
					}
					else {
						formula.append(" ( " + reliability.toString() + " ) * " + id + " +");
						reliability.append(" * (1 - R_" + id + " )");
					}
				}	
			}
			formula.deleteCharAt(formula.length()-1);
			formula.append(" ) ");
		}
		
		return formula;
	}
}
