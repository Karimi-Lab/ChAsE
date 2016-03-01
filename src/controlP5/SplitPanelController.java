package controlP5;

import processing.core.PApplet;

public class SplitPanelController extends Controller {

	public class Divider extends Controller {
		

		protected Divider(ControlP5 theControlP5, ControllerGroup theParent,
				String theName, float theX, float theY, int theWidth,
				int theHeight) {
			super(theControlP5, theParent, theName, theX, theY, theWidth, theHeight);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void setValue(float theValue) {
			// TODO Auto-generated method stub
		}

		@Override
		public void addToXMLElement(ControlP5XMLElement theXMLElement) {
			// TODO Auto-generated method stub
			
		}
		
		public void draw(PApplet theApplet) 
		{
			float[] divCoords = new float[4];
			getDividerCoords(divCoords);

			CColor col = getColor();
			theApplet.noStroke();//stroke(col.getForeground());
			if (theApplet.mouseX > divCoords[0] && theApplet.mouseX < divCoords[2] && 
			    theApplet.mouseY > divCoords[1] && theApplet.mouseY < divCoords[3]) {
				theApplet.fill(col.getBackground());
			} else {
				theApplet.noFill();
			}
			theApplet.rect(divCoords[0], divCoords[1], divCoords[2] - divCoords[0], divCoords[3] - divCoords[1]);
		}
		boolean m_PreviousMouseInside = false;
		boolean m_bIsDraggingDivider = false;
		boolean m_PreviousMousePressed = false;
		@Override
		public void updateInternalEvents(PApplet theApplet)
		{
			float[] divCoords = new float[4];
			getDividerCoords(divCoords);
			
			boolean isMouseInside = theApplet.mouseX >= divCoords[0] && theApplet.mouseY >= divCoords[1] &&
				theApplet.mouseX <= divCoords[2] && theApplet.mouseY <= divCoords[3];
	        if (m_bIsDraggingDivider)
	        {
	        	m_bIsDraggingDivider = theApplet.mousePressed;
	        	if (m_bIsDraggingDivider)
	        	{
	        		m_DividerLocation =	(m_SplitType == HORIZONTAL? 
						(theApplet.mouseX - SplitPanelController.this.position.x()) / SplitPanelController.this.width :
		    			(theApplet.mouseY - SplitPanelController.this.position.y()) / SplitPanelController.this.height);
	        	}
		        
		        if (isFastUpdate() || !m_bIsDraggingDivider)
		        {// update the panes
		        	setDividerLocation(m_DividerLocation);
		        	theApplet.loop();
		        	//theApplet.redraw();
		        }
	        }
	        else if (isMouseInside && !m_PreviousMousePressed)
	        {
	        	if (theApplet.mousePressed && !ControlP5.keyHandler.isAltDown)
	        	{
	            	m_bIsDraggingDivider = true;
	        	}
	        	theApplet.cursor(PApplet.CROSS);
	        }
	        else if (m_PreviousMouseInside)
	        {
	        	theApplet.cursor(PApplet.ARROW);
	        }
	        
	        m_PreviousMouseInside = isMouseInside;
	        m_PreviousMousePressed = theApplet.mousePressed;
		}		
		
	}

	public static final int HORIZONTAL	= 0;
	public static final int VERTICAL 	= 1;
	public static final int LEFT		= 0;
	public static final int TOP			= 0;
	public static final int RIGHT      	= 1;
	public static final int BOTTOM     	= 1;

//	float _x;
//	float _y;
//	int _width;
//	int _height;
	
	int 	m_SplitType = HORIZONTAL;		///< split type: VERTICAL or HORIZONTAL
	double 	m_DividerLocation = 0.5; 	///< initial location of divider 
	int		m_DividerThickness = 7; 	///< thickness of divider bar
	PanelController m_Panes[]; 			///< 2 panes: left, right  or top, bottom
	private boolean	m_bFastUpdate	= false; 	///< update the child panes while the split page is changing (e.g resize)
	
	
	public SplitPanelController(ControlP5 theControlP5, String theName, float theX, float theY, int theWidth, int theHeight)
	{
	    super(theControlP5,  (Tab)(theControlP5.getTab("default")), theName, theX, theY, theWidth, theHeight);
//	    _x = theX; _y = theY; _width = theWidth; _height = theHeight;
	    m_Panes = new PanelController[2];
	    m_Panes[LEFT] = new PanelController(theControlP5, theName+"0", 0, 0, 1, 1);
	    m_Panes[RIGHT] = new PanelController(theControlP5, theName+"1", 0, 0, 1, 1);
	    setDividerLocation(0.5);
	    theControlP5.register(this);
	}
	
