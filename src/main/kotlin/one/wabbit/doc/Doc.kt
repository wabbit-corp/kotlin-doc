package one.wabbit.doc

import kotlinx.serialization.Serializable
import one.wabbit.data.LazyList
import kotlin.math.floor

// https://hackage.haskell.org/package/prettyprinter
// https://github.com/quchen/prettyprinter/blob/5db8a0f55c01ac9388d7cda5290a7c97d9735607/prettyprinter/app/GenerateReadme.hs
// https://github.com/quchen/prettyprinter/blob/5db8a0f55c01ac9388d7cda5290a7c97d9735607/prettyprinter/src/Prettyprinter/Internal.hs#L1752
// https://hackage.haskell.org/package/wl-pprint-1.2.1/docs/Text-PrettyPrint-Leijen.html
// https://hackage.haskell.org/package/wl-pprint-text
// https://hackage.haskell.org/package/wl-pprint-extras
// https://hackage.haskell.org/package/wl-pprint-console
// https://hackage.haskell.org/package/wl-pprint-annotated
// https://hackage.haskell.org/package/pretty
// https://hackage.haskell.org/package/pretty-compact
// https://hackage.haskell.org/package/marked-pretty
// https://hackage.haskell.org/package/final-pretty-printer
// https://hackage.haskell.org/package/boxes-0.1.5/docs/Text-PrettyPrint-Boxes.html
// https://github.com/youxingz/pretty-printer/tree/master/src/main/java/io/pretty/core
// https://github.com/clemtoy/pptree/blob/master/pptree/pptree.py
// https://github.com/tommikaikkonen/prettyprinter/blob/master/prettyprinter/doc.py
// https://github.com/weso/document/blob/master/modules/document/src/main/scala/es/weso/document/Document.scala


@Serializable sealed interface DocOp<out A> {
    @Serializable data class Text(val text: String) : DocOp<Nothing>
    // indentation level for the (next) line
    @Serializable data class Line(val spaces: Int) : DocOp<Nothing>
    @Serializable data class PushAnnotation<A>(val annotation: A) : DocOp<A>
    @Serializable data object PopAnnotation : DocOp<Nothing>
}

@Serializable data class DocOps<out A>(val ops: LazyList<DocOp<A>>) {
    override fun toString(): String {
        val sb = StringBuilder()
        for (op in ops.iterator()) {
            when (op) {
                is DocOp.Text -> sb.append(op.text)
                is DocOp.Line -> sb.append("\n" + " ".repeat(op.spaces))
                is DocOp.PushAnnotation -> { }
                is DocOp.PopAnnotation -> { }
            }
        }
        return sb.toString()
    }
}

// -- | Maximum number of characters that fit in one line. The layout algorithms
//-- will try not to exceed the set limit by inserting line breaks when applicable
//-- (e.g. via 'softline'').
//data PageWidth
//
//    = AvailablePerLine !Int !Double
//    -- ^ Layouters should not exceed the specified space per line.
//    --
//    --   - The 'Int' is the number of characters, including whitespace, that
//    --     fit in a line. A typical value is 80.
//    --
//    --   - The 'Double' is the ribbon with, i.e. the fraction of the total
//    --     page width that can be printed on. This allows limiting the length
//    --     of printable text per line. Values must be between 0 and 1, and
//    --     0.4 to 1 is typical.
//
//    | Unbounded
//    -- ^ Layouters should not introduce line breaks on their own.
//
//    deriving (Eq, Ord, Show, Typeable)
@Serializable sealed interface PageWidth {
    @Serializable data class AvailablePerLine(val lineLength: Int, val ribbonFraction: Double) : PageWidth {
        // -- | The remaining width on the current line.
        //remainingWidth :: Int -> Double -> Int -> Int -> Int
        //remainingWidth lineLength ribbonFraction lineIndent currentColumn =
        //    min columnsLeftInLine columnsLeftInRibbon
        //  where
        //    columnsLeftInLine = lineLength - currentColumn
        //    columnsLeftInRibbon = lineIndent + ribbonWidth - currentColumn
        //    ribbonWidth =
        //        (max 0 . min lineLength . floor)
        //            (fromIntegral lineLength * ribbonFraction)

