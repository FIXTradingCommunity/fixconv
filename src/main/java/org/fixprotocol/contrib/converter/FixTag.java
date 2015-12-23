//
// FixTag.java - FIX Tag
//
// AK, 04 Apr 2011, initial version
//

package org.fixprotocol.contrib.converter;

//...simports:0:
import java.io.UnsupportedEncodingException;
//...e

public class FixTag
  {
  // Some tags we have to have specific hard coded knowledge of
  public static final int ID_BeginString     =    8;
  public static final int ID_BodyLength      =    9;
  public static final int ID_CheckSum        =   10;
  public static final int ID_MsgType         =   35;
  public static final int ID_XmlDataLen      =  212;
  public static final int ID_XmlData         =  213;
  public static final int ID_MessageEncoding =  347;
  public static final int ID_ApplVerID       = 1128;
  public static final int ID_CstmApplVerID   = 1129;
  public static final int ID_ApplExtID       = 1156;

  public int id;
  public byte[] value;
//...sFixTag:2:
public FixTag(int id)
  {
  this.id    = id;
  this.value = null;
  }

public FixTag(int id, byte[] value)
  {
  this.id    = id;
  this.value = value;
  }

public FixTag(int id, String value)
  {
  this.id = id;
  try
    {
    this.value = value.getBytes("US-ASCII");
    }
  catch ( UnsupportedEncodingException e )
    {
    // Doesn't happen, US-ASCII sure to be supported
    }
  }

public FixTag(int id, int value)
  {
  this.id = id;
  try
    {
    this.value = Integer.toString(value).getBytes("US-ASCII");
    }
  catch ( UnsupportedEncodingException e )
    {
    // Doesn't happen, US-ASCII sure to be supported
    }
  }
//...e
//...sgetValue:2:
public String getValue()
  {
  try
    {
    return new String(value, "US-ASCII");
    }
  catch ( UnsupportedEncodingException e )
    {
    // Doesn't happen, US-ASCII sure to be supported
    }
  return null; // never get here
  }
//...e
//...sgetValueInt:2:
public int getValueInt()
  {
  String s = getValue();
  return Integer.parseInt(s);
  }
//...e
//...ssetValue:2:
public void setValue(String value)
  throws FixConvException
  {
  try
    {
    this.value = value.getBytes("US-ASCII");
    }
  catch ( UnsupportedEncodingException uee )
    {
    throw new FixConvException("can't encode string value as ASCII", uee);
    }
  }
//...e
//...ssetValueInt:2:
public void setValueInt(int value)
  {
  try
    {
    setValue(Integer.toString(value));
    }
  catch ( FixConvException e )
    {
    // Doesn't happen, as digits are ASCII characters
    }
  }
//...e
//...sgetCheckSum:2:
public int getCheckSum()
  {
  int sum = 0;
  String idStr = Integer.toString(id);
  for ( int i = 0; i < idStr.length(); i++ )
    sum += idStr.charAt(i);
  sum += '=';
  for ( int i = 0; i < value.length; i++ )
    sum += value[i];
  sum += 1; // SOH
  return sum;
  }
//...e
  }
