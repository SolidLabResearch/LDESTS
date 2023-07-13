@file:JsModule("ldests_compat")

package be.ugent.idlab.predict.ldests.lib.extra

// does the conversion like this:
// --
// async function * values<T> (source: AsyncIterator<T>) {
//    // have to read once first, so the "readable" event gets called
//    source.read();
//    let running = true
//    source.once("end", () => { running = false })
//    while (running) {
//        await new Promise((resolve) => { source.once("readable", resolve ) })
//        let value = source.read()
//        while (value != null) {
//            yield value
//            value = source.read()
//        }
//    }
//}
// FIXME: gradle tasks to use the files generated from js/src, use `npm pack` for yarn build, cp everything (main.js) in src
//  to exampleJsApp/node_modules/base
// TODO: maybe a better name?
@JsName("asyncIteratorToGenerator")
external fun <T> asyncIteratorToGenerator(source: AsyncIterator<T>): AsyncGenerator<T, *, *>
