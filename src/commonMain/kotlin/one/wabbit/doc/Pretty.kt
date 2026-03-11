package one.wabbit.doc

interface Pretty<in T> {
    fun pretty(t: T): String
}
