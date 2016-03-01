package controlP5;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;

import processing.core.PApplet;

public class PanelController extends Controller {

	public interface TransformListener extends EventListener
	{
		void controlSizeChanged(EventObject e);
		void controlPositionChanged(EventObject e);
	}
	
	public static final int ANCHOR_LEFT     = 1 << 0;
	public static final int ANCHOR_RIGHT    = 1 << 1;
	public static final int ANCHOR_TOP      = 1 << 2;
	public static final int ANCHOR_BOTTOM   = 1 << 3;
	public static final int ANCHOR_ALL 		= ANCHOR_LEFT | ANCHOR_RIGHT | ANCHOR_TOP | ANCHOR_BOTTOM; 
	
	float _x;
	float _y;
	int _width;
	int _height;
	float m_Borders[];

	class LayoutItem
	{
		ControllerInterface 	m_Controller;
		int						m_Anchor;
	};
	ArrayList<LayoutItem> m_Items;
	
//	public int left() {return getX();}
//	public int top() {return getY();}
//	public int right() {return getX() + getWidth();}
//	public int bottom() {return getY() + getHeight();}
//	public int hcenter() {return getX() + getWidth() / 2;}
//	public int vcenter() {return getY() + getHeight() / 2;}
	
	public PanelController(ControlP5 theControlP5, String theName, float theX, float theY, int theWidth, int theHeight)
	{
	    super(theControlP5,  (Tab)(theControlP5.getTab("default")), theName, theX, theY, theWidth, theHeight);
	    _x = theX; _y = theY; _width = theWidth; _height = theHeight;
	    m_Items = new ArrayList<LayoutItem>();
	    m_Borders = new float[4];
	    theControlP5.register(this);
	}
	
	public void setBorders(float left, float top, float right, float bottom)
	{
		m_Borders[0] = left;
		m_Borders[1] = top;
		m_Borders[2] = right;
		m_Borders[3] = bottom;
		//TODO: update controller positions
	}
	
	public void addToLayout(ControllerInterface control)
	{
		addToLayout(control, ANCHOR_LEFT | ANCHOR_TOP);
	}
	
	public void addToLayout(ControllerInterface control, int anchor)
	{
		CVector3f pos = control.position();
		control.setPosition(pos.x() + position.x(), pos.y() + position.y());
		LayoutItem item = new LayoutItem();
		item.m_Controller = control;
		item.m_Anchor = anchor;
		m_Items.add(item);
		if (control instanceof Controller)
		{
			((Controller)(control)).moveTo(getTab());
		}
		else if (control instanceof ControllerGroup)
		{
			((ControllerGroup)(control)).moveTo(getTab());
		}
	}

	@Override
	public void setValue(float arg0) {
	}
	
	@Override
	public void addToXMLElement(ControlP5XMLElement arg0) {
	}
	
	@Override
	public void updateInternalEvents(PApplet theApplet) {
		/*
	    if(getIsInside()) {
	        if(isMousePressed && !controlP5.keyHandler.isAltDown) {
	          cX = constrain(mouseX-position.x(),0,width-cWidth);
	          cY = constrain(mouseY-position.y(),0,height-cHeight);
	          setValue(0);
	        }
	      }
        theApplet.redraw();
	    */
	}
	
	@Override
	public void draw(PApplet theApplet) {
		//theApplet.stroke(0, 255, 255);
		//theApplet.noFill();
		CColor col = getColor();
		theApplet.stroke(col.getForeground());
		theApplet.fill(col.getBackground());
		theApplet.rect(position.x(), position.y(), width, height);
		//setPosition(position.x(), position.y());
	}
	
	@Override
	public void setSize(int theWidth, int theHeight)
	{
		theWidth = Math.max(theWidth, 0);
		theHeight = Math.max(theHeight, 0);
		
		super.setSize(theWidth, theHeight);
		for (LayoutItem item:m_Items)
		{
			CVector3f pos = item.m_Controller.position();
			int itemWidth = item.m_Controller.getWidth();
			int itemHeight = item.m_Controller.getHeight();
			
			boolean bPosChanged = false;
			boolean bSizeChanged = false;
			
			if (theWidth != _width)
			{
				if ((item.m_Anchor & ANCHOR_RIGHT) != 0)
				{
					if ((item.m_Anchor & ANCHOR_LEFT) != 0)
					{// anchor left and right -> resize
						bSizeChanged = true;
						itemWidth += theWidth - _width;
					} else
					{// right anchored -> move
						bPosChanged = true;
						pos.x += theWidth - _width;
					}
				}
			}

			if (theHeight != _height)
			{
				if ((item.m_Anchor & ANCHOR_BOTTOM) != 0)
				{
					if ((item.m_Anchor & ANCHOR_TOP) != 0)
					{// anchor left and right -> resize
						bSizeChanged = true;
						itemHeight += theHeight - _height;
					} else
					{// right anchored -> move
						bPosChanged = true;
						pos.y += theHeight - _height;
					}
				}
			}
			
			if (bPosChanged)
			{
				item.m_Controller.setPosition(pos.x(), pos.y());
			}
			if (bSizeChanged)
			{
		    	if (m_TransformListener != null)
		    	{
		    		m_TransformListener.controlSizeChanged(new EventObject(this));
		    	}
				
				if (item.m_Controller instanceof Controller)
				{
					((Controller)(item.m_Controller)).setSize(itemWidth, itemHeight);
				}
				else if (item.m_Controller instanceof ListBox)
				{
					((ListBox)(item.m_Controller)).setWidth(itemWidth);
					((ListBox)(item.m_Controller)).setHeight(itemHeight);
				}
			}
		}
		_width = theWidth;
		_height = theHeight;
	}

	@Override
	public Controller setWidth(int theWidth)
	{
		setSize(theWidth, getHeight());
		return this;
	}
	
	@Override
	public Controller setHeight(int theHeight)
	{
		setSize(getWidth(), theHeight);
		return this;
	}

	@Override
	public void setPosition(float x, float y) {
		super.setPosition(x, y);
		for (LayoutItem item:m_Items)
		{
			CVector3f pos = item.m_Controller.position();
			item.m_Controller.setPosition(pos.x() + x - _x, pos.y() + y - _y);
		}
		_x = x;
		_y = y;
		
    	if (m_TransformListener != null)
    	{
    		m_TransformListener.controlPositionChanged(new EventObject(this));
    	}
	}

	@Override
	public void setVisible(boolean theFlag)
	{
		super.setVisible(theFlag);
		for (LayoutItem item:m_Items)
		{
			if (item.m_Controller instanceof Controller)
			{
				((Controller)(item.m_Controller)).setVisible(theFlag);
			}
			else if (item.m_Controller instanceof ControllerGroup)
			{
				((ControllerGroup)(item.m_Controller)).setVisible(theFlag);
			}
		}
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
		for (LayoutItem item:m_Items)
		{
			if (item.m_Controller instanceof Controller)
			{
				((Controller)(item.m_Controller)).moveTo(theTabName);
			}
			else if (item.m_Controller instanceof ControllerGroup)
			{
				((ControllerGroup)(item.m_Controller)).moveTo(theTabName);
			}
		}
	}
	
	TransformListener m_TransformListener;
    public void addTransformListener(TransformListener listener)
    {
    	m_TransformListener = listener; //TODO: make this a Vector if multiple listeners are needed
    }
    
    public void removeTransformListener(TransformListener listener)
    {
    	m_TransformListener = null; //TODO: make this a Vector if multiple listeners are needed
    }
    
}
