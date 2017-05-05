/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo.handlers.dependency;

import java.util.Iterator;
import java.util.List;

import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Dependency Handler Description.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DependencyHandlerDescription extends HandlerDescription {

    /**
     * Dependencies managed by the dependency handler.
     */
    private DependencyDescription[] m_dependencies = new DependencyDescription[0];

    /**
     * Constructor.
     * @param handler : Handler.
     */
    public DependencyHandlerDescription(Handler handler) {
        super(handler);
    }

    /**
     * Get dependencies description.
     * @return the dependencies list.
     */
    public DependencyDescription[] getDependencies() {
        return m_dependencies;
    }

    /**
     * Add a dependency.
     * 
     * @param dep : the dependency to add
     */
    public void addDependency(DependencyDescription dep) {
        // Verify that the dependency description is not already in the array.
        for (int i = 0; i < m_dependencies.length; i++) {
            if (m_dependencies[i] == dep) {
                return; // NOTHING TO DO, the description is already in the
                        // array
            }
        }
        // The component Description is not in the array, add it
        DependencyDescription[] newDep = new DependencyDescription[m_dependencies.length + 1];
        System.arraycopy(m_dependencies, 0, newDep, 0, m_dependencies.length);
        newDep[m_dependencies.length] = dep;
        m_dependencies = newDep;
    }

    /**
     * Build Dependency Handler description.
     * @return the handler description.
     * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
     */
    public Element getHandlerInfo() {
        Element deps = super.getHandlerInfo();
        for (int i = 0; i < m_dependencies.length; i++) {
            String state = "resolved";
            if (m_dependencies[i].getState() == 2) {
                state = "unresolved";
            }
            if (m_dependencies[i].getState() == 3) {
                state = "broken";
            }
            Element dep = new Element("Requires", "");
            dep.addAttribute(new Attribute("Specification", m_dependencies[i].getInterface()));
            
            if (m_dependencies[i].getFilter() != null) {
                dep.addAttribute(new Attribute("Filter", m_dependencies[i].getFilter()));
            }
            
            if (m_dependencies[i].isOptional()) {
                dep.addAttribute(new Attribute("Optional", "true"));
            } else {
                dep.addAttribute(new Attribute("Optional", "false"));
            }

            if (m_dependencies[i].isMultiple()) {
                dep.addAttribute(new Attribute("Aggregate", "true"));
            } else {
                dep.addAttribute(new Attribute("Aggregate", "false"));
            }
            
            dep.addAttribute(new Attribute("State", state));
            List set = m_dependencies[i].getUsedServices();
            if (set != null) {
                Iterator iterator = set.iterator();
                while (iterator.hasNext()) {
                    Element use = new Element("Uses", "");
                    ServiceReference ref = (ServiceReference) iterator.next();
                    use.addAttribute(new Attribute("service.id", ref.getProperty(Constants.SERVICE_ID).toString()));                
                    String instance = (String) ref.getProperty("instance.name");
                    if (instance != null) {
                        use.addAttribute(new Attribute("instance.name", instance));
                    }
                    dep.addElement(use);
                }
            }
            
            deps.addElement(dep);
        }
        return deps;
    }

}