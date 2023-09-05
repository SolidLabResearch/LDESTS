@file:JsModule("fs")
package be.ugent.idlab.predict.ldests.lib.node

@JsName("readFile")
external fun readFile(filename: String, callback: (err: Error?, data: String?) -> Unit)