//
// FixMessage.java - a list of FixMessageEntity
//
// This is basically a structure with no behaviour.
//
// AK, 04 Apr 2011, initial version
//

package org.fixprotocol.contrib.converter;

//...simports:0:
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
//...e

public class FixMessage
  {
  public int id;
  public String name;
  public String msgType;
  public String abbrName;
  public String category;
  public boolean notReqXML;
  public List<FixMessageEntity> entities = new ArrayList<FixMessageEntity>();
  public Map<Integer,FixMessageEntity> tagIndex = new HashMap<Integer,FixMessageEntity>();
  public FixMessage(int id, String name, String msgType, String abbrName, String category, boolean notReqXML)
    {
    this.id        = id;
    this.name      = name;
    this.msgType   = msgType;
    this.abbrName  = abbrName;
    this.category  = category;
    this.notReqXML = notReqXML;
    }
  }
