package br.unb.cic.goda.rtgoretoprism.generator.goda.producer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import br.unb.cic.goda.model.Actor;
import br.unb.cic.goda.model.Goal;
import br.unb.cic.goda.rtgoretoprism.generator.CodeGenerationException;
import br.unb.cic.goda.rtgoretoprism.generator.goda.parser.CostParser;
import br.unb.cic.goda.rtgoretoprism.generator.goda.writer.ManageWriter;
import br.unb.cic.goda.rtgoretoprism.generator.goda.writer.ParamWriter;
import br.unb.cic.goda.rtgoretoprism.generator.kl.AgentDefinition;
import br.unb.cic.goda.rtgoretoprism.model.kl.Const;
import br.unb.cic.goda.rtgoretoprism.model.kl.GoalContainer;
import br.unb.cic.goda.rtgoretoprism.model.kl.PlanContainer;
import br.unb.cic.goda.rtgoretoprism.model.kl.RTContainer;
import br.unb.cic.goda.rtgoretoprism.paramformula.SymbolicParamAndGenerator;
import br.unb.cic.goda.rtgoretoprism.paramwrapper.ParamWrapper;

public class PARAMProducer {

	private String sourceFolder;
	private String targetFolder;
	private String toolsFolder;
	private Set<Actor> allActors;
	private Set<Goal> allGoals;
	private AgentDefinition ad;

	private String agentName;
	private List<String> leavesId = new ArrayList<String>();
	private Map<String,String> ctxInformation = new HashMap<String,String>();
	private List<String> varReliabilityInformation = new ArrayList<String>();
	private List<String> varCostInformation = new ArrayList<String>();
	private Map<String,String> reliabilityByNode = new HashMap<String,String>();

	public PARAMProducer(Set<Actor> allActors, Set<Goal> allGoals, String in, String out, String tools) {

		this.sourceFolder = in;
		this.targetFolder = out;
		this.toolsFolder = tools;
		this.allActors = allActors;
		this.allGoals = allGoals;
	}

	public PARAMProducer(AgentDefinition ad, Set<Actor> selectedActors, Set<Goal> selectedGoals,
			String sourceFolder, String targetFolder, String toolsFolder) {
		this.sourceFolder = sourceFolder;
		this.targetFolder = targetFolder;
		this.toolsFolder = toolsFolder;
		this.allActors = selectedActors;
		this.allGoals = selectedGoals;
		this.ad = ad;
		this.agentName = "EvaluationActor";
	}

	public void run() throws Exception {

		for(Actor actor : allActors){

			long startTime = 0;
			
			if (this.ad == null) {
				RTGoreProducer producer = new RTGoreProducer(allActors, allGoals, sourceFolder, targetFolder);
				AgentDefinition ad = producer.run();

				this.ad = ad;
				agentName = ad.getAgentName();
			}

			System.out.println("Generating PARAM formulas for: " + agentName);

			// Compose goal formula
			startTime = new Date().getTime();
			String reliabilityForm = composeNodeForm(ad.rootlist.getFirst(), true);
			String costForm = composeNodeForm(ad.rootlist.getFirst(), false);

			reliabilityForm = cleanNodeForm(reliabilityForm, true);
			costForm = cleanNodeForm(costForm, false);

			//Print formula
			printFormula(reliabilityForm, costForm);
			System.out.println( "PARAM formulas created in " + (new Date().getTime() - startTime) + "ms.");
		}
	}

	private String cleanNodeForm(String nodeForm, boolean reliability) {
		
		if (!reliability) {
			nodeForm = replaceReliabilites(nodeForm);
		}
		
		Map<String,String> mapAux = new HashMap<String,String>();
		for (String ctxKey : this.ctxInformation.keySet()) {
			if (nodeForm.contains("CTX_" + ctxKey)) {
				mapAux.put(ctxKey, this.ctxInformation.get(ctxKey));
			}
		}
		this.ctxInformation = mapAux;
		
		nodeForm = nodeForm.replaceAll("\\s+", "");
		return nodeForm;
	}

	private String replaceReliabilites(String nodeForm) {
		if (nodeForm.contains(" R_")) {
			for (Map.Entry<String, String> entry : this.reliabilityByNode.entrySet()){
				String id = entry.getKey();
				if (nodeForm.contains("R_" + id)) {
					String reliability = entry.getValue();
					nodeForm = nodeForm.replaceAll(" R_" + id + " ", " " + reliability + " ");
				}
			}
		}
		return nodeForm;
	}

