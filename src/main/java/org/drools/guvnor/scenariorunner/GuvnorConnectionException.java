package org.drools.guvnor.scenariorunner;

/**
 * Created with IntelliJ IDEA.
 * User: vinod
 * Date: 7/5/12
 * Time: 5:08 PM
 * @author Vinod Kiran
 */
public class GuvnorConnectionException extends Exception {
    public GuvnorConnectionException() {
        super();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public GuvnorConnectionException(String message) {
        super(message);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public GuvnorConnectionException(String message, Throwable cause) {
        super(message, cause);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public GuvnorConnectionException(Throwable cause) {
        super(cause);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
