/**********************************************************************
 * Jhove - JSTOR/Harvard Object Validation Environment
 * Copyright 2004 by JSTOR and the President and Fellows of Harvard College
 *
 **********************************************************************/

package edu.harvard.hul.ois.jhove.module.html;

import edu.harvard.hul.ois.jhove.*;
import edu.harvard.hul.ois.jhove.module.xml.HtmlMetadata;

import java.util.*;

/** Representation of a parsed HTML open tag, including its attributes.
 * This arguable would better be called an element, but JHElement is
 * the name of the abstract superclass.
 * 
 * @author Gary McGath
 *
 */
public class JHOpenTag extends JHElement {

    /** Element name.
     *  Fields are made public to avoid overcomplicating the .jj file */
    public String _name;
    /** List of element attributes. Each
     *           attributes is an array of two strings, the
     *           name and the value.  If no explicit value
     *           was given, attribute[1] is null.  If the
     *           attribute was in quotes, the quotes are still there.
     */
    public List _attributes;
    /** Description of the abstract element. */
    private HtmlTagDesc _element;
    /** Error message generated by parser, or null. */
    private ErrorMessage _errorMessage;
    
    /* Index into _element's content array to indicate the position
     * currently being matched against. */
    private int _contentIdx;
    
    /* Number of elements matched at the current content index. */
    private int _elementCount;

    /**
     *  Constructor.
     * 
     *  @param   elements     The list of parsed elements, to which
     *                        this gets added.  May be null for a stub
     *                        element not generated by the parser.
     *  @param   name         The name of the tag
     *  @param   attrs        A List of attributes, representing
     *                        the parsed attributes of the tag.  Each
     *                        attributes is an array of two strings, the
     *                        name and the value.  If no explicit value
     *                        was given, attribute[1] is null.  If the
     *                        attribute was in quotes, the quotes are still there.
     *  @param   line         Line number, for information reporting
     *  @param   column       Line number, for information reporting
     */
    public JHOpenTag (List elements, String name, List attrs, int line, int column)
    {
        super (elements);
        _name = name.toLowerCase ();
        _attributes = attrs;
        _line = line;
        _column = column;
        //cleanAttributeQuotes ();
        _contentIdx = 0;
        _elementCount = 0;
    }

    /**
     *  Constructor with error message.
     *  This is used to allow constructs which are erroneous but common --
     *  specifically, the closing of a tag with {@code />}.
     * 
     *  @param   elements     The list of parsed elements, to which
     *                        this gets added.  May be null for a stub
     *                        element not generated by the parser.
     *  @param   name         The name of the tag
     *  @param   attrs        A List of attributes, representing
     *                        the parsed attributes of the tag.  Each
     *                        attributes is an array of two strings, the
     *                        name and the value.  If no explicit value
     *                        was given, attribute[1] is null.  If the
     *                        attribute was in quotes, the quotes are still there.
     *  @param   message      An error message indicating that this element
     *                        isn't well-formed, but we'll take it anyway.
     */
    public JHOpenTag (List elements, String name, List attrs, 
            int line, int column, ErrorMessage message)
    {
        this (elements, name, attrs, line, column);
        _errorMessage = message;
    }
    
    /** Constructor for a stub attribute.  This shouldn't ever be used
     *  by the parser, but only by the module for generating implied
     *  elements. */
    public JHOpenTag (String name)
    {
        super (null);
        _name = name;
        _attributes = new ArrayList (1);
        _contentIdx = 0;
    }
    
    /** Associates an the tag with an element definition.  This is done
     *  by the HTML module, not by the parser. */
    public void setElement (HtmlTagDesc element)
    {
        _element = element;
    }
    
    /** Returns the element definition which has been associated with
     *  this tag. */
    public HtmlTagDesc getElement ()
    {
        return _element;
    }


    /** Returns the tag's name. */
    public String getName ()
    {
        return _name;
    }
    
    /** Returns the tag's attributes.
     * 
     *  @return  The attributes as a List. Each
     *           attributes is an array of two strings, the
     *           name and the value.  If no explicit value
     *           was given, attribute[1] is null.  If the
     *           attribute was in quotes, the quotes are still there.
     */
    public List getAttributes ()
    {
        return _attributes;
    }
    
