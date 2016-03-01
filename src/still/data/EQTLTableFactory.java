package still.data;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import still.data.Table.ColType;
import still.expression.Expression;
import still.gui.EnumUtils;

/**
 * 
 * Factory for creating tables from eQTL data: http://www.biovis.net/contest.html
 * @author hyounesy
 *
 */

public class EQTLTableFactory extends TableFactory
{
	public static Table eQTLTable( Container container )
	{
        //MemoryTable memTab = ProcTableFactory.createTable(s_iInitDataSize, s_iInitDataDim);
		MemoryTable memTab = EQTLTableFactory.createPhenoTable(null);
		if( memTab != null )
		{
			memTab.setInputControl( new EQTLPanel(memTab) );
		}
		return memTab;
	}
	
	public enum OutputType
	{
		PHENO_TYPE,
		GENO_TYPE,
		RESULT_SINGLE,
		RESULT_SINGLE_and_GENO_TYPE
	}
	
	public enum TableColumns
	{
		SNP_POSITION,
		SNP_GENE,
		LOG_ODDS_LOW,
		LOG_ODDS_HIGH,
		LOG_ODDS_RATIO_LOW,
		LOG_ODDS_RATIO_HIGH,
		SINGLE_RESULT_B,
		SINGLE_RESULT_S,
		SINGLE_RESULT_R2,
		SINGLE_RESULT_T,
		SINGLE_RESULT_P,
		FAMILY,
		PERSON,
		AFFECTION,
		GENE_EXPRESSION,
		NUM_CONSENSUS
	}
	
	public static int getColIndex(EQTLData data, OutputType type, TableColumns col, int index)
	{
		if (type == OutputType.PHENO_TYPE)
		{
			switch (col)
			{
				case FAMILY:
					return 0;
				case PERSON:
					return 1;
				case AFFECTION:
					return 2;
				case GENE_EXPRESSION:
					if (index < data.getNumPhenoTypes())
						return 3 + index;
					return -1;
				case NUM_CONSENSUS:
					if (index < data.getNumPhenoTypes())
						return 3 + data.getNumPhenoTypes() + index;
					return -1;
			}
		}
		else
		{
			int iResultSingleBaseIndex = 2;
			int iOddsBaseIndex = 2;
			//if (type == OutputType.RESULT_SINGLE_and_GENO_TYPE)
			{
				iOddsBaseIndex += data.getNumPhenoTypes() * 5;
			}
			switch (col)
			{
				case SNP_POSITION:
					return 0;
				case SNP_GENE:
					return 1;
				case LOG_ODDS_LOW:
					return iOddsBaseIndex + index * 4; 
				case LOG_ODDS_HIGH:
					return iOddsBaseIndex + index * 4 + 1; 
				case LOG_ODDS_RATIO_LOW:
					return iOddsBaseIndex + index * 4 + 2; 
				case LOG_ODDS_RATIO_HIGH:
					return iOddsBaseIndex + index * 4 + 3; 
				case SINGLE_RESULT_B:
					if (index < data.getNumPhenoTypes())
						return iResultSingleBaseIndex + index * 5;
					return -1;
				case SINGLE_RESULT_S:
					if (index < data.getNumPhenoTypes())
						return iResultSingleBaseIndex + index * 5 + 1;
					return -1;
				case SINGLE_RESULT_R2:
					if (index < data.getNumPhenoTypes())
						return iResultSingleBaseIndex + index * 5 + 2;
					return -1;
				case SINGLE_RESULT_T:
					if (index < data.getNumPhenoTypes())
						return iResultSingleBaseIndex + index * 5 + 3;
					return -1;
				case SINGLE_RESULT_P:
					if (index < data.getNumPhenoTypes())
						return iResultSingleBaseIndex + index * 5 + 4;
					return -1;
			}
		}
		
		return -1;
		
	}
	
	public static MemoryTable createPhenoTable(EQTLData data)
	{
		int iDataSize = 1;
		int iDim = 1;
		
		//boolean bOutputResultSingle = (type == OutputType.RESULT_SINGLE_and_GENO_TYPE || type == OutputType.RESULT_SINGLE) && data.m_ResultSingle != null;
		//boolean bOutputConsensusRatios = (type == OutputType.GENO_TYPE || type == OutputType.RESULT_SINGLE_and_GENO_TYPE);

		if (data != null && data.m_PhenoType != null)
		{
			iDataSize = data.getNumIndividuals();
			iDim = 2 * data.getNumPhenoTypes() + PhenoType.NUM_INFO_COLUMNS; 
		}
		iDim += 2; // selection and color column
		
        double[][] newTable = new double[iDataSize][iDim];
        String[] colNames = new String[iDim];
		ColType[] colTypes = new ColType[iDim];
		Arrays.fill(colTypes, ColType.NUMERIC);
		colTypes[iDim - 2] = ColType.ATTRIBUTE; // color
		colTypes[iDim - 1] = ColType.ATTRIBUTE; // selection
		
		MemoryTable memTab = new MemoryTable(newTable, colTypes, colNames);
		memTab.setDescriptor("PhenoType");

		if (data != null && data.m_PhenoType != null)
		{
			int col = 0;
			colNames[col++] = "FAMILY";
			colNames[col++] = "PERSON";
			colNames[col++] = "AFFECTION";
			for( int d = 0; d < data.m_PhenoTypeName.length; d++ )
	    	{
	    		colNames[col++] = (d+1) + "-" + data.m_PhenoTypeName[d];
	    	}
			for( int d = 0; d < data.m_PhenoTypeName.length; d++ )
	    	{
	    		colNames[col++] = (d+1) + "-%Consensus";
	    	}

			int iColorCol = col++;
			int iSelectionCol = col++;
    		colNames[iColorCol] = "color";
    		colNames[iSelectionCol] = "selection";
    		for (int i = 0; i < iDataSize; i++)
    		{
    			newTable[i][iColorCol] = 0xFF000000;
    			newTable[i][iSelectionCol] = 0;
    		}

    		updatePhenoTable(memTab, data);
		}
		
		
		return memTab;
	}
	
	public static void updatePhenoTable(MemoryTable memTab, EQTLData data)
	{
		double[][] newTable = memTab.getTable();
		int iDataSize = memTab.rows();

		for (int i = 0; i < iDataSize; i++)
		{
			int col = 0;
			newTable[i][col++] = data.m_PhenoType[i].m_Family;
			newTable[i][col++] = data.m_PhenoType[i].m_Person;
			newTable[i][col++] = data.m_PhenoType[i].m_Affection;
			for( int d = 0; d < data.getNumPhenoTypes(); d++ )
    		{
    			newTable[i][col++] = data.m_PhenoType[i].m_ExpressionLevel[d];
    		}
			for( int d = 0; d < data.getNumPhenoTypes(); d++ )
    		{
    			newTable[i][col++] = data.m_PhenoType[i].m_NumConsensus[d];
    		}
//			newTable[i][col++] = 0xFF000000; // color
//			newTable[i][col++] = 0; // selection
    	}
	}
	
