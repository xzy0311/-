package refuture.refactoring;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import refuture.astvisitor.AllVisiter;
import refuture.sootUtil.Cancel;
import refuture.sootUtil.CastAnalysis;
import refuture.sootUtil.CollectionEntrypoint;
import refuture.sootUtil.ExecutorSubclass;
import refuture.sootUtil.Instanceof;
import refuture.sootUtil.SootConfig;


/**
 * 此类是重构的动作类 重构的预览可能是Wizard的功能吧。.
 */
public class RefutureRefactoring extends Refactoring {
	
	/** The all changes. */
	// 所有的重构变化
	List<Change> allChanges;
	
	/** The all java files. */
	// 项目所有的java文件,将IJavaElement改成了IcompilationUnit,这是由它的初始化过程决定的。
	List<ICompilationUnit> allJavaFiles;
	
	Date startTime;
	
	int refactorPattern;

	boolean disableCancelPattern;
	public static int time = 0;
	/**
	 * Instantiates a new future task refactoring.
	 *
	 * @param selectProject the select project
	 */
	public RefutureRefactoring(IJavaProject selectProject) {		
		allJavaFiles = AnalysisUtils.collectFromSelect(selectProject);
		allChanges = new ArrayList<Change>();
		InitAllStaticfield.init();//初始化所有的静态字段。
		this.refactorPattern = 1;
		this.disableCancelPattern = false;
		startTime =new Date();
		System.out.println("The current start time is "+ startTime);
	}

	public boolean setRefactorPattern(int pattern) {
		this.refactorPattern = pattern;
		return true;
	}
	
	public boolean setDisableCancelPattern(boolean pattern) {
		this.disableCancelPattern = pattern;
		return true;
	}

	@Override
	public String getName() {
		return "reFutureMain";
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		if (allJavaFiles.isEmpty()) {
			return RefactoringStatus.createFatalErrorStatus("Find zero java file");
		}
		ConcurrentLinkedQueue<CompilationUnit> allAST = new ConcurrentLinkedQueue<>();
		allJavaFiles.parallelStream().forEach(cu -> {
		    ASTParser parser = ASTParser.newParser(AST.JLS11);
		    parser.setResolveBindings(true);
		    parser.setStatementsRecovery(true);
		    parser.setBindingsRecovery(true);
		    parser.setSource(cu);
		    CompilationUnit astUnit = (CompilationUnit) parser.createAST(null);
		    allAST.add(astUnit);
		});
		AnalysisUtils.allAST = new ArrayList<>(allAST);
		AllVisiter av = AllVisiter.getInstance();
		for(CompilationUnit cu:allAST) {
			cu.accept(av);
		}
		Date initConfigTime = new Date();
		System.out.println("AST初始化完毕的时间"+"The current time is "+ initConfigTime+"已花费:"+((initConfigTime.getTime()-startTime.getTime())/1000)+"s");
		return RefactoringStatus.createInfoStatus("Ininal condition has been checked");
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		if(refactorPattern ==1) {
			System.out.println("Future重构模式");
			System.out.println("hello xzy ,this is "+ time++ +" times run this model.");
			if(time == 1) {
				SootConfig.setupSoot();//配置初始化soot,用来分析类层次结构
				Date finishSootConfigTime = new Date();
				System.out.println("soot配置完毕的时间"+"The current time is "+ finishSootConfigTime+"已花费:"+((finishSootConfigTime.getTime()-startTime.getTime())/1000)+"s");
			}
			ExecutorSubclass.futureAnalysis();
			ExecutorSubclass.taskTypeAnalysis();
			ExecutorSubclass.executorSubClassAnalysis();
			ExecutorSubclass.wrapperClassAnalysis();
	        ExecutorSubclass.threadPoolExecutorSubClassAnalysis();
	        Instanceof.init();
	        CastAnalysis.init();
	        if(!this.disableCancelPattern) {
		        Cancel.initCancel(allJavaFiles);
	        }
			CollectionEntrypoint.entryPointInit(allJavaFiles);
			Future2Completable.refactor();
		}else if(refactorPattern == 2) {
			FindThread.find(allJavaFiles);
		}
		Date endTime = new Date();
		System.out.println("The current ent time is "+ endTime +"已花费:" + ((endTime.getTime()-startTime.getTime())/1000)+"s");
		return null;
	}


	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if(refactorPattern ==1) {
			allChanges.addAll(Future2Completable.getallChanges());
		}else if(refactorPattern == 2) {
//			allChanges.addAll(ForTask.getallChanges());
		}
		Change[] changes = new Change[allChanges.size()];
		System.arraycopy(allChanges.toArray(), 0, changes, 0, allChanges.size());
		CompositeChange change = new CompositeChange("refuture 待更改", changes);
		return change;
		
	}





}
