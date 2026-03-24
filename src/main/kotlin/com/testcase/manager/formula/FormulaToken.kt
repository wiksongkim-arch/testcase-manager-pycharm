package com.testcase.manager.formula

/**
 * 公式词法单元类型
 */
enum class TokenType {
    // 字面量
    NUMBER,      // 数字
    STRING,      // 字符串
    BOOLEAN,     // 布尔值
    CELL_REF,    // 单元格引用 (A1, B2, etc.)
    RANGE_REF,   // 范围引用 (A1:B10)

    // 函数
    FUNCTION,    // 函数名

    // 运算符
    PLUS,        // +
    MINUS,       // -
    MULTIPLY,    // *
    DIVIDE,      // /
    EQUALS,      // =
    NOT_EQUALS,  // <>
    LESS_THAN,   // <
    GREATER_THAN,// >
    LESS_EQUAL,  // <=
    GREATER_EQUAL,// >=

    // 标点
    LPAREN,      // (
    RPAREN,      // )
    COMMA,       // ,
    SEMICOLON,   // ;

    // 特殊
    EOF,         // 结束
    ERROR        // 错误
}

/**
 * 公式词法单元
 */
data class FormulaToken(
    val type: TokenType,
    val value: String,
    val position: Int = 0
) {
    companion object {
        fun number(value: String, pos: Int = 0) = FormulaToken(TokenType.NUMBER, value, pos)
        fun string(value: String, pos: Int = 0) = FormulaToken(TokenType.STRING, value, pos)
        fun boolean(value: Boolean, pos: Int = 0) = FormulaToken(TokenType.BOOLEAN, value.toString(), pos)
        fun cellRef(value: String, pos: Int = 0) = FormulaToken(TokenType.CELL_REF, value, pos)
        fun rangeRef(value: String, pos: Int = 0) = FormulaToken(TokenType.RANGE_REF, value, pos)
        fun function(value: String, pos: Int = 0) = FormulaToken(TokenType.FUNCTION, value.uppercase(), pos)
        fun operator(type: TokenType, value: String, pos: Int = 0) = FormulaToken(type, value, pos)
        fun eof(pos: Int = 0) = FormulaToken(TokenType.EOF, "", pos)
        fun error(message: String, pos: Int = 0) = FormulaToken(TokenType.ERROR, message, pos)
    }
}
