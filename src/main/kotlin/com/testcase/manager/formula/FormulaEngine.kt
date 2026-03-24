package com.testcase.manager.formula

/**
 * 公式引擎
 * 提供公式解析和计算功能
 */
class FormulaEngine {

    /**
     * 检查字符串是否是公式
     */
    fun isFormula(text: String): Boolean {
        return text.trimStart().startsWith("=")
    }

    /**
     * 解析并计算公式
     */
    fun evaluate(formula: String, context: FormulaContext): FormulaValue {
        if (!isFormula(formula)) {
            return FormulaValue.String(formula)
        }

        // 移除开头的等号
        val expression = formula.trimStart().substring(1).trim()

        return try {
            // 词法分析
            val lexer = FormulaLexer(expression)
            val tokens = lexer.tokenize()

            // 检查是否有错误
            val errorToken = tokens.find { it.type == TokenType.ERROR }
            if (errorToken != null) {
                return FormulaValue.Error("Lexical error: ${errorToken.value}")
            }

            // 语法分析
            val parser = FormulaParser(tokens)
            val ast = parser.parse()

            // 计算结果
            ast.evaluate(context)
        } catch (e: Exception) {
            FormulaValue.Error("Formula error: ${e.message}")
        }
    }

    /**
     * 快速计算公式（使用默认上下文）
     */
    fun evaluateSimple(formula: String): FormulaValue {
        val context = DefaultFormulaContext { _, _ -> null }
        return evaluate(formula, context)
    }

    /**
     * 验证公式语法
     */
    fun validate(formula: String): ValidationResult {
        if (!isFormula(formula)) {
            return ValidationResult(true, null)
        }

        val expression = formula.trimStart().substring(1).trim()

        return try {
            val lexer = FormulaLexer(expression)
            val tokens = lexer.tokenize()

            val errorToken = tokens.find { it.type == TokenType.ERROR }
            if (errorToken != null) {
                return ValidationResult(false, "Syntax error at position ${errorToken.position}: ${errorToken.value}")
            }

            val parser = FormulaParser(tokens)
            parser.parse()

            ValidationResult(true, null)
        } catch (e: Exception) {
            ValidationResult(false, e.message)
        }
    }

