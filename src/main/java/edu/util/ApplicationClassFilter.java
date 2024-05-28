package edu.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;

import java.util.ArrayList;
import java.util.List;

public class ApplicationClassFilter {

    private static List<String> classNames;

    /**
     * @param sootClass
     * @return
     */
    public static boolean isApplicationClass(SootClass sootClass) {
        return isApplicationClass(sootClass.getPackageName());
    }

    /**
     * @return
     */
    public static boolean isApplicationClass(String clsName) {
        if (StringUtils.isBlank(clsName)) {
            return false;
        }
        if (clsName.startsWith("com.google.")
                || clsName.startsWith("soot.")
                || clsName.startsWith("android.")
                || clsName.startsWith("java.")
                || clsName.startsWith("com.facebook.")
                || clsName.startsWith("org.apache.")
                || clsName.startsWith("dalvik.system.")
                || clsName.startsWith("com.android.internal")
        ) {
            return false;
        }
        return true;
    }

    public static boolean containsClassInSystemPackage(String className) {
        return className.contains("android.") || className.contains("java.") || className.contains("javax.")
                || className.contains("sun.") || className.contains("org.omg.")
                || className.contains("org.w3c.dom.") || className.contains("com.google.")
                || className.contains("com.android.") || className.contains("org.apache.")
                || className.contains("soot.")
                || className.contains("androidx.");
    }

    public static boolean isClassInSystemPackage(String className) {
        return className.startsWith("android.") || className.startsWith("java.") || className.startsWith("javax.")
                || className.startsWith("sun.") || className.startsWith("org.omg.")
                || className.startsWith("org.w3c.dom.") || className.startsWith("com.google.")
                || className.startsWith("com.android.") || className.startsWith("org.apache.")
                || className.startsWith("soot.")
                || className.startsWith("kotlinx.")
                || className.startsWith("kotlin.")
                || className.startsWith("androidx.");
    }

    public static boolean isClassSystemPackage(String className) {
        return className.startsWith("<android.") || className.startsWith("<java.") || className.startsWith("<javax.")
                || className.startsWith("<sun.") || className.startsWith("<org.omg.")
                || className.startsWith("<org.w3c.dom.") || className.startsWith("<com.google.")
                || className.startsWith("<com.android.") || className.startsWith("<org.apache.")
                || className.startsWith("<soot.")
                || className.startsWith("<kotlinx.")
                || className.startsWith("<kotlin.")
                || className.startsWith("<androidx.");
    }

    public static boolean isAndroidSystemPackage(String className) {
        return className.startsWith("android.")
                || className.startsWith("com.android.")
                || className.startsWith("androidx.")
                || className.startsWith("com.google.android");
    }

    public static boolean isAndroidSystemAPI(String className) {
        return className.startsWith("<android.")
                || className.startsWith("<com.android.")
                || className.startsWith("<com.google.android")
                || className.startsWith("<androidx.");
    }

    public static boolean isAndroidLifeCycleMethod(String methodName) {
        return methodName.contains("onCreate")
                || methodName.contains("onStart")
                || methodName.contains("onResume")
                || methodName.contains("onPause")
                || methodName.contains("onStop")
                || methodName.contains("onDestroy")
                || methodName.contains("<init>")
                || methodName.contains("finish()")
                ;
    }

    public static boolean isAndroidUIMethod(String unitString) {
        return unitString.startsWith("<android.widget")
                || unitString.startsWith("<android.view")
                || unitString.startsWith("<android.webkit")
                || unitString.startsWith("<android.content.res.Resources")
                || unitString.startsWith("<android.app.Dialog")
                || unitString.startsWith("<android.app.AlertDialog")
                ;
    }

    public static boolean isAndroidSystemAPI(InvokeExpr invokeExpr) {
        try {
            return invokeExpr.getMethod().getDeclaringClass().hasSuperclass()
                    && !invokeExpr.getMethod().getDeclaringClass().getSuperclassUnsafe().getName().equals("java.lang.Object")
                    && (invokeExpr.getMethod().getDeclaringClass().getMethodByName(invokeExpr.getMethod().getName()) != null);
        } catch (RuntimeException e) {
            return true;
        }
    }

    public static boolean isJavaBasicType(String className) {
        return className.startsWith("java.lang.String")
                || className.startsWith("java.lang.Boolean")
                || className.startsWith("java.lang.Byte")
                || className.startsWith("java.lang.Character")
                || className.startsWith("java.lang.Double")
                || className.startsWith("java.lang.Float")
                || className.startsWith("java.lang.Integer")
                || className.startsWith("java.lang.Long")
                || className.startsWith("java.lang.Short")
                ;
    }

    public static boolean isThirdPartyLibrary(String classNameFullPath, String applicationName) {
        if (classNameFullPath.contains(applicationName)) {
            return false;
        }
        return true;
    }

    public static boolean isDummyMethod(SootMethod sootMethod) {
        if (sootMethod.getDeclaringClass().toString().contains("dummyMainClass")) {
            return true;
        }
        return false;
    }

