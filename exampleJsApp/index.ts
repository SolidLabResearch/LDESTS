import { LDESTS, Logging } from "ldests";

async function main() {
    const ldests = new LDESTS();
    ldests.append("../../DAHCC-Data/dataset_participant_sample_accel_data.nt");
    await sleep(50);
    console.log("stopping");
    ldests.close();
    console.log("stopped");
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
