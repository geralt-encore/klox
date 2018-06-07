package com.craftinginterpreters.klox

import com.craftinginterpreters.klox.Expr.Binary
import com.craftinginterpreters.klox.Expr.Grouping
import com.craftinginterpreters.klox.Expr.Literal
import com.craftinginterpreters.klox.Expr.Unary

fun main(args: Array<String>) {
  val expr = Binary(
      Unary(
          Token(TokenType.MINUS, "-", null, 1),
          Literal(123)
      ),
      Token(TokenType.STAR, "*", null, 1),
      Grouping(
          Literal(45.67)
      )
  )

  System.out.println(AstPrinter().print(expr))
}

class AstPrinter: Expr.Visitor<String> {

  fun print(expr: Expr) = expr.accept(this)

  override fun visit(expr: Binary) = parenthesize(expr.operator.lexeme, expr.left, expr.right)

  override fun visit(expr: Grouping) = parenthesize("group", expr.expression)

  override fun visit(expr: Literal) = when {
    expr.value == null -> "nil"
    else -> expr.value.toString()
  }

  override fun visit(expr: Unary) = parenthesize(expr.operator.lexeme, expr.right)

  private fun parenthesize(name: String, vararg expressions: Expr) = buildString {
    append("(")
    append(name)
    expressions.forEach {
      append(" ")
      append(it.accept(this@AstPrinter))
    }
    append(")")
  }
}