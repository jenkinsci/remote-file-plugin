package org.jenkinsci.plugins.workflow.multibranch.extended.scm;

import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;

import java.util.ArrayList;
import java.util.Collection;

public class SCMFilter {

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
