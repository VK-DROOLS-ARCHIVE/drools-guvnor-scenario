package org.drools.guvnor.scenariorunner;

import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        String url = "http://localhost:9080/guvnor-5.3.0";
        GuvnorRepository repository = new GuvnorRepository(url);
        repository.connect();
        List<GuvnorPackage> packages = repository.getPackages();
        for (GuvnorPackage pkg : packages){

            pkg.executeAllScenarios();

            List<GuvnorScenario> scenarioList = pkg.getScenarios();
            for (GuvnorScenario guvnorScenario:scenarioList){
                SingleScenarioExecutionResult resultSummary = guvnorScenario.execute();
                System.out.println("Took "+guvnorScenario.getExecutionTimeMillis()+" millis to execute Scenario "+guvnorScenario.getTitle()+".");
                System.out.println(resultSummary.toString());
                List<String> reasons = guvnorScenario.getReasons();
                for (String reason : reasons){
                    System.out.println(reason);
                }
                System.out.println(guvnorScenario.prettyPrintEventLog());
            }
        }
    }
}
