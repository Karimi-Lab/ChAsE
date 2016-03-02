/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

package org.sfu.chase.input;

/* 
 * ColorRenderer.java (compiles with releases 1.2, 1.3, and 1.4) is used by 
 * TableDialogEditDemo.java.
 */

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.sfu.chase.gui.ColorPalette;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
//import java.awt.GradientPaint;
//import java.awt.Graphics;
//import java.awt.Graphics2D;
import java.awt.Graphics2D;

@SuppressWarnings("serial")
public class ColorRenderer extends JLabel
                           implements TableCellRenderer {
    Border unselectedBorder = null;
    Border selectedBorder = null;
    boolean isBordered = true;
    Color  m_Color[];

    public ColorRenderer(boolean isBordered) {
        this.isBordered = isBordered;
        setOpaque(true); //MUST do this for background to show up.
    }

    public Component getTableCellRendererComponent(
                            JTable table, Object color,
                            boolean isSelected, boolean hasFocus,
                            int row, int column) {
    	//Color newColor = (Color)color;
    	String colorStr = (String)color;
    	int colorIndex = ColorPalette.COLOR_LIST.indexOf(colorStr);
    	colorIndex = colorIndex < 0 ? 0 : colorIndex;
    	m_Color = new Color[3];
    	m_Color[0] = new Color(ColorPalette.COLOR_MIN[colorIndex], false);
    	m_Color[1] = new Color(ColorPalette.COLOR_MED[colorIndex], false);
    	m_Color[2] = new Color(ColorPalette.COLOR_MAX[colorIndex], false);
    	Color newColor = m_Color[2];
        setBackground(newColor);
        if (isBordered) {
            if (isSelected) {
                if (selectedBorder == null) {
                    selectedBorder = BorderFactory.createMatteBorder(2,5,2,5,
                                              table.getSelectionBackground());
                }
                setBorder(selectedBorder);
            } else {
                if (unselectedBorder == null) {
                    unselectedBorder = BorderFactory.createMatteBorder(2,5,2,5,
                                              table.getBackground());
                }
                setBorder(unselectedBorder);
            }
        }
        
        setToolTipText(colorStr);
        		//"RGB value: " + newColor.getRed() + ", " + newColor.getGreen() + ", " + newColor.getBlue());
        return this;
    }
    
    
    public void paint(Graphics g)
    {
		Graphics2D g2d = (Graphics2D)g;
		int iwidth = getWidth()-1;
		int iheight = getHeight()-1;
		
		GradientPaint gradient = new GradientPaint(0, 0, m_Color[0], iwidth/2, 0, m_Color[1], false);
		g2d.setPaint(gradient);
		g2d.fillRect(0, 0, iwidth/2, iheight);
		
		gradient = new GradientPaint(iwidth/2, 0, m_Color[1], iwidth, 0, m_Color[2], false);
		g2d.setPaint(gradient);
		g2d.fillRect(iwidth/2, 0, iwidth/2+1, iheight);

		g2d.setColor(Color.black);
		g2d.drawRect(0, 0, iwidth, iheight);
    }
    
}
