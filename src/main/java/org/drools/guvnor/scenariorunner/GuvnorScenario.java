package org.drools.guvnor.scenariorunner;

import org.apache.commons.lang.time.StopWatch;
import org.drools.*;
import org.drools.base.ClassTypeResolver;
import org.drools.common.InternalWorkingMemory;
import org.drools.compiler.*;
import org.drools.guvnor.server.builder.AuditLogReporter;
import org.drools.ide.common.client.modeldriven.testing.Fixture;
import org.drools.ide.common.client.modeldriven.testing.Scenario;
import org.drools.ide.common.client.modeldriven.testing.VerifyRuleFired;
import org.drools.ide.common.server.testscenarios.RuleCoverageListener;
import org.drools.ide.common.server.testscenarios.ScenarioRunner;
import org.drools.ide.common.server.util.ScenarioXMLPersistence;
import org.restlet.resource.ClientResource;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * Date: 1/25/12
 * Time: 11:12 PM
 * @author Vinod Kiran
 */
public class GuvnorScenario implements Serializable {

    String title;
    boolean archived;
    String status;
    String uuid;
    String author;
    private GuvnorRepository repository;
    private GuvnorPackage guvnorPackage;
    private Scenario scenario;
    private long timeTakenInMills = -1 ;
    List<String> reasons = new ArrayList<String>();
    private AuditLogReporter logReporter;

    GuvnorScenario(GuvnorRepository repository, GuvnorPackage guvnorPackage) {
        this.repository = repository;
        this.guvnorPackage = guvnorPackage;
    }

    public long getExecutionTimeMillis(){
        return timeTakenInMills;
    }

    public String toString(){
        return "GuvnorScenario[title='"+getTitle()+"', Author='"+getAuthor()+"', Status='"+getStatus()
                                            +"', UUID='"+getUuid()+"', Archived="+isArchived()+"]";
    }
    public String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    public boolean isArchived() {
        return archived;
    }

    void setArchived(boolean archived) {
        this.archived = archived;
    }

    public String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    public String getUuid() {
        return uuid;
    }

    void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAuthor() {
        return author;
    }

    void setAuthor(String author) {
        this.author = author;
    }

    public List<String> getReasons() {
        if ( scenario != null ) {
            for (Fixture fixture : scenario.getFixtures()){
                if ( fixture instanceof VerifyRuleFired){
                    VerifyRuleFired verifyRuleFired = (VerifyRuleFired)fixture;
                    reasons.add(verifyRuleFired.getExplanation());
                }
            }
        }
        return reasons;
    }

    public List<String> getEvents() {
        if ( scenario != null && logReporter != null) {
            List<String> events = new ArrayList<String>();
            List<String[]> logEvents = logReporter.buildReport();
            for (String[] logEvent: logEvents){
                for (String event : logEvent ) {
                    //pure hack.....to prevent log from showing just ints (Type)
                    if ( event.trim().length() < 2) {
                        continue;
                    }
                    //end of hack
                    events.add(event);
                }
            }
            return events;
        }
        return Collections.emptyList();
    }

    public String prettyPrintEventLog(){
        StringBuilder sb = new StringBuilder("***********************************************************\n");
        sb.append("\tEvent Log for Scenario '"+getTitle()+"'.\n");
        sb.append("***********************************************************\n");
        for (String event : getEvents()) {
            sb.append(event+"\n");
        }
        sb.append("***********************************************************\n");
        return sb.toString();
    }

