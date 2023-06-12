@file:JsModule("fs")
package be.ugent.idlab.predict.ldests.lib.node

@JsName("createReadStream")
external fun createReadFileStream(filename: String): ReadableNodeStream<String>