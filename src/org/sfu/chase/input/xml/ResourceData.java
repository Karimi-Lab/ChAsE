package org.sfu.chase.input.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A class to parse and store Epigenome Atlas
 * data and region files.
 * 
 * @author Cydney Nielsen
 *
 */
public class ResourceData {
	
	// tags extracted from the XML
	protected static String URL_ROOT_TAG = "URLRoot";
	protected static String ATLAS_ROOT_TAG = "AtlasURL";
	protected static String ENCODE_ROOT_TAG = "EncodeURL";
	protected static String REGIONS_ROOT_TAG = "RegionsURL";
	protected static String PROCESSED_ROOT_TAG = "AtlasProcessedURL";
	protected static String ATLAS_DATA_TAG = "Atlas";
	protected static String ENCODE_DATA_TAG = "Encode";
	protected static String REGIONS_TAG = "Regions";
	protected static String URL_TAG = "URL";
	
	// source XML file
	protected static String ATLAS_SOURCE_FILE = "/resources/epiAtlasFiles.xml";
	
	// parsed items
	protected String m_AtlasUrlRoot;
	protected String m_EncodeUrlRoot;
	protected String m_RegionsUrlRoot;
	protected String m_ProcessedUrlRoot;
	protected List<String> m_AtlasDataFileUrls;
	protected List<String> m_EncodeDataFileUrls;
	protected List<String> m_RegionFileUrls;
	protected String m_AtlasReference = "hg19";
	
	protected Map<String,DefaultMutableTreeNode> m_ResourceMap;
	
	// for selecting tree nodes based on file name - currently only used for region files
	protected Map<String,DefaultMutableTreeNode> m_NodeLookup;
	
	public ResourceData() {
		parseResourceData();
		populateTree();
	}
	
	public List<String> getAtlasDataFileURLs() {
		return m_AtlasDataFileUrls;
	}
	
	public String getAtlasReference() {
		return m_AtlasReference;
	}
	
	public List<String> getEncodeDataFileURLs() {
		return m_EncodeDataFileUrls;
	}
	
	public List<String> getRegionFileURLs() {
		return m_RegionFileUrls;
	}
	
	/**
	 * Constructs a tree from the specified URLs
	 * 
	 * @param type
	 * @return
	 */
	public DefaultMutableTreeNode getResourceTree(String type) {
		return m_ResourceMap.get(type);
	}
	
	public String getAtlasUrlRoot() {
		return m_AtlasUrlRoot;
	}
	
	public String getEncodeUrlRoot() {
		return m_EncodeUrlRoot;
	}
	
