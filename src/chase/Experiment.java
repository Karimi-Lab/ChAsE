package chase;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import chase.gui.ColorPalette;
import chase.input.Parameters;

public class Experiment
{
    public static final String[] NORM_STRINGS = {Parameters.LINEAR, Parameters.GAUSSIAN};
    public static final String[] STAT_STRINGS = {Parameters.REGIONAL, Parameters.GLOBAL};
    public static final String[] BIN_STRINGS  = {Parameters.DIVIDE_BY_SIZE, Parameters.DIVIDE_BY_COUNT};
    
	private Boolean m_Visible =  true;
	private String  m_Name = "";
	private String  m_Filename = "";
	private String  m_NormType = Parameters.DEFAULT_NORM_TYPE;
	private String  m_StatType = Parameters.DEFAULT_STATS_TYPE;
	private String  m_BinningType  = Parameters.DEFAULT_BINNING_TYPE;
	private Boolean m_LogScale = false;
	private ColorPalette  m_Color = new ColorPalette();

	double[][] m_BinaryTable;    // processed binary table
	double[][] m_ReadCountTable; // readcounts per bin
	
	public Experiment()
	{
	}
	
	public Experiment(boolean visible, String name, String fileName,
			int normType, int statType, int binType, boolean logScale, String color)
	{
		m_Visible = new Boolean(visible);
    	m_Name = name;
    	m_Filename = fileName;
    	m_NormType = NORM_STRINGS[normType];
    	m_StatType = STAT_STRINGS[statType];
    	m_BinningType = BIN_STRINGS[binType];
    	m_LogScale = logScale;
    	m_Color.setColor(color);
	}

    public Boolean isVisible() {
		return m_Visible;
	}

	public void setVisible(Boolean visible) {
		m_Visible = visible;
	}

	public String getName() {
		return m_Name;
	}

	public void setName(String name) {
		m_Name = name;
	}

	public String getFilename() {
		return m_Filename;
	}

	public void setFilename(String filename) {
		m_Filename = filename;
	}

	public String getNormType() {
		return m_NormType;
	}

	public void setNormType(String normType) {
		m_NormType = normType;
	}

	public String getStatType() {
		return m_StatType;
	}

	public void setStatType(String statType) {
		m_StatType = statType;
	}

	public String getBinningType() {
		return m_BinningType;
	}

	public void setBinningType(String binType) {
		m_BinningType = binType;
	}
	
	public Boolean isLogScale() {
		return m_LogScale;
	}

	public void setLogScale(Boolean logScale) {
		m_LogScale = logScale;
	}

	public ColorPalette getColor() {
		return m_Color;
	}

	/*
	public String getColorString() {
		int iColor = m_Color.getRGB() & 0x00FFFFFF;
		for (int i = 0; i < Parameters.COLOR_MAX.length; ++i)
			if (Parameters.COLOR_MAX[i] == iColor)
				return Parameters.COLOR_NAMES[i];
			
		String hexString = Integer.toHexString(iColor);
	    while(hexString.length() < 6)  
	        hexString = "0" + hexString;
	    return "0x"+hexString;
	}
	
	public void setColor(Color color) {
		m_Color = color;
	}
	*/
	public void setColorString(ColorPalette color)
	{
		m_Color = color;
		/*
		int index = Parameters.COLOR_LIST.indexOf(color);
		if (index != -1)
			m_Color = new Color(Parameters.COLOR_MAX[index], false);
		else
			m_Color = Color.getColor(color);
		
		if (m_Color == null) {
			try {
				m_Color = Color.decode(color);
			} catch (NumberFormatException e) {
				m_Color = Color.black;
			}
		}*/
	}
	
	public double[][] getData()
	{
		return m_BinaryTable;
	}
	
	public double[][] getReadCountTable()
	{
		return m_ReadCountTable;
	}
	
	public void setData(double[][] values) 
	{
		m_BinaryTable = values;
	}

	public void setReadCountTable(double[][] values) 
	{
		m_ReadCountTable = values;
	}
	
	public boolean writeTable(String filename)
	{
		try {
			FileOutputStream fos = new FileOutputStream(filename);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(m_BinaryTable);
			oos.close();
			fos.close();
		} catch (Exception e) {
			//TODO
			e.printStackTrace();
			return false;
	    }
		return true;
	}
	
	public boolean writeReadCountTable(String filename)
	{
		try {
			FileOutputStream fos = new FileOutputStream(filename);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(m_ReadCountTable);
			oos.close();
			fos.close();
		} catch (Exception e) {
			//TODO
			e.printStackTrace();
			return false;
	    }
		return true;
	}
	
	public boolean readTable(String filename) 
	{
		m_BinaryTable = null;
		try {
            FileInputStream fis = new FileInputStream(filename);
            ObjectInputStream ois = new ObjectInputStream(fis);
            m_BinaryTable = (double[][])ois.readObject();
            ois.close();
            fis.close();
	    } catch (Exception e) {
			//TODO
			e.printStackTrace();
			return false;
	    }
	    return true;
	}
	
	public boolean readReadCountTable(String filename) 
	{
		m_ReadCountTable = null;
		try {
            FileInputStream fis = new FileInputStream(filename);
            ObjectInputStream ois = new ObjectInputStream(fis);
            m_ReadCountTable = (double[][])ois.readObject();
            ois.close();
            fis.close();
	    } catch (Exception e) {
			//TODO
			e.printStackTrace();
			return false;
	    }
	    return true;
	}    
}