	private void printFormula(String reliabilityForm, String costForm) throws CodeGenerationException {

		reliabilityForm = composeFormula(reliabilityForm, true);
		costForm = composeFormula(costForm, false);

		String output = targetFolder + "/";
		
		PrintWriter reliabiltyFormula = ManageWriter.createFile("reliability.out", output);
		PrintWriter costFormula = ManageWriter.createFile("cost.out", output);
		
		ManageWriter.printModel(reliabiltyFormula, reliabilityForm);
		ManageWriter.printModel(costFormula, costForm);
	}

	private String composeFormula(String nodeForm, boolean isReliability) throws CodeGenerationException {

		String body = nodeForm + "\n\n";
		for (String ctxKey : ctxInformation.keySet()) {
			body = body + "//CTX_" + ctxKey + " = " + ctxInformation.get(ctxKey) + "\n";
		}
		for (String var : this.varReliabilityInformation) {
			body = body + var;
		}
		if (!isReliability) {
			for (String var : this.varCostInformation) {
				body = body + var;
			}
		}

		return body;
	}

	//true: compose reliability, false: compose cost
	private String composeNodeForm(RTContainer rootNode, boolean reliability) throws Exception {

		Const decType;
		String dmAnnot;
		String nodeForm;
		String nodeId;
		LinkedList<GoalContainer> decompGoal = new LinkedList<GoalContainer>();
		LinkedList<PlanContainer> decompPlans = new LinkedList<PlanContainer>();

		if(rootNode instanceof GoalContainer) nodeId = rootNode.getClearUId();
		else nodeId = rootNode.getClearElId();

		decompGoal = rootNode.getDecompGoals();
		decompPlans = rootNode.getDecompPlans();
		decType = rootNode.getDecomposition();
		dmAnnot = rootNode.getRtRegex();
		
		if (!decompGoal.isEmpty() || !decompPlans.isEmpty()) {
			setContextList(decompGoal, decompPlans);
		}

		nodeForm = getNodeForm(decType, dmAnnot, nodeId, reliability, rootNode);
		
		/*Run for sub goals*/
		for (GoalContainer subNode : decompGoal) {
			String subNodeId = subNode.getClearUId();
			String subNodeForm = composeNodeForm(subNode, reliability);
			nodeForm = replaceSubForm(nodeForm, subNodeForm, nodeId, subNodeId, reliability);
		}

		/*Run for sub tasks*/
		for (PlanContainer subNode : decompPlans) {
			String subNodeId = subNode.getClearElId();
			String subNodeForm = composeNodeForm(subNode, reliability);
			nodeForm = replaceSubForm(nodeForm, subNodeForm, nodeId, subNodeId, reliability);
		}

		/*If leaf task*/
		if ((decompGoal.size() == 0) && (decompPlans.size() == 0)) {

			this.leavesId.add(nodeId);

			if (reliability) {
				//Create DTMC model (param)
				ParamWriter writer = new ParamWriter(sourceFolder, nodeId);
				String model = writer.writeModel();

				//Call to param (reliability)
				ParamWrapper paramWrapper = new ParamWrapper(toolsFolder, nodeId);
				nodeForm = paramWrapper.getFormula(model);
				nodeForm = nodeForm.replaceFirst("1\\*", "");
				
				this.varReliabilityInformation.add("//R_" + nodeId + " = reliability of node " + nodeId + "\n");
				if (rootNode.isOptional()) {
					nodeForm += "*OPT_" + nodeId;
					this.varReliabilityInformation.add("//OPT_" + nodeId + " = optionality of node " + nodeId + "\n");	
				}
			}
			else {
				//Cost
				nodeForm = getCostFormula(rootNode);
				this.varCostInformation.add("//" + nodeForm + " = cost of node " + nodeId + "\n");
			}
		}
		if (reliability) this.reliabilityByNode.put(nodeId, nodeForm);

		return nodeForm;
	}

