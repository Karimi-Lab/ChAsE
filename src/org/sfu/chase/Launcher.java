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

package org.sfu.chase;

import java.awt.Image;
import java.net.URL;
import java.util.Locale;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

import org.apache.log4j.Logger;

import util.Globals;
import util.UpdateManager;

public class Launcher {
    private static Logger log = Logger.getLogger(org.sfu.chase.Launcher.class);
    
    public static void main(final String args[]) {
        UpdateManager.checkForUpdates();
        
        setDockLabel();
        
        ChaseOp.main(args);
        
        setDockIcon();
		
		// TODO better localization
		Locale.setDefault(new Locale("en", "US"));
    }    
    
    private static void setDockIcon() {
        /*
        MainFrame.getInstance().setIconImage(getIconImage());
        
        if (Globals.IS_MAC) {
            // http://alvinalexander.com/apple/mac/java-mac-native-look/java-on-mac.shtml
            try {
                Image image = getIconImage();
                OSXAdapter.setDockIconImage(image);
            } catch (Exception e) {
                log.error("Error setting apple dock icon", e);
            }
        }*/
    }

    private static Image getIconImage() {
        final String PATH_UI_RESOURCE = "/org/sfu/chase/resources";
        String path = PATH_UI_RESOURCE + "/ApplicationIcon.png";
        URL url = Launcher.class.getResource(path);
        Image image = new ImageIcon(url).getImage();
        return image;
    }
    
    private static void setDockLabel() {
        if (Globals.IS_MAC) {
            try {
                // Set the dock label
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", "ChAsE");
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                log.error("Error setting apple dock label", e);
                e.printStackTrace();
            }
        }

    }

}
