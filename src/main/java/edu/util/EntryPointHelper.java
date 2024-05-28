package edu.util;

import heros.solver.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import soot.*;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.callbacks.AbstractCallbackAnalyzer;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.callbacks.DefaultCallbackAnalyzer;
import soot.jimple.infoflow.android.callbacks.FastCallbackAnalyzer;
import soot.jimple.infoflow.android.callbacks.filters.AlienFragmentFilter;
import soot.jimple.infoflow.android.callbacks.filters.AlienHostComponentFilter;
import soot.jimple.infoflow.android.callbacks.filters.ApplicationCallbackFilter;
import soot.jimple.infoflow.android.callbacks.filters.UnreachableConstructorFilter;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacksSerializer;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointCreator;
import soot.jimple.infoflow.android.iccta.IccInstrumenter;
import soot.jimple.infoflow.android.manifest.IAndroidApplication;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.jimple.infoflow.cfg.LibraryClassPatcher;
import soot.jimple.infoflow.memory.FlowDroidMemoryWatcher;
import soot.jimple.infoflow.memory.FlowDroidTimeoutWatcher;
import soot.jimple.infoflow.memory.IMemoryBoundedSolver;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.infoflow.values.IValueProvider;
import soot.options.Options;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static soot.SootClass.BODIES;
import static soot.SootClass.HIERARCHY;

public class EntryPointHelper {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String DUMMY_CLASS_NAME = "dummyMainClass";
    public static final String DUMMY_METHOD_NAME = "main";
    protected String callbackFile = "./resources/AndroidCallbacks.txt";

    public static Set<String> addtionalDexFiles = new HashSet<String>();

    public AndroidEntryPointCreator entryPointCreator = null;
    public ProcessManifest manifest = null;
    public Set<SootClass> entrypoints = null;
    public MultiMap<SootClass, AndroidCallbackDefinition> callbackMethods = new HashMultiMap<>();
    public MultiMap<SootClass, SootClass> fragmentClasses = new HashMultiMap<>();
    public Set<String> callbackClasses = null;
    public InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
    public IValueProvider valueProvider = null;
    public IccInstrumenter iccInstrumenter = null;
    public ARSCFileParser resources = null;
    public SootClass scView = null;

    public void calculateEntryPoint(String apkFileLocation, String androidJar) throws XmlPullParserException, IOException {
        // Start a new Soot instance
        initializeSoot(apkFileLocation, androidJar);

        // Perform basic app parsing
        parseAppResources(apkFileLocation);

        // We need at least one entry point
        if (entrypoints == null || entrypoints.isEmpty()) {
            logger.warn("No entry points");
            return;
        }

        calculateCallbacks(null);
    }

    private void calculateCallbacks(SootClass entryPoint) throws IOException {
        // Add the callback methods
        LayoutFileParser lfp = null;
        if (callbackClasses != null && callbackClasses.isEmpty()) {
            logger.warn("Callback definition file is empty, disabling callbacks");
        } else {
            lfp = createLayoutFileParser();
            calculateCallbackMethods(lfp, entryPoint);
        }
        logger.info("Entry point calculation done.");
    }

