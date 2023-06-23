package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.Binding
import be.ugent.idlab.predict.ldests.util.InputStream
import be.ugent.idlab.predict.ldests.util.consume
import be.ugent.idlab.predict.ldests.util.log
import be.ugent.idlab.predict.ldests.util.warn

class Stream(
    private val shape: Shape,
    internal val fragments: MutableList<Fragment> = mutableListOf()
) {

    init {
        // FIXME: temp
        fragments.add(
            Fragment(
                id = "fragment1",
                constraints = listOf()
            )
        )
    }

    /**
     * The general query used to extract data from sources
     */
    internal val query = shape.query

    internal suspend fun InputStream<Binding>.insert() = consume {
        val fragments = find(it)
        if (fragments.isEmpty()) {
            // naively creating a new fragment
            // TODO: maybe find a better approach by grouping these bindings first or something?
            // TODO: find a better approach to know what constraints to apply, like basing on existing fragments
            create(it)
                ?.append(it)
                ?: run { warn("Bad fragment found, skipping: $it"); return@consume }
        } else {
            fragments.forEach { fragment -> fragment.append(it) }
        }
    }

    /**
     * Matches the provided data w/ all bindings that match the requirements (tree path and shape). If no fragments
     *  are returned, a new one should be created through `create`.
     */
    internal fun find(data: Binding): List<Stream.Fragment> {
        // TODO: match the constraints in the bindings w/ the fragments
        return fragments
    }

    /**
     * Creates a new fragment that can be used to append the provided binding in. Returns `null` if the data doesn't
     *  match the stream's constraints
     */
    internal fun create(initial: Binding): Stream.Fragment? {
        // TODO: apply the constraints found in the binding (shape & time) w/ general shape to generate
        //  a new fragment
        TODO()
    }

    inner class Fragment(
        id: String,
        constraints: List<Shape.Property>
    ) {

        // id - content, TODO not yet published
        private val resources = mutableMapOf<String, String>()

        internal val url: String = "<TODO>" // combination of parent stream & id something

        private val constrainedShape = shape.applyConstraints(constraints)

        internal val query = constrainedShape.query

        /**
         * Adds the binding to the fragment, throws if the constraints aren't met
         */
        internal fun append(data: Binding) {
            // TODO: proper resource selection
            val result = shape.format(data)
//            resources["0"] = (resources["0"] ?: "") + "${data.id()}:${data.values().joinToString(";")};"
//            resources["0"] = (resources["0"] ?: "") + result
            log("Appended data in fragment, added: $result")
        }

    }

}