	public void setSplitType(int st)
	{
		if (m_SplitType != st)
		{
			m_SplitType = st;
			setDividerLocation(m_DividerLocation);
		}
	}
	
	public void setDividerLocation(double dProportionalLocation)
	{
		m_DividerLocation = Math.min(Math.max(dProportionalLocation, 0.0), 1.0);
		if (m_SplitType == HORIZONTAL)
		{
			m_Panes[LEFT].setPosition(position.x(), position.y());
			m_Panes[RIGHT].setPosition(position.x() + (float)(width * m_DividerLocation) + m_DividerThickness / 2, position.y());
			m_Panes[LEFT].setSize((int)(width * m_DividerLocation - m_DividerThickness / 2), height);
			m_Panes[RIGHT].setSize((int)(width * (1.f - m_DividerLocation) - m_DividerThickness / 2), height);
		}
		else
		{
			m_Panes[TOP].setPosition(position.x(), position.y());
			m_Panes[BOTTOM].setPosition(position.x() , position.y() + (float)(height * m_DividerLocation) + m_DividerThickness / 2);
			m_Panes[TOP].setSize(width, (int)(height * m_DividerLocation - m_DividerThickness / 2));
			m_Panes[BOTTOM].setSize(width, (int)(height * (1.f - m_DividerLocation) - m_DividerThickness / 2));
		}		
	}
	
	Divider m_Divider;
	public void addDivider()
	{
		m_Divider = new Divider(this.controlP5, this.getTab(), this.name()+"_divider", 0, 0, 1, 1);
	}
	
	public PanelController getPane(int side)
	{
		return m_Panes[side];
	}
	
	@Override
	public void setValue(float arg0) {
	}
	@Override
	public void addToXMLElement(ControlP5XMLElement arg0) {
	}
	
	/// left, top, right, bottom
	void getDividerCoords(float[] coords)
	{
		if (m_SplitType == HORIZONTAL)
		{
			coords[0] = (position.x() + width * (float)m_DividerLocation - m_DividerThickness/2); 
			coords[1] = position.y();
			coords[2] = coords[0] + m_DividerThickness;
			coords[3] = coords[1] + height;
		}
		else
		{
			coords[0] = position.x();
			coords[1] = (position.y() + height * (float)m_DividerLocation - m_DividerThickness/2);
			coords[2] = coords[0] + width;
			coords[3] = coords[1] + m_DividerThickness;
		}
	}
	

	
	@Override
	public void draw(PApplet theApplet) 
	{
	}
	
	@Override
	public void setSize(int theWidth, int theHeight) {
		super.setSize(theWidth, theHeight);
		setDividerLocation(m_DividerLocation); // update panes
	}
	
	@Override
	public void setPosition(float x, float y) {
		super.setPosition(x, y);
		setDividerLocation(m_DividerLocation); // update panes
	}
	
	@Override
	public void setVisible(boolean theFlag)
	{
		super.setVisible(theFlag);
		m_Panes[0].setVisible(theFlag);
		m_Panes[1].setVisible(theFlag);
		if (m_Divider != null)
			m_Divider.setVisible(theFlag);
	}
	
	@Override
	public void hide()
	{
		setVisible(false);
	}

	@Override
	public void show()
	{
		setVisible(true);
	}

	@Override
	public void moveTo(java.lang.String theTabName)
	{
		super.moveTo(theTabName);
		m_Panes[0].moveTo(theTabName);
		m_Panes[1].moveTo(theTabName);
		if (m_Divider != null)
			m_Divider.moveTo(theTabName);
	}

	public void setFastUpdate(boolean m_bFastUpdate) {
		this.m_bFastUpdate = m_bFastUpdate;
	}

	public boolean isFastUpdate() {
		return m_bFastUpdate;
	}
}