    public static String transform2Superclass(Unit unit) {
        String currentStmt = Regex.getSubUtilSimple(unit.toString(), "(<.*>)");
        if (StringUtils.isBlank(currentStmt)) {
            throw new RuntimeException("[transform2Superclass]:" + unit);
        }
        if (Scene.v().containsMethod(currentStmt) && Scene.v().getMethod(currentStmt).getDeclaringClass().hasSuperclass()) {
            String methodNameAndParams = Regex.getSubUtilSimple(currentStmt, "(:.*?>)");
            String res = "<" + Scene.v().getMethod(currentStmt).getDeclaringClass().getSuperclassUnsafe().getName().replace("$", ".") + methodNameAndParams;
            return res;
        }
        throw new RuntimeException("[transform2Superclass]:" + unit + " doesn't has super class!");
    }

    public static boolean hasAndroidSuperClass(Unit unit) {
        String currentStmt = Regex.getSubUtilSimple(unit.toString(), "(<.*>)");
        if (StringUtils.isBlank(currentStmt)) {
            return false;
        }

        if (isAndroidSystemAPI(currentStmt)) {
            return false;
        }

        if (Scene.v().containsMethod(currentStmt)
                && Scene.v().getMethod(currentStmt).getDeclaringClass().hasSuperclass()
                && isAndroidSystemPackage(Scene.v().getMethod(currentStmt).getDeclaringClass().getSuperclassUnsafe().getName())
        ) {
            String methodNameAndParams = Regex.getSubUtilSimple(currentStmt, "(:.*?>)");
            String res = "<" + Scene.v().getMethod(currentStmt).getDeclaringClass().getSuperclassUnsafe().getName().replace("$", ".") + methodNameAndParams;
            if (Scene.v().containsMethod(res)) {
                return true;
            }
        }

        return false;
    }

    public static boolean is4ComponentClass(SootClass sootClass) {
        if (CollectionUtils.isEmpty(sootClass.getMethods())) {
            return false;
        }

        for (SootMethod sootMethod : sootClass.getMethods()) {
            if (is4ComponentCallbackMethod(sootMethod)) {
                return true;
            }
        }
        return false;
    }

    public static boolean is4ComponentCallbackMethod(SootMethod sootMethod) {
        String methodName = sootMethod.getName();
        return methodName.startsWith("onCreate")
                || methodName.startsWith("onStart")
                || methodName.startsWith("onResume")
                || methodName.startsWith("onPause")
                || methodName.startsWith("onStop")
                || methodName.startsWith("onDestroy")
                || methodName.startsWith("onStartCommand")
                || methodName.startsWith("onBind")
                || methodName.startsWith("onUnbind")
                || methodName.startsWith("onRebind")
                || methodName.startsWith("onReceive")
                || methodName.startsWith("onRestart")
                ;
    }

    public static boolean containsStartRecording(List<String> apiCallOrderList) {
        for (String apiCall : apiCallOrderList) {
            if (apiCall.contains("android.media.tv.TvRecordingClient") && apiCall.contains("startRecording")) {
                return true;
            }
        }
        return false;
    }


    public static List<String> callbackRule1() {
        List<String> callbackMethodList = new ArrayList<>();
        callbackMethodList.add("onCreate");
        callbackMethodList.add("onStart");
        callbackMethodList.add("onResume");
        callbackMethodList.add("onPause");
        callbackMethodList.add("onStop");
        callbackMethodList.add("onDestroy");
        return callbackMethodList;
    }

    public static List<String> callbackRule2() {
        List<String> callbackMethodList = new ArrayList<>();
        callbackMethodList.add("onStop");
        callbackMethodList.add("onCreate");
        return callbackMethodList;
    }

    public static List<String> callbackRule3() {
        List<String> callbackMethodList = new ArrayList<>();
        callbackMethodList.add("onPause");
        callbackMethodList.add("onResume");
        return callbackMethodList;
    }

    public static List<String> callbackRule4() {
        List<String> callbackMethodList = new ArrayList<>();
        callbackMethodList.add("onStop");
        callbackMethodList.add("onRestart");
        callbackMethodList.add("onStart");
        return callbackMethodList;
    }

    public static List<String> callbackRule5() {
        List<String> callbackMethodList = new ArrayList<>();
        callbackMethodList.add("onCreate");
        callbackMethodList.add("onStartCommand");
        callbackMethodList.add("onDestroy");
        return callbackMethodList;
    }

    public static List<String> callbackRule6() {
        List<String> callbackMethodList = new ArrayList<>();
        callbackMethodList.add("onCreate");
        callbackMethodList.add("onBind");
        callbackMethodList.add("onUnbind");
        callbackMethodList.add("onDestroy");
        return callbackMethodList;
    }

    public static List<String> callbackRule7() {
        List<String> callbackMethodList = new ArrayList<>();
        callbackMethodList.add("onReceive");
        return callbackMethodList;
    }


}