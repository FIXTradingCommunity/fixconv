//
// FixMessageEntity.java - FixFieldRef, FixComponent or FixRepeatingGroup
//
// This is basically a structure with no behaviour.
//
// AK, 04 Apr 2011, initial version
//

package org.fixprotocol.contrib.converter;

//...simports:0:
import java.util.Set;
import java.util.HashSet;
//...e

public class FixMessageEntity
  {
  public int id;
  public String name;
  public boolean required;
  public Set<Integer> tags = new HashSet<Integer>();
  public int position;
  public FixMessageEntity(int id, String name, boolean required, int position)
    {
    this.id       = id;
    this.name     = name;
    this.required = required;
    this.position = position;
    }
  }
