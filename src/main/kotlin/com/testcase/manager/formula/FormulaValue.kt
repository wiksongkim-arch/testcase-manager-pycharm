package com.testcase.manager.formula

/**
 * 公式值类型
 */
sealed class FormulaValue {
    /**
     * 转换为数字
     */
    abstract fun toNumber(): Double

    /**
     * 转换为字符串
     */
    abstract fun toText(): String

    /**
     * 转换为布尔值
     */
    abstract fun toBoolean(): Boolean

    /**
     * 数值类型
     */
    data class Number(val value: Double) : FormulaValue() {
        override fun toNumber(): Double = value
        override fun toText(): String = value.toString()
        override fun toBoolean(): Boolean = value != 0.0
    }

    /**
     * 字符串类型
     */
    data class String(val value: kotlin.String) : FormulaValue() {
        override fun toNumber(): Double = value.toDoubleOrNull() ?: 0.0
        override fun toText(): kotlin.String = value
        override fun toBoolean(): Boolean = value.isNotEmpty()
    }

    /**
     * 布尔类型
     */
    data class Boolean(val value: kotlin.Boolean) : FormulaValue() {
        override fun toNumber(): Double = if (value) 1.0 else 0.0
        override fun toText(): kotlin.String = value.toString()
        override fun toBoolean(): kotlin.Boolean = value
    }

    /**
     * 列表类型（用于范围引用）
     */
    data class List(val values: kotlin.collections.List<FormulaValue>) : FormulaValue() {
        override fun toNumber(): Double = values.firstOrNull()?.toNumber() ?: 0.0
        override fun toText(): kotlin.String = values.joinToString(", ") { it.toText() }
        override fun toBoolean(): kotlin.Boolean = values.isNotEmpty()
    }

    /**
     * 错误类型
     */
    data class Error(val message: kotlin.String) : FormulaValue() {
        override fun toNumber(): Double = Double.NaN
        override fun toText(): kotlin.String = "#$message"
        override fun toBoolean(): kotlin.Boolean = false
    }

    /**
     * 空值
     */
    object Empty : FormulaValue() {
        override fun toNumber(): Double = 0.0
        override fun toText(): kotlin.String = ""
        override fun toBoolean(): kotlin.Boolean = false
    }

    /**
     * 加法运算
     */
    fun add(other: FormulaValue): FormulaValue {
        return when {
            this is Number && other is Number -> Number(this.value + other.value)
            this is String || other is String -> String(this.toText() + other.toText())
            else -> Error("Cannot add ${this::class.simpleName} and ${other::class.simpleName}")
        }
    }

    /**
     * 减法运算
     */
    fun subtract(other: FormulaValue): FormulaValue {
        return when {
            this is Number && other is Number -> Number(this.value - other.value)
            else -> Error("Cannot subtract ${other::class.simpleName} from ${this::class.simpleName}")
        }
    }

    /**
     * 乘法运算
     */
    fun multiply(other: FormulaValue): FormulaValue {
        return when {
            this is Number && other is Number -> Number(this.value * other.value)
            else -> Error("Cannot multiply ${this::class.simpleName} and ${other::class.simpleName}")
        }
    }

    /**
     * 除法运算
     */
    fun divide(other: FormulaValue): FormulaValue {
        return when {
            this is Number && other is Number -> {
                if (other.value == 0.0) {
                    Error("Division by zero")
                } else {
                    Number(this.value / other.value)
                }
            }
            else -> Error("Cannot divide ${this::class.simpleName} by ${other::class.simpleName}")
        }
    }

    /**
     * 比较运算
     */
    fun compareTo(other: FormulaValue): Int {
        return when {
            this is Number && other is Number -> this.value.compareTo(other.value)
            this is String && other is String -> this.value.compareTo(other.value)
            this is Boolean && other is Boolean -> this.value.compareTo(other.value)
            else -> this.toText().compareTo(other.toText())
        }
    }
}
