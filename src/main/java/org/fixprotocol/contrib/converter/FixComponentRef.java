//
// FixComponentRef.java - a reference to a FixComponent
//
// This is basically a structure with no behaviour.
//
// AK, 04 Apr 2011, initial version
//

package org.fixprotocol.contrib.converter;

public class FixComponentRef extends FixMessageEntity
  {
  public int implMinOccurs; // or -1
  public int implMaxOccurs; // or -1
  public FixComponent component = null; // filled in by linking
  public FixComponentRef(int id, String name, boolean required, int implMinOccurs, int implMaxOccurs, int position)
    {
    super(id, name, required, position);
    this.implMinOccurs = implMinOccurs;
    this.implMaxOccurs = implMaxOccurs;
    }
  }