	public static MemoryTable createGenoTable(EQTLData data)
	{
		int iDataSize = 1;
		int iDim = 1;
		
		boolean bOutputResultSingle = data != null && data.m_ResultSingle != null;//(type == OutputType.RESULT_SINGLE_and_GENO_TYPE || type == OutputType.RESULT_SINGLE) && data.m_ResultSingle != null;
		boolean bOutputConsensusRatios = data != null;//(type == OutputType.GENO_TYPE || type == OutputType.RESULT_SINGLE_and_GENO_TYPE);

		if (data != null)
		{
			iDataSize = data.m_ResultSingle[0].length;
			iDim = 2 + //gene number and position to which each SNP belongs  
				   (bOutputResultSingle ? data.m_ResultSingle.length * 5 : 0) + // 5 statistical results per phenotype (B, S, R2, T, P)
				   (bOutputConsensusRatios ? (data.getNumPhenoTypes() + 1) * 4 : 0); // 2 count and 2 ratios (high/low) per phenotype, 1 for affected and non-affected
		}
		
		iDim += 2; // selection and color column
		
        double[][] newTable = new double[iDataSize][iDim];
        String[] colNames = new String[iDim];
		ColType[] colTypes = new ColType[iDim];
		Arrays.fill(colTypes, ColType.NUMERIC);
		colTypes[iDim - 2] = ColType.ATTRIBUTE; // color
		colTypes[iDim - 1] = ColType.ATTRIBUTE; // selection
		
		MemoryTable memTab = new MemoryTable(newTable, colTypes, colNames);
		memTab.setDescriptor("NULL");	
		
		if (data != null)
		{
			if (bOutputResultSingle)
				memTab.setDescriptor("ResultSingleLoci");
			else
				memTab.setDescriptor("ConsensusRatios");
			
			int col = 0;
			colNames[col++] = "Position";
			colNames[col++] = "Gene";

			if (bOutputResultSingle)
			{
				for( int d = 0; d < data.m_ResultSingle.length; d++ )
		    	{
		    		colNames[col++] = "B" + (d + 1);
		    		colNames[col++] = "S" + (d + 1);
		    		colNames[col++] = "R" + (d + 1);
		    		colNames[col++] = "T" + (d + 1);
		    		colNames[col++] = "P" + (d + 1);
		    	}
			}

			if (bOutputConsensusRatios)
			{
				for (int p = 0; p < data.getNumPhenoTypes(); p++)
				{
					colNames[col++] = "#_LOW-" + (p+1);
					colNames[col++] = "#_HIGH-"+ (p+1);
					colNames[col++] = "R_LOW-" + (p+1);
					colNames[col++] = "R_HIGH-"+ (p+1);
				}
				colNames[col++] = "#_WELL";
				colNames[col++] = "#_ILL";
				colNames[col++] = "R_WELL";
				colNames[col++] = "R_ILL";
			}

			int iColorCol = col++;
			int iSelectionCol = col++;
    		colNames[iColorCol] = "color";
    		colNames[iSelectionCol] = "selection";
    		for (int i = 0; i < iDataSize; i++)
    		{
    			newTable[i][iColorCol] = 0xFF000000;
    			newTable[i][iSelectionCol] = 0;
    		}

    		updateGenoTable(memTab, data);
		}
		
		return memTab;
	}
	
	public static void updateGenoTable(MemoryTable memTab, EQTLData data)
	{
		double[][] newTable = memTab.getTable();
		int iDataSize = memTab.rows();

		boolean bOutputResultSingle = data != null && data.m_ResultSingle != null;//(type == OutputType.RESULT_SINGLE_and_GENO_TYPE || type == OutputType.RESULT_SINGLE) && data.m_ResultSingle != null;
		boolean bOutputConsensusRatios = data != null;//(type == OutputType.GENO_TYPE || type == OutputType.RESULT_SINGLE_and_GENO_TYPE);
		
		for (int i = 0; i < iDataSize; i++)
		{
			int iSNPIndex = data.m_ResultSingle[0][i].m_SNPIndex;

			int col = 0;
			newTable[i][col++] = data.m_SNPInfo[iSNPIndex].m_Position;
			newTable[i][col++] = data.m_ResultSingle[0][i].m_PhenoIndex;

			if (bOutputResultSingle)
			{
				for( int d = 0; d < data.m_ResultSingle.length; d++ )
		    	{
					newTable[i][col++] = data.m_ResultSingle[d][i].m_Beta;
					newTable[i][col++] = data.m_ResultSingle[d][i].m_SE;
					newTable[i][col++] = data.m_ResultSingle[d][i].m_R2;
					newTable[i][col++] = data.m_ResultSingle[d][i].m_T;
					newTable[i][col++] = data.m_ResultSingle[d][i].m_P;
		    	}
			}

			if (bOutputConsensusRatios)
			{
				for (int p = 0; p < data.getNumPhenoTypes(); p++)
				{
	    			newTable[i][col++] = data.m_SNPInfo[iSNPIndex].m_CountLow[p];
					newTable[i][col++] = data.m_SNPInfo[iSNPIndex].m_CountHigh[p];
	    			newTable[i][col++] = data.m_SNPInfo[iSNPIndex].m_RatioLow[p];
					newTable[i][col++] = data.m_SNPInfo[iSNPIndex].m_RatioHigh[p];
				}
    			newTable[i][col++] = data.m_SNPInfo[iSNPIndex].m_CountAff1;
				newTable[i][col++] = data.m_SNPInfo[iSNPIndex].m_CountAff2;
    			newTable[i][col++] = data.m_SNPInfo[iSNPIndex].m_RatioAff1;
				newTable[i][col++] = data.m_SNPInfo[iSNPIndex].m_RatioAff2;
			}
		}
	}
	
	public static int getAttributeCol(Table table, String sAttribName)
	{
		int i = 0;
		for (ColType type : table.getColTypes())
		{
			if (type == ColType.ATTRIBUTE && table.getColName(i).equalsIgnoreCase(sAttribName) )
			{
				return i;
			}
			i++;
		}
		return -1;
	}
	
	public static int getSelectionCol(Table table)
	{
		return getAttributeCol(table, "selection");
	}
	
	public static int getColorCol(Table table)
	{
		return getAttributeCol(table, "color");
	}
	
