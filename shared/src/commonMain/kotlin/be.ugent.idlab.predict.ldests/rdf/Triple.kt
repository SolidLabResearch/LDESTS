package be.ugent.idlab.predict.ldests.rdf

expect class Triple {
    val subject: Term
    val predicate: Term
    val `object`: Term
}

expect interface Term {
    val value: String
}