    public SingleScenarioExecutionResult execute() {
        timeTakenInMills = -1;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String packageName = guvnorPackage.getTitle();
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        reasons.clear();
        try {
            URLClassLoader customClassLoader = createURLClassloader();
            RuleBase ruleBase = loadRuleBase(customClassLoader);

            if ( ruleBase != null ) {

                RuleCoverageListener ruleCoverageListener = runScenario(packageName, customClassLoader, ruleBase);

                int[] totals = scenario.countFailuresTotal();
                SingleScenarioExecutionResult executionResult = new SingleScenarioExecutionResult(totals[0], totals[1], getTitle(), "", getUuid());
                executionResult.setPercentCovered(ruleCoverageListener.getPercentCovered());
                return executionResult;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stopWatch.stop();
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
            timeTakenInMills = stopWatch.getTime();
        }
        return new SingleScenarioExecutionResult(1,1,getTitle(),"",getUuid());
    }

    private RuleCoverageListener runScenario(String packageName, URLClassLoader customClassLoader, RuleBase ruleBase) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        StringWriter stringWriter = new StringWriter();
        new ClientResource(repository.getUrl() + "/rest/packages/"+packageName+"/assets/"+getTitle()+"/source").get().write(stringWriter);

        StatefulSession statefulSession = createStatefulSession(ruleBase);

        HashSet<String> imports = getImports(ruleBase.getPackages()[0]);
        ClassTypeResolver resolver = new ClassTypeResolver(imports,customClassLoader);
        scenario = ScenarioXMLPersistence.getInstance().unmarshal(stringWriter.toString());

        logReporter = new AuditLogReporter(statefulSession);

        RuleCoverageListener ruleCoverageListener = createCoverageListener(statefulSession);

        Thread.currentThread().setContextClassLoader(customClassLoader);
        ScenarioRunner runner = new ScenarioRunner(resolver, customClassLoader, (InternalWorkingMemory)statefulSession);
        runner.run(scenario);
        return ruleCoverageListener;
    }

    private StatefulSession createStatefulSession(RuleBase ruleBase) {
        SessionConfiguration sessionConfiguration = new SessionConfiguration();
        sessionConfiguration.setClockType(ClockType.PSEUDO_CLOCK);
        return ruleBase.newStatefulSession(sessionConfiguration, null);
    }

    private RuleCoverageListener createCoverageListener(StatefulSession statefulSession) {
        HashSet<String> expectedRuleSet = new HashSet<String>();
        for (Fixture fixture : scenario.getFixtures()){
            if ( fixture instanceof VerifyRuleFired){
                VerifyRuleFired verifyRuleFired = (VerifyRuleFired)fixture;
                expectedRuleSet.add(verifyRuleFired.getRuleName());
            }
        }
        RuleCoverageListener ruleCoverageListener = new RuleCoverageListener(expectedRuleSet);
        statefulSession.addEventListener(ruleCoverageListener);
        return ruleCoverageListener;
    }

    private RuleBase loadRuleBase(URLClassLoader customClassLoader) throws GuvnorRemoteException {
        RuleBaseConfiguration baseConfiguration = new RuleBaseConfiguration(customClassLoader);
        PackageBuilderConfiguration packageBuilderConfiguration = new PackageBuilderConfiguration(customClassLoader);
        packageBuilderConfiguration.setClassLoaderCacheEnabled(true);
        RuleBase ruleBase = RuleBaseFactory.newRuleBase(baseConfiguration);

        PackageBuilder builder = new PackageBuilder(packageBuilderConfiguration);

        try {
            builder.addPackageFromDrl(new StringReader(guvnorPackage.getDRL()));
        } catch (DroolsParserException e) {
            throw new GuvnorRemoteException(e);
        } catch (IOException e) {
            throw new GuvnorRemoteException(e);
        }
        PackageBuilderErrors errors = builder.getErrors();
        if ( !errors.isEmpty() ) {
            for (DroolsError error : errors.getErrors() ) {
                reasons.add(error.getMessage());
            }
            return null;
        }
        ruleBase.addPackage(builder.getPackage());
        return ruleBase;
    }

    private URLClassLoader createURLClassloader() throws GuvnorRemoteException {
        try {
            List<String> jarList = guvnorPackage.getJARs();
            URL[] urls = new URL[jarList.size()];
            int i=0;
            for ( String jarName : jarList ) {
                File tempJar = guvnorPackage.getJARFile(jarName);
                urls[i] = tempJar.toURI().toURL();
                i++;
            }
            return new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
        } catch (MalformedURLException e) {
           throw new GuvnorRemoteException(e);
        }
    }

    HashSet<String> getImports(org.drools.rule.Package aPackage) {
        HashSet<String> imports = new HashSet<String>();
        imports.add(aPackage.getName() + ".*");
        imports.addAll(aPackage.getImports().keySet());

        return imports;
    }
}
