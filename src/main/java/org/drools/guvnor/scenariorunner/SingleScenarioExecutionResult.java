package org.drools.guvnor.scenariorunner;

import org.drools.guvnor.client.rpc.ScenarioResultSummary;

/**
 * Created with IntelliJ IDEA.
 * User: vinod
 * Date: 27/5/12
 * Time: 8:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class SingleScenarioExecutionResult extends ScenarioResultSummary {
    int percentCovered = -1;
    @Override
    public String toString() {
        if ( getFailures() == 0 )
            return "SUCCESS " + getScenarioName()+" % Coverage : "+percentCovered;
        return "FAILURE " + getScenarioName() + " (" + getFailures() + " failures out of " + getTotal() + ")";
    }

    public SingleScenarioExecutionResult(int failures, int total, String scenarioName, String scenarioDescription, String uuid) {
        super(failures, total, scenarioName, scenarioDescription, uuid);
    }

    public SingleScenarioExecutionResult() {
        super();
    }

    public void setPercentCovered(int coverage){
        percentCovered = coverage;
    }

    public int getPercentCovered(){
        return percentCovered;
    }

    public String[] getUnfiredRules(){
        return null;
    }
}
