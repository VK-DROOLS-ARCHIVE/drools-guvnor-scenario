package org.drools.guvnor.scenariorunner;

/**
 * Created with IntelliJ IDEA.
 * User: vinod
 * Date: 26/5/12
 * Time: 4:24 PM
 * @author Vinod Kiran
 */
public class GuvnorRemoteException extends Exception {
    public GuvnorRemoteException() {
        super();
    }

    public GuvnorRemoteException(String message) {
        super(message);
    }

    public GuvnorRemoteException(String message, Throwable cause) {
        super(message, cause);
    }

    public GuvnorRemoteException(Throwable cause) {
        super(cause);
    }
}
