//
// FixVersion.java - definition of a version of FIX
//
// This is basically a structure with no behaviour.
//
// AK, 04 Apr 2011, initial version
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
  public String version;
  public String customVersion;
  public String beginString;
  public String applVerIDEnum; // can be null for FIXT.x.x entries in the repository
  public FixVersion(String version, String customVersion, String beginString, String applVerIDEnum)
    {
    this.version       = version;
    this.customVersion = customVersion;
    this.beginString   = beginString;
    this.applVerIDEnum = applVerIDEnum;
    }
  }