    /**
     * Calculates the set of callback methods declared in the XML resource files or
     * the app's source code
     *
     * @param lfp       The layout file parser to be used for analyzing UI controls
     * @param component The Android component for which to compute the callbacks.
     *                  Pass null to compute callbacks for all components.
     * @throws IOException Thrown if a required configuration cannot be read
     */
    private void calculateCallbackMethods(LayoutFileParser lfp, SootClass component) throws IOException {
        final InfoflowAndroidConfiguration.CallbackConfiguration callbackConfig = config.getCallbackConfig();

        // Load the APK file
        if (config.getSootIntegrationMode().needsToBuildCallgraph())
            releaseCallgraph();

        // Make sure that we don't have any leftovers from previous runs
        PackManager.v().getPack("wjtp").remove("wjtp.lfp");
        PackManager.v().getPack("wjtp").remove("wjtp.ajc");

        // Get the classes for which to find callbacks
        Set<SootClass> entryPointClasses = getComponentsToAnalyze(component);

        // Collect the callback interfaces implemented in the app's
        // source code. Note that the filters should know all components to
        // filter out callbacks even if the respective component is only
        // analyzed later.
        AbstractCallbackAnalyzer jimpleClass = callbackClasses == null
                ? new DefaultCallbackAnalyzer(config, entryPointClasses, callbackMethods, callbackFile)
                : new DefaultCallbackAnalyzer(config, entryPointClasses, callbackMethods, callbackClasses);
        if (valueProvider != null)
            jimpleClass.setValueProvider(valueProvider);
        jimpleClass.addCallbackFilter(new AlienHostComponentFilter(entrypoints));
        jimpleClass.addCallbackFilter(new ApplicationCallbackFilter(entrypoints));
        jimpleClass.addCallbackFilter(new UnreachableConstructorFilter());
        jimpleClass.collectCallbackMethods();

        // Find the user-defined sources in the layout XML files. This
        // only needs to be done once, but is a Soot phase.
        lfp.parseLayoutFile(config.getAnalysisFileConfig().getTargetAPKFile());

        // Watch the callback collection algorithm's memory consumption
        FlowDroidMemoryWatcher memoryWatcher = null;
        FlowDroidTimeoutWatcher timeoutWatcher = null;
        if (jimpleClass instanceof IMemoryBoundedSolver) {
            // Make sure that we don't spend too much time and memory in the callback
            // analysis
            memoryWatcher = createCallbackMemoryWatcher(jimpleClass);
            timeoutWatcher = createCallbackTimeoutWatcher(callbackConfig, jimpleClass);
        }

        try {
            int depthIdx = 0;
            boolean hasChanged = true;
            boolean isInitial = true;
            while (hasChanged) {
                hasChanged = false;

                // Check whether the solver has been aborted in the meantime
                if (jimpleClass instanceof IMemoryBoundedSolver) {
                    if (((IMemoryBoundedSolver) jimpleClass).isKilled())
                        break;
                }

                // Create the new iteration of the main method
                createMainMethod(component);

                int numPrevEdges = 0;
                if (Scene.v().hasCallGraph()) {
                    numPrevEdges = Scene.v().getCallGraph().size();
                }
                // Since the generation of the main method can take some time,
                // we check again whether we need to stop.
                if (jimpleClass instanceof IMemoryBoundedSolver) {
                    if (((IMemoryBoundedSolver) jimpleClass).isKilled()) {
                        logger.warn("Callback calculation aborted due to timeout");
                        break;
                    }
                }

                if (!isInitial) {
                    // Reset the callgraph
                    releaseCallgraph();

                    // We only want to parse the layout files once
                    PackManager.v().getPack("wjtp").remove("wjtp.lfp");
                }
                isInitial = false;

                // Run the soot-based operations
                constructCallgraphInternal();
                if (!Scene.v().hasCallGraph())
                    throw new RuntimeException("No callgraph in Scene even after creating one. That's very sad "
                            + "and should never happen.");

                lfp.parseLayoutFileDirect(config.getAnalysisFileConfig().getTargetAPKFile());
                PackManager.v().getPack("wjtp").apply();

                // Creating all callgraph takes time and memory. Check whether
                // the solver has been aborted in the meantime
                if (jimpleClass instanceof IMemoryBoundedSolver) {
                    if (((IMemoryBoundedSolver) jimpleClass).isKilled()) {
                        logger.warn("Aborted callback collection because of low memory");
                        break;
                    }
                }

                if (numPrevEdges < Scene.v().getCallGraph().size())
                    hasChanged = true;

                // Collect the results of the soot-based phases
                if (this.callbackMethods.putAll(jimpleClass.getCallbackMethods()))
                    hasChanged = true;

                if (entrypoints.addAll(jimpleClass.getDynamicManifestComponents()))
                    hasChanged = true;

                // Collect the XML-based callback methods
                if (collectXmlBasedCallbackMethods(lfp, jimpleClass))
                    hasChanged = true;

                // Avoid callback overruns. If we are beyond the callback limit
                // for one entry point, we may not collect any further callbacks
                // for that entry point.
                if (callbackConfig.getMaxCallbacksPerComponent() > 0) {
                    for (Iterator<SootClass> componentIt = this.callbackMethods.keySet().iterator(); componentIt
                            .hasNext(); ) {
                        SootClass callbackComponent = componentIt.next();
                        if (this.callbackMethods.get(callbackComponent).size() > callbackConfig
                                .getMaxCallbacksPerComponent()) {
                            componentIt.remove();
                            jimpleClass.excludeEntryPoint(callbackComponent);
                        }
                    }
                }

                // Check depth limiting
                depthIdx++;
                if (callbackConfig.getMaxAnalysisCallbackDepth() > 0
                        && depthIdx >= callbackConfig.getMaxAnalysisCallbackDepth())
                    break;

                // If we work with an existing callgraph, the callgraph never
                // changes and thus it doesn't make any sense to go multiple
                // rounds
                if (config.getSootIntegrationMode() == InfoflowConfiguration.SootIntegrationMode.UseExistingCallgraph)
                    break;
            }
        } catch (Exception ex) {
            logger.error("Could not calculate callback methods", ex);
            throw ex;
        } finally {
            // Shut down the watchers
            if (timeoutWatcher != null)
                timeoutWatcher.stop();
            if (memoryWatcher != null)
                memoryWatcher.close();
        }

        // Filter out callbacks that belong to fragments that are not used by
        // the host activity
        AlienFragmentFilter fragmentFilter = new AlienFragmentFilter(invertMap(fragmentClasses));
        fragmentFilter.reset();
        for (Iterator<Pair<SootClass, AndroidCallbackDefinition>> cbIt = this.callbackMethods.iterator(); cbIt
                .hasNext(); ) {
            Pair<SootClass, AndroidCallbackDefinition> pair = cbIt.next();

            // Check whether the filter accepts the given mapping
            if (!fragmentFilter.accepts(pair.getO1(), pair.getO2().getTargetMethod()))
                cbIt.remove();
            else if (!fragmentFilter.accepts(pair.getO1(), pair.getO2().getTargetMethod().getDeclaringClass())) {
                cbIt.remove();
            }
        }

        // Avoid callback overruns
        if (callbackConfig.getMaxCallbacksPerComponent() > 0) {
            for (Iterator<SootClass> componentIt = this.callbackMethods.keySet().iterator(); componentIt.hasNext(); ) {
                SootClass callbackComponent = componentIt.next();
                if (this.callbackMethods.get(callbackComponent).size() > callbackConfig.getMaxCallbacksPerComponent())
                    componentIt.remove();
            }
        }

        // Make sure that we don't retain any weird Soot phases
        PackManager.v().getPack("wjtp").remove("wjtp.lfp");
        PackManager.v().getPack("wjtp").remove("wjtp.ajc");

        // Warn the user if we had to abort the callback analysis early
        boolean abortedEarly = false;
        if (jimpleClass instanceof IMemoryBoundedSolver) {
            if (((IMemoryBoundedSolver) jimpleClass).isKilled()) {
                logger.warn("Callback analysis aborted early due to time or memory exhaustion");
                abortedEarly = true;
            }
        }
        if (!abortedEarly)
            logger.info("Callback analysis terminated normally");

        // Serialize the callbacks
        if (callbackConfig.isSerializeCallbacks()) {
            CollectedCallbacks callbacks = new CollectedCallbacks(entryPointClasses, callbackMethods, fragmentClasses);
            CollectedCallbacksSerializer.serialize(callbacks, callbackConfig);
        }
    }

