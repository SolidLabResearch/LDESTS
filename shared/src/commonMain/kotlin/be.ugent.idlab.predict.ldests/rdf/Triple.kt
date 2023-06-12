package be.ugent.idlab.predict.ldests.rdf

expect class Triple {
    val subject: Term
    val predicate: Term
    val `object`: Term
}

// TODO: try moving this inside of `Triple` and see if it remains compat with `N3Triple`; should work
expect interface Term {
    val value: String
}
