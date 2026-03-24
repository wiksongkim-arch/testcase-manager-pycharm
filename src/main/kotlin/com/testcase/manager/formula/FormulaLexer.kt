package com.testcase.manager.formula

/**
 * 公式词法分析器
 * 将公式字符串转换为词法单元序列
 */
class FormulaLexer(private val input: String) {
    private var position = 0
    private val length = input.length

    /**
     * 获取下一个词法单元
     */
    fun nextToken(): FormulaToken {
        skipWhitespace()

        if (position >= length) {
            return FormulaToken.eof(position)
        }

        val startPos = position
        val char = input[position]

        return when {
            // 字符串字面量
            char == '"' || char == '\'' -> readString()

            // 数字
            char.isDigit() || (char == '.' && peekNext().isDigit()) -> readNumber()

            // 单元格引用 (A1, B2, etc.)
            char.isUpperCase() && peekNext().isDigit() -> readCellReference()

            // 标识符（函数名）
            char.isLetter() || char == '_' -> readIdentifier()

            // 运算符
            char == '+' -> {
                position++
                FormulaToken.operator(TokenType.PLUS, "+", startPos)
            }
            char == '-' -> {
                position++
                FormulaToken.operator(TokenType.MINUS, "-", startPos)
            }
            char == '*' -> {
                position++
                FormulaToken.operator(TokenType.MULTIPLY, "*", startPos)
            }
            char == '/' -> {
                position++
                FormulaToken.operator(TokenType.DIVIDE, "/", startPos)
            }
            char == '(' -> {
                position++
                FormulaToken(TokenType.LPAREN, "(", startPos)
            }
            char == ')' -> {
                position++
                FormulaToken(TokenType.RPAREN, ")", startPos)
            }
            char == ',' -> {
                position++
                FormulaToken(TokenType.COMMA, ",", startPos)
            }
            char == ';' -> {
                position++
                FormulaToken(TokenType.SEMICOLON, ";", startPos)
            }

            // 比较运算符
            char == '=' -> {
                position++
                if (peek() == '=') {
                    position++
                    FormulaToken.operator(TokenType.EQUALS, "==", startPos)
                } else {
                    FormulaToken.operator(TokenType.EQUALS, "=", startPos)
                }
            }
            char == '<' -> {
                position++
                when (peek()) {
                    '>' -> {
                        position++
                        FormulaToken.operator(TokenType.NOT_EQUALS, "<>", startPos)
                    }
                    '=' -> {
                        position++
                        FormulaToken.operator(TokenType.LESS_EQUAL, "<=", startPos)
                    }
                    else -> FormulaToken.operator(TokenType.LESS_THAN, "<", startPos)
                }
            }
            char == '>' -> {
                position++
                if (peek() == '=') {
                    position++
                    FormulaToken.operator(TokenType.GREATER_EQUAL, ">=", startPos)
                } else {
                    FormulaToken.operator(TokenType.GREATER_THAN, ">", startPos)
                }
            }

            else -> {
                position++
                FormulaToken.error("Unexpected character: $char", startPos)
            }
        }
    }

    /**
     * 读取字符串字面量
     */
    private fun readString(): FormulaToken {
        val startPos = position
        val quote = input[position]
        position++
        val builder = StringBuilder()

        while (position < length && input[position] != quote) {
            if (input[position] == '\\' && position + 1 < length) {
                position++
                when (input[position]) {
                    'n' -> builder.append('\n')
                    't' -> builder.append('\t')
                    'r' -> builder.append('\r')
                    '\\' -> builder.append('\\')
                    '"' -> builder.append('"')
                    '\'' -> builder.append('\'')
                    else -> builder.append(input[position])
                }
            } else {
                builder.append(input[position])
            }
            position++
        }

        if (position < length) {
            position++ // 跳过结束引号
        }

        return FormulaToken.string(builder.toString(), startPos)
    }

    /**
     * 读取数字
     */
    private fun readNumber(): FormulaToken {
        val startPos = position
        val builder = StringBuilder()

        while (position < length && (input[position].isDigit() || input[position] == '.')) {
            builder.append(input[position])
            position++
        }

        return FormulaToken.number(builder.toString(), startPos)
    }

    /**
     * 读取单元格引用
     */
    private fun readCellReference(): FormulaToken {
        val startPos = position
        val builder = StringBuilder()

        // 读取列字母
        while (position < length && input[position].isUpperCase()) {
            builder.append(input[position])
            position++
        }

        // 读取行数字
        while (position < length && input[position].isDigit()) {
            builder.append(input[position])
            position++
        }

        val cellRef = builder.toString()

        // 检查是否是范围引用 (A1:B10)
        if (position < length && input[position] == ':') {
            position++
            val rangeBuilder = StringBuilder(cellRef)
            rangeBuilder.append(':')

            // 读取范围结束
            while (position < length && input[position].isUpperCase()) {
                rangeBuilder.append(input[position])
                position++
            }
            while (position < length && input[position].isDigit()) {
                rangeBuilder.append(input[position])
                position++
            }

            return FormulaToken.rangeRef(rangeBuilder.toString(), startPos)
        }

        return FormulaToken.cellRef(cellRef, startPos)
    }

    /**
     * 读取标识符（函数名）
     */
    private fun readIdentifier(): FormulaToken {
        val startPos = position
        val builder = StringBuilder()

        while (position < length && (input[position].isLetterOrDigit() || input[position] == '_')) {
            builder.append(input[position])
            position++
        }

        val value = builder.toString()

        // 检查是否是布尔值
        return when (value.uppercase()) {
            "TRUE" -> FormulaToken.boolean(true, startPos)
            "FALSE" -> FormulaToken.boolean(false, startPos)
            else -> FormulaToken.function(value, startPos)
        }
    }

    /**
     * 跳过空白字符
     */
    private fun skipWhitespace() {
        while (position < length && input[position].isWhitespace()) {
            position++
        }
    }

    /**
     * 查看当前字符
     */
    private fun peek(): Char {
        return if (position < length) input[position] else '\u0000'
    }

    /**
     * 查看下一个字符
     */
    private fun peekNext(): Char {
        return if (position + 1 < length) input[position + 1] else '\u0000'
    }

    /**
     * 获取所有词法单元
     */
    fun tokenize(): List<FormulaToken> {
        val tokens = mutableListOf<FormulaToken>()
        var token = nextToken()
        while (token.type != TokenType.EOF && token.type != TokenType.ERROR) {
            tokens.add(token)
            token = nextToken()
        }
        if (token.type == TokenType.ERROR) {
            tokens.add(token)
        }
        return tokens
    }
}