    public FlowDroidMemoryWatcher createCallbackMemoryWatcher(AbstractCallbackAnalyzer jimpleClass) {
        FlowDroidMemoryWatcher memoryWatcher = new FlowDroidMemoryWatcher(config.getMemoryThreshold());
        memoryWatcher.addSolver((IMemoryBoundedSolver) jimpleClass);
        return memoryWatcher;
    }

    public FlowDroidTimeoutWatcher createCallbackTimeoutWatcher(final InfoflowAndroidConfiguration.CallbackConfiguration callbackConfig,
                                                                AbstractCallbackAnalyzer analyzer) {
        if (callbackConfig.getCallbackAnalysisTimeout() > 0) {
            FlowDroidTimeoutWatcher timeoutWatcher = new FlowDroidTimeoutWatcher(
                    callbackConfig.getCallbackAnalysisTimeout());
            timeoutWatcher.addSolver((IMemoryBoundedSolver) analyzer);
            timeoutWatcher.start();
            return timeoutWatcher;
        }
        return null;
    }

    /**
     * Inverts the given {@link MultiMap}. The keys become values and vice versa
     *
     * @param original The map to invert
     * @return An inverted copy of the given map
     */
    private <K, V> MultiMap<K, V> invertMap(MultiMap<V, K> original) {
        MultiMap<K, V> newTag = new HashMultiMap<>();
        for (V key : original.keySet())
            for (K value : original.get(key))
                newTag.put(value, key);
        return newTag;
    }

