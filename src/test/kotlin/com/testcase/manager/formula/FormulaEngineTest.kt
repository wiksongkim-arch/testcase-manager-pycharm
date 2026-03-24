package com.testcase.manager.formula

import org.junit.Test
import kotlin.system.measureTimeMillis
import kotlin.test.*

/**
 * 公式引擎测试
 */
class FormulaEngineTest {

    private val engine = FormulaEngine()
    private val context = DefaultFormulaContext { _, _ -> null }

    @Test
    fun `test isFormula detection`() {
        assertTrue(engine.isFormula("=SUM(A1:A10)"))
        assertTrue(engine.isFormula("  =A1+B1"))
        assertFalse(engine.isFormula("Hello World"))
        assertFalse(engine.isFormula("123"))
        assertFalse(engine.isFormula(""))
    }

    @Test
    fun `test basic arithmetic`() {
        // 加法
        val addResult = engine.evaluateSimple("=1+2")
        assertTrue(addResult is FormulaValue.Number)
        assertEquals(3.0, (addResult as FormulaValue.Number).value, 0.001)

        // 减法
        val subResult = engine.evaluateSimple("=10-3")
        assertTrue(subResult is FormulaValue.Number)
        assertEquals(7.0, (subResult as FormulaValue.Number).value, 0.001)

        // 乘法
        val mulResult = engine.evaluateSimple("=4*5")
        assertTrue(mulResult is FormulaValue.Number)
        assertEquals(20.0, (mulResult as FormulaValue.Number).value, 0.001)

        // 除法
        val divResult = engine.evaluateSimple("=20/4")
        assertTrue(divResult is FormulaValue.Number)
        assertEquals(5.0, (divResult as FormulaValue.Number).value, 0.001)
    }

    @Test
    fun `test operator precedence`() {
        val result = engine.evaluateSimple("=2+3*4")
        assertTrue(result is FormulaValue.Number)
        assertEquals(14.0, (result as FormulaValue.Number).value, 0.001)

        val result2 = engine.evaluateSimple("=(2+3)*4")
        assertTrue(result2 is FormulaValue.Number)
        assertEquals(20.0, (result2 as FormulaValue.Number).value, 0.001)
    }

    @Test
    fun `test SUM function`() {
        val result = engine.evaluateSimple("=SUM(1,2,3,4,5)")
        assertTrue(result is FormulaValue.Number)
        assertEquals(15.0, (result as FormulaValue.Number).value, 0.001)
    }

    @Test
    fun `test COUNT function`() {
        val result = engine.evaluateSimple("=COUNT(1,2,3,\"text\",4)")
        assertTrue(result is FormulaValue.Number)
        assertEquals(4.0, (result as FormulaValue.Number).value, 0.001)
    }

    @Test
    fun `test AVERAGE function`() {
        val result = engine.evaluateSimple("=AVERAGE(10,20,30)")
        assertTrue(result is FormulaValue.Number)
        assertEquals(20.0, (result as FormulaValue.Number).value, 0.001)
    }

    @Test
    fun `test MAX function`() {
        val result = engine.evaluateSimple("=MAX(5,10,3,8,1)")
        assertTrue(result is FormulaValue.Number)
        assertEquals(10.0, (result as FormulaValue.Number).value, 0.001)
    }

    @Test
    fun `test MIN function`() {
        val result = engine.evaluateSimple("=MIN(5,10,3,8,1)")
        assertTrue(result is FormulaValue.Number)
        assertEquals(1.0, (result as FormulaValue.Number).value, 0.001)
    }

    @Test
    fun `test IF function`() {
        val result1 = engine.evaluateSimple("=IF(TRUE,\"yes\",\"no\")")
        assertTrue(result1 is FormulaValue.String)
        assertEquals("yes", (result1 as FormulaValue.String).value)

        val result2 = engine.evaluateSimple("=IF(FALSE,\"yes\",\"no\")")
        assertTrue(result2 is FormulaValue.String)
        assertEquals("no", (result2 as FormulaValue.String).value)
    }

    @Test
    fun `test AND function`() {
        val result1 = engine.evaluateSimple("=AND(TRUE,TRUE)")
        assertTrue(result1 is FormulaValue.Boolean)
        assertEquals(true, (result1 as FormulaValue.Boolean).value)

        val result2 = engine.evaluateSimple("=AND(TRUE,FALSE)")
        assertTrue(result2 is FormulaValue.Boolean)
        assertEquals(false, (result2 as FormulaValue.Boolean).value)
    }

    @Test
    fun `test OR function`() {
        val result1 = engine.evaluateSimple("=OR(FALSE,TRUE)")
        assertTrue(result1 is FormulaValue.Boolean)
        assertEquals(true, (result1 as FormulaValue.Boolean).value)

        val result2 = engine.evaluateSimple("=OR(FALSE,FALSE)")
        assertTrue(result2 is FormulaValue.Boolean)
        assertEquals(false, (result2 as FormulaValue.Boolean).value)
    }

