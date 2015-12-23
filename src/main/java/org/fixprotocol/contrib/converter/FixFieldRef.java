//
// FixFieldRef.java - a reference to a FixField
//
// This is basically a structure with no behaviour.
//
// AK, 04 Apr 2011, initial version
//

package org.fixprotocol.contrib.converter;

public class FixFieldRef extends FixMessageEntity
  {
  protected FixField field = null; // filled in by linking
  public FixFieldRef(int id, String name, boolean required, int position)
    {
    super(id, name, required, position);
    }
  }