    private void calculateCallbackMethodsFast(LayoutFileParser lfp, SootClass component) throws IOException {
        // Construct the current callgraph
        releaseCallgraph();
        createMainMethod(component);
        constructCallgraphInternal();

        // Get the classes for which to find callbacks
        Set<SootClass> entryPointClasses = getComponentsToAnalyze(component);

        // Collect the callback interfaces implemented in the app's
        // source code
        AbstractCallbackAnalyzer jimpleClass = callbackClasses == null
                ? new FastCallbackAnalyzer(config, entryPointClasses, callbackFile)
                : new FastCallbackAnalyzer(config, entryPointClasses, callbackClasses);
        if (valueProvider != null)
            jimpleClass.setValueProvider(valueProvider);
        jimpleClass.collectCallbackMethods();

        // Collect the results
        this.callbackMethods.putAll(jimpleClass.getCallbackMethods());
        this.entrypoints.addAll(jimpleClass.getDynamicManifestComponents());

        // Find the user-defined sources in the layout XML files. This
        // only needs to be done once, but is a Soot phase.
        lfp.parseLayoutFileDirect(config.getAnalysisFileConfig().getTargetAPKFile());

        // Collect the XML-based callback methods
        collectXmlBasedCallbackMethods(lfp, jimpleClass);

        // Construct the final callgraph
        releaseCallgraph();
        createMainMethod(component);
        constructCallgraphInternal();
    }

