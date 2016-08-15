package hl7integration.camel;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.GenericMessage;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.*;
import ca.uhn.hl7v2.util.Terser;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
//import com.leidos.def.core.hl7.cda.CDAGenerator;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import org.springframework.stereotype.Component;
import sun.net.www.content.text.Generic;

import java.io.*;
import java.util.Date;
import java.util.Properties;

//import ca.uhn.hl7v2.model.v22.datatype.PN;

@Component
public class Processor {

    public String dequeueMessage(Message in) throws Exception {

        ActiveMQConsumer activeMQConsumer = new ActiveMQConsumer("", "", "");

        String activemqHost = (System.getenv("ACTIVEMQ_SERVICE_HOST")== null) ? getPropValues("activemq-host") : System.getenv("ACTIVEMQ_SERVICE_HOST");
        String activemqPort = (System.getenv("ACTIVEMQ_SERVICE_PORT")== null) ? getPropValues("activemq-port") : System.getenv("ACTIVEMQ_SERVICE_PORT");
        String activemqUri = "tcp://" + activemqHost + ":" + activemqPort;

        ActiveMQConsumer consumer = new ActiveMQConsumer(activemqUri, "admin", "admin");
        String msg;
//            consumer.startReceiving("demo.queue");
        msg = consumer.receiveNextMessage("demo.queue");
        return msg;
    }

