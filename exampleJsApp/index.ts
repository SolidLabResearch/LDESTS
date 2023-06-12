import { LDESTS, Logging } from "ldests";

async function main() {
    const stream = await new LDESTS.Builder("http://localhost:3000/test-stream/")
                        .file("../DAHCC-Data/dataset_participant_sample_accel_data.nt")
                        .create();
    stream.append("../DAHCC-Data/dataset_participant_sample_accel_data.nt");
    await stream.flush();
    await stream.close();
    console.log("Finished main");
}

function sleep(ms) {
    return new Promise((resolve) => {
        setTimeout(resolve, ms);
    });
}

// only allowing warnings and errors from ldests to be logged
Logging.setLogLevel(Logging.WARN);
// executing the test, letting it exit the process accordingly
main();