    /**
     * Collects the XML-based callback methods, e.g., Button.onClick() declared in
     * layout XML files
     *
     * @param lfp         The layout file parser
     * @param jimpleClass The analysis class that gives us a mapping between layout
     *                    IDs and components
     * @return True if at least one new callback method has been added, otherwise
     * false
     */
    private boolean collectXmlBasedCallbackMethods(LayoutFileParser lfp, AbstractCallbackAnalyzer jimpleClass) {
        SootMethod smViewOnClick = Scene.v()
                .grabMethod("<android.view.View$OnClickListener: void onClick(android.view.View)>");

        // Collect the XML-based callback methods
        boolean hasNewCallback = false;
        for (final SootClass callbackClass : jimpleClass.getLayoutClasses().keySet()) {
            if (jimpleClass.isExcludedEntryPoint(callbackClass))
                continue;

            Set<Integer> classIds = jimpleClass.getLayoutClasses().get(callbackClass);
            for (Integer classId : classIds) {
                ARSCFileParser.AbstractResource resource = this.resources.findResource(classId);
                if (resource instanceof ARSCFileParser.StringResource) {
                    final String layoutFileName = ((ARSCFileParser.StringResource) resource).getValue();

                    // Add the callback methods for the given class
                    Set<String> callbackMethods = lfp.getCallbackMethods().get(layoutFileName);
                    if (callbackMethods != null) {
                        for (String methodName : callbackMethods) {
                            final String subSig = "void " + methodName + "(android.view.View)";

                            // The callback may be declared directly in the
                            // class or in one of the superclasses
                            SootClass currentClass = callbackClass;
                            while (true) {
                                SootMethod callbackMethod = currentClass.getMethodUnsafe(subSig);
                                if (callbackMethod != null) {
                                    if (this.callbackMethods.put(callbackClass, new AndroidCallbackDefinition(
                                            callbackMethod, smViewOnClick, AndroidCallbackDefinition.CallbackType.Widget)))
                                        hasNewCallback = true;
                                    break;
                                }

                                SootClass sclass = currentClass.getSuperclassUnsafe();
                                if (sclass == null) {
                                    logger.error(String.format("Callback method %s not found in class %s", methodName,
                                            callbackClass.getName()));
                                    break;
                                }
                                currentClass = sclass;
                            }
                        }
                    }

                    // Add the fragments for this class
                    Set<SootClass> fragments = lfp.getFragments().get(layoutFileName);
                    if (fragments != null) {
                        for (SootClass fragment : fragments) {
                            if (fragmentClasses.put(callbackClass, fragment))
                                hasNewCallback = true;
                        }
                    }

                    // For user-defined views, we need to emulate their
                    // callbacks
                    Set<AndroidLayoutControl> controls = lfp.getUserControls().get(layoutFileName);
                    if (controls != null) {
                        for (AndroidLayoutControl lc : controls) {
                            if (!SystemClassHandler.v().isClassInSystemPackage(lc.getViewClass().getName()))
                                hasNewCallback |= registerCallbackMethodsForView(callbackClass, lc);
                        }
                    }
                } else
                    logger.error("Unexpected resource type for layout class");
            }
        }

        // Collect the fragments, merge the fragments created in the code with
        // those declared in Xml files
        if (fragmentClasses.putAll(jimpleClass.getFragmentClasses())) // Fragments
            // declared
            // in
            // code
            hasNewCallback = true;

        return hasNewCallback;
    }

    /**
     * Registers the callback methods in the given layout control so that they are
     * included in the dummy main method
     *
     * @param callbackClass The class with which to associate the layout callbacks
     * @param lc            The layout control whose callbacks are to be associated
     *                      with the given class
     * @return
     */
    private boolean registerCallbackMethodsForView(SootClass callbackClass, AndroidLayoutControl lc) {
        // Ignore system classes
        if (SystemClassHandler.v().isClassInSystemPackage(callbackClass.getName()))
            return false;

        // Get common Android classes
        if (scView == null)
            scView = Scene.v().getSootClass("android.view.View");

        // Check whether the current class is actually a view
        if (!Scene.v().getOrMakeFastHierarchy().canStoreType(lc.getViewClass().getType(), scView.getType()))
            return false;

        // There are also some classes that implement interesting callback
        // methods.
        // We model this as follows: Whenever the user overwrites a method in an
        // Android OS class, we treat it as a potential callback.
        SootClass sc = lc.getViewClass();
        Map<String, SootMethod> systemMethods = new HashMap<>(10000);
        for (SootClass parentClass : Scene.v().getActiveHierarchy().getSuperclassesOf(sc)) {
            if (parentClass.getName().startsWith("android."))
                for (SootMethod sm : parentClass.getMethods())
                    if (!sm.isConstructor())
                        systemMethods.put(sm.getSubSignature(), sm);
        }

        boolean changed = false;
        // Scan for methods that overwrite parent class methods
        for (SootMethod sm : sc.getMethods()) {
            if (!sm.isConstructor()) {
                SootMethod parentMethod = systemMethods.get(sm.getSubSignature());
                if (parentMethod != null) {
                    // This is a real callback method
                    changed |= this.callbackMethods.put(callbackClass,
                            new AndroidCallbackDefinition(sm, parentMethod, AndroidCallbackDefinition.CallbackType.Widget));
                }
            }
        }
        return changed;
    }


