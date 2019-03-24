package br.unb.cic.goda.rtgoretoprism.generator.goda.producer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.unb.cic.goda.model.Actor;
import br.unb.cic.goda.model.Goal;
import br.unb.cic.goda.rtgoretoprism.action.PRISMCodeGenerationAction;
import br.unb.cic.goda.rtgoretoprism.paramwrapper.ParamWrapper;
import br.unb.cic.integration.Controller;
import br.unb.cic.pistar.model.PistarActor;
import br.unb.cic.pistar.model.PistarLink;
import br.unb.cic.pistar.model.PistarModel;
import br.unb.cic.pistar.model.PistarNode;


public class EvaluatePrism {
	private static final Logger LOGGER = Logger.getLogger(ParamWrapper.class.getName());

	public static void main(String[] args) throws IOException {
		
		//Generating goal model
		Scanner scanner = new Scanner(System.in);
		System.out.print("Number of leaf-tasks: ");
		int num = scanner.nextInt();
		
		System.out.print("Decomposition/Annotation: ");
		String annot = scanner.next();
		
		PistarModel model = generateDefaultModel(num, annot);
		Set<Actor> selectedActors = new HashSet<>();
        Set<Goal> selectedGoals = new HashSet<>();
        Controller.transformToTao4meEntities(model, selectedActors, selectedGoals);
        
        //Evaluating PRISM verification time
        try {
        	cleanDTMCFolder();
        	new PRISMCodeGenerationAction(selectedActors, selectedGoals).run();
        
        	File resultsFile = File.createTempFile("result", null);
        	String resultsPath = resultsFile.getAbsolutePath();
        	String prismPath = "tools/prism";
        	String modelPath = "dtmc/Test.nm";
//        	String propertyPath = "dtmc/ReachabilityMin.pctl";
        	String propertyPath = "dtmc/ReachabilityMax.pctl";
//       	String propertyPath = "dtmc/CostMin.pctl";
//        	String propertyPath = "dtmc/CostMax.pctl";
        	String commandLine = prismPath + " "
                    + modelPath + " "
                    + propertyPath + " ";
                    //+ "-exportresults " + resultsPath;
                    
        	evaluatePrism(commandLine, resultsPath);

			System.out.println("Clear dtmc folder? ");
			String ans = scanner.next();
			if (ans.equals("y") || (ans.equals("Y"))) cleanDTMCFolder();
		} catch (IOException e) {
			e.printStackTrace();
		}     
	}

	private static void evaluatePrism(String commandLine, String resultsPath) throws IOException {
    	int size=30;
    	double[] totalTime = new double[size];
    	
    	for (int i=0; i<size; i++) {
	       	LOGGER.fine(commandLine);
	        Process program = Runtime.getRuntime().exec(commandLine);
	        
	        BufferedReader stdInput = new BufferedReader(new 
	        	     InputStreamReader(program.getInputStream()));
	        	
	        String s = null;
	        while ((s = stdInput.readLine()) != null) {
	        	//System.out.println(s);
	        	if (s.contains("Time for model checking: ")) {
	        		System.out.println(s);
	        		String[] times = s.split(" ");
	        		String time = times[4];
	        		totalTime[i] = Double.parseDouble(time);
	        	}
	        }
	        
	        int exitCode = 0;
	        try {
	            exitCode = program.waitFor();
	        } catch (InterruptedException e) {
	            LOGGER.severe("Exit code: " + exitCode);
	            LOGGER.log(Level.SEVERE, e.toString(), e);
	        }
	        //List<String> lines = Files.readAllLines(Paths.get(resultsPath), Charset.forName("UTF-8"));
    	}

        //Mean time
    	DecimalFormat four = new DecimalFormat("#0.0000");
        double mean = 0;
        double sum = 0.0;
        for(double a : totalTime)
        	sum += a;
        mean = sum/size;
        //Standard deviation
        double temp = 0;
        for(double a :totalTime)
        	temp += (a-mean)*(a-mean);
        double variance = temp/(size-1);
        double sd = Math.sqrt(variance);
        System.out.println("\n\nAverage generation time: " + four.format(mean) + "ms. SD: " + four.format(sd) + "ms.");
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
