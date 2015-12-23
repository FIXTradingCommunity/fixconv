//
// FixTagMessage.java - FIX Tag=value message
//
// AK, 04 Apr 2011, initial version
//

package org.fixprotocol.contrib.converter;

//...simports:0:
import java.util.Vector;
import java.io.UnsupportedEncodingException;
//...e

public class FixTagMessage
  {
  protected String messageEncoding = null;
  public Vector<FixTag> tags = new Vector<FixTag>();
//...sgetMessageEncoding:2:
public String getMessageEncoding()
  {
  if ( messageEncoding == null )
    for ( int i = 0; i < tags.size(); i++ )
      {
      FixTag t = tags.elementAt(i);
      if ( t.id == FixTag.ID_MessageEncoding )
        try
          {
          messageEncoding = new String(t.value, "US-ASCII");
          }
        catch ( UnsupportedEncodingException e )
          {
          // Doesn't happen, US-ASCII sure to be supported
          }
      }
  return messageEncoding;
  }
//...e
//...sgetTag:2:
public FixTag getTag(int id)
  {
  for ( int i = 0; i < tags.size(); i++ )
    {
    FixTag t = tags.elementAt(i);
    if ( t.id == id )
      return t;
    }
  return null;
  }
//...e
//...sgetTagBackwards:2:
// Sometimes its quicker to search backwards

public FixTag getTagBackwards(int id)
  {
  for ( int i = tags.size()-1; i >= 0; i-- )
    {
    FixTag t = tags.elementAt(i);
    if ( t.id == id )
      return t;
    }
  return null;
  }
//...e
//...saddTag:2:
public void addTag(FixTag t)
  {
  tags.add(t);
  }
//...e
//...sgetCheckSum:2:
public int getCheckSum()
  {
  int sum = 0;
  for ( int i = 0; i < tags.size(); i++ )
    {
    FixTag t = tags.elementAt(i);
    sum += t.getCheckSum();
    }
  return sum;
  }
//...e
//...sgetLength:2:
// Length is either the whole thing, or from tag index 2 onwards

public int getLength(int tagIndex)
  {
  int size = 0;
  for ( int i = tagIndex; i < tags.size(); i++ )
    {
    FixTag t = tags.elementAt(i);
    String tagNumber = Integer.toString(t.id);
    int valueSize = t.value.length;
    size += ( tagNumber.length()+1+valueSize+1 );
    }
  return size;
  }
//...e
//...srepair:2:
// Sometimes we'll get messages from dubious sources
// (eg: test packs) and we'll need to add stuff they omit.
// Ensure there is a BodyLength, with the correct value.
// Ensure there is a CheckSum, with the correct value.

public void repair()
  throws FixConvException
  {
  if ( tags.size() < 1 || tags.elementAt(0).id != FixTag.ID_BeginString )
    throw new FixConvException("missing BeginString");
  if ( tags.size() < 2 || tags.elementAt(1).id != FixTag.ID_BodyLength )
    tags.insertElementAt(new FixTag(FixTag.ID_BodyLength), 1);
  if ( tags.size() < 3 || tags.elementAt(2).id != FixTag.ID_MsgType )
    throw new FixConvException("missing MsgType");
  int last = tags.size()-1;
  if ( tags.elementAt(last).id == FixTag.ID_CheckSum )
    tags.removeElementAt(last); // so that BodyLength is correctly calculated 
  tags.elementAt(1).setValueInt(getLength(2));
  String checkSum = Integer.toString(getCheckSum()&0xff);
  checkSum = ("00"+checkSum).substring(2+checkSum.length()-3);
  tags.add(new FixTag(FixTag.ID_CheckSum, checkSum));
  }
//...e
//...stoBytes:2:
public byte[] toBytes()
  {
  int size = getLength(0);
  byte b[] = new byte[size];
  int p = 0;
  for ( int i = 0; i < tags.size(); i++ )
    {
    FixTag t = tags.elementAt(i);
    String tagNumber = Integer.toString(t.id);
    for ( int j = 0; j < tagNumber.length(); j++ )
      b[p++] = (byte) tagNumber.charAt(j);
    b[p++] = (byte) '=';
    int valueSize = t.value.length;
    System.arraycopy(t.value, 0, b, p, valueSize);
    p += valueSize;
    b[p++] = (byte) 1; // SOH character
    }
  return b;
  }
//...e
//...sbytesToString:2:
// Convert byte[] to String, escaping non-ASCII stuff
// This is purely for debug purposes

public static String bytesToString(byte[] b)
  {
  final char[] hexdigits = { '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f' };
  StringBuilder sb = new StringBuilder();
  for ( int i = 0; i < b.length; i++ )
    if ( (char) b[i] == '\\' )
      sb.append("\\\\");
    else if ( (char) b[i] >= ' ' && (char) b[i] <= '~' )
      // You may think this is ugly, but remember, most of FIX is US-ASCII
      sb.append((char) b[i]);
    else
      {
      sb.append("\\x");
      sb.append(hexdigits[(b[i]>>4)&15]);
      sb.append(hexdigits[ b[i]    &15]);
      }
  return sb.toString();
  }
//...e
//...stoString:2:
// This method is only used for debug purposes

public String toString()
  {
  return bytesToString(toBytes());
  }
//...e
  }
