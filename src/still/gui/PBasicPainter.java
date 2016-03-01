package still.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import processing.core.PConstants;

import still.data.Operator;
import still.gui.MouseState;
import still.operators.BasicOp;

public class PBasicPainter extends OPApplet implements ChangeListener, MouseWheelListener
{
	private MouseState	m_CurrMouseState = null;	// current mouse state
	public  MouseState	m_DrawMouseState = null; 	// to store the mouse state during a (potentially slow) draw 

	private boolean m_bKeyAltPressed = false;
	private boolean m_bKeyControlPressed = false;
	private boolean m_bKeyShiftPressed = false;
	
	private static final long serialVersionUID = -3270692393003190622L;
	public int BIG_BORDER_H_L = 25;
	public int BIG_BORDER_H_R = 25;
	public int BIG_BORDER_V_T = 25;
	public int BIG_BORDER_V_B = 25;
	public int[] 	m_ViewCoords 		= new int[4]; //{left, top, right, bottom}
	
	enum OSType
	{
		WINDOWS,
		MAC,
		UNIX,
		SOLARIS,
		UNKNOWN
	};
	private OSType m_OSType;

	private boolean[] m_Keys = new boolean[526];
	 
	public PBasicPainter(Operator op)
	{
		super(op);		
		m_CurrMouseState = new MouseState();
		m_DrawMouseState = new MouseState();
		addMouseWheelListener(this);
		detectOSType();
	}

	public void heavyResize()
	{
		m_ViewCoords[0] = BIG_BORDER_H_L; 
		m_ViewCoords[1] = BIG_BORDER_V_T;
		m_ViewCoords[2] = width - BIG_BORDER_H_R;
		m_ViewCoords[3] = height - BIG_BORDER_V_B;
	}
	
	public void componentResized(ComponentEvent e)
	{
	    SwingUtilities.invokeLater(new Runnable() {
		   public void run() {
			   // Set the preferred size so that the layout managers can handle it
			   heavyResize();
			   invokeRedraw();
		   }
    	});
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		numerics = getNumerics();
		countNumerics();
	    SwingUtilities.invokeLater(new Runnable() {
								   public void run() {
								   // Set the preferred size so that the layout managers can handle it
								   heavyResize();
								   invokeRedraw();
								   }
								   });
	}
	
	public void setup() 
	{
		textFont(createFont("Helvetica",10),10);

		countNumerics();
		heavyResize();
		
		if( this.getOp() instanceof BasicOp )
		{
			numerics = getNumerics();
			// count the number of dimensions
			countNumerics();
			// compute the minimum size
			size(OPAppletViewFrame.MINIMUM_VIEW_WIDTH, OPAppletViewFrame.MINIMUM_VIEW_HEIGHT);
			this.setPreferredSize(new Dimension(OPAppletViewFrame.MINIMUM_VIEW_WIDTH, OPAppletViewFrame.MINIMUM_VIEW_HEIGHT));
		}
		
		heavyResize();
		
		// prevent thread from starving everything else
		noLoop();
		
		this.finished_setup = true;
		
	    SwingUtilities.invokeLater(new Runnable() {
								   public void run() {
								   // Set the preferred size so that the layout managers can handle it
								   invalidate();
								   getParent().validate();
								   }
								   });
	}
	
	public void invokeRedraw()
	{
		loop();   // making sure the redraw will be called even in the case draw() is being executed already.
				  // as redraw() has no effect when draw() is already being executed.
		redraw();
	}
	
