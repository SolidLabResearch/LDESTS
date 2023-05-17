import { SolidConnection, Logging } from "ldests";

async function main() {
    console.log("Creating a connection");
    const connection = new SolidConnection.Builder()
        .url("http://localhost:3000/")
        .root("sensor_data/946715229402/")
        .create();
    console.log("Fetching directories...");
    const directories = await connection.root.directories();
    console.log(`${directories.length} directories found!`);
    console.log("In triples:");
    const triples = await connection.root.data();
    triples.forEach(triple => console.log(triple));
    console.log("Closing the connection...");
    await connection.close();
    // if the length is 0, the playground pod is either empty (unlikely), or something
    //  went wrong with the code (likely); returns 0 if there are contents found
    process.exit(Number(directories.length == 0));
}

// only allowing warnings and errors from ldests to be logged
Logging.setLogLevel(Logging.WARN);
// executing the test, letting it exit the process accordingly
main();
