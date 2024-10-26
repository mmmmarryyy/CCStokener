package org.apache.batik.bridge;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.batik.util.ParsedURL;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGDocument;
import org.apache.batik.dom.svg.XMLBaseSupport;
import org.apache.batik.gvt.font.GVTFontFamily;
import org.apache.batik.gvt.font.GVTFontFace;
import org.apache.batik.gvt.font.AWTFontFamily;
import org.apache.batik.gvt.font.FontFamilyResolver;

/**
 * This class represents a &lt;font-face> element or @font-face rule
 *
 * @author <a href="mailto:bella.robinson@cmis.csiro.au">Bella Robinson</a>
 * @version $Id: FontFace.java,v 1.1 2005/11/21 09:51:18 dev Exp $
 */
public abstract class FontFace extends GVTFontFace implements ErrorConstants {

    /**
     * List of ParsedURL's referencing SVGFonts or TrueType fonts,
     * or Strings naming locally installed fonts.
     */
    List srcs;

    /**
     * Constructes an SVGFontFace with the specfied font-face attributes.
     */
    public FontFace(List srcs, String familyName, float unitsPerEm, String fontWeight, String fontStyle, String fontVariant, String fontStretch, float slope, String panose1, float ascent, float descent, float strikethroughPosition, float strikethroughThickness, float underlinePosition, float underlineThickness, float overlinePosition, float overlineThickness) {
        super(familyName, unitsPerEm, fontWeight, fontStyle, fontVariant, fontStretch, slope, panose1, ascent, descent, strikethroughPosition, strikethroughThickness, underlinePosition, underlineThickness, overlinePosition, overlineThickness);
        this.srcs = srcs;
    }

    /**
     * Constructes an SVGFontFace with the specfied fontName.
     */
    protected FontFace(String familyName) {
        super(familyName);
    }

    public static CSSFontFace createFontFace(String familyName, FontFace src) {
        return new CSSFontFace(new LinkedList(src.srcs), familyName, src.unitsPerEm, src.fontWeight, src.fontStyle, src.fontVariant, src.fontStretch, src.slope, src.panose1, src.ascent, src.descent, src.strikethroughPosition, src.strikethroughThickness, src.underlinePosition, src.underlineThickness, src.overlinePosition, src.overlineThickness);
    }

    /**
     * Returns the font associated with this rule or element.
     */
    public GVTFontFamily getFontFamily(BridgeContext ctx) {
        String name = FontFamilyResolver.lookup(familyName);
        if (name != null) {
            GVTFontFace ff = createFontFace(name, this);
            return new AWTFontFamily(ff);
        }
        Iterator iter = srcs.iterator();
        while (iter.hasNext()) {
            Object o = iter.next();
            if (o instanceof String) {
                String str = (String) o;
                name = FontFamilyResolver.lookup(str);
                if (name != null) {
                    GVTFontFace ff = createFontFace(str, this);
                    return new AWTFontFamily(ff);
                }
            } else if (o instanceof ParsedURL) {
                try {
                    GVTFontFamily ff = getFontFamily(ctx, (ParsedURL) o);
                    if (ff != null) return ff;
                } catch (SecurityException ex) {
                    ctx.getUserAgent().displayError(ex);
                } catch (BridgeException ex) {
                    if (ERR_URI_UNSECURE.equals(ex.getCode())) ctx.getUserAgent().displayError(ex);
                } catch (Exception ex) {
                }
            }
        }
        return new AWTFontFamily(this);
    }

    /**
     * Tries to build a GVTFontFamily from a URL reference
     */
    protected GVTFontFamily getFontFamily(BridgeContext ctx, ParsedURL purl) {
        String purlStr = purl.toString();
        Element e = getBaseElement(ctx);
        SVGDocument svgDoc = (SVGDocument) e.getOwnerDocument();
        String docURL = svgDoc.getURL();
        ParsedURL pDocURL = null;
        if (docURL != null) pDocURL = new ParsedURL(docURL);
        String baseURI = XMLBaseSupport.getCascadedXMLBase(e);
        purl = new ParsedURL(baseURI, purlStr);
        UserAgent userAgent = ctx.getUserAgent();
        try {
            userAgent.checkLoadExternalResource(purl, pDocURL);
        } catch (SecurityException ex) {
            userAgent.displayError(ex);
            return null;
        }
        if (purl.getRef() != null) {
            Element ref = ctx.getReferencedElement(e, purlStr);
            if (!ref.getNamespaceURI().equals(SVG_NAMESPACE_URI) || !ref.getLocalName().equals(SVG_FONT_TAG)) {
                return null;
            }
            SVGDocument doc = (SVGDocument) e.getOwnerDocument();
            SVGDocument rdoc = (SVGDocument) ref.getOwnerDocument();
            Element fontElt = ref;
            if (doc != rdoc) {
                fontElt = (Element) doc.importNode(ref, true);
                String base = XMLBaseSupport.getCascadedXMLBase(ref);
                Element g = doc.createElementNS(SVG_NAMESPACE_URI, SVG_G_TAG);
                g.appendChild(fontElt);
                g.setAttributeNS(XMLBaseSupport.XML_NAMESPACE_URI, "xml:base", base);
                CSSUtilities.computeStyleAndURIs(ref, fontElt, purlStr);
            }
            Element fontFaceElt = null;
            for (Node n = fontElt.getFirstChild(); n != null; n = n.getNextSibling()) {
                if ((n.getNodeType() == Node.ELEMENT_NODE) && n.getNamespaceURI().equals(SVG_NAMESPACE_URI) && n.getLocalName().equals(SVG_FONT_FACE_TAG)) {
                    fontFaceElt = (Element) n;
                    break;
                }
            }
            SVGFontFaceElementBridge fontFaceBridge;
            fontFaceBridge = (SVGFontFaceElementBridge) ctx.getBridge(SVG_NAMESPACE_URI, SVG_FONT_FACE_TAG);
            GVTFontFace gff = fontFaceBridge.createFontFace(ctx, fontFaceElt);
            return new SVGFontFamily(gff, fontElt, ctx);
        }
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, purl.openStream());
            return new AWTFontFamily(this, font);
        } catch (Exception ex) {
        }
        return null;
    }

    /**
     * Default implementation uses the root element of the document 
     * associated with BridgeContext.  This is useful for CSS case.
     */
    protected Element getBaseElement(BridgeContext ctx) {
        SVGDocument d = (SVGDocument) ctx.getDocument();
        return d.getRootElement();
    }
}
