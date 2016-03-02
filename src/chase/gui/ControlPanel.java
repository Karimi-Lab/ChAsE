package chase.gui;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.sfu.chase.ClustFramework;
import org.sfu.chase.ClustInfo;

import processing.core.PApplet;
import processing.core.PConstants;
import still.gui.MouseState;
import util.Utils;
import controlP5.ControlEvent;
import controlP5.ControlListener;
import controlP5.ControlP5;
import controlP5.PanelController;

public class ControlPanel extends controlP5.PanelController implements ControlListener, ChangeListener
{
	
	ClustFramework  m_Framework;
	ClustInfo	    m_ClustInfo;
	DisplayParams	dp;
	ChangeListener	m_ChangeListener;
	MouseState      m_MouseState;
	String			m_Title = "";
	int				m_FillColor = 0x01000000;
	int				m_StrokeColor = 0;
	public int		paneTitleH = 60;
	boolean			m_bShowCloseButton = false;

	public ControlPanel(ControlP5 theControlP5, String theName, int theX,
			int theY, int theWidth, int theHeight, PanelController parent, int anchor)
	{
		super(theControlP5, theName, theX, theY, theWidth, theHeight);
		parent.addToLayout(this, anchor);
		color().setBackground(0x01000000);
		//dp = new DisplayParams(ControlP5.papplet);
		// TODO Auto-generated constructor stub
	}
	
	public void setFramework(ClustFramework framework)
	{
		m_Framework = framework;
	}
	
	public void setClustInfo(ClustInfo cInfo)
	{
		m_ClustInfo = cInfo;
	}
	
	public void setTitle(String title)
	{
		m_Title = title;
	}
	
	public ClustInfo getClustInfo()
	{
		return m_ClustInfo;
	}
	
	public int getPrefferedHeight()
	{
		return 	paneTitleH;
	}
	
	public void setDisplayParams(DisplayParams param)
	{
		dp = param;
//		dp.iconW = param.iconW;
//		dp.iconH = param.iconH;
//		dp.checkBoxSize = param.checkBoxSize;
//		dp.clustGapX = param.clustGapX;
//		dp.iconType = param.iconType;
//		dp.clustRect = param.clustRect;
//		dp.summaryLeft = param.summaryLeft;
	}
	
	public void setMouseState(MouseState m)
	{
		m_MouseState = m;
	}
	
	@Override
	public void draw(PApplet theApplet) 
	{
		theApplet.pushStyle();
		try {
			theApplet.fill(m_FillColor);
			theApplet.stroke(160);
			theApplet.rect(position.x(), position.y(), width, height);
	
			theApplet.textFont(dp.fontPanelTitle);
			theApplet.textSize(dp.panelTitleFontSize);
			theApplet.textAlign(PConstants.LEFT, PConstants.TOP);
			theApplet.fill(dp.panelTitleColor);
			theApplet.text(m_Title, position.x + dp.panelTitleX, position.y + dp.panelTitleX);
		} catch (Exception e) {
			e.printStackTrace();
		}
		theApplet.popStyle();
		
		if (m_bShowCloseButton)
		{
			Utils.Rect closeRect = new Utils.Rect(position.x + width - 15, position.y + 5, 10, 10);
			theApplet.stroke(0);
			theApplet.fill(255);
			if (closeRect.isInside(theApplet.mouseX, theApplet.mouseY))
			{
				theApplet.fill(128);
				if (m_MouseState.isReleased(MouseState.LEFT))
					callChangeListeners(new ControlChangeEvent(this, "close", new Integer(-1)));
			}
			
			theApplet.rect(closeRect.left(), closeRect.top(), closeRect.width(), closeRect.height());
			theApplet.line(closeRect.left() + 2, closeRect.top() + 2, closeRect.right() - 2, closeRect.bottom() - 2);
			theApplet.line(closeRect.left() + 2, closeRect.bottom() - 2, closeRect.right() - 2, closeRect.top() + 2);
		}
	}
	
	@Override
	public void controlEvent(ControlEvent theEvent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		// TODO Auto-generated method stub
		
	}
	
    public void addChangeListener(ChangeListener listener)
    {
    	m_ChangeListener = listener; //TODO: make this a Vector if multiple listeners are needed
    }
    
    public void removeChangeListener(ChangeListener listener)
    {
    	m_ChangeListener = null; //TODO: make this a Vector if multiple listeners are needed
    }
    
    void callChangeListeners(ChangeEvent e)
    {
    	if (m_ChangeListener != null)
    	{
    		m_ChangeListener.stateChanged(e);
    	}
    }
    
	@SuppressWarnings("serial")
	public class ControlChangeEvent extends ChangeEvent
	{
		public ControlChangeEvent(Object source, String _name, Object _value) {
			super(source);
			name = _name;
			value = _value;
		}
		public String name;
		public Object value;
	}
    
	
}