    @Test
    fun `test NOT function`() {
        val result = engine.evaluateSimple("=NOT(TRUE)")
        assertTrue(result is FormulaValue.Boolean)
        assertEquals(false, (result as FormulaValue.Boolean).value)
    }

    @Test
    fun `test CONCAT function`() {
        val result = engine.evaluateSimple("=CONCAT(\"Hello\",\" \",\"World\")")
        assertTrue(result is FormulaValue.String)
        assertEquals("Hello World", (result as FormulaValue.String).value)
    }

    @Test
    fun `test CONCATENATE alias`() {
        val result = engine.evaluateSimple("=CONCATENATE(\"A\",\"B\",\"C\")")
        assertTrue(result is FormulaValue.String)
        assertEquals("ABC", (result as FormulaValue.String).value)
    }

    @Test
    fun `test LEFT function`() {
        val result = engine.evaluateSimple("=LEFT(\"Hello\",2)")
        assertTrue(result is FormulaValue.String)
        assertEquals("He", (result as FormulaValue.String).value)
    }

    @Test
    fun `test RIGHT function`() {
        val result = engine.evaluateSimple("=RIGHT(\"Hello\",2)")
        assertTrue(result is FormulaValue.String)
        assertEquals("lo", (result as FormulaValue.String).value)
    }

    @Test
    fun `test MID function`() {
        val result = engine.evaluateSimple("=MID(\"Hello World\",7,5)")
        assertTrue(result is FormulaValue.String)
        assertEquals("World", (result as FormulaValue.String).value)
    }

    @Test
    fun `test LEN function`() {
        val result = engine.evaluateSimple("=LEN(\"Hello\")")
        assertTrue(result is FormulaValue.Number)
        assertEquals(5.0, (result as FormulaValue.Number).value, 0.001)
    }

    @Test
    fun `test TRIM function`() {
        val result = engine.evaluateSimple("=TRIM(\"  Hello  World  \")")
        assertTrue(result is FormulaValue.String)
        assertEquals("Hello  World", (result as FormulaValue.String).value)
    }

    @Test
    fun `test UPPER function`() {
        val result = engine.evaluateSimple("=UPPER(\"hello\")")
        assertTrue(result is FormulaValue.String)
        assertEquals("HELLO", (result as FormulaValue.String).value)
    }

    @Test
    fun `test LOWER function`() {
        val result = engine.evaluateSimple("=LOWER(\"HELLO\")")
        assertTrue(result is FormulaValue.String)
        assertEquals("hello", (result as FormulaValue.String).value)
    }

    @Test
    fun `test ABS function`() {
        val result = engine.evaluateSimple("=ABS(-5)")
        assertTrue(result is FormulaValue.Number)
        assertEquals(5.0, (result as FormulaValue.Number).value, 0.001)
    }

    @Test
    fun `test ROUND function`() {
        val result = engine.evaluateSimple("=ROUND(3.14159,2)")
        assertTrue(result is FormulaValue.Number)
        assertEquals(3.14, (result as FormulaValue.Number).value, 0.001)
    }

    @Test
    fun `test ISBLANK function`() {
        val result = engine.evaluateSimple("=ISBLANK(\"\")")
        assertTrue(result is FormulaValue.Boolean)
        assertEquals(true, (result as FormulaValue.Boolean).value)
    }

    @Test
    fun `test ISNUMBER function`() {
        val result = engine.evaluateSimple("=ISNUMBER(123)")
        assertTrue(result is FormulaValue.Boolean)
        assertEquals(true, (result as FormulaValue.Boolean).value)
    }

    @Test
    fun `test ISTEXT function`() {
        val result = engine.evaluateSimple("=ISTEXT(\"hello\")")
        assertTrue(result is FormulaValue.Boolean)
        assertEquals(true, (result as FormulaValue.Boolean).value)
    }

    @Test
    fun `test comparison operators`() {
        // 等于
        val eq1 = engine.evaluateSimple("=5=5")
        assertTrue(eq1 is FormulaValue.Boolean)
        assertEquals(true, (eq1 as FormulaValue.Boolean).value)

        // 不等于
        val neq = engine.evaluateSimple("=5<>3")
        assertTrue(neq is FormulaValue.Boolean)
        assertEquals(true, (neq as FormulaValue.Boolean).value)

        // 大于
        val gt = engine.evaluateSimple("=5>3")
        assertTrue(gt is FormulaValue.Boolean)
        assertEquals(true, (gt as FormulaValue.Boolean).value)

        // 小于
        val lt = engine.evaluateSimple("=3<5")
        assertTrue(lt is FormulaValue.Boolean)
        assertEquals(true, (lt as FormulaValue.Boolean).value)

        // 大于等于
        val gte = engine.evaluateSimple("=5>=5")
        assertTrue(gte is FormulaValue.Boolean)
        assertEquals(true, (gte as FormulaValue.Boolean).value)

        // 小于等于
        val lte = engine.evaluateSimple("=3<=5")
        assertTrue(lte is FormulaValue.Boolean)
        assertEquals(true, (lte as FormulaValue.Boolean).value)
    }