	/**
	 */
	/*
	public static MemoryTable createTable1(EQTLData data, OutputType type)
	{
		int iDataSize = 1;
		int iDim = 1;
		
		boolean bOutputResultSingle = (type == OutputType.RESULT_SINGLE_and_GENO_TYPE || type == OutputType.RESULT_SINGLE) && data.m_ResultSingle != null;
		boolean bOutputConsensusRatios = (type == OutputType.GENO_TYPE || type == OutputType.RESULT_SINGLE_and_GENO_TYPE);

		if (data != null)
		{
			if (type == OutputType.PHENO_TYPE && data.m_PhenoType != null)
			{
				iDataSize = data.getNumIndividuals();
				iDim = 2 * data.getNumPhenoTypes() + PhenoType.NUM_INFO_COLUMNS; 
			}
			else if (bOutputResultSingle || bOutputConsensusRatios)
			{
				iDataSize = data.m_ResultSingle[0].length;
				iDim = 2 + //gene number and position to which each SNP belongs  
					   (bOutputResultSingle ? data.m_ResultSingle.length * 5 : 0) + // 5 statistical results per phenotype (B, S, R2, T, P)
					   (bOutputConsensusRatios ? (data.getNumPhenoTypes() + 1) * 4 : 0); // 2 count and 2 ratios (high/low) per phenotype, 1 for affected and non-affected
			}
		}
		
		iDim++; // selection column
		
        double[][] newTable = new double[iDataSize][iDim];
        String[] colNames = new String[iDim];
		ColType[] colTypes = new ColType[iDim];
		Arrays.fill(colTypes, ColType.NUMERIC);
		colTypes[iDim - 1] = ColType.ATTRIBUTE;
		
		MemoryTable memTab = new MemoryTable(newTable, colTypes, colNames);
		memTab.setDescriptor("NULL");
		
		if (data != null)
		{
			if (type == OutputType.PHENO_TYPE && data.m_PhenoType != null)
			{
				memTab.setDescriptor("PhenoType");
				int col = 0;
				colNames[col++] = "FAMILY";
				colNames[col++] = "PERSON";
				colNames[col++] = "AFFECTION";
				for( int d = 0; d < data.m_PhenoTypeName.length; d++ )
		    	{
		    		colNames[col++] = (d+1) + "-" + data.m_PhenoTypeName[d];
		    	}
				for( int d = 0; d < data.m_PhenoTypeName.length; d++ )
		    	{
		    		colNames[col++] = (d+1) + "-%Consensus";
		    	}
				
	    		for (int i = 0; i < iDataSize; i++)
	    		{
					col = 0;
	    			newTable[i][col++] = data.m_PhenoType[i].m_Family;
	    			newTable[i][col++] = data.m_PhenoType[i].m_Person;
	    			newTable[i][col++] = data.m_PhenoType[i].m_Affection;
	    			for( int d = 0; d < data.getNumPhenoTypes(); d++ )
		    		{
		    			newTable[i][col++] = data.m_PhenoType[i].m_ExpressionLevel[d];
		    		}
	    			for( int d = 0; d < data.getNumPhenoTypes(); d++ )
		    		{
		    			newTable[i][col++] = data.m_PhenoType[i].m_NumConsensus[d];
		    		}
		    	}
	    		
	    		colNames[col++] = "selection";
			}
			else if (bOutputResultSingle || bOutputConsensusRatios)
			{
				if (bOutputResultSingle)
					memTab.setDescriptor("ResultSingleLoci");
				else
					memTab.setDescriptor("ConsensusRatios");
				
				int col = 0;
				colNames[col++] = "Position";
				colNames[col++] = "Gene";

				if (bOutputResultSingle)
				{
					for( int d = 0; d < data.m_ResultSingle.length; d++ )
			    	{
			    		colNames[col++] = "B" + (d + 1);
			    		colNames[col++] = "S" + (d + 1);
			    		colNames[col++] = "R" + (d + 1);
			    		colNames[col++] = "T" + (d + 1);
			    		colNames[col++] = "P" + (d + 1);
			    	}
				}

				if (bOutputConsensusRatios)
				{
					for (int p = 0; p < data.getNumPhenoTypes(); p++)
					{
						colNames[col++] = "#_LOW-" + (p+1);
						colNames[col++] = "#_HIGH-"+ (p+1);
						colNames[col++] = "R_LOW-" + (p+1);
						colNames[col++] = "R_HIGH-"+ (p+1);
					}
					colNames[col++] = "#_WELL";
					colNames[col++] = "#_ILL";
					colNames[col++] = "R_WELL";
					colNames[col++] = "R_ILL";
				}
				
	    		for (int i = 0; i < iDataSize; i++)
	    		{
					int iSNPIndex = data.m_ResultSingle[0][i].m_SNPIndex;

					col = 0;
					newTable[i][col++] = data.m_SNPInfo[iSNPIndex].m_Position;
					newTable[i][col++] = data.m_ResultSingle[0][i].m_PhenoIndex;

	    			if (bOutputResultSingle)
					{
						for( int d = 0; d < data.m_ResultSingle.length; d++ )
				    	{
							newTable[i][col++] = data.m_ResultSingle[d][i].m_Beta;
							newTable[i][col++] = data.m_ResultSingle[d][i].m_SE;
							newTable[i][col++] = data.m_ResultSingle[d][i].m_R2;
							newTable[i][col++] = data.m_ResultSingle[d][i].m_T;
							newTable[i][col++] = data.m_ResultSingle[d][i].m_P;
				    	}
					}

					if (bOutputConsensusRatios)
					{
						for (int p = 0; p < data.getNumPhenoTypes(); p++)
						{
			    			newTable[i][col++] = data.m_SNPInfo[iSNPIndex].m_CountLow[p];
							newTable[i][col++] = data.m_SNPInfo[iSNPIndex].m_CountHigh[p];
			    			newTable[i][col++] = data.m_SNPInfo[iSNPIndex].m_RatioLow[p];
							newTable[i][col++] = data.m_SNPInfo[iSNPIndex].m_RatioHigh[p];
						}
		    			newTable[i][col++] = data.m_SNPInfo[iSNPIndex].m_CountAff1;
						newTable[i][col++] = data.m_SNPInfo[iSNPIndex].m_CountAff2;
		    			newTable[i][col++] = data.m_SNPInfo[iSNPIndex].m_RatioAff1;
						newTable[i][col++] = data.m_SNPInfo[iSNPIndex].m_RatioAff2;
					}
		    	}
			}
		}
		return memTab;
	}
	*/
	
	// phenotype for each individual (comes from pheno.dat)
	public static class PhenoType
	{
		public final static int NUM_INFO_COLUMNS = 3; //family/person/affection 
		public int		m_Family;
		public int		m_Person;
		public int		m_Affection;
		public double	m_ExpressionLevel[]; //[e.g. 15] RNA expression levels for this person
		//public double   m_PExpressionLevel[]; // predicted expression level
		//public double	m_PAffection; // predicted affection state: between 1.0 .. 2.0
		public double	m_NumConsensus[];
		//public boolean	m_bSelected = true;
	}
	
	// Genotype data for each individual (comes from sim.ped)
	public static class GenoType
	{
		public int 	m_Family;
		public int 	m_Person;
		public int 	m_Father;
		public int 	m_Mother;
		public int 	m_Sex;
		public char	m_SNP[]; //[7555*2] pair of A/C/G/T SNPs for this person
	}
	
	//The information for one SNP (comes from sim.map and calcCommonSNP) 
	public static class SNPInfo
	{
		public int 		m_Gene; 	//1..15 phenotype id
		public String 	m_Name; 	//chr or rs name
		public int 		m_Position; // base-pair position
		public String 	m_CommonSNP; // the most common pair for this SNP across all individuals
		public double 	m_RatioAll; // 0..1: the ratio of the number of occurrences of the most common pair to the number of individuals.
		
		public String 	m_CommonSNPAff1; // the most common pair for this SNP across non affected individuals
		public String 	m_CommonSNPAff2; // the most common pair for this SNP across affected individuals
		public double 	m_RatioAff1; // 0..1: the ratio of the number of occurrences of the most common pair to the number of individuals, in the non affected individuals.
		public double 	m_RatioAff2; // 0..1: the ratio of the number of occurrences of the most common pair to the number of individuals, in the affected individuals.
		public double	m_CountAff1;
		public double	m_CountAff2;
		
		public double 	m_RatioLow[];  // [size: num phenotypes, e.g. 15]: ratio of consensus occurrence for this SNP among individuals with low expressin of a each phenotype
		public double 	m_RatioHigh[]; // [size: num phenotypes, e.g. 15]: ratio of consensus occurrence for this SNP among individuals with high expression of each phenotype
		public double	m_CountHigh[];
		public double	m_CountLow[];
		
		//public boolean	m_bSelected = true;
	}
	
	// PLINK single results
	public static class ResultSingle
	{
		public int		m_PhenoIndex;
		public int		m_SNPIndex; // SNP index in SNPInfo
		public double 	m_Beta; 	// Regression coefficient - this is a measure of the slope of the line in the regression
		public double 	m_SE; 		// Standard error - a measure of uncertainty on the regression coefficient.
		public double 	m_R2; 		// Regression r-squared - this is a formal estimate of effect size
		public double 	m_T;		// Wald test (based on t-distribution) estimates the deviation of the predicted model, from the null hypothesis (that the SNP has no effect on the observed expression level).
		public double 	m_P; 		// p-value 
	}
	
