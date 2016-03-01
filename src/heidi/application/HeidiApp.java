/*
 * Main Heidi Application class
 * Initialize main window and menus
 */

package heidi.application;

import heidi.project.Project;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import prefuse.data.io.DataIOException;
import prefuse.util.ui.UILib;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class HeidiApp extends JFrame {

	private Project     m_project;
	private JPanel      m_projectPanel;
	private ProjectView m_projectControl;
	
	private JMenuBar  m_menuBar;
	
	private JMenu     m_projectMenu;
	private JMenuItem m_newProject;
	private JMenuItem m_openProject;
	private JMenuItem m_saveProject;
	private JMenuItem m_saveProjectAs;
	private JMenuItem m_projectProperties;
	private JMenuItem m_exit;
	
	private JMenu     m_groupMenu;
	private JMenuItem m_addGroup;
	private JMenuItem m_removeGroup;
	private JMenuItem m_groupProperties;

	private JMenu     m_dimMenu;
	private JMenuItem m_dimProperties;
	
	private JMenu     m_plotMenu;
	private JMenuItem m_addPlot;
	private JMenuItem m_removePlot;
	private JMenuItem m_plotProperties;
	
	private JMenu     m_helpMenu;
	private JMenuItem m_aboutMenuItem;
	private JMenuItem m_contentsMenuItem;
	
	private static HeidiApp s_Heidi;
	
	private static final long serialVersionUID = -8736334472550575119L;

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
	
		if (s_Heidi != null) {
			System.out.println("There is already a Heidi Application running.  Please shut down the current application before launching another one.");
			return;
		}
		
		// Prefuse logging is too verbose and is affecting performance
		// Scale the logging output back
		Logger prefuseLogger = Logger.getLogger("prefuse.data.expression.parser.ExpressionParser");
		prefuseLogger.setLevel(Level.SEVERE);
		
		s_Heidi = new HeidiApp();
		if (args.length > 0) {
			String fileName = args[0];
			File file = new File(fileName);
			s_Heidi.openProject(file);
		}
		s_Heidi.setVisible(true);
	}
	
	public static HeidiApp getHeidiApp() {
		return s_Heidi;
	}
	
	private HeidiApp() {
		super("Heidi");
		UILib.setPlatformLookAndFeel();
		createContent();
		createMenuBar();
		// TODO Prompt for saving when closing with the "X" button
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);		
		pack();
	}

	public Project getProject() {
		return m_project;
	}
	
	private void createContent() {
		// This first panel is used to facilitate re-parenting when plots or views are detached.
		// It is not really needed when all plots and views are docked.
		m_projectPanel = new JPanel();
		m_projectPanel.setBorder(BorderFactory.createEmptyBorder());
		
		Container contentPane = getContentPane();
		// TODO - remember the previous application size
		contentPane.setPreferredSize(new Dimension((int)(2.0*1.1618*600.0), 600));

		// layout
		GridBagLayout layout = new GridBagLayout();
		contentPane.setLayout(layout);
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		constraints.fill = GridBagConstraints.BOTH;
		contentPane.add(m_projectPanel, constraints);
	}
	
	private void createMenuBar() {
		// Menu Bar
		m_menuBar = new JMenuBar();
		setJMenuBar(m_menuBar);
		
		// Project menu
		m_projectMenu = new JMenu();
		m_projectMenu.setText("Project");
		m_menuBar.add(m_projectMenu);
		
		// Project -> New
		m_newProject = new JMenuItem();
		m_newProject.setText("New Project");
		m_newProject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				newProject();
			}
		});
		m_projectMenu.add(m_newProject);
		
		// Project -> Open
		m_openProject = new JMenuItem();
		m_openProject.setText("Open Project");
		m_openProject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				openProject();
			}
		});
		m_projectMenu.add(m_openProject);
		
		// Project -> Save
		m_saveProject = new JMenuItem();
		m_saveProject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		m_saveProject.setText("Save Project");
		m_saveProject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				saveProject(false);
			}
		});
		m_projectMenu.add(m_saveProject);
		
		// File -> Save As
		m_saveProjectAs = new JMenuItem();
		m_saveProjectAs.setText("Save Project As ...");
		m_saveProjectAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				saveProject(true);
			}
		});
		m_projectMenu.add(m_saveProjectAs);
		
		// Project -> Properties
		m_projectProperties = new JMenuItem();
		m_projectProperties.setText("Project Properties");
		m_projectProperties.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				showProjectProperties();
			}
		});
		m_projectMenu.add(m_projectProperties);
		
		// Project -> Exit
		m_exit = new JMenuItem();
		m_exit.setText("Exit");
		m_exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				exitApplication();
			}
		});
		m_projectMenu.add(m_exit);
		
		// TODO - add support for most recent files
		
		// Group Menu
		m_groupMenu = new JMenu();
		m_groupMenu.setText("Group");
		m_menuBar.add(m_groupMenu);
		
		// Group -> Add
		m_addGroup = new JMenuItem();
		m_addGroup.setText("Add Group");
		m_addGroup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				addGroup();
			}
		});
		m_groupMenu.add(m_addGroup);
		
		// Group -> Remove
		m_removeGroup = new JMenuItem();
		m_removeGroup.setText("Remove Group");
		m_removeGroup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				// TODO implement remove group
			}
		});
		m_groupMenu.add(m_removeGroup);
		
		// Group -> Properties
		m_groupProperties = new JMenuItem();
		m_groupProperties.setText("Group Properties");
		m_groupProperties.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				showGroupProperties();
			}
		});
		m_groupMenu.add(m_groupProperties);
		
		// Dimension Menu
		m_dimMenu = new JMenu();
		m_dimMenu.setText("Dimension");
		m_menuBar.add(m_dimMenu);
		
		// Dimension -> Properties
		m_dimProperties = new JMenuItem();
		m_dimProperties.setText("Dimension Properties");
		m_dimProperties.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				showDimProperties();
			}
		});
		m_dimMenu.add(m_dimProperties);
		
		// Plot Menu
		m_plotMenu = new JMenu();
		m_plotMenu.setText("Plot");
		m_menuBar.add(m_plotMenu);
		
		// Plot -> Add
		m_addPlot = new JMenuItem();
		m_addPlot.setText("Add Plot");
		m_addPlot.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				addPlot();
			}
		});
		m_plotMenu.add(m_addPlot);
		
		// Plot -> Remove
		m_removePlot = new JMenuItem();
		m_removePlot.setText("Remove Plot");
		m_removePlot.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				// TODO - implement remove plot
			}
		});
		m_plotMenu.add(m_removePlot);
		
		// Plot -> Properties
		m_plotProperties = new JMenuItem();
		m_plotProperties.setText("Plot Properties");
		m_plotProperties.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				showPlotProperties();
			}
		});
		m_plotMenu.add(m_plotProperties);
		
		
		// Help Menu
		m_helpMenu = new JMenu();
		m_helpMenu.setText("Help");
		m_menuBar.add(m_helpMenu);
		
		// Help -> Contents
		m_contentsMenuItem = new JMenuItem();
		m_contentsMenuItem.setText("Contents");
		m_helpMenu.add(m_contentsMenuItem);
		
		// Help -> About
		m_aboutMenuItem = new JMenuItem();
		m_aboutMenuItem.setText("About");
		m_helpMenu.add(m_aboutMenuItem);

	}
	
	private void addGroup() {
		if (m_projectControl == null) {
			String title = "No project selected";
			String message = "You must create a new project or open an existing project before you can add a group.";
			JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		m_projectControl.addGroup(this);
	}
	
	private void addPlot() {
		if (m_projectControl == null) {
			String title = "No project selected";
			String message = "You must create a new project or open an existing project before you can add a plot.";
			JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		m_projectControl.addPlot(this);
	}
	
	private boolean closeProject() {
		if (m_project == null) {
			return true;
		}
		
		if (m_project.isDirty()) {
			int choice = promptForSave(m_project);
			if (choice == JOptionPane.CANCEL_OPTION) {
				// continue with current project
				return false;
			}
			
			if (choice == JOptionPane.YES_OPTION) {
				if (!saveProject(false)) {
					return false;
				}
			}
		}
		m_projectPanel.removeAll();
		m_projectPanel.setLayout(new GridBagLayout());
		m_projectPanel.revalidate();
		
		m_projectControl = null;
		m_project = null;
		
		setTitle("");
		return true;
	}
	
	private void exitApplication() {
		if (closeProject()) {
			System.exit(0);
		}
	}
	
	private Document loadDocument(File file) throws DataIOException {
		Document dom = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = factory.newDocumentBuilder();
			dom = db.parse(file.getAbsolutePath());
		} catch (ParserConfigurationException pce) {
			throw new DataIOException(pce.getCause());
		} catch (SAXException se) {
			throw new DataIOException(se.getCause());
		} catch (IOException ioe) {
			throw new DataIOException(ioe.getCause());
		}
		return dom;
	}
	
	private void newProject() {
		
		if (!closeProject()) {
			// do not proceed with opening a new project
			return;
		}
		
		m_project = new Project();

		m_projectControl = new ProjectView(m_project);
		
		GridBagLayout layout = new GridBagLayout();
		m_projectPanel.setLayout(layout);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		constraints.fill = GridBagConstraints.BOTH;
		m_projectPanel.add(m_projectControl, constraints);
		
		m_projectPanel.revalidate();
		
		m_projectControl.showProjectProperties(this);
		
		setTitle(m_project.getName());
	}
	
	private void openProject() {
		
		if (!closeProject()) {
			// do not proceed with opening a new project
			return;
		}
		
		// Dialog to select a project file
		JFileChooser fileChooser = new JFileChooser();
		FileFilter filter = new FileNameExtensionFilter("Project File", "project");
		fileChooser.setFileFilter(filter);
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setName("Open Project file");
		
	    if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
	    	return;
	    }
		File file = fileChooser.getSelectedFile();
		openProject(file);
	}
	
	private void openProject(File file) {
	
		if (!closeProject()) {
			// do not proceed with opening a new project
			return;
		}

		startProgress();
		try {
			// initialize DOM
			Document dom = loadDocument(file);	
			if (dom == null) {
				throw new DataIOException();
			}
			// Create project from DOM
			Element rootElement = dom.getDocumentElement();
			m_project = Project.CreateFromDom(file, rootElement);
			setTitle(m_project.getName());
			
		} catch (DataIOException de) {
			String title = "Unable to load project";
			String message = "An error was encountered loading the project file "+file.getAbsolutePath()+".  Please see error log for details.";
			JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
			return;
		} finally {
			endProgress();
		}
		
		m_projectControl = new ProjectView(m_project);
		
		// layout
		GridBagLayout layout = new GridBagLayout();
		m_projectPanel.setLayout(layout);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		constraints.fill = GridBagConstraints.BOTH;
		m_projectPanel.add(m_projectControl, constraints);
		
		m_projectPanel.revalidate();
	}
	
	private int  promptForSave(Project project) {
		String prompt = "The project \""+project.getName()+"\" has unsaved changes.\nDo you wish to save the changes before proceeding?";
		return JOptionPane.showConfirmDialog(this, prompt);
	}
	
	private boolean saveProject(boolean promptForFile) {
		if (m_project == null) {
			return true;
		}
		
		File file = m_project.getFile();
		if (promptForFile || file == null) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			FileFilter filter = new FileNameExtensionFilter("Project File", "project");
			fileChooser.setFileFilter(filter);
			fileChooser.setAcceptAllFileFilterUsed(false);
			fileChooser.setMultiSelectionEnabled(false);
			fileChooser.setName("Save Project to file");
			
			if (file != null) {
				fileChooser.setCurrentDirectory(file);
			}
			
			int result = fileChooser.showSaveDialog(this);
			if (result != JFileChooser.APPROVE_OPTION) {
				return false;
			}
			file = fileChooser.getSelectedFile();
			String path = file.getAbsolutePath(); 
			if (!path.endsWith(".project")) {
				file = new File(path+".project");
			}
			m_project.setFile(file);
		}
		
		// Create DOM for project
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			//get an instance of builder
			DocumentBuilder db = factory.newDocumentBuilder();

			//create an instance of DOM
			Document dom = db.newDocument();
			
			//create the root element 
			Element rootElement = dom.createElement("root");
			dom.appendChild(rootElement);

			// create all of the child elements
			m_project.save(dom, rootElement);
			
			// write to file
			OutputFormat format = new OutputFormat(dom);
			format.setIndenting(true);

			FileOutputStream output = new FileOutputStream(file);
			XMLSerializer serializer = new XMLSerializer(output, format);

			serializer.serialize(dom);
			
			output.close();


		} catch(ParserConfigurationException ex) {
			//TODO log error
			return false;
		} catch(IOException ie) {
			//TODO log error
			return false;
		}

		return true;
	}
	
	private void showProjectProperties() {
		if (m_projectControl == null) {
			String title = "No project selected";
			String message = "You must create a new project or open an existing project before you can view or edit the project properties.";
			JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		m_projectControl.showProjectProperties(this);

	}
	
	private void showDimProperties() {
		if (m_project == null) {
			String title = "No project selected";
			String message = "You must create a new project or open an existing project before you can view or edit the dimension properties.";
			JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		m_projectControl.showDimProperties(this);
	}
	
	private void showGroupProperties() {
		if (m_project == null) {
			String title = "No project selected";
			String message = "You must create a new project or open an existing project before you can view or edit the group properties.";
			JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		m_projectControl.showGroupProperties(this);
	}
	
	private void showPlotProperties() {
		if (m_project == null) {
			String title = "No project selected";
			String message = "You must create a new project or open an existing project before you can view or edit the plot properties.";
			JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		m_projectControl.showPlotProperties(this);
	}
	
	private void startProgress() {
		Container contentPane = getContentPane();
		contentPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}
	
	private void endProgress() {
		Container contentPane = getContentPane();
		contentPane.setCursor(null);
	}
}