        fun remainingWidth(lineIndent: Int, currentColumn: Int): Int {
            val columnsLeftInLine = lineLength - currentColumn
            val columnsLeftInRibbon = lineIndent + (ribbonFraction * lineLength) - currentColumn
            return maxOf(0, minOf(lineLength, floor(lineLength * ribbonFraction).toInt()))
        }
    }
    @Serializable data object Unbounded : PageWidth
}

@Serializable sealed interface Doc<out Annotation> {
    @Serializable private data object Empty : Doc<Nothing> {
        override fun toString(): String = this.layoutCompact().toString()
    }

    @Serializable private data object Line : Doc<Nothing> {
        override fun toString(): String = this.layoutCompact().toString()
    }

    @Serializable private data class FlatAlt<Annotation>(
        val doc: Doc<Annotation>,
        val flat: Doc<Annotation>
    ) : Doc<Annotation> {
        override fun toString(): String = this.layoutCompact().toString()
    }

    @Serializable private data class Text(val text: String) : Doc<Nothing> {
        init {
            require(text.contains('\n').not()) { "Text must not contain newlines" }
        }

        override fun toString(): String = this.layoutCompact().toString()
    }

    @Serializable private data class Nest<Annotation>(
        val indent: Int,
        val doc: Doc<Annotation>
    ) : Doc<Annotation> {
        override fun toString(): String = this.layoutCompact().toString()
    }

    @Serializable private data class Annotated<Annotation>(
        val annotation: Annotation,
        val doc: Doc<Annotation>
    ) : Doc<Annotation> {
        override fun toString(): String = this.layoutCompact().toString()

    }
    @Serializable private data class Concat<Annotation>(
        val docs: List<Doc<Annotation>>
    ) : Doc<Annotation> {
        override fun toString(): String = this.layoutCompact().toString()
    }

    @Serializable private data class Union<Annotation>(
        val left: Doc<Annotation>,
        val right: Doc<Annotation>
    ) : Doc<Annotation> {
        override fun toString(): String = this.layoutCompact().toString()
    }


    fun nest(indent: Int): Doc<Annotation> = Doc.nest(indent, this)
    fun hang(indent: Int): Doc<Annotation> = Doc.hang(indent, this)
    fun group(): Doc<Annotation> = Doc.group(this)
    fun align(): Doc<Annotation> = Doc.align(this)

//    private data class Column<Annotation>(
//        val f: (Int) -> Doc<Annotation>) : Doc<Annotation>
//    private data class WithPageWidth<Annotation>(
//        val f: (Int) -> Doc<Annotation>) : Doc<Annotation>
//    private data class Nesting<Annotation>(
//        val f: (Int) -> Doc<Annotation>) : Doc<Annotation>

    fun layoutCompactIter(): Iterator<DocOp<Annotation>> = iterator {
        var col: Int = 0
        val stack = mutableListOf<Doc<Annotation>>()
        stack.add(this@Doc)

        while (stack.isNotEmpty()) {
            val doc = stack.removeAt(stack.size - 1)
            when (doc) {
                is Empty -> {}
                is Line -> yield(DocOp.Line(0))
                is FlatAlt -> {
                    stack.add(doc.doc)
                }
                is Text -> {
                    yield(DocOp.Text(doc.text))
                    col += doc.text.length
                }
                is Nest -> {
                    stack.add(doc.doc)
                }
                is Annotated -> {
                    yield(DocOp.PushAnnotation(doc.annotation))
                    stack.add(doc.doc)
                    yield(DocOp.PopAnnotation)
                }
                is Concat -> {
                    for (subdoc in doc.docs.reversed()) {
                        stack.add(subdoc)
                    }
                }
                is Union -> {
                    stack.add(doc.left)
                }
            }
        }
    }

    fun layoutCompact(): DocOps<Annotation> = DocOps(LazyList.from(layoutCompactIter()))

