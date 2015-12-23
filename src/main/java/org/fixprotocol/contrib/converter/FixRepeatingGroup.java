//
// FixRepeatingGroup.java - a NoXxx FixFieldRef and a list of FixMessageEntity
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

public class FixRepeatingGroup extends FixMessageEntity
  {
  public int implMinOccurs; // or -1 (not officially a part of FIX yet)
  public int implMaxOccurs; // or -1 (not officially a part of FIX yet)
  public FixField field = null; // filled in by linking
  public List<FixMessageEntity> entities = new ArrayList<FixMessageEntity>();
  public Map<Integer,FixMessageEntity> tagIndex = new HashMap<Integer,FixMessageEntity>();
  public FixRepeatingGroup(int id, String name, boolean required, int implMinOccurs, int implMaxOccurs, int position)
    {
    super(id, name, required, position);
    this.implMinOccurs = implMinOccurs;
    this.implMaxOccurs = implMaxOccurs;
    }
  }
