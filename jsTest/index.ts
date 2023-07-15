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

const shape = Shape.Companion.parse({
    "type": "https://saref.etsi.org/core/Measurement",
    "identifier": "https://saref.etsi.org/core/hasTimestamp",
    "constants": {
        "http://rdfs.org/ns/void#inDataset": [
            "https://dahcc.idlab.ugent.be/Protego/_participant1"
        ],
        "https://saref.etsi.org/core/measurementMadeBy": [
            "https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/Smartphone/OnePlus_IN2023",
            "https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/Smartphone/RNG",
        ],
        "https://saref.etsi.org/core/relatesToProperty": [
            "https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/SmartphoneAcceleration/x",
            "https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/SmartphoneAcceleration/y",
            "https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/SmartphoneAcceleration/z"
        ]
    },
    "variables": {
        "https://saref.etsi.org/core/hasValue": "http://www.w3.org/2001/XMLSchema#float"
    }
});

async function main() {
    const stream = await new LDESTS.Builder("test-stream")
        .config({ 'window': 5, 'resourceSize': 2500 })
        .shape(shape)
        .queryUri("https://saref.etsi.org/core/measurementMadeBy")
        .attachSolidPublisher("http://localhost:3000")
        .create();
    for await (const triple of generateRandomData(10)) {
        stream.insert(triple);
    }
    await stream.flush();
    const triples = new Array<Quad>();
    for await (const triple of generateRandomData(10)) {
        triples.push(triple)
    }
    await stream.insertStore(triples);
    await stream.flush();
    await stream.query(
        "http://localhost:3000",
        (triple) => console.log(`Got object ${triple.toJSON()}`),
        {
            "https://saref.etsi.org/core/relatesToProperty": ["https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/SmartphoneAcceleration/x", "https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/SmartphoneAcceleration/z"],
            "https://saref.etsi.org/core/measurementMadeBy": "https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/Smartphone/RNG"
        },
        946718829400,
        946718829900
    );
    await stream.close();
    console.log("Finished main");
}

// executing the test, letting it exit the process accordingly
main();
