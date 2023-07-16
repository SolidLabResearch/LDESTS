# LDESTS: Linked Data Event Streams for Time Series
## Introduction
LDESTS aims to make time series data streams using RDF feasible on platforms such as Solid. It achieves this by using a data model (also referred to as "shape") defining the per-sample structure. By defining this structure in advance, repetition in structure can be avoided by filtering out the unique properties of individual samples. As only these unique properties are encoded in the resulting data, a **lossless, highly compressed** stream can be achieved.
## Currently supported
This tool is developed using [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html), allowing for multiple target platforms. Currently, **only JS/TS on Node.js is supported**. There are currently **no** plans to **support JS/TS on Web**. There are plans to add Java (and Android) integration **at a later stage**.
## Getting started
**Notice:** building from source has only been tested on **Linux**. While the build process should work for any platform capable of using Gradle, other platforms have not been tested. If you come across an issue building the library from source, be sure to let us know by creating an [issue](https://github.com/SolidLabResearch/LDESTS/issues/new).
### JS/TS on Node.js
#### NPM
Currently, **no NPM releases are available** due to the use of unreleased dependencies and unusual project hierarchy. If you want to integrate LDESTS in your own project right now, you have to build the library from source.
#### From source
The build process requires the commands `git`, `yarn` and `npm` to function, so make sure these are properly installed and configured before proceeding.\
Begin by cloning the repository to a known location:

```
[user@host ~]$ git clone https://github.com/SolidLabResearch/LDESTS.git
```
Next, navigate to the new folder and start the building process:
```
[user@host ~]$ cd LDESTS
[user@host LDESTS]$ ./gradlew jsLibrary:jsBuild
```
This process can take some time, as it fetches and configures all dependencies for the build to take place. After the process has finished, the folder `bin/js` should contain the resulting library. From here, you can add it as a dependency to your own project by running:
```
[user@host MyProject]$ npm install file:path/to/LDESTS/bin/js
```
Finally, you should be able to include the library into your own project, with type definitions available in TS projects:
```ts
import { LDESTS, Shape } from "ldests";
```
## How to use
### Node.js
After having [built and included LDESTS in your own project](#jsts-on-nodejs), you can take inspiration from the example found in `jsTest` to get started.
#### Creating a stream
Integrating custom streams is typically a two steps process.
Create a data model/shape
```ts
const shape: Shape = Shape.Companion.parse({
    type: "myDataType",
    identifier: "myDataIdentifier",
    constants: {
        "myConstantProperty": ["myConstantValues", "..."]
    },
    variables: {
        "myVariableProperty": "myVariableDatatype"
    }
});
```
Initialise or continue an LDESTS stream using the shape from above
```ts
const stream = await new LDESTS.Builder("myStream")
    .shape(shape)
    .build();
```
The stream created above can be configured to your needs: you can
- customise the configuration to have fragments to your desired size;
- attach different publishers (ranging from Solid pods to in-memory `N3Store`s);
- configure how to fragment your stream.

If you no longer need your stream object, you can `close` the stream, allowing all last transactions to finalise and any connections to stop properly:
```ts
await stream.close();
```
#### Inserting data
A stream can append new data through various sources:
```ts
stream.insert(triple); // adds a single triple "asynchronously" to the input stream
await stream.insertAsStore(triples); // adds an array of triples, converted to a store to be processed as a whole, and `await`s until finished
await stream.insertStore(store); // adds an entire store, to be processed as a whole, and `await`s until finished
await stream.append("path/to/file.nt"); // adds an entire file, to be processed as a whole, and `await`s until finished
```
It is possible for the resulting stream to not reflect new data *yet*. To make sure the stream has these new additions available to consumers, the stream has to be flushed:
```ts
await stream.flush(); // ensures all additional data is processed and published before it returns
```
#### Consuming a stream
First, the stream instance has to be created (as seen [here](#creating-a-stream)). Later, it will be possible to [automatically infer the stream's settings](#roadmapfuture-work) (including the shape) when using a single publisher. Currently, only Solid pods are compatible with querying. Querying is possible through callbacks with `query` or directly as an `N3Store` by using `queryAsStore`:
```ts
await stream.query(
    { type: PublisherType.Solid, url: "http://solid.local" } as SolidPublisherConfig, // looks for "myStream" as defined above on "solid.local"
    (triple) => console.log(`Got object ${triple.object.value}`), // logging the received triple's objects
    { "myFirstConstraint": ["myConstantValue", "..."] } // adding extra constraints to the data
    // extra time constraints can be added here as well
);
```
By providing constraints to the call, the stream can filter the available data so only relevant fragments are considered. Time constraints can also be added as a 4th and 5th parameter. `await`ing the result of `query` is not required, but can help with flow control.\
**Note:** as these triples are regenerated from the compressed stream, the exact subject URIs are lost. Every sample's subject is still unique throughout the query, however.
## How it works
The stream's data model describes every possible variation of the incoming data. By providing a property or a set of properties the stream has to fragment the incoming data with, all possible fixed data models can be generated. Every resulting model gets its own data stream. These data streams are being appended to when incoming data matches that stream's model.\
Matching what data belongs to which data stream is being done through multiple SPARQL queries. As the individual data models represent a template for the individual samples of that stream, generic queries for these samples can be made, allowing for multiple SPARQL queries to occur over the input stream. By then analysing the resulting bindings generated from these queries, the data can be associated with the right fragment of that model's data stream.\
Rereading this data also heavily uses the data models: by first analysing the requested data's constraints, a selection of data streams can be made. The fragments making up the various streams of interest are then filtered based on time. Every matching fragment is then read and parsed, which involves the recreation of all triples making up the original data sample, thanks to the presence of the data models.
## Roadmap/Future work
Current features and changes are either planned/possible (depending on requirements/demand, in no particular order):
- Support for the JVM (& Android)
- Automatic shape generation using samples from the data streams
- SPARQL querying support on LDESTS streams
- Inferring stream properties
- Supporting variable stream layout on a per-publisher basis
- Manual publisher management, including more granular control over their configurations
- Support for custom publisher implementations
- Proper (automated) releases for Node.js JS/TS on NPM (currently not yet possible [as noted above](#npm))
## Credits
This research was partly funded the Flemish Government under the “Onderzoeksprogramma
Artificiële Intelligentie (AI) Vlaanderen” programme, the SolidLab Vlaanderen project (Flemish
Government, EWI and RRF project VV023/10) and the FWO Project FRACTION (Nr. G086822N).\
The Node.js JS/TS integration uses
- [RDFJS/N3](https://github.com/rdfjs/N3.js/) for everything related to RDF storage (both in-memory and turtle files);
- [Comunica](https://github.com/comunica/comunica/) for querying triple sources, such as in-memory stores and Solid pods, through SPARQL queries;
- [Incremunica](https://github.com/maartyman/incremunica/) (**currently unreleased**) for querying ongoing RDF streams, created by manual insertion, through SPARQL queries

under the hood.
