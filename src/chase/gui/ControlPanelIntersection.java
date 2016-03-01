package chase.gui;

import java.util.Arrays;

import javax.swing.event.ChangeEvent;

import processing.core.PApplet;
import processing.core.PConstants;
import chase.ClustInfo;
import chase.Utils;
import chase.ClustCombine.CombineOp;
import chase.Utils.Rect;
import controlP5.Button;
import controlP5.ControlEvent;
import controlP5.ControlP5;
import controlP5.PanelController;

public class ControlPanelIntersection extends ControlPanel 
{
	public 				ClusterDisplay	m_CDisplay;
	ClustInfo[]			m_ComparisonSet;
	boolean[]			m_bChecks;
	
	ClustInfo			m_ClustResult = null;
	
	CombineOp			m_CombineOp = CombineOp.INTERSECT;
	Button buttonAdd;
	
//	public JPopupMenu 	popupOnOff;
	
//	Scrollbar		m_ScrollClustX = null;   

	public ControlPanelIntersection(ControlP5 theControlP5, String theName, int theX,
			int theY, int theWidth, int theHeight, PanelController parent, int anchor) {
		super(theControlP5, theName, theX, theY, theWidth, theHeight, parent, anchor);
		
		m_Title = "Method: Comparison";
		m_bShowCloseButton = true;
//		createPopup();
		
		buttonAdd = theControlP5.addButton("ADD_INTERSECT", 1.0f, 0, 0, 50, 20);
		buttonAdd.setCaptionLabel("Add");
		addToLayout(buttonAdd, PanelController.ANCHOR_LEFT | PanelController.ANCHOR_TOP);
		buttonAdd.addListener(this);
		
	}

	public void setClusterDisplay(ClusterDisplay cdisplay)
	{
		m_CDisplay = cdisplay;
	}

	public void setComparisonSet(ClustInfo[] set)
	{
		m_ComparisonSet = set;
		m_bChecks = new boolean[set.length];
		Arrays.fill(m_bChecks, true);
		
		m_ClustResult = getNewComparison();
	}
	
	@Override
	public int getPrefferedHeight()
	{
		if (m_ComparisonSet == null)
			return super.getPrefferedHeight();
		
		return (int)(paneTitleH + (dp.plotH + dp.plotGapY) * (m_ComparisonSet.length + 1.5f));
	}

	public ClustInfo getCurrentResult()
	{
		return m_ClustResult;
	}