    /**
     * Creates a new layout file parser. Derived classes can override this method to
     * supply their own parser.
     *
     * @return The newly created layout file parser.
     */
    public LayoutFileParser createLayoutFileParser() {
        return new LayoutFileParser(this.manifest.getPackageName(), this.resources);
    }

    /**
     * Triggers the callgraph construction in Soot
     */
    protected void constructCallgraphInternal() {
        // If we are configured to use an existing callgraph, we may not replace
        // it. However, we must make sure that there really is one.
        if (config.getSootIntegrationMode() == InfoflowConfiguration.SootIntegrationMode.UseExistingCallgraph) {
            if (!Scene.v().hasCallGraph())
                throw new RuntimeException("FlowDroid is configured to use an existing callgraph, but there is none");
            return;
        }

        // Make sure that we don't have any weird leftovers
        releaseCallgraph();

        // Construct the actual callgraph
        logger.info("Constructing the callgraph...");
        PackManager.v().getPack("cg").apply();

        // Make sure that we have a hierarchy
        Scene.v().getOrMakeFastHierarchy();
    }


    /**
     * Releases the callgraph and all intermediate objects associated with it
     */
    protected void releaseCallgraph() {
        // If we are configured to use an existing callgraph, we may not release
        // it
        if (config.getSootIntegrationMode() == InfoflowConfiguration.SootIntegrationMode.UseExistingCallgraph)
            return;

        Scene.v().releaseCallGraph();
        Scene.v().releasePointsToAnalysis();
        Scene.v().releaseReachableMethods();
        G.v().resetSpark();
    }

    private void createMainMethod(SootClass component) {
        // There is no need to create a main method if we don't want to generate
        // a callgraph
        if (config.getSootIntegrationMode() == InfoflowConfiguration.SootIntegrationMode.UseExistingCallgraph)
            return;

        // Always update the entry point creator to reflect the newest set
        // of callback methods
        entryPointCreator = createEntryPointCreator(component);
        SootMethod dummyMainMethod = entryPointCreator.createDummyMain();
        Scene.v().setEntryPoints(Collections.singletonList(dummyMainMethod));
        if (!dummyMainMethod.getDeclaringClass().isInScene())
            Scene.v().addClass(dummyMainMethod.getDeclaringClass());

        // addClass() declares the given class as a library class. We need to
        // fix this.
        dummyMainMethod.getDeclaringClass().setApplicationClass();
    }

    /**
     * Creates the {@link AndroidEntryPointCreator} instance which will later create
     * the dummy main method for the analysis
     *
     * @param component The single component to include in the dummy main method.
     *                  Pass null to include all components in the dummy main
     *                  method.
     * @return The {@link AndroidEntryPointCreator} responsible for generating the
     * dummy main method
     */
    private AndroidEntryPointCreator createEntryPointCreator(SootClass component) {
        Set<SootClass> components = getComponentsToAnalyze(component);

        // If we we already have an entry point creator, we make sure to clean up our
        // leftovers from previous runs
        if (entryPointCreator == null)
            entryPointCreator = new AndroidEntryPointCreator(manifest, components);
        else {
            entryPointCreator.removeGeneratedMethods(false);
            entryPointCreator.reset();
        }

        MultiMap<SootClass, SootMethod> callbackMethodSigs = new HashMultiMap<>();
        if (component == null) {
            // Get all callbacks for all components
            for (SootClass sc : this.callbackMethods.keySet()) {
                Set<AndroidCallbackDefinition> callbackDefs = this.callbackMethods.get(sc);
                if (callbackDefs != null)
                    for (AndroidCallbackDefinition cd : callbackDefs)
                        callbackMethodSigs.put(sc, cd.getTargetMethod());
            }
        } else {
            // Get the callbacks for the current component only
            for (SootClass sc : components) {
                Set<AndroidCallbackDefinition> callbackDefs = this.callbackMethods.get(sc);
                if (callbackDefs != null)
                    for (AndroidCallbackDefinition cd : callbackDefs)
                        callbackMethodSigs.put(sc, cd.getTargetMethod());
            }
        }
        entryPointCreator.setCallbackFunctions(callbackMethodSigs);
        entryPointCreator.setFragments(fragmentClasses);
        entryPointCreator.setComponents(components);
        return entryPointCreator;
    }

