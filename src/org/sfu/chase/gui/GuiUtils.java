package org.sfu.chase.gui;

import processing.core.PConstants;
import processing.core.PGraphics;
import still.gui.MouseState;

public class GuiUtils 
{
	static MouseState    m_MouseState;
	static DisplayParams dp;
	
	static void setMouseState(MouseState ms)
	{
		m_MouseState = ms;
	}
	
	static MouseState getMouseState()
	{
		return m_MouseState;
	}
	
	static void setDisplayParams(DisplayParams _dp)
	{
		dp = _dp;
	}
	
	static int CHECKBOX_SIZE = 20; 
	static boolean checkbox(PGraphics gx, float left, float top, boolean checked)
	{
		boolean bClicked = false;
		
		if (m_MouseState.clickedInside(MouseState.LEFT, left, top, CHECKBOX_SIZE, CHECKBOX_SIZE)) {
			bClicked = true;
			checked = !checked;
		}
		
		gx.pushStyle();
		try{
			gx.fill(255);
			gx.stroke(m_MouseState.isStartIn(left, top, CHECKBOX_SIZE, CHECKBOX_SIZE) &&
					  m_MouseState.isIn(left, top, CHECKBOX_SIZE, CHECKBOX_SIZE) ? 0xFFFF0000 : 0);
			gx.rect(left, top, CHECKBOX_SIZE, CHECKBOX_SIZE);
			gx.fill(0);
			if (checked)
			{
				gx.textFont(dp.fontGuiFx);
				gx.textSize(CHECKBOX_SIZE);
				gx.textAlign(PConstants.CENTER, PConstants.CENTER);
				gx.text("z", left + CHECKBOX_SIZE/2, top + CHECKBOX_SIZE/2);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		gx.popStyle();
		return bClicked;
	}
	
}
