const _engine = require("../lib/incremunica/engines/query-sparql-incremental/lib/QueryEngine.js");
const _store = require("../lib/incremunica/packages/incremental-rdf-streaming-store/lib/StreamingStore.js");

exports.QueryEngine = _engine.QueryEngine;
exports.StreamingStore = _store.StreamingStore;
