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
    public static void addPath(String xmlPath, String pathToExe, List<String> blowers)
            throws ParserConfigurationException, IOException, SAXException, TransformerException {
        File file = new File(xmlPath);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);

        NodeList blocks = doc.getElementsByTagName("B");
        Node block = blocks.item(0);

        updateBlock(block, doc, pathToExe, xmlPath, blowers);

        for (int i = 1; i < blocks.getLength(); i++){
            block = blocks.item(i);
            updateBlock(block, doc, pathToExe, "^" + xmlPath, blowers);
        }

        NodeList calls = doc.getElementsByTagName("CALL");
        NodeList subrts = doc.getElementsByTagName("SUBRT");
        for (int i = 0; i < subrts.getLength(); i++){
            Element subrt = (Element) subrts.item(i);
            String name = subrt.getAttributeNode("NAME").getNodeValue().split("#")[0];
            if (name.contains("Measurement") || name.contains("measurement") || name.contains("MEASUREMENT")){
                if (! name.contains("@")){
                    name = name + "@temperature";
                }
                for (String blower : blowers){
                    name = name + "#" + blower;
                }
                subrt.getAttributes().getNamedItem("NAME").setNodeValue(name);

                for (int j = 0; j < calls.getLength(); j++){
                    String calledBlk = calls.item(j).getAttributes().getNamedItem("BLK").getNodeValue().split("#")[0];
                    if (calledBlk.contains("Measurement") || calledBlk.contains("measurement") || calledBlk.contains("MEASUREMENT")) {
                        if (!calledBlk.contains("@") || calledBlk.equals(name.split("#")[0])) {
                            calledBlk = name;
                            calls.item(j).getAttributes().getNamedItem("BLK").setNodeValue(calledBlk);
                        }
                    }
                }

                NodeList blocksInMeasurement = subrt.getElementsByTagName("B");
                for (int j = 0; j < blocksInMeasurement.getLength(); j++){
                    String[] bName = blocksInMeasurement.item(j).getAttributes().getNamedItem("NAME").getNodeValue().split("@");
                    blocksInMeasurement.item(j).getAttributes().getNamedItem("NAME").setNodeValue(bName[0]);
                }
            }
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

    private static void updateBlock(Node block, Document doc, String pathToExe, String xmlPath, List<String> blowers){
        String name = block.getAttributes().getNamedItem("NAME").getNodeValue().split("#")[0];
        if (! name.contains("@")){
            name = name + "@temperature";
        }
        for (String blower : blowers){
            name = name + "#" + blower;
        }
        block.getAttributes().getNamedItem("NAME").setNodeValue(name);

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
            if (atr.getNodeValue().contains("_temp_control.xml")){
                e.setAttribute("APP", pathToExe);
                return;
            }
        }
        Element e = doc.createElement("COM");
        e.setAttribute("APP",   pathToExe);
        e.setAttribute("CMD", "RUNAPP");
        e.setAttribute("PARAMS", "'" + xmlPath + "'");
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
