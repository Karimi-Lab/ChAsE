//TODO: optimize the plot_bounds calculation (currently d^2 * n), and call less often.
package still.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.NumberFormat;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import processing.core.*;
import still.data.Operator;
import still.data.TableEvent;

public class PParallelCoordPainter extends OPApplet implements ChangeListener, MouseWheelListener
{
	private static final long serialVersionUID = -1634791160581772727L;

	public int BIG_BORDER_H_L = 25;
	public int BIG_BORDER_H_R = 25;
	public int BIG_BORDER_V_T = 25;
	public int BIG_BORDER_V_B = 25;
	public int[] 	m_MagnifiedViewCoords 		= new int[4]; //{left, top, right, bottom}
	int m_iViewCoordWidth = 0;
	int m_iViewCoordHeight = 0;

	class Coordinate
	{
		public int	  m_ColumnIndex;
		public double m_dMin;
		public double m_dMax;
		public double m_dSelectedMin;
		public double m_dSelectedMax;
	}
	
	Coordinate m_Coordinates[] = null;
	
	public PParallelCoordPainter(Operator op)
	{
		super(op);		
		m_bIsDrawing = true;
		addMouseWheelListener(this);
		initCoordinates();
		m_bIsDrawing = false;
	}
	
	void initCoordinates()
	{
		double dMin = Double.MAX_VALUE;
		double dMax = Double.NEGATIVE_INFINITY;
		numerics = getNumerics();
		countNumerics();
		m_Coordinates = new Coordinate[numerics.size()];
		for (int col = 0; col < numerics.size(); col++)
		{
			m_Coordinates[col] = new Coordinate();
			m_Coordinates[col].m_dMin = Double.MAX_VALUE;
			m_Coordinates[col].m_dMax = Double.NEGATIVE_INFINITY;
			m_Coordinates[col].m_ColumnIndex = numerics.get(col);
			for (int i = 0; i < op.rows(); i++)
			{
				double dVal = op.getMeasurement(i, m_Coordinates[col].m_ColumnIndex);
				m_Coordinates[col].m_dMin = Math.min(dVal, m_Coordinates[col].m_dMin);
				m_Coordinates[col].m_dMax = Math.max(dVal, m_Coordinates[col].m_dMax);
			}
			m_Coordinates[col].m_dSelectedMin = m_Coordinates[col].m_dMin;
			m_Coordinates[col].m_dSelectedMax = m_Coordinates[col].m_dMax;
			dMin = Math.min(dMin, m_Coordinates[col].m_dMin);
			dMax = Math.max(dMax, m_Coordinates[col].m_dMax);
		}
		
		boolean bMatchAll = true; // TODO: control with a GUI checkbox
		if (bMatchAll)
		{
			for (int col = 0; col < numerics.size(); col++)
			{
				m_Coordinates[col].m_dSelectedMin = m_Coordinates[col].m_dMin = dMin;
				m_Coordinates[col].m_dSelectedMax = m_Coordinates[col].m_dMax = dMax;
			}
		}
		
	}
	
	public void heavyResize()
	{
		calcMagnifiedView(0, 0, 0);
		m_bUpdatePGCoord = true;
	}
	
	public void componentResized(ComponentEvent e)
	{
	    SwingUtilities.invokeLater(new Runnable() {
		   public void run() {
			   // Set the preferred size so that the layout managers can handle it
			   heavyResize();
			   invokeRedraw(true);
		   }
    	});
	}
	
