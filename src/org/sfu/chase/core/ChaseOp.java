package org.sfu.chase.core;

import java.awt.event.ActionEvent;
import java.io.File;

import org.sfu.chase.core.ClustFramework;
import org.sfu.chase.gui.PChasePainter;
import org.sfu.chase.input.DataModel;
import org.sfu.chase.input.InputDialog;
import org.sfu.chase.util.UpdateManager;

import still.data.MemoryTable;
import still.data.Operator;
import still.data.Table;
import still.data.TableFactory;
import still.operators.BasicOp;

public class ChaseOp extends BasicOp
{
	private static final long serialVersionUID = 5118434778904392340L;
	
	public ClustFramework m_Framework;
	private static String m_gffFilePath;// = "/Users/hyounesy/SFU/Research/BioVis/data/Brad/Allenhancers_nopromok4me3.gff";
	// "/Users/hyounesy/SFU/research/BioVis/Brad/Allenhancers_nopromok4me3.gff"; //SFU
	// "/Users/hyounesy/_wig/tss_hg19_+-3000_noNeighbors.gff"
	private static int m_NumGroups = 19;
	
	static InputDialog m_InputDialog;
	
	public ChaseOp( Table newTable, boolean isActive, String paramString )
	{
		this( newTable, isActive );
	}
	
	public ChaseOp( Table newTable, boolean isActive )
	{
		super(newTable, isActive);
		m_Framework = new ClustFramework();
		m_Framework.setTable(input, m_NumGroups);
    	m_Framework.readRegions(m_gffFilePath);
		loadOperatorView();
	}
	
	public ChaseOp(DataModel dataModel)
	{
		super(new MemoryTable(dataModel.getDataTable().getData(), null), true);
		m_Framework = new ClustFramework();
    	m_Framework.setTable(dataModel, true);
    	if ((new File(m_InputDialog.getWorkspaceFilename()).exists())) {
    		m_Framework.loadFramework(m_InputDialog.getWorkspaceFilename());
    	}
		loadOperatorView();
	}
	

	public static String getMenuName()
	{
		return "View:chaseFunc";
	}

	public String toString()
	{
		return "[EpiClust]";
	}
	
	public String getSaveString( ) {
		
		return "";
	}
	
	public void activate()
	{
		this.isActive = true;
		this.updateMap();
		function = new BasicFunction(this);
		isLazy  		= true;
		setView( new ChaseView( this ) );
	}

	@Override
	protected void computeNewColumns()
	{
		//TODO: create/recompute the operator output here
		if (m_Framework != null)
		{
			m_Framework.setTable(input, m_NumGroups);
//			(PchasePainter)(((chaseView)this.view).m_Painter).
		}
		
		m_NewColumns = null;
	}

	@Override
	public void loadOperatorView()
	{
		// comment out if no view
		setView( new ChaseView( this ) );
	}
	
	public class ChaseView extends BasicOp.BasicView
	{
		private static final long serialVersionUID = 4698263995759097051L;
		
		public ChaseView(Operator op)
		{
			super(op);
			init();
		}
		
		@Override
		protected void createPainter(Operator op)
		{
			m_Painter = new PChasePainter(op);
			((PChasePainter)m_Painter).setFramework(m_Framework);
		}
		
		@Override
		protected void buildGUI()
		{
			this.removeAll();
			m_Painter.frame.setTitle("ChAsE: Chromatin Analysis and Exploration Tool");
			//m_Painter.frame.setExtendedState(Frame.MAXIMIZED_BOTH);
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			//TODO: code to handle the GUI actions
		}
		
		public PChasePainter getChasePainter()
		{
			return (PChasePainter)m_Painter;
		}
	}
	
	public boolean modifyInput()
	{
		int result = m_InputDialog.showModal();
		if (result != 0)
		{
			boolean bResetClusters = (result & InputDialog.MODIFIED_REGIONS) != 0;
	    	m_Framework.setTable(m_InputDialog.getDataModel(), bResetClusters);
	    	PChasePainter chasePainter = ((ChaseView)getView()).getChasePainter();
	    	chasePainter.refreshWorkspace(bResetClusters);
	    	//((ChaseView)getView()).getChasePainter().frame.setTitle(m_InputDialog.getRegionName);
	    	return true;
		}
		return false;
	}
	
	public boolean openDialog()
	{
		int result = m_InputDialog.showOpenDialog();
		if (result != 0)
		{
			boolean bResetClusters = (result & InputDialog.MODIFIED_REGIONS) != 0;
	    	m_Framework.setTable(m_InputDialog.getDataModel(), bResetClusters);
	    	PChasePainter chasePainter = ((ChaseView)getView()).getChasePainter();
	    	if ((new File(m_InputDialog.getWorkspaceFilename()).exists())) {
	    		m_Framework.loadFramework(m_InputDialog.getWorkspaceFilename());
	    	}
    		chasePainter.refreshWorkspace(bResetClusters);
	    	return true;
		}
		return false;
	}
	
	public void saveWorkspace()
	{
		m_Framework.saveFramework(m_InputDialog.getWorkspaceFilename());
	}
	
	public static void main( final String[] args )
	{
	    /*
	    // not working after Java 1.6: https://developer.apple.com/library/mac/documentation/Java/Conceptual/Java14Development/07-NativePlatformIntegration/NativePlatformIntegration.html
        // http://stackoverflow.com/questions/3154638/setting-java-swing-application-name-on-mac
	    try {
	        // take the menu bar off the jframe
	        System.setProperty("apple.laf.useScreenMenuBar", "true");

	        // set the name of the application menu item
	        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "ChAsE");

	        // set the look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        */	    
	    
        UpdateManager.checkForUpdates();
		m_InputDialog = new InputDialog();
		if (m_InputDialog.showModal() != 0)
		{
			@SuppressWarnings("unused")
			ChaseOp op = new ChaseOp(m_InputDialog.getDataModel());
		}
		else
		{
			System.exit(0);
		}
	}
}
