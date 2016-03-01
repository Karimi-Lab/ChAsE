package still.data;

import java.util.ArrayList;

import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;
//import org.rosuda.JRI.REXP;
//import org.rosuda.REngine.Rserve.RConnection;

public class MathLibs
{
	static String s_OutputText;
	//see: http://www.rosuda.org/R/nightly/JavaDoc/org/rosuda/JRI/RMainLoopCallbacks.html
	static class RCallback implements RMainLoopCallbacks
	{
		@Override
		public void rBusy(Rengine arg0, int arg1) {
			// TODO Auto-generated method stub
			System.out.println("RCallback.rBusy("+arg0+","+arg1+")");
		}

		@Override
		public String rChooseFile(Rengine arg0, int arg1) {
			// TODO Auto-generated method stub
			System.out.println("RCallback.rChooseFile("+arg0+","+arg1+")");
			return null;
		}

		@Override
		public void rFlushConsole(Rengine arg0) {
			// TODO Auto-generated method stub
			System.out.println("RCallback.rFlushConsole("+arg0+")");
		}

		@Override
		public void rLoadHistory(Rengine arg0, String arg1) {
			// TODO Auto-generated method stub
			System.out.println("RCallback.rLoadHistory("+arg0+","+arg1+")");
		}

		@Override
		public String rReadConsole(Rengine arg0, String arg1, int arg2) {
			// TODO Auto-generated method stub
			System.out.println("RCallback.rReadConsole("+arg0+","+arg1+","+arg2+")");
			return null;
		}

		@Override
		public void rSaveHistory(Rengine arg0, String arg1) {
			// TODO Auto-generated method stub
			System.out.println("RCallback.rSaveHistory("+arg0+","+arg1+")");
		}

		@Override
		public void rShowMessage(Rengine arg0, String arg1) {
			// TODO Auto-generated method stub
			System.out.println("RCallback.rShowMessage("+arg0+","+arg1+")");
			s_OutputText += arg1 + "\n";
		}

		@Override
		public void rWriteConsole(Rengine arg0, String arg1, int arg2) {
			// TODO Auto-generated method stub
			//System.out.println("RCallback.rWriteConsole(" + arg0 + ", " + arg1 + ", " + arg2 + ")");
			System.out.print(arg1);
			s_OutputText += arg1;// + "\n";
		}
	}

	static Rengine s_REngine = null;
	public static Rengine getREngine()
	{
		return s_REngine;
	}
	
	public static String getOutputText()
	{
		return s_OutputText;
	}
	
	public static void clearOutputText()
	{
		s_OutputText = "";
	}
	

	//	static RConnection s_RConnection = null;
//	public static RConnection getRConnection()
//	{
//		return s_RConnection;
//	}

	public static boolean initR()
	{
		boolean bDisableR = true;
		if (bDisableR)
			return false;
		/*
		try
		{
			// > library(Rserve)
			// > Rserve(args="--no-save")
			if (s_RConnection == null)
			{
				s_RConnection = new RConnection();
				org.rosuda.REngine.REXP x = s_RConnection.eval("R.version.string");
				System.out.println(x.asString());
				s_RConnection.eval("m <- replicate(2, rnorm(1000))");
				s_RConnection.eval("plot(m)");
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}*/
		
		try
		{
			try
			{
				if (s_REngine == null)
				{
					final String[] args = {"--save"}; // arguments to be passed to REngine
					s_REngine = new Rengine(args, false, null);
			        if (s_REngine != null)
			        {
						//assert(Rengine.versionCheck());
				        if (!s_REngine.waitForR())
				        {
				        	//assert(false);
				        	s_REngine = null;
				        }
				        else
				        {
				        	s_REngine.addMainLoopCallbacks(new RCallback());
//				        	double d[][] = new double[20][15];
//				        	Rassign("xx",d);
//				        	double d2[][] = s_REngine.eval("xx").asMatrix();
				        	return true;
				        }
			        }
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		catch (Error e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	static void shutdownR()
	{
		if (s_REngine != null)
		{
			s_REngine.end();
		}
	}
	
	static public boolean Rassign(String sym, double[][] val)
	{
		if (val == null || val[0] == null)
			return false;
		
    	s_REngine.eval(sym+"<-matrix(," + val.length + "," + val[0].length + ")");
    	boolean bSuccess = true;
    	for (int i = 0; i < val.length; i++)
    	{
    		bSuccess = s_REngine.assign("LEET1337",val[i]) && bSuccess;
    		s_REngine.eval(sym+"["+(i+1)+",]<-LEET1337");
    	}
    	return bSuccess;
	}
	
	static public boolean Rassign(String sym, Table t)
	{
		if (t == null || t.rows() == 0)
			return false;

		ArrayList<Integer> numericIdxs = Operator.getNumericIndices(t);
		
		if (numericIdxs.size() == 0)
			return false;
		
    	s_REngine.eval(sym+"<-matrix(," + t.rows() + "," + numericIdxs.size() + ")");
    	boolean bSuccess = true;
    	double v[] = new double[numericIdxs.size()];
    	for (int i = 0; i < t.rows(); i++)
    	{
    		for (int j = 0; j < numericIdxs.size(); j++)
    		{
    			v[j] = t.getMeasurement(i, numericIdxs.get(j));
    		}
    		bSuccess = s_REngine.assign("LEET1337",v) && bSuccess;
    		s_REngine.eval(sym+"["+(i+1)+",]<-LEET1337");
    	}
    	return bSuccess;
	}

	static boolean initMatlab()
	{// not implemented
		return false;
	}
	
	static boolean initOctave()
	{//not implemented
		return false;
	}
	

}
