package com.testcase.manager.formula

/**
 * 公式抽象语法树节点类型
 */
sealed class FormulaNode {
    abstract fun evaluate(context: FormulaContext): FormulaValue
}

/**
 * 数值节点
 */
data class NumberNode(val value: Double) : FormulaNode() {
    override fun evaluate(context: FormulaContext): FormulaValue = FormulaValue.Number(value)
}

/**
 * 字符串节点
 */
data class StringNode(val value: String) : FormulaNode() {
    override fun evaluate(context: FormulaContext): FormulaValue = FormulaValue.String(value)
}

/**
 * 布尔节点
 */
data class BooleanNode(val value: Boolean) : FormulaNode() {
    override fun evaluate(context: FormulaContext): FormulaValue = FormulaValue.Boolean(value)
}

/**
 * 单元格引用节点
 */
data class CellRefNode(val column: String, val row: Int) : FormulaNode() {
    override fun evaluate(context: FormulaContext): FormulaValue {
        return context.getCellValue(column, row)
    }
}

/**
 * 范围引用节点
 */
data class RangeRefNode(val startCol: String, val startRow: Int, val endCol: String, val endRow: Int) : FormulaNode() {
    override fun evaluate(context: FormulaContext): FormulaValue {
        val values = mutableListOf<FormulaValue>()
        val startColIndex = columnToIndex(startCol)
        val endColIndex = columnToIndex(endCol)

        for (row in startRow..endRow) {
            for (colIndex in startColIndex..endColIndex) {
                val col = indexToColumn(colIndex)
                values.add(context.getCellValue(col, row))
            }
        }
        return FormulaValue.List(values)
    }

    private fun columnToIndex(col: String): Int {
        var result = 0
        for (char in col) {
            result = result * 26 + (char - 'A' + 1)
        }
        return result
    }

    private fun indexToColumn(index: Int): String {
        var n = index
        val result = StringBuilder()
        while (n > 0) {
            n--
            result.insert(0, 'A' + (n % 26))
            n /= 26
        }
        return result.toString()
    }
}

/**
 * 函数调用节点
 */
data class FunctionCallNode(
    val name: String,
    val arguments: List<FormulaNode>
) : FormulaNode() {
    override fun evaluate(context: FormulaContext): FormulaValue {
        val args = arguments.map { it.evaluate(context) }
        return context.callFunction(name, args)
    }
}

/**
 * 二元运算节点
 */
data class BinaryOpNode(
    val operator: TokenType,
    val left: FormulaNode,
    val right: FormulaNode
) : FormulaNode() {
    override fun evaluate(context: FormulaContext): FormulaValue {
        val leftValue = left.evaluate(context)
        val rightValue = right.evaluate(context)

        return when (operator) {
            TokenType.PLUS -> leftValue.add(rightValue)
            TokenType.MINUS -> leftValue.subtract(rightValue)
            TokenType.MULTIPLY -> leftValue.multiply(rightValue)
            TokenType.DIVIDE -> leftValue.divide(rightValue)
            TokenType.EQUALS -> FormulaValue.Boolean(leftValue.compareTo(rightValue) == 0)
            TokenType.NOT_EQUALS -> FormulaValue.Boolean(leftValue.compareTo(rightValue) != 0)
            TokenType.LESS_THAN -> FormulaValue.Boolean(leftValue.compareTo(rightValue) < 0)
            TokenType.GREATER_THAN -> FormulaValue.Boolean(leftValue.compareTo(rightValue) > 0)
            TokenType.LESS_EQUAL -> FormulaValue.Boolean(leftValue.compareTo(rightValue) <= 0)
            TokenType.GREATER_EQUAL -> FormulaValue.Boolean(leftValue.compareTo(rightValue) >= 0)
            else -> FormulaValue.Error("Unknown operator: $operator")
        }
    }
}

/**
 * 一元运算节点
 */
data class UnaryOpNode(
    val operator: TokenType,
    val operand: FormulaNode
) : FormulaNode() {
    override fun evaluate(context: FormulaContext): FormulaValue {
        val value = operand.evaluate(context)
        return when (operator) {
            TokenType.MINUS -> when (value) {
                is FormulaValue.Number -> FormulaValue.Number(-value.value)
                else -> FormulaValue.Error("Cannot negate non-numeric value")
            }
            TokenType.PLUS -> value
            else -> FormulaValue.Error("Unknown unary operator: $operator")
        }
    }
}
