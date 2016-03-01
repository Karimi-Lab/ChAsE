package still.gui;

import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.event.ChangeEvent;

import still.data.Operator;

public class PSamplePainter extends PBasicPainter
{
	private static final long serialVersionUID = -4468727316635587950L;

	public PSamplePainter(Operator op)
	{
		super(op);		
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		super.actionPerformed(e);
	}
	
	@Override
	public void setup() 
	{
		super.setup();
	}
	
	
	@Override
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
 		
// 		if( this.getOp() instanceof SampleOp )
// 		{
// 			SampleOp op = (SampleOp) this.getOp();
// 		}
	}
	
	@Override
	public synchronized void mousePressed()
	{
		super.mousePressed();
	}
	
	@Override
	public synchronized void mouseReleased()
	{
		super.mouseReleased();
	}

	
	@Override
	public synchronized void mouseDragged()
	{
		super.mouseDragged();
	}
	
	@Override
	public synchronized void mouseMoved()
	{
		super.mouseMoved();
    }

	@Override
	public synchronized void mouseWheelMoved (MouseWheelEvent e)
	{
		super.mouseWheelMoved(e);
	}
	
	@Override
	public void keyPressed()
	{
//		if (key == CODED)
//		{
//			if (keyCode == ALT)
//			{
//			}
//		}
	}
	
	@Override
	public void stateChanged(ChangeEvent e)
	{
		//TODO handle events coming from the View gui controls
//		if( e.getSource() instanceof JSlider )
//		{
//		}
	}
}