	private String getNodeForm(Const decType, String dmAnnot, String nodeId, boolean reliability, RTContainer rootNode) throws Exception {
		
		List<String> childrenNodes = getChildrenId(rootNode);
		StringBuilder formula = new StringBuilder();
		
		//TO-DO: Define the formulae templates outside this method (improve legibility)
		
		if (dmAnnot == null) {
			if (childrenNodes.size() <= 1) return nodeId;
		
			//AND-Decomposition
			if (decType.equals(Const.AND)) {
				if (reliability) {
					formula.append("( ");
					for (String id : childrenNodes) {
						//Children is context-dependent
						if (this.ctxInformation.containsKey(id)) {
							formula.append(" CTX_" + id + " * " + id + " * ");
						}
						//Children is context-free
						else {
							formula.append(id + " * ");
						}
					}
					formula.delete(formula.lastIndexOf("*"), formula.length()-1);
					formula.append(")");
				}
				else {
					
					SymbolicParamAndGenerator param = new SymbolicParamAndGenerator();
					formula = param.getSequentialAndCost((String[]) childrenNodes.toArray(new String[0]), this.ctxInformation);
					formula.append(" * R_" + nodeId + " ");
				}
			}
			//OR-Decomposition
			else {
				if (this.ctxInformation.containsKey(childrenNodes.get(0)) && this.ctxInformation.containsKey(childrenNodes.get(1))) {
					formula.append("( - CTX_" + childrenNodes.get(0) + " * " + childrenNodes.get(0) 
						+ " * CTX_" + childrenNodes.get(1) + " * " + childrenNodes.get(1)
						+ " + CTX_" + childrenNodes.get(0) + " * " + childrenNodes.get(0) 
						+ " + CTX_" + childrenNodes.get(1) + " * " + childrenNodes.get(1) + " ) ");
				}
				else if (this.ctxInformation.containsKey(childrenNodes.get(0))) {
					formula.append("( - CTX_" + childrenNodes.get(0) + " * " + childrenNodes.get(0) 
					+ " * " + childrenNodes.get(1)
					+ " + CTX_" + childrenNodes.get(0) + " * " + childrenNodes.get(0) 
					+ " + " + childrenNodes.get(1) + " ) ");
				}
				else if (this.ctxInformation.containsKey(childrenNodes.get(1))) {
					formula.append("( - " + childrenNodes.get(0)
					+ " * CTX_" + childrenNodes.get(1) + " * " + childrenNodes.get(1)
					+ " + " + childrenNodes.get(0)
					+ " + CTX_" + childrenNodes.get(1) + " * " + childrenNodes.get(1) + " ) ");
				}
				else {
					formula.append("( - " + childrenNodes.get(0) + " * " + childrenNodes.get(1) + " + " + childrenNodes.get(0) + " + " + childrenNodes.get(1) + " ) ");
				}
				
				String removeFromFormula = new String();
				String sumCost = new String();
				if (!reliability) {
					formula = replaceAll(formula, " " + childrenNodes.get(0) + " ", " R_" + childrenNodes.get(0) + " ");
					formula = replaceAll(formula, " " + childrenNodes.get(1) + " ", " R_" + childrenNodes.get(1) + " ");
					
					if (this.ctxInformation.containsKey(childrenNodes.get(0)) && this.ctxInformation.containsKey(childrenNodes.get(1))) {
						removeFromFormula = " - CTX_" + childrenNodes.get(0) + " * " + "R_" + childrenNodes.get(0) + " * CTX_" + childrenNodes.get(1)+ " * " + childrenNodes.get(1);
						sumCost = "CTX_" + childrenNodes.get(0) + " * " + childrenNodes.get(0) + " + CTX_" + childrenNodes.get(1) + " * " + childrenNodes.get(1);
					}
					else if (this.ctxInformation.containsKey(childrenNodes.get(0))) {
						removeFromFormula = " - CTX_" + childrenNodes.get(0) + " * " + "R_" + childrenNodes.get(0) + " * " + childrenNodes.get(1);
						sumCost = "CTX_" + childrenNodes.get(0) + " * " + childrenNodes.get(0) + " + " + childrenNodes.get(1);
					}
					else if (this.ctxInformation.containsKey(childrenNodes.get(1))) {
						removeFromFormula = " - R_" + childrenNodes.get(0) + " * CTX_" + childrenNodes.get(1)+ " * " + childrenNodes.get(1);
						sumCost = childrenNodes.get(0) + " + CTX_" + childrenNodes.get(1) + " * " + childrenNodes.get(1);
					}
					else {
						removeFromFormula = " - R_" + childrenNodes.get(0) + " * " + childrenNodes.get(1);
						sumCost = childrenNodes.get(0) + " + " + childrenNodes.get(1);
					}
				}

				for (int i = 2; i < childrenNodes.size(); i++) {
					if (!reliability) {
						if (this.ctxInformation.containsKey(childrenNodes.get(i))) {
							removeFromFormula += " - " + formula.toString() + " * CTX_" + childrenNodes.get(i) + " * " + childrenNodes.get(i);
							sumCost += " + CTX_" + childrenNodes.get(i) + " * " + childrenNodes.get(i);
						}
						else {
							removeFromFormula += " - " + formula + " * " + childrenNodes.get(i);
							sumCost += " + " + childrenNodes.get(i);
						}
					}
					String currentFormula = formula.toString();
					if (this.ctxInformation.containsKey(childrenNodes.get(i))) {
						formula.insert(0, "( - ");
						formula.append(currentFormula + " * CTX_" + childrenNodes.get(i) 
						+ " * " + childrenNodes.get(i) + " + " + currentFormula + " + CTX_" 
								+ childrenNodes.get(i) + " * " + childrenNodes.get(i) + " ) ");
					}
					else {
						formula.insert(0, "( - ");
						formula.append(currentFormula + " * " + childrenNodes.get(i) + " + " + currentFormula + " + " + childrenNodes.get(i) + " ) ");
					}
					if (!reliability) formula = replaceAll(formula, " " + childrenNodes.get(i) + " ", "R_" + childrenNodes.get(i));
				}
				if (!reliability) {
					String reliabilityFormula = formula.toString();
					formula = new StringBuilder();
					formula.append(" ( ( " + sumCost + " ) * " + reliabilityFormula + " " + removeFromFormula + " )");
				}
			}
			return formula.toString();
		}
		else {
			//DM-annotation (children nodes should always contain context information)
			if (childrenNodes.size() == 1) {
				//Assuring children nodes contain ctx information
				if (!this.ctxInformation.containsKey(childrenNodes.get(0))) {
					throw new Exception();
				}
				
				if (reliability) return " ( CTX_" + childrenNodes.get(0) + " * " + childrenNodes.get(0) + " )";
				formula.append(" ( CTX_" + childrenNodes.get(0) + " * R_" + childrenNodes.get(0) + " * " + childrenNodes.get(0) + " )");
			}
			else {
				//Assuring children nodes contain ctx information
				if (!this.ctxInformation.containsKey(childrenNodes.get(0)) && !this.ctxInformation.containsKey(childrenNodes.get(1))) {
					throw new Exception();
				}
				
				formula.append("( - CTX_" + childrenNodes.get(0) + " * " + childrenNodes.get(0)
				+ " * CTX_" + childrenNodes.get(1) + " * " + childrenNodes.get(1) 
				+ " + CTX_" + childrenNodes.get(0) + " * " + childrenNodes.get(0)
				+ " + CTX_" + childrenNodes.get(1) + " * " + childrenNodes.get(1) + " ) ");

				String removeFromFormula = new String();
				String sumCost = new String();
				if (!reliability) {
					formula = replaceAll(formula, " " + childrenNodes.get(0) + " ", " R_" + childrenNodes.get(0) + " ");
					formula = replaceAll(formula, " " + childrenNodes.get(1) + " ", " R_" + childrenNodes.get(1) + " ");
					removeFromFormula = " - CTX_" + childrenNodes.get(0) + " * R_" + childrenNodes.get(0) + " * CTX_" + childrenNodes.get(1) + " * " + childrenNodes.get(1);
					sumCost = " CTX_" + childrenNodes.get(0) + " * " + childrenNodes.get(0) + " + CTX_" + childrenNodes.get(1) + " * " + childrenNodes.get(1);
				}

				for (int i = 2; i < childrenNodes.size(); i++) {
					if (!this.ctxInformation.containsKey(childrenNodes.get(i))) {
						throw new Exception();
					}
					
					if (!reliability) {
						removeFromFormula += " - " + formula.toString() + " * CTX_" + childrenNodes.get(i) + " * " + childrenNodes.get(i);
						sumCost += " + " + childrenNodes.get(i);
					}
					String currentFormula = formula.toString();
					formula.insert(0, "( - ");
					formula.append(" * CTX_" + childrenNodes.get(i) + " * " + childrenNodes.get(i) 
						+ " + " + currentFormula + " + CTX_" + childrenNodes.get(i) + " * " + childrenNodes.get(i) + " ) ");
					if (!reliability) formula = replaceAll(formula, " " + childrenNodes.get(i) + " ", "R_" + childrenNodes.get(i));
				}
				if (!reliability) {
					String reliabilityFormula = formula.toString();
					formula = new StringBuilder();
					formula.append(" ( ( " + sumCost + " ) * " + reliabilityFormula + " " + removeFromFormula + " )");
				}
			}
		}
		return formula.toString();
	}

