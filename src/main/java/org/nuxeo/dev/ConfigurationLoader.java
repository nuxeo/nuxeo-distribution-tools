/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.dev;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.dev.ConfigurationReader.SectionReader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ConfigurationLoader {

    protected Set<String> bundles;
    protected Set<String> libs;
    protected ConfigurationReader reader;
    protected String template; //template is not yet used
    protected String profile;
    protected String version; // version of nuxeo-dev-tools (which contain distrib configs)
    
    public ConfigurationLoader() {
        bundles = new HashSet<String>();
        libs = new HashSet<String>();
        reader = new ConfigurationReader();
        reader.addReader("bundles", new ArtifactReader(bundles));
        reader.addReader("libs", new ArtifactReader(libs));
        reader.addReader("properties", new PropertiesReader());
        reader.addReader("config", new ConfigReader());
    }
    
    public ConfigurationReader getReader() {
        return reader;
    }
    
    public Set<String> getBundles() {
        return bundles;
    }
    
    public Set<String> getLibs() {
        return libs;
    }
    
    public String getProfile() {
        return profile;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String getTemplate() {
        if (template == null) {
            if (profile == null) {
                throw new IllegalArgumentException("Neither profile or template was defined in config section");
            }
            template = "org.nuxeo.ecm.distribution:nuxeo-distribution-config::zip:"+profile;
        }
        return template;
    }
    
    class ArtifactReader implements SectionReader {
        protected Set<String> result; 
        ArtifactReader(Set<String> result) {
            this.result = result;
        }
        public void readLine(String section, String line) throws IOException {
            result.add(line);
        }
    }
    
    
    class PropertiesReader implements SectionReader {
        public void readLine(String section, String line) throws IOException {
            int p = line.indexOf('=');
            if (p == -1) {
                throw new IOException("Invalid properties line: "+line);
            }
            String key = line.substring(0, p).trim();
            String value = line.substring(p+1).trim();
            System.setProperty(key, value);
        }
    }

    class ConfigReader implements SectionReader {
        public void readLine(String section, String line) throws IOException {
            int p = line.indexOf('=');
            if (p == -1) {
                throw new IOException("Invalid configuration line: "+line);
            }
            String key = line.substring(0, p).trim();
            String value = line.substring(p+1).trim();
            if ("profile".equals(key)) {
                profile = value;
            } else if ("template".equals(key)) {
                template = value;
            } else if ("version".equals(key)) {
                version = value;
            } else {
                throw new IOException("Unknown configuration property: "+key);
            }
        }
    }
    
}
