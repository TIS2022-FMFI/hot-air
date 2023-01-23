package XML;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class XMLEditor {
    public static void addPath(String xmlPath, String pathToExe) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        File file = new File(xmlPath);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);

        NodeList blocks = doc.getElementsByTagName("B");
        Node block = blocks.item(0);

        updateBlock(block, doc, pathToExe, xmlPath);

        for (int i = 1; i < blocks.getLength(); i++){
            block = blocks.item(i);
            updateBlock(block, doc, pathToExe, "$" + xmlPath);
        }


        FileOutputStream output = new FileOutputStream(xmlPath);
        writeXml(doc, output);
    }

    private static Node getChildNodeWithTag(Node parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(tagName)) {
                return child;
            }
        }
        return null;
    }

    private static List<Node> getAllChildNodesWithTag(Node parent, String tagName){
        NodeList children = parent.getChildNodes();
        List<Node> result = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(tagName)) {
                result.add(child);
            }
        }
        return result;
    }

    private static void updateBlock(Node block, Document doc, String pathToExe, String xmlPath){
        String name = block.getAttributes().getNamedItem("NAME").getNodeValue();
        if (! name.contains("@")){
            block.getAttributes().getNamedItem("NAME").setNodeValue(name + "#blower_id1@temperature");
        }

        Node actions = getChildNodeWithTag(block, "ACTIONS");
        if (actions == null){
            Element a = doc.createElement("ACTIONS");
            block.appendChild(a);
            actions = block.getLastChild();
        }

        List<Node> coms = getAllChildNodesWithTag(actions, "COM");
        for (Node com : coms){
            Element e = (Element) com;
            Node atr = e.getAttributeNode("PARAMS");
            if (Objects.equals(atr.getNodeValue(), xmlPath)){
                e.setAttribute("APP", pathToExe);
                return;
            }
        }
        Element e = doc.createElement("COM");
        e.setAttribute("APP", pathToExe);
        e.setAttribute("CMD", "RUNAPP");
        e.setAttribute("PARAMS", xmlPath);
        actions.appendChild(e);
    }

    private static void writeXml(Document doc, OutputStream output) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);

        transformer.transform(source, result);
    }
}
