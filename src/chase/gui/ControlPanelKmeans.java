package chase.gui;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;

import processing.core.PApplet;
import chase.ClustInfo;
import chase.Utils;
import controlP5.Button;
import controlP5.ControlEvent;
import controlP5.ControlFont;
import controlP5.ControlP5;
import controlP5.PanelController;
import controlP5.Textfield;
import controlP5.Textlabel;

public class ControlPanelKmeans extends ControlPanel 
{
//	int 			m_iCount = 1;
//	public 			ClusterDisplay	m_CDisplay;
	boolean[]		m_Checks;
	Textfield 		m_TextK;
	int				m_K = 2;
	
	Button buttonInc;
	Button buttonDec;

	public ControlPanelKmeans(ControlP5 theControlP5, String theName, int theX,
			int theY, int theWidth, int theHeight, PanelController parent, int anchor) {
		super(theControlP5, theName, theX, theY, theWidth, theHeight, parent, anchor);
		// TODO Auto-generated constructor stub
		
		m_Title = "Method: K-means";
		m_bShowCloseButton = true;
		createPopup();
				
//		Utils.Rect sliderRect = new Utils.Rect(100, 5, 100, 15);

		int buttonH = 20;

		m_TextK = theControlP5.addTextfield("TEXT_K", 20, (int)paneTitleH, 40, buttonH);
		m_TextK.valueLabel().setControlFont(ControlP5.getControlFont());
		m_TextK.setText(Integer.toString(m_K));
		m_TextK.setCaptionLabel("");
		m_TextK.valueLabel().setColor(0xFF000000);
		m_TextK.setColorBackground(0xFFffffff);
		m_TextK.setAutoClear(false);
		m_TextK.addListener(this);
		Textlabel labelK = theControlP5.addTextlabel("LABEL_K", "k", (int)m_TextK.position().x - 15, (int)m_TextK.position().y + 5);
		labelK.setColorValueLabel(0);
		addToLayout(labelK);

		
//		Slider sliderCount = theControlP5.addSlider("clusterCount", 1, 20, 1, 
//				                          (int)sliderRect.left(), (int)sliderRect.top(), (int)sliderRect.width(), (int)sliderRect.height());
//		sliderCount.setNumberOfTickMarks(20);
//		sliderCount.setSliderMode(Slider.FLEXIBLE);
//		sliderCount.setDecimalPrecision(0);
//		sliderCount.valueLabel().setColor(0xFF000000);
//		
//		addToLayout(sliderCount, PanelController.ANCHOR_LEFT | PanelController.ANCHOR_TOP);
//		sliderCount.setLabel(" K");
		
		buttonInc = theControlP5.addButton("INC_K", 1.0f,
				(int)m_TextK.position().x + m_TextK.getWidth(), (int)m_TextK.position().y, 
				15, buttonH/2);
		buttonInc.setLabel(" ");
		buttonInc.addListener(this);
		addToLayout(buttonInc);

		buttonDec = theControlP5.addButton("DEC_K", 1.0f,
				(int)m_TextK.position().x + m_TextK.getWidth(), (int)m_TextK.position().y + buttonH/2, 
				15, buttonH/2);
		buttonDec.setLabel(" ");
		buttonDec.addListener(this);
		addToLayout(buttonDec, PanelController.ANCHOR_LEFT | PanelController.ANCHOR_TOP);
		
		int buttonX = (int)m_TextK.position().x + m_TextK.getWidth() + 20;
		int buttonY = (int)m_TextK.position().y;
		int buttonW = 40;
		
		Button buttonCluster = theControlP5.addButton("RUN_KMEANS", 1.0f, buttonX, buttonY, buttonW, buttonH);
		buttonCluster.setCaptionLabel("Run");
		addToLayout(buttonCluster, PanelController.ANCHOR_LEFT | PanelController.ANCHOR_TOP);

		addToLayout(m_TextK, PanelController.ANCHOR_LEFT | PanelController.ANCHOR_TOP);
		buttonCluster.addListener(this);
	}
	
	@Override
	public void setDisplayParams(DisplayParams param)
	{
		super.setDisplayParams(param);
		ControlFont cfont = new ControlFont(dp.fontGuiFx);
		cfont.setSize(12);
		buttonInc.captionLabel().setControlFont(cfont);
		buttonInc.captionLabel().set("w");
		buttonDec.captionLabel().setControlFont(cfont);
		buttonDec.captionLabel().set("s");
		
	}
	
