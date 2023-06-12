// function used to iterate over an AsyncIterator
async function * asyncIteratorToGenerator(source) {
   source.read();
   let running = true
   source.once("end", () => { running = false })
   while (running) {
       await new Promise((resolve) => { source.once("readable", resolve) })
       let value = source.read()
       while (value != null) {
           yield value
           value = source.read()
       }
   }
}

exports.asyncIteratorToGenerator = asyncIteratorToGenerator