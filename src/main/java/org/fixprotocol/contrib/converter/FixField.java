//
// FixField.java - FIX Field, definition of a tag
//
// This is basically a structure with no behaviour.
//
// AK, 04 Apr 2011, initial version
//

package org.fixprotocol.contrib.converter;

public class FixField
  {
  enum ConvType
    {
    Text,
    UTCTimestamp,
    UTCTimeOnly,
    UTCDateOnly,
    LocalMktDate,
    TZTimeOnly,
    TZTimestamp,
    EncodedText,
    Base64,
    XMLData
    }
  public int id;
  public String name;
  public String type;
  public int assocDataTag; // or -1
  public FixField assocDataTagField    = null;
  public FixField assocDataLenTagField = null;
  public String abbrName; // or null
  public String baseCategory; // or null
  public String baseCategoryAbbrName; // or null
  public boolean notReqXML;
  public boolean encoded;
  public ConvType convType;
  public FixField(int id, String name, String type, int assocDataTag, String abbrName, String baseCategory, String baseCategoryAbbrName, boolean notReqXML, boolean encoded)
    {
    this.id                   = id;
    this.name                 = name;
    this.type                 = type;
    this.assocDataTag         = assocDataTag;
    this.abbrName             = abbrName;
    this.baseCategory         = baseCategory;
    this.baseCategoryAbbrName = baseCategoryAbbrName;
    this.notReqXML            = notReqXML;
    this.encoded              = encoded;
    if ( encoded )
      convType = ConvType.EncodedText;
    else if ( type.equals("UTCTimestamp") )
      convType = ConvType.UTCTimestamp;
    else if ( type.equals("UTCTimeOnly") )
      convType = ConvType.UTCTimeOnly;
    else if ( type.equals("UTCDateOnly") )
      convType = ConvType.UTCDateOnly;
    else if ( type.equals("LocalMktDate") )
      convType = ConvType.LocalMktDate;
    else if ( type.equals("TZTimeOnly") )
      convType = ConvType.TZTimeOnly;
    else if ( type.equals("TZTimestamp") )
      convType = ConvType.TZTimestamp;
    else if ( type.equals("data") )
      convType = ConvType.Base64;
    else if ( type.equals("XMLData") )
      convType = ConvType.XMLData;
    else
      convType = ConvType.Text;
    }
  }
