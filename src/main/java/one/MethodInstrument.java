package one;

import soot.*;
import soot.jimple.*;
import soot.options.Options;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static one.Constants.*;

public class MethodInstrument {

    private static final Set<String> TARGET_PACKAGE_METHOD_NAME = new HashSet<>();
    public static void initsoot() {
        //G.reset();
        Options.v().set_allow_phantom_refs(true);//设置允许伪类（Phantom class），指的是soot为那些在其classpath找不到的类建立的模型
        Options.v().set_prepend_classpath(true);//prepend the VM's classpath to Soot's own classpath
        Options.v().set_output_format(Options.output_format_dex);//设置soot的输出格式
        Options.v().set_android_jars(androidJar);//设置android jar包路径
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_process_dir(Collections.singletonList(APK_APK));
        Options.v().set_force_overwrite(true);
        Options.v().set_whole_program(true);
        Options.v().setPhaseOption("cg", "verbose:true");
        Options.v().set_process_multiple_dex(true);
        //Options.v().set_soot_classpath("");
        Scene.v().loadNecessaryClasses();
        //call soot.Main
        //soot.Main.main(args);
    }

    public static void instrument() {
        initsoot();
        PackManager.v().getPack("jtp").add(
                new Transform("jtp.MyTransform", new MyTransform()));//添加自己的BodyTransformer
        PackManager.v().runPacks();
        PackManager.v().writeOutput();
        saveAllMethodNames();
        apkSign();
        //moveAPK2Done();
    }

    public static String getTargetPackageName(){
        try{
            String c = String.format("%s dump xmltree %s AndroidManifest.xml", AAPT, APK_APK);
            System.out.println(c);
            Process process  = Runtime.getRuntime().exec(c);
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            // 读取输出结果
            String line;
            String ans = null;
            while ((line = reader.readLine()) != null) {
                if(line.contains("package=")){
                    ans = line.substring(line.indexOf("package=") + "package=".length() + 1);
                    ans = ans.substring(0, ans.indexOf("\""));
                    break;
                }
            }

            if(ans==null){
                throw new RuntimeException("No target package name!");
            }
            return ans;
        }catch (Exception e){
            e.printStackTrace();
        }

        throw new RuntimeException("No target package name!");

    }