    //-- | This is the default layout algorithm, and it is used by 'show', 'putDoc'
    //-- and 'hPutDoc'.
    //--
    //-- @'layoutPretty'@ commits to rendering something in a certain way if the
    //-- remainder of the current line fits the layout constraints; in other words,
    //-- it has up to one line of lookahead when rendering. Consider using the
    //-- smarter, but a bit less performant, @'layoutSmart'@ algorithm if the results
    //-- seem to run off to the right before having lots of line breaks.
    // layoutPretty
    //    :: LayoutOptions
    //    -> Doc ann
    //    -> SimpleDocStream ann
    //layoutPretty (LayoutOptions pageWidth_@(AvailablePerLine lineLength ribbonFraction)) =
    //    layoutWadlerLeijen
    //        (FittingPredicate
    //             (\lineIndent currentColumn _initialIndentY sdoc ->
    //                 fits
    //                     (remainingWidth lineLength ribbonFraction lineIndent currentColumn)
    //                     sdoc))
    //        pageWidth_
    //  where
    //    fits :: Int -- ^ Width in which to fit the first line
    //         -> SimpleDocStream ann
    //         -> Bool
    //    fits w _ | w < 0      = False
    //    fits _ SFail          = False
    //    fits _ SEmpty         = True
    //    fits w (SChar _ x)    = fits (w - 1) x
    //    fits w (SText l _t x) = fits (w - l) x
    //    fits _ SLine{}        = True
    //    fits w (SAnnPush _ x) = fits w x
    //    fits w (SAnnPop x)    = fits w x
//    fun layoutPretty(pageWidth: PageWidth): DocOps<Annotation> {
//        when (pageWidth) {
//            is PageWidth.Unbounded -> return layoutUnbounded()
//            is PageWidth.AvailablePerLine -> {
//                fun fits(w: Int, sdoc: DocOps<Annotation>): Boolean {
//                    var w: Int = w
//                    if (w < 0) return false
//                    for (op in sdoc.ops) {
//                        when (op) {
//                            is DocOp.Text -> w -= op.text.length
//                            is DocOp.Line -> return true
//                            is DocOp.PushAnnotation -> {}
//                            is DocOp.PopAnnotation -> {}
//                        }
//                        if (w < 0) return false
//                    }
//                    return true
//                }
//
//                val lineLength = pageWidth.lineLength
//                val ribbonFraction = pageWidth.ribbonFraction
//                val fittingPredicate: (Int, Int, Int, DocOps<Annotation>) -> Boolean = { lineIndent, currentColumn, _initialIndentY, sdoc ->
//                    fits(pageWidth.remainingWidth(lineIndent, currentColumn), sdoc)
//                }
//
//                return layoutWadlerLeijen(fittingPredicate, pageWidth)
//            }
//        }
//    }

