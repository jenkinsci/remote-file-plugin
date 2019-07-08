package org.jenkinsci.plugins.workflow.multibranch.extended.scm;

import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This class consist of necessary methods to filter SCM Descriptor for Remote Jenkins File Plugin
 * @author Aytunc BEKEN, aytuncbeken.ab@gmail.com
 */
public class SCMFilter {

    /**
     * This methods return filtered SCM Descriptor to show in the Remote Jenkins File Plugin SCM Definition
     * @return Collection of SCM Descriptors which do not contain "None"
     */
    public static Collection<? extends SCMDescriptor<?>> filter() {
        ArrayList<SCMDescriptor<?>> list = new ArrayList<>();
        for (SCMDescriptor scmDescriptor : SCM.all()) {
            if (!scmDescriptor.getDisplayName().equals("None")) {
                list.add(scmDescriptor);
            }
        }
        return list;
    }
}