    private static void apkSign(){
        try{
            String cmd = String.format("%s sign --ks %s --ks-key-alias %s --ks-pass pass:%s --key-pass pass:%s %s"
                    , APKSIGNER, KEYSTORE, KA, KEYSTORE_PASSWORD, KEYSTORE_PASSWORD, APK_SOOT_OUTPUT);
            Runtime.getRuntime().exec(cmd);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void moveAPK2Done(){
        Path src = Paths.get(APK_APK);
        Path dst = Paths.get(APK_DONE);

        try{
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("APK ===>>> done");
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    static class MyTransform extends BodyTransformer {
        @Override
        protected void internalTransform(Body body, String arg1,
                                         Map<String, String> arg2) {
            Iterator<Unit> unitsIterator = body.getUnits().snapshotIterator();//获取Body里所有的units,一个Body对应Java里一个方法的方法体，Unit代表里面的语句
            while (unitsIterator.hasNext()) {
                Stmt insertionPlace = findInsertionStatement(body, unitsIterator);

                if (insertionPlace == null) {
                    break;
                }

                List<Unit> testList = insertClassMethodInfo(body);
                body.getUnits().insertBefore(testList, insertionPlace);
            }
        }

        private Stmt findInsertionStatement(Body body, Iterator<Unit> unitsIterator) {
            boolean thisObjectAssign = false;
            while (unitsIterator.hasNext()) {
                Stmt stmt = (Stmt) unitsIterator.next();
                if (!NOT_TARGET_PACKAGE.test(body.getMethod().toString())) {
                    TARGET_PACKAGE_METHOD_NAME.add(body.getMethod().getDeclaringClass()+"."+body.getMethod().getName());
                    if (stmt.toString().contains("@this")) {
                        thisObjectAssign = true;
                    } else if (thisObjectAssign) {
                        if (!stmt.toString().contains("@parameter")) {
                            return stmt;
                        }

                    }
                }
            }

            return null;
        }

        private List<Unit> insertClassMethodInfo(Body body) {
            List<Unit> insertedStmts = new ArrayList<>();

            /*
            * Thread t;
                t = Thread.currentThread();
            * */

            Local threadInstance = Jimple.v().newLocal("thread",
                    RefType.v("java.lang.Thread"));
            body.getLocals().add(threadInstance);

            SootClass threadClazz = Scene.v().getSootClass("java.lang.Thread");
            SootMethod currentThreadMethod = threadClazz.getMethod(
                    "java.lang.Thread currentThread()");
            Stmt assignThreadInstance = Jimple.v().newAssignStmt(threadInstance,
                    Jimple.v().newStaticInvokeExpr(currentThreadMethod.makeRef()));
            insertedStmts.add(assignThreadInstance);

            /*
            * StackTraceElement[] stackTraceArray;
                stackTraceArray = t.getStackTrace();
            * */

            Local stackTraceArray = Jimple.v().newLocal("stackTraceArray",
                    ArrayType.v(RefType.v("java.lang.StackTraceElement"),
                            1));
            body.getLocals().add(stackTraceArray);
            SootClass stackTraceElementClazz = Scene.v()
                    .forceResolve("java.lang.StackTraceElement", SootClass.SIGNATURES);
            SootMethod getStackTraceMethod = threadClazz
                    .getMethod("java.lang.StackTraceElement[] getStackTrace()");
            Stmt assignStackTrace = Jimple.v().newAssignStmt(stackTraceArray,
                    Jimple.v().newVirtualInvokeExpr(threadInstance,
                            getStackTraceMethod.makeRef()));
            insertedStmts.add(assignStackTrace);

            /*
            * String methodName;
              StackTraceElement targetStackTraceElement = stackTrace[2];
              methodName = targetStackTraceElement.getMethodName();
            * */

            Local methodName = Jimple.v().newLocal("methodName",
                    RefType.v("java.lang.String"));
            body.getLocals().add(methodName);
            Local targetStackTraceElement = Jimple.v().newLocal("targetStackTraceElement",
                    RefType.v("java.lang.StackTraceElement"));
            body.getLocals().add(targetStackTraceElement);
            Stmt assignTargetStackTraceElement = Jimple.v().newAssignStmt(targetStackTraceElement
                    , Jimple.v().newArrayRef(stackTraceArray, IntConstant.v(2)));
            insertedStmts.add(assignTargetStackTraceElement);
            SootMethod getMethodNameMethod = stackTraceElementClazz
                    .getMethod("java.lang.String getMethodName()");
            Stmt assignMethodName = Jimple.v().newAssignStmt(methodName
                    , Jimple.v().newVirtualInvokeExpr(targetStackTraceElement,
                            getMethodNameMethod.makeRef()));
            insertedStmts.add(assignMethodName);

            /*
            * String className;
              className = targetStackTraceElement.getClassName();
            *
            * */

            Local className = Jimple.v().newLocal("className",
                    RefType.v("java.lang.String"));
            body.getLocals().add(className);
            SootMethod getClassNameMethod = stackTraceElementClazz
                    .getMethod("java.lang.String getClassName()");
            Stmt assignClassName = Jimple.v().newAssignStmt(className,
                    Jimple.v().newVirtualInvokeExpr(targetStackTraceElement,
                            getClassNameMethod.makeRef()));
            insertedStmts.add(assignClassName);

            /*
            * String result;
                result = className.concat(" ");
                result = result.concat(methodName);
            * */
            Local result = Jimple.v().newLocal("result",
                    RefType.v("java.lang.String"));
            body.getLocals().add(result);
            SootMethod concatMethod = Scene.v()
                    .getSootClass("java.lang.String")
                    .getMethod("java.lang.String concat(java.lang.String)");
            Stmt assignResult1 = Jimple.v().newAssignStmt(result,
                    Jimple.v().newVirtualInvokeExpr(className,
                            concatMethod.makeRef(),
                            StringConstant.v(" ")));
            insertedStmts.add(assignResult1);
            Stmt assignResult2 = Jimple.v().newAssignStmt(result,
                    Jimple.v().newVirtualInvokeExpr(result, concatMethod.makeRef(), methodName));
            insertedStmts.add(assignResult2);

            /*
            * String separator;
                separator = "\n";
                result = result.concat(separator);
            * */

            Local separator = Jimple.v().newLocal("separator",
                    RefType.v("java.lang.String"));
            body.getLocals().add(separator);
            SootMethod lineSeparatorMethod = Scene.v()
                    .getSootClass("java.lang.System")
                    .getMethod("java.lang.String lineSeparator()");
            Stmt assignSeparator = Jimple.v().newAssignStmt(separator,
                    Jimple.v().newStaticInvokeExpr(
                            lineSeparatorMethod.makeRef()));
            Stmt assignResult3 = Jimple.v().newAssignStmt(result,
                    Jimple.v().newVirtualInvokeExpr(result, concatMethod.makeRef(),
                            separator));
            insertedStmts.add(assignSeparator);
            insertedStmts.add(assignResult3);

            /*
            Log.i("The method", result);
            *
            * */

            SootClass logClass = Scene.v().getSootClass("android.util.Log");//获取android.util.Log类
            SootMethod logMethod = logClass.getMethod("int d(java.lang.String,java.lang.String)");
            StaticInvokeExpr staticInvokeExpr = Jimple.v()
                    .newStaticInvokeExpr(logMethod.makeRef(),
                            StringConstant.v("The method"), result);
            Stmt invokeStmt = Jimple.v().newInvokeStmt(staticInvokeExpr);
            insertedStmts.add(invokeStmt);

            /*
            String allStackTraceElementsString;
            allStackTraceElementsString = Arrays.toString(stackTrace);
            * Log.i("All stack content", allStackTraceElementsString);
            * */

            SootClass arraysClass = Scene.v().getSootClass("java.util.Arrays");
            Local allStackTraceElementsString = Jimple.v().newLocal("allStackTraceElementsString",
                    Scene.v().getSootClass("java.lang.String").getType());
            body.getLocals().add(allStackTraceElementsString);
            SootMethod arraysToStringMethod = arraysClass
                    .getMethod("java.lang.String toString(java.lang.Object[])");
            Local tempCastLocal = Jimple.v().newLocal("tempCastObjectArray",
                    ArrayType.v(Scene.v().getSootClass("java.lang.Object").getType(), 1));
            body.getLocals().add(tempCastLocal);
            Stmt assignCast = Jimple.v().newAssignStmt(tempCastLocal, Jimple.v().newCastExpr(stackTraceArray,
                    ArrayType.v(Scene.v().getSootClass("java.lang.Object").getType(), 1)));
            insertedStmts.add(assignCast);

            StaticInvokeExpr arraysToStringInvokeExpr2 = Jimple.v()
                    .newStaticInvokeExpr(arraysToStringMethod.makeRef()
                    , tempCastLocal);
            Stmt assignArraysString = Jimple.v().newAssignStmt(allStackTraceElementsString, arraysToStringInvokeExpr2);
            insertedStmts.add(assignArraysString);
            staticInvokeExpr = Jimple.v()
                    .newStaticInvokeExpr(logMethod.makeRef(),
                            StringConstant.v("All stack content"), allStackTraceElementsString);
            invokeStmt = Jimple.v().newInvokeStmt(staticInvokeExpr);
            insertedStmts.add(invokeStmt);

            return insertedStmts;
        }
    }

    private static void saveAllMethodNames(){
        try(PrintWriter pw = new PrintWriter(new FileWriter(METHOD_NAME_FILE))){
            TARGET_PACKAGE_METHOD_NAME.forEach(pw::println);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