    /** Process the element to extract any available metadata. */
    protected void processElement (HtmlMetadata mdata)
    {
        if ("html".equals (_name)) {
            processHtml (mdata);
        }
        else if ("meta".equals (_name)) {
            processMeta (mdata);
        }
        else if ("a".equals (_name)) {
            processA (mdata);
        }
        else if ("img".equals (_name)) {
            processImg (mdata);
        }
        else if ("frame".equals (_name)) {
            processFrame (mdata);
        }
        else if ("script".equals (_name)) {
            processScript (mdata);
        }
        
        /* Look for certain attributes in any tag. */
        Iterator iter = _attributes.iterator ();
        while (iter.hasNext ()) {
            JHAttribute attr = (JHAttribute) iter.next ();
            if ("lang".equals (attr.getName ()) && attr.getValue () != null) {
                mdata.addLanguage (attr.getValue ());
            }
        }
    }
    
    
    /** Returns <code>true</code> if the tag given in the parameter is
     *  allowable in our context. */
    protected boolean allowsTag (String tag, HtmlDocDesc doc)
    {
        return _element.allowsTag (tag, _contentIdx, doc);
    }
    
    /** Checks if we can accept another element at the current
     *  content index. */
    protected boolean canGetMore ()
    {
        return _element.canGetMoreAt (_contentIdx, _elementCount);
    }
    
    /** Counts off a component at the current index. */
    protected void countComponent ()
    {
        _elementCount++;
    }
    
    /** Increments the value of _contentIdx */
    protected void advanceIndex ()
    {
        _contentIdx++;
        _elementCount = 0;
    }
    
    
    /** Reports whether it's legal to advance to the next content
     *  index.  The index is assumed to be legal, but the one
     *  to which it's trying to advance may not be. */
    protected boolean canAdvance ()
    {
        return _element.canAdvanceFrom (_contentIdx, _elementCount);
    }

    /** Returns the error message associated with this element.
     *  If it returns a non-null value, the tag is not well-formed,
     *  and the error should be reported. 
     */
    protected ErrorMessage getErrorMessage ()
    {
        return _errorMessage;
    }


    /** Processes metadata from an HTML tag */
    private void processHtml (HtmlMetadata mdata)
    {
        String lang = null;
        Iterator iter = _attributes.iterator ();
        while (iter.hasNext ()) {
            JHAttribute attr = (JHAttribute) iter.next ();
            if ("lang".equals (attr.getName ())) {
                lang = attr.getValue ();
            }
        }
        if (lang != null) {
            mdata.setLanguage(lang);
        }
    }


    /** Processes metadata from a META tag */
    private void processMeta (HtmlMetadata mdata) 
    {
        String name = null;
        String httpeq = null;
        String content = null;
        Iterator iter = _attributes.iterator ();
        while (iter.hasNext ()) {
            JHAttribute attr = (JHAttribute) iter.next ();
            String attname = attr.getName ();
            String attval = attr.getValue ();
            if ("name".equals (attname)) {
                name = attval;
            }
            if ("http-equiv".equals (attname)) {
                httpeq = attval;
            }
            if ("content".equals (attname)) {
                content = attval;
            }
        }
        if (name != null || httpeq != null || content != null) {
            List plist = new ArrayList (3);
            if (name != null) {
                plist.add (new Property ("Name",
                        PropertyType.STRING,
                        name));
            }
            if (httpeq != null) {
                plist.add (new Property ("Httpequiv",
                        PropertyType.STRING,
                        httpeq));
            }
            if (content != null) {
                plist.add (new Property ("Content",
                        PropertyType.STRING,
                        content));
            }
            mdata.addMeta (new Property ("Meta",
                        PropertyType.PROPERTY,
                        PropertyArity.LIST,
                        plist));
        }
    }

