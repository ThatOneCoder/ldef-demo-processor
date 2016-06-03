package hl7integration.camel;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
//import ca.uhn.hl7v2.model.v22.datatype.PN;
import ca.uhn.hl7v2.model.v251.message.ADT_A01;
import ca.uhn.hl7v2.model.v251.segment.MSH;
import ca.uhn.hl7v2.parser.DefaultXMLParser;
import ca.uhn.hl7v2.parser.EncodingNotSupportedException;
import ca.uhn.hl7v2.parser.Parser;

import ca.uhn.hl7v2.parser.XMLParser;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

@Component
public class RespondACK {

    public Message process(Message in) throws Exception {
//        System.out.println(in.toString());
        printMultilineMessageToScreen(in.toString());
        Message out = in.generateACK();
//        System.out.println(out.toString());
        return out;

    }

    public void printMultilineMessageToScreen(String msg) throws IOException {
        File file = new File(msg);
        BufferedReader reader = new BufferedReader(new StringReader(msg));

        String line = null;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }
}

