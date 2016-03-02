package org.sfu.chase.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;

import org.sfu.chase.core.ClustInfo;
import org.sfu.chase.util.Utils;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import still.gui.MouseState;
import controlP5.Button;
import controlP5.ControlEvent;
import controlP5.ControlP5;
import controlP5.PanelController;

public class ControlPanelOnOff extends ControlPanel 
{
	public  ClusterDisplay	m_CDisplay;
	public  JPopupMenu 	popupOnOff;
	Button  buttonAdd;
	boolean m_bBothOnOff = false; // allowing for both on and off (creates multiple combinations)
	ClustInfo m_CurrResult = null;
	
//	Scrollbar		m_ScrollClustX = null;   

	public ControlPanelOnOff(ControlP5 theControlP5, String theName, int theX,
			int theY, int theWidth, int theHeight, PanelController parent, int anchor) {
		super(theControlP5, theName, theX, theY, theWidth, theHeight, parent, anchor);
		
		m_Title = "Method: Signal Query";
		m_bShowCloseButton = true;
		
		createPopup();
		m_ClustInfo = new ClustInfo();
		
		buttonAdd = theControlP5.addButton("ADD_ONOFF", 1.0f, 0, 0, 50, 20);
		buttonAdd.setCaptionLabel("Add");
		addToLayout(buttonAdd, PanelController.ANCHOR_LEFT | PanelController.ANCHOR_TOP);
		buttonAdd.addListener(this);
	}
	
	public void setClusterDisplay(ClusterDisplay cdisplay)
	{
		m_CDisplay = cdisplay;
	}

	@Override
	public void setClustInfo(ClustInfo cInfo)
	{
		boolean bOnlyRoot = false; // if the operator only allows create on/off from the root
		m_ClustInfo = new ClustInfo();
		if (!bOnlyRoot)
		{
//			if (m_ClustInfo != cInfo)
//				m_ClustInfo = cInfo;
			m_ClustInfo.copyFrom(cInfo);
			m_ClustInfo.m_Child = null; // hack to avoid messing up the children of the root
		}
		else
		{
			m_ClustInfo.copyFrom(m_Framework.getRoot());
			m_ClustInfo.m_Child = null; // hack to avoid messing up the children of the root
		}
		m_CurrResult = getNewResult();
	}
	
	public ClustInfo getNewResult()
	{
		ClustInfo result = new ClustInfo();
		result.copyFrom(m_ClustInfo.m_Child != null ? m_ClustInfo.m_Child : m_ClustInfo);
		return result;
	}
	
	public ClustInfo getCurrentResult()
	{
		return m_CurrResult;
	}
	
	@Override
	public int getPrefferedHeight()
	{
		return (int)(Math.max(3 * dp.checkBoxSize, paneTitleH) + dp.plotH * 3.5 + 20);
	}

