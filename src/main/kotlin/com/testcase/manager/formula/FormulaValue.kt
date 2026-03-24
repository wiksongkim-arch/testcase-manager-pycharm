package com.testcase.manager.formula

/**
 * 公式值类型
 * 用于表示公式计算结果的 sealed class
 */
sealed class FormulaValue {
    /**
     * 转换为数字
     */
    abstract fun toNumber(): Double

    /**
     * 转换为字符串（返回 Kotlin String）
     */
    abstract fun asString(): kotlin.String

    /**
     * 转换为布尔值（返回 Kotlin Boolean）
     */
    abstract fun asBoolean(): kotlin.Boolean

    /**
     * 数值类型
     */
    data class Number(val value: Double) : FormulaValue() {
        override fun toNumber(): Double = value
        override fun asString(): kotlin.String = value.toString()
        override fun asBoolean(): kotlin.Boolean = value != 0.0
    }

    /**
     * 字符串类型
     */
    data class Str(val value: kotlin.String) : FormulaValue() {
        override fun toNumber(): Double = value.toDoubleOrNull() ?: 0.0
        override fun asString(): kotlin.String = value
        override fun asBoolean(): kotlin.Boolean = value.isNotEmpty()
    }

    /**
     * 布尔类型
     */
    data class Bool(val value: kotlin.Boolean) : FormulaValue() {
        override fun toNumber(): Double = if (value) 1.0 else 0.0
        override fun asString(): kotlin.String = value.toString()
        override fun asBoolean(): kotlin.Boolean = value
    }

    /**
     * 列表类型（用于范围引用）
     */
    data class List(val values: kotlin.collections.List<FormulaValue>) : FormulaValue() {
        override fun toNumber(): Double = values.firstOrNull()?.toNumber() ?: 0.0
        override fun asString(): kotlin.String = values.joinToString(", ") { it.asString() }
        override fun asBoolean(): kotlin.Boolean = values.isNotEmpty()
    }

    /**
     * 错误类型
     */
    data class Error(val message: kotlin.String) : FormulaValue() {
        override fun toNumber(): Double = Double.NaN
        override fun asString(): kotlin.String = "#${message}"
        override fun asBoolean(): kotlin.Boolean = false
    }

    /**
     * 空值
     */
    object Empty : FormulaValue() {
        override fun toNumber(): Double = 0.0
        override fun asString(): kotlin.String = ""
        override fun asBoolean(): kotlin.Boolean = false
    }

    /**
     * 加法运算
     */
    fun add(other: FormulaValue): FormulaValue {
        return when {
            this is Number && other is Number -> Number(this.value + other.value)
            this is Str || other is Str -> Str(this.asString() + other.asString())
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
            this is Str && other is Str -> this.value.compareTo(other.value)
            this is Bool && other is Bool -> this.value.compareTo(other.value)
            else -> this.asString().compareTo(other.asString())
        }
    }
}