    public void parseAppResources(String apkFileLocation) throws IOException, XmlPullParserException {
        config.getAnalysisFileConfig().setTargetAPKFile(apkFileLocation);
        final File targetAPK = new File(config.getAnalysisFileConfig().getTargetAPKFile());
        if (!targetAPK.exists())
            throw new RuntimeException(
                    String.format("Target APK file %s does not exist", targetAPK.getCanonicalPath()));

        // Parse the resource file
        long beforeARSC = System.nanoTime();
        this.resources = new ARSCFileParser();
        this.resources.parse(targetAPK.getAbsolutePath());
        logger.info("ARSC file parsing took " + (System.nanoTime() - beforeARSC) / 1E9 + " seconds");

        // To look for callbacks, we need to start somewhere. We use the Android
        // lifecycle methods for this purpose.
        this.manifest = new ProcessManifest(targetAPK, resources);
        SystemClassHandler.v().setExcludeSystemComponents(config.getIgnoreFlowsInSystemPackages());
        Set<String> entryPoints = manifest.getEntryPointClasses();
        this.entrypoints = new HashSet<>(entryPoints.size());
        for (String className : entryPoints) {
            SootClass sc = Scene.v().getSootClassUnsafe(className);
            if (sc != null)
                this.entrypoints.add(sc);
        }
    }

    /**
     * Gets the components to analyze. If the given component is not null, we assume
     * that only this component and the application class (if any) shall be
     * analyzed. Otherwise, all components are to be analyzed.
     *
     * @param component A component class name to only analyze this class and the
     *                  application class (if any), or null to analyze all classes.
     * @return The set of classes to analyze
     */
    private Set<SootClass> getComponentsToAnalyze(SootClass component) {
        if (component == null)
            return this.entrypoints;
        else {
            // We always analyze the application class together with each
            // component as there might be interactions between the two
            Set<SootClass> components = new HashSet<>(2);
            components.add(component);

            IAndroidApplication app = manifest.getApplication();
            if (app != null) {
                String applicationName = app.getName();
                if (applicationName != null && !applicationName.isEmpty())
                    components.add(Scene.v().getSootClassUnsafe(applicationName));
            }
            return components;
        }
    }

    /**
     * Initializes soot for running the soot-based phases of the application
     * metadata analysis
     */
    private void initializeSoot(String apkFileLocation, String androidJar) {
        logger.info("Initializing Soot...");

        // Clean up any old Soot instance we may have
        G.reset();

        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        //Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_whole_program(true);
        Options.v().set_process_dir(Collections.singletonList(apkFileLocation));

        Options.v().set_android_jars(androidJar);
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_keep_offset(false);
        Options.v().set_keep_line_number(true);
        Options.v().set_throw_analysis(Options.throw_analysis_dalvik);
        Options.v().set_process_multiple_dex(true);
        Options.v().set_search_dex_in_archives(true);
        Options.v().set_ignore_resolution_errors(true);

        Scene.v().addBasicClass("androidx.activity.ComponentActivity", BODIES);
        Scene.v().addBasicClass("android.app.Application$ActivityLifecycleCallbacks", HIERARCHY);
        Scene.v().addBasicClass("android.app.Application$OnProvideAssistDataListener", HIERARCHY);

        Main.v().autoSetOptions();
        Options.v().setPhaseOption("cg.spark", "on");

        // Load whatever we need
        logger.info("Loading dex files...");
        Scene.v().loadNecessaryClasses();

        // Make sure that we have valid Jimple bodies
        PackManager.v().getPack("wjpp").apply();

        // Patch the callgraph to support additional edges. We do this now,
        // because during callback discovery, the context-insensitive callgraph
        // algorithm would flood us with invalid edges.
        LibraryClassPatcher patcher = new LibraryClassPatcher();
        patcher.patchLibraries();
    }
}
