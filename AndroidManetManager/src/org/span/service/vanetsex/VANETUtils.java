package org.span.service.vanetsex;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import android.text.SpannableStringBuilder;

public class VANETUtils {
    
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    
    public static int getLastByteOfIpAddress(String strIp) {
        String strLastByte = null;
        if(strIp != null) {
            StringTokenizer st = new StringTokenizer(strIp, ".");
            
            while(st.hasMoreElements()) {
                strLastByte = (String)st.nextElement();
            }
        }
        
        int lastByte = -1;
        try {
            lastByte = Integer.parseInt(strLastByte);
        } catch(Exception e) {
        }
        
        return lastByte;
    }
    
    public static CharSequence removeExcessBlankLines(CharSequence source) {

        if(source == null)
            return "";

        int newlineStart = -1;
        int nbspStart = -1;
        int consecutiveNewlines = 0;
        SpannableStringBuilder ssb = new SpannableStringBuilder(source);
        for(int i = 0; i < ssb.length(); ++i) {
            final char c = ssb.charAt(i);
            if(c == '\n') {
                if(consecutiveNewlines == 0)
                    newlineStart = i;

                ++consecutiveNewlines;
                nbspStart = -1;
            }
            else if(c == '\u00A0') {
                if(nbspStart == -1)
                    nbspStart = i;
            }
            else if(consecutiveNewlines > 0) {

                // note: also removes lines containing only whitespace,
                //       or nbsp; except at the beginning of a line
                if( !Character.isWhitespace(c) && c != '\u00A0') {

                    // we've reached the end
                    if(consecutiveNewlines > 2) {
                        // replace the many with the two
                        ssb.replace(newlineStart, nbspStart > newlineStart ? nbspStart : i, "\n\n");
                        i -= i - newlineStart;
                    }

                    consecutiveNewlines = 0;
                    nbspStart = -1;
                }
            }
        }

        return ssb;
    }
    
    
//    public static Map<VANETMessage, Long> mapMillisecToMessage = new HashMap<VANETMessage, Long>();
    
    public static String formatTimeInterval(final long intervalMilliseconds) {
    	long l = intervalMilliseconds;
        final long hr = TimeUnit.MILLISECONDS.toHours(l);
        final long min = TimeUnit.MILLISECONDS.toMinutes(l - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
//        final long ms = TimeUnit.MILLISECONDS.toMillis(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
        return String.format("%02d:%02d:%02d" /* + ".%03d" */, hr, min, sec /*, ms */);
    }
    

}
