package org.jivesoftware.spark.plugin.red5;

import java.util.*;
import org.jivesoftware.smack.packet.*;

public class PhoneExtension implements PacketExtension {

    private String elementName;
    private String namespace;
    private String type;
    private String callID;
    private String device;

    private Map<String, String> map;

    public PhoneExtension(String elementName, String namespace, String type, String device, String callID) {
        this.elementName = elementName;
        this.namespace = namespace;
        this.type = type;
        this.device = device;
        this.callID = callID;
    }

    public String getElementName() {
        return elementName;
    }

    public String getNamespace() {
        return namespace;
    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();

        buf.append("<").append(elementName).append(" xmlns=\"").append(namespace).append("\"").append(" type=\"").append(type).append("\"").append(" callID=\"").append(callID).append("\"").append(" device=\"").append(device).append("\">");

        for (Iterator i=getNames(); i.hasNext(); ) {
            String name = (String)i.next();
            String value = getValue(name);

            buf.append("<").append(name).append(">").append(value).append("</").append(name).append(">");
        }

        buf.append("</").append(elementName).append(">");
        return buf.toString();
    }

    public synchronized Iterator getNames() {

        if (map == null) {
            return Collections.EMPTY_LIST.iterator();
        }
        return Collections.unmodifiableMap(new HashMap(map)).keySet().iterator();
    }


    public synchronized String getValue(String name) {

        if (map == null) {
            return null;
        }
        return (String)map.get(name);
    }

    public synchronized void setValue(String name, String value) {

        if (map == null) {
            map = new HashMap <String, String>();
        }

        map.put(name, value);
    }
}