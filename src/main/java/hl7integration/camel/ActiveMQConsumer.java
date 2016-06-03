package hl7integration.camel;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.GenericMessage;
import ca.uhn.hl7v2.parser.*;
import ca.uhn.hl7v2.util.Terser;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;
import com.datastax.driver.core.Cluster;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.io.*;
import java.util.Properties;

public class ActiveMQConsumer {

    private String brokerUrl;
    private String username;
    private String password;

    public ActiveMQConsumer(final String brokerUrl, String username, String password) {
        this.brokerUrl = brokerUrl;
        this.username = username;
        this.password = password;
    }

    public void startReceiving(final String queueName) throws Exception {
        Connection connection = null;
        Session session = null;
        try {
            // get the connection factory
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(this.username, this.password, this.brokerUrl);
            // create connection
            connection = connectionFactory.createConnection();

            // start
            connection.start();

            // create session
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // create queue (it will create if queue doesn't exist)
            Destination queue = session.createQueue(queueName);

            MessageConsumer consumer = session.createConsumer(queue);

            // create listener
            // create listener
            MessageListener messageListener = new MessageListener() {

                @Override
                public void onMessage(Message message) {
                    // only text type message
                    String msg = "";
                    if (message instanceof TextMessage) {
                        TextMessage txt = (TextMessage) message;
                        try {
                            msg = txt.getText();
                        } catch (JMSException e) {
                            e.printStackTrace();
                        }

                        try {
                            if (!isA03Message(msg)) {
                                printMultilineMessageToScreen(msg);
                            }


                            // validate that the HL7 message is a proper 2.5.1 message
                            Boolean isValidHL7 = validateHL7(msg);

                            if (isValidHL7) {
                                recordMessage("HL7", msg, "VALID");

                                Boolean isA03 = isA03Message(msg);
                                if (isA03) {

                                    // check if this message is an ED (Emergency) message
                                    Boolean isED = isERMessage(msg);
                                    if (isED) {

                                        //TODO: convert to XML
                                        recordMessage("XML", msg, "XML");

                                        //TODO: validate cda portion???
                                    } else {
                                        recordMessage("HL7", msg, "HL7-NOT-ED");
                                    }


                                } else {
                                    recordMessage("HL7", msg, "HL7-NOT-A03");
                                }

                            } else {
                                recordMessage("HL7", msg, "HL7-NOT-VALID");
                            }

                        } catch (JMSException e) {
                            System.out.println("error retrieving message");
                            System.exit(1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }
            };

            consumer.setMessageListener(messageListener);
            System.in.read();
            consumer.close();
            session.close();
            connection.close();

        } catch (Exception e) {
            System.out.println("Exception while sending message to the queue" + e);
            throw e;
        }

    }

    public String receiveNextMessage(final String queueName) throws Exception {
        Connection connection = null;
        Session session = null;
        try {
            // get the connection factory
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(this.username, this.password, this.brokerUrl);
            // create connection
            connection = connectionFactory.createConnection();

            // start
            connection.start();

            // create session
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // create queue (it will create if queue doesn't exist)
            Destination queue = session.createQueue(queueName);


            MessageConsumer consumer = session.createConsumer(queue);

            TextMessage msg = (TextMessage) consumer.receive();

            printMultilineMessageToScreen(msg.getText());

//
//            // create listener
//            MessageListener messageListener = new MessageListener() {
//
//                @Override
//                public void onMessage(Message message) {
//                    // only text type message
//                    String msg = "";
//                    if (message instanceof TextMessage) {
//                        TextMessage txt = (TextMessage) message;
//                        try {
//                            msg = txt.getText();
//                            System.out.println("woot");
//                        } catch (JMSException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            };
//
//            consumer.setMessageListener(messageListener);
//            System.out.println("working");
//            System.in.read();
            consumer.close();
            session.close();
            connection.close();
//            return

            return msg.getText();

        } catch (Exception e) {
            System.out.println("Exception while sending message to the queue" + e);
            throw e;
        }
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

    public String convertXMLtoCDA(String xml) {
        String cda = "";

        //TODO: implement XML to CDA parser

        return cda;
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

        if (msg.indexOf("ADT") > -1) {
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

        if (msg.indexOf("ADT") > -1) {
            if (msg.indexOf("A03") > -1) {
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

}
