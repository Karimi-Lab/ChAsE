package org.sfu.chase.gui;

import java.util.ArrayList;
import java.util.Arrays;

public class ColorPalette
{
	public static String[] COLOR_NAMES = {"black","blue","green", "orange", "pink","purple","BYR", "GYR"};
	//public static int[] COLOR_MAX = {0x525252, 0x081D58, 0x00441B, 0xD95F0E, 0xDD3497, 0x54278F};
	//public static int[] COLOR_MED = {0xBDBDBD, 0x1D91C0, 0x90A28D, 0xECAF87, 0xEE9ACB, 0xAA93C7};
	//public static int[] COLOR_MIN = {0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF};
	public static int[] COLOR_MIN = {0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0x0571B0, 0x1A9850};
	public static int[] COLOR_MED = {0x969696, 0x6BAED6, 0x74C476, 0xFD8D3C, 0xF768A1, 0x9E9AC8, 0xFFFFBF, 0xFFFFBF};
	public static int[] COLOR_MAX = {0x252525, 0x08519C, 0x006D2C, 0xA63603, 0x7A0177, 0x54278F, 0xCA0020, 0xD73027};

	public static ArrayList<String> COLOR_LIST = new ArrayList<String>(Arrays.asList(COLOR_NAMES)); 
	
	int m_ColorIndex = 0;
	
	public int getColorIndex()
	{
		return m_ColorIndex;
	}
	
	public void setColorIndex(int index)
	{
		if (index >= 0 && index < COLOR_NAMES.length)
			m_ColorIndex = index;
	}
	
	public String getColorString()
	{
		return COLOR_NAMES[m_ColorIndex];
	}
	
	public void setColor(String colorName)
	{
		int index = COLOR_LIST.indexOf(colorName);
		if (index != -1)
			m_ColorIndex = index;
	}
	
	public int interpolateColor(double ratio)
	{
		return COLOR_MAX[m_ColorIndex]; //TODO
	}
}
