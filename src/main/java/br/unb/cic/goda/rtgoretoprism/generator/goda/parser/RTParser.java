package br.unb.cic.goda.rtgoretoprism.generator.goda.parser;

import br.unb.cic.RTRegexBaseVisitor;
import br.unb.cic.RTRegexLexer;
import br.unb.cic.RTRegexParser;
import br.unb.cic.RTRegexParser.*;
import br.unb.cic.goda.rtgoretoprism.model.kl.Const;
import br.unb.cic.goda.rtgoretoprism.generator.goda.parser.CustomRTRegexVisitor;
import br.unb.cic.goda.rtgoretoprism.generator.goda.parser.ThrowingErrorListener;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RTParser {

	public static Object[] parseRegex(String uid, String regex, Const decType, boolean param) throws IOException{
		//Reading the DSL script
		InputStream is = new ByteArrayInputStream(regex.getBytes("UTF-8"));

		//Loading the DSL script into the ANTLR stream.
		CharStream cs = new ANTLRInputStream(is);

		//Passing the input to the lexer to create tokens
		RTRegexLexer lexer = new RTRegexLexer(cs);
		lexer.removeErrorListeners();
		lexer.addErrorListener(ThrowingErrorListener.INSTANCE);

		CommonTokenStream tokens = new CommonTokenStream(lexer);

		//Passing the tokens to the parser to create the parse trea. 
		RTRegexParser parser = new RTRegexParser(tokens);
		parser.removeErrorListeners();
		parser.addErrorListener(ThrowingErrorListener.INSTANCE);

		//Semantic model to be populated
		//Graph g = new Graph();

		//Adding the listener to facilitate walking through parse tree. 
		//parser.addParseListener(new MyRTRegexBaseListener());

		//invoking the parser. 
		//parser.prog();

		//Graph.printGraph(g);

		//ParseTreeWalker walker = new ParseTreeWalker();
		//walker.walk(new MyRTRegexBaseListener(), parser.prog());

		ParseTree tree = parser.rt();
		CustomRTRegexVisitor rtRegexVisitor = new CustomRTRegexVisitor(uid, decType, param);
		rtRegexVisitor.visit(tree);

		return new Object [] 	{rtRegexVisitor.reliabilityFormula,
				rtRegexVisitor.costFormula,
				rtRegexVisitor.decisionMemory,
				rtRegexVisitor.retryMemory,
				rtRegexVisitor.tryMemory,
				rtRegexVisitor.timeMemory};
    }
}

class CustomRTRegexVisitor extends  RTRegexBaseVisitor<String> {

	final String uid;
	final Const decType;
	final boolean param;
	String reliabilityFormula = new String();
	String costFormula = new String();	
	List<String> decisionMemory = new ArrayList<String>();
	Map<String, Object[]> retryMemory = new HashMap<String, Object[]>();
	Map<String, String[]> tryMemory = new HashMap<String, String[]>();
	Map<String, Boolean[]> timeMemory = new HashMap<String, Boolean[]>();
	
	public CustomRTRegexVisitor(String uid, Const decType, boolean param) {
		this.decType = decType;
		this.param = param;

		if (uid.contains("_")) {
			this.uid = uid.substring(0, uid.indexOf('_'));
		}
		else {
			this.uid = uid;
		}
	}

	@Override
	public String visitPrintExpr(PrintExprContext ctx) {
		visit(ctx.expr());		
		return "Goals sorted";
	}
	
	@Override
	public String visitGId(GIdContext ctx) {
		String gid = ctx.t.getText() + ctx.id().getText();
		if(ctx.t.getType() == RTRegexParser.TASK) {
			gid = uid + '_' + gid;
		}
		if (!timeMemory.containsKey(gid)) {
            timeMemory.put(gid, new Boolean[]{false, false});
        }
		return gid;
	}
	
	@Override
	public String visitGDM(GDMContext ctx) {
		String gidAo = super.visit(ctx.expr(0));
		String gidBo = super.visit(ctx.expr(1));
		
		String [] gidAs = gidAo.split("-");
		String [] gidBs = gidBo.split("-");		
		String [] gids = new String[gidAs.length + gidBs.length];
		int i = 0;
		for(String gidB : gidBs){
			for(String gidA : gidAs){
				if(!decisionMemory.contains(gidA))
					decisionMemory.add(gidA);
				gids[i] = gidA.replaceAll("\\.", "_");
				i++;
			}
			if(!decisionMemory.contains(gidB))
				decisionMemory.add(gidB);
			gids[i] = gidB.replaceAll("\\.", "_");
			i++;
		}
		return gidAo + '-' + gidBo;
	}

	@Override
	public String visitGRetry(GRetryContext ctx) {
		String gid = visit(ctx.expr());
		String k = ctx.FLOAT().getText();
		
		retryMemory.put(gid, new Object[]{Const.RTRY, Integer.parseInt(ctx.FLOAT().getText())});
        k = String.valueOf(Integer.valueOf(k) + 1);
		
		return gid;
	}

	@Override
	public String visitGTry(GTryContext ctx) {
		String gidT = visit(ctx.expr(0));
		String gidS = visit(ctx.expr(1));
		String gidF = visit(ctx.expr(2));
		
		Boolean[] pathTimeS, pathTimeF;
        if (gidS != null) {
            pathTimeS = timeMemory.get(gidS);
            pathTimeS[1] = pathTimeS[1] = true;
        }
        
        if (gidF != null) {
            pathTimeF = timeMemory.get(gidF);
            pathTimeF[1] = pathTimeF[1] = true;
        }
        tryMemory.put(gidT, new String[]{gidS, gidF});
        return gidT;
	}

	@Override
	public String visitGSkip(GSkipContext ctx) {
		return null;
	}

	@Override
	public String visitGTime(GTimeContext ctx) {
		String gidAo = visit(ctx.expr(0));
        String gidBo = visit(ctx.expr(1));
        
        //String [] gidAs = gidAo.split("-");
        String[] gidBs = gidBo.split("-");
        for (String gidB : gidBs) {
            Boolean[] pathTimeB = timeMemory.get(gidB);
            if (ctx.op.getType() == RTRegexParser.INT) {
                pathTimeB[0] = true;
            } else if (ctx.op.getType() == RTRegexParser.SEQ) {
                pathTimeB[1] = true;
            }
        }
        return gidAo + '-' + gidBo;
	}
	
}
