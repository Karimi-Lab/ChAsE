package org.sfu.chase.input;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FileUtils;
import org.sfu.chase.core.Experiment;
import org.sfu.chase.gui.ColorPalette;
import org.sfu.chase.util.FileDialogUtils;
import org.sfu.chase.util.UpdateManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@SuppressWarnings("serial")
public class InputDialog extends JPanel implements ActionListener, ChangeListener
{ 
    private JTable 	m_TableXP;
    DataModel	   	m_DataModel;
    JTextField 		m_TextRegion;
    JTextField 		m_TextWorkingDir;
    JCheckBox       m_CheckEqualRegionSize;
    JSpinner 		m_SpinnerRegionSize;
    JSpinner 		m_SpinnerBins;
    ProgressDialog	m_ProgressDialog;
    
    Dialog       	m_ModalDialog;
    //boolean 		m_ModalResult;
    private String m_LastAddedExperiment = "";
    
    int				 m_Result = 0;
    public static final int MODIFIED_REGIONS     = 1 << 0;
    public static final int MODIFIED_EXPERIMENTS = 1 << 1;
    
    
    // whether any of the fields are modified and data needs to be processed again
    boolean 		m_bDirty = true; 
    
    public InputDialog() 
    {
        super();
     
        m_ProgressDialog = new ProgressDialog();

        m_DataModel = new DataModel();
        m_DataModel.addPropertyChangeListener(m_ProgressDialog);
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        add(new JLabel(UpdateManager.getToolName()));
        add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.LINE_START);
        add(new JLabel("Continue Previous Analysis:"));
        add(newButton("Open Existing Analysis Directory...", "OPEN", "/resources/icon_folder_open.png", DEFAULT_BUTTON_WIDTH*5/2));
        add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.LINE_START);
        add(new JLabel("Start New Analysis:"));
        add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.LINE_START);
        
        // Experiments Table
        createTableXP();
        JScrollPane scrollTableXP = new JScrollPane(m_TableXP);
        scrollTableXP.setAlignmentY(TOP_ALIGNMENT);
        
        // Experiments Action buttons
        JPanel panelXPButtons = new JPanel();
        panelXPButtons.setLayout(new BoxLayout(panelXPButtons, BoxLayout.Y_AXIS));
        panelXPButtons.setAlignmentY(TOP_ALIGNMENT);
        panelXPButtons.add(newButton("Add File", "ADD_XP", "/resources/icon_add.png"));
        panelXPButtons.add(newButton("Add URL", "ADD_URL", "/resources/icon_add_url.png"));
        panelXPButtons.add(newButton("Remove", "REMOVE_XP", "/resources/icon_delete.png"));
        panelXPButtons.add(newButton("Up", "MOVE_XP_UP", "/resources/icon_up.png"));
        panelXPButtons.add(newButton("Down", "MOVE_XP_DOWN", "/resources/icon_down.png"));

        JPanel panelXP = new JPanel();
        panelXP.setLayout(new BoxLayout(panelXP, BoxLayout.X_AXIS));
        panelXP.setAlignmentX(LEFT_ALIGNMENT);
        panelXP.add(scrollTableXP);
        panelXP.add(panelXPButtons);
        
        // Input regions
        JPanel panelInputRegion = new JPanel();
        panelInputRegion.setLayout(new BoxLayout(panelInputRegion, BoxLayout.X_AXIS));
        panelInputRegion.add(m_TextRegion = new JTextField());
        m_TextRegion.setEditable(false);
        panelInputRegion.add(newButton("Set Region File ...", "SET_REGION", null, DEFAULT_BUTTON_WIDTH*2));
        panelInputRegion.setMaximumSize(new Dimension(Short.MAX_VALUE, 25));
        panelInputRegion.setAlignmentX(LEFT_ALIGNMENT);
        
        // equal region size
        JPanel panelEqualRegions = new JPanel();
        panelEqualRegions.setLayout(new BoxLayout(panelEqualRegions, BoxLayout.Y_AXIS));
        m_CheckEqualRegionSize = new JCheckBox("Equal Region Size", true);
        m_CheckEqualRegionSize.addChangeListener(this);
        panelEqualRegions.add(m_CheckEqualRegionSize);

        // region size
        JPanel panelRegionSize = new JPanel();
        panelRegionSize.setLayout(new BoxLayout(panelRegionSize, BoxLayout.Y_AXIS));
        panelRegionSize.add(new JLabel("Resize All Regions to (bp)"));
        m_SpinnerRegionSize = new JSpinner(new SpinnerNumberModel(1000, 1, 100000, 1));
		m_SpinnerRegionSize.setMaximumSize(new Dimension(150, 25));
		m_SpinnerRegionSize.setAlignmentX(LEFT_ALIGNMENT);
		m_SpinnerRegionSize.addChangeListener(this);
		panelRegionSize.add(m_SpinnerRegionSize);
        
		// num bins
        JPanel panelNumBins = new JPanel();
        panelNumBins.setLayout(new BoxLayout(panelNumBins, BoxLayout.Y_AXIS));
        panelNumBins.add(new JLabel("Number of Bins per Region"));
		m_SpinnerBins = new JSpinner(new SpinnerNumberModel(30, 1, 10000, 1));
		m_SpinnerBins.setMaximumSize(new Dimension(150, 25));
		m_SpinnerBins.setAlignmentX(LEFT_ALIGNMENT);
		m_SpinnerBins.addChangeListener(this);
        panelNumBins.add(m_SpinnerBins);

        // regionsize + numbins
        JPanel panelRegionParams = new JPanel();
        panelRegionParams.setLayout(new BoxLayout(panelRegionParams, BoxLayout.X_AXIS));
        panelRegionParams.add(panelEqualRegions);
        panelRegionParams.add(Box.createRigidArea(new Dimension(10, 0)));
        panelRegionParams.add(panelRegionSize);
        panelRegionParams.add(Box.createRigidArea(new Dimension(50, 0)));
        panelRegionParams.add(panelNumBins);
        panelRegionParams.add(Box.createHorizontalGlue());
        panelRegionParams.setAlignmentX(LEFT_ALIGNMENT);
        panelRegionParams.setMaximumSize(new Dimension(Short.MAX_VALUE, 25));
        
        // Input regions
        JPanel panelWorkingDir = new JPanel();
        panelWorkingDir.setLayout(new BoxLayout(panelWorkingDir, BoxLayout.X_AXIS));
        panelWorkingDir.add(m_TextWorkingDir = new JTextField());
        panelWorkingDir.add(newButton("Set Working Directory ...", "SET_WORKING_DIR", null, DEFAULT_BUTTON_WIDTH * 2));
        panelWorkingDir.setMaximumSize(new Dimension(Short.MAX_VALUE, 25));
        panelWorkingDir.setAlignmentX(LEFT_ALIGNMENT);
        m_TextWorkingDir.setEditable(false);

        // cancel - ok
        JPanel panelBottomButtons = new JPanel();
        panelBottomButtons.setLayout(new BoxLayout(panelBottomButtons, BoxLayout.X_AXIS));
        panelBottomButtons.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        panelBottomButtons.add(Box.createHorizontalGlue());
        panelBottomButtons.add(newButton("Cancel", "CANCEL", null));
        registerKeyboardAction(this, "Cancel", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        panelBottomButtons.add(Box.createRigidArea(new Dimension(10, 0)));
//        panelBottomButtons.add(newButton("Open...", "OPEN", null));
//        panelBottomButtons.add(Box.createRigidArea(new Dimension(10, 0)));
        panelBottomButtons.add(newButton("Ok", "OK", null));
        panelBottomButtons.setAlignmentX(LEFT_ALIGNMENT);

        // Main layout
        add(new JLabel("Epigenetic Marks (WIG or bigWIG)"));
        add(panelXP);
        
        add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.LINE_START);
        add(new JLabel("Regions Set (GFF or BED)"));
        add(panelInputRegion);
        
        add(panelRegionParams);
        
        add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.LINE_START);
        add(new JLabel("Working Directory (to ouput processing results)"));
        add(panelWorkingDir);
        add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.LINE_START);
        
        add(panelBottomButtons);
        
        // create the modal dialog
    	JFrame window = new JFrame();
    	//window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		m_ModalDialog = new Dialog(window, "Input Data", true);
		m_ModalDialog.setLayout( new BoxLayout(m_ModalDialog, BoxLayout.Y_AXIS) );
		m_ModalDialog.add(this);
		m_ModalDialog.pack();
    }
    
    public int showModal()
    {
    	m_Result = 0;
		m_ModalDialog.setLocationRelativeTo(null);
		m_ModalDialog.setVisible(true);
		return m_Result;
    }
    
    public int showOpenDialog()
    {
    	if (openDirectory())
    	{
    		m_ModalDialog.setLocationRelativeTo(null);
    		m_ModalDialog.setVisible(true);
    		return m_Result;
    	}
		m_ModalDialog.setVisible(false);
    	return 0;
    }
    
    public DataModel getDataModel()
    {
    	return m_DataModel;
    }
    
    public String getWorkspaceFilename()
    {
    	return m_DataModel.getParam().getWorkspaceFileName();
    }

    static final int DEFAULT_BUTTON_WIDTH = 100;
    private JButton newButton(String label, String actionCommand, String iconFilename)
    {
    	return newButton(label, actionCommand, iconFilename, DEFAULT_BUTTON_WIDTH);
    }
    
    private JButton newButton(String label, String actionCommand, String iconFilename, int btnWidth)
    {
    	ImageIcon icon = null;
    	if (iconFilename != null) {
    		icon = new ImageIcon(getClass().getResource(iconFilename));
    	}
    	
    	JButton button = null;
    	if (icon != null) {
    		button = new JButton(label, icon);
    		button.setHorizontalAlignment(SwingConstants.LEFT);
    		button.setHorizontalTextPosition(SwingConstants.RIGHT);
    	} else {
    		button = new JButton(label, null);
    	}
    	
    	button.setActionCommand(actionCommand);
    	button.addActionListener(this);
    	Dimension buttonDim = new Dimension(btnWidth, 30);
    	button.setMinimumSize(buttonDim);
    	button.setMaximumSize(buttonDim);
    	button.setPreferredSize(buttonDim);
    	return button;
    }
    
    @Override
    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.equalsIgnoreCase("ADD_XP"))
        {
        	addExperiment();
        }
        if (command.equalsIgnoreCase("ADD_URL"))
        {
            addExperimentURL();
        }
        else if (command.equalsIgnoreCase("REMOVE_XP"))
        {
        	removeExperiment();
        }
        else if (command.equalsIgnoreCase("MOVE_XP_UP") || command.equalsIgnoreCase("MOVE_XP_DOWN"))
        {
        	moveExperiment((command.equalsIgnoreCase("MOVE_XP_UP") ? -1 : 1));
        }
        else if (command.equalsIgnoreCase("SET_REGION"))
        {
        	selectRegionFile();
        }
        else if (command.equalsIgnoreCase("SET_WORKING_DIR"))
        {
        	setWorkingDir();
        }
        else if (command.equalsIgnoreCase("OPEN"))
        {
        	openDirectory();
        }
        else if (command.equalsIgnoreCase("OK"))
        {	
        	try {
	        	if (!m_bDirty || processInput()) 
	        	{
	            	m_ModalDialog.setVisible(false);
	        	}
        	} catch (Exception e) {
				e.printStackTrace();
		    	JOptionPane.showMessageDialog(this, "Error: " + e.toString(), "Error", JOptionPane.OK_OPTION);
        	}
        }
        else if (command.equalsIgnoreCase("CANCEL"))
        {
        	if (m_DataModel.getParam() != null)
        	{
	        	m_DataModel.writeParamsToExperiments();
	    		m_TextRegion.setText(m_DataModel.getParam().getRegionsFileName());
	    		m_CheckEqualRegionSize.setSelected(m_DataModel.getParam().getEqualRegionSize());
	    		m_SpinnerRegionSize.setValue(new Integer(m_DataModel.getParam().getRegionsSize()));
	    		m_SpinnerBins.setValue(new Integer(m_DataModel.getParam().getNumBins()));
        	}
        	
        	m_Result = 0;
        	m_ModalDialog.setVisible(false);
        }
    }
    
	@Override
	public void stateChanged(ChangeEvent e) 
	{
        if (e.getSource() == m_CheckEqualRegionSize)
        {
            m_SpinnerRegionSize.setEnabled(m_CheckEqualRegionSize.isSelected());
            if (!m_CheckEqualRegionSize.isSelected()) {
                m_SpinnerBins.setValue(new Integer(1));
            }
                
            m_bDirty = true;
        }
        else if (e.getSource() == m_SpinnerBins || e.getSource() == m_SpinnerRegionSize)
		{
			m_bDirty = true;
		}
	}
    
    /*
	public void aboutToHidePanel() {
    	// set the region label if any
		m_Param.setRegionsLabel(getFilteredLabelTextField());
			
		// set the analysis directory
		m_Param.setAnalysisDir(m_CurrentAnalysisDirSelection);
		
		// set number of bins
		m_Param.setNumBins(Integer.parseInt(m_BinTextField.getText()));
		
		// set number of clusters
		m_Param.setKvalue(Integer.parseInt(m_ClusterTextField.getText()));
		
		// set the stats type
		String sType = m_StatsOptions.getSelectedItem().toString();
		if (sType.equals("globally")) {
			m_Param.setStatsType("global");
		} else if (sType.equals("regionally")) {
			m_Param.setStatsType("regional");
		}

		// final properties check
		try {
			m_Param.prepareNewAnalysis();
		} catch (IllegalArgumentException ex) {
			MessageUtils.showMessage(ex.getMessage());
			return;
		} catch (IOException ex) {
			MessageUtils.showMessage(ex.getMessage());
			return;
		}

		// Remove old files to ensure the .dat and .stat files of 
		// a 'new analysis' are always recomputed which 
		// distinguishes it from 'open analysis' option.
		// The only exception is Epigenome Atlas .dat and .stat files
		// which are downloaded and not recomputed each time
		if (cleanDir) {
			m_Param.cleanDir(m_Param.getAnalysisDir());
		}

		// notify listeners that about to close this window
		firePropertyChange("finished", false, true);
	}
    */
    
    /**
     * Adds new experiment(s) to the experiments list
     */
    void addExperiment()
    {
        final String[] extensions = {"wig","gz","zip","bigwig","bw"};
        FilenameFilter filter = FileDialogUtils.createIgnoreDirectoriesFilenameFilter(extensions);
        File initialDirectory = null;
        if (!m_LastAddedExperiment.equals("")) {
            initialDirectory = new java.io.File(m_LastAddedExperiment).getParentFile();
        }
        File[] files = FileDialogUtils.chooseMultiple("Open Wig File(s)", initialDirectory, filter);
        if(files != null) {
            for (int i = 0; i < files.length; i++)
			{
				m_DataModel.getExperiments().add(new Experiment(true, files[i].getName(), files[i].getAbsolutePath(), 1, 1, 0, false, "blue"));
				if (i == 0) {
					m_LastAddedExperiment = files[i].getAbsolutePath();
				}
			}
            m_TableXP.updateUI();
        	m_bDirty = true;
		}
    }
    
    void addExperimentURL()
    {
        String urlString = (String)JOptionPane.showInputDialog (
                this, "URL:\n", "Add URL", JOptionPane.PLAIN_MESSAGE, null, null,
                "ftp://");
        if (urlString != null)
        {
            try {
                String fName = null;
                // try to get name from URL string
                URL url = new URL(urlString);
                int slashIndex = url.getPath().lastIndexOf('/');
                fName = url.getPath().substring(slashIndex + 1);
                
                m_DataModel.getExperiments().add(new Experiment(true, fName, urlString, 1, 1, 0, false, "blue"));
                m_TableXP.updateUI();
                m_bDirty = true;
            } catch (MalformedURLException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.toString(), "Error", JOptionPane.OK_OPTION);
            }
            
            
        }

    }
    
    /**
     * removes the currently selected experiment 
     */
    void removeExperiment()
    {
		int selectedRow = m_TableXP.getSelectedRow();
    	if (selectedRow != -1 && selectedRow < m_TableXP.getRowCount())
    	{
    		m_DataModel.getExperiments().remove(selectedRow);
        	if (selectedRow >= m_TableXP.getRowCount()) {
        		m_TableXP.getSelectionModel().setSelectionInterval(selectedRow - 1, selectedRow - 1);
        	}
        	m_TableXP.updateUI();
        	m_bDirty = true;
    	}
    }
    
    /**
     * moves the currently selected experiment up or down by offset
     * @param offset
     */
    void moveExperiment(int offset) 
    {
    	int currIndex = m_TableXP.getSelectedRow();
    	int nextIndex = currIndex + offset;
    	
    	if (currIndex >= 0 && nextIndex >= 0 && nextIndex < m_TableXP.getRowCount())
    	{
    		Experiment next = m_DataModel.getExperiments().get(nextIndex);
    		m_DataModel.getExperiments().set(nextIndex, m_DataModel.getExperiments().get(currIndex));
    		m_DataModel.getExperiments().set(currIndex, next);
    		m_TableXP.getSelectionModel().setSelectionInterval(nextIndex, nextIndex);
            m_TableXP.updateUI();
        	m_bDirty = true;
    	}
    }
    
    /**
     * Opens a file dialog to select a region (gff) file
     */
    void selectRegionFile()
    {
        final String[] extensions = {"gff", "bed"};
        File initialFile = null;
        File initialDirectory = null;
        if (!m_TextRegion.getText().equals("")) {
            initialFile = new java.io.File(m_TextRegion.getText());
            initialDirectory = initialFile.getParentFile();
        }
        File selectedFile = FileDialogUtils.chooseFile("Open Input Region", initialDirectory, initialFile, extensions, FileDialog.LOAD);
        if(selectedFile != null) 
        {
			try
			{
				String path = selectedFile.getCanonicalPath();
				m_ProgressDialog.setIndeterminate(true);
				if (m_ProgressDialog.runTask(m_DataModel.getWorkerReadRegions(path)))
				{
					m_TextRegion.setText(path);
					m_Result |= MODIFIED_REGIONS;
					m_bDirty = true;
					m_CheckEqualRegionSize.setSelected(true);
					m_SpinnerRegionSize.setEnabled(true);
					m_SpinnerRegionSize.setValue(new Integer(m_DataModel.getMaxRegionSize()));
				}
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
			m_ProgressDialog.setIndeterminate(false);
		}
    }
    
    /**
     * Opens the file dialog to specify the working directory
     */
    void setWorkingDir()
    {
        File initialDir = null;
        if (!m_TextWorkingDir.getText().equals("")) {
            initialDir = new java.io.File(m_TextWorkingDir.getText()).getParentFile();
        }
        File selectedFile = FileDialogUtils.chooseDirectory("Set Working Directory", initialDir);
		if (selectedFile != null)
		{
			try {
				if (!m_TextWorkingDir.getText().equals(selectedFile.getCanonicalPath()))
				{
					m_bDirty = true;
					m_TextWorkingDir.setText(selectedFile.getCanonicalPath());
				}
			} catch (IOException e) {
				e.printStackTrace();
		    	JOptionPane.showMessageDialog(this, "Error: " + e.toString(), "Error", JOptionPane.OK_OPTION);
			}
		}
    }
    
    /**
     * Opens an existing working directory
     * @return true if successfull
     */
    public boolean openDirectory()
    {
        File initialDir = null;
        if (!m_TextWorkingDir.getText().equals("")) {
            initialDir = new java.io.File(m_TextWorkingDir.getText()).getParentFile();
        }
        File selectedFile = FileDialogUtils.chooseDirectory("Open Working Directory", initialDir);
        try {
            if (selectedFile != null) {
				if (m_ProgressDialog.runTask(m_DataModel.getWorkerLoadData(selectedFile)))
				{
					m_TextWorkingDir.setText(selectedFile.getCanonicalPath());
					
					m_TextRegion.setText(m_DataModel.getParam().getRegionsFileName());
					
                    m_CheckEqualRegionSize.setSelected(m_DataModel.getParam().getEqualRegionSize());
                    int iRegionSize = m_DataModel.getParam().getRegionsSize();
					iRegionSize = iRegionSize < 0 ? m_DataModel.getMaxRegionSize() : iRegionSize;
					m_SpinnerRegionSize.setValue(iRegionSize);
                    m_SpinnerRegionSize.setEnabled(m_CheckEqualRegionSize.isSelected());
					
					m_SpinnerBins.setValue(new Integer(m_DataModel.getParam().getNumBins()));
					m_TableXP.updateUI();
					m_bDirty = false;
					m_Result = 0xFF; // everything can be modified
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
	    	JOptionPane.showMessageDialog(this, "Error: " + e.toString(), "Error", JOptionPane.OK_OPTION);
		}
		return false;
    }
    
    /**
     * Process the input experiments and input regions and save the preprocesed files.
     * @return true if successfull
     */
    boolean processInput()
    {
    	String sWorkingDir = getWorkingDir();
    	if (sWorkingDir.isEmpty())
    	{
			JFileChooser fc = new JFileChooser(); 
		    fc.setDialogTitle("Specify Working Directory");
		    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if( fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION )
			{
				sWorkingDir = fc.getSelectedFile().getAbsolutePath();
				m_TextWorkingDir.setText(sWorkingDir);
			}
			else
			{// user cancelled
				return false;
			}
    	}
    	
    	boolean bRegionParamsUnchanged = false;
    	if (m_DataModel.getParam() != null) {
    	    bRegionParamsUnchanged =
    	           (m_DataModel.getParam().getRegionsFileName().equals(m_TextRegion.getText()))
    	        && (m_DataModel.getParam().getNumBins() == (Integer)m_SpinnerBins.getValue())
    	        && (m_DataModel.getParam().getEqualRegionSize() == m_CheckEqualRegionSize.isSelected())
    	        && (m_DataModel.getParam().getRegionsSize() == (Integer)m_SpinnerRegionSize.getValue());
    	}
    	
    	
        File fWorkingDir = new File(sWorkingDir);
        if (fWorkingDir.exists() && !bRegionParamsUnchanged) 
        {
            if (JOptionPane.showConfirmDialog(this
                    , "All contents of the working directory\n" + sWorkingDir + "\nwill be removed.\nDo you want to proceed?" 
                    , "Confirm Directory Remove", JOptionPane.YES_NO_OPTION) == 0) 
            {
                try {
                    FileUtils.deleteDirectory(new File(sWorkingDir + "/tables"));
                    FileUtils.deleteDirectory(new File(sWorkingDir + "/stats"));
                    FileUtils.deleteDirectory(new File(sWorkingDir + "/clusters"));
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            } else {
                return false;
            }
        }
    	
    	if (!fWorkingDir.exists()) {
    		if (!fWorkingDir.mkdir()) {
    	    	JOptionPane.showMessageDialog(this, "Unable to create output directory: " + sWorkingDir, "Error", JOptionPane.OK_OPTION);
    			return false;
    		}
    	}
    	
		m_Result |= MODIFIED_EXPERIMENTS;
		m_DataModel.writeExperimentsToParams();
    	m_DataModel.getParam().setAnalysisDir(fWorkingDir);
    	m_DataModel.getParam().setRegionsFileName(m_TextRegion.getText());
    	m_DataModel.getParam().setNumBins((Integer)m_SpinnerBins.getValue());
    	m_DataModel.getParam().setEqualRegionSize(m_CheckEqualRegionSize.isSelected());
    	m_DataModel.getParam().setRegionsSize((Integer)m_SpinnerRegionSize.getValue());

    	try {
	    	if (m_ProgressDialog.runTask(m_DataModel.getWorkerProcessData())) {
	    		m_bDirty = false;
	    		return true;
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
	    	JOptionPane.showMessageDialog(this, "Error: " + e.toString(), "Error", JOptionPane.OK_OPTION);
    	}
    	
    	return false;
    }
    
    /**
     * returns the current working directory
     * @return
     */
    private String getWorkingDir()
    {
    	return m_TextWorkingDir.getText();
    }
    
	/**
	 * Creates the table to hold input experiments
	 */
    private void createTableXP()
    {
        m_TableXP = new JTable(new TableXPModel());
        m_TableXP.setPreferredScrollableViewportSize(new Dimension(600, 150));
        m_TableXP.setFillsViewportHeight(true);
        //m_TableXP.getSelectionModel().addListSelectionListener(new RowListener());
        //m_TableXP.getColumnModel().getSelectionModel().addListSelectionListener(new ColumnListener());
        m_TableXP.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        //Set up renderer and editor for the Color column.
        m_TableXP.setDefaultRenderer(Color.class, new ColorRenderer(true));
        m_TableXP.setDefaultEditor(Color.class, new ColorEditor());
        
        m_TableXP.getColumnModel().getColumn(Columns.FILE_NAME.ordinal()).setCellRenderer(new TableCellStringRenderer());
        m_TableXP.getColumnModel().getColumn(Columns.SAMPLE_NAME.ordinal()).setCellRenderer(new TableCellStringRenderer());

        JComboBox comboBoxNorm = new JComboBox();
        comboBoxNorm.addItem(Experiment.NORM_STRINGS[0]);
        comboBoxNorm.addItem(Experiment.NORM_STRINGS[1]);
        m_TableXP.getColumnModel().getColumn(Columns.NORM_TYPE.ordinal()).setCellEditor(new DefaultCellEditor(comboBoxNorm));

        JComboBox comboBoxStat = new JComboBox();
        comboBoxStat.addItem(Experiment.STAT_STRINGS[0]);
        comboBoxStat.addItem(Experiment.STAT_STRINGS[1]);
        m_TableXP.getColumnModel().getColumn(Columns.STAT_TYPE.ordinal()).setCellEditor(new DefaultCellEditor(comboBoxStat));

        JComboBox comboBoxBinning = new JComboBox();
        comboBoxBinning.addItem(Experiment.BIN_STRINGS[0]);
        comboBoxBinning.addItem(Experiment.BIN_STRINGS[1]);
        m_TableXP.getColumnModel().getColumn(Columns.BIN_TYPE.ordinal()).setCellEditor(new DefaultCellEditor(comboBoxBinning));
        
        JComboBox comboBoxColors = new JComboBox();
        for (int i = 0; i < ColorPalette.COLOR_NAMES.length; i++) {
        	comboBoxColors.addItem(ColorPalette.COLOR_NAMES[i]);
        	comboBoxColors.setRenderer(new ColorCellRenderer());
        }
        m_TableXP.getColumnModel().getColumn(Columns.COLOR.ordinal()).setCellEditor(new DefaultCellEditor(comboBoxColors));
        m_TableXP.getColumnModel().getColumn(Columns.COLOR.ordinal()).setCellRenderer(new ColorRenderer(true));
        
        for (int i = 0; i < s_PreferredColumnWidths.length; i++) {
        	m_TableXP.getColumnModel().getColumn(i).setPreferredWidth(s_PreferredColumnWidths[i]);
        }
    }

    /**
     * Renderer with tool tip for string elements
     */
    private class TableCellStringRenderer extends JLabel implements TableCellRenderer {
    	public Component getTableCellRendererComponent(JTable table, Object str, boolean isSelected, boolean hasFocus, int row, int column) {
    		setHorizontalTextPosition(SwingConstants.RIGHT);
    		setText(str.toString());
    		setToolTipText(str.toString());
    		setOpaque(true);
    		setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
    		setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
    		return this;
    	}
    }


    /**
     * columns for the input experiments 
     */
	enum Columns {
		VISIBLE,
		SAMPLE_NAME,
		NORM_TYPE,
		STAT_TYPE,
		BIN_TYPE,
		LOG_SCALE,
		COLOR,
		FILE_NAME,
	}
	
	/**
	 * name of the input experiments columns
	 */
    private static final String[] s_ColumnNames = {
    	"Visible",
        "Label",
        "Normalization",
        "Stats",
        "Binning",
        "Log",
        "Color",
        "Filename",
    };
    
    private static final int[] s_PreferredColumnWidths = {50, 100, 75, 75, 75, 50, 50, 1};
    
    class TableXPModel extends AbstractTableModel 
    {
        public int getColumnCount() {
        	return s_ColumnNames.length;
        }

        public int getRowCount() {
            return m_DataModel.getExperiments().size();
        }

        public String getColumnName(int col) {
            return s_ColumnNames[col];
        }

        public Object getValueAt(int row, int col) {
        	Experiment xp = m_DataModel.getExperiment(row);
        	if (xp == null)
        		return null;
        	
        	switch(Columns.values()[col]) {
	    		case VISIBLE:     return xp.isVisible();
	    		case SAMPLE_NAME: return xp.getName();
	    		case FILE_NAME:   return xp.getFilename();
	    		case NORM_TYPE:   return xp.getNormType();
	    		case STAT_TYPE:   return xp.getStatType();
	    		case BIN_TYPE:    return xp.getBinningType();
	    		case LOG_SCALE:   return xp.isLogScale();
	    		case COLOR:       return xp.getColor().getColorString();
	    	}
	    	return "";
        }

        @SuppressWarnings("unchecked")
		public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public boolean isCellEditable(int row, int col) {
        	//return m_TableXP.getSelectionModel().isSelectedIndex(row) &&
        	return col != Columns.FILE_NAME.ordinal();
        }

        public void setValueAt(Object value, int row, int col) {
        	Experiment xp = m_DataModel.getExperiments().get(row);
        	if (xp == null)
        		return;

        	switch(Columns.values()[col]) {
	    		case VISIBLE:     xp.setVisible((Boolean)value); break;
	    		case SAMPLE_NAME: xp.setName((String)value); break;
	    		case FILE_NAME:   xp.setFilename((String)value); break;
	    		case NORM_TYPE:   xp.setNormType((String)value); break;
	    		case STAT_TYPE:   xp.setStatType((String)value); break;
	    		case BIN_TYPE:    xp.setBinningType((String)value); break;
	    		case LOG_SCALE:   xp.setLogScale((Boolean)value); break;
	    		case COLOR:       xp.getColor().setColor((String)value); break;
        	}
        	m_bDirty = true;
            fireTableCellUpdated(row, col);
        }
    }
    
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    static void createAndShowGUI() {
        //Disable boldface controls.
        UIManager.put("swing.boldMetal", Boolean.TRUE); 

        //Create and set up the window.
        JFrame frame = new JFrame("Input");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        InputDialog newContentPane = new InputDialog();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	InputDialog dlg = new InputDialog();
            	dlg.showModal();
                //createAndShowGUI();
            }
        });
    }
}