    /**
     * 获取公式中的单元格引用
     */
    fun getCellReferences(formula: String): List<String> {
        if (!isFormula(formula)) {
            return emptyList()
        }

        val expression = formula.trimStart().substring(1).trim()

        return try {
            val lexer = FormulaLexer(expression)
            val tokens = lexer.tokenize()
            tokens.filter { it.type == TokenType.CELL_REF || it.type == TokenType.RANGE_REF }
                .map { it.value }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取建议的函数列表
     */
    fun getFunctionSuggestions(prefix: String): List<FunctionInfo> {
        return BUILT_IN_FUNCTIONS.filter {
            it.name.startsWith(prefix, ignoreCase = true)
        }
    }

    /**
     * 获取所有可用函数
     */
    fun getAllFunctions(): List<FunctionInfo> {
        return BUILT_IN_FUNCTIONS.toList()
    }

    /**
     * 验证结果
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String?
    )

    /**
     * 函数信息
     */
    data class FunctionInfo(
        val name: String,
        val description: String,
        val syntax: String,
        val examples: List<String>,
        val category: FunctionCategory
    )

    /**
     * 函数分类
     */
    enum class FunctionCategory {
        MATH,
        TEXT,
        LOGICAL,
        STATISTICAL,
        INFORMATION
    }

    companion object {
        /**
         * 内置函数列表
         */
        val BUILT_IN_FUNCTIONS = listOf(
            // 数学函数
            FunctionInfo(
                "SUM",
                "计算所有参数的和",
                "SUM(number1, [number2], ...)",
                listOf("=SUM(A1:A10)", "=SUM(1, 2, 3, 4, 5)"),
                FunctionCategory.MATH
            ),
            FunctionInfo(
                "COUNT",
                "计算参数列表中数字的个数",
                "COUNT(value1, [value2], ...)",
                listOf("=COUNT(A1:A10)", "=COUNT(1, 2, \"text\", 3)"),
                FunctionCategory.STATISTICAL
            ),
            FunctionInfo(
                "AVERAGE",
                "返回参数的平均值",
                "AVERAGE(number1, [number2], ...)",
                listOf("=AVERAGE(A1:A10)", "=AVERAGE(10, 20, 30)"),
                FunctionCategory.STATISTICAL
            ),
            FunctionInfo(
                "MAX",
                "返回参数列表中的最大值",
                "MAX(number1, [number2], ...)",
                listOf("=MAX(A1:A10)", "=MAX(5, 10, 15)"),
                FunctionCategory.STATISTICAL
            ),
            FunctionInfo(
                "MIN",
                "返回参数列表中的最小值",
                "MIN(number1, [number2], ...)",
                listOf("=MIN(A1:A10)", "=MIN(5, 10, 15)"),
                FunctionCategory.STATISTICAL
            ),
            FunctionInfo(
                "ABS",
                "返回数字的绝对值",
                "ABS(number)",
                listOf("=ABS(-5)", "=ABS(A1)"),
                FunctionCategory.MATH
            ),
            FunctionInfo(
                "ROUND",
                "将数字四舍五入到指定的位数",
                "ROUND(number, num_digits)",
                listOf("=ROUND(3.14159, 2)", "=ROUND(A1, 0)"),
                FunctionCategory.MATH
            ),

            // 文本函数
            FunctionInfo(
                "CONCAT",
                "将多个文本字符串合并为一个",
                "CONCAT(text1, [text2], ...)",
                listOf("=CONCAT(\"Hello\", \" \", \"World\")", "=CONCAT(A1, B1)"),
                FunctionCategory.TEXT
            ),
            FunctionInfo(
                "CONCATENATE",
                "CONCAT 的别名",
                "CONCATENATE(text1, [text2], ...)",
                listOf("=CONCATENATE(\"Hello\", \" \", \"World\")"),
                FunctionCategory.TEXT
            ),
            FunctionInfo(
                "LEFT",
                "从文本字符串的第一个字符开始返回指定个数的字符",
                "LEFT(text, [num_chars])",
                listOf("=LEFT(\"Hello\", 2)", "=LEFT(A1, 3)"),
                FunctionCategory.TEXT
            ),
            FunctionInfo(
                "RIGHT",
                "从文本字符串的最后一个字符开始返回指定个数的字符",
                "RIGHT(text, [num_chars])",
                listOf("=RIGHT(\"Hello\", 2)", "=RIGHT(A1, 3)"),
                FunctionCategory.TEXT
            ),
            FunctionInfo(
                "MID",
                "从文本字符串的指定位置开始返回指定个数的字符",
                "MID(text, start_num, num_chars)",
                listOf("=MID(\"Hello World\", 7, 5)"),
                FunctionCategory.TEXT
            ),
            FunctionInfo(
                "LEN",
                "返回文本字符串中的字符个数",
                "LEN(text)",
                listOf("=LEN(\"Hello\")", "=LEN(A1)"),
                FunctionCategory.TEXT
            ),
            FunctionInfo(
                "TRIM",
                "删除文本中的多余空格",
                "TRIM(text)",
                listOf("=TRIM(\"  Hello  World  \")"),
                FunctionCategory.TEXT
            ),
            FunctionInfo(
                "UPPER",
                "将文本转换为大写",
                "UPPER(text)",
                listOf("=UPPER(\"hello\")"),
                FunctionCategory.TEXT
            ),
            FunctionInfo(
                "LOWER",
                "将文本转换为小写",
                "LOWER(text)",
                listOf("=LOWER(\"HELLO\")"),
                FunctionCategory.TEXT
            ),

            // 逻辑函数
            FunctionInfo(
                "IF",
                "根据条件返回不同的值",
                "IF(logical_test, value_if_true, [value_if_false])",
                listOf("=IF(A1>10, \"大\", \"小\")", "=IF(A1=B1, TRUE, FALSE)"),
                FunctionCategory.LOGICAL
            ),
            FunctionInfo(
                "AND",
                "检查所有条件是否都为 TRUE",
                "AND(logical1, [logical2], ...)",
                listOf("=AND(A1>0, A1<100)", "=AND(TRUE, TRUE, FALSE)"),
                FunctionCategory.LOGICAL
            ),
            FunctionInfo(
                "OR",
                "检查是否有任一条件为 TRUE",
                "OR(logical1, [logical2], ...)",
                listOf("=OR(A1<0, A1>100)", "=OR(FALSE, FALSE, TRUE)"),
                FunctionCategory.LOGICAL
            ),
            FunctionInfo(
                "NOT",
                "对逻辑值取反",
                "NOT(logical)",
                listOf("=NOT(TRUE)", "=NOT(A1>10)"),
                FunctionCategory.LOGICAL
            ),

            // 信息函数
            FunctionInfo(
                "ISBLANK",
                "检查单元格是否为空",
                "ISBLANK(value)",
                listOf("=ISBLANK(A1)"),
                FunctionCategory.INFORMATION
            ),
            FunctionInfo(
                "ISNUMBER",
                "检查值是否为数字",
                "ISNUMBER(value)",
                listOf("=ISNUMBER(A1)", "=ISNUMBER(123)"),
                FunctionCategory.INFORMATION
            ),
            FunctionInfo(
                "ISTEXT",
                "检查值是否为文本",
                "ISTEXT(value)",
                listOf("=ISTEXT(A1)", "=ISTEXT(\"hello\")"),
                FunctionCategory.INFORMATION
            )
        )
    }
}
