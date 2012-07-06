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
package de.akquinet.gomobile.deployment.mojo.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import de.akquinet.gomobile.deployment.mojo.BundleHeader;

/**
 * A {@code ManifestBuilder} handles addition of header valuesto the manifest.
 */
public class ManifestBuilder {

    /**
     * Referenced packages (by the composites).
     */
    private List<String> m_referredPackages = new ArrayList<String>();
    
    /**
     * Added bundle manifest headers.
     */
	private List<BundleHeader> m_headers = new ArrayList<BundleHeader>();


    /**
     * Add all given package names in the referred packages list
     * @param packageNames additional packages
     */
    public void addReferredPackage(Set<String> packageNames) {
        m_referredPackages.addAll(packageNames);
    }
    
    /**
     * Add a header ant its corresponding value to this manaifest
     * @param header a bundle header
     */
    public void addHeader(BundleHeader header) {
        m_headers.add(header);
    }

    /**
     * Update the given manifest.
     * @param original original manifest to be modified
     * @return modified manifest
     */
    public Manifest build(final Manifest original) {
        Attributes att = original.getMainAttributes();

        // add new headers or override original ones
        setExtraHeaders(att);
        
        // Add new imports
        setImports(att);

        return original;
    }

    private void setExtraHeaders(Attributes att) {
		for (BundleHeader header : m_headers) {
			att.putValue(header.getName(), header.getValue());
		}
	}

	/**
     * Add imports to the given manifest attribute list.
     * @param att : the manifest attribute list to modify.
     */
    private void setImports(Attributes att) {
        Map<String, Map<String, String>> imports = parseHeader(att.getValue("Import-Package"));
        Map<String, String> ver = new TreeMap<String, String>();
        
        // Add referred imports from the metadata
        for (int i = 0; i < m_referredPackages.size(); i++) {
            String pack = m_referredPackages.get(i);
            imports.put(pack, new TreeMap<String, String>());
        }

        // Write imports
        if (!imports.isEmpty())
        	att.putValue("Import-Package", printClauses(imports, "resolution:"));
    }

    /**
     * Standard OSGi header parser. This parser can handle the format
     * <pre>
     * clauses ::= clause ( ',' clause ) +
     * clause ::= name ( ';' name ) (';' key '=' value )
     * </pre>
     * This is mapped to a Map { name => Map { attr|directive => value } }
     *
     * @param value String to parse.
     * @return parsed map.
     */
    protected Map<String, Map<String, String>> parseHeader(String value) {
        if (value == null || value.trim().length() == 0) {
            return new HashMap<String, Map<String, String>>();
        }

        Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
        QuotedTokenizer qt = new QuotedTokenizer(value, ";=,");
        char del;
        do {
            boolean hadAttribute = false;
            Map<String, String> clause = new HashMap<String, String>();
            List<String> aliases = new ArrayList<String>();
            aliases.add(qt.nextToken());
            del = qt.getSeparator();
            while (del == ';') {
                String adname = qt.nextToken();
                if ((del = qt.getSeparator()) != '=') {
                    if (hadAttribute) {
                        throw new IllegalArgumentException("Header contains name field after attribute or directive: " + adname + " from " + value);
                    }
                    aliases.add(adname);
                } else {
                    String advalue = qt.nextToken();
                    clause.put(adname, advalue);
                    del = qt.getSeparator();
                    hadAttribute = true;
                }
            }
            for (Iterator<String> i = aliases.iterator(); i.hasNext();) {
                result.put(i.next(), clause);
            }
        } while (del == ',');
        return result;
    }

    /**
     * Print a standard Map based OSGi header.
     *
     * @param exports : map { name => Map { attribute|directive => value } }
     * @param allowedDirectives list of allowed directives.
     * @return the clauses
     */
    private String printClauses(Map<String, Map<String, String>> exports, String allowedDirectives) {
        StringBuffer sb = new StringBuffer();
        String del = "";

        for (Iterator i = exports.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String name = (String) entry.getKey();
            Map map = (Map) entry.getValue();
            sb.append(del);
            sb.append(name);

            for (Iterator j = map.entrySet().iterator(); j.hasNext();) {
                Map.Entry entry2 = (Map.Entry) j.next();
                String key = (String) entry2.getKey();

                // Skip directives we do not recognize
                if (key.endsWith(":") && allowedDirectives.indexOf(key) < 0) {
                    continue;
                }

                String value = (String) entry2.getValue();
                sb.append(";");
                sb.append(key);
                sb.append("=");
                boolean dirty = value.indexOf(',') >= 0 || value.indexOf(';') >= 0;
                if (dirty) {
                    sb.append("\"");
                }
                sb.append(value);
                if (dirty) {
                    sb.append("\"");
                }
            }
            del = ", ";
        }
        return sb.toString();
    }



}