	@Override
	public int getPrefferedHeight()
	{
		return (int)(paneTitleH + Math.max(30, dp.checkBoxSize * 3));
	}
	
	@Override
	public void setClustInfo(ClustInfo cInfo)
	{
		if (m_ClustInfo != cInfo && cInfo != null)
		{
			m_ClustInfo = cInfo;
			if (cInfo.getNumKmeansActiveGroups() != m_Framework.getNumGroups())
			{
				cInfo.initKmeansActiveGroups(m_Framework.getNumGroups());
			}

			m_Checks = new boolean[m_Framework.getNumGroups()];
			for (int gi = 0; gi < m_Framework.getNumGroups(); gi++)
			{
				m_Checks[gi] = m_ClustInfo.getKmeansActiveGroup(gi);
			}
		}
	}
	
	@Override
	public void draw(PApplet app) 
	{
		super.draw(app);
		if (m_ClustInfo == null || m_ClustInfo.getStats() == null || m_ClustInfo.getStats().m_Count == 0)
			return;
		
		app.pushStyle();
		try {
			// draw the cluster plots
			Utils.Rect plotsRect = new Utils.Rect(dp.clustRect.left() + (dp.plotW - dp.checkBoxSize)/2, position.y + paneTitleH, dp.clustRect.width(), dp.checkBoxSize * 2);
	
			int numGroups = m_Framework.getNumGroups();
			
			if (numGroups != m_Checks.length)
			{
				ClustInfo tmpClustInfo = m_ClustInfo;
				m_ClustInfo = null;
				setClustInfo(tmpClustInfo);
			}
			
			app.smooth();
	
			// draw the checkboxes
			boolean bAllChecked = true;
			for (int gi = 0; gi < numGroups; gi++)
			{
				Utils.Rect plotRect = new Utils.Rect(plotsRect.left() + gi * (dp.plotW + dp.plotGapX), plotsRect.top(), GuiUtils.CHECKBOX_SIZE, GuiUtils.CHECKBOX_SIZE);
				if (plotRect.right() > plotsRect.right()) {
					break; // clicp the checkboxes that are outside panel
				}
	
				if (GuiUtils.checkbox(app.g, plotRect.left(), plotRect.top(), m_Checks[gi])) {
					m_Checks[gi] = !m_Checks[gi];
				}
                bAllChecked &= m_Checks[gi];
			}
			
			
            Utils.Rect plotRect = new Utils.Rect(dp.summaryLeft + (dp.plotW - dp.checkBoxSize) / 2, plotsRect.top(), GuiUtils.CHECKBOX_SIZE, GuiUtils.CHECKBOX_SIZE);
            if (GuiUtils.checkbox(app.g, plotRect.left(), plotRect.top(), bAllChecked)) {
                for (int gi = 0; gi < numGroups; gi++)
                {
                    m_Checks[gi] = !bAllChecked;
                }
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
		    //System.out.println("controller : " + theEvent.controller().name());
			String strK = m_TextK.getText();
			try
			{
				int newK = Integer.parseInt(strK);
				m_K = Math.max(1, newK);
			}
			catch(NumberFormatException e)
			{
				m_TextK.setText(Integer.toString(m_K));
			}
			
			if (theEvent.controller().name().equals("RUN_KMEANS") || theEvent.controller().name().equals("TEXT_K"))
			{
				for (int gi = 0; gi < m_Framework.getNumGroups(); gi++)
				{
					m_ClustInfo.setKmeansActiveGroup(gi, m_Checks[gi]);
				}

				m_Framework.kmeans(m_ClustInfo, m_K);
				callChangeListeners(new ChangeEvent(this));
			}
			else if (theEvent.controller().name().equals("INC_K"))
			{
				m_TextK.setText(Integer.toString(++m_K));
			}
			else if (theEvent.controller().name().equals("DEC_K"))
			{
				m_K = Math.max(1, m_K - 1);
				m_TextK.setText(Integer.toString(m_K));
			}
		}
		
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		// TODO Auto-generated method stub
		System.out.println("controller : " + e.toString());
	}
	
	public JPopupMenu popupCluster;
	void createPopup()
	{
		popupCluster = new JPopupMenu();
		JMenuItem toggleAllItem = new JMenuItem("Toggle Checkboxes");
		toggleAllItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean flag = !m_Checks[0];
				for (int gi = 0; gi < m_Checks.length; gi++)
					m_Checks[gi] = flag;
			}
		});
		popupCluster.add(toggleAllItem);
		callChangeListeners(new ChangeEvent(this));
	}

}
