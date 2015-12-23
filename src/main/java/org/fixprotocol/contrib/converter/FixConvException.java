//
// FixConvException.java - FIX Converter Exception
//
// AK, 04 Apr 2011, initial version
//

package org.fixprotocol.contrib.converter;

//...simports:0:
import java.lang.Throwable;
import java.lang.Exception;
//...e

public class FixConvException extends Exception
  {
  public FixConvException(String message)
    { super(message); }
  public FixConvException(String message, Throwable cause)
    { super(message, cause); }
  }
