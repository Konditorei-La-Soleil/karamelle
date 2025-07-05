package moe.lasoleil.karamelle

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private val intComparator = Comparator.naturalOrder<Int>()

class UtilsTest {

    @Test
    fun `insertAt should add element at specified index`() {
        val original = arrayOf("a", "b", "d", "e")
        val expected = arrayOf("a", "b", "c", "d", "e")

        val result = original.insertAt(2, "c")

        assertEquals(expected.toList(), result.toList())
    }

    @Test
    fun `insertAt should work at beginning of array`() {
        val original = arrayOf(2, 3, 4)
        val expected = arrayOf(1, 2, 3, 4)

        val result = original.insertAt(0, 1)

        assertEquals(expected.toList(), result.toList())
    }

    @Test
    fun `insertAt should work at end of array`() {
        val original = arrayOf(1.1, 2.2, 3.3)
        val expected = arrayOf(1.1, 2.2, 3.3, 4.4)

        val result = original.insertAt(original.size, 4.4)

        assertEquals(expected.toList(), result.toList())
    }

    @Test
    fun `insertAt should work with empty array`() {
        val original = emptyArray<String>()
        val expected = arrayOf("single")

        val result = original.insertAt(0, "single")

        assertEquals(expected.toList(), result.toList())
    }

    @Test
    fun `insertAt should throw when index is negative`() {
        val original = arrayOf(1, 2, 3)

        assertThrows<IndexOutOfBoundsException> {
            original.insertAt(-1, 0)
        }
    }

    @Test
    fun `insertAt should throw when index is greater than size`() {
        val original = arrayOf(1, 2, 3)

        assertThrows<IndexOutOfBoundsException> {
            original.insertAt(original.size + 1, 4)
        }
    }

    @Test
    fun `insertAt should maintain array type`() {
        val original = arrayOf<Number>(1, 2, 3)
        val expected = arrayOf<Number>(1, 2, 2.5, 3)

        val result = original.insertAt(2, 2.5)

        assertEquals(expected.toList(), result.toList())
        result.forEach { assertIs<Number>(it) }
    }

    @Test
    fun `removeAt should remove element at specified index`() {
        // Arrange
        val original = arrayOf("a", "b", "c", "d")
        val expected = arrayOf("a", "b", "d")

        // Act
        val result = original.removeAt(2)

        // Assert
        assertEquals(expected.size, result.size)
        assertEquals(expected.joinToString(), result.joinToString())
    }

    @Test
    fun `removeAt should handle first element removal`() {
        // Arrange
        val original = arrayOf(1, 2, 3, 4)
        val expected = arrayOf(2, 3, 4)

        // Act
        val result = original.removeAt(0)

        // Assert
        assertEquals(expected.size, result.size)
        assertEquals(expected.joinToString(), result.joinToString())
    }

    @Test
    fun `removeAt should handle last element removal`() {
        // Arrange
        val original = arrayOf(1.1, 2.2, 3.3)
        val expected = arrayOf(1.1, 2.2)

        // Act
        val result = original.removeAt(2)

        // Assert
        assertEquals(expected.size, result.size)
        assertEquals(expected.joinToString(), result.joinToString())
    }

    @Test
    fun `removeAt should throw when index is out of bounds`() {
        // Arrange
        val array = arrayOf("x", "y", "z")

        // Act & Assert
        assertThrows<IndexOutOfBoundsException> {
            array.removeAt(-1)
        }
        assertThrows<IndexOutOfBoundsException> {
            array.removeAt(3)
        }
    }

    @Test
    fun `removeAt should return empty array when removing from single-element array`() {
        // Arrange
        val original = arrayOf(true)

        // Act
        val result = original.removeAt(0)

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun `removeAt should work with custom types`() {
        // Arrange
        data class Person(val name: String)
        val original = arrayOf(Person("Alice"), Person("Bob"), Person("Charlie"))
        val expected = arrayOf(Person("Alice"), Person("Charlie"))

        // Act
        val result = original.removeAt(1)

        // Assert
        assertEquals(expected.size, result.size)
        assertEquals(expected.map { it.name }, result.map { it.name })
    }

    @Test
    fun `find exact match at central index`() {
        val array = arrayOf(1, 2, 3, 4, 5)
        val result = array.searchItem(2, 3, intComparator)
        assertEquals(2, result)
    }

    @Test
    fun `find first occurrence when multiple matches exist`() {
        val array = arrayOf(1, 2, 2, 2, 3, 4)
        val result = array.searchItem(2, 2, intComparator)
        assertEquals(2, result)
    }

    @Test
    fun `find item on central index not matches comparator`() {
        val array = arrayOf(1, 2, 3, 4, 5)
        val result = array.searchItem(3, 2, intComparator)
        assertEquals(-1, result)
    }

    @Test
    fun `handle empty array`() {
        val array = emptyArray<Int>()
        assertThrows<ArrayIndexOutOfBoundsException> {
            array.searchItem(0, 1, intComparator)
        }
    }

    @Test
    fun `handle custom comparator with equal comparison but different objects`() {
        val customComparator = Comparator<String> { a, b -> a.length.compareTo(b.length) }
        val array = arrayOf("a", "bb", "bb", "ccc")

        // Search for "dd" which has same length as "bb" but isn't equal
        val result1 = array.searchItem(1, "dd", customComparator)
        assertEquals(-1, result1)

        // Search for "bb" which exists
        val result2 = array.searchItem(1, "bb", customComparator)
        assertEquals(1, result2)
    }

    @Test
    fun `handle all elements equal by comparator but not by equals`() {
        data class Wrapper(val value: Int)

        val wrapperComparator = Comparator<Wrapper> { a, b -> 0 } // Always equal
        val array = arrayOf(Wrapper(1), Wrapper(2), Wrapper(3))

        // Searching for Wrapper(2) - should only match the exact instance
        val result = array.searchItem(1, Wrapper(2), wrapperComparator)
        assertEquals(1, result)

        // Searching for Wrapper(4) - same comparator result but not in array
        val notFound = array.searchItem(1, Wrapper(4), wrapperComparator)
        assertEquals(-1, notFound)
    }

    @Test
    fun `throw exception when central index is out of bounds`() {
        val array = arrayOf(1, 2, 3)
        assertThrows<ArrayIndexOutOfBoundsException> {
            array.searchItem(5, 2, intComparator)
        }
    }

}