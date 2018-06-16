package com.craftinginterpreters.klox

import com.craftinginterpreters.klox.TokenType.BANG
import com.craftinginterpreters.klox.TokenType.BANG_EQUAL
import com.craftinginterpreters.klox.TokenType.EQUAL_EQUAL
import com.craftinginterpreters.klox.TokenType.GREATER
import com.craftinginterpreters.klox.TokenType.GREATER_EQUAL
import com.craftinginterpreters.klox.TokenType.LESS_EQUAL
import com.craftinginterpreters.klox.TokenType.MINUS
import com.craftinginterpreters.klox.TokenType.PLUS
import com.craftinginterpreters.klox.TokenType.SLASH
import com.craftinginterpreters.klox.TokenType.STAR

class Interpreter : Expr.Visitor<Any?> {

  fun interpret(expression: Expr) {
    try {
      val value = evaluate(expression)
      System.out.println(stringify(value))
    } catch (error: RuntimeError) {
      runtimeError(error)
    }
  }

  override fun visit(expr: Expr.Binary): Any? {
    val left = evaluate(expr.left)
    val right = evaluate(expr.right)

    return when (expr.operator.type) {
      GREATER -> {
        checkNumberOperands(expr.operator, left, right)
        left as Double > right as Double
      }
      GREATER_EQUAL -> {
        checkNumberOperands(expr.operator, left, right)
        left as Double >= right as Double
      }
//      LESS -> {
//        checkNumberOperands(expr.operator, left, right)
//        left as Double < right as Double
//      }
      LESS_EQUAL -> {
        checkNumberOperands(expr.operator, left, right)
        left as Double <= right as Double
      }
      MINUS -> {
        checkNumberOperands(expr.operator, left, right)
        left as Double - right as Double
      }
      PLUS -> when {
        left is Double && right is Double -> left + right
        left is String && right is String -> left + right
        else -> throw RuntimeError(expr.operator, "Operands must be two numbers or two strings.")
      }
      SLASH -> {
        checkNumberOperands(expr.operator, left, right)
        left as Double / right as Double
      }
      STAR -> {
        checkNumberOperands(expr.operator, left, right)
        left as Double * right as Double
      }
      BANG_EQUAL -> !isEqual(left, right)
      EQUAL_EQUAL -> isEqual(left, right)
      else -> null
    }
  }

  override fun visit(expr: Expr.Grouping) = evaluate(expr.expression)

  override fun visit(expr: Expr.Literal) = expr.value

  override fun visit(expr: Expr.Unary): Any? {
    val right = evaluate(expr.right)

    when (expr.operator.type) {
      BANG -> !isTruthy(right)
      MINUS -> {
        checkNumberOperand(expr.operator, right)
        -(right as Double)
      }
    }

    return null
  }

  private fun evaluate(expr: Expr) = expr.accept(this)

  private fun isTruthy(it: Any?) = when (it) {
    null -> false
    is Boolean -> it
    else -> true
  }

  private fun isEqual(a: Any?, b: Any?) = when {
    a == null && b == null -> true
    a == null -> false
    else -> a == b
  }

  private fun checkNumberOperand(operator: Token, operand: Any?) {
    if (operand is Double) return

    throw RuntimeError(operator, "Operand must be a number.")
  }

  private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
    if (left is Double && right is Double) return

    throw RuntimeError(operator, "Operands must be numbers.")
  }

  private fun stringify(any: Any?): String {
    if (any == null) return "nil"

    // Hack. Work around Java adding ".0" to integer-valued doubles.
    if (any is Double) {
      var text = any.toString()
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length - 2)
      }
      return text
    }

    return any.toString()
  }
}