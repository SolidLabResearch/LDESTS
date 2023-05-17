import * as ldests from "ldests"

async function main() {
    // const conn = ldests.be.ugent.idlab.predict.ldests.api.create("https://pod.playground.solidlab.be/")
    // const conn = ldests.be.ugent.idlab.predict.ldests.api.SolidConnection.Companion.create("https://pod.playground.solidlab.be/");
    const conn = new ldests.be.ugent.idlab.predict.ldests.api.SolidConnection.Builder()
        .url("https://pod.playground.solidlab.be/")
        .create();
    const directories = await conn.root.directories()
    console.log(directories)
    console.log(directories[0])
    const files = await directories[0].files()
    console.log(files)
    console.log(await files[0].content())
    console.log(await directories[0].directories())
    await conn.close()
}

main()