	/**
	 * Looks for the tag and gets the text content
	 */
	private String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			Element el = (Element)nl.item(0);
			textVal = el.getFirstChild().getNodeValue();
		}
		return textVal;
	}
	
	/**
	 * Calls getTextValue and returns a int value
	 */
	@SuppressWarnings("unused")
	private int getIntValue(Element ele, String tagName) {
		//in production application you would catch the exception
		return Integer.parseInt(getTextValue(ele,tagName));
	}
	
	public String getProcessedUrlRoot() {
		return m_ProcessedUrlRoot;
	}
	
	public String getRegionsUrlRoot() {
		return m_RegionsUrlRoot;
	}
	
	public DefaultMutableTreeNode lookupNode(String s) {
		if (m_NodeLookup.containsKey(s)) {
			return m_NodeLookup.get(s);
		} else {
			return null;
		}
	}
	
	public void parseResourceData() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			//Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();
			//parse using builder to get DOM representation of the XML file
			// TODO: get xml file from web
			InputStream in = getClass().getResourceAsStream(ATLAS_SOURCE_FILE);
			Document dom = db.parse(in);
			//get the root element
			Element docEle = dom.getDocumentElement();
			parseURLRoots(docEle);
			parseDataTags(docEle);
			parseRegionTags(docEle);
			populateTree();
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (SAXException se) {
			se.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	protected void parseDataTags(Element docEle) {
		// get a nodelist of Atlas Data elements
		NodeList nl = docEle.getElementsByTagName(ATLAS_DATA_TAG);
		m_AtlasDataFileUrls = new ArrayList<String>(nl.getLength());
		if (nl != null && nl.getLength() > 0) {
			for (int i = 0 ; i < nl.getLength(); i++) {
				Element el = (Element)nl.item(i);
				String path = getTextValue(el, URL_TAG);
				m_AtlasDataFileUrls.add(path);
			}
		}
		Collections.sort(m_AtlasDataFileUrls);
		
		// get a nodelist of Encode Data elements
		nl = docEle.getElementsByTagName(ENCODE_DATA_TAG);
		m_EncodeDataFileUrls = new ArrayList<String>(nl.getLength());
		if (nl != null && nl.getLength() > 0) {
			for (int i = 0 ; i < nl.getLength(); i++) {
				Element el = (Element)nl.item(i);
				String path = getTextValue(el, URL_TAG);
				m_EncodeDataFileUrls.add(path);
			}
		}
		Collections.sort(m_EncodeDataFileUrls);
	}
	
	protected void parseRegionTags(Element docEle) {
		// get a nodelist of Region elements
		NodeList nl = docEle.getElementsByTagName(REGIONS_TAG);
		m_RegionFileUrls = new ArrayList<String>(nl.getLength());
		if (nl != null && nl.getLength() > 0) {
			for (int i = 0 ; i < nl.getLength(); i++) {
				Element el = (Element)nl.item(i);
				String path = getTextValue(el, URL_TAG);
				m_RegionFileUrls.add(path);
			}
		}
		Collections.sort(m_RegionFileUrls);	
	}
	
	protected void parseURLRoots(Element docEle) throws IOException {
		NodeList nl = docEle.getElementsByTagName(URL_ROOT_TAG);
		if (nl.getLength() != 1) {
			throw new IOException("ERROR: Unexpected number of " + URL_ROOT_TAG + " tags in XML");
		}
		Element el = (Element)nl.item(0);
		m_AtlasUrlRoot = getTextValue(el, ATLAS_ROOT_TAG);
		m_EncodeUrlRoot = getTextValue(el, ENCODE_ROOT_TAG);
		m_RegionsUrlRoot = getTextValue(el, REGIONS_ROOT_TAG);
		m_ProcessedUrlRoot = getTextValue(el, PROCESSED_ROOT_TAG);
	}
	
	protected void populateTree() {
		m_ResourceMap = new HashMap<String,DefaultMutableTreeNode>();
		m_NodeLookup = new HashMap<String,DefaultMutableTreeNode>();
		DefaultMutableTreeNode topNode = null;
		
		List<String> fNames = m_RegionFileUrls;
		// create the top node
		String[] parts = m_RegionsUrlRoot.split("/");
		// topNode = new DefaultMutableTreeNode(parts[parts.length-1]);
		topNode = new DefaultMutableTreeNode("regions");
		populateTree(fNames, topNode);
		m_ResourceMap.put("regions", topNode);
		
		fNames = m_AtlasDataFileUrls;
		// create the top node
		parts = m_AtlasUrlRoot.split("/");
		topNode = new DefaultMutableTreeNode(parts[parts.length-1]);
		populateTree(fNames, topNode);
		m_ResourceMap.put("atlas", topNode);
		
		fNames = m_EncodeDataFileUrls;
		// create the top node
		parts = m_EncodeUrlRoot.split("/");
		topNode = new DefaultMutableTreeNode(parts[parts.length-1]);
		populateTree(fNames, topNode);
		m_ResourceMap.put("encode", topNode);
	}
	
	@SuppressWarnings("unchecked")
	protected void populateTree(List<String> fNames, DefaultMutableTreeNode topNode) {
		// create tree structure from URLs
		for (String fName: fNames) {
			String[] parts = fName.split("/");
			for (int i=0; i<parts.length; i++) {	
				String p = parts[i];
				DefaultMutableTreeNode newNode; 
				if (topNode != null) {
					// check if this node already exists
					boolean hasNode = false;
					if (topNode.toString().equals(p)) {
						hasNode = true;
					} else {
						Enumeration children = topNode.children();
						DefaultMutableTreeNode nextNode = null;
						while (children.hasMoreElements()) {
							nextNode = (DefaultMutableTreeNode)children.nextElement();
							if (nextNode.toString().equals(p)) {
								hasNode = true;
								break;
							}
						}
						if (hasNode) {
							newNode = nextNode;
						} else {
							newNode = new DefaultMutableTreeNode(p);
							topNode.add(newNode);
							if (p.endsWith("gff")) {
								m_NodeLookup.put(p, newNode);
							}
						}
						topNode = newNode;
					}
				} else {
					System.err.println(this.getClass() + " ERROR: Do no expect a null root");
				}
			}
			topNode = (DefaultMutableTreeNode)topNode.getRoot();
		}
	}
}
