package fr.delthas.skype.message;

import fr.delthas.skype.ParseException;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Class of fr.delthas.skype
 *
 * @author aivanov
 */
public class TestMessage {

  private DocumentBuilder documentBuilder;


  @Test
  public void test() throws Exception {
    documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

    String xml = getXml("fr/delthas/skype/message/picture.xml");
    System.out.println(getXMLField(xml, "URIObject"));
  }

  private static Document getPlaintext(String string) {
    return Jsoup.parseBodyFragment(string);
  }

  public static String getXml(String path) throws IOException {
    return IOUtils.toString(ClassLoader.getSystemResourceAsStream(path), "UTF-8");
  }


  private org.w3c.dom.Document getDocument(String XML) throws ParseException {
    try {
      return documentBuilder.parse(new InputSource(new StringReader(XML)));
    } catch (IOException | SAXException e) {
      throw new ParseException(e);
    }
  }

  private List<String> getXMLFields(String XML, String fieldName) throws ParseException {
    NodeList nodes = getDocument(XML).getElementsByTagName(fieldName);
    List<String> fields = new ArrayList<>(nodes.getLength());
    for (int i = 0; i < nodes.getLength(); i++) {
      fields.add(nodes.item(i).getTextContent());
    }
    return fields;
  }

  private String getXMLField(String XML, String fieldName) throws ParseException {
    List<String> fields = getXMLFields(XML, fieldName);
    if (fields.size() > 1) {
      throw new ParseException();
    }
    if (fields.size() == 0) {
      return null;
    }
    return fields.get(0);
  }

}
