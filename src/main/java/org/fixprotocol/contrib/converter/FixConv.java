//
// FixConv.java - FIX Converter
//
// AK, 13 May 2011, initial version
//

package org.fixprotocol.contrib.converter;

//...simports:0:
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Calendar;
import java.util.TimeZone;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
//...e

public class FixConv
  {
  // Helper code

//...slogging:2:
public static boolean loggingEnabled = false;

protected String indent = "";
//...slog:2:
protected void log(String s)
  {
  if ( loggingEnabled )
    System.out.println(indent+s);
  }
//...e
//...spush:2:
protected void push(String s)
  {
  log(s);
  indent += "    ";
  log("{");
  }
//...e
//...spop:2:
protected void pop()
  {
  log("}");
  indent = indent.substring(4);
  }
//...e
//...e

//...sbase64Encode:2:
protected static String base64Encode(byte[] b, int len)
  {
  String base64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
  String e = "";
  int i = 0;
  while ( i + 3 <= len )
    {
    int v0 = b[i++] & 0xff;
    int v1 = b[i++] & 0xff;
    int v2 = b[i++] & 0xff;
    e += String.valueOf( base64.charAt(  (v0>>2)          & 0x3f ) );
    e += String.valueOf( base64.charAt( ((v0<<4)|(v1>>4)) & 0x3f ) );
    e += String.valueOf( base64.charAt( ((v1<<2)|(v2>>6)) & 0x3f ) );
    e += String.valueOf( base64.charAt(   v2              & 0x3f ) );
    }
  switch ( len - i )
    {
    case 1:
      {
      int v0 = b[i] & 0xff;
      e += String.valueOf( base64.charAt(  (v0>>2)          & 0x3f ) );
      e += String.valueOf( base64.charAt(  (v0<<4)          & 0x3f ) );
      e += "==";
      }
      break;
    case 2:
      {
      int v0 = b[i++] & 0xff;
      int v1 = b[i  ] & 0xff; 
      e += String.valueOf( base64.charAt(  (v0>>2)          & 0x3f ) );
      e += String.valueOf( base64.charAt( ((v0<<4)|(v1>>4)) & 0x3f ) );
      e += String.valueOf( base64.charAt(  (v1<<2)          & 0x3f ) );
      e += "=";
      }
      break;
    }
  return e;
  }
//...e
//...sbase64Decode:2:
protected static byte[] base64Decode(String s)
  throws FixConvException
  {
  String base64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
  int i = 0, j = 0;
  byte b[] = new byte[(s.length()/4)*3];
    // Every 4 characters input can produce 3 (or less) output
  while ( i + 4 <= s.length() )
    {
    int v0 = base64.indexOf(s.charAt(i++));
    int v1 = base64.indexOf(s.charAt(i++));
    int v2 = base64.indexOf(s.charAt(i++));
    int v3 = base64.indexOf(s.charAt(i++));
    if ( v0 == -1 || v1 == -1 || v2 == -1 || v3 == -1 )
      return null; // base64 encoded data contains bad character
    if ( v0 == 64 || v1 == 64 )
      break;
    b[j++] = (byte) (  (v0<<2)|(v1>>4)         );
    if ( v2 == 64 )
      break;
    b[j++] = (byte) ( ((v1<<4)|(v2>>2)) & 0xff );
    if ( v3 == 64 )
      break;
    b[j++] = (byte) ( ((v2<<6)| v3    ) & 0xff );
    }
  if ( i < s.length() )
    throw new FixConvException("base64 encoded data must be a multiple of 4 characters");
  if ( j < b.length )
    {
    byte b2[] = new byte[j];
    System.arraycopy(b, 0, b2, 0, j);
    b = b2;
    }
  return b;
  }
//...e

//...sXML scanning helpers:2:
// Code used for scanning <?xml text, looking for encoding="XYZ"

//...swsChar:2:
protected static boolean wsChar(char c)
  {
  return c == ' ' || c == '\t' || c == '\n' || c == '\r';
  }
//...e
//...snameStartChar:2:
protected static boolean nameStartChar(char c)
  {
  return ( c >= 'A' && c <= 'Z' ) ||
         ( c >= 'a' && c <= 'z' ) ||
         c == ':' || c == '_' ||
         ( c >= '\u00c0' && c <= '\u00d6' ) ||
         ( c >= '\u00d8' && c <= '\u00f6' ) ||
         ( c >= '\u00f8' && c <= '\u02ff' ) ||
         ( c >= '\u0370' && c <= '\u037d' ) ||
         ( c >= '\u037f' && c <= '\u1fff' ) ||
         ( c >= '\u200c' && c <= '\u200d' ) ||
         ( c >= '\u2070' && c <= '\u218f' ) ||
         ( c >= '\u2c00' && c <= '\u2fef' ) ||
         ( c >= '\u3001' && c <= '\ud7ff' ) ||
         ( c >= '\uf900' && c <= '\ufdcf' ) ||
         ( c >= '\ufdf0' && c <= '\ufffd' );
  }
//...e
//...snameChar:2:
protected static boolean nameChar(char c)
  {
  return nameStartChar(c) ||
         ( c >= '0' && c <= '9' ) ||
         c == '-' || c == '.' ||
         c == (char)0xb7 ||
         ( c >= '\u0300' && c <= '\u036f' ) ||
         ( c >= '\u203f' && c <= '\u2040' );
  }
//...e

// Two almost identical implementations of nextToken, so as to avoid the
// need to convert between String <-> byte[] before using it.

//...snextToken byte\91\\93\:2:
protected static int nextToken(byte[] b, int i, StringBuffer sb)
  {
  while ( i < b.length && wsChar((char)b[i]) )
    i++;
  if ( i == b.length )
    return -1; // no more tokens
  if ( (char)b[i] == '?' && i+2 <= b.length && (char)b[i+1] == '>' )
    return -1; // no more tokens, as we hit ?>
  if ( nameStartChar((char)b[i]) )
    {
    sb.append((char)b[i++]);
    while ( i < b.length && nameChar((char)b[i]) )
      sb.append((char)b[i++]);
    return i;
    }
  if ( (char)b[i] == '"' )
    {
    ++i;
    sb.append("\""); // so caller knows its a quoted string
    while ( i <= b.length && (char)b[i] != '"' )
      sb.append((char)b[i++]);
    if ( i == b.length )
      return -1; // unterminated string
    return i+1;
    }
  else if ( (char)b[i] == '=' )
    {
    sb.append('=');
    return i+1;
    }
  else
    return -1; // something unexpected
  }
//...e
//...snextToken String:2:
protected static int nextToken(String s, int i, StringBuffer sb)
  {
  while ( i < s.length() && wsChar(s.charAt(i)) )
    i++;
  if ( i == s.length() )
    return -1; // no more tokens
  if ( s.charAt(i) == '?' && i+2 <= s.length() && s.charAt(i+1) == '>' )
    return -1; // no more tokens, as we hit ?>
  if ( nameStartChar(s.charAt(i)) )
    {
    sb.append(s.charAt(i++));
    while ( i < s.length() && nameChar(s.charAt(i)) )
      sb.append(s.charAt(i++));
    return i;
    }
  if ( s.charAt(i) == '"' )
    {
    ++i;
    sb.append("\""); // so caller knows its a quoted string
    while ( i <= s.length() && s.charAt(i) != '"' )
      sb.append(s.charAt(i++));
    if ( i == s.length() )
      return -1; // unterminated string
    return i+1;
    }
  else if ( s.charAt(i) == '=' )
    {
    sb.append('=');
    return i+1;
    }
  else
    return -1; // something unexpected
  }
//...e
//...e
//...sbytesToString:2:
public static String bytesToString(byte[] b, String enc)
  throws FixConvException
  {
  int skip = 0;
  if ( enc == null )
    enc = "UTF-8";
  if ( enc.equalsIgnoreCase("XML") )
    // Try to autodetect encoding used, based on BOM or XML header
    {
    if ( b.length >= 3 &&
         b[0] == (byte) 0xef && b[1] == (byte) 0xbb && b[2] == (byte) 0xbf )
      {
      enc = "UTF-8";
      skip = 3;
      }
    else if ( b.length >= 2 &&
         b[0] == (byte) 0xfe && b[1] == (byte) 0xff )
      {
      enc = "UTF-16BE";
      skip = 2;
      }
    else if ( b.length >= 2 &&
         b[0] == (byte) 0xff && b[1] == (byte) 0xfe )
      {
      enc = "UTF-16LE";
      skip = 3;
      }
    else if ( b.length >= 6 &&
         b[0] == '<' && b[1] == '?' && b[2] == 'x' && b[3] == 'm' && b[4] == 'l' &&
         wsChar((char)b[5]) )
      // No BOM, but it looks like XML, so try scanning the <?xml text
      {
      String token2 = "", token1 = "", token0 = "";
      int i = 6;
      for ( ;; )
        {
        StringBuffer sb = new StringBuffer();
        i = nextToken(b, i, sb);
        if ( i == -1 )
          {
          enc = "UTF-8";
          break;
          }
        token2 = token1; token1 = token0; token0 = sb.toString();
        if ( token2.equals("encoding") && token1.equals("=") && token0.startsWith("\"") )
          {
          enc = token0.substring(1);
          if ( enc.equals("UTF-16") )
            throw new FixConvException("can't convert byte[] to String using encoding XML, there was no BOM, but we found <?xml encoding=\"UTF-16\", which is illegal");
          break;
          }
        }
      }
    else
      // No BOM, not XML, might as well try UTF-8
      enc = "UTF-8";
    }
  try
    {
    return new String(b, skip, b.length-skip, enc);
    }
  catch ( UnsupportedEncodingException e )
    {
    throw new FixConvException("can't convert byte[] to String using encoding "+enc, e);
    }
  }
//...e
//...sstringToBytes:2:
public static byte[] stringToBytes(String s, String enc)
  throws FixConvException
  {
  if ( enc == null )
    enc = "UTF-8";
  try
    {
    if ( enc.equalsIgnoreCase("XML") )
      {
      if ( s.startsWith("<?xml") && wsChar(s.charAt(5)) )
        {
        String token2 = "", token1 = "", token0 = "";
        int i = 6;
        for ( ;; )
          {
          StringBuffer sb = new StringBuffer();
          i = nextToken(s, i, sb);
          if ( i == -1 )
            {
            enc = "UTF-8";
            break;
            }
          token2 = token1; token1 = token0; token0 = sb.toString();
          if ( token2.equals("encoding") && token1.equals("=") && token0.startsWith("\"") )
            {
            enc = token0.substring(1);
            if ( enc.equals("UTF-16BE") || enc.equals("UTF-16LE") )
              throw new FixConvException("can't convert String to byte[] using encoding XML, we found <?xml encoding=\""+enc+"\", which is illegal");
            // getBytes() will return BOM preceeded big endian data, as per
            // http://java.sun.com/javase/6/docs/api/java/nio/charset/Charset.html
            break;
            }
          }
        }
      else
        enc = "UTF-8"; // no <?xml prefix, so use default
      }
    return s.getBytes(enc);
    }
  catch ( UnsupportedEncodingException e )
    {
    throw new FixConvException("can't convert String to byte[] using encoding "+enc, e);
    }
  }
//...e

//...sstrToDoc:2:
protected static Document strToDoc(String s)
  throws FixConvException
  {
  try
    {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    // not validating
    // Note: we don't support validation against DTDs during parsing
    // and so we can't support ignoringelementcontentwhitespace either.
    dbf.setNamespaceAware(true); // must do this, else can't validate ok
    dbf.setCoalescing(true);
    dbf.setExpandEntityReferences(true);
    dbf.setIgnoringComments(true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    db.setErrorHandler(
      new ErrorHandler()
        {
        public void warning(SAXParseException e)
          throws SAXException
          {
          }
        public void error(SAXParseException e)
          throws SAXException
          {
          }
        public void fatalError(SAXParseException e)
          throws SAXException
          {
          }
        }
      );
    Document d = db.parse(new InputSource(new StringReader(s)));
      // Note: we avoid using new InputStream(new StringInputStream()),
      // as we want to avoid working at the byte level,
      // after all, we already have a String, which is a sequence of chars.
      // Note that StringInputStream is deprecated for this reason.
    return d;
    }
  catch ( Exception e )
    {
    throw new FixConvException("error parsing XML document", e);
    }
  }
//...e
//...sdocToStr:2:
protected static String docToStr(Document d)
  throws FixConvException
  {
  try
    {
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer t = tf.newTransformer(); // the "identity" transformation
    t.setOutputProperty("indent", "yes");
    t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    CharArrayWriter caw = new CharArrayWriter();
    t.transform(
      new DOMSource((Node) d),
      new StreamResult(caw)
      );
    caw.flush();
    return caw.toString();
    }
  catch ( Exception e )
    {
    throw new FixConvException("error serialising XML", e);
    }
  }
//...e

  protected FixRepo repo;

  protected Map<String,String> messageEncodingToCharset = new HashMap<String,String>();

//...sFixConv:2:
public FixConv(Document d)
  throws FixConvException
  {
  // Ensure we can make from FIX message encodings (per FixTag.ID_MessageEncoding)
  // to the canonical charset name used in java.io.
  // The names on the right are supported by Sun Java.
  messageEncodingToCharset.put("EUC-JP"     , "EUC_JP"   );
  messageEncodingToCharset.put("ISO-2022-JP", "ISO2022JP");
  messageEncodingToCharset.put("Shift_JIS"  , "SJIS"     );
  messageEncodingToCharset.put("UTF-8"      , "UTF-8"    );

  repo = new FixRepo(d);
  }
//...e

  public static final String NS_STARTS_WITH = "http://www.fixprotocol.org/FIXML-";
  public static final String HDR_NAME = "StandardHeader";
  public static final String HDR_ABBRNAME = "Hdr";

  // Conversion from FIXML to tag=value

//...sfirstElement:2:
protected static Element firstElement(Node n)
  {
  while ( n != null && n.getNodeType() != Node.ELEMENT_NODE )
    n = n.getNextSibling();
  return (Element) n;
  }
//...e
//...snextElement:2:
protected static Element nextElement(Node n)
  {
  do
    n = n.getNextSibling();
  while ( n != null && n.getNodeType() != Node.ELEMENT_NODE );
  return (Element) n;
  }
//...e

//...saddTag:2:
public void addTag(FixTagMessage ftm, FixTag t, String abbrName)
  {
  if ( t.value != null )
    log("addTag "+abbrName+" "+t.id+"="+FixTagMessage.bytesToString(t.value));
  else
    log("addTag "+abbrName+" "+t.id);
  ftm.addTag(t);
  }
//...e
//...sxmlToTagError:2:
protected void xmlToTagError(String error)
  throws FixConvException
  {
  log("xmlToTagError: "+error);
  throw new FixConvException(error);
  }
//...e
//...sxmlToTagElement:2:
// Attributes on element correspond to fieldRefs
// Nested elements may be componentRefs or repeatingGroups

//...sstringToValue:2:
//...sscanNumber:2:
private static int scanNumber(String s, int i, StringBuffer sb)
  {
  sb.setLength(0);
  while ( i < s.length() && Character.isDigit(s.charAt(i)) )
    sb.append(s.charAt(i++));
  return i;
  }
//...e
//...spad:2:
private static String pad(int value, int width)
  {
  String s = Integer.toString(value);
  while ( s.length() < width )
    s = "0"+s;
  return s;
  }
//...e

protected byte[] stringToValue(
  FixTagMessage ftm,
  String val,
  FixField f
  )
  throws FixConvException
  {  
  try
    {
    switch ( f.convType )
      {
//...sText:8:
case Text:
  // The vastly most common case
  return val.getBytes("US-ASCII");
//...e
//...sUTCTimestamp:8:
case UTCTimestamp:
  // Convert from YYYY-MM-DDThh:mm:ss[.ttt] (ie: from xs:dateTime)
  //           to YYYYMMDD-hh:mm:ss[.ttt]
  // I think I have to worry about Z|(+|-)OO[:oo] offsets
  // as unfortunately I have seen this in the field
  {
  if ( val.length() < 19 )
    throw new FixConvException("can't map xs:dateTime "+val+" to UTCTimestamp");
  Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
  StringBuffer sb = new StringBuffer();
  int i = 0;
  i = scanNumber(val, i, sb);
  cal.set(Calendar.YEAR, Integer.parseInt(sb.toString()));
  if ( i == val.length() || val.charAt(i) != '-' )
    throw new FixConvException("can't map xs:dateTime "+val+" to UTCTimestamp, expected -");
  i = scanNumber(val, i+1, sb);
  cal.set(Calendar.MONTH, Integer.parseInt(sb.toString())-1);
  if ( i == val.length() || val.charAt(i) != '-' )
    throw new FixConvException("can't map xs:dateTime "+val+" to UTCTimestamp, expected -");
  i = scanNumber(val, i+1, sb);
  cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(sb.toString()));
  if ( i == val.length() || val.charAt(i) != 'T' )
    throw new FixConvException("can't map xs:dateTime "+val+" to UTCTimestamp, expected T");
  i = scanNumber(val, i+1, sb);
  cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(sb.toString()));
  if ( i == val.length() || val.charAt(i) != ':' )
    throw new FixConvException("can't map xs:dateTime "+val+" to UTCTimestamp, expected :");
  i = scanNumber(val, i+1, sb);
  cal.set(Calendar.MINUTE, Integer.parseInt(sb.toString()));
  if ( i == val.length() || val.charAt(i) != ':' )
    throw new FixConvException("can't map xs:dateTime "+val+" to UTCTimestamp, expected :");
  i = scanNumber(val, i+1, sb);
  cal.set(Calendar.SECOND, Integer.parseInt(sb.toString()));
  boolean gotMillis;
  if ( i < val.length() && val.charAt(i) == '.' )
    {
    gotMillis = true;
    i = scanNumber(val, i+1, sb);
    cal.set(Calendar.MILLISECOND, Integer.parseInt(sb.toString()));
    }
  else
    {
    gotMillis = false;
    cal.set(Calendar.MILLISECOND, 0);
    }
  if ( i < val.length() )
    {
    if ( val.charAt(i) == 'Z' )
      ; // Nothing more to do
    else if ( val.charAt(i) == '+' || val.charAt(i) == '-' )
      {
      int sign = ( val.charAt(i) == '-' ) ? 1 : -1;
      i = scanNumber(val, i+1, sb);
      cal.add(Calendar.HOUR_OF_DAY, sign*Integer.parseInt(sb.toString()));
      if ( i < val.length() )
        {
        if ( val.charAt(i) == ':' )
          {
          i = scanNumber(val, i+1, sb);
          cal.add(Calendar.MINUTE, sign*Integer.parseInt(sb.toString()));
          }
        else
          throw new FixConvException("can't map xs:dateTime "+val+" to UTCTimestamp, expected nothing or :");
        }
      }
    else
      throw new FixConvException("can't map xs:dateTime "+val+" to UTCTimestamp, expected nothing, Z, + or -");
    }
  val = pad(cal.get(Calendar.YEAR),4)
      + pad(cal.get(Calendar.MONTH)+1,2)
      + pad(cal.get(Calendar.DAY_OF_MONTH),2)
      + "-"
      + pad(cal.get(Calendar.HOUR_OF_DAY),2)
      + ":"
      + pad(cal.get(Calendar.MINUTE),2)
      + ":"
      + pad(cal.get(Calendar.SECOND),2);
  if ( gotMillis )
    val += ( "." + pad(cal.get(Calendar.MILLISECOND),3) );
  return val.getBytes("US-ASCII");
  }
//...e
//...sUTCTimeOnly:8:
case UTCTimeOnly:
  // Convert from hh:mm:ss[.ttt] (is: from xs:time)
  //           to hh:mm:ss[.ttt]
  // We can't worry about Z|(+|-)OO[:oo] as we don't know the corresponding date field
  {
  if ( val.length() < 8 )
    throw new FixConvException("can't map xs:time "+val+" to UTCTimeOnly");
  int len = ( val.length() >= 12 && val.charAt(8) == '.' ) ? 12 : 8;
  if ( len < val.length() )
    log("mapping of xs:time "+val+" to UTCTimeOnly discards TZ suffix");
  return val.substring(0,len).getBytes("US-ASCII");
  }
//...e
//...sUTCDateOnly:8:
case UTCDateOnly:
  // Convert from YYYY-MM-DD (ie: from xs:date)
  //           to YYYYMMDD
  // We can't worry about Z|(+|-)OO[:oo], as we don't know the hour in the day
  {
  if ( val.length() < 10 )
    throw new FixConvException("can't map xs:date "+val+" to UTCDateOnly");
  if ( val.length() > 10 )
    log("mapping of xs:date to UTCDateOnly discards TZ suffix");
  val = val.substring(0,4)+val.substring(5,7)+val.substring(8,10);
  return val.getBytes("US-ASCII");
  }
//...e
//...sLocalMktDate:8:
case LocalMktDate:
  // Convert from YYYY-MM-DD (ie: from xs:date)
  //           to YYYYMMDD
  // We can't worry about Z|(+|-)OO[:oo], as we don't know the hour in the day
  {
  if ( val.length() < 10 )
    throw new FixConvException("can't map xs:date "+val+" to LocalMktDate");
  if ( val.length() > 10 )
    log("mapping of xs:date to LocalMktDate discards TZ suffix");
  val = val.substring(0,4)+val.substring(5,7)+val.substring(8,10);
  return val.getBytes("US-ASCII");
  }
//...e
//...sTZTimeOnly:8:
case TZTimeOnly:
  // Convert from hh:mm:ss[Z|(+|-)OO[:oo]] (ie: from xs:time)
  //           to hh:mm[:ss][Z|(+|-)OO[:oo]]
  // We can't worry about Z|(+|-)OO[:oo] as we don't know the corresponding date field
  {
  if ( val.length() < 8 )
    throw new FixConvException("can't map xs:time "+val+" to TZTimeOnly");
  int len = ( val.length() >= 12 && val.charAt(8) == '.' ) ? 12 : 8;
  if ( len < val.length() )
    log("mapping of xs:time "+val+" to TZTimeOnly discards TZ suffix");
  return val.substring(0,len).getBytes("US-ASCII");
  }
//...e
//...sTZTimestamp:8:
case TZTimestamp:
  // Convert from YYYY-MM-DDThh:mm:ss[Z|(+|-)oo[:oo]] (ie: from xs:dateTime)
  //           to YYYYMMDD-hh:mm:ss[Z|(+|-)oo[:oo]]
  {
  if ( val.length() < 19 )
    throw new FixConvException("can't map xs:dateTime "+val+" to TZTimestamp");
  val = val.substring(0,4)+val.substring(5,7)+val.substring(8,10)+"-"+val.substring(11);
  return val.getBytes("US-ASCII");
  }
//...e
//...sEncodedText:8:
case EncodedText:
  {
  String messageEncoding = ftm.getMessageEncoding();
  if ( messageEncoding == null )
    // The MsgEncd tag is in the Hdr, and is processed before data encoded by it
    // so we shouldn't get here
    xmlToTagError("don't know the message encoding, so can't encode field "+f.id);
  String charset = messageEncodingToCharset.get(messageEncoding);
  if ( charset == null )
    xmlToTagError("don't know about message encoding "+messageEncoding);
  return val.getBytes(charset);
  }
//...e
//...sBase64:8:
case Base64:
  return base64Decode(val);
//...e
      }
    }
  catch ( UnsupportedEncodingException uee )
    {
    xmlToTagError("charset not supported");
    }
  return null; // can't get here
  }