    /** Processes metadata from an A element.  Only elements with an
     *  HREF attribute are of interest here.  We ignore links
     *  to anchors. */
    private void processA (HtmlMetadata mdata) 
    {
        Iterator iter = _attributes.iterator ();
        while (iter.hasNext ()) {
            JHAttribute attr = (JHAttribute) iter.next ();
            if ("href".equals (attr.getName ())) {
                String link = attr.getValue ();
                if (link.length() > 0 && link.charAt (0) != '#') {
                    mdata.addLink (link);
                }
                break;
            }
        }
    }
    
    /** Processes metadata from the IMG element. */
    private void processImg (HtmlMetadata mdata)
    {
        String alt = null;
        String longdesc = null;
        String src = null;
        int height = -1;
        int width = -1;
        Iterator iter = _attributes.iterator ();
        while (iter.hasNext ()) {
            JHAttribute attr = (JHAttribute) iter.next ();
            String attname = attr.getName ();
            String attval = attr.getValue ();
            if ("alt".equals (attname)) {
                alt = attval;
            }
            else if ("src".equals (attname)) {
                src = attval;
            }
            else if ("longdesc".equals (attname)) {
                longdesc = attval;
            }
            else if ("height".equals (attname)) {
                try {
                    height = Integer.parseInt(attval);
                }
                catch (Exception e) {}
            }
            else if ("width".equals (attname)) {
                try {
                    width = Integer.parseInt(attval);
                }
                catch (Exception e) {}
            }
        }
        List plist = new ArrayList (5);
        if (alt != null) {
            plist.add (new Property ("Alt",
                    PropertyType.STRING,
                    alt));
        }
        if (longdesc != null) {
            plist.add (new Property ("Longdesc",
                    PropertyType.STRING,
                    longdesc));
        }
        if (src != null) {
            plist.add (new Property ("Src",
                    PropertyType.STRING,
                    src));
        }
        if (height >= 0) {
            plist.add (new Property ("Height",
                    PropertyType.INTEGER,
                    new Integer (height)));
        }
        if (width >= 0) {
            plist.add (new Property ("Width",
                    PropertyType.INTEGER,
                    new Integer (width)));
        }
        if (!plist.isEmpty ()) {
            mdata.addImage(new Property ("Image",
                    PropertyType.PROPERTY,
                    PropertyArity.LIST,
                    plist));
        }
    }
    
    /** Processes metadata from the FRAME element. */
    private void processFrame (HtmlMetadata mdata)
    {
        String name = null;
        String title = null;
        String src = null;
        String longdesc = null;
        Iterator iter = _attributes.iterator ();
        while (iter.hasNext ()) {
            JHAttribute attr = (JHAttribute) iter.next ();
            String attname = attr.getName ();
            String attval = attr.getValue ();
            if ("name".equals (attname)) {
                name = attval;
            }
            else if ("title".equals (attname)) {
                title = attval;
            }
            else if ("src".equals (attname)) {
                src = attval;
            }
            else if ("longdesc".equals (attname)) {
                longdesc = attval;
            }
        }
        List plist = new ArrayList (4);
        if (name != null) {
            plist.add (new Property ("Name",
                    PropertyType.STRING,
                    name));
        }
        if (title != null) {
            plist.add (new Property ("Title",
                    PropertyType.STRING,
                    title));
        }
        if (longdesc != null) {
            plist.add (new Property ("Longdesc",
                    PropertyType.STRING,
                    longdesc));
        }
        if (src != null) {
            plist.add (new Property ("src",
                    PropertyType.STRING,
                    src));
        }
        if (!plist.isEmpty ()) {
            mdata.addFrame(new Property ("Frame",
                    PropertyType.PROPERTY,
                    PropertyArity.LIST,
                    plist));
        }
    }

    /** Processes metadata from the SCRIPT element. */
    private void processScript (HtmlMetadata mdata)
    {
        Iterator iter = _attributes.iterator ();
        while (iter.hasNext ()) {
            JHAttribute attr = (JHAttribute) iter.next ();
            String attname = attr.getName ();
            String attval = attr.getValue ();
            if ("type".equals (attname) && attval.length() > 0 ) {
                mdata.addScript (attval);
            }
        }
    }
}
