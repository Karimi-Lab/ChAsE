package org.sfu.chase.core;

import java.util.Arrays;

public class ClustCombine
{

    public enum CombineOp
    {
        UNION,
        INTERSECT,
        SUBTRACT
    }
    
    public static ClustInfo combine(ClustInfo cInfo1, ClustInfo cInfo2, CombineOp op)
    {
    	if (cInfo1.size() == 0)
    		return cInfo2;
    	else if (cInfo2.size() == 0)
    		return cInfo1;
    	
        int i1 = 0;
        int i2 = 0;
        int maxLength = op == CombineOp.UNION     ? cInfo1.m_Indices.length + cInfo2.m_Indices.length :
                        op == CombineOp.INTERSECT ? Math.min(cInfo1.m_Indices.length, cInfo2.m_Indices.length) :
                        op == CombineOp.INTERSECT ? cInfo1.m_Indices.length : 0;
        int[] newIndices = new int[maxLength];
        int i = 0; // index to the result array
        
        boolean finished1 = cInfo1.m_Indices.length == 0;
        boolean finished2 = cInfo2.m_Indices.length == 0;
        while (!finished1 || !finished2)
        {
            if (!finished1 && !finished2 && cInfo1.m_Indices[i1] == cInfo2.m_Indices[i2])
            {
            	if (op == CombineOp.INTERSECT || op == CombineOp.UNION)
            	{
            		newIndices[i++] = cInfo1.m_Indices[i1];
            	}
            	++i1; ++i2;
            }
            else if (finished2 || (!finished1 && cInfo1.m_Indices[i1] < cInfo2.m_Indices[i2]))
            {
            	if (op == CombineOp.SUBTRACT || op == CombineOp.UNION)
            	{
            		newIndices[i++] = cInfo1.m_Indices[i1];
            	}
            	++i1;
            }
            else if (finished1 || cInfo1.m_Indices[i1] > cInfo2.m_Indices[i2])
            {
            	if (op == CombineOp.UNION)
            	{
            		newIndices[i++] = cInfo2.m_Indices[i2];
            	}
            	++i2;
            }
            
            finished1 = i1 >= cInfo1.m_Indices.length;
            finished2 = i2 >= cInfo2.m_Indices.length;
        }
        
        ClustInfo cInfo = null;
        if (i > 0)
        {
	        cInfo = new ClustInfo();
	        cInfo.m_Indices = Arrays.copyOf(newIndices, i);
        }
        return cInfo;
    }
}
