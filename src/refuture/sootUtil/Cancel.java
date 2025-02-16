package refuture.sootUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.MethodInvocation;

import refuture.astvisitor.AllVisiter;
import refuture.refactoring.AnalysisUtils;
import refuture.refactoring.RefutureException;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.ValueBox;
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocalBox;

// 这个类就像ExecutorSubclass一样，在最开始初始化一次。找到所有的调用future.cancel()的local。
// 然后通过一个方法，能够得到当前输入的invocstmt 左值的local是否可能指向同一个对象，就行了。
public class Cancel {
	private static List <Local> invocCancelLocals;
	
	public static boolean initStaticField() {
		invocCancelLocals = new ArrayList<Local>();
		return true;
	}
	
	public static void initCancel(List<ICompilationUnit> allJavaFiles) {
		Set<String> allFutureAndsubName = ExecutorSubclass.getStringInSootClassSet(ExecutorSubclass.allFutureSubClasses);
		List<MethodInvocation> invocationNodes = AllVisiter.getInstance().getMInvocResult();
		for(MethodInvocation invocationNode:invocationNodes) {
			if(!invocationNode.getName().toString().equals("cancel") || !isTrue(invocationNode) || invocationNode.getExpression() == null) {
				continue;
			}
			//到这里都是cancel(true)了。根据astBinding 得到接收器类型。
			String typeName = AnalysisUtils.getTypeName4Exp(invocationNode.getExpression());
			if(typeName == null) { throw new RefutureException(invocationNode);}
			if(allFutureAndsubName.contains(typeName)) {
				//在这里确定了调用了future.cancel()。接下来开始将exp对应的sootlocal存入静态字段。
				Stmt invocStmt = AdaptAst.getJimpleStmt(invocationNode);
				if(invocStmt == null) {
					continue; 
				}
				//得到定义的unit中的Local，并加入待分析里面。
				List<ValueBox> lvbs = invocStmt.getUseBoxes();
				for(ValueBox vb : lvbs) {
					if(vb instanceof JimpleLocalBox) {
						JimpleLocalBox jlb = (JimpleLocalBox) vb;
						Local futureLocal = (Local)jlb.getValue();
						invocCancelLocals.add(futureLocal);
						//将所在的方法加入entryPoints。
					}
				}
			}
			else if(typeName == "java.lang.Object") {
				System.out.println("||可能需要精确度更高的分析取代ASTBinding||");
			}
		}
		System.out.println("本程序中共包含调用cancel(true)的Future实例:"+invocCancelLocals.size()+"处");
	}
	
	public static boolean futureUseCancelTure(MethodInvocation invocationNode, Stmt invocStmt) {
		if(invocCancelLocals.isEmpty()) {
			//该程序没有调用cancel(true)。
			AnalysisUtils.debugPrint("[Cancel.futureUseCancelTure]没有调用cancel(true)");
			return false;
		}
		List<ValueBox> defBox = invocStmt.getDefBoxes();
		if(!defBox.isEmpty()) {
			Local futureLocal = (Local) defBox.get(0).getValue();
			PointsToAnalysis pa = Scene.v().getPointsToAnalysis();
			PointsToSet futureLocalSet = pa.reachingObjects(futureLocal);
			if(futureLocalSet.isEmpty()) {
				AnalysisUtils.debugPrint("分析是否和调用cancel的变量为别名时，无法得到分配点,保守决定排除");
				return true;
			}
			for(Local cancelLocal: invocCancelLocals) {
				PointsToSet cancelLocalSet = pa.reachingObjects(cancelLocal);
				if(futureLocalSet.hasNonEmptyIntersection(cancelLocalSet)) {
					AnalysisUtils.debugPrint("[Cancel.futureUseCancelTure]根据别名分析，该future可能后继调用cancel(true),排除");
					return true;
				}
			}
		}
		return false;
	}
	private static boolean isTrue(MethodInvocation cancelInvoc) {
		List argus = cancelInvoc.arguments();
		if(argus.size() == 1 && argus.get(0) instanceof BooleanLiteral) {
			BooleanLiteral boolExp = (BooleanLiteral) argus.get(0);
			if(boolExp.booleanValue() == true) {
				return true;
			}
		}
		
		return false;
	}
	
}
