package org.jivesoftware.spark.plugin.red5;

import java.util.Map;
import java.util.Iterator;
import org.jivesoftware.smack.packet.*;

public class PhoneIQ extends IQ {

    public String getChildElementXML() {
        StringBuffer buf = new StringBuffer();
        buf.append(getExtensionsXML());
        return buf.toString();
    }
}