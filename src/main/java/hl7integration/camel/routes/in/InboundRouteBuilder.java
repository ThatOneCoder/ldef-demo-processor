package hl7integration.camel.routes.in;

import hl7integration.camel.Processor;
import org.apache.camel.component.hl7.HL7DataFormat;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InboundRouteBuilder extends SpringRouteBuilder {
    private static final Logger log = LoggerFactory.getLogger(InboundRouteBuilder.class);

    @Override
    public void configure() throws Exception {

        Processor processor = new Processor();
        String hl7Dir = processor.getPropValues("hl7-message-dir");
        from("bean:processor?method=dequeueMessage").routeId("Processor-Camel-Route")//.delay(100)
                .to("bean:processor?method=recordHL7Message")
                .end();
    }
}
