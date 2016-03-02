/*******************************************************************************
 * Copyright (c) 2015 Hamid Younesy.
 *
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of Hamid Younesy All rights are reserved.
 * This software is supplied without any warranty or guaranteed support whatsoever. 
 * The copyright holder is not responsible for its use, misuse, or functionality.
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 3.0 which is available at http://opensource.org/licenses/LGPL-3.0.
 *******************************************************************************/

package util;

import org.apache.log4j.*;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;

import java.io.File;
import java.io.IOException;

public class DirectoryManager {

    private static Logger log = Logger.getLogger(DirectoryManager.class);

    private static File USER_HOME;
    private static File USER_DIRECTORY;         // FileSystemView.getFileSystemView().getDefaultDirectory();
    private static File APPLICATION_DIRECTORY;   // The application directory
    private static File R_LIB_DIRECTORY;     // The application directory

    @SuppressWarnings("serial")
    static class DataLoadException extends RuntimeException {

        private String message;
        private String fileName;

        public DataLoadException(String message) {
            this.message = message.replace("<html>", "");
        }

        public DataLoadException(String message, String fileName) {
            if(message != null) this.message = message.replace("<html>", "");
            this.fileName = fileName;
        }

        @Override
        public String getMessage() {
            return fileName == null ? message : "An error occurred while accessing:    " + fileName + "<br>" + message;
        }
    }
    
    private static File getUserHome() {
        if (USER_HOME == null) {
            String userHomeString = System.getProperty("user.home");
            USER_HOME = new File(userHomeString);
        }
        return USER_HOME;
    }

    /**
     * The user directory.  On Mac and Linux this should be the user home directory.  On Windows platforms this
     * is the "My Documents" directory.
     */
    public static synchronized File getUserDirectory() {
        if (USER_DIRECTORY == null) {
            System.out.print("Fetching user directory... ");
            USER_DIRECTORY = FileSystemView.getFileSystemView().getDefaultDirectory();
            //Mostly for testing, in some environments USER_DIRECTORY can be null
            if (USER_DIRECTORY == null) {
                USER_DIRECTORY = getUserHome();
            }
        }
        return USER_DIRECTORY;
    }


    public static File getApplicationDirectory() {

        if (APPLICATION_DIRECTORY == null) {

            // Hack for known Java / Windows bug.   Attempt to remvoe (possible) read-only bit from user directory
            if (System.getProperty("os.name").startsWith("Windows")) {
                try {
                    Runtime.getRuntime().exec("attrib -r \"" + getUserDirectory().getAbsolutePath() + "\"");
                } catch (Exception e) {
                    // We tried
                }
            }

            APPLICATION_DIRECTORY = getApplicationDirectoryOverride();

            // If still null, try the default place
            if (APPLICATION_DIRECTORY == null) {
                File rootDir = getUserHome();
                APPLICATION_DIRECTORY = new File(rootDir, "ChAsE");

                if (!APPLICATION_DIRECTORY.exists()) {
                    try {
                        boolean wasSuccessful = APPLICATION_DIRECTORY.mkdir();
                        if (!wasSuccessful) {
                            System.err.println("Failed to create user directory!");
                            APPLICATION_DIRECTORY = null;
                        }
                    } catch (Exception e) {
                        log.error("Error creating application directory", e);
                    }
                }
            }


            if (APPLICATION_DIRECTORY == null || !APPLICATION_DIRECTORY.exists() || !canWrite(APPLICATION_DIRECTORY)) {
                int option = JOptionPane.showConfirmDialog(null,
                        "<html>The default application directory (" + APPLICATION_DIRECTORY + ") " +
                                "cannot be accessed.  Click Yes to choose a new folder or No to exit.<br>" +
                                "This folder will be used to create the 'ChAsE' directory",
                        "Application Directory Error", JOptionPane.YES_NO_OPTION);

                if (option == JOptionPane.YES_OPTION) {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {   
                        File parentDirectory = chooser.getSelectedFile();
                        if (parentDirectory != null) {
                            APPLICATION_DIRECTORY = new File(parentDirectory, "ChAsE");
                            APPLICATION_DIRECTORY.mkdir();
                            //Preferences prefs = Preferences.userNodeForPackage(Globals.class);
                            //prefs.put(DIR_USERPREF, APPLICATION_DIRECTORY.getAbsolutePath());
                        }
                    }
                }
            }

            if (APPLICATION_DIRECTORY == null || !APPLICATION_DIRECTORY.canRead()) {
                throw new DataLoadException("Cannot read from user directory", APPLICATION_DIRECTORY.getAbsolutePath());
            } else if (!canWrite(APPLICATION_DIRECTORY)) {
                throw new DataLoadException("Cannot write to user directory", APPLICATION_DIRECTORY.getAbsolutePath());
            }

            log.info("Application Directory: " + APPLICATION_DIRECTORY.getAbsolutePath());
        }
        return APPLICATION_DIRECTORY;
    }

