//
// FixVersion.java - definition of a version of FIX
//
// This is basically a structure with no behaviour.
//
// AK, 04 Apr 2011, initial version
// AK, 24 Dec 2015, enhance extension pack handling
//

package org.fixprotocol.contrib.converter;

//...simports:0:
import java.util.Map;
import java.util.HashMap;
//...e

public class FixVersion
  {
  public Map<Integer,FixField> fieldsById = new HashMap<Integer,FixField>(200);
  public Map<Integer,FixComponent> componentsById = new HashMap<Integer,FixComponent>();
  public Map<String,FixComponent> componentsByAbbrName = new HashMap<String,FixComponent>();
  public Map<String,FixMessage> messagesByMsgType = new HashMap<String,FixMessage>();
  public Map<String,FixMessage> messagesByAbbrName = new HashMap<String,FixMessage>();
  public String version; // eg: "FIX5.0SP2"
  public String extPack; // eg: "_EP196" or ""
  public String customVersion;
  public String beginString;
  public String applVerIDEnum; // can be null for FIXT.x.x entries in the repository
  public FixVersion(String version, String extPack, String customVersion, String beginString, String applVerIDEnum)
    {
    this.version       = version;
    this.extPack       = extPack;
    this.customVersion = customVersion;
    this.beginString   = beginString;
    this.applVerIDEnum = applVerIDEnum;
    }
  }
