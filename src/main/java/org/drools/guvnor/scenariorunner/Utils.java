package org.drools.guvnor.scenariorunner;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: vinod
 * Date: 26/5/12
 * Time: 11:59 AM
 * @author Vinod Kiran
 */
public class Utils {
    static DocumentBuilder documentBuilder = null;

    static {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false); // never forget this!
        try {
            documentBuilder =  factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static DocumentBuilder getDocumentBuilder() throws GuvnorRemoteException {
        return documentBuilder;
    }

    public static Document getDocument(InputStream is) throws GuvnorRemoteException{
        try {
            return documentBuilder.parse(is);
        } catch (SAXException e) {
            throw new GuvnorRemoteException(e);
        } catch (IOException e) {
            throw new GuvnorRemoteException(e);
        }
    }
}
