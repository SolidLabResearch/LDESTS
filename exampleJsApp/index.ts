import { LDESTS, Shape } from "ldests";
import { Quad, DataFactory } from "n3";
const { namedNode, literal } = DataFactory;

async function * generateRandomData(limit: number) {
    var i = 0
    const properties = [
        "https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/SmartphoneAcceleration/x",
        "https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/SmartphoneAcceleration/y",
        "https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/SmartphoneAcceleration/z"
    ];
    while (i < limit) {
        yield new Quad(
            namedNode(`http://example.com/obs${i}`),
            namedNode("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
            namedNode("https://saref.etsi.org/core/Measurement")
        );
        yield new Quad(
            namedNode(`http://example.com/obs${i}`),
            namedNode("https://saref.etsi.org/core/hasValue"),
            literal(i)
        );
        yield new Quad(
            namedNode(`http://example.com/obs${i}`),
            namedNode("https://saref.etsi.org/core/hasTimestamp"),
            literal(new Date().toISOString())
        );
        yield new Quad(
            namedNode(`http://example.com/obs${i}`),
            namedNode("https://saref.etsi.org/core/relatesToProperty"),
            namedNode(properties[Math.floor(Math.random() * properties.length)])
        );
        yield new Quad(
            namedNode(`http://example.com/obs${i}`),
            namedNode("https://saref.etsi.org/core/measurementMadeBy"),
            namedNode("https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/Smartphone/RNG")
        );
        yield new Quad(
            namedNode(`http://example.com/obs${i}`),
            namedNode("http://rdfs.org/ns/void#inDataset"),
            namedNode("https://dahcc.idlab.ugent.be/Protego/_participant1")
        );
        ++i;
        await new Promise((resolve) => setTimeout(resolve, 200));
    }
}

// creating a shape with properties
// constant("rdfs:inDataset", "protego:_participant1")
// constant("saref:measurementMadeBy", "dahcc_smartphone:OnePlus_IN2023")
// constant("saref:relatesToProperty", "dahcc_acc:x", "dahcc_acc:y", "dahcc_acc:z")
// variable("saref:hasValue", "xml#float")
const shape = new Shape.Builder("https://saref.etsi.org/core/Measurement", "https://saref.etsi.org/core/hasTimestamp")
    .constant(
        "http://rdfs.org/ns/void#inDataset", [
            "https://dahcc.idlab.ugent.be/Protego/_participant1"
        ]
    ).constant(
        "https://saref.etsi.org/core/measurementMadeBy", [
            "https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/Smartphone/OnePlus_IN2023",
            "https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/Smartphone/RNG",
        ]
    ).constant(
        "https://saref.etsi.org/core/relatesToProperty", [
            "https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/SmartphoneAcceleration/x",
            "https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/SmartphoneAcceleration/y",
            "https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/SmartphoneAcceleration/z"
        ]
    ).variable(
        "https://saref.etsi.org/core/hasValue",
        "http://www.w3.org/2001/XMLSchema#float"
    ).build();

async function main() {
    const stream = await new LDESTS.Builder("test-stream")
        .config({ 'window': 5, 'resourceSize': 2500 })
        .shape(shape)
        .queryUri("https://saref.etsi.org/core/measurementMadeBy")
        .attachSolidPublisher("http://localhost:3000")
//         .attachDebugPublisher()
        .create();
    await stream.append("../DAHCC-Data/dataset_participant_sample_accel_data.nt");
    // for await (const triple of generateRandomData(10)) {
    //     stream.insert(triple);
    // }
    await stream.flush();
    const triples = new Array<Quad>();
    for await (const triple of generateRandomData(10)) {
        triples.push(triple)
    }
    stream.insertBulk(triples);
    await stream.flush();
    await stream.query(
        "http://localhost:3000",
        (triple) => console.log(`Got object ${triple.object.value}`),
        {
            "https://saref.etsi.org/core/relatesToProperty": ["https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/SmartphoneAcceleration/x", "https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/SmartphoneAcceleration/z"],
            "https://saref.etsi.org/core/measurementMadeBy": "https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/Smartphone/OnePlus_IN2023"
        },
        946718829400,
        946718829900
    );
    await stream.close();
    console.log("Finished main");
}

// executing the test, letting it exit the process accordingly
main();
