package Burniee.xml;

import Burniee.Logs.GeneralLogger;
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
    private static final HashMap<String, String> waveforms = new HashMap<>();
    private static final HashMap<String, List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>>>> subroutines = new HashMap<>();
    private static Set<String> blowers = new HashSet<>();

    /**
     * Parses xml file and returns sequence of phases (blocks) in order in which they are executed.
     * @param xmlPath Path to xml file
     * @return the return value is in format: List<SimpleEntry<phaseName, List<SimpleEntry<BlowerID, temperature$time>>>>
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws NoBlowersException throws this, when no blower ID is found in any name of block in xml
     */
    public static List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>>> XMLtoCommands(String xmlPath)
            throws ParserConfigurationException, IOException, SAXException, NoBlowersException {
        File file = new File(xmlPath);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);

        blowers = allBlowers(doc);
        if (blowers.isEmpty()) throw new NoBlowersException("No blowers mentioned in xml with path: " + xmlPath);
        setWaveforms(doc);
        setSubroutines(doc);

        Element firstChild = getFirstElemChild(doc.getDocumentElement());
        if (firstChild != null) return getThreadTimeAndTemp(firstChild);

        return new ArrayList<>();
    }

    /**
     * Returns IDs of all blowers used in this xml file
     * @param xmlPath Path to xml file
     * @return Set of IDs of blowers used in this xml
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static Set<String> getAllBlowers(String xmlPath)
            throws ParserConfigurationException, IOException, SAXException {
        File file = new File(xmlPath);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);
        return allBlowers(doc);
    }

    /**
     * Returns name of the project from NAME attribute of <PROGRAM> tag
     * @param xmlPath Path to xml file
     * @return Name of the project
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static String getProjectName(String xmlPath)
            throws ParserConfigurationException, IOException, SAXException {
        File file = new File(xmlPath);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);
        return doc.getDocumentElement().getAttributeNode("NAME").getNodeValue();
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

    private static AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>> getBlockTimeAndTemp(Element block){
        List<AbstractMap.SimpleEntry<String, String>> res = new ArrayList<>();
        String[] name = block.getAttributes().getNamedItem("NAME").getNodeValue().split("#");
        List<String> visited = new ArrayList<>();

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
            String temp;
            try {
                temp = name[0].split("@")[1];
                Double.parseDouble(temp);
            } catch (NumberFormatException e){
                throw new NumberFormatException("Incorrect temperature value in block: "
                        + block.getAttributes().getNamedItem("NAME"));
            } catch (IndexOutOfBoundsException e){
                throw new IndexOutOfBoundsException("No temperature value in block: "
                        + block.getAttributes().getNamedItem("NAME"));
            }
            for (int i = 1; i < name.length; i++){
                res.add(new AbstractMap.SimpleEntry<>(name[i], temp + "$" + time));
                visited.add(name[i]);
            }
            for (String blower : blowers){
                if (!visited.contains(blower)){
                    res.add(new AbstractMap.SimpleEntry<>(blower, "0$" + time));
                }
            }

            return new AbstractMap.SimpleEntry<>(name[0].split("@")[0], res);
        } catch (NullPointerException e){
            return null;
        }
    }

    private static AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>> getBlockTimeAndTemp(
            Element block, List<String> blowers, List<String> temps){
        List<AbstractMap.SimpleEntry<String, String>> res = new ArrayList<>();
        String name = block.getAttributes().getNamedItem("NAME").getNodeValue();

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

            for (int i = 0; i < blowers.size(); i++){
                try{
                    Double.parseDouble(temps.get(i));
                    res.add(new AbstractMap.SimpleEntry<>(blowers.get(i), temps.get(i) + "$" + time));

                } catch (NumberFormatException e){
                    throw new NumberFormatException("Incorrect temperature value in block: "
                            + block.getAttributes().getNamedItem("NAME"));
                }
            }

            return new AbstractMap.SimpleEntry<>(name, res);
        } catch (NullPointerException e){
            return null;
        }
    }

    private static List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>>> getThreadTimeAndTemp(Element thread){
        List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>>> res = new ArrayList<>();
        NodeList children = thread.getChildNodes();
        for (int i = 0; i < children.getLength(); i++){
            Node n = children.item(i);

            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("B")){
                res.add(getBlockTimeAndTemp((Element) n));
            } else if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("CALL")){
                res.addAll(subroutines.get(n.getAttributes().getNamedItem("BLK").getNodeValue()));
            } else if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("SEQ")){
                List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>>> seq = getSequenceTimeAndTemp((Element) n);
                res.addAll(seq);
            }
        }

        return res;
    }

    private static List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>>> getThreadTimeAndTemp(
            Element thread, List<String> blowers, List<String> temps){
        List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>>> res = new ArrayList<>();
        NodeList children = thread.getChildNodes();
        for (int i = 0; i < children.getLength(); i++){
            Node n = children.item(i);

            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("B")){
                res.add(getBlockTimeAndTemp((Element) n, blowers, temps));
            } else if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("CALL")){
                String callName = n.getAttributes().getNamedItem("BLK").getNodeValue();
                try {
                    res.addAll(subroutines.get(callName));
                } catch (NullPointerException e){
                    GeneralLogger.writeExeption(new NullPointerException("SUBRT with name " + callName + "not found"));
                }
            } else if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("SEQ")){
                List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>>> seq = getSequenceTimeAndTemp((Element) n, blowers, temps);
                res.addAll(seq);
            }
        }

        return res;
    }

    private static List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>>> getSequenceTimeAndTemp(Element seq) {
        List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>>> result = new ArrayList<>();
        Element firstChild = getFirstElemChild(seq);
        if (firstChild != null && firstChild.getNodeName().equals("THREAD")) {
            List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>>> one_iteration = getThreadTimeAndTemp(firstChild);
            int repetitions = (int) Double.parseDouble(seq.getAttributes().getNamedItem("REP").getNodeValue());
            for (int i = 0; i < repetitions; i++){
                result.addAll(one_iteration);
            }
        }
        return result;
    }

    private static List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>>> getSequenceTimeAndTemp(
            Element seq, List<String> blowers, List<String> temps) {
        List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>>> result = new ArrayList<>();
        Element firstChild = getFirstElemChild(seq);
        if (firstChild != null && firstChild.getNodeName().equals("THREAD")) {
            List<AbstractMap.SimpleEntry<String, List<AbstractMap.SimpleEntry<String, String>>>> one_iteration = getThreadTimeAndTemp(firstChild, blowers, temps);
            int repetitions = (int) Double.parseDouble(seq.getAttributes().getNamedItem("REP").getNodeValue());
            for (int i = 0; i < repetitions; i++){
                result.addAll(one_iteration);
            }
        }
        return result;
    }

    private static void setSubroutines(Document doc){
        NodeList subrts = doc.getElementsByTagName("SUBRT");
        for (int i = 0; i < subrts.getLength(); i++){
            Node subrt = subrts.item(i);
            Element firstChild = getFirstElemChild((Element) subrt);
            String subrtName = ((Element) subrt).getAttribute("NAME");
            if (firstChild != null && firstChild.getNodeName().equals("THREAD")){
                if (subrtName.contains("Measurement") || subrtName.contains("measurement") || subrtName.contains("MEASUREMENT")){
                    List<String> blowers = new ArrayList<>();
                    List<String> temps = new ArrayList<>();
                    String[] names = subrtName.split("#");
                    String tmp = names[0].split("@")[1];
                    for (int j = 1; j < names.length; j++){
                        blowers.add(names[j]);
                        temps.add(tmp);
                    }
                    subroutines.put(subrtName, getThreadTimeAndTemp(firstChild, blowers, temps));
                } else {
                    subroutines.put(subrt.getAttributes().getNamedItem("NAME").getNodeValue(), getThreadTimeAndTemp(firstChild));
                }
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

    private static Set<String> allBlowers(Document doc){
        Set<String> blowers = new HashSet<>();

        NodeList blocks = doc.getElementsByTagName("B");
        for (int i = 0; i < blocks.getLength(); i++){
            Node block = blocks.item(i);
            String[] name = block.getAttributes().getNamedItem("NAME").getNodeValue().split("#");
            for (int j = 1; j < name.length; j++){
                String[] s = name[j].split("@");
                blowers.add(s[0]);
            }
        }

        NodeList subrts = doc.getElementsByTagName("SUBRT");
        for (int i = 0; i < subrts.getLength(); i++){
            Node subrt = subrts.item(i);
            String subrtName = ((Element) subrt).getAttribute("NAME");
            if (subrtName.contains("Measurement") || subrtName.contains("measurement") || subrtName.contains("MEASUREMENT")){
                String[] names = subrtName.split("#");
                for (int j = 1; j < names.length; j++){
                    blowers.add(names[j]);
                }
            }
        }

        return blowers;
    }
}
