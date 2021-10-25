
package de.dfki.sds.spread2rml.io;

import de.dfki.sds.mschroeder.commons.lang.swing.LoadingListener;

/**
 * 
 */
public class ConsoleLoadingListener implements LoadingListener {

    @Override
    public void setMaximum(int max) {
        
    }

    @Override
    public void setCurrent(int current) {
        
    }

    @Override
    public boolean cancel() {
        return false;
    }

    @Override
    public void status(String status) {
        System.out.println(status);
    }

}