    companion object {
        fun <A> annotate(annotation: A, doc: Doc<A>): Doc<A> =
            Annotated(annotation, doc)

        val empty: Doc<Nothing> = Empty
        fun <A> concat(docs: List<Doc<A>>): Doc<A> = Concat(docs)
        fun <A> concat(vararg docs: Doc<A>): Doc<A> =
            concat(docs.toList())

        fun text(value: String): Doc<Nothing> =
            if (value.isEmpty()) empty
            else vsep(value.split('\n').map { Text(it) })
        fun text(value: () -> String): Doc<Nothing> =
            text(value())

        inline fun stringBuilder(crossinline build: StringBuilder.() -> Unit): Doc<Nothing> =
            text(StringBuilder().apply(build).toString())

        fun viaToString(arg: Any?): Doc<Nothing> =
            Doc.text(arg.toString())

        fun viaToString(vararg args: Any?): Doc<Nothing> {
            val sb = StringBuilder()
            for (x in args) {
                sb.append(x)
            }
            return Doc.text(sb.toString())
        }

        fun punctuate(punct: Doc<Nothing>, docs: List<Doc<Nothing>>): List<Doc<Nothing>> {
            val result = mutableListOf<Doc<Nothing>>()
            for ((index, doc) in docs.withIndex()) {
                result.add(doc)
                if (index < docs.size - 1) {
                    result.add(punct)
                }
            }
            return result
        }

        fun <A> encloseSep(open: Doc<A>, close: Doc<A>, separator: Doc<A>, docs: List<Doc<A>>): Doc<A> {
            val result = mutableListOf<Doc<A>>()
            result.add(open)
            for ((index, doc) in docs.withIndex()) {
                result.add(doc)
                if (index < docs.size - 1) {
                    result.add(separator)
                }
            }
            result.add(close)
            return concat(result)
        }

        fun <A> encloseSep(open: String, close: String, separator: String, docs: List<Doc<A>>): Doc<A> =
            encloseSep(text(open), text(close), text(separator), docs)

        /**
         * `hsep` concatenates all documents `xs` horizontally by putting a space between all entries.
          */
        fun <A> hsep(docs: List<Doc<A>>): Doc<A> {
            val result = mutableListOf<Doc<A>>()
            for ((index, doc) in docs.withIndex()) {
                result.add(doc)
                if (index < docs.size - 1) {
                    result.add(Text(" "))
                }
            }
            return concat(result)
        }

        /**
         * `vsep` concatenates all documents `xs` above each other. If a `group`' undoes the line breaks inserted
         * by `vsep`, the documents are separated with a `space` instead.
          */
        fun <A> vsep(docs: List<Doc<A>>): Doc<A> {
            val result = mutableListOf<Doc<A>>()
            for ((index, doc) in docs.withIndex()) {
                result.add(doc)
                if (index < docs.size - 1) {
                    result.add(line)
                }
            }
            return concat(result)
        }
        fun <A> vsep(vararg docs: Doc<A>): Doc<A> =
            vsep(docs.toList())

        /**
         * `fillSep` concatenates the documents `xs` horizontally as long as it fits the page, then inserts a `line`
         * and continues doing that for all documents in `xs`. (`line` means that if `group`ed, the documents
         * are separated with a `space` instead of newlines. Use `fillCat` if you do not want a `space`.)
          */
        fun <A> fillSep(docs: List<Doc<A>>): Doc<A> {
            val result = mutableListOf<Doc<A>>()
            for ((index, doc) in docs.withIndex()) {
                result.add(doc)
                if (index < docs.size - 1) {
                    result.add(softline)
                }
            }
            return concat(result)
        }

        /**
         * `sep` tries laying out the documents `xs` separated with `space`s, and if this does not fit the page,
         * separates them with newlines. This is what differentiates it from `vsep`, which always lays out its
         * contents beneath each other.
          */
        fun <A> sep(docs: List<Doc<A>>): Doc<A> = group(vsep(docs))

        /**
         * Lays out the document x with the current nesting level (indentation of the following lines) increased by i.
         * Negative values are allowed, and decrease the nesting level accordingly.
         */
        fun <A> nest(indent: Int, doc: Doc<A>): Doc<A> =
            if (indent == 0) doc
            else Nest(indent, doc)

        fun <A> align(doc: Doc<A>): Doc<A> = TODO()

        fun <A> hang(indent: Int, doc: Doc<A>): Doc<A> = align(nest(indent, doc))

        /**
         * The line document advances to the next line and indents to the current nesting level.
         * `line` behaves like space if the line break is undone by `group`.
         */
        val line: Doc<Nothing> = FlatAlt(Line, Text(" "))
        val lineOrEmpty: Doc<Nothing> = FlatAlt(Line, Text(""))
        fun lineOr(space: String): Doc<Nothing> = FlatAlt(Line, Text(space))

        /**
         * softline behaves like space if the resulting output fits the page, otherwise like line.
         */
        val softline: Doc<Nothing> = Union(Text(" "), Line)
        val softlineOrEmpty: Doc<Nothing> = Union(Text(""), Line)
        fun softlineOr(space: String): Doc<Nothing> = Union(Text(space), Line)

        val hardline: Doc<Nothing> = Line

        /**
         * (group x) tries laying out x into a single line by removing the contained line breaks; if this does
         * not fit the page, or when a hardline within x prevents it from being flattened, x is laid out without
         * any changes.
          */
        //group :: Doc ann -> Doc ann
        //-- See note [Group: special flattening]
        //group x = case x of
        //    Union{} -> x
        //    FlatAlt a b -> case changesUponFlattening b of
        //        Flattened b' -> Union b' a
        //        AlreadyFlat  -> Union b a
        //        NeverFlat    -> a
        //    _ -> case changesUponFlattening x of
        //        Flattened x' -> Union x' x
        //        AlreadyFlat  -> x
        //        NeverFlat    -> x
        fun <Ann> group(doc: Doc<Ann>): Doc<Ann> = when (doc) {
            is Union -> doc
            is FlatAlt -> when (val changes = changesUponFlattening(doc.flat)) {
                is FlattenResult.Flattened -> Union(changes.value, doc.doc)
                is FlattenResult.AlreadyFlat -> Union(doc.flat, doc.doc)
                is FlattenResult.NeverFlat -> doc.doc
            }
            else -> when (val changes = changesUponFlattening(doc)) {
                is FlattenResult.Flattened -> Union(changes.value, doc)
                is FlattenResult.AlreadyFlat -> doc
                is FlattenResult.NeverFlat -> doc
            }
        }

        private sealed interface FlattenResult<out A> {
            data class Flattened<A>(val value: A) : FlattenResult<A>
            data object AlreadyFlat : FlattenResult<Nothing>
            data object NeverFlat : FlattenResult<Nothing>

            fun <B> map(f: (A) -> B): FlattenResult<B> = when (this) {
                is Flattened -> Flattened(f(value))
                is AlreadyFlat -> AlreadyFlat
                is NeverFlat -> NeverFlat
            }
        }

        //changesUponFlattening :: Doc ann -> FlattenResult (Doc ann)
        //changesUponFlattening = \doc -> case doc of
        //    FlatAlt _ y     -> Flattened (flatten y)
        //    Line            -> NeverFlat
        //    Union x _       -> Flattened x
        //    Nest i x        -> fmap (Nest i) (changesUponFlattening x)
        //    Annotated ann x -> fmap (Annotated ann) (changesUponFlattening x)
        //
        //    Column f        -> Flattened (Column (flatten . f))
        //    Nesting f       -> Flattened (Nesting (flatten . f))
        //    WithPageWidth f -> Flattened (WithPageWidth (flatten . f))
        //
        //    Cat x y -> case (changesUponFlattening x, changesUponFlattening y) of
        //        (NeverFlat    ,  _          ) -> NeverFlat
        //        (_            , NeverFlat   ) -> NeverFlat
        //        (Flattened x' , Flattened y') -> Flattened (Cat x' y')
        //        (Flattened x' , AlreadyFlat ) -> Flattened (Cat x' y)
        //        (AlreadyFlat  , Flattened y') -> Flattened (Cat x y')
        //        (AlreadyFlat  , AlreadyFlat ) -> AlreadyFlat
        //
        //    Empty  -> AlreadyFlat
        //    Char{} -> AlreadyFlat
        //    Text{} -> AlreadyFlat
        //    Fail   -> NeverFlat
        private fun <A> changesUponFlattening(doc: Doc<A>): FlattenResult<Doc<A>> {
            when (doc) {
                is FlatAlt -> return flatten(doc.flat)?.let { FlattenResult.Flattened(it) } ?: FlattenResult.NeverFlat
                is Line -> return FlattenResult.NeverFlat

                is Union -> return FlattenResult.Flattened(doc.left)
                is Nest -> return changesUponFlattening(doc.doc).map { Nest(doc.indent, it) }
                is Annotated -> return changesUponFlattening(doc.doc).map { Annotated(doc.annotation, it) }

//            is Column -> FlattenResult.Flattened(Column { col -> flatten(doc.f(col)) })
//            is Nesting -> FlattenResult.Flattened(Nesting { nesting -> flatten(doc.f(nesting)) })
//            is WithPageWidth -> FlattenResult.Flattened(WithPageWidth { pageWidth -> flatten(doc.f(pageWidth)) })

                is Concat -> {
                    val result = mutableListOf<Doc<A>>()

                    for (d in doc.docs) {
                        when (val flat = changesUponFlattening(d)) {
                            is FlattenResult.NeverFlat -> return FlattenResult.NeverFlat
                            is FlattenResult.AlreadyFlat -> result.add(d)
                            is FlattenResult.Flattened -> result.add(flat.value)
                        }
                    }

                    return FlattenResult.Flattened(concat(result))
                }

                is Empty -> return FlattenResult.AlreadyFlat
                is Text -> return FlattenResult.AlreadyFlat
            }
        }

        //    -- Flatten, but don’t report whether anything changes.
        //    flatten :: Doc ann -> Doc ann
        //    flatten = \doc -> case doc of
        //        FlatAlt _ y     -> flatten y
        //        Cat x y         -> Cat (flatten x) (flatten y)
        //        Nest i x        -> Nest i (flatten x)
        //        Line            -> Fail
        //        Union x _       -> flatten x
        //        Column f        -> Column (flatten . f)
        //        WithPageWidth f -> WithPageWidth (flatten . f)
        //        Nesting f       -> Nesting (flatten . f)
        //        Annotated ann x -> Annotated ann (flatten x)
        //
        //        x@Fail   -> x
        //        x@Empty  -> x
        //        x@Char{} -> x
        //        x@Text{} -> x
        private fun <A> flatten(doc: Doc<A>): Doc<A>? {
            when (doc) {
                is Line -> return null
                is Empty -> return doc
                is Text -> return doc
                is FlatAlt -> return flatten(doc.flat)
                is Concat -> {
                    val result = mutableListOf<Doc<A>>()
                    for (d in doc.docs) {
                        val flat = flatten(d)
                        if (flat != null) {
                            result.add(flat)
                        } else {
                            return null
                        }
                    }
                    return concat(result)
                }

                is Nest -> return flatten(doc.doc)?.let { Nest(doc.indent, it) }
                is Union -> return flatten(doc.left)
                is Annotated -> return flatten(doc.doc)?.let { Annotated(doc.annotation, it) }

//                is Column ->
//                    return Column { col -> flatten(doc.f(col)) }
//                is WithPageWidth ->
//                    return WithPageWidth { pageWidth -> flatten(doc.f(pageWidth)) }
//                is Nesting ->
//                    return Nesting { nesting -> flatten(doc.f(nesting)) }
            }
        }
    }

