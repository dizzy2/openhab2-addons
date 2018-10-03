/**
 *
 */
package org.openhab.binding.samsungac.internal;

import org.eclipse.jdt.annotation.NonNull;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author rastiiik
 *
 */
public class StateMachineSaxHandler extends DefaultHandler {
    public static interface EnterStateHandler {
        public void enterState(Attributes attrs) throws SAXException;
    }

    /**
     * Parser state base class for SAX parser state machine.
     */
    public static class ElementParserState {
        private ElementParserState parentState = null;
        private int recursion_counter = 0;

        public ElementParserState enterState(ElementParserState parentState, Attributes attrs) throws SAXException {
            this.parentState = parentState;
            this.recursion_counter = 1;
            return this;
        }

        public ElementParserState startElement(ElementParserState parentState, String name, Attributes attrs)
                throws SAXException {
            recursion_counter++;
            return this;
        }

        public ElementParserState endElement(String name) throws SAXException {
            if (--recursion_counter > 0) {
                return this;
            } else {
                ElementParserState parentState = this.parentState;

                this.parentState = null;
                return parentState;
            }
        }

        static ElementParserState fromEnterStateHandler(EnterStateHandler enterStateHandler) {
            return new ElementParserState() {

                @Override
                public ElementParserState enterState(ElementParserState parentState, Attributes attrs)
                        throws SAXException {
                    enterStateHandler.enterState(attrs);
                    return super.enterState(parentState, attrs);
                }

            };
        }

    }

    private final ElementParserState rootParserState;
    private ElementParserState actualState = null;

    /**
     * @param rootParserState
     */
    public StateMachineSaxHandler(@NonNull ElementParserState rootParserState) {
        this.rootParserState = rootParserState;
    }

    @Override
    public void startDocument() throws SAXException {
        this.actualState = rootParserState;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        actualState = actualState.startElement(actualState, qName, attributes);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        actualState = actualState.endElement(qName);
    }
}
