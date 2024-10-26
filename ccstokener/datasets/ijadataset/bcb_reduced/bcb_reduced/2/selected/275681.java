package com.io_software.utils.web;

import com.abb.util.SimpleTokenizer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.io.StreamTokenizer;
import java.io.IOException;
import java.io.Serializable;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.PrintWriter;

/** Represents a form in an HTML page (<tt>FORM</tt> tag). A form is something
    a user can submit by pressing a buttong in the form. This will then
    submit the contents of the input elements of the submitted form to
    a server. The server's URL is specified in the form's <tt>action</tt>
    attribute. The input elements are declared between the opening and the
    closing form tags.<p>

    A form can choose between two methods for submitting the input
    element values to the server: GET and POST. The GET method
    encodes the element values into the URL that get's sent as a
    request to the server. The POST method transfers the element
    values in the data stream sent to the server together with the
    request.<p>
    
    Note, that this class does currently not recognize input elements
    with type <tt>image</tt> as special. Tags of this type won't be
    converted into submit tags or anything else. So asking the
    form for the submit button text will only return something valid
    if there actually was an input element with type <tt>submit</tt>.
    
    @see HTMLPage
    @author Axel Uhl
    @version $Id: Form.java,v 1.9 2001/04/03 16:30:53 aul Exp $
  */
public class Form implements Serializable {

    /** creates the internal data structures of the new <tt>Form</tt> object-
    
	@param base the URL of the document in which the form was found.
		This is important for resolving the action URL that should
		be found in the <tt>FORM</tt> tag in case it is a relative URL.
      */
    public Form(URL base) {
        this.base = base;
        inputElements = new Vector();
        radios = new Hashtable();
        publicResultsRatio = -1;
    }