//...e

public void xmlToTagElement(
  Element e,
  Element eBatchHdr, // consider inheriting from this
  List<FixMessageEntity> entities,
  FixTagMessage ftm,
  FixMessage m,
  FixVersion v
  )
  throws FixConvException
  {
  Element e2 = firstElement(e.getFirstChild());
  for ( FixMessageEntity me : entities )
    if ( me instanceof FixFieldRef )
      {
      FixFieldRef fr = (FixFieldRef) me;
      FixField f = fr.field;
      if ( f.convType == FixField.ConvType.XMLData )
        {
        if ( e2 != null )
          try
            {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setCoalescing(true);
            dbf.setExpandEntityReferences(true);
            dbf.setIgnoringComments(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document d = db.newDocument();
            d.appendChild( (Element) d.importNode(e2, true) );
            byte[] b = stringToBytes(docToStr(d), null);
            FixTag tLen = new FixTag(f.assocDataLenTagField.id, Integer.toString(b.length));
            addTag(ftm, tLen, "(XMLData length)");
            FixTag t = new FixTag(f.id, b);
            addTag(ftm, t, "(XMLData)");
            e2 = nextElement(e2);
            }
          catch ( ParserConfigurationException pce )
            {
            throw new FixConvException("JAXP configuration problem", pce);
            }
        }
      else
        {
        String abbrName;
        if ( f.baseCategory != null &&
             f.baseCategory.equals(m.category) )
          abbrName = f.baseCategoryAbbrName;
        else
          abbrName = f.abbrName;
        if ( abbrName != null ) 
          {
          String val = e.getAttribute(abbrName);
          if ( val.equals("") )
            {
            if (e.getLocalName().equals(HDR_ABBRNAME) && eBatchHdr != null )
              // Cope with the message has a Hdr, but doesn't define a field
              // but there is a Batch Hdr that does, which we need to inherit
              val = eBatchHdr.getAttribute(f.abbrName);
            else if ( f.id == FixTag.ID_ApplVerID && v.applVerIDEnum != null )
              // Ensure the message has an ApplVerID
              val = v.applVerIDEnum;
            else if ( f.id == FixTag.ID_ApplExtID )
              // Ensure the message has an ApplExtID, if appropriate
              {
              Element eFIXML = e;
              while ( ! eFIXML.getLocalName().equals("FIXML") )
                eFIXML = (Element) eFIXML.getParentNode();
              val = eFIXML.getAttribute("xv");
              }
            else if ( f.id == FixTag.ID_CstmApplVerID && ! v.customVersion.equals("") )
              // Ensure the message has an CstmApplVerID, if appropriate
              val = v.customVersion;
            }
          if ( ! val.equals("") )
            {
            byte[] b = stringToValue(ftm, val, f);
            if ( f.assocDataLenTagField != null )
              {
              FixTag tLen = new FixTag(f.assocDataLenTagField.id, Integer.toString(b.length));
              addTag(ftm, tLen, "(length)");
              }
            FixTag t = new FixTag(f.id, b);
            addTag(ftm, t, abbrName);
            }
          }
        }
      }
    else if ( me instanceof FixComponentRef )
      {
      FixComponentRef cr = (FixComponentRef) me;
      FixComponent c = cr.component;
      if ( c.repeating )
        // This is the common case - a component containing a single repeatingGroup
        // The repeating group elements will not be in a containing element
        {
        if ( c.entities.size() != 1 )
          throw new FixConvException("repeating component should contain one message entity");
        FixMessageEntity me2 = c.entities.get(0);
        if ( ! ( me2 instanceof FixRepeatingGroup ) )
          throw new FixConvException("repeating component should contain one repeating group");
        FixRepeatingGroup rg = (FixRepeatingGroup) me2;
        if ( e2 != null && e2.getLocalName().equals(c.abbrName) )
          {
          FixTag tNo = new FixTag(rg.field.id); // patch later
          addTag(ftm, tNo, "(no)");
          int n = 0;
          do
            {
            xmlToTagRepeatingGroup(e2, null, ftm, rg, m, v);
            ++n;
            e2 = nextElement(e2);
            }
          while ( e2 != null && e2.getLocalName().equals(c.abbrName) );    
          tNo.setValueInt(n);
          }
        }
      else
        {
        if ( e2 != null && e2.getLocalName().equals(c.abbrName) )
          {
          xmlToTagComponent(e2, eBatchHdr, ftm, c, m, v);
          e2 = nextElement(e2);
          }
        else if ( c.name.equals(HDR_NAME) && eBatchHdr != null )
          // Cope with the case there is a Batch Hdr,
          // but a nested message does not have a Hdr
          xmlToTagComponent(eBatchHdr, null, ftm, c, m, v);
        }
      }
    else if ( me instanceof FixRepeatingGroup )
      // This is the uncommon case
      // typically a message containing a repeatingGroup
      // eg: FIX.4.4 Logon message contains NoMsgTypes repeatingGroup
      {
      throw new FixConvException("Unified FIX Repository content problem - repeatingGroup must be within a component");
/*
      log("@@@ what is the abbrName to use (it can't be rg.field.abbrName)");
      String abbrName = "???";
      FixRepeatingGroup rg = (FixRepeatingGroup) me;
      if ( e2 != null && e2.getLocalName().equals(abbrName) )
        {
        FixTag tNo = new FixTag(rg.field.id); // patch later
        addTag(ftm, tNo, "(no)");
        int n = 0;
        do
          {
          xmlToTagRepeatingGroup(e2, null, ftm, rg, m, v);
          ++n;
          e2 = nextElement(e2);
          }
        while ( e2 != null && e2.getLocalName().equals(abbrName) );    
        tNo.setValueInt(n);
        }
*/
      }
  if ( e2 != null )
    xmlToTagError("unexpected content "+e2.getLocalName());
  }
//...e
//...sxmlToTagRepeatingGroup:2:
public void xmlToTagRepeatingGroup(
  Element e,
  Element eBatchHdr,
  FixTagMessage ftm,
  FixRepeatingGroup rg,
  FixMessage m,
  FixVersion v
  )
  throws FixConvException
  {
  push("xmlToTagRepeatingGroup "+rg.name);
  xmlToTagElement(e, eBatchHdr, rg.entities, ftm, m, v);
  pop();
  }
//...e
//...sxmlToTagComponent:2:
public void xmlToTagComponent(
  Element e,
  Element eBatchHdr,
  FixTagMessage ftm,
  FixComponent c,
  FixMessage m,
  FixVersion v
  )
  throws FixConvException
  {
  push("xmlToTagComponent "+c.name);
  xmlToTagElement(e, eBatchHdr, c.entities, ftm, m, v);
  pop();
  }
//...e
//...sxmlToTagMessage:2:
public FixTagMessage xmlToTagMessage(
  Element e,
  Element eBatchHdr,
  FixMessage m,
  FixVersion v
  )
  throws FixConvException
  {
  push("xmlToTagMessage "+m.name);
  FixTagMessage ftm = new FixTagMessage();
  addTag(ftm, new FixTag(FixTag.ID_BeginString, v.beginString), "(header)");
  FixTag tBodyLength = new FixTag(FixTag.ID_BodyLength);
  addTag(ftm, tBodyLength, "(header)");
  int tagIndex = ftm.tags.size();
  addTag(ftm, new FixTag(FixTag.ID_MsgType, m.msgType), "(header)");
  xmlToTagElement(e, eBatchHdr, m.entities, ftm, m, v);
  tBodyLength.setValueInt(ftm.getLength(tagIndex));
  String checkSum = Integer.toString(ftm.getCheckSum()&0xff);
  checkSum = ("00"+checkSum).substring(2+checkSum.length()-3);
  addTag(ftm, new FixTag(FixTag.ID_CheckSum, checkSum), "(trailer)");
  pop();
  return ftm;
  }
//...e
//...sxmlToTag:2:
//...sversionOfFIXML:2:
protected String versionOfFIXML(Element e, String extMaj, String extMin, String extSP, String extCv)
  throws FixConvException
  {
  // Look at the namespace
  String ns = e.getNamespaceURI();
    // eg: ends with "4-4"     for FIX 4.4
    //               "5-0"     for FIX 5.0
    //               "5-0-SP2" for FIX 5.0 with SP2
    //               "5"       for FIX 5.0 with SP3 or later
  String nsMaj = null;
  String nsMin = null;
  String nsSP  = null;
  if ( ns != null )
    {
    if ( ! ns.startsWith(NS_STARTS_WITH) )
      xmlToTagError("can't determine version: namespace is "+ns+", but must start with "+NS_STARTS_WITH);
    StringTokenizer st = new StringTokenizer(ns.substring(NS_STARTS_WITH.length()), "-");
    if ( !st.hasMoreTokens() )
      xmlToTagError("can't determine version: no major version in namespace "+ns);
    nsMaj = st.nextToken();
    if ( st.hasMoreTokens() )
      {
      nsMin = st.nextToken();
      if ( st.hasMoreTokens() )
        nsSP = st.nextToken();
      }
    }

  // Look at the root elements versioning attributes
  String vStr = e.getAttribute("v"); // eg: "4.4", "FIX.5.0", or "" 
  if ( ! vStr.equals("") && ! vStr.startsWith("FIX.") )
    vStr = "FIX."+vStr; // now "FIX.4.4", "FIX.5.0SP1", or ""
  String cvStr = e.getAttribute("cv"); // custom verson name, eg: "CME1", or ""

  String version;
  if ( nsMaj == null )
    // no information from namespace
    // use the v= attribute if present
    // else look for external config
    {
    if ( ! vStr.equals("") )
      version = vStr;
    else
      {
      if ( extMaj == null || extMin == null )
        xmlToTagError("can't determine version: no namespace, no v attribute and no external configuration");
      version = "FIX."+extMaj+"."+extMin;
      }
    if ( extSP != null )
      version += extSP;
    }
  else if ( nsMin == null )
    // the major version is in the namespace
    // this is the new FIX.5.0SP3 and onwards style
    // use v= attribute if present (but check major part matches)
    // else look for external config (again check major part matches)
    {
    if ( ! vStr.equals("") )
      {
      if ( ! vStr.startsWith("FIX."+nsMaj+".") )
        xmlToTagError("inconsistent version: namespace says "+ns+", v attribute says "+vStr);
      version = vStr;
      }
    else
      {
      if ( extMaj == null || extMin == null )
        xmlToTagError("can't determine version: no namespace, no v attribute and no external configuration");
      if ( !extMaj.equals(nsMaj) )
        xmlToTagError("inconsistent version: namespace says "+ns+", external configuration says "+vStr);
      version = "FIX."+extMaj+"."+extMin;
      }
    if ( extSP != null )
      version += extSP;
    }
  else
    // we have major and minor version in the namespace
    // we may also have the service pack in the namespace
    // we trust what the namespace says
    // but if there is a v= attribute, it must agree with it
    {
    version = "FIX."+nsMaj+"."+nsMin+(nsSP!=null?nsSP:"");
    if ( ! vStr.equals("") && ! version.startsWith(vStr) )
      xmlToTagError("inconsistent version: namespace says "+ns+", v attribute says "+vStr);
    }

  String customVersion;
  if ( ! cvStr.equals("") )
    customVersion = cvStr;
  else if ( extCv != null )
    customVersion = extCv;
  else
    customVersion = "";

  return version+"/"+customVersion;
  }
//...e

public List<FixTagMessage> xmlToTag(
  Document d,
  String extMaj, // eg: "5", or null
  String extMin, // eg: "0", or null
  String extSP,  // eg: "SP2", or null
  String extCv   // eg: "mycustomversion", or null
  )
  throws FixConvException
  {
  push("xmlToTag");

  List<FixTagMessage> ftms = new ArrayList<FixTagMessage>();

  Element e = d.getDocumentElement();
  if ( ! e.getLocalName().equals("FIXML") )
    xmlToTagError("root element should be FIXML");

  String version = versionOfFIXML(e, extMaj, extMin, extSP, extCv);
  FixVersion v = repo.getVersion(version);
  if ( v == null )
    xmlToTagError("don't understand FIX version "+version);

  Element e2 = firstElement(e.getFirstChild());
  if ( e2 == null )
    xmlToTagError("expected Batch or message element");

  if ( e2.getLocalName().equals("Batch") )
    // can have one or more Batches,
    // each Batch has optional Hdr, followed by zero or more Messages
    {
    push("batch");
    do
      {
      Element e3 = firstElement(e2.getFirstChild());
      Element eBatchHdr;
      if ( e3 != null && e3.getNodeName().equals("Hdr") )
        // Batch Hdr has the same things that a message Hdr has,
        // although the generated schema has provision for additional stuff.
        // Also, there is no seperate component defined for the Batch Hdr.
        {
        eBatchHdr = e3;
        e3 = nextElement(e3);
        }
      else
        eBatchHdr = null;
      while ( e3 != null )
        {
        FixMessage m = v.messagesByAbbrName.get(e3.getLocalName());
        if ( m == null )
          xmlToTagError("FIXML message has element "+e3.getLocalName()+" which does not correspond to a message in FIX Repository for version "+version);
        FixTagMessage ftm = xmlToTagMessage(e3, eBatchHdr, m, v);
        ftms.add(ftm);
        e3 = nextElement(e3);
        }
      e2 = nextElement(e2);
      }
    while ( e2 != null && e2.getLocalName().equals("Batch") );
    pop();
    }
  else
    // Should be a single message
    {
    FixMessage m = v.messagesByAbbrName.get(e2.getLocalName());
    if ( m == null )
      xmlToTagError("FIXML message has element "+e2.getLocalName()+" which does not correspond to a message in FIX Repository for version "+version);
    FixTagMessage ftm = xmlToTagMessage(e2, null, m, v);
    ftms.add(ftm);
    e2 = nextElement(e2);
    }
  if ( e2 != null )
    xmlToTagError("unexpected message content after Batch(es) or message");
  pop();

  return ftms;
  }
//...e

  // Converting between of FIX tag=value messages and byte[]

//...sfixTagMessageToBytes:2:
// This method is really only here for symmetry with bytesToFixTagMessage
public static byte[] fixTagMessageToBytes(FixTagMessage ftm)
  throws FixConvException
  {
  return ftm.toBytes();
  }
//...e
//...sbytesToFixTagMessage:2:
public FixTagMessage bytesToFixTagMessage(
  byte[] b,
  String extMaj, // eg: "5", or null
  String extMin, // eg: "0", or null
  String extSP,  // eg: "SP2", or null
  String extCv   // eg: "mycustomversion", or null
  )
  throws FixConvException
  {
  String version = null;
  FixVersion v = null; // don't know the version yet
  FixTagMessage ftm = new FixTagMessage();
  int p = 0;
  while ( p < b.length )
    {
    int id = 0;
    for ( ; p < b.length && b[p] >= (byte) '0' && b[p] <= (byte) '9' ; p++ )
      id = id*10 + ( (byte) b[p]-'0' );
    if ( p == b.length || b[p] != '=' )
      throw new FixConvException("bad tag (tag="+id+"), at byte position "+p);
    p++;
    int length = -1;
    if ( v != null )
      {
      FixField f = v.fieldsById.get(id);
      if ( f == null )
        throw new FixConvException("unknown field (tag="+id+"), at byte position "+p);
      if ( f.assocDataLenTagField != null )
        {
        FixTag tLen = ftm.getTagBackwards(f.assocDataLenTagField.id);
        if ( tLen == null )
          throw new FixConvException("binary field (tag="+id+") without preceeding length tag "+f.assocDataLenTagField.id+", at byte position "+p);
        length = tLen.getValueInt();
        }
      }
    if ( length == -1 )
      // Scan upto SOH
      {
      int q = p;
      while ( q < b.length && b[q] != 1 )
        q++;
      byte[] value = new byte[q-p];
      System.arraycopy(b, p, value, 0, q-p);
      ftm.addTag(new FixTag(id, value));
      p = q;
      try
        {
        if ( id == FixTag.ID_BeginString )
          // Time to work out an initial stab at the version
          {
          String beginString = new String(value, "US-ASCII");
          if ( beginString.startsWith("FIX.") )
            v = repo.getVersion(beginString+"/"+(extCv!=null?extCv:""));
          else if ( extMaj != null && extMin != null )
            v = repo.getVersion(extMaj+"."+extMin+(extSP!=null?extSP:"")+"/"+(extCv!=null?extCv:""));
          else
            v = null;
          // v could still be null at this point
          }
        else if ( id == FixTag.ID_ApplVerID )
          // This refines our knowledge from "FIXT.x.x" to "FIX.x.x" or "FIX.x.xSPx"
          {
          String applVerIDEnum = new String(value, "US-ASCII");
          version = repo.mapApplVerIDEnumToVersion(applVerIDEnum);
          if ( version == null )
            throw new FixConvException("don't know how to map ApplVerID field of "+applVerIDEnum+" to a FIX version");
          v = repo.getVersion(version+"/"+(extCv!=null?extCv:""));
          // v could still be null at this point
          }
        else if ( id == FixTag.ID_CstmApplVerID )
          // This refines our knowledge to include the custom version
          {
          if ( version == null )
            throw new FixConvException("got CstmApplVerID field, but haven't had ApplVerID field yet");
          String cstmApplVerID = new String(value, "US-ASCII");
          v = repo.getVersion(version+"/"+cstmApplVerID);
          if ( v == null )
            throw new FixConvException("FIX version "+version+"/"+cstmApplVerID+" not in the FIX Repository");
          }
        }
      catch ( UnsupportedEncodingException uee )
        {
        // Won't happen, US-ASCII is sure to be supported
        }
      }
    else
      // Read that many bytes
      {
      if ( p+length >= b.length )
        throw new FixConvException("binary field (tag="+id+") extends beyond end of message");
      byte[] value = new byte[length];
      System.arraycopy(b, p, value, 0, length);
      ftm.addTag(new FixTag(id, value));
      p += length;
      }
    if ( b[p] != 1 )
      throw new FixConvException("field (tag="+id+") not terminated by SOH, at byte position "+p);
    p++;
    }
  return ftm;
  }
//...e

  // Pretty printing

//...sfixTagMessageToPretty:2:
// This method is really only here for pretty printing tag=value messages
public String fixTagMessageToPretty(
  FixTagMessage ftm,
  String extMaj, // eg: "5", or null
  String extMin, // eg: "0", or null
  String extSP,  // eg: "SP2", or null
  String extCv   // eg: "mycustomversion", or null
  )
  throws FixConvException
  {
  String version = null;
  FixVersion v = null; // don't know the version yet
  StringBuilder sb = new StringBuilder();
  for ( int i = 0; i < ftm.tags.size(); i++ )
    {
    FixTag t = ftm.tags.elementAt(i);
    int id = t.id;
    sb.append(Integer.toString(id));
    if ( v != null )
      {
      sb.append("(");
      FixField f = v.fieldsById.get(id);
      sb.append( f != null ? f.name : "???" );
      sb.append(")");
      }
    else if ( id == FixTag.ID_BeginString )
      sb.append("(BeginString)");
    sb.append("=");
    sb.append(FixTagMessage.bytesToString(t.value));
    sb.append("\n");
    if ( id == FixTag.ID_BeginString )
      // Time to work out an initial stab at the version
      {
      String beginString = t.getValue();
      if ( beginString.startsWith("FIX.") )
        v = repo.getVersion(beginString+"/"+(extCv!=null?extCv:""));
      else if ( extMaj != null && extMin != null )
        v = repo.getVersion(extMaj+"."+extMin+(extSP!=null?extSP:"")+"/"+(extCv!=null?extCv:""));
      else
        v = null;
      // v could still be null at this point
      }
    else if ( id == FixTag.ID_ApplVerID )
      // This refines our knowledge from "FIXT.x.x" to "FIX.x.x" or "FIX.x.xSPx"
      {
      String applVerIDEnum = t.getValue();
      version = repo.mapApplVerIDEnumToVersion(applVerIDEnum);
      if ( version == null )
        throw new FixConvException("don't know how to map ApplVerID field of "+applVerIDEnum+" to a FIX version");
      v = repo.getVersion(version+"/"+(extCv!=null?extCv:""));
      // v could still be null at this point
      }
    else if ( id == FixTag.ID_CstmApplVerID )
      // This refines our knowledge to include the custom version
      {
      if ( version == null )
        throw new FixConvException("got CstmApplVerID field, but haven't had ApplVerID field yet");
      String cstmApplVerID = t.getValue();
      v = repo.getVersion(version+"/"+cstmApplVerID);
      if ( v == null )
        throw new FixConvException("FIX version "+version+"/"+cstmApplVerID+" not in the FIX Repository");
      }
    }
  return sb.toString();
  }
//...e

  // Conversion from tag=value to FIXML

//...sclass FixElements:2:
// Elements are added to a FixElements, and then when they've all been added
// they get transfered (in order) as children of a parent Element
// This is to cope with wildly out of order tags

public class FixElements
  {
//...sclass ElemPos:4:
public class ElemPos implements Comparable
  {
  protected int position;
  protected int repetition;
  public ElemPos(int position, int repetition)
    {
    this.position   = position;
    this.repetition = repetition;
    }
  public boolean equals(Object o)
    {
    ElemPos fep = (ElemPos) o;
    return position == fep.position && repetition == fep.repetition;
    }
  public int compareTo(Object o)
    {
    ElemPos fep = (ElemPos) o;
    if ( position   < fep.position   ) return -1;
    if ( position   > fep.position   ) return  1;
    if ( repetition < fep.repetition ) return -1;
    if ( repetition > fep.repetition ) return  1; 
    return 0;
    }
  }
//...e
  protected Map<String,Element> elementsByAbbrName = new HashMap<String,Element>();
  protected TreeMap<ElemPos,Element> elementsByPos = new TreeMap<ElemPos,Element>();
  public Element getElement(String abbrName)
    {
    return elementsByAbbrName.get(abbrName);
    }
  public void addElement(Element e, int position, int repetition)
    {
    elementsByAbbrName.put(e.getLocalName(), e);
    elementsByPos.put(new ElemPos(position, repetition), e);
    }
  public void toParent(Element eParent)
    {
    for ( ElemPos p : elementsByPos.keySet() )
      {
      Element e = elementsByPos.get(p);
      eParent.appendChild(e);
      }
    }
  }
//...e

//...stagToXmlFieldRef:2:
//...svalueToString:2:
protected String valueToString(
  FixTag t,
  FixField f,
  String charset
  )
  throws FixConvException
  {
  switch ( f.convType )
    {
//...sText:6:
case Text:
  // The vastly most common case
  return t.getValue();
//...e
//...sUTCTimestamp:6:
case UTCTimestamp:
  // Convert from YYYYMMDD-hh:mm:ss[.ttt]
  //           to YYYY-MM-DDThh:mm:ss[.ttt] (ie: to xs:dateTime)
  // Given this is UTC, its fine that the output has no Z|(+|-)OO[:oo]
  {
  String s = t.getValue();
  int i = s.indexOf("-");
  if ( i != 8 )
    throw new FixConvException("can't map UTCTimestamp "+s+" to xs:dateTime");
  return s.substring(0,4)+"-"+s.substring(4,6)+"-"+s.substring(6,8)+"T"+s.substring(9);
  }
//...e
//...sUTCTimeOnly:6:
case UTCTimeOnly:
  // Convert from hh:mm:ss[.ttt]
  //           to hh:mm:ss[.ttt] (ie: to xs:time)
  // Given this is UTC, its fine that the output has no Z|(+|-)OO[:oo]
  return t.getValue(); // Yes, thats right, no translation to do
//...e
//...sUTCDateOnly:6:
case UTCDateOnly:
  // Convert from YYYYMMDD
  //           to YYYY-MM-DD (ie: to xs:date)
  // Given this is UTC, its fine that the output has no Z|(+|-)OO[:oo]
  {
  String s = t.getValue();
  if ( s.length() != 8 )
    throw new FixConvException("can't map UTCDateOnly "+s+" to xs:date");
  return s.substring(0,4)+"-"+s.substring(4,6)+"-"+s.substring(6);
  }
//...e
//...sLocalMktDate:6:
case LocalMktDate:
  // Convert from YYYYMMDD
  //           to YYYY-MM-DD (ie: to xs:date)
  // IMPORTANT: The generated xs:date has no Z|(+|-)OO[:oo] suffix
  //            FIXML readers should be careful how they interpret such fields
  {
  String s = t.getValue();
  if ( s.length() != 8 )
    throw new FixConvException("can't map LocalMktDate "+s+" to xs:date");
  return s.substring(0,4)+"-"+s.substring(4,6)+"-"+s.substring(6);
  }
//...e
//...sTZTimeOnly:6:
case TZTimeOnly:
  // Convert from hh:mm[:ss][Z|(+|-)OO[:oo]]
  //           to hh:mm:ss[Z|(+|-)OO[:oo]] (ie: to xs:time)
  // IMPORTANT: The generated xs:date has no Z|(+|-)OO[:oo] suffix
  //            FIXML readers should be careful how they interpret such fields
  {
  String s = t.getValue();
  if ( s.length() >= 6 && s.charAt(5) != ':' )
    s = s.substring(0,5)+":00"+s.substring(5); // add missing seconds
  return s;
  }
//...e
//...sTZTimestamp:6:
case TZTimestamp:
  // Convert from YYYYMMDD-hh:mm:ss[Z|(+|-)oo[:oo]]
  //           to YYYY-MM-DDThh:mm:ss[Z|(+|-)oo[:oo]] (ie: to xs:dateTime)
  {
  String s = t.getValue();
  int i = s.indexOf("-");
  if ( i != 8 )
    throw new FixConvException("can't map TZTimestamp "+s+" to xs:dateTime");
  return s.substring(0,4)+"-"+s.substring(4,6)+"-"+s.substring(6,8)+"T"+s.substring(9);
  }
//...e
//...sEncodedText:6:
case EncodedText:
  {
  if ( charset == null )
    throw new FixConvException("encoded tag="+t.id+" in message, but MessageEncoding not specified");
  try
    {
    return new String(t.value, charset);
    }
  catch ( UnsupportedEncodingException uee )
    {
    throw new FixConvException("charset "+charset+" not supported");
    }
  }
//...e
//...sBase64:6:
case Base64:
  return base64Encode(t.value, t.value.length);
//...e
    }
  return null; // can't get here
  }
//...e

protected int tagToXmlFieldRef(
  FixTagMessage ftm, int i,
  Element e, String charset,
  FixFieldRef fr,
  FixMessage m
  )
  throws FixConvException
  {
  FixField f = fr.field;
  FixTag t = ftm.tags.elementAt(i);
  if ( f.convType == FixField.ConvType.XMLData )
    // XMLData fields will be marked with notReqXML="1"
    // because they have no representation in the XSDs.
    // So we don't check the notReqXML flag here.
    {
    log("tagToXmlFieldRef "+t.id+" of type XMLData");
    Document d = strToDoc(bytesToString(t.value, null));
    Element eNested = d.getDocumentElement();
    e.appendChild( e.getOwnerDocument().adoptNode(eNested) );
    }
  else if ( ! f.notReqXML && f.assocDataTag == -1 )
    {
    String s = valueToString(t, f, charset);
    String abbrName = f.baseCategoryAbbrName != null
      ? f.baseCategoryAbbrName
      : f.abbrName;
    log("tagToXmlFieldRef "+t.id+" "+abbrName+"="+s);
    e.setAttribute(abbrName, s);
    }
  return i+1;
  }
//...e
//...stagToXmlComponentRef:2:
// The tags for components could be interleaved.
// So we may get called to create a new element, or to add to an existing one.

protected int tagToXmlComponentRef(
  FixTagMessage ftm, int i, int n,
  Document d, FixElements fe, String ns, String charset,
  FixComponentRef cr,
  FixMessage m,
  FixVersion v
  )
  throws FixConvException 
  {
  FixComponent c = cr.component;
  push("tagToXmlComponentRef "+c.abbrName);
  if ( c.repeating )
    // This is the common case for repeatingGroups
    // a component contains a single repeatingGroup
    // Don't create a XML element under which to put each of the groups
    {
    if ( c.entities.size() != 1 )
      throw new FixConvException("repeating component should contain one message entity");
    FixMessageEntity me = c.entities.get(0);
    if ( ! ( me instanceof FixRepeatingGroup ) )
      throw new FixConvException("repeating component should contain one repeating group");
    i = tagToXmlRepeatingGroup(ftm, i, n, d, fe, c.abbrName, cr.position, ns, charset, (FixRepeatingGroup) me, m, v);
    }
  else
    {
    Element e2existing = fe.getElement(c.abbrName);
    Element e2 = ( e2existing != null ) ? e2existing : d.createElementNS(ns, c.abbrName);
    FixElements fe2 = new FixElements();
    while ( i < n )
      {
      FixTag t = ftm.tags.elementAt(i);
      FixMessageEntity me = c.tagIndex.get(t.id);
      if ( me == null )
        break;
      i = tagToXmlMessageEntity(ftm, i, n, d, e2, fe2, ns, charset, me, m, v);
      }
    fe2.toParent(e2); // put all the child elements below e2 in the right order
    if ( e2.getFirstChild() != null || e2.getAttributes().getLength() > 0 ) 
      if ( e2 != e2existing )
        fe.addElement(e2, cr.position, 0); // include e2 in elements
    }
  pop();
  return i;
  }
//...e
//...stagToXmlRepeatingGroup:2:
protected int tagToXmlRepeatingGroup(
  FixTagMessage ftm, int i, int n,
  Document d, FixElements fe, String abbrName, int crPosition, String ns, String charset,
  FixRepeatingGroup rg,
  FixMessage m,
  FixVersion v
  )
  throws FixConvException 
  {
  push("tagToXmlRepeatingGroup "+abbrName);
  // Process the NoXxx tag
  FixTag tNo = ftm.tags.elementAt(i);
  if ( tNo.id != rg.field.id )
    throw new FixConvException("repeating group must start with NoXxx tag, tag="+rg.field.id);
  int nr = tNo.getValueInt();
  if ( nr <= 0 )
    throw new FixConvException("repeating group must have at least one group, tag="+rg.field.id+", value="+nr);
  i++;

  // For each repetition...
  for ( int r = 0; r < nr; r++ )
    {
    push("repetition "+(r+1)+"/"+nr);
    // we are expecting at least the first tag
    if ( i == n )
      throw new FixConvException("missing first field of group at field position "+i);
    FixTag tFirst = ftm.tags.elementAt(i);
    if ( rg.tagIndex.get(tFirst.id) == null )
      throw new FixConvException("group should not start with tag="+tFirst.id+" at field position "+i);

    // Look to see how much of the message we process this time around
    int nThisRep;
    if ( r+1 < nr )
      {
      for ( nThisRep = i+1; nThisRep < n; nThisRep++ )
        if ( ftm.tags.elementAt(nThisRep).id == tFirst.id )
          break;
      }
    else
      nThisRep = ftm.tags.size();

    Element e2 = d.createElementNS(ns, abbrName);

    FixElements fe2 = new FixElements();

    while ( i < nThisRep )
      {
      FixTag t = ftm.tags.elementAt(i);
      FixMessageEntity me = rg.tagIndex.get(t.id);
      if ( me == null )
        break;
      i = tagToXmlMessageEntity(ftm, i, nThisRep, d, e2, fe2, ns, charset, me, m, v);
      }
    fe2.toParent(e2); // put all the child elements below e2 in the right order
    fe.addElement(e2, crPosition != -1 ? crPosition : rg.position, r); // add this repetition to elements
    pop();
    }
  pop();
  return i;
  }
//...e
//...stagToXmlMessageEntity:2:
protected int tagToXmlMessageEntity(
  FixTagMessage ftm, int i, int n,
  Document d, Element e, FixElements fe, String ns, String charset,
  FixMessageEntity me,
  FixMessage m,
  FixVersion v
  )
  throws FixConvException
  {
  if ( me instanceof FixFieldRef )
    i = tagToXmlFieldRef(ftm, i, e, charset, (FixFieldRef) me, m);
  else if ( me instanceof FixComponentRef )
    i = tagToXmlComponentRef(ftm, i, n, d, fe, ns, charset, (FixComponentRef) me, m, v);
  else if ( me instanceof FixRepeatingGroup )
    // This is the uncommon case
    // typically a repeatingGroup within a message
    {
    throw new FixConvException("Unified FIX Repository content problem - repeatingGroup must be within a component");
/*
    log("@@@ What is the abbrName value to use?");
    i = tagToXmlRepeatingGroup(ftm, i, n, d, fe, "???", -1, ns, charset, (FixRepeatingGroup) me, m, v);
*/
    }
  return i;
  }
//...e
//...stagToXml:2:
public Document tagToXml(
  FixTagMessage ftm,
  String extMaj, // eg: "5", or null
  String extMin, // eg: "0", or null
  String extSP,  // eg: "SP2", or null
  String extCv   // eg: "mycustomversion", or null
  )
  throws FixConvException
  {
  DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
  dbf.setNamespaceAware(true);
  dbf.setCoalescing(true);
  dbf.setExpandEntityReferences(true);
  dbf.setIgnoringComments(false);
  try
    {
    push("tagToXml");

    // Basic structural checks
    int n = ftm.tags.size();
    if ( n < 4 )
      // Every FIX message has 8=beginString 9=bodyLength, 35=msgType, stuff, 10=checkSum
      // stuff is probably 49=senderCompId 56=targetCompId, more stuff
      // if FIX.5.0 or later, more stuff may start with 1128=appVerId, yet more stuff
      throw new FixConvException("message needs at least 4 tags, has only "+n);
    int id;
    id = ftm.tags.elementAt(0).id;
    if ( id != FixTag.ID_BeginString )
      throw new FixConvException("first tag must be BeginString, not "+id);
    id = ftm.tags.elementAt(1).id;
    if ( id != FixTag.ID_BodyLength )
      throw new FixConvException("second tag must be BodyLength, not "+id);
    id = ftm.tags.elementAt(2).id;
    if ( id != FixTag.ID_MsgType )
      throw new FixConvException("third tag must be MsgType, not "+id);
    String msgType = ftm.tags.elementAt(2).getValue();

    // Work out the version (and encoding)
    String version = ftm.tags.elementAt(0).getValue(); // eg: "FIX.4.4" or "FIXT.1.1"
    String applExtID = null;
    String customVersion = "";
    String charset = null;
    for ( int i = 3; i < n; i++ )
      {
      FixTag t = ftm.tags.elementAt(i);
      if ( t.id == FixTag.ID_ApplVerID )
        {
        String applVerIDEnum = t.getValue();
        version = repo.mapApplVerIDEnumToVersion(applVerIDEnum);
        if ( version == null )
          throw new FixConvException("don't know how to map ApplVerID tag of "+applVerIDEnum+" to a FIX version");
        }
      else if ( t.id == FixTag.ID_ApplExtID )
        applExtID = t.getValue();
      else if ( t.id == FixTag.ID_CstmApplVerID )
        customVersion = t.getValue();
      else if ( t.id == FixTag.ID_MessageEncoding )
        {
        String messageEncoding = t.getValue();
        charset = messageEncodingToCharset.get(messageEncoding);
        if ( charset == null )
          throw new FixConvException("don't know about message encoding "+messageEncoding);
        break; // Don't need to look further than this
        }
      }

    if ( version.startsWith("FIXT.") )
      {
      if ( extMaj == null || extMin == null )
        throw new FixConvException("no ApplVerID in message and no external configuration");
      version = "FIX."+extMaj+"."+extMin;
      if ( extSP != null )
        version += extSP;
      }

    if ( customVersion.equals("") )
      if ( extCv != null )
        customVersion = extCv;

    FixVersion v = repo.getVersion(version+"/"+customVersion);
    if ( v == null )
      throw new FixConvException("message is FIX version "+version+"/"+customVersion+" but this isn't in the repository");

    // Namespace
    // 5.0SP3 onwards only has major version in namspace
    String ns = NS_STARTS_WITH+version.substring(4,5);
    if ( version.startsWith("FIX.4.") ||
         version.equals("FIX.5.0"   ) ||
         version.equals("FIX.5.0SP1") ||
         version.equals("FIX.5.0SP2") )
      {
      // others also include minor version
      ns += ( "-"+version.substring(6,7) );
      if ( version.length() > 7 )
        // and unfortunately, even the service pack too
        ns += ( "-"+version.substring(7) );
      }
    String vStr = version.substring(4,7);

    DocumentBuilder db = dbf.newDocumentBuilder();
    Document d = db.newDocument();
    Element eRoot = d.createElementNS(ns, "FIXML");
    eRoot.setAttribute("v", vStr);
    if ( applExtID != null )
      eRoot.setAttribute("xv", applExtID);
    if ( ! customVersion.equals("") )
      eRoot.setAttribute("cv", customVersion);
    d.appendChild(eRoot);

    FixMessage m = v.messagesByMsgType.get(msgType);
    if ( m == null )
      throw new FixConvException("message with MsgType="+msgType+" not in FIX version "+version+"/"+customVersion);

    // Message element
    Element eMsg = d.createElementNS(ns, m.abbrName);

    FixElements fe = new FixElements();

    int i = 3;
    while ( i < n )
      {
      FixTag t = ftm.tags.elementAt(i);
      FixMessageEntity me = m.tagIndex.get(t.id);
      if ( me == null )
        throw new FixConvException("unexpected tag="+t.id+" at field position "+i);
      i = tagToXmlMessageEntity(ftm, i, n, d, eMsg, fe, ns, charset, me, m, v);
      }

    fe.toParent(eMsg);
    eRoot.appendChild(eMsg);

    pop();
    return d;
    }
  catch ( ParserConfigurationException e )
    {
    // Won't happen
    return null; // keep compiler happy
    }
  }
//...e
  }
