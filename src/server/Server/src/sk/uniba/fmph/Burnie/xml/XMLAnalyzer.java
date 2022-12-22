package sk.uniba.fmph.Burnie.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class XMLAnalyzer {
    static HashMap<String, String> waveforms = new HashMap<>();
    static HashMap<String, HashMap<String, List<String>>> subroutines = new HashMap<>();

    public static HashMap<String, List<String>> XMLtoCommands(String xmlPath)
            throws ParserConfigurationException, IOException, SAXException {
        File file = new File(xmlPath);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);

        setWaveforms(doc);
        setSubroutines(doc);

        Element firstChild = getFirstElemChild(doc.getDocumentElement());
        if (firstChild != null) return getThreadTimeAndTemp(firstChild);

        return new HashMap<>();
    }

    private static void setWaveforms(Document doc){
        NodeList nodes = doc.getElementsByTagName("W");
        for (int i = 0; i < nodes.getLength(); i++){
            Node n = nodes.item(i);
            waveforms.put(
                    n.getAttributes().getNamedItem("NAME").getNodeValue(),
                    n.getAttributes().getNamedItem("TIME").getNodeValue()
            );
        }
    }

    private static HashMap<String, String> getBlockTimeAndTemp(Element block){
        HashMap<String, String> result = new HashMap<>();
        String[] name = block.getAttributes().getNamedItem("NAME").getNodeValue().split("#");
        for (int i = 1; i < name.length; i++){
            String[] s = name[i].split("\\$");
            try{
                Double.parseDouble(s[1]);
                result.put(s[0], s[1]);
            } catch (NumberFormatException e){
                throw new NumberFormatException("Incorrect temperature value in block: "
                        + block.getAttributes().getNamedItem("NAME"));
            }
        }
        try {
            Element gen = getFirstElemChild(Objects.requireNonNull(getFirstElemChild(block)));
            String type = Objects.requireNonNull(gen).getAttributes().getNamedItem("TYPE").getNodeValue();
            String time = "";

            if (Objects.equals(type, "PER")) {
                double cyc = Double.parseDouble(gen.getAttributes().getNamedItem("CYC").getNodeValue());
                double freq = Double.parseDouble(gen.getAttributes().getNamedItem("FREQ").getNodeValue());
                time = String.valueOf(cyc / freq);
            } else if (Objects.equals(type, "RAMP")) {
                time = gen.getAttributes().getNamedItem("RTIME").getNodeValue();
            } else if (Objects.equals(type, "WAVE")) {
                time = waveforms.get(gen.getAttributes().getNamedItem("WAVE").getNodeValue());
            }

            for (String key : result.keySet()) {
                result.put(key, result.get(key) + "$" + time);
            }
            return result;
        } catch (NullPointerException e){
            return new HashMap<>();
        }
    }

    private static HashMap<String, List<String>> getThreadTimeAndTemp(Element thread){
        HashMap<String, List<String>> result = new HashMap<>();
        NodeList children = thread.getChildNodes();
        for (int i = 0; i < children.getLength(); i++){
            Node n = children.item(i);

            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("B")){
                HashMap<String, String> b = getBlockTimeAndTemp((Element) n);
                for (String key: b.keySet()){
                    result.putIfAbsent(key, new ArrayList<>());
                    result.get(key).add(b.get(key));
                }
            } else if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("CALL")){
                HashMap<String, List<String>> subrt = subroutines.get(n.getAttributes().getNamedItem("BLK").getNodeValue());
                for (String key: subrt.keySet()){
                    result.putIfAbsent(key, new ArrayList<>());
                    result.get(key).addAll(subrt.get(key));
                }
            } else if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("SEQ")){
                HashMap<String, List<String>> seq = getSequenceTimeAndTemp((Element) n);
                for (String key: seq.keySet()){
                    result.putIfAbsent(key, new ArrayList<>());
                    result.get(key).addAll(seq.get(key));
                }
            }
        }

        return result;
    }

    private static HashMap<String, List<String>> getSequenceTimeAndTemp(Element seq) {
        HashMap<String, List<String>> result = new HashMap<>();
        Element firstChild = getFirstElemChild(seq);
        if (firstChild != null && firstChild.getNodeName().equals("THREAD")) {
            result = getThreadTimeAndTemp(firstChild);
        }

        int repetitions = (int) Double.parseDouble(seq.getAttributes().getNamedItem("REP").getNodeValue());

        for (String key : result.keySet()) {
            List<String> tmp = new ArrayList<>();
            for (int i = 0; i < repetitions; i++){
                tmp.addAll(result.get(key));
            }
            result.put(key, tmp);
        }

        return result;
    }

    private static void setSubroutines(Document doc){
        NodeList subrts = doc.getElementsByTagName("SUBRT");
        for (int i = 0; i < subrts.getLength(); i++){
            Node subrt = subrts.item(i);
            Element firstChild = getFirstElemChild((Element) subrt);
            if (firstChild != null && firstChild.getNodeName().equals("THREAD")){
                subroutines.put(subrt.getAttributes().getNamedItem("NAME").getNodeValue(), getThreadTimeAndTemp(firstChild));
            }
        }
    }

    private static Element getFirstElemChild(Element e){
        NodeList eChildren = e.getChildNodes();
        for (int i = 0; i < eChildren.getLength(); i++){
            if (eChildren.item(i).getNodeType() == Node.ELEMENT_NODE){
                return (Element) eChildren.item(i);
            }
        }
        return null;
    }
}
