package vmio.com.blemultipleconnect.gps;

import android.content.Context;
import android.widget.Toast;

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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by DatNT on 10/24/2017.
 */

public class GPXHelper {

    private Element segmentTrack;
    private Document doc;
    private boolean firstElement;
    public GPXHelper() {
        firstElement = true;
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void createGPXTrack() {
        // root elements
        Element rootElement = doc.createElement("gpx");
        doc.appendChild(rootElement);

        // set gpx version
        Attr atrrVersion = doc.createAttribute("version");
        atrrVersion.setValue("1.0");
        rootElement.setAttributeNode(atrrVersion);

        // track elements
        Element track = doc.createElement("trk");
        rootElement.appendChild(track);

        // name elements
        Element name = doc.createElement("name");
        name.appendChild(doc.createTextNode("GNS 2000 plus"));
        track.appendChild(name);

        // number elements
        Element number = doc.createElement("number");
        number.appendChild(doc.createTextNode("1"));
        track.appendChild(number);

        // number elements
        segmentTrack = doc.createElement("trkseg");
        track.appendChild(segmentTrack);
    }

    public void addWayPoint(MLocation location) {
        // element track point
        Element trackPoint = doc.createElement("trkpt");
        segmentTrack.appendChild(trackPoint);

        // set latitude and longitude
        Attr attrLat = doc.createAttribute("lat");
        attrLat.setValue(String.valueOf(location.latitude));
        trackPoint.setAttributeNode(attrLat);

        Attr attrLon = doc.createAttribute("lon");
        attrLon.setValue(String.valueOf(location.longitude));
        trackPoint.setAttributeNode(attrLon);

        // element attitude
        Element ele = doc.createElement("ele");
        ele.appendChild(doc.createTextNode(String.valueOf(location.altitude_gps)));
        trackPoint.appendChild(ele);

        // element time
        Element time = doc.createElement("time");
        time.appendChild(doc.createTextNode(getCurrentTime()));
        trackPoint.appendChild(time);

        // Change flag add first element
        firstElement = false;
    }

    public boolean saveGpxFile(Context context, String path){
        try {
            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(path));
            // Output to console for testing
            // StreamResult result = new StreamResult(System.out);
            transformer.transform(source, result);
            Toast.makeText(context, "GPXファイルを保存しました。",Toast.LENGTH_SHORT).show();
            return true;
        } catch (TransformerConfigurationException e){
            e.printStackTrace();
            return false;
        } catch (TransformerException e){
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkFirstElement(){
        return isFirstElement();
    }
    private String getCurrentTime(){
        DateTime dateTime = new DateTime();
        DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        return dtf.print(dateTime);
    }

    public boolean isFirstElement() {
        return firstElement;
    }

    public void setFirstElement(boolean firstElement) {
        this.firstElement = firstElement;
    }
}
