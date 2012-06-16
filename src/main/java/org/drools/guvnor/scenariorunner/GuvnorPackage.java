package org.drools.guvnor.scenariorunner;

import org.drools.guvnor.client.rpc.ScenarioResultSummary;
import org.restlet.resource.ClientResource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * Date: 1/26/12
 * Time: 1:45 PM
 * @author Vinod Kiran
 */
public class GuvnorPackage implements Serializable{
    String title;
    GuvnorRepository guvnorRepository;
    List<GuvnorScenario> scenarioList;
    List<String> jars;

    public GuvnorPackage() {
    }

    public GuvnorPackage(String title) {
        this.title = title;
    }

    protected void setGuvnorRepository(GuvnorRepository repository) throws GuvnorRemoteException{
        this.guvnorRepository = repository;
        populateAssets();
    }
    
    public GuvnorRepository getGuvnorRepository(){
        return guvnorRepository;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getJARs(){
        return jars;
    }

    public List<GuvnorScenario> getScenarios() {
        return scenarioList;
    }

    protected void populateAssets() throws GuvnorRemoteException {
        String packageName = getTitle();
        scenarioList = new ArrayList<GuvnorScenario>();
        jars = new ArrayList<String>();
        StringWriter stringWriter = new StringWriter();
        try {
            new ClientResource(guvnorRepository.getUrl() + "/rest/packages/"+packageName+"/assets").get().write(stringWriter);
//            DocumentBuilder builder = Utils.getDocumentBuilder();
//            Document doc = builder.parse(new ByteArrayInputStream(stringWriter.getBuffer().toString().getBytes()));
            Document doc = Utils.getDocument(new ByteArrayInputStream(stringWriter.getBuffer().toString().getBytes()));
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();
            XPathExpression expr = xpath.compile("/feed/entry/metadata/format/value[text()='scenario']/../../..");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            for (int i = 0; i < nodes.getLength(); i++) {
                GuvnorScenario scenario = populateScenarioFromNode(nodes.item(i));
                scenarioList.add(scenario);
            }
            expr = xpath.compile("/feed/entry/metadata/format/value[text()='jar']/../../../title");
            result = expr.evaluate(doc, XPathConstants.NODESET);
            nodes = (NodeList) result;
            for (int i = 0; i < nodes.getLength(); i++) {
                jars.add(nodes.item(i).getTextContent());
            }
        } catch (IOException e) {
            new GuvnorRemoteException(e);
        } catch (XPathExpressionException e) {
            new GuvnorRemoteException(e);
        }
    }


    private GuvnorScenario populateScenarioFromNode(Node item) {
        GuvnorScenario scenario = new GuvnorScenario(guvnorRepository, this);
        NodeList nodeList = item.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++){
            Node n = nodeList.item(i);
            String nodeName = n.getNodeName();
            //System.out.println(nodeName);
            if ( "title".equalsIgnoreCase(nodeName)) {
                scenario.setTitle(n.getTextContent());
            } else if ("author".equalsIgnoreCase(nodeName)){
                scenario.setAuthor(n.getFirstChild().getTextContent());
            } else if ("metadata".equalsIgnoreCase(nodeName)){
                NodeList metadataNodeList = n.getChildNodes();
                for (int j = 0; j < metadataNodeList.getLength(); j++){
                    Node metadataNode = metadataNodeList.item(j);
                    String mdChildName = metadataNode.getNodeName();
                    if ("state".equalsIgnoreCase(mdChildName)){
                        scenario.setStatus(metadataNode.getFirstChild().getTextContent());
                    } else if ("uuid".equalsIgnoreCase(mdChildName)){
                        scenario.setUuid(metadataNode.getFirstChild().getTextContent());
                    } else if ("archived".equalsIgnoreCase(mdChildName)){
                        scenario.setArchived(Boolean.parseBoolean(metadataNode.getFirstChild().getTextContent()));
                    }
                }
            }
        }
        return scenario;
    }

    public String getDRL() throws GuvnorRemoteException{
        StringWriter stringWriter = new StringWriter();
        try {
            new ClientResource(guvnorRepository.getUrl() + "/rest/packages/"+getTitle()+"/source").get().write(stringWriter);
        } catch (IOException e) {
            throw new GuvnorRemoteException(e);
        }
        return stringWriter.toString();
    }

    public File getJARFile(String jarName) throws GuvnorRemoteException{
        try {
            File tempFile = File.createTempFile(title,"sr");
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            new ClientResource(guvnorRepository.getUrl() + "/rest/packages/"+getTitle()+"/assets/"+jarName+"/binary").get().write(bufferedOutputStream);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
//            System.out.println(tempFile.getAbsolutePath());
//            JarFile jarFile = new JarFile(tempFile);
//            Enumeration<JarEntry> entries = jarFile.entries();
//            while (entries.hasMoreElements()){
//                JarEntry entry = entries.nextElement();
//                System.out.println(entry.getName());
//            }
//            jarFile.close();
            return tempFile;
        } catch (IOException e) {
            throw new GuvnorRemoteException(e);
        }
    }

    public boolean executeAllScenarios(){
        List<ScenarioResultSummary> resultSummaries = new ArrayList<ScenarioResultSummary>();
        for (GuvnorScenario scenario : scenarioList) {
            resultSummaries.add(scenario.execute());
        }
        ScenarioResultSummary[] summaries = resultSummaries.toArray( new ScenarioResultSummary[resultSummaries.size()] );
//        return new BulkTestRunResult( null,
//            resultSummaries.toArray( summaries ),
//            coverage.getPercentCovered(),
//            coverage.getUnfiredRules() );
        return false;
    }
}
