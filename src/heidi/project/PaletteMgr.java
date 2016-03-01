package heidi.project;

import java.util.HashMap;
import java.util.Vector;

import prefuse.Constants;
import prefuse.util.ColorLib;

public class PaletteMgr {
	
	private HashMap<Integer, Vector<int[]>> m_palettes;
	
	private static PaletteMgr s_PaletteMgr;
	
	private static final int SCHEME1_START  = ColorLib.rgb(239, 237, 245);
	private static final int SCHEME1_END    = ColorLib.rgb(63, 0, 125); 

	private static final int SCHEME2_START = ColorLib.rgb(222, 235, 247); 
	private static final int SCHEME2_END   = ColorLib.rgb(8, 48, 107);

	private static final int SCHEME3_START = ColorLib.rgb(229, 245, 224);
	private static final int SCHEME3_END   = ColorLib.rgb(0, 68, 27); 

	private static final int SCHEME4_START = ColorLib.rgb(254, 230, 206); 
	private static final int SCHEME4_END   = ColorLib.rgb(127, 39, 4); 

	private static final int SCHEME5_START = ColorLib.rgb(254, 224, 210); 
	private static final int SCHEME5_END   = ColorLib.rgb(103, 0, 13); 

	private static final int SCHEME6_START  = ColorLib.rgb(240, 240, 240);
	private static final int SCHEME6_END    = ColorLib.rgb(0, 0, 0); 
	
	private static final int NOMINAL_INDEX   = 0; 
	private static final int ORDINAL_INDEX   = 1; 
	
	public static PaletteMgr GetInstance() {
		if (s_PaletteMgr == null) {
			s_PaletteMgr = new PaletteMgr();
		}
		return s_PaletteMgr;
	}
	
	private PaletteMgr() {
		m_palettes = new HashMap<Integer, Vector<int[]>>();
	}
	
	public Vector<int[]> getPalettes(int colorCount) {
		
		// limit maximum number of palette colors to 50
		colorCount = Math.min(colorCount, 50);
		
		Integer key = new Integer(colorCount);
		
		if (m_palettes.containsKey(key)) {
			Vector<int[]> value = m_palettes.get(key);
			return (Vector<int[]>)value.clone();
		}
		
		int[] startColors = new int[]{SCHEME1_START, SCHEME2_START, SCHEME3_START, SCHEME4_START, SCHEME5_START, SCHEME6_START,};
		int[] endColors   = new int[]{SCHEME1_END,   SCHEME2_END,   SCHEME3_END,   SCHEME4_END,   SCHEME5_END,   SCHEME6_END,};
		
		// create default palettes
		Vector<int[]> palettes = new Vector<int[]>();
		palettes.add(NOMINAL_INDEX, ColorLib.getCategoryPalette(colorCount));
		palettes.add(ORDINAL_INDEX, ColorLib.getInterpolatedPalette(colorCount, startColors[0], endColors[0]));
		for (int i = 1; i < startColors.length; i++) {
			palettes.add(ColorLib.getInterpolatedPalette(colorCount, startColors[i], endColors[i]));
		}
		m_palettes.put(key, palettes);
		
		return (Vector<int[]>)palettes.clone();
	}
	
	public int[] getDefaultPalette(int type, int colorCount) {
		
		Vector<int[]> palettes = getPalettes(colorCount);
		
		switch (type) {
		case Constants.ORDINAL:
			return palettes.get(ORDINAL_INDEX).clone();
		case Constants.NOMINAL:
			return palettes.get(NOMINAL_INDEX).clone();
		case Constants.NUMERICAL:
			return palettes.get(ORDINAL_INDEX).clone();
		}
		
		return palettes.get(0).clone();
	}
	
	public void addPalette(int[] palette) {
		Integer key = new Integer(palette.length);
		
		if (!m_palettes.containsKey(key)) {
			Vector<int[]> palettes = getPalettes(palette.length);
			palettes.add(palette);
			m_palettes.put(key, palettes);
		} else {
			Vector<int[]> palettes = m_palettes.get(key);
			if (!palettes.contains(palette)) {
				palettes.add(palette);
			}
		}
	}
}
