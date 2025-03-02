//package one.wabbit.doc
//
//import one.wabbit.random.gen.Gen
//import one.wabbit.random.gen.foreach
//import kotlin.test.Test
//import kotlin.test.assertEquals
//
//class DocSpec {
//    val genString = Gen.string(
//        Gen.int(1 .. 20),
//        Gen.oneOf(listOf('a', 'b', 'c', 'd', 'e', ' ')))
//
//    fun <Ann> genDoc(annGen: Gen<Ann>) = Gen.recursive<Doc<Ann>> { genDoc ->
//        Gen.oneOfGen(
//            Gen.const(Doc.empty),
//            Gen.const(Doc.line),
//            Gen.map(genDoc, annGen) { doc, ann -> Doc.annotate(ann, doc) },
//            Gen.map(genString) { Doc.text(it) },
//            Gen.map(genDoc, genDoc)  { doc1, doc2 -> Doc.concat(doc1, doc2) },
//            Gen.map(genDoc, Gen.int(0 .. 10)) { doc, i -> Doc.nest(i, doc) },
//            //Gen.map(genDoc)  { Doc.group(it) },
//        )
//    }
//
//    @Test fun textHomomorphism() {
//        // text "" = empty
//        // text (s ++ t) = text s <> text t
//
//        assertEquals(Doc.empty, Doc.text(""))
//
//        genString.zip(genString).foreach {
//            val (s, t) = it
//            val doc1 = Doc.text(s)
//            val doc2 = Doc.text(t)
//            assertEquals(Doc.text(s + t), doc1 + doc2)
//        }
//    }
//
////    @Test fun nestHomomorphism() {
////        // nest 0 x = x
////        // nest (i + j) x = nest i (nest j x)
////
////        Gen.foreach(genDoc, Gen.int(0 .. 10), Gen.int(0 .. 10)) { x, i, j ->
////            assertEquals(x, Doc.nest(0, x))
////            assertEquals(Doc.nest(i + j, x), Doc.nest(i, Doc.nest(j, x)))
////        }
////    }
//
//    // nest i (x <> y) = nest i x <> nest i y
//    // nest i empty = empty
//    // nest i (text s) = text s
//
//    // (x <> y) <> z = x <> (y <> z)
//    // (x $$ y) $$ z = x $$ (y $$ z)
//    //
//    // x <> text "" = x
//    //
//    // nest k (x $$ y) = nest k x $$ nest k y
//    // nest k (x <> y) = nest k x <> y
//    // x <> nest k y = x <> y
//    // nest k (nest k' x) = nest (k + k') x
//    // nest 0 x = x
//    //
//    // (x $$ y) <> z = x $$ (y <> z)
//    // text s <> ((text "" <> y) $$ z) = (text s <> y) $$ nest (length s) z
//    // text s <> text t = text (s ++ t)
//
//    @Test fun test() {
//        fun prettySig(name: String, types: List<String>, resultType: String): Doc<Nothing> =
//            Doc.sep(
//                listOf(Doc.text(name) besides Doc.text("::")) +
//                types.map { Doc.text(it) besides Doc.text("->")  } +
//                listOf(Doc.text(resultType)))
//
//        println(prettySig("f", listOf("Int", "Int"), "IO ()").layoutCompact())
//    }
//}
