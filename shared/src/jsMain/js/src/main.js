// function used to iterate over an AsyncIterator
async function * asyncIteratorToGenerator(source) {
   source.read();
   let running = true
   const end = new Promise((resolve) => source.once("end", () => resolve(false)))
   while (running) {
       const hold = new Promise((resolve) => source.once("readable", () => resolve(true)))
       running = await Promise.race([hold, end])
       let value = source.read()
       while (value != null) {
           yield value
           value = source.read()
       }
   }
}

exports.asyncIteratorToGenerator = asyncIteratorToGenerator