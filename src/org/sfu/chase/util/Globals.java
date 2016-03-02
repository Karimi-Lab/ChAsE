/*
 * Copyright (c) 2007-2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 */

package org.sfu.chase.util;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class Globals {

    private static Logger log = Logger.getLogger(Globals.class);

    // Default user folder

    final static public Pattern commaPattern = Pattern.compile(",");
    final static public Pattern tabPattern = Pattern.compile("\t");
    final static public Pattern colonPattern = Pattern.compile(":");
    final static public Pattern dashPattern = Pattern.compile("-");
    final static public Pattern equalPattern = Pattern.compile("=");
    final static public Pattern semicolonPattern = Pattern.compile(";");
    final static public Pattern singleTabMultiSpacePattern = Pattern.compile("\t|( +)");
    final static public Pattern forwardSlashPattern = Pattern.compile("/");
    final static public Pattern whitespacePattern = Pattern.compile("\\s+");


    @SuppressWarnings("rawtypes")
	public static List emptyList = new ArrayList();
    public static String VERSION;
    public static String BUILD;
    public static String TIMESTAMP;
    public static double log2 = Math.log(2);


    final public static boolean IS_WINDOWS =
            System.getProperty("os.name").toLowerCase().startsWith("windows");
    
    final public static boolean IS_MAC =
            System.getProperty("os.name").toLowerCase().startsWith("mac");

    final public static boolean IS_LINUX =
            System.getProperty("os.name").toLowerCase().startsWith("linux");

    final public static boolean IS_JWS =
            System.getProperty("webstart.version", null) != null || System.getProperty("javawebstart.version", null) != null;

    public static final String JAVA_VERSION_STRING = "java.version";

    public static boolean development;
    public static boolean psa;

    static {
        Properties properties = new Properties();
        try {
            properties.load(Globals.class.getResourceAsStream("/resources/about.properties"));
        } catch (IOException e) {
            log.error("*** Error retrieving version and build information! ***", e);
        }
        VERSION = properties.getProperty("version", "???");
        BUILD = properties.getProperty("build", "???");
        TIMESTAMP = properties.getProperty("timestamp", "???");

        //Runtime property overrides compile-time property, if both exist.
        //If neither exist we default to false
        final String prodProperty = System.getProperty("development", properties.getProperty("development", "false"));
        development = Boolean.parseBoolean(prodProperty);
        if(development){
            log.warn("Development mode is enabled");
        }
        
        final String psaProperty = System.getProperty("psa", properties.getProperty("psa", "false"));
        psa = Boolean.parseBoolean(psaProperty);
    }

    public static String applicationString() {
        return "ChAsE Version " + VERSION + " (" + BUILD + ")" + TIMESTAMP;
    }

    public static String versionString() {
        return "<html>Version " + VERSION + " (" + BUILD + ")<br>" + TIMESTAMP;
    }

    public static boolean isDevelopment() {
        return development;
    }
    
    public static boolean isParameterSpaceAnalysis() {
    	return psa;
    }


    /**
     * Checks whether the current JVM is the minimum specified version
     * or higher. Only compares up to as many characters as
     * in {@code minVersion}
     *
     * @param minVersion
     * @return
     */
    public static boolean isVersionOrHigher(String minVersion) {
        String curVersion = System.getProperty(JAVA_VERSION_STRING);
        if (curVersion.length() >= minVersion.length()) {
            curVersion = curVersion.substring(0, minVersion.length());
        }
        return curVersion.compareTo(minVersion) >= 0;
    }
}