	@Override
	public void draw(PApplet app) 
	{
		if (m_ClustInfo.getNumThresholds() == 0)
		{
			m_ClustInfo.initThresholds(m_Framework.getNumGroups());
		}
		
		super.draw(app);
		
		if (m_ClustInfo == null || m_ClustInfo.getStats().m_Count == 0)
			return;

		app.pushStyle();
		try {
			app.smooth();
			
			// draw the cluster plots
			Utils.Rect topPlotsRect = new Utils.Rect(dp.clustRect.left(),
					position.y + Math.max(3.5f * dp.checkBoxSize, paneTitleH) + 20,
					dp.clustRect.width(), dp.plotH);
			Utils.Rect previewPlotsRect = new Utils.Rect(topPlotsRect.left(), topPlotsRect.bottom() + dp.plotH, topPlotsRect.width(), dp.plotH);
			
			buttonAdd.position().x = dp.treeRect.left(); 
			buttonAdd.position().y = previewPlotsRect.vcenter() - buttonAdd.getHeight() / 2;
	
			// draw the cluster plots
			{	
				// cache these to restore afterwards
				DisplayParams.PlotType prevPlotType = dp.plotType;
				Utils.Rect prevClustRect = dp.clustRect;
				ClustInfo prevActiveClust = m_CDisplay.m_ActivePlotClust; 
				
				try
				{
					dp.clustRect = topPlotsRect;
					m_CDisplay.drawOneCluster(app.g, dp, m_ClustInfo, topPlotsRect.left() - dp.clustOffsetX, topPlotsRect.top(), topPlotsRect);
					
					if (m_CurrResult != null && m_CurrResult.size() > 0) {
						m_CDisplay.drawOneCluster(app.g, dp, m_CurrResult, previewPlotsRect.left() - dp.clustOffsetX, previewPlotsRect.top(), previewPlotsRect);
					}
				} catch (Exception e)
				{
					e.printStackTrace();
				}
				
				m_CDisplay.m_ActivePlotClust = prevActiveClust;
				dp.clustRect = prevClustRect;
				dp.plotType = prevPlotType;
			}
		
			{// draw the preview label
				app.textSize(dp.regionTitleFontSize);
				app.fill(dp.regionTitleColor);
				app.textAlign(PConstants.LEFT, PConstants.BOTTOM);
				app.text("Input", dp.treeRect.left(), topPlotsRect.top());
				app.text("Preview", dp.treeRect.left(), previewPlotsRect.top());
			}
			
			float onRectTop = topPlotsRect.top() - 3.5f * dp.checkBoxSize;
			float offRectTop = topPlotsRect.top() - 2.0f * dp.checkBoxSize;
			{// draw the labels for on off checkboxes
				app.textSize(dp.checkBoxSize);
				app.fill(0);
				app.textAlign(PConstants.RIGHT, PConstants.CENTER);
				app.text("on", dp.summaryLeft, onRectTop + dp.checkBoxSize/2);
				app.text("off", dp.summaryLeft, offRectTop + dp.checkBoxSize/2);
			}
			
			// draw the threshold sliders and the histograms
			boolean bThresholdModified = false;
			boolean bDrawThresholdLine = false;
			boolean bOnAllChecked = true;
			boolean bOffAllChecked = true;
			int numGroups = m_Framework.getNumGroups();
            float cbSize = dp.checkBoxSize;
			for (int gi = 0; gi < numGroups; gi++)
			{
				Utils.Rect plotRect = new Utils.Rect(topPlotsRect.left() - dp.clustOffsetX + gi * (dp.plotW + dp.plotGapX), 
						topPlotsRect.top(), dp.plotW, dp.plotH);
				boolean bDisabled = !m_ClustInfo.getThreshold(gi).off && !m_ClustInfo.getThreshold(gi).on;
	
				if (plotRect.left() < topPlotsRect.left())
					continue;
				
				if (plotRect.right() > topPlotsRect.right())
					break;
	
				if (bDisabled)// && !mouseRect.isInside(m_MouseState.x(), m_MouseState.y()))
				{// show the plot as disabled
					app.fill(255, 200);
					app.noStroke();
					app.rect(plotRect.left(), plotRect.top(), plotRect.width()+1, plotRect.height()+1);
					//theApplet.rect(plotRect.left(), plotRect.top(), plotRect.width(), plotRect.height());
				}
	
				if (bDrawThresholdLine)
				{
					// draw the threshold slider
					float r = 5; // slider size
					float sliderX = plotRect.left();// - peakHistSize - r-5;
					float sliderY = topPlotsRect.top() + (1-m_ClustInfo.getThreshold(gi).value) * dp.plotH;
					app.stroke(bDisabled ? 160 : 0);
					app.line(sliderX + r, sliderY, plotRect.left() + dp.plotW, sliderY);
					app.fill(bDisabled ? 160 : 0);
					app.beginShape();
						app.vertex(sliderX, sliderY - r);
						app.vertex(sliderX + r, sliderY);
						app.vertex(sliderX, sliderY + r);
					app.endShape(PGraphics.CLOSE);
					
					// draw the number of on/off data for each group
					app.textSize(Math.min(dp.plotW / 3, 10));
					m_CDisplay.drawNumberOfThresholdRegions(app.g, m_ClustInfo, gi, m_ClustInfo.getThreshold(gi).value, plotRect.hcenter(), sliderY, false);
				}
				
				// draw the on/off checkboxes
				{
					Utils.Rect onRect = new Utils.Rect(plotRect.hcenter() - cbSize/2, onRectTop, cbSize, cbSize);
					Utils.Rect offRect = new Utils.Rect(plotRect.hcenter() - cbSize/2, offRectTop, cbSize, cbSize);
					if (m_MouseState.isReleased(MouseState.LEFT) && onRect.isInside(m_MouseState.x(), m_MouseState.y()))
					{
						m_ClustInfo.getThreshold(gi).on = !m_ClustInfo.getThreshold(gi).on;
						if (!m_bBothOnOff && m_ClustInfo.getThreshold(gi).on)
							m_ClustInfo.getThreshold(gi).off = false;
						bThresholdModified = true;
					}
					if (m_MouseState.isReleased(MouseState.LEFT) && offRect.isInside(m_MouseState.x(), m_MouseState.y()))
					{
						m_ClustInfo.getThreshold(gi).off = !m_ClustInfo.getThreshold(gi).off;
						if (!m_bBothOnOff && m_ClustInfo.getThreshold(gi).off)
							m_ClustInfo.getThreshold(gi).on = false;
						bThresholdModified = true;
					}
					
					// on
					app.fill(m_ClustInfo.getThreshold(gi).on ? onRect.isInside(m_MouseState.x(), m_MouseState.y()) ? 0xFFFF0000 : 0 : 255);
					app.stroke(onRect.isInside(m_MouseState.x(), m_MouseState.y()) ? 0xFFFF0000 : 0);
					app.ellipse(onRect.left(), onRect.top(), cbSize, cbSize);
					
					//app.fill(m_ClustInfo.getThreshold(gi).on ? onRect.isInside(m_MouseState.x(), m_MouseState.y()) ? 0xFFFF0000 : 0 : 255);
					//app.noStroke();
					//app.ellipse(onRect.left()+3, onRect.top()+3, cbSize - 6, cbSize - 6);
					//DrawUtils.arrowHead(app.g, onRect.hcenter() - dp.checkBoxSize/2, onRect.top()+1, dp.checkBoxSize, onRect.height()-2, DrawUtils.ARROW_DIR_UP);
					
					// off
					app.fill(m_ClustInfo.getThreshold(gi).off ? offRect.isInside(m_MouseState.x(), m_MouseState.y()) ? 0xFFFF0000 : 0 : 255);
					app.stroke(offRect.isInside(m_MouseState.x(), m_MouseState.y()) ? 0xFFFF0000 : 0);
					app.ellipse(offRect.left(), offRect.top(), cbSize, cbSize);
					//app.fill(m_ClustInfo.getThreshold(gi).off ? offRect.isInside(m_MouseState.x(), m_MouseState.y()) ? 0xFFFF0000 : 0 : 255);
					//app.noStroke();
					//app.ellipse(offRect.left()+3, offRect.top()+3, cbSize - 6, cbSize - 6);
					//DrawUtils.arrowHead(app.g, offRect.hcenter() - dp.checkBoxSize/2, offRect.top()+2, dp.checkBoxSize, offRect.height()-2, DrawUtils.ARROW_DIR_DOWN);
					
					bOnAllChecked &= m_ClustInfo.getThreshold(gi).on;
                    bOffAllChecked &= m_ClustInfo.getThreshold(gi).off;
				}
			}
			
			{// draw on/off boxes above the summary plot
                Utils.Rect onRect = new Utils.Rect(dp.summaryLeft + (dp.plotW - cbSize)/2, onRectTop, cbSize, cbSize);
                Utils.Rect offRect = new Utils.Rect(dp.summaryLeft + (dp.plotW - cbSize)/2, offRectTop, cbSize, cbSize);
                app.fill(bOnAllChecked ? onRect.isInside(m_MouseState.x(), m_MouseState.y()) ? 0xFFFF0000 : 0 : 255);
                app.stroke(onRect.isInside(m_MouseState.x(), m_MouseState.y()) ? 0xFFFF0000 : 0);
                app.ellipse(onRect.left(), onRect.top(), cbSize, cbSize);
    
                app.fill(bOffAllChecked ? offRect.isInside(m_MouseState.x(), m_MouseState.y()) ? 0xFFFF0000 : 0 : 255);
                app.stroke(offRect.isInside(m_MouseState.x(), m_MouseState.y()) ? 0xFFFF0000 : 0);
                app.ellipse(offRect.left(), offRect.top(), cbSize, cbSize);
                
                if (m_MouseState.isReleased(MouseState.LEFT) && onRect.isInside(m_MouseState.x(), m_MouseState.y()))
                {
                    for (int gi = 0; gi < numGroups; gi++)
                    {
                        m_ClustInfo.getThreshold(gi).on = !bOnAllChecked;
                        if (!m_bBothOnOff && m_ClustInfo.getThreshold(gi).on)
                            m_ClustInfo.getThreshold(gi).off = false;
                    }
                    bThresholdModified = true;
                }
                
                if (m_MouseState.isReleased(MouseState.LEFT) && offRect.isInside(m_MouseState.x(), m_MouseState.y()))
                {
                    for (int gi = 0; gi < numGroups; gi++)
                    {
                        m_ClustInfo.getThreshold(gi).off = !bOffAllChecked;
                        if (!m_bBothOnOff && m_ClustInfo.getThreshold(gi).off)
                            m_ClustInfo.getThreshold(gi).on = false;
                    }
                    bThresholdModified = true;
                }
			}
			
			if (bThresholdModified)
			{
				m_Framework.createThresholdClusters(m_ClustInfo);
				m_CurrResult = getNewResult();
				callChangeListeners(new ChangeEvent(this));
				callChangeListeners(new ControlChangeEvent(this, "summaryPlot", new Integer(-1)));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		app.popStyle();
	}
	
	@Override
	public void controlEvent(ControlEvent theEvent) {
		// TODO Auto-generated method stub
		if (theEvent.isController())
		{
			if (theEvent.controller().name().equals("ADD_ONOFF"))
			{
				callChangeListeners(new ControlChangeEvent(this, "add", new Integer(-1)));
			}
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		// TODO Auto-generated method stub
		
	}

	void createPopup()
	{
		popupOnOff = new JPopupMenu();
		
//		JMenuItem itemAuto = new JMenuItem("Suggest All On");
//		itemAuto.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				m_Framework.setAutomaticThresholds(m_ClustInfo);
//				m_Framework.createThresholdClusters(m_ClustInfo);
//				callChangeListeners(new ChangeEvent(this));
//			}
//		});
//		popupOnOff.add(itemAuto);
		
		JMenuItem itemToggleOn = new JMenuItem("Toggle All On");
		itemToggleOn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean flag = !m_ClustInfo.getThreshold(0).on;
				for (int gi = 0; gi < m_Framework.getNumGroups(); gi++) {
					m_ClustInfo.getThreshold(gi).on = flag;
					if (!m_bBothOnOff && flag) {
						m_ClustInfo.getThreshold(gi).off = false;
					}
				}
				
				m_Framework.createThresholdClusters(m_ClustInfo);
				callChangeListeners(new ChangeEvent(this));
			}
		});
		popupOnOff.add(itemToggleOn);

		JMenuItem itemToggleOff = new JMenuItem("Toggle All Off");
		itemToggleOff.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean flag = !m_ClustInfo.getThreshold(0).off;
				for (int gi = 0; gi < m_Framework.getNumGroups(); gi++) {
					m_ClustInfo.getThreshold(gi).off = flag;
					if (!m_bBothOnOff && flag) {
						m_ClustInfo.getThreshold(gi).on = false;
					}
				}
				m_Framework.createThresholdClusters(m_ClustInfo);
				callChangeListeners(new ChangeEvent(this));
			}
		});
		popupOnOff.add(itemToggleOff);
	}
}