    interface FittingPredicate<A> {
        operator fun invoke(
            lineIndent: Int, currentColumn: Int,
            initialIndentY: Int,
            sdoc: DocOps<A>): Boolean
    }

//    //-- | The Wadler/Leijen layout algorithm
//    //layoutWadlerLeijen
//    //    :: forall ann. FittingPredicate ann
//    //    -> PageWidth
//    //    -> Doc ann
//    //    -> SimpleDocStream ann
//    fun <A> layoutWadlerLeijen(fits: FittingPredicate<A>, pageWidth: PageWidth, doc: Doc<A>): DocOps<A> {
//
//        //layoutWadlerLeijen
//        //    (FittingPredicate fits)
//        //    pageWidth_
//        //    doc
//        //  = best 0 0 (Cons 0 doc Nil)
//        //  where
//
//        //    -- Select the better fitting of two documents:
//        //    -- Choice A if it fits, otherwise choice B.
//        //    --
//        //    -- The fit of choice B is /not/ checked! It is ultimately the user's
//        //    -- responsibility to provide an alternative that can fit the page even when
//        //    -- choice A doesn't.
//        //    selectNicer
//        //        :: Int           -- ^ Current nesting level
//        //        -> Int           -- ^ Current column
//        //        -> SimpleDocStream ann -- ^ Choice A.
//        //        -> SimpleDocStream ann -- ^ Choice B. Should fit more easily
//        //                               --   (== be less wide) than choice A.
//        //        -> SimpleDocStream ann -- ^ Choice A if it fits, otherwise B.
//        //    selectNicer lineIndent currentColumn x y
//        //        | fits lineIndent currentColumn (initialIndentation y) x = x
//        //        | otherwise = y
//        fun selectNicer(
//            lineIndent: Int, currentColumn: Int,
//            x: DocOps<A>, y: DocOps<A>): DocOps<A> =
//            if (fits(lineIndent, currentColumn, initialIndentation(y), x)) x
//            else y
//
//        //    initialIndentation :: SimpleDocStream ann -> Maybe Int
//        //    initialIndentation sds = case sds of
//        //        SLine i _    -> Just i
//        //        SAnnPush _ s -> initialIndentation s
//        //        SAnnPop s    -> initialIndentation s
//        //        _            -> Nothing
//        fun initialIndentation(sds: DocOps<A>): Int? {
//            var sds = sds
//            while (true) {
//                when (sds) {
//                    is DocOps.Line -> return sds.spaces
//                    is DocOps.PushAnnotation -> sds = sds.ops
//                    is DocOps.PopAnnotation -> sds = sds.ops
//                    else -> return null
//                }
//            }
//        }
//
//        //    -- * current column >= current nesting level
//        //    -- * current column - current indentaion = number of chars inserted in line
//        //    best
//        //        :: Int -- Current nesting level
//        //        -> Int -- Current column, i.e. "where the cursor is"
//        //        -> LayoutPipeline ann -- Documents remaining to be handled (in order)
//        //        -> SimpleDocStream ann
//        //    best !_ !_ Nil           = SEmpty
//        //    best nl cc (UndoAnn ds)  = SAnnPop (best nl cc ds)
//        //    best nl cc (Cons i d ds) = case d of
//        //        Fail            -> SFail
//        //        Empty           -> best nl cc ds
//        //        Char c          -> let !cc' = cc+1 in SChar c (best nl cc' ds)
//        //        Text l t        -> let !cc' = cc+l in SText l t (best nl cc' ds)
//        //        Line            -> let x = best i i ds
//        //                               -- Don't produce indentation if there's no
//        //                               -- following text on the same line.
//        //                               -- This prevents trailing whitespace.
//        //                               i' = case x of
//        //                                   SEmpty  -> 0
//        //                                   SLine{} -> 0
//        //                                   _       -> i
//        //                           in SLine i' x
//        //        FlatAlt x _     -> best nl cc (Cons i x ds)
//        //        Cat x y         -> best nl cc (Cons i x (Cons i y ds))
//        //        Nest j x        -> let !ij = i+j in best nl cc (Cons ij x ds)
//        //        Union x y       -> let x' = best nl cc (Cons i x ds)
//        //                               y' = best nl cc (Cons i y ds)
//        //                           in selectNicer nl cc x' y'
//        //        Column f        -> best nl cc (Cons i (f cc) ds)
//        //        WithPageWidth f -> best nl cc (Cons i (f pageWidth_) ds)
//        //        Nesting f       -> best nl cc (Cons i (f i) ds)
//        //        Annotated ann x -> SAnnPush ann (best nl cc (Cons i x (UndoAnn ds)))
//        val result = mutableListOf<DocOp<A>>()
//        val stack = mutableListOf<Doc<A>>()
//        stack.add(doc)
//        var currentNestingLevel = 0
//        var currentColumn = 0
//
//        while (stack.isNotEmpty()) {
//            check(currentColumn >= currentNestingLevel)
//            val first = stack.removeLast()
//
//            when (first) {
//                is Doc.Empty -> {}
//                is Doc.Line -> {
//                    val x = best(currentNestingLevel, currentNestingLevel, queue)
//                    val i = when (x) {
//                        is DocOps.Empty -> 0
//                        is DocOps.Line -> 0
//                        else -> currentNestingLevel
//                    }
//                    result.add(DocOp.Line(i))
//                    queue.addFirst(x)
//                }
//                is Doc.Text -> {
//                    currentColumn += first.text.length
//                    result.add(DocOp.Text(first.text))
//                }
//                is Doc.Nest -> {
//                    val ij = currentNestingLevel + first.indent
//                    queue.addFirst(first.doc)
//                }
//                is Doc.Annotated -> {
//                    result.add(DocOp.PushAnnotation(first.annotation))
//                    queue.addFirst(first.doc)
//                    result.add(DocOp.PopAnnotation)
//                }
//                is Doc.Concat -> {
//                    for (d in first.docs.reversed()) {
//                        queue.addFirst(d)
//                    }
//                }
//                is Doc.Union -> {
//                    val x = best(currentNestingLevel, currentColumn, queue)
//                    val y = best(currentNestingLevel, currentColumn, queue)
//                    val nicer = selectNicer(currentNestingLevel, currentColumn, x, y)
//                    queue.addFirst(nicer)
//                }
//            }
//        }
}

operator fun <A> Doc<A>.plus(other: Doc<A>): Doc<A> = Doc.concat(this, other)
infix fun <A> Doc<A>.besides(other: Doc<A>): Doc<A> = Doc.concat(this, Doc.text(" "), other)