	public static StringBuilder replaceAll(StringBuilder sb, String find, String replace){
	    return new StringBuilder(Pattern.compile(find).matcher(sb).replaceAll(replace));
	}

	private void setContextList(LinkedList<GoalContainer> decompGoal, LinkedList<PlanContainer> decompPlans) {
		
		if (!decompGoal.isEmpty()) {
			for (GoalContainer goal : decompGoal) {
				List<String> ctxList = goal.getFulfillmentConditions();
				if (!ctxList.isEmpty()) {
					List<String> cleanCtx = clearCtxList(ctxList);
					String ctxConcat = concatCtxInformation(cleanCtx);
					this.ctxInformation.put(goal.getClearUId(), ctxConcat);
				}
			}
		}
		else if (!decompPlans.isEmpty()) {
			for (PlanContainer plan : decompPlans) {
				List<String> ctxList = plan.getFulfillmentConditions();
				if (!ctxList.isEmpty()) {
					List<String> cleanCtx = clearCtxList(ctxList);
					String ctxConcat = concatCtxInformation(cleanCtx);
					this.ctxInformation.put(plan.getClearElId(), ctxConcat);
				}
			}
		}
	}

	private String concatCtxInformation(List<String> cleanCtx) {
		String ctxConcat = new String();
		for (String ctx : cleanCtx) {
			if (ctxConcat.length() == 0) {
				ctxConcat = "(" + ctx + ")";
			}
			else {
				ctxConcat = ctxConcat.concat(" & (" + ctx + ")");
			}
		}
		return ctxConcat;
	}

