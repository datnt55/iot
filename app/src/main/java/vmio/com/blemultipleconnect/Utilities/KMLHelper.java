package vmio.com.blemultipleconnect.Utilities;

import android.content.Context;
import android.location.Location;

import com.platypii.baseline.measurements.MLocation;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static vmio.com.blemultipleconnect.Utilities.Define.mMioTempDirectory;

/**
 * Created by DatNT on 10/24/2017.
 */

public class KMLHelper {

    private Element coordinates;
    private Document doc;
    private Element document;
    private String pathName;
    private String description;
    private Context mContext;
    private int count;


    public final static String START_RECORD = "startCall";
    public final static String END_RECORD = "endCall";

    public KMLHelper() {
        count = 0;
    }


    public void createGPXTrack() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        // root elements
        Element rootElement = doc.createElement("kml");
        doc.appendChild(rootElement);
        Attr xmlns = doc.createAttribute("xmlns");
        xmlns.setValue("http://www.opengis.net/kml/2.2");
        rootElement.setAttributeNode(xmlns);

        // set dockument tag
        document = doc.createElement("Document");
        rootElement.appendChild(document);

        Element name = doc.createElement("name");
        document.appendChild(name);
        name.appendChild(doc.createTextNode("pathName"));

        Element eleDescription = doc.createElement("description");
        document.appendChild(eleDescription);
        eleDescription.appendChild(doc.createTextNode("description"));

        Element style = doc.createElement("Style");
        document.appendChild(style);
        Attr styleId = doc.createAttribute("id");
        styleId.setValue("poligon");
        style.setAttributeNode(styleId);

        Element lineStyle = doc.createElement("LineStyle");
        style.appendChild(lineStyle);

        Element lineColor = doc.createElement("color");
        lineColor.appendChild(doc.createTextNode("7f00fff"));
        lineStyle.appendChild(lineColor);

        Element lineWidth = doc.createElement("width");
        lineWidth.appendChild(doc.createTextNode("4"));
        lineStyle.appendChild(lineWidth);

        Element polyStyle = doc.createElement("PolyStyle");
        style.appendChild(polyStyle);

        Element polyColor = doc.createElement("color");
        polyColor.appendChild(doc.createTextNode("7f00ff00"));
        polyStyle.appendChild(polyColor);

        Element placeMark = doc.createElement("Placemark");
        document.appendChild(placeMark);

        Element placeName = doc.createElement("name");
        placeName.appendChild(doc.createTextNode("Ai-Tec"));
        placeMark.appendChild(placeName);

        Element placeDescription = doc.createElement("description");
        placeDescription.appendChild(doc.createTextNode(""));
        placeMark.appendChild(placeDescription);

        Element styleUrl = doc.createElement("styleUrl");
        styleUrl.appendChild(doc.createTextNode("#poligon"));
        placeMark.appendChild(styleUrl);

        Element lineString = doc.createElement("LineString");
        placeMark.appendChild(lineString);

        Element extrude = doc.createElement("extrude");
        extrude.appendChild(doc.createTextNode("1"));
        lineString.appendChild(extrude);

        Element tessellate = doc.createElement("tessellate");
        tessellate.appendChild(doc.createTextNode("1"));
        lineString.appendChild(tessellate);

        Element altitudeMode = doc.createElement("altitudeMode");
        altitudeMode.appendChild(doc.createTextNode("absolute"));
        lineString.appendChild(altitudeMode);

        coordinates = doc.createElement("coordinates");
        lineString.appendChild(coordinates);
    }

    public void addWayPoint(MLocation location) {
        String position = location.longitude +","+location.latitude+","+location.altitude_gps+"\n";
        coordinates.appendChild(doc.createTextNode(position));
    }

    public String saveKMLFile(Context context){
        try {
            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            String worker = new SharePreference(context).getId();
            File kmlFile = new File(mMioTempDirectory, worker+"_"+count+".kml");
            StreamResult result = new StreamResult(kmlFile);
            // Output to console for testing
            // StreamResult result = new StreamResult(System.out);
            transformer.transform(source, result);
            //Toast.makeText(context, " Save GPX file success",Toast.LENGTH_SHORT).show();
            count++;
            return kmlFile.getAbsolutePath();
        } catch (TransformerConfigurationException e){
            e.printStackTrace();
            return null;
        } catch (TransformerException e){
            e.printStackTrace();
            return null;
        }
    }

    public String getKMLFileName(){
        DateTime dateTime = new DateTime();
        DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy_MM_dd'T'HH_mm_ss");
        File file = new File(mMioTempDirectory, "KML");
        if (!file.exists())
            file.mkdir();
        File kmlFile = new File(file, dtf.print(dateTime)+".kml");
        return kmlFile.getAbsolutePath();
    }

//    public ArrayList<LocationGPS> getTrajectory(String file){
//        ArrayList<LocationGPS> gpsArrayList = new ArrayList<>();
//        XmlPullParserFactory parserFactory;
//        try {
//            parserFactory = XmlPullParserFactory.newInstance();
//            XmlPullParser parser = parserFactory.newPullParser();
//            InputStream is = new FileInputStream(new File(file));
//            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
//            parser.setInput(is, null);
//            int eventType = parser.getEventType();
//            while (eventType != XmlPullParser.END_DOCUMENT) {
//                String eltName = null;
//
//                switch (eventType) {
//                    case XmlPullParser.START_TAG:
//                        eltName = parser.getName();
//                        if ("coordinates".equals(eltName)) {
//                            String coordinate = parser.nextText();
//                            String[]list = coordinate.split("[\\r\\n]+");
//                            for (int i = 0 ; i < list.length; i++){
//                                double lon = Double.parseDouble(list[i].split(",")[0]);
//                                double lat = Double.parseDouble(list[i].split(",")[1]);
//                                gpsArrayList.add(new LocationGPS(lon,lat));
//                            }
//                        }
//                        break;
//                }
//
//                eventType = parser.next();
//            }
//
//        } catch (XmlPullParserException e) {
//            return gpsArrayList;
//        } catch (IOException e) {
//            return gpsArrayList;
//        }
//        return gpsArrayList;
//    }
}
