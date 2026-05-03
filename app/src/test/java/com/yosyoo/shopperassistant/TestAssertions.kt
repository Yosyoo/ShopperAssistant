package com.yosyoo.shopperassistant

fun expectEquals(
    expected: Any?,
    actual: Any?,
) {
    if (expected != actual) {
        throw AssertionError("Expected <$expected>, actual <$actual>.")
    }
}

fun expectTrue(value: Boolean) {
    if (!value) {
        throw AssertionError("Expected true.")
    }
}

fun expectFalse(value: Boolean) {
    if (value) {
        throw AssertionError("Expected false.")
    }
}

inline fun <reified T : Throwable> expectThrows(block: () -> Unit): T {
    try {
        block()
    } catch (throwable: Throwable) {
        if (throwable is T) {
            return throwable
        }
        throw AssertionError("Expected ${T::class.java.simpleName}, actual ${throwable::class.java.simpleName}.")
    }
    throw AssertionError("Expected ${T::class.java.simpleName} to be thrown.")
}
