package com.testcase.manager.formula

/**
 * 公式解析器
 * 将词法单元序列解析为抽象语法树
 */
class FormulaParser(private val tokens: List<FormulaToken>) {
    private var position = 0

    /**
     * 解析公式
     */
    fun parse(): FormulaNode {
        if (tokens.isEmpty() || tokens.first().type == TokenType.EOF) {
            return StringNode("")
        }
        return parseExpression()
    }

    /**
     * 解析表达式（最低优先级）
     */
    private fun parseExpression(): FormulaNode {
        return parseComparison()
    }

    /**
     * 解析比较表达式
     */
    private fun parseComparison(): FormulaNode {
        var left = parseAdditive()

        while (true) {
            val token = peek()
            when (token.type) {
                TokenType.EQUALS,
                TokenType.NOT_EQUALS,
                TokenType.LESS_THAN,
                TokenType.GREATER_THAN,
                TokenType.LESS_EQUAL,
                TokenType.GREATER_EQUAL -> {
                    consume()
                    val right = parseAdditive()
                    left = BinaryOpNode(token.type, left, right)
                }
                else -> break
            }
        }

        return left
    }

    /**
     * 解析加减表达式
     */
    private fun parseAdditive(): FormulaNode {
        var left = parseMultiplicative()

        while (true) {
            val token = peek()
            when (token.type) {
                TokenType.PLUS, TokenType.MINUS -> {
                    consume()
                    val right = parseMultiplicative()
                    left = BinaryOpNode(token.type, left, right)
                }
                else -> break
            }
        }

        return left
    }

    /**
     * 解析乘除表达式
     */
    private fun parseMultiplicative(): FormulaNode {
        var left = parseUnary()

        while (true) {
            val token = peek()
            when (token.type) {
                TokenType.MULTIPLY, TokenType.DIVIDE -> {
                    consume()
                    val right = parseUnary()
                    left = BinaryOpNode(token.type, left, right)
                }
                else -> break
            }
        }

        return left
    }

    /**
     * 解析一元表达式
     */
    private fun parseUnary(): FormulaNode {
        val token = peek()
        return when (token.type) {
            TokenType.MINUS -> {
                consume()
                UnaryOpNode(TokenType.MINUS, parsePrimary())
            }
            TokenType.PLUS -> {
                consume()
                UnaryOpNode(TokenType.PLUS, parsePrimary())
            }
            else -> parsePrimary()
        }
    }

    /**
     * 解析基本表达式
     */
    private fun parsePrimary(): FormulaNode {
        val token = peek()

        return when (token.type) {
            TokenType.NUMBER -> {
                consume()
                NumberNode(token.value.toDouble())
            }

            TokenType.STRING -> {
                consume()
                StringNode(token.value)
            }

            TokenType.BOOLEAN -> {
                consume()
                BooleanNode(token.value.toBoolean())
            }

            TokenType.CELL_REF -> {
                consume()
                parseCellRef(token.value)
            }

            TokenType.RANGE_REF -> {
                consume()
                parseRangeRef(token.value)
            }

            TokenType.FUNCTION -> {
                consume()
                parseFunctionCall(token.value)
            }

            TokenType.LPAREN -> {
                consume()
                val expr = parseExpression()
                expect(TokenType.RPAREN)
                expr
            }

            else -> {
                consume()
                StringNode("")
            }
        }
    }

    /**
     * 解析单元格引用
     */
    private fun parseCellRef(ref: String): CellRefNode {
        val (col, row) = parseCellReference(ref)
        return CellRefNode(col, row)
    }

    /**
     * 解析范围引用
     */
    private fun parseRangeRef(ref: String): RangeRefNode {
        val parts = ref.split(":")
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid range reference: $ref")
        }
        val (startCol, startRow) = parseCellReference(parts[0])
        val (endCol, endRow) = parseCellReference(parts[1])
        return RangeRefNode(startCol, startRow, endCol, endRow)
    }

    /**
     * 解析单元格引用字符串
     */
    private fun parseCellReference(ref: String): Pair<String, Int> {
        val colBuilder = StringBuilder()
        val rowBuilder = StringBuilder()

        for (char in ref) {
            when {
                char.isUpperCase() -> colBuilder.append(char)
                char.isDigit() -> rowBuilder.append(char)
            }
        }

        val col = colBuilder.toString()
        val row = rowBuilder.toString().toIntOrNull() ?: 1

        return col to row
    }

    /**
     * 解析函数调用
     */
    private fun parseFunctionCall(name: String): FunctionCallNode {
        expect(TokenType.LPAREN)

        val arguments = mutableListOf<FormulaNode>()

        // 空参数列表
        if (peek().type == TokenType.RPAREN) {
            consume()
            return FunctionCallNode(name, arguments)
        }

        // 解析参数
        while (true) {
            arguments.add(parseExpression())

            when (peek().type) {
                TokenType.COMMA, TokenType.SEMICOLON -> {
                    consume()
                    continue
                }
                TokenType.RPAREN -> {
                    consume()
                    break
                }
                else -> {
                    // 尝试继续解析
                    if (position >= tokens.size - 1) {
                        break
                    }
                    consume()
                }
            }
        }

        return FunctionCallNode(name, arguments)
    }

    /**
     * 查看当前词法单元
     */
    private fun peek(): FormulaToken {
        return if (position < tokens.size) tokens[position] else FormulaToken.eof()
    }

    /**
     * 消费当前词法单元
     */
    private fun consume(): FormulaToken {
        return if (position < tokens.size) tokens[position++] else FormulaToken.eof()
    }

    /**
     * 期望特定类型的词法单元
     */
    private fun expect(type: TokenType): FormulaToken {
        val token = peek()
        if (token.type != type) {
            throw IllegalArgumentException("Expected $type but found ${token.type}")
        }
        return consume()
    }
}
