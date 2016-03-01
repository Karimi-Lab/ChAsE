//TODO: optimize the plot_bounds calculation (currently d^2 * n), and call less often.
package still.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import processing.core.*;
import still.data.Operator;
import still.operators.RFuncOp;

public class PImagePainter extends OPApplet implements ChangeListener, MouseWheelListener
{
	private static final long serialVersionUID = -3270692393003190622L;
	public int BIG_BORDER_H_L = 25;
	public int BIG_BORDER_H_R = 25;
	public int BIG_BORDER_V_T = 25;
	public int BIG_BORDER_V_B = 25;
	public int[] 	m_MagnifiedViewCoords 		= new int[4]; //{left, top, right, bottom}

	public PImagePainter(Operator op) {
		super(op);		
		addMouseWheelListener(this);
	}
		
	public void heavyResize()
	{
		calcMagnifiedView(0, 0, 0);
	}
	
	public void componentResized(ComponentEvent e)
	{
	    SwingUtilities.invokeLater(new Runnable() {
		   public void run() {
			   // Set the preferred size so that the layout managers can handle it
			   heavyResize();
			   redraw();
		   }
    	});
	}
	
	public void mouseWheelMoved (MouseWheelEvent e)
	{
		//println( e.toString() );
		calcMagnifiedView(e.getWheelRotation(), 0, 0);
		redraw();
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
								   redraw();
								   }
								   });
	}
	
	public void setup() 
	{
		textFont(createFont("Helvetica",10),10);

		countNumerics();
		heavyResize();
		
		if( this.getOp() instanceof RFuncOp )
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
	
	public void calcMagnifiedView(double dZoom, double dX, double dY)
	{
		m_MagnifiedViewCoords[0] = BIG_BORDER_H_L; 
		m_MagnifiedViewCoords[1] = BIG_BORDER_V_T;
		m_MagnifiedViewCoords[2] = width - BIG_BORDER_H_R;
		m_MagnifiedViewCoords[3] = height - BIG_BORDER_V_B;
	}
	
	boolean m_bIsDrawing = false;
	public synchronized void draw()
	{
		m_bIsDrawing = true;
		background(128 + 64 + 32);
		while (this.getOp().isUpdating()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
    	if( this.getOp() instanceof RFuncOp)
    	{
			fill(0);	
			drawMagnifiedPlot();
		}
    	
		m_bIsDrawing = false;
	}
	
	void drawHeatMap(PGraphics pgOut)
	{
	}
	
	PImage m_Image = null;
	public void setImage(String filename)
	{
		m_Image = null;
		if (filename != null)
		{
			m_Image = loadImage(filename);
		}
	}

	void drawMagnifiedPlot()
	{
 		fill(255);
 		beginShape();
 		vertex(m_MagnifiedViewCoords[0] - 1, m_MagnifiedViewCoords[1] - 1);
 		vertex(m_MagnifiedViewCoords[2] + 1, m_MagnifiedViewCoords[1] - 1);
 		vertex(m_MagnifiedViewCoords[2] + 1, m_MagnifiedViewCoords[3] + 1);
 		vertex(m_MagnifiedViewCoords[0] - 1, m_MagnifiedViewCoords[3] + 1);
 		endShape(CLOSE);
		
		if (m_Image != null)
		{
	 		imageMode(PApplet.CORNER);
			image(m_Image, m_MagnifiedViewCoords[0], m_MagnifiedViewCoords[1], 
						m_MagnifiedViewCoords[2] - m_MagnifiedViewCoords[0],
						m_MagnifiedViewCoords[3] - m_MagnifiedViewCoords[1]);
		}
	}
	
	public void mouseReleased()
	{
		// are we creating a box?
	}

	
	public void mouseDragged()
	{
		if (m_bIsDrawing)
			return;
		
		if (mouseButton == RIGHT)
		{
			calcMagnifiedView(0, mouseX - pmouseX, mouseY - pmouseY);
			redraw();
			return;
		}
	}
	
	public void mouseMoved()
	{
		//redraw();    	 
    }
	
	public void mousePressed()
	{
		if (mouseButton != LEFT)
			return;
	}
	
	@Override
	public void stateChanged(ChangeEvent e)
	{
	}
}