	private String getCostFormula(RTContainer rootNode) throws IOException {
		PlanContainer plan = (PlanContainer) rootNode;
	
		if (plan.getCostRegex() != null) {
			Object [] res = CostParser.parseRegex(plan.getCostRegex());
			return (String) res[2];
		}
		
		return "W_"+rootNode.getClearElId();
	}

	private List<String> clearCtxList(List<String> ctxAnnot) {

		List<String> clearCtx = new ArrayList<String>();
		for (String ctx : ctxAnnot) {
			String[] aux;
			if (ctx.contains("assertion condition")) {
				aux = ctx.split("^assertion condition\\s*");
			}
			else {
				aux = ctx.split("^assertion trigger\\s*");
			}
			clearCtx.add(aux[1]);
		}

		return clearCtx;
	}

	private String replaceSubForm(String nodeForm, String subNodeForm, String nodeId, String subNodeId, boolean reliability) {

		if (nodeForm.equals(nodeId)) {
			nodeForm = subNodeForm;
		}
		else {
			subNodeId = restricToString(subNodeId);
			subNodeForm = restricToString(subNodeForm);
			nodeForm = nodeForm.replaceAll(subNodeId, subNodeForm);
		}
		
		if (subNodeForm.contains("CTX") && nodeForm.contains("XOR")) {
			for (Map.Entry<String, String> entry : ctxInformation.entrySet())
			{
				if (entry.getKey().contains(subNodeId.trim())) {
					nodeForm = nodeForm.replaceAll("XOR_" + subNodeId.trim(), entry.getKey());
					return nodeForm;
				}
			}
		}
		return nodeForm;
	}

	private String restricToString(String subNodeString) {
		return " " + subNodeString + " ";
	}

	private List<String> getChildrenId(RTContainer rootNode) {
		List<String> ids = new ArrayList<String>();
		LinkedList<RTContainer> children = rootNode.getDecompElements();
		
		if (children.isEmpty()) return ids;
		
		for (RTContainer child : children) {
			if(child instanceof GoalContainer) ids.add(child.getClearUId());
			else ids.add(child.getClearElId());
		}
		
		if (ids.size() == 1) return ids;
		
		return ids;
	}
}