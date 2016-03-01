package still.operators;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.gjt.sp.jedit.syntax.ModeProvider;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

import still.data.Function;
import still.data.Group;
import still.data.Map;
import still.data.MathLibs;
import still.data.Operator;
import still.data.Table;
import still.data.TableEvent;
import still.data.TableFactory;
import still.data.TableEvent.TableEventType;
import still.gui.OPAppletViewFrame;
import still.gui.OperatorView;
import still.gui.PImagePainter;

//TODO: extend this from BasicOp and remove m_bAppendInput and m_NewColumnNames

public class RFuncOp extends Operator implements Serializable
{
	private static final long serialVersionUID = -6874872676907052211L;
	
	/// matrix output of the R command.
	double [][] m_RMatrix 	= null;
	
	/// whether to append the input table
	boolean m_bAppendInput 	= true;
	
	String [] m_NewColumnNames = null;

	/// current R command
	String m_sRFuncString 	= null;
	
	// R working Directory
	String m_sRWorkingDir = ""; 
	
	public RFuncOp( Table newTable, boolean isActive, String paramString )
	{
		this( newTable, isActive );
	}
	
	public RFuncOp( Table newTable, boolean isActive )
	{
		super( newTable );
		//runRFunc();
		this.isActive = isActive;
		if( isActive )
		{
			this.updateMap();
			function = new RFunction(this);
			//this.updateFunction();
			isLazy  		= true;
			setView( new RFuncView( this ) );
		}
		
		try
		{
			m_sRWorkingDir = (new File(System.getProperty("user.dir") + "../../srcR")).getCanonicalPath();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static String getMenuName()
	{
		return "Data:RFunc";
	}

	public String toString()
	{
		return "[Data:RFunc]";
	}
	
	public String getSaveString( ) {
		
		return "";
	}
	
	public void activate()
	{
		this.isActive = true;
		this.updateMap();
		function = new RFunction(this);
		//this.updateFunction();
		isLazy  		= true;
		setView( new RFuncView( this ) );
	}
	
	@Override
	public void updateFunction()
	{
		//runRFunc();
		function = new RFunction(this);
		//((RFuncView)getView()).buildGUI();
	}

	public int rows()
	{
		if (m_bAppendInput)
			return input.rows();
		else if (m_RMatrix != null)
			return m_RMatrix.length;

		return 0;
	}

	@Override
	public String getColName( int dim )
	{
		int newDim = m_bAppendInput ? dim - input.columns() : dim;
		if (newDim < 0)
		{
			return input.getColName(dim);
		}
		else if (m_NewColumnNames != null && newDim < m_NewColumnNames.length)
		{
			return m_NewColumnNames[newDim];
		}
		else
		{
			return super.getColName(dim);
		}
	}
	
	public void setMeasurement( int point_idx, int dim, double value )
	{
		if (m_bAppendInput)
		{
			if (dim >= input.columns())
			{
				if (m_RMatrix != null &&  point_idx < m_RMatrix.length)
				{
					m_RMatrix[point_idx][dim - input.columns()] = value;
				}
			}
			else
			{
				input.setMeasurement(point_idx, dim, value);
			}
		}
		else if (m_RMatrix != null &&  m_RMatrix[0] != null && dim < m_RMatrix[0].length )
		{
			m_RMatrix[point_idx][dim] = value;
		}
	}
	

	public class RFunction implements Function
	{
		private RFuncOp m_Operator = null;
		
		public RFunction(RFuncOp op)
		{
			m_Operator = op;
		}
		
		@Override
		public Table apply(Group group) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double compute(int row, int col) 
		{
			if (m_bAppendInput)
			{
				if (col >= m_Operator.input.columns())
				{
					if (m_Operator.m_RMatrix != null &&  row < m_Operator.m_RMatrix.length)
					{
						return m_Operator.m_RMatrix[row][col - m_Operator.input.columns()];
					}
				}
				else
				{
					return m_Operator.input.getMeasurement(row, col);
				}
			}
			else if (m_Operator.m_RMatrix != null &&  m_Operator.m_RMatrix[0] != null && col < m_Operator.m_RMatrix[0].length )
			{
				return m_Operator.m_RMatrix[row][col];
			}
			return 0;
		}

		@Override
		public Group inverse(Table dims) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double[] invert( Map map, int row, int col, double value )
		{
			double[] ret = new double[1];
			ret[0] = value;
			return ret;
		}

		@Override
		public int[] outMap() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	@Override
	public void updateMap()
	{
		int outdims = (m_RMatrix == null || m_RMatrix.length == 0) ? 0 : m_RMatrix[0].length;
		
		if (m_bAppendInput)
		{
			if (m_RMatrix == null || m_RMatrix.length != input.rows())
				outdims = 0;
			map = Map.fullBipartiteAppend(getNumericIndices(input), 
											getNonNumericIndices( input ),									
											input.columns(),
											outdims);
		}
		else
		{
			ArrayList<Integer> numericIndicesFromOutdims = new ArrayList<Integer>();
			for( int i = 0; i < outdims; i++ )
				numericIndicesFromOutdims.add(i);

			ArrayList<Integer> nonNumericIndicesFromOutdims = new ArrayList<Integer>();
			for( int i = outdims; i < outdims+getNonNumericIndices( input ).size(); i++ )
				nonNumericIndicesFromOutdims.add(i);
			
			map = Map.fullBipartiteExcept( getNumericIndices(input), 
											numericIndicesFromOutdims,			
											getNonNumericIndices( input ),
											nonNumericIndicesFromOutdims,
											input.columns(), outdims + getNonNumericDims(input));
		}
	}
	
/*
	void runRFuncRServe()
	{
		String imagePath = null;//"/Users/hyounesy/specc.png";
		try
		{
	        File tempFile = File.createTempFile("dimstiller", ".png");
	        tempFile.deleteOnExit();
	        //imagePath = tempFile.getPath();
		}
		catch (IOException ex)
		{
		}

		String device = "png"; // device we'll call (this would work with pretty much any bitmap device)
		// connect to Rserve (if the user specified a server at the command line, use it, otherwise connect locally)
		String server = "127.0.0.1"; // allows connecting to a remote R server
		try {
			RConnection c = new RConnection(server);
            org.rosuda.REngine.REXP xp = c.parseAndEval("try("+device+"(filename='" + imagePath + "',width=100,height=100))");// ,width="+width+", height="+height+")"
            if (xp.inherits("try-error")) { // if the result is of the class try-error then there was a problem
                System.err.println("Can't open "+device+" graphics device:\n"+xp.asString());
                // this is analogous to 'warnings', but for us it's sufficient to get just the 1st warning
                org.rosuda.REngine.REXP w = c.eval("if (exists('last.warning') && length(last.warning)>0) names(last.warning)[1] else 0");
                if (w.isString()) System.err.println(w.asString());
                return;
            }
                 
            // ok, so the device should be fine - let's plot - replace this by any plotting code you desire ...
            c.parseAndEval("data(iris); attach(iris); plot(Sepal.Length, Petal.Length, col=unclass(Species)); dev.off()");
			// close RConnection, we're done
			c.close();
        } catch (RserveException rse) { // RserveException (transport layer - e.g. Rserve is not running)
            System.out.println(rse);
        } catch (REXPMismatchException mme) { // REXP mismatch exception (we got something we didn't think we get)
            System.out.println(mme);
            mme.printStackTrace();
        } catch(Exception e) { // something else
            System.err.println("Something went wrong, but it's not the Rserve: "
							   +e.getMessage());
            e.printStackTrace();
        }
	}
*/	
	void runRFunc()//String sRCmd, String sRWorkDir)
	{
		Rengine re = MathLibs.getREngine();
		if (re != null && m_sRFuncString != null)
		{
			re.eval("rm(rfunc)"); // remove any previous rfunc declarations
			re.eval("rcolnames<-function(){}");
			re.eval("setwd('" + m_sRWorkingDir + "')"); // set working directory
			
			String imagePath = null;
			try
			{
		        File tempFile = File.createTempFile("dimstiller", ".png");
		        tempFile.deleteOnExit();
		        imagePath = tempFile.getPath();
			}
			catch (IOException ex)
			{
			}

	        PImagePainter imagePainter = ((PImagePainter)((OPAppletViewFrame)((RFuncView)getView()).getViewFrame()).procApp);
			if (imagePath != null)
			{
				int width = imagePainter.m_MagnifiedViewCoords[2] - imagePainter.m_MagnifiedViewCoords[0];
				int height = imagePainter.m_MagnifiedViewCoords[3] - imagePainter.m_MagnifiedViewCoords[1];
				re.eval("png(filename=\"" + imagePath + "\" ,width="+width+", height="+height+")");
			}
			
			re.eval(m_sRFuncString);
			MathLibs.Rassign("x", input);
			REXP rxp = re.eval("rfunc(x)");
			
			REXP rcolnames = re.eval("rcolnames()");
			m_NewColumnNames = null;
			if (rcolnames != null)
			{
				m_NewColumnNames = rcolnames.asStringArray();
			}
			
			
			getView().getViewFrame().setVisible(false);
			if (imagePath != null)
			{
				re.eval("dev.off()");
			}
			try
			{
				imagePainter.setImage(null);
				File f = new File(imagePath);
				if (f != null && f.exists() && f.length() > 0)
				{
					imagePainter.setImage(imagePath);
					getView().getViewFrame().setVisible(true);
//					JFrame ff = new JFrame("Test image");
//					ff.setSize(new Dimension(200,200));
//		            ff.pack();
//		            ff.setVisible(true);
				}
			}
			catch (Exception e)
			{
			}
			
			if (rxp != null)
			{
				m_RMatrix = rxp.asMatrix();
				if (m_RMatrix == null)
				{
					double [] d = rxp.asDoubleArray();
					if (d != null)
					{
						m_RMatrix = new double[d.length][1];
						for (int j = 0; j < d.length; j++)
						{
							m_RMatrix[j][0] = d[j];
						}
					}
				}
				
//				if (m_RMatrix != null && m_RMatrix.length != input.rows())
//				{
//					m_bAppendInput = false;
//				}
			}
			else
			{
				m_RMatrix = null;
			}
			
			if( this.isActive() )
			{
				SwingUtilities.invokeLater(new Runnable()
				{
			        public void run()
			        {
			        	PImagePainter imagePainter = ((PImagePainter)((OPAppletViewFrame)((RFuncView)getView()).getViewFrame()).procApp); 
			    		imagePainter.redraw();
			        }
				});
			}
		}

		((RFuncView)getView()).setOutputText(MathLibs.getOutputText());
		//updateMap();
        //super.tableChanged( new TableEvent( this, TableEvent.TableEventType.TABLE_CHANGED ));		    
	}

	public void tableChanged( TableEvent te ) 
	{
		if (te.type != TableEventType.ATTRIBUTE_CHANGED)
		{
			runRFunc();
			updateMap();
			updateFunction();
		}
		super.tableChanged(te, true);
		//((RFuncView)getView()).buildGUI();
	}
	
	void paramsChanged()
	{
		//updateMap();
        //super.tableChanged(new TableEvent( this, TableEvent.TableEventType.TABLE_CHANGED ));		    
	}

	public void loadOperatorView()
	{
		setView( new RFuncView( this ) );
	}
	
	public class RFuncView extends OperatorView
	{
		private static final long serialVersionUID = -5919900799213575720L;

//		JCheckBox m_CheckBoxMetaData = null;
		
		RFuncOp m_Operator = null;
		
//		JTextArea m_TextArea = null;
		org.gjt.sp.jedit.textarea.StandaloneTextArea m_TextAreaInput = null;
		JTextArea m_TextAreaOutput = null;
		String	  m_sFilename = null;
		JLabel 	  m_LabelFileName = null;
		JCheckBox m_CheckBoxAppend = null;

		PImagePainter m_ImagePainter = null;
		
		public RFuncView(Operator op)
		{
			super(op);
			m_Operator = (RFuncOp) op;

			m_ImagePainter = new PImagePainter(op);
			vframe = new OPAppletViewFrame("E"+op.getExpressionNumber()+":"+op, m_ImagePainter );			
			vframe.addComponentListener(this);
			vframe.setVisible(false);
			buildGUI();
		}
		
		public void setOutputText(String text)
		{
			if (m_TextAreaOutput != null)
				m_TextAreaOutput.setText(text);
		}
		
		void buildGUI()
		{
			this.removeAll();
			JButton buttonOpen = new JButton("Open ...");
			buttonOpen.setActionCommand("OPEN");
			buttonOpen.addActionListener(this);

			JButton buttonSave = new JButton("Save ...");
			buttonSave.setActionCommand("SAVE");
			buttonSave.addActionListener(this);

			JButton buttonRDir = new JButton("R Work Dir");
			buttonRDir.setActionCommand("RDIR");
			buttonRDir.addActionListener(this);

			JButton buttonRun = new JButton("Run!");
			buttonRun.setActionCommand("RUN");
			buttonRun.addActionListener(this);

			JButton buttonSaveCSV = new JButton("Save CSV...");
			buttonSaveCSV.setActionCommand("SAVECSV");
			buttonSaveCSV.addActionListener(this);

			JPanel panelButtons = new JPanel(new GridLayout(5, 1));
			panelButtons.add(buttonOpen);
			panelButtons.add(buttonSave);
			panelButtons.add(buttonRDir);
			panelButtons.add(buttonRun);
			panelButtons.add(buttonSaveCSV);
			
			if (m_TextAreaInput == null)
			{
				
				m_TextAreaInput = org.gjt.sp.jedit.textarea.StandaloneTextArea.createTextArea();
				org.gjt.sp.jedit.Mode mode = new org.gjt.sp.jedit.Mode("java");
				mode.setProperty("file", System.getProperty("user.dir") + "/../lib/r.xml");
				//mode.setProperty("view.gutter.lineNumbers", "true");
				ModeProvider.instance.addMode(mode);
				m_TextAreaInput.getBuffer().setMode(mode);
				m_TextAreaInput.setText("rfunc <- function(x)\n{\n  0; #x;\n}");
			}
			if (m_TextAreaOutput == null)
			{
				m_TextAreaOutput = new JTextArea();
				 Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
			        //txt.setFont(font);
			        //txt.setForeground(Color.BLUE);
				m_TextAreaOutput.setFont(font);
				//m_TextAreaOutput.setEditable(false);
			}

			if (m_LabelFileName == null)
			{
				m_LabelFileName = new JLabel();
				m_LabelFileName.setText("Untitled");
			}
			m_CheckBoxAppend = new JCheckBox("Append Input");
			m_CheckBoxAppend.setSelected(m_Operator.m_bAppendInput);
			m_CheckBoxAppend.addActionListener(this);

			JPanel panelBottom = new JPanel(new GridLayout(2, 1));
			panelBottom.add(m_CheckBoxAppend);
			
			JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
												  m_TextAreaInput,
												  //new JScrollPane(new JTextArea()),
												  new JScrollPane(m_TextAreaOutput));
			splitPane.setDividerLocation(0.5);    
			splitPane.setOneTouchExpandable(true);
			
			
			this.add(splitPane, BorderLayout.CENTER);
			this.add(panelButtons, BorderLayout.EAST);
			this.add(m_LabelFileName, BorderLayout.NORTH);
			this.add(panelBottom, BorderLayout.SOUTH);
		}
		
		public void actionPerformed(ActionEvent e)
		{
			if( e.getActionCommand().equalsIgnoreCase("OPEN") )
			{
				JFileChooser fc = new JFileChooser();
				fc.setFileFilter(new FileNameExtensionFilter("R Document", "R", "r"));
				
				if (m_sFilename != null)
				{
					fc.setCurrentDirectory(new File(m_sFilename));
				}
				else
				{
					fc.setCurrentDirectory(new File(m_sRWorkingDir));
				}
				
				if( fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION )
				{
					try
					{
						m_sFilename = fc.getSelectedFile().getCanonicalPath();
						Reader reader = new FileReader(m_sFilename);
						//m_TextArea.read(reader, null);
						{
							JTextArea hack = new JTextArea();
							hack.read(reader, null);
							m_TextAreaInput.setText(hack.getText());
						}
					    m_LabelFileName.setText(m_sFilename);
					}
					catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
			else if( e.getActionCommand().equalsIgnoreCase("SAVE") )
			{
				JFileChooser fc = new JFileChooser();
				fc.setFileFilter(new FileNameExtensionFilter("R Document", "R", "r"));
				if (m_sFilename != null)
				{
					fc.setSelectedFile(new File(m_sFilename));
					fc.setCurrentDirectory(new File(m_sFilename));
				}
				else
				{
					fc.setCurrentDirectory(new File(m_sRWorkingDir));
				}
				
				if( fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION )
				{
					try
					{
						m_sFilename = fc.getSelectedFile().getCanonicalPath();
						Writer w = new FileWriter(m_sFilename);
						//m_TextArea.write(w);
						{
							JTextArea hack = new JTextArea();
							hack.setText(m_TextAreaInput.getText());
							hack.write(w);
						}
					    m_LabelFileName.setText(m_sFilename);
					}
					catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
			else if( e.getActionCommand().equalsIgnoreCase("RUN") )
			{
				try
				{
			        File tempfile = File.createTempFile("dimstiller", ".R");
			        tempfile.deleteOnExit();
					Writer w = new FileWriter(tempfile);
					//m_TextArea.write(w);
					{
						JTextArea hack = new JTextArea();
						hack.setText(m_TextAreaInput.getText());
						hack.write(w);
					}
					m_Operator.m_sRFuncString = "source(\""+tempfile.getPath()+"\")"; //m_TextArea.getText();
					MathLibs.clearOutputText();
					//m_Operator.runRFunc();
			        operator.tableChanged( new TableEvent( operator, TableEvent.TableEventType.TABLE_CHANGED ));		    
				}
				catch(Exception ex){
					ex.printStackTrace();
				}
			}
			else if( e.getActionCommand().equalsIgnoreCase("RDIR") )
			{
				try
				{
					JFileChooser fc = new JFileChooser();
					fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					fc.setSelectedFile(new File(m_sRWorkingDir));
					if( fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION )
					{
						m_sRWorkingDir = fc.getSelectedFile().getCanonicalPath();
					}
				}
				catch (IOException ex)
				{
					ex.printStackTrace();
				}
			}
			else if( e.getActionCommand().equalsIgnoreCase("SAVECSV") )
			{
				JFileChooser fc = new JFileChooser();
				fc.setFileFilter(new FileNameExtensionFilter("CSV File", "CSV"));
				if( fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION )
				{
					TableFactory.saveTableCSV(m_Operator, fc.getSelectedFile());
				}
			}			
			else if (e.getSource() == m_CheckBoxAppend)
			{
				m_Operator.m_bAppendInput = m_CheckBoxAppend.isSelected();
				//m_Operator.paramsChanged();
				updateMap();
				m_Operator.tableChanged(new TableEvent( this, TableEvent.TableEventType.TABLE_CHANGED), true); // downstream

			}
		}
	}
}
