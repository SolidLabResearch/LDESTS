import { LDESTS, Shape, Logging } from "ldests";

async function main() {
    // creating a shape with properties
    // constant("rdfs:inDataset", "protego:_participant1")
    // constant("saref:measurementMadeBy", "dahcc_smartphone:OnePlus_IN2023")
    // constant("saref:relatesToProperty", "dahcc_acc:x", "dahcc_acc:y", "dahcc_acc:z")
    // variable("saref:hasValue", "xml#float")
    const shape = new Shape.Builder("<https://saref.etsi.org/core/Measurement>", "<https://saref.etsi.org/core/hasTimestamp>")
        .constant(
            "<http://rdfs.org/ns/void#inDataset>", [
                "<https://dahcc.idlab.ugent.be/Protego/_participant1>"
            ]
        ).constant(
            "<https://saref.etsi.org/core/measurementMadeBy>", [
                "<https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/Smartphone/OnePlus_IN2023>"
            ]
        ).constant(
            "<https://saref.etsi.org/core/relatesToProperty>", [
                "<https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/SmartphoneAcceleration/x>",
                "<https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/SmartphoneAcceleration/y>",
                "<https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/SmartphoneAcceleration/z>"
            ]
        ).variable(
            "<https://saref.etsi.org/core/hasValue>",
            "<http://www.w3.org/2001/XMLSchema#dateTime>"
        ).build();
    console.log("Created a first query!:");
    console.log(shape.query);
    const shape2 = shape.narrow([
        { "uri": "<https://saref.etsi.org/core/relatesToProperty>", "values": ["<https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/SmartphoneAcceleration/x>"] }
    ]);
    console.log("Created a second query!:");
    console.log(shape2.query);
    // const stream = await new LDESTS.Builder("http://localhost:3000/test-stream/")
    //     .shape(shape)
    //     .create();
    // stream.append("../DAHCC-Data/dataset_participant_sample_accel_data.nt");
    // await stream.flush();
    // await stream.close();
    console.log("Finished main");
}

function sleep(ms) {
    return new Promise((resolve) => {
        setTimeout(resolve, ms);
    });
}

// only allowing warnings and errors from ldests to be logged
Logging.setLogLevel(Logging.LOG);
// executing the test, letting it exit the process accordingly
main();
