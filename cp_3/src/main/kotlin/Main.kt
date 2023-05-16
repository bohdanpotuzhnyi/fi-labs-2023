import java.nio.charset.Charset

fun gcdExt(x: Int, y: Int): Triple<Int, Int, Int> {
    if (x == 0) {
        return Triple(y, 0, 1)
    }
    val (gcd, x1, y1) = gcdExt(y % x, x)
    val newX = y1 - (y / x) * x1
    return Triple(gcd, newX, x1)
}

fun gcd(x: Int, y: Int): Int {
    return gcdExt(x, y).first
}

fun inversed(x: Int, mod: Int): Int {
    return gcdExt(x, mod).second
}

fun solveModEq(a: Int, b: Int, m: Int): Set<Int>? {
    var M = m
    var d = gcd(a, m)
    if (b % d != 0) {
        return null
    }
    var aNew = a / d
    var bNew = b / d
    var mNew = m / d
    var x = (inversed(aNew, mNew) * bNew) % mNew
    val roots = mutableSetOf(x)
    while (true) {
        x = (x - mNew) % M
        if (x in roots) {
            break
        }
        roots.add(x)
    }
    return roots
}

fun countBigrams(text: List<Int>): MutableMap<Int, Int> {
    val frequencies = mutableMapOf<Int, Int>()
    for (bigram in text) {
        frequencies[bigram] = frequencies.getOrDefault(bigram, 0) + 1
    }
    return frequencies
}

fun getProbability(text: List<Int>): MutableMap<Int, Double> {
    val frequencies = countBigrams(text)
    val total = frequencies.values.sum()
    return frequencies.mapValues { it.value.toDouble() / total }.toMutableMap()
}

fun linearDecode(text: List<Int>, a: Int, b: Int, charset: String): String {
    val mod = charset.length * charset.length
    var result = ""
    for (y in text) {
        val xRaw = (inversed(a, mod) * (y - b)) % mod
        val x = if (xRaw < 0) xRaw + mod else xRaw
        val firstIndex = x / charset.length
        val secondIndex = x % charset.length
        val first = charset[firstIndex]
        val second = charset[secondIndex]
        result += "$first$second"
    }
    return result
}

fun convertToBigrams(text: String, charset: String): MutableList<Int> {
    val arr = mutableListOf<Int>()
    val mod = charset.length
    for (i in 0 until text.length - 1 step 2) {
        val first = charset.indexOf(text[i])
        val second = charset.indexOf(text[i + 1])
        val ind = first * mod + second
        arr.add(ind)
    }
    return arr
}

fun main() {
    val alphabet1 = "абвгдежзийклмнопрстуфхцчшщыьэюя"
    val alphabet2 = "абвгдежзийклмнопрстуфхцчшщьыэюя"

    val openMostFrequent = "стнотонаен"
    val impossibleBigrams = listOf("йй", "ыы", "ьь", "шш", "щщ", "ээ")

    val cypher =  {}.javaClass.getResource("/14.txt").readText(Charset.forName("UTF-8")).filter{it in alphabet1}

    for (charset in listOf(alphabet1, alphabet2)) {
        val bigrOpenMostFreq = convertToBigrams(openMostFrequent, charset)
        val bigrCyph = convertToBigrams(cypher, charset)
        val cyphBigrams = getProbability(bigrCyph)
        val cyphMostFrequent = cyphBigrams.keys.sortedByDescending { cyphBigrams[it] }.take(5)
        val mod = charset.length * charset.length

        val toSolve = mutableSetOf<Quadruple<Int, Int, Int, Int>>()
        val combinationsX = bigrOpenMostFreq.flatMap { x1 -> bigrOpenMostFreq.map { x2 -> Pair(x1, x2) } }.filterNot { it.first == it.second }
        val combinationsY = cyphMostFrequent.flatMap { y1 -> cyphMostFrequent.map { y2 -> Pair(y1, y2) } }.filterNot { it.first == it.second }
        for ((x1, x2) in combinationsX) {
            for ((y1, y2) in combinationsY) {
                toSolve.add(Quadruple((x1 - x2 + mod) % mod, (y1 - y2 + mod) % mod, x1, y1))
            }
        }

        val keys = mutableSetOf<Pair<Int, Int>>()
        for ((a, b, x1, y1) in toSolve) {
            val roots = solveModEq(a, b, mod)
            if (roots != null) {
                for (root in roots) {
                    val keyB = (y1 - (x1 * root)) % mod
                    keys.add(Pair(root, keyB))
                }
            }
        }

        val decoded = keys.map { key -> linearDecode(bigrCyph, key.first, key.second, charset) }

        val filtered = decoded.filterNot { dec -> impossibleBigrams.any { bigram -> dec.contains(bigram) } }

        val finalList = mutableListOf<String>()
        for (dec in filtered) {
            val decBigr = convertToBigrams(dec, charset)
            val decFreq = getProbability(decBigr)
            val decMostFrequent = decFreq.keys.sortedByDescending { decFreq[it] }.take(15)
            if (decMostFrequent.intersect(bigrOpenMostFreq.toSet()).size > 3) {
                finalList.add(dec)
            }
        }
        if(finalList.isNotEmpty()) {
            for (finalText in finalList) {
                val correspondingKey = keys.first { key -> linearDecode(bigrCyph, key.first, key.second, charset) == finalText }
                println("Charset: " + charset.lowercase())
                val resKey = Pair( (correspondingKey.first + mod) % mod, (correspondingKey.second + mod) % mod )
                println("Key for the following text is: $resKey")
                println(finalText.lowercase())
                break
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