	@Override
	public void draw(PApplet app) 
	{
		super.draw(app);
		
		if (m_ComparisonSet == null)
			return;

		boolean comparisonChanged = false;

		app.pushStyle();
		try {
			app.smooth();
	
			// draw the cluster plots for the comparison set
			Utils.Rect prevClustRect = dp.clustRect;
			ClustInfo activeClust = m_CDisplay.m_ActivePlotClust;
	
			Utils.Rect previewPlotsRect = null;
			for (int i = 0; i < m_ComparisonSet.length; i++)
			{
				// draw the cluster plots
	
				Utils.Rect plotsRect = new Utils.Rect(dp.clustRect.left(), position.y + paneTitleH + i*(dp.plotH + dp.plotGapY),
													  dp.clustRect.width(), dp.plotH);
				dp.clustRect = plotsRect;
				m_CDisplay.drawOneCluster(app.g, dp, m_ComparisonSet[i], plotsRect.left() - dp.clustOffsetX, plotsRect.top(), plotsRect);
	
				
				{//draw the checkboxes
					Rect cbRect = new Rect(dp.treeRect.left(), plotsRect.top(), dp.checkBoxSize, dp.plotH);
					
					if (GuiUtils.checkbox(app.g, cbRect.left(), cbRect.vcenter() - GuiUtils.CHECKBOX_SIZE/2, m_bChecks[i])) {
						comparisonChanged = true;
						m_bChecks[i] = !m_bChecks[i];
					}
					
					app.textSize(12);
					app.fill(64);
					app.textAlign(PConstants.RIGHT, PConstants.CENTER);
					app.text(m_bChecks[i] ? "elements of " : "complement of ", dp.summaryLeft, cbRect.vcenter());
					if (i > 0) {
						app.text("and     ", dp.summaryLeft, cbRect.vcenter() - (dp.plotH + dp.plotGapY)/2);
					}
				}
				
				previewPlotsRect = plotsRect;
			}
			
			if (comparisonChanged)
			{
				m_ClustResult = getNewComparison();
				callChangeListeners(new ControlChangeEvent(this, "summaryPlot", new Integer(-1)));
			}
			
			previewPlotsRect.setTop(previewPlotsRect.bottom() + dp.plotH);
			
			buttonAdd.position().x = dp.treeRect.left(); 
			buttonAdd.position().y = previewPlotsRect.vcenter() - buttonAdd.getHeight() / 2;
			buttonAdd.setVisible(isVisible() && m_ClustResult != null);
			
			// draw the plots for the intersection result
			if (m_ClustResult != null && m_ClustResult.size() > 0)
			{
				m_CDisplay.drawOneCluster(app.g, dp, m_ClustResult, previewPlotsRect.left() - dp.clustOffsetX, previewPlotsRect.top(), previewPlotsRect);
			}
			
			m_CDisplay.m_ActivePlotClust = activeClust;
			
			dp.clustRect = prevClustRect;
			
			{// draw the preview label
				app.textSize(dp.regionTitleFontSize);
				app.fill(dp.regionTitleColor);
				app.textAlign(PConstants.LEFT, PConstants.BOTTOM);
				app.text("Preview", dp.treeRect.left(), previewPlotsRect.top());
				app.text("Input", dp.treeRect.left(), position.y + paneTitleH);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		app.popStyle();
	}

	@Override
	public void controlEvent(ControlEvent theEvent) {
		if (theEvent.isController())
		{
			if (theEvent.controller().name().equals("ADD_INTERSECT"))
			{
				callChangeListeners(new ControlChangeEvent(this, "add", new Integer(0)));
			}
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	public ClustInfo getNewComparison()
	{
		int dataSize = m_Framework.getDataSize();
		int includedSet[] = new int[dataSize];
		for (int c = 0; c < m_ComparisonSet.length; c++)
		{
			ClustInfo cInfo = m_ComparisonSet[c];
			if (!m_bChecks[c])
			{// first include every thing
				for (int i = 0; i < includedSet.length; i++)
					includedSet[i]++;
			}
			
			for (int i = 0; i < cInfo.size(); i++)
				includedSet[cInfo.m_Indices[i]] += m_bChecks[c] ? 1 : -1;
		}
		
		int resultCount = 0;
		for (int i = 0; i < includedSet.length; i++)
		{
			if ((m_CombineOp == CombineOp.INTERSECT && includedSet[i] == m_ComparisonSet.length) ||
				(m_CombineOp == CombineOp.UNION && includedSet[i] > 0))
				resultCount++;
		}
		
		if (resultCount > 0)
		{
			int[] indices = new int[resultCount];
			int currIndex = 0;
			for (int i = 0; i < includedSet.length; i++)
			{
				if ((m_CombineOp == CombineOp.INTERSECT && includedSet[i] == m_ComparisonSet.length) ||
					(m_CombineOp == CombineOp.UNION && includedSet[i] > 0))
					indices[currIndex++] = i;
			}
			
			ClustInfo newClust = new ClustInfo();
			newClust.m_Indices = indices;
			m_Framework.calcClustStats(newClust);
			return newClust;
		}
		
		return null;
	}
	
	
	/*
	void createPopup()
	{
		popupOnOff = new JPopupMenu();
		
		JMenuItem itemAuto = new JMenuItem("Suggest All On");
		itemAuto.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_Framework.setAutomaticThresholds(m_ClustInfo);
				m_Framework.createThresholdClusters(m_ClustInfo);
				callChangeListeners(new ChangeEvent(this));
			}
		});
		popupOnOff.add(itemAuto);
		
		JMenuItem itemToggleOn = new JMenuItem("Toggle All On");
		itemToggleOn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean flag = !m_ClustInfo.getThreshold(0).on;
				for (int gi = 0; gi < m_Framework.getNumGroups(); gi++)
					m_ClustInfo.getThreshold(gi).on = flag;
				m_Framework.createThresholdClusters(m_ClustInfo);
				callChangeListeners(new ChangeEvent(this));
			}
		});
		popupOnOff.add(itemToggleOn);

		JMenuItem itemToggleOff = new JMenuItem("Toggle All Off");
		itemToggleOff.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean flag = !m_ClustInfo.getThreshold(0).off;
				for (int gi = 0; gi < m_Framework.getNumGroups(); gi++)
					m_ClustInfo.getThreshold(gi).off = flag;
				m_Framework.createThresholdClusters(m_ClustInfo);
				callChangeListeners(new ChangeEvent(this));
			}
		});
		popupOnOff.add(itemToggleOff);
	}
	*/
}