    public void recordHL7Message(String msg) {
        if (msg.length() > 0) {
            try {
                recordMessage(msg, "TRANSFORM");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void printMultilineMessageToScreen(String msg) throws IOException {
        File file = new File(msg);
        BufferedReader reader = new BufferedReader(new StringReader(msg));

        String line = null;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }

    public String getPropValues(String property) throws IOException {
        String result = "";
        InputStream inputStream = null;

        try {
            Properties prop = new Properties();
            String propFileName = "endpoint.properties";

            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

            // get the property value and print it out
            result = prop.getProperty(property);

        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            inputStream.close();
        }
        return result;
    }


    public String convertHL7ToXML(String in) throws Exception {
        String xml = "";
        HapiContext context = new DefaultHapiContext();
        Parser pipeParser = context.getPipeParser();
        XMLParser xmlParser = new DefaultXMLParser();

        ca.uhn.hl7v2.model.Message message = pipeParser.parse(in);

        try {
            xml = xmlParser.encode(message);
        } catch (EncodingNotSupportedException e) {
            e.printStackTrace();
        } catch (HL7Exception e) {
            e.printStackTrace();
        }

        return xml;
    }

    public void recordMessage(String msg, String status) throws Exception {
        java.util.Date date = new java.util.Date();
        String mongoIp = (System.getenv("MONGODB_SERVICE_HOST")== null) ? getPropValues("mongo-ip") : System.getenv("MONGODB_SERVICE_HOST");

        // create mongo client
        MongoClient mongo = new MongoClient( mongoIp , 27017 );

        // get mongo db
        DB db = mongo.getDB("ldef_hl7_demo");

        // get mongo collection (aka table)
        DBCollection table = db.getCollection("message");

        // create document(data)
        BasicDBObject document = new BasicDBObject();

        // create hapi context
        HapiContext context = new DefaultHapiContext();

        // set up
        context.setModelClassFactory(new GenericModelClassFactory());
        context.setValidationContext(ValidationContextFactory.noValidation());
        GenericMessage genMessage = (GenericMessage) context.getPipeParser().parse(msg);
        Terser terser = new Terser(genMessage);

        // parse message for content
        String assign_auth = String.valueOf(terser.get("/MSH-4-2"));
        String eoc_acc = String.valueOf(terser.get("/PID-3-1"));
        String episode = assign_auth + eoc_acc;
//        System.out.println("episode: " + episode);
//        System.out.print(msg);

        String dt = "dateof( now() )";
        String type = String.valueOf(terser.get("/MSH-9-1"));

        String query = ("INSERT INTO message (id, episode, message, status, statusdate, type) VALUES (now(),'" + episode + "','" + msg + "','" + status + "'," + dt + ",'" + type + "')");

        // add data to a mongo document
        document.put("episode", episode);
        document.put("message", msg);
        document.put("status", status);
        document.put("statusdate", date);
        document.put("type", type);
        table.insert(document);

//        System.out.println("query");
//        System.out.println(query);
        // record message event
        // last_modified, action, data

//        session.execute(query);
//
//        // close cluster
//        cluster.close();
    }

    public void recordMessage(String msgType, String msg, String status) throws Exception {
        java.util.Date date = new java.util.Date();

        // create cluster
//        Cluster cluster;
//
//        // create session
//        Session session = null;
//
//        String cassandraIp = getPropValues("cassandra-ip");
//        String keyspace = getPropValues("cassandra-keyspace");

        String mongoIp = getPropValues("mongo-ip");
//
//        cluster = Cluster.builder().addContactPoint(cassandraIp).build();
////        cluster = Cluster.builder().addContactPoint("10.32.227.87").build();
//        session = cluster.connect(keyspace);
////        cluster = Cluster.builder().addContactPoint("10.32.227.87").build();
////        System.out.println("connected to keyspace " + keyspace + " at IP " + cassandraIp);


        // create mongo client
        MongoClient mongo = new MongoClient(mongoIp, 27017);

        // get mongo db
        DB db = mongo.getDB("ldef_hl7_demo");

        // get mongo collection (aka table)
        DBCollection table = db.getCollection("message");

        // create document(data)
        BasicDBObject document = new BasicDBObject();

        // create hapi context
        HapiContext context = new DefaultHapiContext();

        // set up
        context.setModelClassFactory(new GenericModelClassFactory());
        context.setValidationContext(ValidationContextFactory.noValidation());
        GenericMessage genMessage = (GenericMessage) context.getPipeParser().parse(msg);
        Terser terser = new Terser(genMessage);

        // parse message for content
        String assign_auth = String.valueOf(terser.get("/MSH-4-2"));
        String eoc_acc = String.valueOf(terser.get("/PID-3-1"));
        String episode = assign_auth + eoc_acc;
//        System.out.println("episode: " + episode);
//        System.out.print(msg);

        String dt = "dateof( now() )";
        String type = String.valueOf(terser.get("/MSH-9-1"));

        if (msgType == "XML") {
            String xml = convertHL7ToXML(msg);
            msg = xml;
        }

        String query = ("INSERT INTO message (id, episode, message, status, statusdate, type) VALUES (now(),'" + episode + "','" + msg + "','" + status + "'," + dt + ",'" + type + "')");
        // add data to a mongo document
        document.put("episode", episode);
        document.put("message", msg);
        document.put("status", status);
        document.put("statusdate", date);
        document.put("type", type);
        table.insert(document);

//        System.out.println("query");
//        System.out.println(query);
        // record message event
        // last_modified, action, data

//        session.execute(query);
//
//        // close cluster
//        cluster.close();
    }

    public boolean isERMessage(String msg) throws HL7Exception {
        boolean response = false;

        //TODO: if the message is not an ER message, return false, else return true

        // create hapi context
        HapiContext context = new DefaultHapiContext();

        // set up
        context.setModelClassFactory(new GenericModelClassFactory());
        context.setValidationContext(ValidationContextFactory.noValidation());
        GenericMessage genMessage = (GenericMessage) context.getPipeParser().parse(msg);
        Terser terser = new Terser(genMessage);

        // parse message for content
        String hl7Type = String.valueOf(terser.get("/MSH-9-1"));

        if (hl7Type == "ADT") {
            String type = String.valueOf(terser.get("/PV1-2-1"));
            if (type == "E") {
                response = true;
            } else {
                response = false;
            }
        } else {
            // return true for ORU (HACK!!!)
            response = true;
        }

        return response;
    }

    public boolean isA03Message(String msg) throws HL7Exception {
        boolean response = false;

        //TODO: if the message is not an ER message, return false, else return true

        // create hapi context
        HapiContext context = new DefaultHapiContext();

        if (msg.contains("ADT")) {
//            System.out.println(msg.indexOf("A03"));

        }

        // set up
        context.setModelClassFactory(new GenericModelClassFactory());
        context.setValidationContext(ValidationContextFactory.noValidation());
        GenericMessage genMessage = (GenericMessage) context.getPipeParser().parse(msg);
        Terser terser = new Terser(genMessage);

        // parse message for content
        String hl7Type = String.valueOf(terser.get("/MSH-9-1"));
//        System.out.println("HL7 TYPE: " + hl7Type);

        if (msg.contains("ADT")) {
            if (msg.contains("A03")) {
                response = true;
            }
        } else {
            // return true for ORU (HACK!!!)
            response = true;
        }

        return response;
    }

    public boolean validateHL7(String msg) {
        boolean response = false;

        //TODO: use hapi to validate the hl7 message
        HapiContext context = new DefaultHapiContext();
        context.setValidationContext(ValidationContextFactory.defaultValidation());
        PipeParser pipeParser = context.getPipeParser();

        try {
            pipeParser.parse(msg);
            response = true;
        } catch (HL7Exception e) {
            response = false;
            System.out.println("Invalid Message: " + e.getMessage());
        }

        return response;
    }

}

