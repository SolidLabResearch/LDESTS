package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.core.Shape.Companion.id
import be.ugent.idlab.predict.ldests.core.Stream.Fragment.Rules.Companion.split
import be.ugent.idlab.predict.ldests.rdf.Binding
import be.ugent.idlab.predict.ldests.rdf.NamedNodeTerm
import be.ugent.idlab.predict.ldests.rdf.TripleProvider
import be.ugent.idlab.predict.ldests.rdf.query
import be.ugent.idlab.predict.ldests.util.consume
import be.ugent.idlab.predict.ldests.util.join
import be.ugent.idlab.predict.ldests.util.log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class Stream {

    private val shape: Shape
    private val fragments: MutableList<Fragment> = mutableListOf()

    // TODO: check the rules w/ remote stream, if present, and change them here prior to publishing if necessary
    // TODO: properly function when no rules exist, making a single entry w/ empty constraints
    private val ruleSet: MutableList<Fragment.Rules>

    constructor(
        shape: Shape,
        rules: List<Fragment.Rules>
    ) {
        this.shape = shape
        this.ruleSet = rules.toMutableList()
    }

    constructor(
        shape: Shape,
        queryUris: List<NamedNodeTerm>
    ) {
        this.shape = shape
        // FIXME: use all! uris from the list
        this.ruleSet = this.shape.split(queryUris.first()).toMutableList()
    }

    /**
     * Inserts the triples provided by the source directly into the buffer through multiple streams, does not return a
     *  new stream as it consumes directly and blocks (suspends) until finished.
     */
    internal suspend fun TripleProvider.insert() {
        // src: https://stackoverflow.com/a/55895056
        coroutineScope {
            // iterating over all existing rules, using their queries to obtain data, and using the data's timestamp
            //  to get the appropriate fragment (creating one if necessary)
            ruleSet.map { rules ->
                async {
                    query(rules.shape.query)
                        .consume { binding ->
                            val timestamp = binding.id()
                            findOrCreate(rules = rules, timestamp = timestamp)
                                .forEach { it.append(binding) }
                        }
                        .join()
                }
            }.awaitAll()
        }
    }

    /**
     * Finds all fragments satisfying the provided rules, and containing the data for the provided timestamp. All
     *  provided fragments should contain data that fits these parameters (rules & timestamp). Creates a new fragment
     *  that satisfies these parameters, if necessary.
     * Returns at least one fragment
     */
    // TODO: timestamp is maybe not versatile enough (e.g. geospatial ?)
    internal fun findOrCreate(rules: Fragment.Rules, timestamp: Long) = fragments
        .filter { it.rules == rules /* && timestamp ... */ }
        .ifEmpty {
            val new = Fragment(
                // FIXME: better ID system
                id = "fragment_${fragments.size}",
                rules = rules
            )
            fragments.add(new)
            listOf(new)
        }

    class Fragment internal constructor(
        id: String,
        val rules: Rules
    ) {

        init {
            log("Created a fragment with id `$id`")
        }

        class Rules(
            constraints: Map<NamedNodeTerm, Shape.Property>,
            parent: Shape
        ) {

            /**
             * The associated shape with this rule set.
             * Also contains the generated query, used to get all data satisfying the rules (adapted query) that were
             *  applied on the base shape (UNUSED! base query)
             */
            val shape = parent.narrow(constraints)

            companion object {

                /**
                 * Divides the given shape into various rules that can be used to create new fragments with, so the
                 *  provided `uri` can be used to query over (e.g. constant `ex:prop` with 3 possible values, results in
                 *  3 rules dividing `ex:prop` over the three possible values, so up to 3 fragments can be made)
                 */
                fun Shape.split(vararg uris: NamedNodeTerm): List<Rules> {
                    if (uris.isEmpty()) {
                        return listOf(
                            Rules(
                                constraints = mapOf() /* no constraints */,
                                parent = this
                            )
                        )
                    }
                    // creating the initial set of rules
                    val iter = uris.iterator()
                    var uri = iter.next()
                    var rules = (properties[uri] as Shape.ConstantProperty).values.map {
                        Rules(
                            constraints = mapOf(uri to Shape.ConstantProperty(it)),
                            parent = this
                        )
                    }
                    // recursively split all existing fragments from the previous iteration according
                    //  to the current iteration's uri
                    while (iter.hasNext()) {
                        uri = iter.next()
                        rules = rules.flatMap { rule ->
                            (rule.shape.properties[uri] as Shape.ConstantProperty).values.map { prop ->
                                Rules(
                                    constraints = mapOf(uri to Shape.ConstantProperty(prop)),
                                    parent = rule.shape
                                )
                            }
                        }
                    }
                    return rules
                }
            }

        }

        // resource id/url - content, TODO not yet published
        private val resources = mutableMapOf<String, String>()

        internal val url: String = "<TODO>/$id" // combination of parent stream & id something

        /**
         * Adds the binding to the fragment, throws if the constraints aren't met
         */
        internal fun append(data: Binding) {
            val result = rules.shape.format(data)
//            resources["0"] = (resources["0"] ?: "") + "${data.id()}:${data.values().joinToString(";")};"
//            resources["0"] = (resources["0"] ?: "") + result
            log("Appended data in fragment, added: $result")
        }

    }

}
