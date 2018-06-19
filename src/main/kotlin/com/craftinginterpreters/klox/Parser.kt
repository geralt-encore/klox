package com.craftinginterpreters.klox

import com.craftinginterpreters.klox.error as loxError
import java.util.ArrayList




class Parser(private val tokens: List<Token>) {

  private var current = 0

  fun parse(): List<Stmt> {
    val statements = mutableListOf<Stmt?>()
    while (!isAtEnd()) {
      statements += declaration()
    }

    return statements.filterNotNull()
  }

  private fun declaration() = try {
    if (match(TokenType.VAR)) varDeclaration() else statement()
  } catch (error: ParseError) {
    synchronize()
    null
  }

  private fun varDeclaration(): Stmt {
    val name = consume(TokenType.IDENTIFIER, "Expect variable name.")

    var initializer: Expr? = null
    if (match(TokenType.EQUAL)) {
      initializer = expression()
    }

    consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
    return Stmt.Var(name, initializer)
  }

  private fun statement() = when {
    match(TokenType.PRINT) -> printStatement()
    match(TokenType.LEFT_BRACE) -> Stmt.Block(block())
    else -> expressionStatement()
  }

  private fun printStatement(): Stmt {
    val value = expression()
    consume(TokenType.SEMICOLON, "Expect ';' after value.")
    return Stmt.Print(value)
  }

  private fun block(): List<Stmt> {
    val statements = mutableListOf<Stmt?>()

    while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration())
    }

    consume(TokenType.RIGHT_BRACE, "Expect '}' after block.")
    return statements.filterNotNull()
  }

  private fun expressionStatement(): Stmt {
    val expr = expression()
    consume(TokenType.SEMICOLON, "Expect ';' after expression.")
    return Stmt.Expression(expr)
  }

  private fun expression() = assignment()

  private fun assignment(): Expr {
    val expr = equality()

    if (match(TokenType.EQUAL)) {
      val equals = previous()
      val value = assignment()

      if (expr is Expr.Variable) {
        val name = expr.name
        return Expr.Assign(name, value)
      }

      error(equals, "Invalid assignment target.")
    }

    return expr
  }

  private fun equality(): Expr {
    var expr = comparison()

    while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
      val operator = previous()
      val right = comparison()
      expr = Expr.Binary(expr, operator, right)
    }

    return expr
  }

  private fun comparison(): Expr {
    var expr = addition()

    while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
      val operator = previous()
      val right = addition()
      expr = Expr.Binary(expr, operator, right)
    }

    return expr
  }

  private fun addition(): Expr {
    var expr = multiplication()

    while (match(TokenType.MINUS, TokenType.PLUS)) {
      val operator = previous()
      val right = multiplication()
      expr = Expr.Binary(expr, operator, right)
    }

    return expr
  }

  private fun multiplication(): Expr {
    var expr = unary()

    while (match(TokenType.SLASH, TokenType.STAR)) {
      val operator = previous()
      val right = unary()
      expr = Expr.Binary(expr, operator, right)
    }

    return expr
  }

  private fun unary(): Expr {
    if (match(TokenType.BANG, TokenType.MINUS)) {
      val operator = previous()
      val right = unary()
      return Expr.Unary(operator, right)
    }

    return primary()
  }

  private fun primary(): Expr {
    if (match(TokenType.FALSE)) return Expr.Literal(false)
    if (match(TokenType.TRUE)) return Expr.Literal(true)
    if (match(TokenType.NIL)) return Expr.Literal(null)

    if (match(TokenType.NUMBER, TokenType.STRING)) {
      return Expr.Literal(previous().literal)
    }

    if (match(TokenType.IDENTIFIER)) {
      return Expr.Variable(previous())
    }

    if (match(TokenType.LEFT_PAREN)) {
      val expr = expression()
      consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
      return Expr.Grouping(expr)
    }

    throw error(peek(), "Expect expression.")
  }

  private fun consume(type: TokenType, message: String): Token {
    if (check(type)) return advance()

    throw error(peek(), message)
  }

  private fun error(token: Token, message: String): ParseError {
    loxError(token, message)

    return ParseError()
  }

  private fun match(vararg types: TokenType): Boolean {
    types.forEach { type ->
      if (check(type)) {
        advance()
        return true
      }
    }

    return false
  }

  private fun check(tokenType: TokenType): Boolean {
    if (isAtEnd()) return false

    return peek().type == tokenType
  }

  private fun advance(): Token {
    if (!isAtEnd()) current++

    return previous()
  }

  private fun previous() = tokens[current - 1]

  private fun isAtEnd() = peek().type === TokenType.EOF

  private fun peek() = tokens[current]

  private fun synchronize() {
    advance()

    while (!isAtEnd()) {
      if (previous().type === TokenType.SEMICOLON) return

      @Suppress("NON_EXHAUSTIVE_WHEN")
      when (peek().type) {
        TokenType.CLASS, TokenType.FUN, TokenType.VAR, TokenType.FOR,
        TokenType.IF, TokenType.WHILE, TokenType.PRINT, TokenType.RETURN -> return
      }

      advance()
    }
  }

  class ParseError : RuntimeException()
}