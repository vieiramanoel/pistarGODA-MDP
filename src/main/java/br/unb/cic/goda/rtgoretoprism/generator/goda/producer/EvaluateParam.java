package br.unb.cic.goda.rtgoretoprism.generator.goda.producer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import br.unb.cic.goda.model.Actor;
import br.unb.cic.goda.model.Goal;
import br.unb.cic.goda.rtgoretoprism.action.RunParamAction;
import br.unb.cic.integration.Controller;
import br.unb.cic.pistar.model.PistarActor;
import br.unb.cic.pistar.model.PistarLink;
import br.unb.cic.pistar.model.PistarModel;
import br.unb.cic.pistar.model.PistarNode;

public class EvaluateParam {

	public static void main(String[] args) throws IOException {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Number of leaf-tasks: ");
		int num = scanner.nextInt();
		
		System.out.print("Construction: ");
		String annot = scanner.next();
		
		PistarModel model = generateDefaultModel(num, annot);
		Set<Actor> selectedActors = new HashSet<>();
        Set<Goal> selectedGoals = new HashSet<>();
        Controller.transformToTao4meEntities(model, selectedActors, selectedGoals);
        try {
			cleanDTMCFolder();
			new RunParamAction(selectedActors, selectedGoals).run();
			System.out.println("Clear dtmc folder? ");
			String ans = scanner.next();
			if (ans.equals("y")) cleanDTMCFolder();
		} catch (IOException e) {
			e.printStackTrace();
		}     
	}

	private static PistarModel generateDefaultModel(int num, String annot) {
		List<PistarNode> nodes = new ArrayList<PistarNode>();
		
		//Creating goal
		PistarNode goal = new PistarNode();
		Map<String,String> customProperty = new HashMap<String,String>();
		customProperty.put("selected", "true");
		goal.setCustomProperties(customProperty);
		goal.setId("0");
		goal.setText("G0: goal");
		goal.setType("istar.Goal");
		nodes.add(goal);
		
		//Creating means-end task
		PistarNode task = new PistarNode();
		task.setId("1");
		task.setText("T1: task");
		if (annot.equals("DM")) task.setText(getDM(num));
		task.setType("istar.Task");
		nodes.add(task);
		
		//Creating means-end refinement
		PistarLink link = new PistarLink();
		link.setSource("1");
		link.setTarget("0");
		link.setType("istar.AndRefinementLink");
		List<PistarLink> links = new ArrayList<PistarLink>();
		links.add(link);
		
		for (int i=0; i<num; i++) {
			PistarNode t = new PistarNode();
			t.setId(Integer.toString(i+2));
			t.setText("T1." + Integer.toString(i) + ": task");
			t.setType("istar.Task");
			if (annot.equals("DM")) {
				Map<String,String> property = new HashMap<String,String>();
				property.put("creationProperty", "assertion trigger ctx=" + Integer.toString(i));
				t.setCustomProperties(property);
			}
			nodes.add(t);
			
			PistarLink l = new PistarLink();
			l.setSource(t.getId());
			l.setTarget("1");
			if (annot.equals("AND")) l.setType("istar.AndRefinementLink");
			else l.setType("istar.OrRefinementLink");
			links.add(l);
		}
		
		//Creating actor
		PistarActor actor = new PistarActor();
		actor.setNodes(nodes);
		actor.setText("Test");
		actor.setType("istar.Actor");
		List<PistarActor> actors = new ArrayList<PistarActor>();
		actors.add(actor);
		
		//Composing model
		PistarModel model = new PistarModel();
		model.setActors(actors);
		model.setLinks(links);

		return model;
	}
	
    private static String getDM(int num) {
		StringBuilder dm = new StringBuilder();
		dm.append("T1: text [DM(T1.");
		for (int i=0; i<num; i++){
			dm.append(Integer.toString(i));
			if (i != num-1) dm.append(",T1.");
			else dm.append(")]");
		}
		return dm.toString();
	}

	private static void cleanDTMCFolder() throws IOException {
        Files.walk(Paths.get("dtmc"), FileVisitOption.FOLLOW_LINKS)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(f -> {
                    if (f.isFile()) {
                        f.delete();
                    }
                });
    }
}
