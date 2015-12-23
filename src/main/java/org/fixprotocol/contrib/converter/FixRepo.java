//
// FixRepo.java - Representation of FIX Repository
//
// AK, 13 Jun 2011, initial version
//

package org.fixprotocol.contrib.converter;

//...simports:0:
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
//...e

public class FixRepo
  {
  // The definition of field ApplVerID (1128) does give the enum values,
  // but unfortunately not the FIX versions in the form "FIX.4.4" etc..
  // The closest you get is @symbolicName="FIX44", so even if we didn't have
  // these maps, we'd still need a mapping between @symbolicName and version.
  public Map<String,String> versionToApplVerIDEnum = new HashMap<String,String>();
  public Map<String,String> applVerIDEnumToVersion = new HashMap<String,String>();
//...saddVersionEnum:2:
protected void addVersionEnum(String version, String applVerID)
  {
  versionToApplVerIDEnum.put(version, applVerID);
  applVerIDEnumToVersion.put(applVerID, version);
  }
//...e
//...smapVersionToApplVerIDEnum:2:
public String mapVersionToApplVerIDEnum(String version)
  {
  return versionToApplVerIDEnum.get(version);
  }
//...e
//...smapApplVerIDEnumToVersion:2:
public String mapApplVerIDEnumToVersion(String applVerIDEnum)
  {
  return applVerIDEnumToVersion.get(applVerIDEnum);
  }
//...e

  protected int edition;

  protected Map<String,FixVersion> versions = new HashMap<String,FixVersion>();
    // String key is version/customVersion

  // Parsing the FIX Repository
  // General rules
  //   work from a DOM
  //   maintain a context (like a XPath expression) for use in exception text
  //   silently ignore elements we know don't contain anything useful to us
  //   emit a warning for anything else we find but don't process

//...sparseError:2:
protected void parseError(String context, String error)
  throws FixConvException
  {
  throw new FixConvException(context+": "+error);
  }
//...e
//...sparseWarning:2:
public static boolean parseWarningsEnabled = false;

protected void parseWarning(String context, String warning)
  {
  if ( parseWarningsEnabled )
    System.out.println("parseWarning: "+context+": "+warning);
  }
//...e
//...sparseAttribute:2:
protected String parseAttribute(String context, Element e, String attr)
  throws FixConvException
  {
  if ( ! e.hasAttribute(attr) )
    parseError(context, "@"+attr+" missing");
  return e.getAttribute(attr);
  }

protected String parseAttribute(String context, Element e, String attr, String def)
  {
  return e.hasAttribute(attr) ? e.getAttribute(attr) : def;
  }
//...e
//...sparseField:2:
protected FixField parseField(String context, Element e)
  throws FixConvException
  {
  String idStr = parseAttribute(context, e, "id");
  int id = Integer.parseInt(idStr);
  context += "[@id='"+idStr+"']";
  String name = parseAttribute(context, e, "name");
  String type = parseAttribute(context, e, "type");
  String assocDataTagStr = parseAttribute(context, e, "associatedDataTag", "-1");
  int assocDataTag = Integer.parseInt(assocDataTagStr);
  if ( assocDataTag == 0 )
    // Sometimes the value 0 is used in the file instead of missing out the attribute
    assocDataTag = -1; // internally, we use -1 to mean there isn't one
  if ( ! type.equals("Length") )
    assocDataTag = -1; // only interested in lengths 
  String abbrName = parseAttribute(context, e, "abbrName", null);
  String baseCategory = parseAttribute(context, e, "baseCategory", null);
  String baseCategoryAbbrName = null;
  if ( baseCategory != null )
    // This field has a shorter abbrName that applies for messages of
    // a given category
    baseCategoryAbbrName = parseAttribute(context, e, "baseCategoryAbbrName");
  String notReqXMLStr = parseAttribute(context, e, "notReqXML", "0");
    // Jim Northey advises default value for notReqXML is 0, ie: needed in FIXML
  boolean notReqXML = ( Integer.parseInt(notReqXMLStr) != 0 );
  boolean encoded;
  if ( edition >= 2011 )
    // Hopefully this will annotate encoded fields with encoded="1"
    // (Email sent to Jim Northey about this)
    {
    String encodedStr = parseAttribute(context, e, "encoded", "0");
    encoded = ( Integer.parseInt(encodedStr) != 0 );
    }
  else
    // Use a somewhat dodgy algorithm to guess
    encoded = ( name.startsWith("Encoded") && ! name.endsWith("Len") );
  return new FixField(id, name, type, assocDataTag, abbrName, baseCategory, baseCategoryAbbrName, notReqXML, encoded);
  }
//...e
//...sparseFields:2:
protected void parseFields(String context, Element e, FixVersion v)
  throws FixConvException
  {
  NodeList nl = e.getChildNodes();
  for ( int i = 0; i < nl.getLength(); i++ )
    {
    Node n = nl.item(i);
    if ( n.getNodeType() == Node.ELEMENT_NODE )
      {
      Element e2 = (Element) n;
      String e2name = e2.getLocalName();
      if ( e2name.equals("field") )
        {
        FixField f = parseField(context+"/field", e2);
        v.fieldsById.put(new Integer(f.id), f);
        }
      else
        parseWarning(context, "skipping "+e2name);
      }
    }
  }
//...e
//...sparseMessageEntity:2:
protected FixMessageEntity parseMessageEntity(String context, Element e, int position)
  throws FixConvException
  {
  String ename = e.getLocalName();
  String idStr = parseAttribute(context, e, "id");
  int id = Integer.parseInt(idStr);
  String name = parseAttribute(context, e, "name");
  String requiredStr = parseAttribute(context, e, "required", "0");
    // Jim Northey advises the default should be 0, ie: not required
  boolean required = ( Integer.parseInt(requiredStr) != 0 );
  if ( ename.equals("fieldRef") )
    return new FixFieldRef(id, name, required, position);
  else if ( ename.equals("componentRef") )
    {
    String implMinOccursStr = parseAttribute(context, e, "implMinOccurs", "-1"); // @@@ is default 1
    int implMinOccurs = Integer.parseInt(implMinOccursStr);
    String implMaxOccursStr = parseAttribute(context, e, "implMaxOccurs", "-1"); // @@@ is default 1
    int implMaxOccurs = Integer.parseInt(implMaxOccursStr);
    return new FixComponentRef(id, name, required, implMinOccurs, implMaxOccurs, position);
    }
  else if ( ename.equals("repeatingGroup") )
    {
    String implMinOccursStr = parseAttribute(context, e, "implMinOccurs", "-1"); // @@@ is default 1
    int implMinOccurs = Integer.parseInt(implMinOccursStr);
    String implMaxOccursStr = parseAttribute(context, e, "implMaxOccurs", "-1"); // @@@ is default 1
    int implMaxOccurs = Integer.parseInt(implMaxOccursStr);
    FixRepeatingGroup rg = new FixRepeatingGroup(id, name, required, implMinOccurs, implMaxOccurs, position);
    parseMessageEntities(context, e, rg.entities);
    return rg;
    }
  else
    return null; // will not get here
  }
//...e
//...sparseMessageEntities:2:
// Parse sub elements into a List<FixMessageEntity>.
// Later, during linking, we'll build HashMaps to find things quicker.

//...sisMessageEntity:2:
protected boolean isMessageEntity(String s)
  {
  return s.equals("fieldRef") || s.equals("componentRef") || s.equals("repeatingGroup");
  }
//...e

protected void parseMessageEntities(String context, Element e, List<FixMessageEntity> entities)
  throws FixConvException
  {
  int position = 0;
  NodeList nl = e.getChildNodes();
  for ( int i = 0; i < nl.getLength(); i++ )
    {
    Node n = nl.item(i);
    if ( n.getNodeType() == Node.ELEMENT_NODE )
      {
      Element e2 = (Element) n;
      String e2name = e2.getLocalName();
      if ( isMessageEntity(e2name) )
        {
        FixMessageEntity me = parseMessageEntity(context+"/"+e2name, e2, position++);
        entities.add(me);
        }
      else
        parseWarning(context, "skipping "+e2name);
      }
    }
  }
//...e
//...sparseComponent:2:
protected FixComponent parseComponent(String context, Element e)
  throws FixConvException
  {
  String idStr = parseAttribute(context, e, "id");
  int id = Integer.parseInt(idStr);
  context += "[@id='"+idStr+"']";
  String name = parseAttribute(context, e, "name");
  String typeStr = parseAttribute(context, e, "type");
  FixComponent.ComponentType type;
  if ( typeStr.equals("Block") )
    type = FixComponent.ComponentType.Block;
  else if ( typeStr.equals("BlockRepeating") )
    type = FixComponent.ComponentType.BlockRepeating;
  else if ( typeStr.equals("ImplicitBlock") )
    type = FixComponent.ComponentType.ImplicitBlock;
  else if ( typeStr.equals("ImplicitBlockRepeating") )
    type = FixComponent.ComponentType.ImplicitBlockRepeating;
  else if ( typeStr.equals("OptimisedImplicitBlockRepeating") )
    type = FixComponent.ComponentType.OptimisedImplicitBlockRepeating;
  else if ( typeStr.equals("XMLDataBlock") )
    type = FixComponent.ComponentType.XMLDataBlock;
  else
    {
    parseError(context, "component type of "+typeStr+" not understood");
    type = FixComponent.ComponentType.Block; // won't get here, but keep compiler happy
    }
  String repeatingStr = parseAttribute(context, e, "repeating", "0");
  boolean repeating = ( Integer.parseInt(repeatingStr) != 0 );
  String abbrName = parseAttribute(context, e, "abbrName", null);
  if ( abbrName == null )
    abbrName = name; // the XML element has got to have a name
  String notReqXMLStr = parseAttribute(context, e, "notReqXML", "0");
    // Assume the default aligns with the notReqXML="0" default used elsewhere
  boolean notReqXML = ( Integer.parseInt(notReqXMLStr) != 0 );
  FixComponent c = new FixComponent(id, name, type, repeating, abbrName, notReqXML);
  parseMessageEntities(context, e, c.entities);
  return c;
  }
//...e
//...sparseComponents:2:
protected void parseComponents(String context, Element e, FixVersion v)
  throws FixConvException
  {
  NodeList nl = e.getChildNodes();
  for ( int i = 0; i < nl.getLength(); i++ )
    {
    Node n = nl.item(i);
    if ( n.getNodeType() == Node.ELEMENT_NODE )
      {
      Element e2 = (Element) n;
      String e2name = e2.getLocalName();
      if ( e2name.equals("component") )
        {
        FixComponent c = parseComponent(context+"/component", e2);
        v.componentsById.put(new Integer(c.id), c);
        if ( ! c.notReqXML )
          v.componentsByAbbrName.put(c.abbrName, c);
        }
      else
        parseWarning(context, "skipping "+e2name);
      }
    }
  }
//...e
//...sparseMessage:2:
protected FixMessage parseMessage(String context, Element e)
  throws FixConvException
  {
  String idStr = parseAttribute(context, e, "id");
  int id = Integer.parseInt(idStr);
  context += "[@id='"+idStr+"']";
  String name = parseAttribute(context, e, "name");
  String msgType = parseAttribute(context, e, "msgType");
  String abbrName = parseAttribute(context, e, "abbrName", null);
  if ( abbrName == null )
    abbrName = name; // the XML element has got to have a name
  String category = parseAttribute(context, e, "category");
  String notReqXMLStr = parseAttribute(context, e, "notReqXML");
  boolean notReqXML = ( Integer.parseInt(notReqXMLStr) != 0 );
  FixMessage m = new FixMessage(id, name, msgType, abbrName, category, notReqXML);
  parseMessageEntities(context, e, m.entities);
  return m;
  }
//...e
//...sparseMessages:2:
protected void parseMessages(String context, Element e, FixVersion v)
  throws FixConvException
  {
  NodeList nl = e.getChildNodes();
  for ( int i = 0; i < nl.getLength(); i++ )
    {
    Node n = nl.item(i);
    if ( n.getNodeType() == Node.ELEMENT_NODE )
      {
      Element e2 = (Element) n;
      String e2name = e2.getLocalName();
      if ( e2name.equals("message") )
        {
        FixMessage m = parseMessage(context+"/message", e2);
        v.messagesByMsgType.put(m.msgType, m);
// now we'll also map session messages in XML form to tag=value
//      if ( ! m.notReqXML )
        if ( ! m.abbrName.equals("") )
          v.messagesByAbbrName.put(m.abbrName, m);
        }
      else
        parseWarning(context, "skipping "+e2name);
      }
    }
  }
//...e
//...sparseVersion:2:
protected FixVersion parseVersion(
  String context,
  Element e,
  String version,      // eg: "FIX.4.4", "FIX.5.0SP1", etc..
  String customVersion // eg: "", "CME"
  )
  throws FixConvException
  {
  String beginString = version.startsWith("FIX.4.")
    ? version.substring(0,7) // eg: "FIX.4.4"
    : "FIXT.1.1";
  String applVerIDEnum = versionToApplVerIDEnum.get(version);
  FixVersion v = new FixVersion(version, customVersion, beginString, applVerIDEnum);
  NodeList nl = e.getChildNodes();
  for ( int i = 0; i < nl.getLength(); i++ )
    {
    Node n = nl.item(i);
    if ( n.getNodeType() == Node.ELEMENT_NODE )
      {
      Element e2 = (Element) n;
      String e2name = e2.getLocalName();
      if ( e2name.equals("abbreviations") )
        ; // we're not interested in these
      else if ( e2name.equals("datatypes") )
        ; // we're not interested in these
      else if ( e2name.equals("categories") )
        ; // we're not interested in these
      else if ( e2name.equals("sections") )
        ; // we're not interested in these
      else if ( e2name.equals("fields") )
        parseFields(context+"/fields", e2, v);
      else if ( e2name.equals("components") )
        parseComponents(context+"/components", e2, v);
      else if ( e2name.equals("messages") )
        parseMessages(context+"/messages", e2, v);
      else
        parseWarning(context, "skipping element "+e2name);
      }
    }
  return v;
  }
//...e
//...sparseRepo:2:
protected void parseRepo(String context, Element e)
  throws FixConvException
  {
  NodeList nl = e.getChildNodes();
  for ( int i = 0; i < nl.getLength(); i++ )
    {
    Node n = nl.item(i);
    if ( n.getNodeType() == Node.ELEMENT_NODE )
      {
      Element e2 = (Element) n;
      String e2name = e2.getLocalName();
      if ( e2name.equals("fix") )
        {
        String context2 = context+"/fix";
        String version = parseAttribute(context2, e2, "version"); // eg: "FIX.4.4", "FIX.5.0SP1"
        String customVersion = parseAttribute(context2, e2, "customVersion", "");
        context2 += "[@version='"+version+"' and @customVersion='"+customVersion+"']";
        String fixmlStr = parseAttribute(context2, e2, "fixml");
        if ( Integer.parseInt(fixmlStr) == 1 )
          {
          FixVersion v = parseVersion(context2, e2, version, customVersion);
          versions.put(version+"/"+customVersion, v);
          }
        }
      else
        parseWarning(context, "skipping element "+e2name);
      }
    }
  }
//...e

  // Linking process ensures that references refer to things that exist

//...slinkError:2:
protected void linkError(String error)
  throws FixConvException
  {
  throw new FixConvException(error);
  }
//...e
//...slinkField:2:
protected void linkField(FixVersion v, FixField f)
  throws FixConvException
  {
  if ( f.assocDataTag != -1 )
    {
    FixField f2;
    if ( (f2 = v.fieldsById.get(new Integer(f.assocDataTag))) == null )
      linkError("field "+f.id+" has associatedDataTag of "+f.assocDataTag+" which is not defined");
    f.assocDataTagField = f2;
    f2.assocDataLenTagField = f;
    }
  }
//...e
//...slinkFieldRef:2:
protected void linkFieldRef(FixVersion v, FixFieldRef fr)
  throws FixConvException
  {
  if ( (fr.field = v.fieldsById.get(new Integer(fr.id))) == null )
    linkError("fieldRef "+fr.name+" refers to field "+fr.id+" which is not defined");
  fr.tags.add(fr.field.id);
  }
//...e
//...slinkComponentRef:2:
protected void linkComponentRef(FixVersion v, FixComponentRef cr)
  throws FixConvException
  {
  if ( (cr.component = v.componentsById.get(new Integer(cr.id))) == null )
    linkError("componentRef "+cr.name+" refers to component "+cr.id+" which is not defined");
  linkComponent(v, cr.component);
  for ( FixMessageEntity me : cr.component.entities )
    cr.tags.addAll(me.tags);
  }
//...e
//...slinkRepeatingGroup:2:
protected void linkRepeatingGroup(FixVersion v, FixRepeatingGroup rg)
  throws FixConvException
  {
  if ( (rg.field = v.fieldsById.get(new Integer(rg.id))) == null )
    linkError("repeatingGroup "+rg.name+" refers to field "+rg.id+" which is not defined");
  rg.tags.add(rg.field.id);
  for ( FixMessageEntity me : rg.entities )
    {
    linkMessageEntity(v, me);
    rg.tags.addAll(me.tags);
    for ( Integer id : me.tags )
      rg.tagIndex.put(id, me);
    }
  }
//...e
//...slinkMessageEntity:2:
protected void linkMessageEntity(FixVersion v, FixMessageEntity me)
  throws FixConvException
  {
  if ( me instanceof FixFieldRef )
    linkFieldRef(v, (FixFieldRef) me);
  else if ( me instanceof FixComponentRef )
    linkComponentRef(v, (FixComponentRef) me);
  else if ( me instanceof FixRepeatingGroup )
    linkRepeatingGroup(v, (FixRepeatingGroup) me);
  }
//...e
//...slinkComponent:2:
protected void linkComponent(FixVersion v, FixComponent c)
  throws FixConvException
  {
  for ( FixMessageEntity me : c.entities )
    {
    linkMessageEntity(v, me);
    for ( Integer id : me.tags )
      c.tagIndex.put(id, me);
    }
  }
//...e
//...slinkMessage:2:
protected void linkMessage(FixVersion v, FixMessage m)
  throws FixConvException
  {
  for ( FixMessageEntity me : m.entities )
    {
    linkMessageEntity(v, me);
    for ( Integer id : me.tags )
      m.tagIndex.put(id, me);
    }
  }
//...e
//...slinkVersion:2:
protected void linkVersion(FixVersion v)
  throws FixConvException
  {
  for ( Integer id : v.fieldsById.keySet() )
    {
    FixField f = v.fieldsById.get(id);
    linkField(v, f);
    }
  for ( Integer id : v.componentsById.keySet() )
    {
    FixComponent c = v.componentsById.get(id);
    linkComponent(v, c);
    }
  for ( String msgType : v.messagesByMsgType.keySet() )
    {
    FixMessage m = v.messagesByMsgType.get(msgType);
    linkMessage(v, m);
    }
  }
//...e
//...slinkRepo:2:
protected void linkRepo()
  throws FixConvException
  {
  for ( String id : versions.keySet() )
    {
    FixVersion v = versions.get(id);
    linkVersion(v);
    }
  }
//...e

//...sgetEdition:2:
public int getEdition()
  {
  return edition;
  }
//...e
//...sgetVersion:2:
// Here, version is of the form "version/customVersion"

public FixVersion getVersion(String version)
  {
  return versions.get(version);
  }
//...e

//...sFixRepo:2:
public FixRepo(Document d)
  throws FixConvException
  {
  // We need to be able to determine the ApplVerID tag
  addVersionEnum("FIX.2.7"   , "0");
  addVersionEnum("FIX.3.0"   , "1");
  addVersionEnum("FIX.4.0"   , "2");
  addVersionEnum("FIX.4.1"   , "3");
  addVersionEnum("FIX.4.2"   , "4");
  addVersionEnum("FIX.4.3"   , "5");
  addVersionEnum("FIX.4.4"   , "6");
  addVersionEnum("FIX.5.0"   , "7");
  addVersionEnum("FIX.5.0SP1", "8");
  addVersionEnum("FIX.5.0SP2", "9");
  addVersionEnum("FIX.5.0SP3", "10"); // guess the future

  Element e = d.getDocumentElement();
  if ( ! e.getLocalName().equals("fixRepository") )
    parseError("/", "root element must be fixRepository");
  String editionStr = parseAttribute("/fixRepository", e, "edition");
  edition = Integer.parseInt(editionStr);
  if ( edition < 2010 )
    parseError("/fixRepository", "@edition must be at least 2010");
  parseRepo("/fixRepository", e);
  linkRepo();
  }
//...e
  }
