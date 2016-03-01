package chase;

import java.io.File;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Enumeration;

import javax.swing.JEditorPane;
import javax.swing.JOptionPane;

public class UpdateManager {
    
    static final String[] systemProperties = {
        "java.version",      //The version of Java Runtime Environment.
        "java.vendor",       //      The name of Java Runtime Environment vendor
        "java.vendor.url",//    The URL of Java vendor
        "java.home",//   The directory of Java installation 
        "java.vm.specification.version",//   The specification version of Java Virtual Machine
        "java.vm.specification.vendor",//    The name of specification vendor of Java Virtual Machine 
        "java.vm.specification.name",//  Java Virtual Machine specification name
        "java.vm.version",//     JVM implementation version
        "java.vm.vendor",//  JVM implementation vendor
        "java.vm.name",//    JVM  implementation name
        "java.specification.version",//  The name of specification version Java Runtime Environment 
        "java.specification.vendor",//    JRE specification vendor
        "java.specification.name",//     JREspecification name
        "java.class.version",//  Java class format version number
        "java.library.path",//   List of paths to search when loading libraries
        "java.io.tmpdir",//  The path of temp file
        "java.compiler",//   The Name of JIT compiler to use
        "java.ext.dirs",//   The path of extension directory or directories
        "os.name",//     The name of OS name
        "os.arch",//     The OS architecture
        "os.version"//  The version of OS
    };
    
    public static void main(String[] args) {
        checkForUpdates();
    }
    
    public static int getBuildNumber() {
        return 121110;
    }

    public static String getIDStr() {
        return "github";
    }

    public static String getToolName() {
        return "ChAsE (v 1.0.12)";
    }

    public static void checkForUpdates()
    {
        int MB =1048576;
        String allProperties = "";
        allProperties += "build=" + getBuildNumber();
        allProperties += "&tool=" + getToolName();
        allProperties += "&idstr=" + getIDStr();
        allProperties += "&msg=";
        allProperties += "Available processors (cores): " + Runtime.getRuntime().availableProcessors() + "<br>\n";
        allProperties += "Free memory: " + (Runtime.getRuntime().freeMemory() / MB) + "<br>\n";
        long maxMemory = Runtime.getRuntime().maxMemory();
        allProperties += "Maximum memory: " + (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory / MB) + "<br>\n";
        allProperties += "Total memory available to JVM: " + (Runtime.getRuntime().totalMemory() / MB) + "<br>\n";
        for (int i = 0; i < systemProperties.length; ++i) {
            allProperties += systemProperties[i] + ": " + System.getProperty(systemProperties[i]) + "<br>\n";
        }
        
        /* Get a list of all filesystem roots on this system */
        File[] roots = File.listRoots();
        /* For each filesystem root, print some info */
        for (File root : roots) {
            allProperties += "File system root: " + root.getAbsolutePath() + "<br>\n";
            allProperties += "Total space: " + (root.getTotalSpace() / MB) + "<br>\n";
            allProperties += "Free space: " + (root.getFreeSpace() / MB) + "<br>\n";
            allProperties += "Usable space: " + (root.getUsableSpace() / MB) + "<br>\n";
        }
        
        //System.out.println(allProperties);
        try {
            System.out.println("Checking for updates");
            String response = sendPost(allProperties);
            //System.out.println(response);
            
            String[] tokens = response.split("\n");
            boolean readNextLine = false;
            boolean readBuildURL = false;
            String buildURL = "";
            int latestBuildNo = -1;
            for (String token: tokens) {
                if (readBuildURL) {
                    buildURL = token;
                    break;
                } else if (readNextLine) {
                    latestBuildNo = Integer.parseInt(token);
                    readBuildURL = true;
                }
                else if (token.startsWith("Latest Build:")) {
                    readNextLine = true;
                }
            }
            
            if (latestBuildNo == -1) {
                throw new Exception("Unable to connect to update server.");
            }
            
            if (latestBuildNo ==  666) {
                int mc = JOptionPane.ERROR_MESSAGE; //WARNING_MESSAGE;
                JOptionPane.showMessageDialog (null, "Unregistered Vesion.", "Error", mc);
                System.exit(0);
            }
            
            //System.out.println("Latest Build: " + buildURL);
            if (latestBuildNo > getBuildNumber()) {
                System.out.println("New version available");
                JEditorPane ep = new JEditorPane("text/html", "<html>"
                        + "Please download the new version: <br> <br>  <a href=\"" + buildURL + "\">" + buildURL + "</a> <br>"
                        + "</html>");
                /*
                // handle link events
                ep.addHyperlinkListener(new HyperlinkListener()
                {
                    @Override
                    public void hyperlinkUpdate(HyperlinkEvent e)
                    {
                        if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
                            ProcessHandler.launchUrl(e.getURL().toString()); // roll your own link launcher or use Desktop if J6+
                    }
                });*/
                ep.setEditable(false);
                //ep.setBackground(label.getBackground());
                int mc = JOptionPane.INFORMATION_MESSAGE;
                JOptionPane.showMessageDialog(null, ep, "New version available. Please download the new version from the ChAsE webpage.", mc);
                
                
                //JOptionPane.showMessageDialog (null, "<html>Please download the new version: <a href=\"" + buildURL + "\">" + buildURL + "</a></html>"
                //        , "New version available", mc);
                //System.exit(0);
            } else {
                System.out.println("Up to date.");
            }
            
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            System.out.println(e.toString());
            
            int mc = JOptionPane.INFORMATION_MESSAGE; //WARNING_MESSAGE;
            JOptionPane.showMessageDialog (null, "Unable to connect to update server.", "Warning", mc);
            //System.exit(0);
        }
    }

    private static String sendPost(String message) throws Exception 
    {
        String url = "http://genecalc.t15.org/chase_update.php";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
 
        //add reuqest header
        final String USER_AGENT = "[JVM]";
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
 
        //String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";
 
        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(message);
        wr.flush();
        wr.close();
 
        int responseCode = con.getResponseCode();
        //System.out.println("\nSending 'POST' request to URL : " + url);
        //System.out.println("Post parameters : " + urlParameters);
        System.out.println("Response Code : " + responseCode);
        StringBuffer response = new StringBuffer();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine + "\n");
            //System.out.println(inputLine);
        }
        in.close();
        return response.toString();
    }    
}
