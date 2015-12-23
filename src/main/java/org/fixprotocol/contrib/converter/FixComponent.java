//
// FixComponent.java - a list of FixMessageEntity
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

public class FixComponent
  {
  enum ComponentType
    {
    Block,
    BlockRepeating,
    ImplicitBlock,
    ImplicitBlockRepeating,
    OptimisedImplicitBlockRepeating,
    XMLDataBlock,
    }
  public int id;
  public String name;
  public ComponentType type;
  public boolean repeating;
  public String abbrName;
  public boolean notReqXML;
  public List<FixMessageEntity> entities = new ArrayList<FixMessageEntity>();
  public Map<Integer,FixMessageEntity> tagIndex = new HashMap<Integer,FixMessageEntity>();
  public FixComponent(int id, String name, ComponentType type, boolean repeating, String abbrName, boolean notReqXML)
    {
    this.id        = id;
    this.name      = name;
    this.type      = type;
    this.repeating = repeating;
    this.abbrName  = abbrName;
    this.notReqXML = notReqXML;
    }
  }