	public void mouseWheelMoved (MouseWheelEvent e)
	{
		//println( e.toString() );
		calcMagnifiedView(e.getWheelRotation(), 0, 0);
		invokeRedraw(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		numerics = getNumerics();
		countNumerics();
		updateColorSelectionCol();
	    SwingUtilities.invokeLater(new Runnable() {
								   public void run() {
								   // Set the preferred size so that the layout managers can handle it
								   heavyResize();
								   invokeRedraw(true);
								   }
								   });
		initCoordinates();
	}
	
	public void setup() 
	{
		textFont(createFont("Helvetica",10),10);

		countNumerics();
		updateColorSelectionCol();
		heavyResize();
		
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
	
	boolean m_bUpdatePGCoord = true;
	public void calcMagnifiedView(double dZoom, double dX, double dY)
	{
		m_MagnifiedViewCoords[0] = BIG_BORDER_H_L; 
		m_MagnifiedViewCoords[1] = BIG_BORDER_V_T;
		m_MagnifiedViewCoords[2] = width - BIG_BORDER_H_R;
		m_MagnifiedViewCoords[3] = height - BIG_BORDER_V_B;
		
		m_iViewCoordWidth  = m_MagnifiedViewCoords[2] - m_MagnifiedViewCoords[0];
		m_iViewCoordHeight = m_MagnifiedViewCoords[3] - m_MagnifiedViewCoords[1];
	}
	
	public void invokeRedraw(boolean bUpdatePGCoord)
	{
		if (bUpdatePGCoord)
		{
			m_bUpdatePGCoord = true; // only to be able to place breakpoints
		}
		m_bUpdatePGCoord = bUpdatePGCoord;
		redraw();
	}
	
	boolean m_bIsDrawing = false;
	PGraphics m_PGCoord = null; 
	public synchronized void draw()
	{
		if (m_bIsDrawing)
			return;
		
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
		
		if (m_bUpdatePGCoord)
		{
			try
			{
				updatePGCoord();
			}
			catch (Exception ex)
			{
				System.out.println("Exception: PParallelCoordPainter.updatePGCoord()");
				ex.printStackTrace();
			}
			catch (Error er)
			{
				System.out.println("Error: PParallelCoordPainter.updatePGCoord()");
				er.printStackTrace();
			}
			m_bUpdatePGCoord = false;
		}
		imageMode(CORNER);
		image(m_PGCoord, m_MagnifiedViewCoords[0], m_MagnifiedViewCoords[1], m_iViewCoordWidth, m_iViewCoordHeight);
		
		if (m_bSelecting && m_SelectionStart != null && m_SelectionEnd != null)
		{
			noFill();
			stroke(128, 0, 0);
 			strokeWeight(2);
			rect(m_SelectionStart.x, m_SelectionStart.y, m_SelectionEnd.x - m_SelectionStart.x, m_SelectionEnd.y - m_SelectionStart.y);
		}
		
		// draw axis labels
		
 		//TODO: move these into a member variable and function
		float fCoordTop = 10;;
 		float fCoordBottom = m_iViewCoordHeight - 20;
 		float fCoordLength = fCoordBottom - fCoordTop;

 		textAlign(CENTER);
 		fill(0);
 		if (fCoordLength > 0)
 		{
 			int iCurrCol = -1;
 			float fCurrColX = -1;
 			int iNumCols = numerics.size();
	 		float fDx = (1.f * m_iViewCoordWidth) / iNumCols;
	 		float fXCoord = fDx / 2 + m_MagnifiedViewCoords[0];
	 		for (int c = 0; c < m_Coordinates.length; c++)
	 		{
	 			if (m_Coordinates[c] != null)
	 			{
	 		 		fill(0);
	 		 		
	 		 		if (Math.abs(mouseX - fXCoord) < fDx / 2)
	 		 		{// high light the axis closest to the cursor
	 		 			fill(255, 0, 0);
	 		 			iCurrCol = c;
	 		 			fCurrColX = fXCoord;
	 		 		}
	 				text(this.getOp().input.getColName(m_Coordinates[c].m_ColumnIndex), fXCoord, m_MagnifiedViewCoords[1]);
	 			}
	 			fXCoord += fDx;
	 		}
	 		
	 		fill(128, 0, 0);
	 		if (iCurrCol != -1 && m_Coordinates[iCurrCol] != null && fCoordBottom > fCoordTop && 
	 			mouseY - m_MagnifiedViewCoords[1] > fCoordTop && mouseY - m_MagnifiedViewCoords[1] < fCoordBottom)
	 		{
	 	 		textAlign(LEFT);
	 	 		double fVal = m_Coordinates[iCurrCol].m_dMin;
	 	 		if (m_Coordinates[iCurrCol].m_dMax > m_Coordinates[iCurrCol].m_dMin)
	 	 		{
	 	 			fVal = m_Coordinates[iCurrCol].m_dMin + 
	 	 				   (m_Coordinates[iCurrCol].m_dMax - m_Coordinates[iCurrCol].m_dMin) *  
	 	 				   ((mouseY - m_MagnifiedViewCoords[1]) - fCoordTop) / (fCoordBottom - fCoordTop);
	 	 		}

	 	 		NumberFormat m_NumberFormatDigits = NumberFormat.getInstance();
				m_NumberFormatDigits.setMinimumFractionDigits(0);
				m_NumberFormatDigits.setMaximumFractionDigits(3);
				
	 	 		text(m_NumberFormatDigits.format(fVal), fCurrColX, mouseY);
	 		}
 		}
		m_bIsDrawing = false;
	}
	
	

	void updatePGCoord()
	{
		if (m_PGCoord == null || m_PGCoord.width != m_iViewCoordWidth || m_PGCoord.height != m_iViewCoordHeight)
		{
			m_PGCoord = createGraphics(m_iViewCoordWidth, m_iViewCoordHeight, P2D);
		}

		float fSelMinX = 0;
		float fSelMinY = 0;
		float fSelMaxX = 0;
		float fSelMaxY = 0;
		if (m_SelectionStart != null && m_SelectionEnd != null)
		{
			fSelMinX = -m_MagnifiedViewCoords[0] + Math.min(m_SelectionStart.x, m_SelectionEnd.x);
			fSelMinY = -m_MagnifiedViewCoords[1] + Math.min(m_SelectionStart.y, m_SelectionEnd.y);
			fSelMaxX = -m_MagnifiedViewCoords[0] + Math.max(m_SelectionStart.x, m_SelectionEnd.x);
			fSelMaxY = -m_MagnifiedViewCoords[1] + Math.max(m_SelectionStart.y, m_SelectionEnd.y);
		}
		boolean bSelectionUpdated = false;
		
		m_PGCoord.beginDraw();

		m_PGCoord.fill(255);
		m_PGCoord.beginShape();
		m_PGCoord.vertex(0, 0);
		m_PGCoord.vertex(m_iViewCoordWidth, 0);
		m_PGCoord.vertex(m_iViewCoordWidth, m_iViewCoordHeight);
		m_PGCoord.vertex(0, m_iViewCoordHeight);
		m_PGCoord.endShape(CLOSE);
		//m_PGCoord.smooth()
 		float fCoordTop = 10;
 		float fCoordBottom = m_iViewCoordHeight - 20;
 		float fCoordLength = fCoordBottom - fCoordTop;
 		float fStrokeWeight = 1.0f;
 		float fStrokeAlpha  = 10; // 0..255
 		if (fCoordLength > 0)
 		{
 			m_PGCoord.smooth();

 			int iNumCols = numerics.size();
	 		float fDx = (1.f * m_iViewCoordWidth) / iNumCols;
	 		float fXCoord = fDx / 2;
 			float fXCoords[] = new float[m_Coordinates.length]; // x coordinate of each dimension
 			float fYCoords[] = new float[m_Coordinates.length]; // y coordinate of each dimension of one point
	 		for (int c = 0; c < m_Coordinates.length; c++)
	 		{
	 			fXCoords[c] = fXCoord;
	 			
	 			m_PGCoord.strokeWeight(3);
				m_PGCoord.stroke(0, 0, 128);
	 			m_PGCoord.line(fXCoord, fCoordTop, fXCoord, fCoordBottom);
	 			if (m_Coordinates[c] == null)
	 				return;
				// draw the range boxes
	 			/*
	 			if (m_Coordinates[c].m_dMax > m_Coordinates[c].m_dMin)
	 			{
		 			float fRangeMin = (float)(fCoordTop + fCoordLength * (m_Coordinates[c].m_dSelectedMin - m_Coordinates[c].m_dMin) / (m_Coordinates[c].m_dMax - m_Coordinates[c].m_dMin));
					float fRangeMax = (float)(fCoordTop + fCoordLength * (m_Coordinates[c].m_dSelectedMax - m_Coordinates[c].m_dMin) / (m_Coordinates[c].m_dMax - m_Coordinates[c].m_dMin));
					m_PGCoord.noFill();
					m_PGCoord.stroke(128, 128, 0);
		 			m_PGCoord.strokeWeight(1);
					m_PGCoord.rect(fXCoord - 7, fRangeMin, 13, fRangeMax - fRangeMin);
	 			}*/
	 			fXCoord += fDx;
	 		}
	 		
 			m_PGCoord.strokeWeight(fStrokeWeight);
 			for (int i = 0; i < op.rows(); i++)
 			{		
 		 		boolean bIsPointSelected = false;
 		 		int 	iIntersecting = -1;
 		 		
		 		for (int c = 0; c < m_Coordinates.length; c++)
		 		{
		 			fYCoords[c] = fCoordTop + fCoordLength / 2;
		 			if (m_Coordinates[c] == null)
		 			{
			 			return;//fYCoords[c] = fCoordTop + fCoordLength / 2;
		 			}
		 			if (m_Coordinates[c].m_dMax > m_Coordinates[c].m_dMin)
		 			{
		 				fYCoords[c] = (float)(fCoordTop + fCoordLength * (op.getMeasurement(i, m_Coordinates[c].m_ColumnIndex) - m_Coordinates[c].m_dMin) / (m_Coordinates[c].m_dMax - m_Coordinates[c].m_dMin));
		 			}
		 			
 					if (m_iSelectionCol != -1 && m_bUpdateSelection && iIntersecting != 0 &&
 						(fXCoords[c] >= fSelMinX && fXCoords[c] <= fSelMaxX))
 					{
 						iIntersecting = (fYCoords[c] >= fSelMinY && fYCoords[c] <= fSelMaxY) ? 1 : 0;
 					}
		 		}
		 		
		 		if (m_iSelectionCol != -1)
		 		{
		 			bIsPointSelected = op.getMeasurement(i, m_iSelectionCol) > 0;
			 		if (m_bUpdateSelection)
			 		{
			 			if (iIntersecting == 1)
			 			{
			 				bIsPointSelected = !m_bKeyAltPressed;
		 					op.setMeasurement(i, m_iSelectionCol, bIsPointSelected ? 1 : 0);
		 					bSelectionUpdated = true;
			 			}
	 					else if (!m_bKeyShiftPressed && !m_bKeyAltPressed)
	 					{
	 						bIsPointSelected = false;
		 					op.setMeasurement(i, m_iSelectionCol, 0);
		 					bSelectionUpdated = true;
	 					}
			 		}
		 		}
 		 		
		 		
				m_PGCoord.stroke(0, 0, 0, fStrokeAlpha);
				if (bIsPointSelected)
				{
					m_PGCoord.stroke(255, 0, 0, fStrokeAlpha);
				}
				else if (m_iColorCol != -1)
				{
					int col = (int)getOp().getMeasurement(i, m_iColorCol);
					m_PGCoord.stroke(red(col), green(col), blue(col), fStrokeAlpha);
				}

		 		for (int c = 0; c < m_Coordinates.length - 1; c++)
		 		{
 					m_PGCoord.line(fXCoords[c], fYCoords[c], fXCoords[c + 1], fYCoords[c + 1]);
	 			}
 			}
 		}
		m_PGCoord.endDraw();
		m_bUpdateSelection = false;
		
		if (bSelectionUpdated)
		{
			// send the table change event to other operators
			if (bSelectionUpdated)
			{
				getOp().tableChanged(new TableEvent( getOp(), TableEvent.TableEventType.ATTRIBUTE_CHANGED, "selection", null, false), true); // downstream
				getOp().tableChanged(new TableEvent( getOp(), TableEvent.TableEventType.ATTRIBUTE_CHANGED, "selection", null, true), true); // upstream
			}
		}
		
	}
	
	boolean m_bUpdateSelection = false;
	public void mouseReleased()
	{
		if (m_bSelecting)
		{
			m_bUpdateSelection = true;
			invokeRedraw(true);
			m_bSelecting = false;
		}
	}

	PVector m_SelectionStart;
	PVector m_SelectionEnd;
	boolean m_bSelecting = false;
	public void mouseDragged()
	{
		if (m_bIsDrawing)
			return;
		if (m_bSelecting)
		{
			m_SelectionEnd = new PVector(mouseX, mouseY);
		}
		invokeRedraw(false);    	 
	}
	
	public void mouseMoved()
	{
		invokeRedraw(false);
    }
	
	public void mousePressed()
	{
		if (mouseButton != LEFT)
			return;
		m_SelectionStart = new PVector(mouseX, mouseY);
		m_bSelecting = true;
	}
	
	boolean m_bKeyAltPressed = false;
	boolean m_bKeyShiftPressed = false;
	public void keyPressed()
	{
		if (key == CODED)
		{
			if (keyCode == ALT)
			{
				m_bKeyAltPressed = true;
			} else if (keyCode == SHIFT)
			{
				m_bKeyShiftPressed = true;
			} 
		}
	}
	
	public void keyReleased()
	{
		if (key == CODED)
		{
			if (keyCode == ALT)
			{
				m_bKeyAltPressed = false;
			} else if (keyCode == SHIFT)
			{
				m_bKeyShiftPressed = false;
			} 
		}
	}	
	
	
	@Override
	public void stateChanged(ChangeEvent e)
	{
	}
}
