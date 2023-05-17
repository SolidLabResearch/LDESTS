import { SolidConnection } from "ldests"

async function main() {
    const connection = new SolidConnection.Builder()
        .url("https://pod.playground.solidlab.be/")
        .create();
    const directories = await connection.root.directories()
    console.log(directories)
    await connection.close()
}

main()