    private static File getApplicationDirectoryOverride() {
        return null;
        /*
        Preferences userPrefs = null;
        File override = null;
        try {
            // See if an override location has been specified.  This is stored with the Java Preferences API
            userPrefs = Preferences.userNodeForPackage(Globals.class);
            String userDir = userPrefs.get(DIR_USERPREF, null);
            if (userDir != null) {
                override = new File(userDir);
                if (!override.exists()) {
                    override = null;
                    userPrefs.remove(DIR_USERPREF);
                }
            }
        } catch (Exception e) {
            userPrefs.remove(DIR_USERPREF);
            override = null;
            System.err.println("Error creating user directory");
            e.printStackTrace();
        }
        return override;*/
    }


    public static File getRLibsDirectory() {
        if (R_LIB_DIRECTORY == null) {

            //Create the Genome Cache
            R_LIB_DIRECTORY = new File(getApplicationDirectory(), "RLibs");
            if (!R_LIB_DIRECTORY.exists()) {
                R_LIB_DIRECTORY.mkdir();
            }
            if (!R_LIB_DIRECTORY.canRead()) {
                throw new DataLoadException("Cannot read from user directory", R_LIB_DIRECTORY.getAbsolutePath());
            } else if (!R_LIB_DIRECTORY.canWrite()) {
                throw new DataLoadException("Cannot write to user directory", R_LIB_DIRECTORY.getAbsolutePath());
            }
        }
        return R_LIB_DIRECTORY;
    }

    public static synchronized File getLogFile() throws IOException {

        File logFile = new File(getApplicationDirectory(), "ChAsE.log");
        if (!logFile.exists()) {
            logFile.createNewFile();
        }
        return logFile;

    }


    /**
     * Return the user preferences property file  ("~/ChAsE/prefs.properties").
     *
     * @return
     */
    public static synchronized File getPreferencesFile() {

        File applicationDirectoy = getApplicationDirectory();
        File applicationPropertyFile = new File(applicationDirectoy, "prefs.properties");

        if (!applicationPropertyFile.exists()) {
            try {
                applicationPropertyFile.createNewFile();
            } catch (IOException e) {
                log.error("Could not create property file: " + applicationPropertyFile, e);
            }
        }

        return applicationPropertyFile;
    }



    private static boolean canWrite(File directory) {
        // There are bugs in the Windows Java JVM that can cause user directories to be non-writable (target fix is
        // Java 7).  The only way to know if the directory is writable for sure is to try to write something.
        if (Globals.IS_WINDOWS) {
            File testFile = null;
            try {
                testFile = new File(directory, "chase1337win.testfile");
                if (testFile.exists()) {
                    testFile.delete();
                }
                testFile.deleteOnExit();
                testFile.createNewFile();
                return testFile.exists();
            } catch (IOException e) {
                return false;
            } finally {
                if (testFile.exists()) {
                    testFile.delete();
                }
            }
        } else {
            return directory.canWrite();
        }

    }

    public static void initializeLog() {

        Logger logger = Logger.getRootLogger();

        PatternLayout layout = new PatternLayout();
        layout.setConversionPattern("%p [%d{ISO8601}] [%F:%L]  %m%n");

        // Create a log file that is ready to have text appended to it
        try {
            File logFile = getLogFile();
            RollingFileAppender appender = new RollingFileAppender();
            appender.setName("CHASE_ROLLING_APPENDER");
            appender.setFile(logFile.getAbsolutePath());
            appender.setThreshold(Level.ALL);
            appender.setMaxFileSize("1000KB");
            appender.setMaxBackupIndex(1);
            appender.setLayout(layout);
            appender.setAppend(true);
            appender.activateOptions();
            logger.addAppender(appender);

        } catch (IOException e) {
            // Can't create log file, just log to console
            System.err.println("Error creating log file");
            e.printStackTrace();
            ConsoleAppender consoleAppender = new ConsoleAppender();
            logger.addAppender(consoleAppender);
        }
    }
}
