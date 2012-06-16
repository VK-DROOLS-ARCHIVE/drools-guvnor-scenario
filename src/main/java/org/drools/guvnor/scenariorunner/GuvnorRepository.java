package org.drools.guvnor.scenariorunner;

import org.restlet.resource.ClientResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vinod Kiran
 */
public class GuvnorRepository {
    static Logger log = LoggerFactory.getLogger(GuvnorRepository.class);

    String url = "";
    String user;
    String password;
    boolean connected = false;

    public GuvnorRepository(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isConnected() {
        return connected;
    }

    public void connect(){
        connected = true;
    }

    public List<GuvnorPackage> getPackages() throws GuvnorConnectionException, GuvnorRemoteException {
        if ( !isConnected() ) {
            throw new GuvnorConnectionException("Not Connected! Please call connect method on GuvnorRepository!");
        }
        List<GuvnorPackage> packageList = new ArrayList<GuvnorPackage>();
        StringWriter stringWriter = new StringWriter();
        try {
            new ClientResource(url + "/rest/packages").get().write(stringWriter);
            Document doc = Utils.getDocument(new ByteArrayInputStream(stringWriter.getBuffer().toString().getBytes()));
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();
            XPathExpression expr = xpath.compile("/feed/entry/title/text()");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            //System.out.println("nodes.getLength() == " + nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                GuvnorPackage guvnorPackage = new GuvnorPackage(nodes.item(i).getNodeValue());
                guvnorPackage.setGuvnorRepository(this);
                packageList.add(guvnorPackage);
            }
        } catch (IOException e) {
            throw new GuvnorRemoteException(e);
        } catch (XPathExpressionException e) {
            throw new GuvnorRemoteException(e);
        }
        return packageList;
    }

    public String getUrl() {
        return url;
    }

}