	boolean m_bIsDrawing = false;
	public synchronized void draw()
	{
		noLoop();

		m_DrawMouseState.copy(m_CurrMouseState);
		m_CurrMouseState.setPX(m_CurrMouseState.x());
		m_CurrMouseState.setPY(m_CurrMouseState.y());
		m_CurrMouseState.reset(); // to indicate that the mouse state has been processed
		
		m_bIsDrawing = true;
		background(200);
		while (this.getOp().isUpdating()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
    	try
    	{
			fill(0);	
			drawPlot();
		}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
    	
		m_bIsDrawing = false;
	}
	
	protected void drawPlot()
	{
		//TODO: Write your custom drawing functions here
 		fill(255);
 		beginShape();
 		vertex(m_ViewCoords[0] - 1, m_ViewCoords[1] - 1);
 		vertex(m_ViewCoords[2] + 1, m_ViewCoords[1] - 1);
 		vertex(m_ViewCoords[2] + 1, m_ViewCoords[3] + 1);
 		vertex(m_ViewCoords[0] - 1, m_ViewCoords[3] + 1);
 		endShape(CLOSE);
	}
	
	@Override
	public synchronized void mouseClicked()
	{
		m_CurrMouseState.setClickCount(mouseEvent.getClickCount());
	}

	@Override
	public synchronized void mousePressed()
	{
		m_CurrMouseState.setX(mouseX);
		m_CurrMouseState.setY(mouseY);
		m_CurrMouseState.setButton(mouseButton == LEFT ? MouseState.LEFT : MouseState.RIGHT);
		m_CurrMouseState.setStartX(mouseX);
		m_CurrMouseState.setStartY(mouseY);
		m_CurrMouseState.setState(MouseState.PRESSED);
	}

	@Override
	public synchronized void mouseReleased()
	{
		m_CurrMouseState.setX(mouseX);
		m_CurrMouseState.setY(mouseY);
		m_CurrMouseState.setEndX(mouseX);
		m_CurrMouseState.setEndY(mouseY);
		m_CurrMouseState.setState(MouseState.RELEASED);
	}

	
	@Override
	public synchronized void mouseDragged()
	{
		m_CurrMouseState.setX(mouseX);
		m_CurrMouseState.setY(mouseY);
		m_CurrMouseState.setEndX(mouseX);
		m_CurrMouseState.setEndY(mouseY);
		m_CurrMouseState.setState(MouseState.DRAGGING);
	}
	
	@Override
	public synchronized void mouseMoved()
	{
		m_CurrMouseState.setX(mouseX);
		m_CurrMouseState.setY(mouseY);
		if (m_CurrMouseState.state() == -1) // don't change the state if it hasn't been processed already
		{
			m_CurrMouseState.setStartX(mouseX);
			m_CurrMouseState.setStartY(mouseY);
		}
    }

	@Override
	public synchronized void mouseWheelMoved (MouseWheelEvent e)
	{
		m_CurrMouseState.setX(mouseX);
		m_CurrMouseState.setY(mouseY);
		invokeRedraw();
	}
	
	
	@Override
	public synchronized void keyPressed()
	{
		if (keyCode < m_Keys.length) {
			m_Keys[keyCode] = true;
		}
		
		if (key == PConstants.ESC) {
			key = 0; // to prevent from ESC terminating the entire app
		}
		
		if (key == CODED)
		{
			if (keyCode == ALT) {
				m_bKeyAltPressed = true;
			} else if (keyCode == SHIFT) {
				m_bKeyShiftPressed = true;
			} else if (keyCode == CONTROL) {
				m_bKeyControlPressed = true;
			}
		}
	}
	
	@Override
	public synchronized void keyReleased()
	{
		if (keyCode < m_Keys.length) {
			m_Keys[keyCode] = false;
		}
		
		if (key == CODED)
		{
			if (keyCode == ALT) {
				m_bKeyAltPressed = false;
			} else if (keyCode == SHIFT) {
				m_bKeyShiftPressed = false;
			} else if (keyCode == CONTROL) {
				m_bKeyControlPressed = false;
			}
		}
	}
	
	@Override
	public void stateChanged(ChangeEvent e)
	{
	}

	public boolean checkKey(int k)
	{
		if (m_Keys.length >= k) {
			return m_Keys[k];  
		}
		return false;
	}
	
	public boolean isAltPressed() {
		return m_bKeyAltPressed;
	}

	public boolean isControlPressed() {
		return m_bKeyControlPressed;
	}

	public boolean isShiftPressed() {
		return m_bKeyShiftPressed;
	}
	
	private void detectOSType()
	{
		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.indexOf("win") >= 0) {
			m_OSType = OSType.WINDOWS;
		} else if (OS.indexOf("mac") >= 0) {
			m_OSType = OSType.MAC;
		} else if (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0) {
			m_OSType = OSType.UNIX;
		} else if (OS.indexOf("sunos") >= 0) {
			m_OSType = OSType.SOLARIS;
		} else {
			m_OSType = OSType.UNKNOWN;
		}
	}
	
	public OSType getOSType() 
	{
		return m_OSType;
	}
}