	// PLINK double results
	public static class ResultDouble
	{
		public int		m_PhenoIndex;
		public int 		m_SNPIndex1;
		public int 		m_SNPIndex2;
		public double 	m_Beta;
		public double 	m_Stat;
		public double 	m_P;
	}
	
	// Grouping info for a phenotype
	public static class GroupInfo
	{
		public double			m_dMinEx; // minimum expression level for this phenotype among all individuals
		public double			m_dMaxEx; // maximum expression level for this phenotype among all individuals

		public final static int NUM_GROUPS = 3; // number of groups: {mediumEx, lowEx, highEx}  or {0, Well, Sick}

		/// Normalized threshold deciding low expression individuals. 0.0 will only pick individual(s) having lowest expression level.
		public double			m_dThresholdLow = 0.33;
		/// Normalized threshold deciding high expression individuals. 1.0 will only pick individual(s) having highest expression level.
		public double			m_dThresholdHigh = 0.66;
		public int 				m_Size[];//[3]  number of individuals in each group
		
		public static final int	NUM_HIST_BINS = 50;
		public double			m_Hist[];//[NUM_HIST_BINS] / histogram of number of individuals by expression level
		
		public int	m_ConsensusAllele[][]; //[NUM_GROUPS][getNumSNPs()]: index of consensus allele of each group
		
		public GroupInfo()
		{
			m_Size = new int[NUM_GROUPS]; // number of individuals within each group
			m_Hist = new double[NUM_HIST_BINS];
			//m_ConsensusAllele = new int[NUM_GROUPS][];
		}
	}
	
	public static class EQTLData
	{
		public String	  		m_PhenoTypeName[];	// [size: num phenotypes, e.g. 15] name of pheno types
		public PhenoType 		m_PhenoType[]; 		// [size: num individuals, e.g. 500]
		public GenoType  		m_GenoType[];  		// [size: num individuals, e.g. 500]
		public SNPInfo	  		m_SNPInfo[];   		// [size: num SNPs, e.g. 7555]
		public ResultSingle		m_ResultSingle[][];	// [15][7555]
		public ResultDouble		m_ResultDouble[][];	// [15][7555]
		public HashMap<String, Integer> m_SNPHash;
		public GroupInfo		m_GroupInfo[]; // [15 + 1] Group info for each phenotype + affection status
		
		
		public int getNumIndividuals()
		{
			if (m_PhenoType != null)
				return m_PhenoType.length;
			return 0;
		}

//		public int getNumSelectedIndividuals()
//		{
//			int iSelected = 0;
//			if (m_PhenoType != null)
//			{
//				for (int i = 0; i < m_PhenoType.length; i++)
//				{
//					iSelected += m_PhenoType[i].m_bSelected ? 1 : 0;
//				}
//			}
//			return iSelected;
//		}

		public int getNumPhenoTypes()
		{
			if (m_PhenoTypeName != null)
				return m_PhenoTypeName.length;
			return 0;
		}
		
		public String getPhenoTypeName(int i)
		{
			if (m_PhenoTypeName != null && i < m_PhenoTypeName.length)
			{
				return m_PhenoTypeName[i];
			}
			return "";
		}

		public int getNumSNPs()
		{
			if (m_SNPInfo != null)
				return m_SNPInfo.length;
			return 0;
		}
		
//		public int getNumSelectedSNPs()
//		{
//			int iSelected = 0;
//			if (m_SNPInfo != null)
//			{
//				for (int i = 0; i < m_SNPInfo.length; i++)
//				{
//					iSelected += m_SNPInfo[i].m_bSelected ? 1 : 0;
//				}
//			}
//			return iSelected;
//		}
		
		
		public int getSNPIndex(String snpName)
		{
			if (m_SNPHash != null)
			{
				Integer i = m_SNPHash.get(snpName);
				return i != null? i : -1;
			}
			return -1;
		}
		
		public int getSNPPosition(String snpName)
		{
			int i = getSNPIndex(snpName);
			return i != -1 ? m_SNPInfo[i].m_Position : -1;
		}
		
		public final static int NUM_ALLELE = 16; // number of SNP alleles
		public final static String[] s_AlleleStrings = {"AA", "AC", "AG", "AT", "CA", "CC", "CG", "CT", "GA", "GC", "GG", "GT", "TA", "TC", "TG", "TT"};

		/**
		 * Returns the allele's sequential index based on the two allele characters 
		 * @param c1   A, C, G, T
		 * @param c2   A, C, G, T
		 * @return  allele's sequential index: 0..15
		 */
		public int getAlleleIndex(char c1, char c2)
		{
			boolean bAllowSNPSwap = true; // if true, Dd == dD
			if (bAllowSNPSwap && c1 > c2)
			{// CA => AC
				char c = c1; c1 = c2; c2 = c;
			}
			int index1 = (c1 == 'A' ? 0 : c1 == 'C' ? 1 : c1 == 'G' ? 2 : 3);
			int index2 = (c2 == 'A' ? 0 : c2 == 'C' ? 1 : c2 == 'G' ? 2 : 3);
			return 4 * index1  + index2;
		}

		/**
		 * Returns the allele character 
		 * @param index		allele's sequential index: 0 .. 15
		 * @return			associated character string: AA, AC, ..., TG, TT
		 */
		public static String getAlleleString(int index)
		{
			if (index >= 0 && index < s_AlleleStrings.length)
				return s_AlleleStrings[index];
			return null;
		}

