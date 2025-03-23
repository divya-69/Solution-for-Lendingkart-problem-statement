import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;

public class XMLToJSONUtility {

    private static final Logger logger = LoggerFactory.getLogger(XMLToJSONUtility.class);

    public static String convertXMLToJSON(String xmlString) throws Exception {
        // Parse XML
        Document document = parseXML(xmlString);
        
        // Convert XML to JSON
        JSONObject json = convertDocumentToJSON(document);
        
        // Add custom field (MatchSummary.TotalMatchScore)
        addTotalMatchScore(json, document);
        
        return json.toString(4); // Pretty print with 4 spaces indentation
    }

    private static Document parseXML(String xmlString) throws Exception {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream is = new ByteArrayInputStream(xmlString.getBytes());
            return builder.parse(is);
        } catch (Exception e) {
            logger.error("Error parsing XML", e);
            throw new Exception("Error parsing XML", e);
        }
    }

    private static JSONObject convertDocumentToJSON(Document document) {
        try {
            JSONObject json = new JSONObject();
            Node root = document.getDocumentElement();
            convertNodeToJSON(root, json);
            return json;
        } catch (Exception e) {
            logger.error("Error converting XML Document to JSON", e);
            return new JSONObject(); // Return empty object if conversion fails
        }
    }

    private static void convertNodeToJSON(Node node, JSONObject jsonObject) {
        // Recursive method to handle XML nodes
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            String nodeName = node.getNodeName();
            NodeList childNodes = node.getChildNodes();
            if (childNodes.getLength() == 1 && childNodes.item(0).getNodeType() == Node.TEXT_NODE) {
                jsonObject.put(nodeName, node.getTextContent());
            } else {
                JSONObject subObject = new JSONObject();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    convertNodeToJSON(childNodes.item(i), subObject);
                }
                jsonObject.put(nodeName, subObject);
            }
        }
    }

    private static void addTotalMatchScore(JSONObject json, Document document) {
        try {
            NodeList matchNodes = document.getElementsByTagName("Match");
            int totalMatchScore = 0;

            for (int i = 0; i < matchNodes.getLength(); i++) {
                Node matchNode = matchNodes.item(i);
                NodeList matchChildren = matchNode.getChildNodes();
                for (int j = 0; j < matchChildren.getLength(); j++) {
                    if (matchChildren.item(j).getNodeName().equals("Score")) {
                        String scoreStr = matchChildren.item(j).getTextContent();
                        try {
                            totalMatchScore += Integer.parseInt(scoreStr);
                        } catch (NumberFormatException e) {
                            logger.warn("Invalid score value: " + scoreStr);
                        }
                    }
                }
            }

            // Add custom field for TotalMatchScore
            JSONObject resultBlock = json.getJSONObject("Response").getJSONObject("ResultBlock");
            JSONObject matchSummary = new JSONObject();
            matchSummary.put("TotalMatchScore", totalMatchScore);
            resultBlock.put("MatchSummary", matchSummary);

        } catch (Exception e) {
            logger.error("Error adding TotalMatchScore to JSON", e);
        }
    }

    public static void main(String[] args) {
        String xmlInput = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Response>\n" +
                "    <ResultBlock>\n" +
                "        <ErrorWarnings>\n" +
                "            <Errors errorCount=\"0\" />\n" +
                "            <Warnings warningCount=\"1\">\n" +
                "                <Warning>\n" +
                "                    <Number>102001</Number>\n" +
                "                    <Message>Minor mismatch in address</Message>\n" +
                "                    <Values>\n" +
                "                        <Value>Bellandur</Value>\n" +
                "                        <Value>Bangalore</Value>\n" +
                "                    </Values>\n" +
                "                </Warning>\n" +
                "            </Warnings>\n" +
                "        </ErrorWarnings>\n" +
                "        <MatchDetails>\n" +
                "            <Match>\n" +
                "                <Entity>John</Entity>\n" +
                "                <MatchType>Exact</MatchType>\n" +
                "                <Score>35</Score>\n" +
                "            </Match>\n" +
                "            <Match>\n" +
                "                <Entity>Doe</Entity>\n" +
                "                <MatchType>Exact</MatchType>\n" +
                "                <Score>50</Score>\n" +
                "            </Match>\n" +
                "        </MatchDetails>\n" +
                "        <API>\n" +
                "            <RetStatus>SUCCESS</RetStatus>\n" +
                "            <ErrorMessage />\n" +
                "            <SysErrorCode />\n" +
                "            <SysErrorMessage />\n" +
                "        </API>\n" +
                "    </ResultBlock>\n" +
                "</Response>";

        try {
            String jsonOutput = convertXMLToJSON(xmlInput);
            System.out.println(jsonOutput);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
