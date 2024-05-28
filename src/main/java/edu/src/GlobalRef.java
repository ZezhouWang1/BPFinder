package edu.src;

import edu.model.TypeStateCallback;
import edu.model.TypeStateComponent;
import edu.model.TypeStateICC;
import edu.model.TypeStateMethod;
import edu.model.sourcefile.ResourceLeakRule;
import edu.model.sourcefile.TypeStateRule;
import soot.SootMethod;

import java.util.ArrayList;
import java.util.List;

public class GlobalRef 
{
	/**
	 * analysis results
	 */
	public static List<TypeStateMethod> typeStateMethodList = new ArrayList<>();
	public static List<TypeStateComponent> typeStateComponentList = new ArrayList<>();
	public static List<TypeStateICC> typeStateICCList = new ArrayList<>();
	public static List<TypeStateCallback> typeStateCallbackList = new ArrayList<>();

	public static List<TypeStateRule> typeStateRules = new ArrayList<>();
	public static List<ResourceLeakRule> resourceLeakRules = new ArrayList<>();
	public static List<String> allAPIList = new ArrayList<>();

	public static List<List<String>> apiCallOrders4Method = new ArrayList<>();
	public static List<List<String>> apiCallOrders4Component = new ArrayList<>();
	public static List<List<String>> apiCallOrders4ICC = new ArrayList<>();
	public static List<List<String>> apiCallOrders4Callback = new ArrayList<>();

	public static List<SootMethod> apiCallOrders4Method_correspondingTargetMethod = new ArrayList<>();

	public static List<List<SootMethod>> methodCallOrders4Component = new ArrayList<>();
	public static List<List<SootMethod>> methodCallOrders4ICC = new ArrayList<>();
	public static List<List<SootMethod>> methodCallOrders4Callback = new ArrayList<>();

	public static String typeStateRulesPath = "./resources/TypeStateRules.txt";
	public static String resourceLeakRulesPath = "./resources/ResourceLeakRules.txt";
	public static String allAPIListPath = "./resources/AllAPI.txt";

	public static Integer minSDKVersion = 1;
	public static Integer maxSDKVersion = 0;
	public static Integer targetSDKVersion = 0;

	public static final String FIELD_VERSION_SDK_INT = "<android.os.Build$VERSION: int SDK_INT>";
	public static final String FIELD_VERSION_SDK = "<android.os.Build$VERSION: java.lang.String SDK>";

	public static final String IC3_PATH = "/Users/xsun0035/Desktop/";

}