    @Test
    fun `test nested functions`() {
        val result = engine.evaluateSimple("=SUM(IF(TRUE,1,0),IF(FALSE,1,0),3)")
        assertTrue(result is FormulaValue.Number)
        assertEquals(4.0, (result as FormulaValue.Number).value, 0.001)
    }

    @Test
    fun `test string concatenation with plus`() {
        val result = engine.evaluateSimple("=\"Hello\"+\" \"+\"World\"")
        assertTrue(result is FormulaValue.String)
        assertEquals("Hello World", (result as FormulaValue.String).value)
    }

    @Test
    fun `test division by zero`() {
        val result = engine.evaluateSimple("=10/0")
        assertTrue(result is FormulaValue.Error)
    }

    @Test
    fun `test unknown function`() {
        val result = engine.evaluateSimple("=UNKNOWN(1,2,3)")
        assertTrue(result is FormulaValue.Error)
    }

    @Test
    fun `test formula validation`() {
        val valid1 = engine.validate("=SUM(1,2,3)")
        assertTrue(valid1.isValid)

        val valid2 = engine.validate("Not a formula")
        assertTrue(valid2.isValid)

        val invalid = engine.validate("=SUM(")
        assertFalse(invalid.isValid)
    }

    @Test
    fun `test getCellReferences`() {
        val refs1 = engine.getCellReferences("=SUM(A1:B10)")
        assertEquals(listOf("A1:B10"), refs1)

        val refs2 = engine.getCellReferences("=A1+B2+C3")
        assertEquals(listOf("A1", "B2", "C3"), refs2)

        val refs3 = engine.getCellReferences("Not a formula")
        assertTrue(refs3.isEmpty())
    }

    @Test
    fun `test getFunctionSuggestions`() {
        val suggestions = engine.getFunctionSuggestions("SU")
        assertTrue(suggestions.any { it.name == "SUM" })
        assertTrue(suggestions.any { it.name == "AVERAGE" })
    }

    @Test
    fun `test getAllFunctions`() {
        val functions = engine.getAllFunctions()
        assertTrue(functions.isNotEmpty())
        assertTrue(functions.any { it.name == "SUM" })
        assertTrue(functions.any { it.name == "IF" })
        assertTrue(functions.any { it.name == "CONCAT" })
    }

    @Test
    fun `test performance - complex formula`() {
        val iterations = 1000
        val formula = "=SUM(IF(A>10,A*2,A/2),COUNT(1,2,3),CONCAT(\"test\",\"data\"))"

        val context = DefaultFormulaContext { col, row ->
            when (col) {
                "A" -> 15.0
                else -> null
            }
        }

        val time = measureTimeMillis {
            repeat(iterations) {
                engine.evaluate(formula, context)
            }
        }

        println("Complex formula evaluation: $iterations iterations in ${time}ms")
        println("Average: ${time.toDouble() / iterations}ms per evaluation")

        // 应该能在合理时间内完成
        assertTrue(time < 5000, "Performance test took too long: ${time}ms")
    }

    @Test
    fun `test lexer tokenize`() {
        val lexer = FormulaLexer("SUM(A1,10,\"text\")")
        val tokens = lexer.tokenize()

        assertTrue(tokens.isNotEmpty())
        assertEquals(TokenType.FUNCTION, tokens[0].type)
        assertEquals("SUM", tokens[0].value)
    }

    @Test
    fun `test parser complex expression`() {
        val lexer = FormulaLexer("1+2*3")
        val tokens = lexer.tokenize()
        val parser = FormulaParser(tokens)
        val ast = parser.parse()

        assertNotNull(ast)
        assertTrue(ast is BinaryOpNode)
    }

    @Test
    fun `test cell reference with context`() {
        val context = DefaultFormulaContext { col, row ->
            if (col == "A" && row == 1) 100.0 else null
        }

        val result = engine.evaluate("=A1", context)
        assertTrue(result is FormulaValue.Number)
        assertEquals(100.0, (result as FormulaValue.Number).value, 0.001)
    }

    @Test
    fun `test range reference`() {
        val context = DefaultFormulaContext { col, row ->
            when {
                col == "A" && row in 1..3 -> row * 10.0
                else -> null
            }
        }

        val result = engine.evaluate("=SUM(A1:A3)", context)
        assertTrue(result is FormulaValue.Number)
        assertEquals(60.0, (result as FormulaValue.Number).value, 0.001)
    }
}