		public int getIndividualAllele(int id, int snp)
		{
			return getAlleleIndex(m_GenoType[id].m_SNP[snp * 2], m_GenoType[id].m_SNP[snp * 2 + 1]);
		}

		
		public void readSNPMap(String filename)
		{
			String strLine = null;
			int iSNPIndex = 0;
			String[] tokens = null;
			try {
				BufferedReader input =  new BufferedReader(new FileReader(filename));
				ArrayList<SNPInfo> snpMap = new ArrayList<SNPInfo>();
				m_SNPHash = new HashMap<String, Integer>();
				// read the header
		        while (( strLine = input.readLine()) != null) {
		        	tokens = strLine.split("[\t ]+");// space or tab
		        	SNPInfo sm = new SNPInfo();
		        	sm.m_Gene = Integer.valueOf(tokens[0]);
		        	sm.m_Name = tokens[1];
		        	sm.m_Position = Integer.valueOf(tokens[3]);
		        	snpMap.add(sm);
		        	m_SNPHash.put(sm.m_Name, new Integer(iSNPIndex));
		        	iSNPIndex++;
		        }
		        m_SNPInfo = new SNPInfo[snpMap.size()];
		        m_SNPInfo = snpMap.toArray(m_SNPInfo);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public void readPhenoType(String filename)
		{
			try {
				BufferedReader input =  new BufferedReader(new FileReader(filename));
				ArrayList<PhenoType> phenoTypes = new ArrayList<PhenoType>();
				// read the header
				String strLine = input.readLine();
				String[] tokens = strLine.split("[\t ]+");// space or tab
				m_PhenoTypeName = new String[tokens.length - PhenoType.NUM_INFO_COLUMNS];

				for (int i = 0; i < m_PhenoTypeName.length; i++)
				{
					m_PhenoTypeName[i] = tokens[i + PhenoType.NUM_INFO_COLUMNS];
				}
				
				m_GroupInfo = new GroupInfo[m_PhenoTypeName.length + 1];
				for (int i = 0; i <= m_PhenoTypeName.length; i++)
				{
					m_GroupInfo[i] = new GroupInfo();
					m_GroupInfo[i].m_ConsensusAllele = new int[GroupInfo.NUM_GROUPS][getNumSNPs()];
				}
				
		        while (( strLine = input.readLine()) != null)
		        {
		        	tokens = strLine.split("[\t ]+");// space or tab
		        	PhenoType p = new PhenoType();
		        	p.m_Family = Integer.valueOf(tokens[0]);
		        	p.m_Person = Integer.valueOf(tokens[1]);
		        	p.m_Affection = Integer.valueOf(tokens[2]);
		        	p.m_ExpressionLevel = new double[getNumPhenoTypes()];
		        	p.m_NumConsensus = new double[getNumPhenoTypes()];
					for (int i = 0; i < getNumPhenoTypes(); i++)
					{
						p.m_ExpressionLevel[i] = Double.valueOf(tokens[i + PhenoType.NUM_INFO_COLUMNS]);
					}
					phenoTypes.add(p);
		        }
		        m_PhenoType = new PhenoType[phenoTypes.size()];
		        m_PhenoType = phenoTypes.toArray(m_PhenoType);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void readGenoType(String filename)
		{
			try {
				BufferedReader input =  new BufferedReader(new FileReader(filename));
				ArrayList<GenoType> genoTypes = new ArrayList<GenoType>();
				// read the header
				String strLine = null;
				
		        while (( strLine = input.readLine()) != null) {
		        	String[] tokens = strLine.split("[\t ]+");// space or tab
		        	GenoType g = new GenoType();
		        	g.m_Family = Integer.valueOf(tokens[0]);
		    		g.m_Person = Integer.valueOf(tokens[1]);
		    		g.m_Father = Integer.valueOf(tokens[2]);
		    		g.m_Mother = Integer.valueOf(tokens[3]);
		    		g.m_Sex = Integer.valueOf(tokens[4]);
		        	g.m_SNP = new char[tokens.length - 6];
		        	for (int i = 0; i < tokens.length - 6; i++)
		        	{
		        		g.m_SNP[i] = tokens[i + 6].charAt(0);  
		        	}
					genoTypes.add(g);
		        }
		        m_GenoType = new GenoType[genoTypes.size()];
		        m_GenoType = genoTypes.toArray(m_GenoType);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		int BONUS_SNP_INDEX = 2476 - 1;
		int BONUS_INDIVIDUAL_INDEX = 425 - 1;
		boolean OUTPUT_BONUS_STATS = false;

		/**
		 * 	Calculates the common SNPs based on the grouping for each of phenotypes
		 *  Should be called after calling readSNPMap and readGenoType
		 */
		public void calcAllImportantSNPs()
		{
			OUTPUT_BONUS_STATS = false;
			BONUS_SNP_INDEX = getSNPIndex("rs1366462");
			if (OUTPUT_BONUS_STATS)
			{//DEBUG:
				System.out.printf("Individual[%d].snp[%d] = '%c%c'\n", BONUS_INDIVIDUAL_INDEX+1, BONUS_SNP_INDEX+1,
						m_GenoType[BONUS_INDIVIDUAL_INDEX].m_SNP[BONUS_SNP_INDEX * 2],
						m_GenoType[BONUS_INDIVIDUAL_INDEX].m_SNP[BONUS_SNP_INDEX * 2 + 1]);
			}

			for (int iPh = 0; iPh <= getNumPhenoTypes(); iPh++)
			{
				calcImportantSNPs(iPh);
			}
		}
		
		public void calcImportantSNPs(int iPh)
		{
			int[] iGroupIds = computeGroups(iPh);
			calcSNPRatio(iPh, iGroupIds, null);
			
			boolean bSecondaryRatios = false;
			if (bSecondaryRatios && iPh < getNumPhenoTypes())
			{
				double[] ratios = new double[getNumSNPs()];
				double[] ratiosMax = new double[getNumSNPs()];
				for (int snp = 0; snp < ratios.length; snp++)
				{
					ratios[snp] = m_SNPInfo[snp].m_RatioLow[iPh];
				}
				int[] sorted = FloatIndexer.sortFloatsRev(ratios); // sort high to low
				int NUM_IMPORTANT = 10;
//				int[]   alleleCount   = new int[NUM_ALLELE]; // count the total occurrence of each allele in this SNP
//				double[] alleleMean   = new double[NUM_ALLELE];
//				double[] alleleStdDev = new double[NUM_ALLELE];
				int[] iNewGroupIds = new int[getNumIndividuals()];
				for (int s = 0; s < NUM_IMPORTANT; s++)
				{
					int snp = sorted[s];
					Arrays.fill(iNewGroupIds, 0);
//					calcAlleleFreq(iPh, snp, alleleCount, alleleMean, alleleStdDev);
					int iNumG1 = 0;
					int iNumG2 = 0;
					for (int id = 0; id < getNumIndividuals(); id++)
					{
						int g1 = iGroupIds[id];
						if (g1 == 0)
							continue;
						int g2 = g1 == 1 ? 2 : 1;
						int a = getIndividualAllele(id, snp); 
						if (a == m_GroupInfo[iPh].m_ConsensusAllele[g2][snp])
						{// individuals that have the consenses allele of the other group
							iNewGroupIds[id] = 2;
							iNumG2++;
						}
						else if (a == m_GroupInfo[iPh].m_ConsensusAllele[g1][snp])
						{
							iNewGroupIds[id] = 1;
							iNumG1++;
						}
					}
					
					if (iNumG1 > 10 && iNumG2 > 10)
					{
						calcSNPRatio(iPh, iNewGroupIds, ratios);
						double maxRatio = Double.NEGATIVE_INFINITY;
						int maxSNP = -1;
						for (int s1 = 0; s1 < ratios.length; s1++)
						{
							ratiosMax[s1] = Math.max(ratios[s1], ratiosMax[s1]);
							if (ratiosMax[s1] > maxRatio && s1 != getSNPIndex("rs1366462"))
							{
								maxSNP = s1;
								maxRatio = ratiosMax[s1];
							}
						}
						
//						if (maxSNP == getSNPIndex("rs10942224"))//"rs1366462"))
//						{
//							for (int g2 = 0; g2 < 3; g2++)
//								for (int g1 = 0; g1 < 3; g1++)
//								{
//									System.out.println("---------------------------(" + g1 + ", " + g2 + ")");
//									for (int id = 0; id < getNumIndividuals(); id++)
//									{
//										if (iGroupIds[id] == g1 && iNewGroupIds[id] == g2)
//										{
//											System.out.println(getAlleleString(getIndividualAllele(id, snp)) +" "+ 
//															   getAlleleString(getIndividualAllele(id, maxSNP)) + "    "+
//															   m_PhenoType[id].m_ExpressionLevel[iPh]);
//										}
//									}
//								}
//						}
					}
				}
				
				for (int snp = 0; snp < ratios.length; snp++)
				{
					m_SNPInfo[snp].m_RatioHigh[iPh] = ratiosMax[snp];
				}
			}
			
			boolean bAverageRatios = false;
			if (bAverageRatios && iPh < getNumPhenoTypes())
			{
				double dLow  = m_GroupInfo[iPh].m_dThresholdLow;
				double dHigh = m_GroupInfo[iPh].m_dThresholdHigh;
				double[] ratios = new double[getNumSNPs()];
				double[] ratiosAll = new double[getNumSNPs()];
				for (int i = 2; i <= 4; i++)
				{
					m_GroupInfo[iPh].m_dThresholdLow = i*0.1;
					m_GroupInfo[iPh].m_dThresholdHigh = 1.0 - i*0.1;
					iGroupIds = computeGroups(iPh);
					calcSNPRatio(iPh, iGroupIds, ratios);
					for (int snp = 0; snp < ratios.length; snp++)
					{
						ratiosAll[snp] += ratios[snp];
					}
				}
				m_GroupInfo[iPh].m_dThresholdLow = dLow;
				m_GroupInfo[iPh].m_dThresholdHigh = dHigh;
				computeGroups(iPh);
	
				for (int snp = 0; snp < ratios.length; snp++)
				{
					m_SNPInfo[snp].m_RatioHigh[iPh] = ratiosAll[snp] / 3.0;
				}
			}
		}

		
		/**
		 *  Calculates the histogram of the individuals based on their expression levels for each of the phenotypes. stores the results in m_GroupInfo
		 */
		public void calcExpressionHist(int iPheno)
		{
			m_GroupInfo[iPheno].m_dMinEx = Double.MAX_VALUE;
			m_GroupInfo[iPheno].m_dMaxEx = Double.NEGATIVE_INFINITY;
			for (int id = 0; id < getNumIndividuals(); id++)
			{// iterate through individuals
				m_GroupInfo[iPheno].m_dMinEx = Math.min(m_PhenoType[id].m_ExpressionLevel[iPheno], m_GroupInfo[iPheno].m_dMinEx);
				m_GroupInfo[iPheno].m_dMaxEx = Math.max(m_PhenoType[id].m_ExpressionLevel[iPheno], m_GroupInfo[iPheno].m_dMaxEx);
			}
			double rangeEx =  m_GroupInfo[iPheno].m_dMaxEx - m_GroupInfo[iPheno].m_dMinEx;
			for (int id = 0; id < getNumIndividuals(); id++)
			{// iterate through individuals: 1=low  2=high  0=medium
				double r = (m_PhenoType[id].m_ExpressionLevel[iPheno] - m_GroupInfo[iPheno].m_dMinEx) / rangeEx;
				m_GroupInfo[iPheno].m_Hist[Math.min((int)(r * GroupInfo.NUM_HIST_BINS), GroupInfo.NUM_HIST_BINS - 1)]++;
			}
		}
		
		/**
		 * Computes the group ids for individuals based on their expression level for a certain phenotype.
		 * @param iPheno	index of the phenotype. 0..getNumPhenoTypes() - 1: expression. getNumPhenotypes() : affection
		 * @return			int[] of size: getNumIndividuals(), groups id for each individual
		 */
		public int[] computeGroups(int iPheno)
		{
			assert(m_PhenoType != null && m_SNPInfo != null && m_GenoType != null && m_GenoType[0].m_SNP.length == getNumSNPs() * 2);
			
			// group each individual into one of 3 groups: 
			// 0: medium expression 
			// 1: low expression / non-affected
			// 2: high expression / affected
			int iGroupIds[] = new int[getNumIndividuals()];
			
			boolean bGroupByAffection = (iPheno == getNumPhenoTypes()); 
			if (bGroupByAffection)
			{// group individuals based on affection
				for (int id = 0; id < getNumIndividuals(); id++)
				{
					iGroupIds[id] = m_PhenoType[id].m_Affection;
				}
			}
			else
			{// group the individuals based on the expression level of a gene
				calcExpressionHist(iPheno);
				double rangeEx =  m_GroupInfo[iPheno].m_dMaxEx - m_GroupInfo[iPheno].m_dMinEx;
				for (int id = 0; id < getNumIndividuals(); id++)
				{// iterate through individuals: 1=low  2=high  0=medium
					double r = (m_PhenoType[id].m_ExpressionLevel[iPheno] - m_GroupInfo[iPheno].m_dMinEx) / rangeEx;
					iGroupIds[id] = (r <= m_GroupInfo[iPheno].m_dThresholdLow) ? 1 : (r >= m_GroupInfo[iPheno].m_dThresholdHigh) ? 2 : 0;
				}
			}			
		
			//int iGroupSize[] = new int[NUM_GROUPS]; // number of individuals within each group
			Arrays.fill(m_GroupInfo[iPheno].m_Size, 0);
			
			for (int id = 0; id < getNumIndividuals(); id++)
			{// iterate through individuals
				m_GroupInfo[iPheno].m_Size[iGroupIds[id]]++;
				if (iPheno < getNumPhenoTypes())
					m_PhenoType[id].m_NumConsensus[iPheno] = 0;
			}
			return iGroupIds;
		}
		
		public void calcAlleleFreq(int iPheno, int snp, int[] alleleCount, double[] alleleMean, double[] alleleStdDev)
		{
			//int[]   alleleCount   = new int[NUM_ALLELE]; // count the total occurrence of each allele in this SNP
			//double[] alleleMean   = new double[NUM_ALLELE];
			//double[] alleleStdDev = new double[NUM_ALLELE];
			//for (int snp = 0; snp < getNumSNPs(); snp++) 
			{// iterate through SNPs 
				Arrays.fill(alleleMean, 0);
				Arrays.fill(alleleStdDev, 0);
				for (int id = 0; id < getNumIndividuals(); id++)
				{// iterate through individuals
					int alleleIndex = getIndividualAllele(id, snp);
					alleleCount[alleleIndex]++;
					if (iPheno < getNumPhenoTypes())
					{
						alleleMean[alleleIndex] += m_PhenoType[id].m_ExpressionLevel[iPheno];
					}
					else
					{
						alleleMean[alleleIndex] += (1.5 - m_PhenoType[id].m_Affection);
					}
				}
				
				for (int a = 0; a < NUM_ALLELE; a++)
				{
					if (alleleCount[a] > 0)
					{
						alleleMean[a] /= alleleCount[a];
						//dMinAlleleFreq = Math.min(alleleMean[a], dMinAlleleFreq);
						//dMaxAlleleFreq = Math.max(alleleMean[a], dMaxAlleleFreq);
					}
				}
				for (int id = 0; id < getNumIndividuals(); id++)
				{// iterate through individuals
					int alleleIndex = getIndividualAllele(id, snp);
					double diff = 0; 
					if (iPheno < getNumPhenoTypes())
					{
						diff = m_PhenoType[id].m_ExpressionLevel[iPheno] - alleleMean[alleleIndex];
					}
					else
					{
						diff = (1.5 - m_PhenoType[id].m_Affection) - alleleMean[alleleIndex];
					}
					alleleStdDev[alleleIndex] += (diff*diff);
				}
				
//				double dMinAlleleMean = Double.MAX_VALUE;
//				double dMaxAlleleMean = Double.NEGATIVE_INFINITY;
//				double dMinAlleleStdDev = Double.MAX_VALUE;
//				double dMaxAlleleStdDev = Double.NEGATIVE_INFINITY;
//				for (int a = 0; a < NUM_ALLELE; a++)
//				{
//					if (alleleCount[a] > 0 && alleleMean[a] < dMinAlleleMean)
//					{
//						dMinAlleleMean = alleleMean[a];
//						dMinAlleleStdDev = Math.sqrt(alleleStdDev[a] / alleleCount[a]);
//					}
//					if (alleleCount[a] > 0 && alleleMean[a] > dMaxAlleleMean)
//					{
//						dMaxAlleleMean = alleleMean[a];
//						dMaxAlleleStdDev = Math.sqrt(alleleStdDev[a] / alleleCount[a]);
//					}
//				}
			}
		}
		
		public void calcSNPRatio(int iPheno, int iGroupIds[], double[] output)
		{
			boolean bGroupByAffection = (iPheno == getNumPhenoTypes());
			// these are used inside the snp code, and pulled out of the loop for performance
			int[]   alleleCount      = new int[NUM_ALLELE]; // count the total occurrence of each allele in this SNP
			int[][] groupAlleleCount = new int[GroupInfo.NUM_GROUPS][NUM_ALLELE]; // count the occurrence of each allele for each group type
			int[] iGroupConsensusAllele = new int[GroupInfo.NUM_GROUPS]; // index of consensus allele for a group
			// (log) odds of occurrence of consensus allele of a group in the other groups
			double alleleOdds[][] =  new double[GroupInfo.NUM_GROUPS][GroupInfo.NUM_GROUPS]; 
			
			for (int snp = 0; snp < getNumSNPs(); snp++) 
			{// iterate through SNPs 
				Arrays.fill(alleleCount, 0);
				Arrays.fill(groupAlleleCount[0], 0);
				Arrays.fill(groupAlleleCount[1], 0);
				Arrays.fill(groupAlleleCount[2], 0);
				
				for (int id = 0; id < getNumIndividuals(); id++)
				{// iterate through individuals
					int alleleIndex = getIndividualAllele(id, snp);
					alleleCount[alleleIndex]++;
					groupAlleleCount[iGroupIds[id]][alleleIndex]++;
				}
				
				if (OUTPUT_BONUS_STATS && snp == BONUS_SNP_INDEX)
					System.out.printf("------------\n GENE = %d\n------------\n", iPheno + 1);
				
				int iConsensus = 0; // index of consensus (allele with maximum occurrence)  across all individuals
				Arrays.fill(iGroupConsensusAllele, 0);
				for (int a = 0; a < NUM_ALLELE; a++)
				{
					iConsensus = alleleCount[a] > alleleCount[iConsensus] ? a : iConsensus;
					for (int g = 0; g < GroupInfo.NUM_GROUPS; g++)
					{
						iGroupConsensusAllele[g] = groupAlleleCount[g][a] > groupAlleleCount[g][iGroupConsensusAllele[g]] ? a : iGroupConsensusAllele[g];
						
						if (output == null)
						{
							m_GroupInfo[iPheno].m_ConsensusAllele[g][snp] = iGroupConsensusAllele[g];
						}
						
						
						if (OUTPUT_BONUS_STATS && snp == BONUS_SNP_INDEX && alleleCount[a] > 0)
							System.out.printf("%%%2d ", m_GroupInfo[iPheno].m_Size[g] > 0 ? (100 *groupAlleleCount[g][a] / m_GroupInfo[iPheno].m_Size[g]): 0);
					}
					if (OUTPUT_BONUS_STATS && snp == BONUS_SNP_INDEX && alleleCount[a] > 0)
						System.out.printf("---> %s\n", s_AlleleStrings[a]);
				}
				
				for (int g1 = 0; g1 < GroupInfo.NUM_GROUPS; g1++)
				{
					for (int g2 = 0; g2 < GroupInfo.NUM_GROUPS; g2++)
					{
						// calculates odds:
						//alleleOdds[g1][g2] = ((groupAlleleCount[g2][iGroupConsensusAllele[g1]] + 1) / (m_GroupInfo[iPheno].m_Size[g2] - groupAlleleCount[g2][iGroupConsensusAllele[g1]] + 1)); // +1 to avoid division by 0
						// calculates probability:
						alleleOdds[g1][g2] = (groupAlleleCount[g2][iGroupConsensusAllele[g1]] + 1.0);// / (m_GroupInfo[iPheno].m_Size[g2] + 1);//- groupAlleleCount[g2][iGroupConsensusAllele[g1]] + 1)); // +1 to avoid division by 0
					}
				}

				double rLowRatio  = Math.log10(alleleOdds[1][1]) - Math.log10(alleleOdds[1][2]);
				double rHighRatio = Math.log10(alleleOdds[2][2]) - Math.log10(alleleOdds[2][1]);
				
				double rLow = rLowRatio;//(alleleOdds[1][1]/ (m_GroupInfo[iPheno].m_Size[1] + 1));
				double rHigh = rHighRatio;//(alleleOdds[2][2]/ (m_GroupInfo[iPheno].m_Size[2] + 1));
				//rLow += rHigh;
				//double rLow = dMinAlleleMean;
				//double rHigh= dMaxAlleleMean;
				
				rLowRatio = (rLowRatio + rHighRatio) ;/// Math.sqrt(2.0);
//				rHighRatio = 
//					2*Math.log10(alleleOdds[1][1]) +
//					2*Math.log10(alleleOdds[2][2]) +
//					2*Math.log10(alleleOdds[0][0]) -
//					Math.log10(alleleOdds[1][2]) -
//					Math.log10(alleleOdds[1][0]) -
//					Math.log10(alleleOdds[2][1]) -
//					Math.log10(alleleOdds[2][0]) -
//					Math.log10(alleleOdds[0][1]) -
//					Math.log10(alleleOdds[0][0]);

				//double rLowRatio = alleleOdds[1][1] / alleleOdds[1][2];
				//double rHighRatio= alleleOdds[2][2] / alleleOdds[2][1];
				//if (rLowRatio < 1.0)
				//	rLowRatio = -1.0/rLowRatio;
				//if (rHighRatio < 1.0)
				//	rHighRatio = -1.0/rHighRatio;
				
				if (output != null)
				{
					output[snp] = rLowRatio;
				}
				else
				{
					m_SNPInfo[snp].m_CommonSNP = s_AlleleStrings[iConsensus];
					m_SNPInfo[snp].m_CommonSNPAff1 = s_AlleleStrings[iGroupConsensusAllele[1]];
					m_SNPInfo[snp].m_CommonSNPAff2 = s_AlleleStrings[iGroupConsensusAllele[2]];
					m_SNPInfo[snp].m_RatioAll = (1.0 * alleleCount[iConsensus]) / getNumIndividuals();
	
					if (bGroupByAffection)
					{
						m_SNPInfo[snp].m_CountAff1 = rLow;
						m_SNPInfo[snp].m_CountAff2 = rHigh;
						m_SNPInfo[snp].m_RatioAff1 = rLowRatio;
						m_SNPInfo[snp].m_RatioAff2 = rHighRatio;
					}
					else
					{
						if (m_SNPInfo[snp].m_RatioLow == null)
							m_SNPInfo[snp].m_RatioLow = new double[getNumPhenoTypes()];
						m_SNPInfo[snp].m_RatioLow[iPheno] = rLowRatio;
						
						if (m_SNPInfo[snp].m_RatioHigh == null)
							m_SNPInfo[snp].m_RatioHigh = new double[getNumPhenoTypes()];
//						m_SNPInfo[snp].m_RatioHigh[iPheno] = rHighRatio;
						
						if (m_SNPInfo[snp].m_CountLow == null)
							m_SNPInfo[snp].m_CountLow = new double[getNumPhenoTypes()];
						m_SNPInfo[snp].m_CountLow[iPheno] = rLow;
						
						if (m_SNPInfo[snp].m_CountHigh == null)
							m_SNPInfo[snp].m_CountHigh = new double[getNumPhenoTypes()];
						m_SNPInfo[snp].m_CountHigh[iPheno] = rHigh;
						
						final double rMinRatioCutOff = 2.0;
						final double rMinPercentageCutOff = 0.3;
						
						for (int id = 0; id < getNumIndividuals(); id++)
						{// count the percentage of consensus each individual has
							int g = iGroupIds[id];
							int gg = (g == 1 ? 2 : g == 2 ? 1 : 0);
							//if (oddsRatio[g][g] > rMinPercentageCutOff && oddsRatio[g][g] / oddsRatio[g][gg] > rMinRatioCutOff)
							if ((g == 1 && rLowRatio > 0.1) || (g == 2 && rHighRatio > 0.5))
							{
								if (getIndividualAllele(id, snp) == iGroupConsensusAllele[g])
								{
									m_PhenoType[id].m_NumConsensus[iPheno] += 1;
								}
							}
						}
					}
				}
			} // (for snp
			
		}

		void readResultSingle(String[] filenames)
		{
			m_ResultSingle = new ResultSingle[filenames.length][];
			for (int f = 0; f < filenames.length; f++)
			{
				BufferedReader input;
				try {
					input = new BufferedReader(new FileReader(filenames[f]));
					ArrayList<ResultSingle> results = new ArrayList<ResultSingle>();
			        String strLine = null;
			        int row = 0;
					while ((strLine = input.readLine()) != null) {
			        	String[] tokens = strLine.split("[\t ]+");// space or tab
			        	if (row == 0 && tokens[1].equalsIgnoreCase("CHR"))
			        		continue; // header
			        	
			        	ResultSingle r = new ResultSingle();
			        	r.m_PhenoIndex = Integer.valueOf(tokens[1]);
			        	r.m_SNPIndex = row;
			        	if (tokens[5].equals("NA"))
			        	{
				    		r.m_Beta	= 0;
				    		r.m_SE 		= 0;
				    		r.m_R2 		= 0;
				    		r.m_T 		= 0;
				    		r.m_P 		= 0;
			        	}
			        	else
			        	{
				        	//r.m_BasePair = Integer.valueOf(tokens[3]);
				    		r.m_Beta	= Double.valueOf(tokens[5]);
				    		r.m_SE 		= Double.valueOf(tokens[6]);
				    		r.m_R2 		= Double.valueOf(tokens[7]);
				    		r.m_T 		= Double.valueOf(tokens[8]);
				    		r.m_P 		= Double.valueOf(tokens[9]);
			        	}
						results.add(r);
						row++;
			        }
					m_ResultSingle[f] = new ResultSingle[results.size()];
					m_ResultSingle[f] = results.toArray(m_ResultSingle[f]);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		public void processAllFiles(String path)
		{
			String s_ResultFileExt[] = {"CDH1",	"CDH10", "CDH11", "CDH19", "PCDH1", "PCDH10", "PCDH17", "PCDH19", "PCDH8", "CDH2", "CDH22", "CDH5", "CDH6", "CDH7", "CDH9"};

			readSNPMap(path + "/data/sim.map");
			readPhenoType(path + "/data/pheno.dat");
			readGenoType(path + "/data/sim.ped");
			calcAllImportantSNPs();
			
			String[] singleResultFiles = new String[s_ResultFileExt.length];
			for (int f = 0; f < singleResultFiles.length; f++)
			{
				singleResultFiles[f] = path + "/results/single_locus_results/qassoc." + (f+1) + "." + s_ResultFileExt[f];
			}
			readResultSingle(singleResultFiles);
		}
	}
	

	public static class EQTLPanel extends JPanel implements ActionListener
	{
		private static final long serialVersionUID = 2197937831621201501L;
		
		JTextArea	m_TextPath			= null;
		JButton		m_ButtonPath			= null;
		JButton 	m_ButtonGenerate 		= null;
		JButton 	m_ButtonSave			= null;
		JCheckBox 	m_CheckCorrelation		= null;
		JComboBox   m_ComboDataType			= null;

		MemoryTable m_InternalTable = null;
		Expression 	m_Exp = null;
		MemoryTable m_MemTable = null;
		boolean 	block = false;
		
		EQTLData	m_EQTLData = null;
		String 		m_Path = "/Users/hyounesy/SFU/research/BioVis/eQTL/contest_dataset_2011_v2";
		//String		m_Path = "/Users/hyounesy/SFU/Research/BioVis/eQTL/contest_dataset_2011_v2/contest_dataset_2011_v2";

		//String 		m_Path = "/Users/hyounesy/Desktop/BioVisContest/biovis_2011_contest_demonstration/dataset.easiest";
		//String		m_Path = "/Users/hyounesy/SFU/Research/BioVis/eQTL/biovis_2011_contest_demonstration/biovis_2011_contest_demonstration/dataset.easiest";
		
		OutputType	m_OutputType = OutputType.PHENO_TYPE;

		public EQTLPanel(MemoryTable intTab)
		{
			super();
			m_InternalTable 	= intTab;
			
			this.setBorder(	BorderFactory.createEmptyBorder(10, 10, 10, 10));
			this.setLayout(new BorderLayout(5,5));
			JPanel masterPanel = new JPanel(new GridLayout(8, 1, 5, 5)); // (rows, cols, hgap, vgap)
			this.add(masterPanel, BorderLayout.CENTER);
			
			m_TextPath	= new JTextArea(m_Path);
			m_ButtonPath = new JButton("Select Path");
			masterPanel.add(m_TextPath);
			masterPanel.add(m_ButtonPath);
			
			m_ComboDataType  = EnumUtils.getComboBox(OutputType.values(), m_OutputType, "Output Type", this);
			masterPanel.add(new JLabel("Correlated: "));
			masterPanel.add(m_ComboDataType);
			

			m_CheckCorrelation = new JCheckBox("");
			m_CheckCorrelation.setSelected(false);
			masterPanel.add(new JLabel("Correlated: "));
			masterPanel.add(m_CheckCorrelation);
			
			m_ButtonGenerate 	= new JButton( "Generate" );
			m_ButtonGenerate.addActionListener(this);
			masterPanel.add(m_ButtonGenerate);			
			
			m_ButtonSave	= new JButton( "Save" );
			m_ButtonSave.addActionListener(this);
			masterPanel.add(m_ButtonSave);			
			
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			if( e.getSource() == m_ButtonGenerate )
			{
				m_Path = m_TextPath.getText();
				m_OutputType = OutputType.values()[m_ComboDataType.getSelectedIndex()];
				
				m_EQTLData = new EQTLData();
				m_EQTLData.processAllFiles(m_Path);
				// make a new table
				if (m_OutputType == OutputType.PHENO_TYPE)
				{
					m_MemTable = EQTLTableFactory.createPhenoTable(m_EQTLData);
				}
				else
				{
					m_MemTable = EQTLTableFactory.createGenoTable(m_EQTLData);
				}
				
				if( m_MemTable != null )
				{
					m_MemTable.setInputControl( new EQTLPanel(m_MemTable) );
				}
		    	block = true;
		    	
				for ( TableListener tl : m_InternalTable.getTableListeners() ) {
					
					if (tl instanceof Expression) {
						
						m_Exp = (Expression)tl;							
					}
				}
				
				SwingUtilities.invokeLater(new Runnable() {
			        public void run() {
			        	
			        	try {
			        		
			        		while( block ) {
			        			Thread.sleep(1000);
			        		}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						
			        	m_Exp.setTable(m_MemTable);
			        	m_InternalTable = m_MemTable;
			        }
			      });
				// inform the expression
				block = false;
			}
			else if( e.getSource() == m_ButtonSave )
			{
				JFileChooser fc = new JFileChooser( );
				int returnVal = fc.showSaveDialog(this);
				if( returnVal == JFileChooser.APPROVE_OPTION )
				{
					saveTableCSV(m_InternalTable, fc.getSelectedFile());
				}
			}

		}
	}
}
