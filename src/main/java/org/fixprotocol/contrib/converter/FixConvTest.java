//
// FixConvTest.java - FIX Converter Test
//
// AK, 04 Apr 2011, initial version
//

package org.fixprotocol.contrib.converter;

//...simports:0:
import java.util.List;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Serializable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.XMLConstants;
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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
//...e

public class FixConvTest
  {
//...sreadTextFileUTF8:2:
public static String readTextFileUTF8(String fn)
  throws IOException
  {
  StringBuffer sb = new StringBuffer();
  InputStream is = new FileInputStream(fn);
  InputStreamReader isr = new InputStreamReader(is, "UTF-8");
  BufferedReader br = new BufferedReader(isr);
  String line;
  while ( (line = br.readLine()) != null )
    {
    sb.append(line);
    sb.append("\n");
    }
  br.close();
  return sb.toString();
  }
//...e
//...swriteTextFileUTF8:2:
public static void writeTextFileUTF8(String fn, String text)
  throws IOException
  {
  OutputStream os = new FileOutputStream(fn);
  OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
  osw.write(text);
  osw.close();
  }
//...e
//...sreadBinaryFile:2:
public static byte[] readBinaryFile(String fn)
  throws IOException
  {
  File f = new File(fn);
  long len = f.length();
  byte[] b = new byte[(int) len];
  FileInputStream fis = new FileInputStream(f);
  fis.read(b);
  fis.close();
  return b;
  }
//...e
//...swriteBinaryFile:2:
public static void writeBinaryFile(String fn, byte[] b)
  throws IOException
  {
  OutputStream os = new FileOutputStream(fn);
  os.write(b);
  os.close();
  }
//...e
//...ssantitizeFix:2:
// Sometimes folks edit the FIX binary message files using text editors.
// As a result they can end up with end-of-line characters on the end.
// So truncate these.

public static byte[] sanitizeFix(byte[] b)
  {
  if ( b.length >= 2 && b[b.length-2] == '\r' && b[b.length-1] == '\n' )
    {
    byte[] b2 = new byte[b.length-2];
    System.arraycopy(b, 0, b2, 0, b.length-2);
    return b2;
    }
  else if ( b.length >= 1 && b[b.length-1] == '\n' )
    {
    byte[] b2 = new byte[b.length-1];
    System.arraycopy(b, 0, b2, 0, b.length-1);
    return b2;
    }
  else
    return b;
  }
//...e
//...sstrToDoc:2:
protected static Document strToDoc(String s)
  throws Exception
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
//...e
//...sdocToStr:2:
protected static String docToStr(Document d)
  throws Exception
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
//...e
//...sdisplay:2:
// Display a validation error
protected static void display(String kind, SAXParseException e)
  {
  System.err.println(e.getLineNumber()+":"+e.getColumnNumber()+" "+kind+" "+e.getSystemId()+" "+e.getMessage());
  }
//...e
//...smain:2:
public static void main(String[] args)
  {
  try
    {
    if ( args.length == 0 )
      {
      System.out.println("usage: FixConvTest xmlToTag FixRepository.xml file.fixml [file.tag [extMaj [extMin [extSP [extCv]]]]]");
      System.out.println("   or: FixConvTest tagToXml FixRepository.xml file.tag [file.fixml [extMaj [extMin [extSP [extCv]]]]]");
      System.out.println("   or: FixConvTest repair FixRepository.xml file.tag [file2.tag [extMaj [extMin [extSP [extCv]]]]]");
      System.out.println("   or: FixConvTest pretty FixRepository.xml file.tag");
      System.out.println("   or: FixConvTest validate file.fixml fixml-main-X-X.xsd");
      System.exit(1);
      }
    String cmd = args[0];
    if ( cmd.equals("xmlToTag") )
      {
      String s = readTextFileUTF8(args[1]);
      Document d = strToDoc(s);
      FixConv.loggingEnabled = true;
      FixConv fc = new FixConv(d);
      String s2 = readTextFileUTF8(args[2]);
      String extMaj = ( args.length >= 5 ) ? args[4] : null;
      String extMin = ( args.length >= 6 ) ? args[5] : null;
      String extSP  = ( args.length >= 7 ) ? args[6] : null;
      String extCv  = ( args.length >= 8 ) ? args[7] : null;
      System.out.println("=== Input FIXML message");
      System.out.println(s2);
      Document d2 = strToDoc(s2);
      System.out.println("=== Converting");
      List<FixTagMessage> ftms = fc.xmlToTag(d2, extMaj, extMin, extSP, extCv);
      int i = 1;
      for ( FixTagMessage ftm : ftms )
        {
        System.out.println("=== Output tag=value message "+i);
        System.out.println(ftm.toString());
        if ( args.length >= 4 )
          {
          String fn = args[3];
          if ( ftms.size() > 1 )
            fn += ( "-" + Integer.toString(i) );
          writeBinaryFile(fn, ftm.toBytes());
          }
        ++i;
        }
      }
    else if ( cmd.equals("tagToXml") )
      {
      String s = readTextFileUTF8(args[1]);
      Document d = strToDoc(s);
      FixConv.loggingEnabled = true;
      FixConv fc = new FixConv(d);
      byte[] b = sanitizeFix( readBinaryFile(args[2]) );
      String extMaj = ( args.length >= 5 ) ? args[4] : null;
      String extMin = ( args.length >= 6 ) ? args[5] : null;
      String extSP  = ( args.length >= 7 ) ? args[6] : null;
      String extCv  = ( args.length >= 8 ) ? args[7] : null;
      FixTagMessage ftm = fc.bytesToFixTagMessage(b, extMaj, extMin, extSP, extCv);
      System.out.println("=== Input tag=value message");
      System.out.println(ftm.toString());
      ftm.repair();
      System.out.println("=== Input tag=value message (repaired)");
      System.out.println(ftm.toString());
      System.out.println("=== Converting");
      Document d2 = fc.tagToXml(ftm, extMaj, extMin, extSP, extCv);
      String s2 = docToStr(d2);
      System.out.println("=== Output FIXML message");
      System.out.println(s2);
      if ( args.length >= 4 )
        writeTextFileUTF8(args[3], s2);
      }
    else if ( cmd.equals("repair") )
      {
      String s = readTextFileUTF8(args[1]);
      Document d = strToDoc(s);
      FixConv.loggingEnabled = true;
      FixConv fc = new FixConv(d);
      byte[] b = sanitizeFix( readBinaryFile(args[2]) );
      String extMaj = ( args.length >= 5 ) ? args[3] : null;
      String extMin = ( args.length >= 6 ) ? args[4] : null;
      String extSP  = ( args.length >= 7 ) ? args[5] : null;
      String extCv  = ( args.length >= 8 ) ? args[6] : null;
      FixTagMessage ftm = fc.bytesToFixTagMessage(b, extMaj, extMin, extSP, extCv);
      System.out.println("=== Input tag=value message");
      System.out.println(ftm.toString());
      ftm.repair();
      System.out.println("=== Output tag=value message (repaired)");
      System.out.println(ftm.toString());
      if ( args.length >= 4 )
        writeBinaryFile(args[3], ftm.toBytes());
      }
    else if ( cmd.equals("pretty") )
      {
      String s = readTextFileUTF8(args[1]);
      Document d = strToDoc(s);
      FixConv.loggingEnabled = true;
      FixConv fc = new FixConv(d);
      byte[] b = sanitizeFix( readBinaryFile(args[2]) );
      String extMaj = ( args.length >= 5 ) ? args[3] : null;
      String extMin = ( args.length >= 6 ) ? args[4] : null;
      String extSP  = ( args.length >= 7 ) ? args[5] : null;
      String extCv  = ( args.length >= 8 ) ? args[6] : null;
      FixTagMessage ftm = fc.bytesToFixTagMessage(b, extMaj, extMin, extSP, extCv);
      System.out.println("=== Input tag=value message");
      System.out.println(ftm.toString());
      String pretty = fc.fixTagMessageToPretty(ftm, extMaj, extMin, extSP, extCv);
      System.out.println("=== Output prettified message");
      System.out.println(pretty);
      }
    else if ( cmd.equals("validate") )
      {
      String s = readTextFileUTF8(args[1]);
      Document d = strToDoc(s);
      try
        {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        sf.setErrorHandler(
          new ErrorHandler()
            {
            public void warning(SAXParseException e)
              throws SAXException
              {
              FixConvTest.display("Warning", e);
              }
            public void error(SAXParseException e)
              throws SAXException
              {
              FixConvTest.display("Error", e);
              }
            public void fatalError(SAXParseException e)
              throws SAXException
              {
              FixConvTest.display("Fatal Error", e);
              }
            }
          );
        Schema sch = sf.newSchema(new File(args[2]));
        Validator val = sch.newValidator();
        val.validate(new DOMSource(d));
        }
      catch ( SAXParseException e )
        {
        display("Exception", e);
        }
      }
    }
  catch ( Exception e )
    {
    e.printStackTrace(System.out);
    System.exit(1);
    }
  }
//...e
  }
