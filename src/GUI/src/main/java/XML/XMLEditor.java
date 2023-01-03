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

public class XMLEditor {
    public static void addPath(String xmlPath, String pathToExe) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        File file = new File(xmlPath);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);

        NodeList blocks = doc.getElementsByTagName("B");
        Node block = blocks.item(0);

        Node actions = getChildNodeWithTag(block);
        if (actions == null){
            Element a = doc.createElement("ACTIONS");
            block.appendChild(a);
            actions = block.getLastChild();
        }

        Element e = doc.createElement("COM");
        e.setAttribute("APP", pathToExe + "%" + xmlPath + "%");
        e.setAttribute("CMD", "RUNAPP");
        e.setAttribute("PARAMS", "%" + xmlPath + "%");
        actions.appendChild(e);

        for (int i = 1; i < blocks.getLength(); i++){
            block = blocks.item(i);
            String name = block.getAttributes().getNamedItem("NAME").getNodeValue();
            if (! name.contains("$")){
                block.getAttributes().getNamedItem("NAME").setNodeValue(name + "#blower_id1$temperature#blower_id2$temperature");
            }
            Element a = doc.createElement("ACTIONS");
            block.appendChild(a);
            actions = block.getLastChild();
            e = doc.createElement("COM");
            e.setAttribute("APP", pathToExe + "%" + xmlPath + "%");
            e.setAttribute("CMD", "RUNAPP");
            e.setAttribute("PARAMS", "%" + xmlPath + "%");
            actions.appendChild(e);
        }


        FileOutputStream output = new FileOutputStream(xmlPath);
        writeXml(doc, output);
    }

    private static Node getChildNodeWithTag(Node parent) {
        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals("ACTIONS")) {
                return child;
            }
        }
        return null;
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
