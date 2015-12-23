//
// FixTest.java - FIX Converter
//
// AK, 20 Dec 2015, initial version
//

package org.fixprotocol.contrib.converter;

//...simports:0:
import java.util.List;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.w3c.dom.Document;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
//...e

public class FixTest
  {
  // getting at test resources
//...sreadTextResourceUTF8:2:
private static String readTextResourceUTF8(String fn)
  throws IOException
  {
  StringBuffer sb = new StringBuffer();
  InputStream is = FixTest.class.getClassLoader().getResourceAsStream(fn);
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
//...sreadBinaryResource:2:
private static byte[] readBinaryResource(String fn)
  throws IOException
  {
  InputStream is = FixTest.class.getClassLoader().getResourceAsStream(fn);
  ByteArrayOutputStream baos = new ByteArrayOutputStream();
  byte[] b = new byte[0x1000];
  int n;
  while ( (n = is.read(b, 0, 0x1000)) != -1 )
    {
    baos.write(b, 0, n);
    b = new byte[0x1000];
    }
  is.close();
  return baos.toByteArray();
  }
//...e
//...sstrToDoc:2:
// If FixConvTest.strToDoc throws Exception, then it simply bombs.
// Here though, Exception ripples up all the way through the call stack. Yuk.
// In these tests, we know the conversion can't fail so hide the Exception.
// Yes this is nasty, something to fix in the future.
private static Document strToDoc(String s)
  {
  try
    {
    return FixConvTest.strToDoc(s);
    }
  catch ( Exception e )
    {
    return null;
    }
  }
//...e
//...sdocToStr:2:
// Similar comment as above.
private static String docToStr(Document d)
  {
  try
    {
    return FixConvTest.docToStr(d);
    }
  catch ( Exception e )
    {
    return null;
    }
  }
//...e

  public FixConv fc;
//...sinitialize:2:
@Before
public void initialize()
  throws IOException, FixConvException
  {
  String fixRep = readTextResourceUTF8("FixRepository-2010_Subset.xml");
  Document d = strToDoc(fixRep);
  fc = new FixConv(d);
  }
//...e
//...stagToXml:2:
public void tagToXml(String fn)
  throws IOException, FixConvException
  {
  byte[] tag = readBinaryResource("messages/" + fn + ".tag");
  tag = FixConvTest.sanitizeFix( tag );
  FixTagMessage ftm = fc.bytesToFixTagMessage(tag, null, null, null, null);
  ftm.repair();
  Document d = fc.tagToXml(ftm, null, null, null, null);
  String xml = docToStr(d);    
  String xmlExpected = readTextResourceUTF8("messages/" + fn + ".tag.fixml");
  assertEquals("converting " + fn + " from tag=value to XML", xmlExpected, xml);
  }
//...e

  @Test
  public void testTagToXmlSuccess()
    throws IOException, FixConvException
    {
    tagToXml("fix44-ExecRpt");
    tagToXml("fix44-Heartbeat");
    tagToXml("fix44-XMLData");
    tagToXml("fix44-data");
    tagToXml("fix44-encoded");
    tagToXml("fix44-group-fields-out-of-order");
    tagToXml("fix50sp2-Reject");
    tagToXml("fix50sp2-SecurityXML");
    }

  @Test(expected=FixConvException.class)
  public void testTagToXmlFixVersion()
    throws IOException, FixConvException
    { tagToXml("fix41-fix-version-not-in-repository"); }

  @Test(expected=FixConvException.class)
  public void testTagToXmlCountWrong()
    throws IOException, FixConvException
    { tagToXml("fix44-count-wrong"); }

  @Test(expected=FixConvException.class)
  public void testTagToXmlMalformedTag()
    throws IOException, FixConvException
    { tagToXml("fix44-malformed-tag"); }


  @Test(expected=FixConvException.class)
  public void testTagToXmlRepeatZero()
    throws IOException, FixConvException
    { tagToXml("fix44-repeat-zero"); }

  @Test(expected=FixConvException.class)
  public void testTagToXmlTagNotInRep()
    throws IOException, FixConvException
    { tagToXml("fix44-tag-not-in-repository"); }

  @Test(expected=FixConvException.class)
  public void testTagToXmlSecNonWellFormed()
    throws IOException, FixConvException
    { tagToXml("fix50sp2-SecurityXML-non-well-formed"); }

//...sxmlToTag:2:
public void xmlToTag(String fn)
  throws IOException, FixConvException
  {
  String xml = readTextResourceUTF8("messages/" + fn + ".fixml");
  Document d = strToDoc(xml);
  List<FixTagMessage> ftms = fc.xmlToTag(d, null, null, null, null);
  int i = 1;
  for ( FixTagMessage ftm : ftms )
    {
    byte[] tag = ftm.toBytes();
    String fn2 = fn;
    if ( ftms.size() > 1 )
      fn2 += ( "-" + Integer.toString(i) );
    byte[] tagExpected = readBinaryResource("messages/" + fn2 + ".fixml.tag");
    tagExpected = FixConvTest.sanitizeFix( tagExpected );
    assertArrayEquals("converting " + fn + " from FIXML to tag=value (result " + i + ")", tagExpected, tag);
    ++i;
    }
  }
//...e

  @Test
  public void testXmlToTagSuccess()
    throws IOException, FixConvException
    {
    xmlToTag("fix44-AllocationInstructionAck");
    xmlToTag("fix44-AllocationReport");
    xmlToTag("fix44-AllocationReportAck");
    xmlToTag("fix44-NewOrderList");
    xmlToTag("fix44-NewOrderSingle");
    xmlToTag("fix44-NewOrderSingle-batch");
    xmlToTag("fix44-encoded");
    xmlToTag("fix44-v-attr");
    xmlToTag("fix50sp2-Reject");
    xmlToTag("fix50sp2-SecurityXML");
    }

  // FIX.4.2 predates the new FIXML
  @Test(expected=FixConvException.class)
  public void testXmlToTagUnknownVersion()
    throws IOException, FixConvException
    { xmlToTag("fix42-NewOrderSingle"); }

  @Test(expected=FixConvException.class)
  public void testXmlToTagUnexpectedContent()
    throws IOException, FixConvException
    { xmlToTag("fix44-AllocationInstruction"); }

  @Test(expected=FixConvException.class)
  public void testXmlToTagUnexpectedContent2()
    throws IOException, FixConvException
    { xmlToTag("fix44-NewOrderList-broken"); }

  @Test(expected=FixConvException.class)
  public void testXmlToTagUnknownCustomVersion()
    throws IOException, FixConvException
    { xmlToTag("fix44-cv-unknown"); }

  @Test(expected=FixConvException.class)
  public void testXmlToTagVersionMismatch()
    throws IOException, FixConvException
    { xmlToTag("fix44-v-mismatch"); }

  }
