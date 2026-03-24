package com.testcase.manager.formula

/**
 * 公式上下文接口
 * 提供公式计算所需的上下文信息
 */
interface FormulaContext {
    /**
     * 获取单元格值
     */
    fun getCellValue(column: String, row: Int): FormulaValue

    /**
     * 调用函数
     */
    fun callFunction(name: String, arguments: List<FormulaValue>): FormulaValue
}

/**
 * 默认公式上下文
 */
class DefaultFormulaContext(
    private val dataProvider: (String, Int) -> Any?
) : FormulaContext {
    private val functions = mutableMapOf<String, FormulaFunction>()

    init {
        // 注册内置函数
        registerBuiltInFunctions()
    }

    override fun getCellValue(column: String, row: Int): FormulaValue {
        val value = dataProvider(column, row)
        return convertToFormulaValue(value)
    }

    override fun callFunction(name: String, arguments: List<FormulaValue>): FormulaValue {
        val function = functions[name.uppercase()]
            ?: return FormulaValue.Error("Unknown function: $name")
        return function.execute(arguments)
    }

    /**
     * 注册函数
     */
    fun registerFunction(name: String, function: FormulaFunction) {
        functions[name.uppercase()] = function
    }

    /**
     * 注册内置函数
     */
    private fun registerBuiltInFunctions() {
        // SUM 函数
        registerFunction("SUM", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                var sum = 0.0
                for (arg in arguments) {
                    when (arg) {
                        is FormulaValue.Number -> sum += arg.value
                        is FormulaValue.List -> sum += arg.values.sumOf { it.toNumber() }
                        else -> sum += arg.toNumber()
                    }
                }
                return FormulaValue.Number(sum)
            }
        })

        // COUNT 函数
        registerFunction("COUNT", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                var count = 0
                for (arg in arguments) {
                    when (arg) {
                        is FormulaValue.List -> count += arg.values.count { it !is FormulaValue.Empty }
                        is FormulaValue.Empty -> { }
                        else -> count++
                    }
                }
                return FormulaValue.Number(count.toDouble())
            }
        })

        // AVERAGE 函数
        registerFunction("AVERAGE", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                val values = mutableListOf<Double>()
                for (arg in arguments) {
                    when (arg) {
                        is FormulaValue.Number -> values.add(arg.value)
                        is FormulaValue.List -> values.addAll(arg.values.map { it.toNumber() })
                        else -> values.add(arg.toNumber())
                    }
                }
                return if (values.isNotEmpty()) {
                    FormulaValue.Number(values.average())
                } else {
                    FormulaValue.Error("No values to average")
                }
            }
        })

        // MAX 函数
        registerFunction("MAX", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                val values = mutableListOf<Double>()
                for (arg in arguments) {
                    when (arg) {
                        is FormulaValue.Number -> values.add(arg.value)
                        is FormulaValue.List -> values.addAll(arg.values.map { it.toNumber() })
                        else -> values.add(arg.toNumber())
                    }
                }
                return if (values.isNotEmpty()) {
                    FormulaValue.Number(values.maxOrNull()!!)
                } else {
                    FormulaValue.Error("No values for MAX")
                }
            }
        })

        // MIN 函数
        registerFunction("MIN", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                val values = mutableListOf<Double>()
                for (arg in arguments) {
                    when (arg) {
                        is FormulaValue.Number -> values.add(arg.value)
                        is FormulaValue.List -> values.addAll(arg.values.map { it.toNumber() })
                        else -> values.add(arg.toNumber())
                    }
                }
                return if (values.isNotEmpty()) {
                    FormulaValue.Number(values.minOrNull()!!)
                } else {
                    FormulaValue.Error("No values for MIN")
                }
            }
        })

        // IF 函数
        registerFunction("IF", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                if (arguments.size < 2) {
                    return FormulaValue.Error("IF requires at least 2 arguments")
                }
                val condition = arguments[0].asBoolean()
                return if (condition) {
                    arguments[1]
                } else {
                    arguments.getOrNull(2) ?: FormulaValue.Bool(false)
                }
            }
        })

        // AND 函数
        registerFunction("AND", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                return FormulaValue.Bool(arguments.all { it.asBoolean() })
            }
        })

        // OR 函数
        registerFunction("OR", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                return FormulaValue.Bool(arguments.any { it.asBoolean() })
            }
        })

        // NOT 函数
        registerFunction("NOT", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                if (arguments.isEmpty()) {
                    return FormulaValue.Error("NOT requires 1 argument")
                }
                return FormulaValue.Bool(!arguments[0].asBoolean())
            }
        })

        // CONCAT 函数
        registerFunction("CONCAT", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                val result = StringBuilder()
                for (arg in arguments) {
                    when (arg) {
                        is FormulaValue.List -> result.append(arg.values.joinToString("") { it.asString() })
                        else -> result.append(arg.asString())
                    }
                }
                return FormulaValue.Str(result.toString())
            }
        })

        // CONCATENATE 函数（CONCAT 的别名）
        registerFunction("CONCATENATE", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                return functions["CONCAT"]?.execute(arguments)
                    ?: FormulaValue.Error("CONCAT not found")
            }
        })

        // LEFT 函数
        registerFunction("LEFT", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                if (arguments.isEmpty()) {
                    return FormulaValue.Error("LEFT requires at least 1 argument")
                }
                val text = arguments[0].asString()
                val length = arguments.getOrNull(1)?.toNumber()?.toInt() ?: 1
                return FormulaValue.Str(text.take(length.coerceAtLeast(0)))
            }
        })

        // RIGHT 函数
        registerFunction("RIGHT", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                if (arguments.isEmpty()) {
                    return FormulaValue.Error("RIGHT requires at least 1 argument")
                }
                val text = arguments[0].asString()
                val length = arguments.getOrNull(1)?.toNumber()?.toInt() ?: 1
                return FormulaValue.Str(text.takeLast(length.coerceAtLeast(0)))
            }
        })

        // MID 函数
        registerFunction("MID", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                if (arguments.size < 2) {
                    return FormulaValue.Error("MID requires at least 2 arguments")
                }
                val text = arguments[0].asString()
                val start = arguments[1].toNumber().toInt().coerceAtLeast(1)
                val length = arguments.getOrNull(2)?.toNumber()?.toInt() ?: (text.length - start + 1)
                val actualStart = (start - 1).coerceIn(0, text.length)
                val actualLength = length.coerceAtLeast(0)
                return FormulaValue.Str(text.substring(actualStart, (actualStart + actualLength).coerceAtMost(text.length)))
            }
        })

        // LEN 函数
        registerFunction("LEN", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                if (arguments.isEmpty()) {
                    return FormulaValue.Error("LEN requires 1 argument")
                }
                return FormulaValue.Number(arguments[0].asString().length.toDouble())
            }
        })

        // TRIM 函数
        registerFunction("TRIM", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                if (arguments.isEmpty()) {
                    return FormulaValue.Error("TRIM requires 1 argument")
                }
                return FormulaValue.Str(arguments[0].asString().trim())
            }
        })

        // UPPER 函数
        registerFunction("UPPER", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                if (arguments.isEmpty()) {
                    return FormulaValue.Error("UPPER requires 1 argument")
                }
                return FormulaValue.Str(arguments[0].asString().uppercase())
            }
        })

        // LOWER 函数
        registerFunction("LOWER", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                if (arguments.isEmpty()) {
                    return FormulaValue.Error("LOWER requires 1 argument")
                }
                return FormulaValue.Str(arguments[0].asString().lowercase())
            }
        })

        // ABS 函数
        registerFunction("ABS", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                if (arguments.isEmpty()) {
                    return FormulaValue.Error("ABS requires 1 argument")
                }
                return FormulaValue.Number(kotlin.math.abs(arguments[0].toNumber()))
            }
        })

        // ROUND 函数
        registerFunction("ROUND", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                if (arguments.isEmpty()) {
                    return FormulaValue.Error("ROUND requires at least 1 argument")
                }
                val number = arguments[0].toNumber()
                val digits = arguments.getOrNull(1)?.toNumber()?.toInt() ?: 0
                val factor = kotlin.math.pow(10.0, digits.toDouble())
                return FormulaValue.Number(kotlin.math.round(number * factor) / factor)
            }
        })

        // ISBLANK 函数
        registerFunction("ISBLANK", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                if (arguments.isEmpty()) {
                    return FormulaValue.Error("ISBLANK requires 1 argument")
                }
                return FormulaValue.Bool(arguments[0] is FormulaValue.Empty || arguments[0].asString().isEmpty())
            }
        })

        // ISNUMBER 函数
        registerFunction("ISNUMBER", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                if (arguments.isEmpty()) {
                    return FormulaValue.Error("ISNUMBER requires 1 argument")
                }
                return FormulaValue.Bool(arguments[0] is FormulaValue.Number)
            }
        })

        // ISTEXT 函数
        registerFunction("ISTEXT", object : FormulaFunction {
            override fun execute(arguments: List<FormulaValue>): FormulaValue {
                if (arguments.isEmpty()) {
                    return FormulaValue.Error("ISTEXT requires 1 argument")
                }
                return FormulaValue.Bool(arguments[0] is FormulaValue.Str)
            }
        })
    }

    /**
     * 将任意值转换为 FormulaValue
     */
    private fun convertToFormulaValue(value: Any?): FormulaValue {
        return when (value) {
            null -> FormulaValue.Empty
            is Number -> FormulaValue.Number(value.toDouble())
            is kotlin.String -> FormulaValue.Str(value)
            is kotlin.Boolean -> FormulaValue.Bool(value)
            else -> FormulaValue.Str(value.toString())
        }
    }
}

/**
 * 公式函数接口
 */
interface FormulaFunction {
    fun execute(arguments: List<FormulaValue>): FormulaValue
}