    /** parses the form data out of an HTML stream. Expects the
	tokenizer to be positioned after the <tt>FORM</tt> tag's
	closing &gt;. The tokenizer will be positioned after the closing
	<tt>form</tt> tag's &gt;.
	
	@param st the tokenizer to use for parsing the form data
	@param attributes {@link String} keys are the attribute names,
		{@link String} elements are the attribute values.
      */
    public void parse(SimpleTokenizer st, Hashtable attributes) throws IOException {
        String methodSpec = "";
        if (attributes.containsKey("method")) methodSpec = ((String) attributes.get("method")).toLowerCase();
        if (methodSpec.equals("get")) method = GET; else if (methodSpec.equals("post")) method = POST; else if (methodSpec.equals("put")) method = PUT; else method = GET;
        if (attributes.containsKey("name")) name = (String) attributes.get("name");
        String actionAttr = (String) attributes.get("action");
        if (actionAttr != null) try {
            action = new URL(base, actionAttr);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        enctype = (String) attributes.get("enctype");
        parseFormContents(st);
    }

    /** parses the contents between the opening and closing <tt>FORM</tt>
	tag, creating a {@link FormInputElement} object for each found
	<tt>INPUT</tt> tag. When done, the tokenizer will be positioned
	after the closing <tt>form</tt> tag's &gt;.
	
	@param st the tokenizer to use for parsing the form contents
      */
    private void parseFormContents(SimpleTokenizer st) throws IOException {
        int tokenType = st.nextToken();
        boolean endOfFormFound = false;
        while (tokenType != StreamTokenizer.TT_EOF && !endOfFormFound) {
            if (tokenType == '<') {
                Hashtable tagAttributes = new Hashtable();
                String tagName = parseTag(st, tagAttributes);
                if (tagName.equals("/form")) endOfFormFound = true; else {
                    FormInputElement element = FormInputElementFactory.createFormInputElement(this, st, tagName, tagAttributes);
                    if (element != null && !(element instanceof RadioInputElement)) {
                        if (element.getType() != null && element.getType().toLowerCase().equals("submit")) submitButtonText = element.getValue(); else if (element.getType() != null && element.getType().toLowerCase().equals("reset")) resetButtonText = element.getValue(); else inputElements.addElement(element);
                    }
                    tokenType = st.nextToken();
                }
            } else tokenType = st.nextToken();
        }
    }

    /** retrieves the text on the submit button. If none was
	specified in the submit input element or no submit tag was
	found, this defaults to "Submit".
	
	@return the text on the submit button (see above)
      */
    public String getSubmitButtonText() {
        if (submitButtonText != null) return submitButtonText; else return "Submit";
    }

    /** retrieves the text on the reset button. If none was
	specified in the reset input element or no reset tag was
	found, this defaults to "reset".
	
	@return the text on the submit button (see above)
      */
    public String getResetButtonText() {
        if (resetButtonText != null) return resetButtonText; else return "reset";
    }

    /** adds a radio button input element to this form. The trick
	about doing this is that elements with equal names are
	merged together into one instance of class
	{@link RadioInputElement}.
	
	@param r the radio input element to add to this form
	@see #radios
      */
    public void addRadio(RadioInputElement r) {
        if (radios.containsKey(r.getName())) {
            RadioInputElement rOld = (RadioInputElement) radios.get(r.getName());
            try {
                rOld.add(r);
            } catch (DifferentRadioNamesException e) {
                System.err.println("Internal error:");
                e.printStackTrace();
                System.err.println("radio input element " + r + " was not added to form " + this);
            }
        } else {
            inputElements.addElement(r);
            radios.put(r.getName(), r);
        }
    }

    /** determines whether this form's parameter space has finite size.
	This is the case if all input elements have finite option space.
	E.g. a radio button group is finite because the number of
	different parameters that may be submitted for it are determined
	by the number of "radio" input elements declared with the
	same "name" attribute and different "value" attributes.
	A <tt>text</tt> input element type is an example of an
	"infinite" input element since the number of possible inputs is
	potentially unlimited.
      */
    public boolean hasFiniteParameterSpace() {
        for (Enumeration e = inputElements.elements(); e.hasMoreElements(); ) {
            FormInputElement fie = (FormInputElement) e.nextElement();
            if (!fie.hasFiniteParameterSpace()) return false;
        }
        return true;
    }

    /** for each input element of this form that has a finite
	number of options (non-hidden text fields don't apply
	for obvious reasons) counts the number of options and
	multiplies them on the result, starting with 1. The
	resulting number tells the number of combinations for
	those input elements that are finite.<p>
	
	As a special case for forms with no input elements this
	method returns <tt>1</tt> in conformance with the behavior
	of {@link #enumerateParameterSpace}, yielding exactly one
	empty {@link ActualFormParameters} set. So the number of
	elements provided by {@link #enumerateParameterSpace} should
	be identical to the number returned by this method.<p>
	
	Note, that this method also returns a result for forms
	containing input elements with infinite parameter space.
	For those, the returned number only makes limited sense.
	
	@return the product of the number of options for each
		contained input element with finite parameter space
      */
    public double countParameterSpaceElements() {
        double result = 1;
        for (Enumeration e = inputElements.elements(); e.hasMoreElements(); ) {
            FormInputElement fie = (FormInputElement) e.nextElement();
            if (fie.hasFiniteParameterSpace()) {
                int count = 0;
                for (Enumeration p = fie.enumerateParameterSpace(); p.hasMoreElements(); p.nextElement()) count++;
                result *= count;
            }
        }
        return result;
    }

    /** parses an HTML tag into a tag name and a set of attributes.
	The tokenizer is expected to be positioned after the opening
	&lt; and will be positioned after the closing '&gt;'
	when this method returns, if a correctly formatted tag was
	matched. Otherwise, the tokenizer will be positioned after
	the first token not matching the correct tag syntax.<p>
	
	The expected syntax is:<p>
	
	<tt>'&lt;' [ non-word/-string token ] tag-name ([ attribute-name [ '=' attribute-value ]])* '>'</tt>
	<p>

	Note, that attribute names and values enclosed in single
	quotes (') will have those quotes removed.<p>
	
	The <tt>attribute-value</tt> may be a string in quotes
	("). Those will also be removed automatically.
	
	@param st the tokenizer from which to read the tag info
	@param attributes an output parameter. Supposed to reference
		a valid hashtable. This method will add an entry for
		each of the tag's attributes to it, where the key
		is a {@link String} denoting the attribute name, and
		the element is the {@link String} value of the
		attribute, if any, <tt>Form.NULL</tt> otherwise.<p>
		
		The attribute names are converted to all lowercase.
		The attribute value's case is taken over unmodified.
	@return the name of the tag. Note, that an end tag's name starts
		with a '/'. The tagname is converted to lowercase letters. Note
		also, that tags that have more than one leading
		non-word/-string token (e.g. an HTML comment) are returned as
		empty tag names.
      */
    public static String parseTag(SimpleTokenizer st, Hashtable attributes) throws IOException {
        int tokenType = st.nextToken();
        String tagName = "";
        if (tokenType != StreamTokenizer.TT_WORD && tokenType != '\"') {
            tagName = "" + ((char) tokenType);
            tokenType = st.nextToken();
        }
        if (tokenType == StreamTokenizer.TT_WORD) {
            tagName += st.sval;
            tagName = tagName.toLowerCase();
            tokenType = st.nextToken();
            while (tokenType != StreamTokenizer.TT_EOF && tokenType != '>') {
                if (tokenType == '\"' || tokenType == StreamTokenizer.TT_WORD) {
                    String attributeName = trimQuotes(st.sval.toLowerCase());
                    tokenType = st.nextToken();
                    if (tokenType == '=') {
                        tokenType = st.nextToken();
                        if (tokenType == '\"' || tokenType == StreamTokenizer.TT_WORD) {
                            String value = trimQuotes(st.sval);
                            attributes.put(attributeName, value);
                        }
                        tokenType = st.nextToken();
                    } else attributes.put(attributeName, NULL);
                } else tokenType = st.nextToken();
            }
        } else {
            tagName = "";
            while (tokenType != '>' && tokenType != StreamTokenizer.TT_EOF) tokenType = st.nextToken();
        }
        return tagName;
    }

    /** appends the textual representation of a tag to the output buffer

	@param tagname name of the tag to append. May contain the "/"
	indicating a closing tag
	@param attributes keys are expected to be of type {@link String},
	representing the names of the attribute for the tag; the values are
	also of type {@link String}, representing the values of the attributes
	@param buffer the buffer to which to append the string representation
    */
    public static void appendTag(String tagname, Hashtable attributes, StringBuffer buffer) {
        buffer.append("<");
        buffer.append(tagname);
        if (attributes.size() > 0) buffer.append(" ");
        for (Enumeration e = attributes.keys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            String value = (String) attributes.get(key);
            buffer.append(" ");
            buffer.append(key);
            buffer.append("=\"");
            buffer.append(value);
            buffer.append("\"");
        }
        buffer.append(">");
    }

    /** Given a string this method will remove single quotes if the
	string is enclosed in them. This also means that a single
	trailing quote won't be removed. A leading single quote
	won't be removed either.

	@param s the string to trim
	@return the trimmed string with enclosing single quotes
	removed
    */
    private static String trimQuotes(String s) {
        String result = s;
        if (s.length() > 1 && s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'') {
            result = s.substring(1, s.length() - 1);
        }
        return result;
    }

    /** computes the action URL from the base URL and the possibly
	relative action URL into a usable absolute URL.
	
	@return the absolute action URL of this form
      */
    public URL getActionURL() throws MalformedURLException {
        if (action == null) return base; else return new URL(base, action.toString());
    }

    /** allows to set the action URL. This is intended <em>only as a
	workaround</em> for forms where the automatically extracted
	action URL is somehow erroneous. This may e.g. happen in case
	the HTML page in which the form is embedded uses the
	&lt;BASE&gt; tag with a URL that ends on "/" but is not really
	absolute.

	@param url the action URL to set for this form
    */
    public void setActionURL(URL url) {
        action = url;
    }

    /** retrieves the form's name corresponding with the <tt>name</tt>
	attribute in the form tag.
	
	@return the form's name or <tt>null</tt> if no name attribute was
		specified in the form tag.
      */
    public String getName() {
        return name;
    }

    /** retrieves the HTTP method used to transmit the form parameters
	to the server. Parsed from the <tt>FORM</tt> tag's <tt>method</tt>
	attribute.
	
	@return the HTTP method used to transmit this form's parameters
		to the server. Can be one of {@link #GET}, {@link #POST}
		or {@link #PUT}.
      */
    public int getMethod() {
        return method;
    }

    /** sets the ratio of parameter space elements leading to
	outputs findable using a public search engine over the
	total number of evaluated parameter space elements.
	
	@param ratio the ratio to set
	@see #publicResultsRatio
	@see #getPublicResultsRatio
      */
    public void setPublicResultsRatio(double ratio) {
        publicResultsRatio = ratio;
    }

    /** retrieves the ratio of parameter space elements leading to
	outputs findable using a public search engine over the
	total number of evaluated parameter space elements.
	
	@return the ratio described above. Note, that if negative
		this means the value hasn't been set yet.
	@see #publicResultsRatio
	@see #setPublicResultsRatio
      */
    public double getPublicResultsRatio() {
        return publicResultsRatio;
    }

    /** retrieves an enumeration over the set of input elements found in
	this form
    
	@return an enumeration over the input elements found in this form.
		The elements iterated over are of type {@link FormInputElement}.
      */
    public Enumeration getInputElements() {
        return inputElements.elements();
    }

    /** retrieves a single input element by its name. Note, that
	several input elements may reasonably occur having the same
	name (e.g. checkboxes). In this case one of the elements with
	the selected name is picked at random.
    
	@param name the input element name to search for. Case sensitive
		and must not be <tt>null</tt>
	@return an input element with the specified name if found or
		<tt>null</tt> if not found
      */
    public FormInputElement getInputElementByName(String name) {
        for (Enumeration e = inputElements.elements(); e.hasMoreElements(); ) {
            FormInputElement fie = (FormInputElement) e.nextElement();
            if (name.equals(fie.getName())) return fie;
        }
        return null;
    }

    /** Enumerates all parameter combinations for this form that
	can be enumerated (for all finite parameters space input
	elements).<p>
	
	The resulting {@link Enumeration} object iterates over
	instances of class {@link ActualFormParameters}, each of which
	allows generating a URL appendix or a POST data stream of
	it, depending on whether a GET or a POST mehod shall be
	used to file a request for this particular form.
	
	@return an enumeration iterating over {@link ActualFormParameters}
		objects corresponding with all valid parameter
		combinations known ahead (for all input elements for
		which <tt>hasFiniteParameterSpace</tt> returns
		<tt>true</tt>).
	@see #hasFiniteParameterSpace
      */
    public Enumeration enumerateParameterSpace() {
        return new FormParameterSpaceEnumeration();
    }

    /** computes a string representation of this form. This consists
	of the action URL, the method (GET/POST), the encoding type
	and the list of input elements.
	
	@return the string representation of this form's structural
		elemnets (<em>not</em> its HTML source!)
      */
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("Form action: ");
        try {
            result.append(getActionURL().toString());
            result.append(" method=");
            result.append((method == GET) ? "GET" : "POST");
            result.append(", encoding type is ");
            result.append(enctype);
            result.append(". Submit button text: ");
            result.append(getSubmitButtonText());
            result.append(", reset button text: ");
            result.append(getResetButtonText());
            result.append(". Input elements are:\n    ");
            for (Enumeration e = inputElements.elements(); e.hasMoreElements(); ) {
                result.append(e.nextElement().toString());
                result.append("\n    ");
            }
            if (TO_STRING_WITH_URLS) {
                result.append("\nGET URLs:\n");
                result.append(getURLs());
            }
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
        return result.toString();
    }

    /** assembles all HTTP GET URLs for the complete parameter space of
	this form as enumerated by <tt>enumerateParameterSpace</tt> if
	the form has method {@link #GET}. Otherwise, the plain action
	URL (see {@link #getActionURL}) is returned.
	
	@return a string with each line containig a valid GET URL
		with a valid parameter set for the form, one line
		for each element contained in the complete enumeration
		of the form's parameter space in case the form uses
		method {@link #GET}, otherwise the plain action URL.
	@see #enumerateParameterSpace
      */
    private String getURLs() throws MalformedURLException {
        String r;
        if (getMethod() == GET) {
            StringBuffer result = new StringBuffer();
            for (Enumeration e = enumerateParameterSpace(); e.hasMoreElements(); ) {
                ActualFormParameters afps = (ActualFormParameters) e.nextElement();
                try {
                    URL url = new URL(getActionURL().toString() + "?" + afps.toString());
                    result.append("  " + url.toString());
                    if (e.hasMoreElements()) result.append("\n");
                } catch (MalformedURLException mue) {
                    mue.printStackTrace();
                }
            }
            r = result.toString();
        } else r = getActionURL().toString();
        return r;
    }

    /** To a given set of actual form parameters adds default parameters
	for those input elements for which no values were specified in
	the passed parameter set.
	
	@param params the parameter set to be augmented by the
		default values for those input elements for which no
		values were specified
	@see FormInputElement#getDefaultParameters
      */
    public void addDefaultParametersTo(ActualFormParameters params) {
        for (Enumeration e = inputElements.elements(); e.hasMoreElements(); ) {
            FormInputElement fie = (FormInputElement) e.nextElement();
            ActualFormParameters defaults = fie.getDefaultParameters();
            for (Enumeration p = defaults.getParameters(); p.hasMoreElements(); ) {
                ActualFormParameter afp = (ActualFormParameter) p.nextElement();
                if (!params.containsValueForName(afp.getName())) params.add(afp);
            }
        }
    }

    /** constructs a set of HTTP request header properties that
	simulate a Netscape session. It does explicitly <em>not</em> use the
	{@link #getDefaultProperties} method because subclasses may redefine
	that particularly in order to add the Netscape properties with this
	method. This would lead to an endless recursion.<p>

	Thie method may help in some cases where a form tries to identify a
	Java robot and exclude it from the service.

	@return a set of HTTP request header properties that can be used as
	parameter to the {@link
	#submitForm(com.io_software.utils.web.ActualFormParameters,java.util.Hashtable)}
	method
    */
    public Hashtable getNetscapeRequestProperties() {
        Hashtable result = new Hashtable();
        result.put("Connection", "Keep-Alive");
        result.put("User-Agent", "Mozilla/4.73 [en] (WinNT; I)");
        result.put("Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, image/png, */*");
        result.put("Accept-Language", "en,de");
        result.put("Accept-Charset", "iso-8859-1,*,utf-8");
        result.put("Content-type", "application/x-www-form-urlencoded");
        return result;
    }

    /** computes a new hashtable with the referer document as the only
	default property. This table can be extended with no harm
	because a new one is computed with each call to this method.

	@return a table with a default property "Referer" telling the
	base URL of this form
    */
    public Hashtable getDefaultProperties() {
        Hashtable result = new Hashtable();
        result.put("Referer", base.toString());
        return result;
    }

    /** Submits the form without any additional HTTP request properties.
	See {@link #submitForm(ec.metrics.ActualFormParameters)} for
	a detailed description.
	
	@param params the parameters to be sent to the server
	@return the content stream returned from the server as reply
		to the form request. The returned reader will do buffering.
      */
    public Reader submitForm(ActualFormParameters params) throws MalformedURLException, IOException {
        return submitForm(params, getDefaultProperties());
    }

    /** retrieves the result for this form given the set of actual
	form parameters. In case the HTTP method is {@link #GET}, the
	URL is assembled, submitted and retrieved. If the form uses
	method {@link #POST}, the action URL is opened with output mode,
	so that the parameter set can be written to the URL output
	stream.<p>
	
	Note, that the {@link #PUT} method is currently not supported
	by this method.
	
	@param params the parameters to be sent to the server
	@param properties additional HTTP request properties like
		cookies or a different user agent specification.
		Keys and values are treated with {@link Object#toString}
		before setting them as properties. Keys are property
		names, values are property values. Note, that the
		{@link #getDefaultProperties} method is <em>not</em>
		used to get any default request properties in this
		method.
	@return the content stream returned from the server as reply
		to the form request. The returned reader will do buffering.
      */
    public Reader submitForm(ActualFormParameters params, Hashtable properties) throws MalformedURLException, IOException {
        InputStream resultStream;
        URL url;
        if (getMethod() == POST) url = getActionURL(); else url = new URL(getActionURL().toString() + "?" + params.toString());
        URLConnection c = url.openConnection();
        for (Enumeration e = properties.keys(); e.hasMoreElements(); ) {
            Object k = e.nextElement();
            Object v = properties.get(k);
            c.setRequestProperty(k.toString(), v.toString());
        }
        if (getMethod() == POST) {
            c.setDoOutput(true);
            c.setDoInput(true);
            OutputStream os = c.getOutputStream();
            c.connect();
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
            pw.print(params.toString());
            pw.flush();
            pw.close();
            resultStream = c.getInputStream();
        } else resultStream = c.getInputStream();
        return new BufferedReader(new InputStreamReader(resultStream));
    }

    /** Inner class of class {@link Form}, implementing the enumeration
	on the form's parameter space. This is useful since it avoids
	a complete construction of the parameter space before the
	iteration can be started and thus scales even for the wierdest
	forms.<p>
	
	Before providing the details about the standard algorithm,
	a special case shall be explained: If the form does not have
	any input elements (see {@link #inputElements}), the standard
	algorithm would produce an empty enumeration. But a form that
	does not have any input elements (except the buttons which
	don't show up in {@link #inputElements}) can be submitted
	simply without providing any arguments. So for this case
	both {@link #nextElement} and {@link #hasMoreElements} check
	for this special case and handle it properly, producing an
	enumeration that contains exactly one element, namely an
	instance of {@link ActualFormParameters} with no parameters
	inside.<p>
	
	Now the standard algorithm for the enumeration:<p>
	
		A set of nested loops is simulated in a single loop with
		an additional recursive procedure helping to advance the
		iterators. From outside to inside the nesting levels
		correspond with the form input elements in the
		{@link Form#inputElements} vector.<p>

		The {@link #afps} array is set up to hold the current set of
		actual parameters up to the n-th level for the n-th
		array element. Inner nesting levels will fetch the
		next {@link ActualFormParameters} object from their iterator,
		take a copy of the upper level <tt>ActualFormParameters</tt>
		object and add the current level's element to it.
		The resulting element will be stored in <tt>afps</tt> as the
		current level's current <tt>ActualFormParameters</tt>
		object.<p>

		This will recursively descend until the bottom level
		has been reached. Then, a complete and valid set of
		actual form parameters has been accumulated in the
		innermost {@link #afps} element. This will be returned
		by {@link #nextElement}.<p>

		After that, a recursive procedure takes place to advance
		the iterators "by one" (see {@link #advanceIterators}):
		If there are more elements on the
		innermost nesting level then the iterator is advanced and
		the parameter computation repeated. If the iterator
		"bumped", it will be relaunched and the procedure will
		recursively step one level up, try again to advance that
		iterator and, if successful, descend along the already
		described path for computing the {@link ActualFormParameters}
		sets for the new iterator positions.<p>

		The loop ends if the outermost iterator "bumped".
      */
    public class FormParameterSpaceEnumeration implements Enumeration {

        /** Initializes the enumeration. The resulting
	    {@link Enumeration} object iterates over
	    instances of class {@link ActualFormParameters}, each of which
	    allows generating a URL appendix or a POST data stream of
	    it, depending on whether a GET or a POST mehod shall be
	    used to file a request for this particular form.
	  */
        protected FormParameterSpaceEnumeration() {
            levels = inputElements.size();
            afps = new ActualFormParameters[levels];
            iterators = new Enumeration[levels];
            int i = 0;
            for (Enumeration e = inputElements.elements(); e.hasMoreElements(); ) iterators[i++] = ((FormInputElement) e.nextElement()).enumerateParameterSpace();
            backtracklevel = 0;
            if (levels > 0 && iterators[0].hasMoreElements()) {
                backtracklevel = 1;
                afps[0] = (ActualFormParameters) iterators[0].nextElement();
            }
        }

        /** If the enumeration has more elements, returns the next
	    element in the enumeration and advances it by one. Handles
	    the special case of no input elements by returning an
	    empty {@link ActualFormParameters} set and setting the
	    flag {@link #nextElementCalled}.
	    
	    @return the next element in the enumeration, supposed to be
		    of type {@link ActualFormParameters}
	    @exception NoSuchElementException in case the enumeration
		    had <tt>hasMoreElements() == false</tt> when the
		    method was called.
	  */
        public Object nextElement() throws NoSuchElementException {
            if (inputElements.size() == 0) {
                nextElementCalled = true;
                return new ActualFormParameters();
            } else if (!hasMoreElements()) throw new NoSuchElementException(); else {
                initializeParameterSets(backtracklevel);
                ActualFormParameters result = afps[levels - 1];
                backtracklevel = advanceIterators(levels - 1);
                return result;
            }
        }

        /** Tells whether a call to {@link #nextElement} will return a
	    valid element of the enumeration. In the special case of
	    the form not having any input elements returns the
	    negated value of the {@link #nextElementCalled} attribute
	    that is set by {@link #nextElement} upon the first call
	    in this special case.
	    
	    @return <tt>true</tt> if a call to {@link #nextElemen} will
		    successfully return another element of the enumeration,
		    <tt>false</tt> otherwise.
	  */
        public boolean hasMoreElements() {
            if (inputElements.size() == 0) return !nextElementCalled; else return (backtracklevel > 0);
        }

        /** Starting at <tt>level</tt> initializes the elements of the
	    <tt>ActualFormParameters</tt> array

	    @param level the level from which to start to initialize
		    the array elements by accessing the iterators' first
		    element. Expected to be positive or zero. Corresponds
		    with the indexes into <tt>afps</tt>.
	    @param afps the array whose elements to initialize
	    @param iterators the array of iterators, indexes corresponding
		    with thos in <tt>afps</tt>.
	  */
        private void initializeParameterSets(int level) {
            for (int i = level; i < afps.length; i++) {
                if (i > 0) afps[i] = new ActualFormParameters(afps[i - 1]); else afps[i] = new ActualFormParameters();
                ActualFormParameters a = (ActualFormParameters) iterators[i].nextElement();
                afps[i].add(a);
            }
        }

        /** Starting at <tt>level</tt> tries to advance the iterator
	    on that level by one and to compute the corresponding
	    element in <tt>afps</tt> by taking the <tt>afps</tt> element
	    of the next outer level (if not already on the outermost)
	    and adding the next element on <tt>level</tt> to it. In this
	    case, <tt>level+1</tt> is returned.<p>

	    If advancing the iterator on <tt>level</tt> fails, the
	    iterator is reset by requesting a new one on the corresponding
	    <tt>inputElements</tt> element and the method calls itself
	    recursively with <tt>level-1</tt> and returns the result
	    of the recursive call.<p>

	    @param level the level on which to start trying to advance
		    an iterator to the next element
	    @param iterators the array of iterators to use. If an iterator
		    bumps and is reset, the new iterator is placed into this
		    array.
	    @param afps For the level where advancing succeeded, the
		    resulting <tt>ActualFormParameters</tt> object is
		    inserted into the corresponding position in <tt>afps</tt>.
		    All other elements are left untouched, but those with
		    larger indexes are invalid and have to be reinitialized
		    using <tt>initializedParameterSets</tt>
	    @return the level from which on up to the highest level number
		    the contents of <tt>afps</tt> are invalid and have
		    to be reinitialized
	  */
        private int advanceIterators(int level) {
            int result;
            if (iterators[level].hasMoreElements()) {
                ActualFormParameters lAfps = (ActualFormParameters) iterators[level].nextElement();
                if (level > 0) afps[level] = new ActualFormParameters(afps[level - 1]); else afps[level] = new ActualFormParameters();
                afps[level].add(lAfps);
                result = level + 1;
            } else {
                iterators[level] = ((FormInputElement) inputElements.elementAt(level)).enumerateParameterSpace();
                if (level > 0) result = advanceIterators(level - 1); else result = 0;
            }
            return result;
        }

        /** set up to hold the current set of actual parameters up to
	    the n-th level for the n-th array element. So only afps[levels]
	    is complete.
	  */
        private ActualFormParameters[] afps;

        /** enumerations over the form's input elements' parameter space
	    as returned by their
	    {@link FormInputElement#enumerateParameterSpace} method.
	    The whole array is used by the recursive algorithm as the
	    "loop counter".
	  */
        private Enumeration[] iterators;

        /** up to which level did {@link advanceIterators} pop up the
	    iterator stack until a successful advancing took place?
	  */
        private int backtracklevel;

        /** the number of input elements of the form */
        private int levels;

        /** In the special case of no input elements this flag tells
	    if {@link #nextElement} has already been called and thus
	    represents the result of a call to {@link #hasMoreElements}
	    for this special case.
	  */
        private boolean nextElementCalled = false;
    }

    /** the URL of the document in which this form was found */
    private URL base;

    /** the <tt>name</tt> attribute, if specified in the form tag,
	<tt>null</tt> otherwise.
      */
    private String name;

    /** contains the input elements of this form. Contained elements
	are of type {@link FormInputElement}. This does <em>not</em>
	contain	any submit and cancel buttons. Note also, that for
	{@link RadioInputElement}s only one representative per name is
	contained in this collection (the first one found for each name).
	See also {@link #radios}.
      */
    private Vector inputElements;

    /** A form has a parameter space that, if finite (see
	{@link #hasFiniteParameterSpace}), can be enumerated. The
	form can be submitted with each of these parameter sets and
	the results can be searched using a public web search engine.
	This attribute tells the ratio of form outputs found that
	way over the total number of parameter combinations submitted
	during that course.<p>
	
	A negative value means the value hasn't been set yet.
	
	@see FormContentSearcher
	@see #setPublicResultsRatio
      */
    private double publicResultsRatio;

    /** stores the radio button input elements of this form in
	addition to their representation in <tt>inputElements</tt>
	in order to allow easy access based on the <tt>name</tt>
	attribute to allow quick merging of corresponding elements.<p>
	
	The keys are {@link String} objects denoting the <tt>name</tt>
	attribute of the radio input elements, the values are
	instances of class {@link RadioInputElement}.
      */
    private Hashtable radios;

    /** the eoncoding type (<tt>enctype</tt> attribute of the FORM
	tag
      */
    private String enctype;

    /** the action URL of this form */
    private URL action;

    /** the string on the submit button, extracted from the <tt>submit</tt>
	input element
      */
    private String submitButtonText;

    /** the string on the reset button, extracted from the <tt>reset</tt>
	input element
      */
    private String resetButtonText;

    /** the method. One of <tt>Form.GET</tt> and <tt>Form.POST</tt>. */
    private int method;

    /** identifies the GET method */
    public static final int GET = 1;

    /** identifies the POST method */
    public static final int POST = 2;

    /** identifies the PUT method */
    public static final int PUT = 3;

    /** tells whether or not to include the GET URLs for the form's
	complete parameter space in the <tt>toString</tt> representation
      */
    private static final boolean TO_STRING_WITH_URLS = false;

    /** a string constant used as null attribute value in a hashtable's
	element side, since <tt>null</tt> cannot be inserted
      */
    public static final String NULL = "";
}
