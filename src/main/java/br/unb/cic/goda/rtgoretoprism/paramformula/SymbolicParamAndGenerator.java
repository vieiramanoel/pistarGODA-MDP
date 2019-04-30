package br.unb.cic.goda.rtgoretoprism.paramformula;

import java.util.Map;

public class SymbolicParamAndGenerator {

	public StringBuilder getSequentialAndCost (String[] nodes, Map<String, String> ctxInformation) {
		
		//StringBuilder reliability = getReliability(nodes);
		
		StringBuilder formula = new StringBuilder();
		formula.append("(");
		for (String node : nodes) {
			if (ctxInformation.containsKey(node)) {
				//formula.append(reliability + " CTX_" + node + " * " + node + " +");
				formula.append(" CTX_" + node + " * " + node + " +");
			}
			else {
				//formula.append(reliability + " " + node + " +");
				formula.append(" " + node + " +");
			}
		}
		formula.deleteCharAt(formula.length()-1);
		formula.append(" )");
		return formula;
	}

	public String getParallelAndCost(String[] nodes) {
	
		int numNodes = nodes.length;
		
		StringBuilder reliability = getReliability(nodes);
		StringBuilder formula = new StringBuilder();
		formula.append("(");
		
		for (String node : nodes) {
			//Number even of nodes
			if ((numNodes % 2) == 0) {
				if (formula.length() == 1) formula.append("(");
				formula.append(" " + Integer.toString(numNodes+1) + " *" + reliability + " " + node + " +");
			}
			//Number odd of nodes
			else {
				formula.append(" 2 *" + reliability + " " + node + " +");
			}	
		}
		
		formula.deleteCharAt(formula.length()-1);
		formula.append(")");
		if ((numNodes % 2) == 0) formula.append("/2)");
		return formula.toString();
	}

	private StringBuilder getReliability(String[] nodes) {
		StringBuilder reliability = new StringBuilder();
		for (String node : nodes) {
			reliability.append(" R_" + node + " *");
		}
		return reliability;
	}
}
