package heidi.project;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DomUtil {

	public static String GetNodeText(Node node) {
		StringBuffer result = new StringBuffer();
		if (!node.hasChildNodes()) {
			return "";
		}

		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node subnode = list.item(i);
			if (subnode.getNodeType() == Node.TEXT_NODE) {
				result.append(subnode.getNodeValue());
			} else if (subnode.getNodeType() == Node.CDATA_SECTION_NODE) {
				result.append(subnode.getNodeValue());
			} else if (subnode.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
				// Recurse into the subtree for text
				// (and ignore comments)
				result.append(GetNodeText(subnode));
			}
		}
		return result.toString();
	}
}
