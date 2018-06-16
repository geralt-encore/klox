package com.craftinginterpreters.klox

import com.craftinginterpreters.klox.Expr.*
import com.craftinginterpreters.klox.TokenType.BANG
import com.craftinginterpreters.klox.TokenType.BANG_EQUAL
import com.craftinginterpreters.klox.TokenType.CLASS
import com.craftinginterpreters.klox.TokenType.EOF
import com.craftinginterpreters.klox.TokenType.EQUAL_EQUAL
import com.craftinginterpreters.klox.TokenType.FALSE
import com.craftinginterpreters.klox.TokenType.FOR
import com.craftinginterpreters.klox.TokenType.FUN
import com.craftinginterpreters.klox.TokenType.GREATER
import com.craftinginterpreters.klox.TokenType.GREATER_EQUAL
import com.craftinginterpreters.klox.TokenType.IF
import com.craftinginterpreters.klox.TokenType.LEFT_PAREN
import com.craftinginterpreters.klox.TokenType.LESS
import com.craftinginterpreters.klox.TokenType.LESS_EQUAL
import com.craftinginterpreters.klox.TokenType.MINUS
import com.craftinginterpreters.klox.TokenType.NIL
import com.craftinginterpreters.klox.TokenType.NUMBER
import com.craftinginterpreters.klox.TokenType.PLUS
import com.craftinginterpreters.klox.TokenType.PRINT
import com.craftinginterpreters.klox.TokenType.RETURN
import com.craftinginterpreters.klox.TokenType.RIGHT_PAREN
import com.craftinginterpreters.klox.TokenType.SEMICOLON
import com.craftinginterpreters.klox.TokenType.STRING
import com.craftinginterpreters.klox.TokenType.TRUE
import com.craftinginterpreters.klox.TokenType.VAR
import com.craftinginterpreters.klox.TokenType.WHILE
import com.craftinginterpreters.klox.error as loxError


class Parser(private val tokens: List<Token>) {

  private var current = 0

  fun parse() = try {
    expression()
  } catch (error: ParseError) {
    null
  }

  private fun expression() = equality()

  private fun equality(): Expr {
    var expr = comparison()

    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      val operator = previous()
      val right = comparison()
      expr = Binary(expr, operator, right)
    }

    return expr
  }

  private fun comparison(): Expr {
    var expr = addition()

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      val operator = previous()
      val right = addition()
      expr = Binary(expr, operator, right)
    }

    return expr
  }

  private fun addition(): Expr {
    var expr = multiplication()

    while (match(MINUS, PLUS)) {
      val operator = previous()
      val right = multiplication()
      expr = Binary(expr, operator, right)
    }

    return expr
  }

  private fun multiplication(): Expr {
    var expr = unary()

    while (match(TokenType.SLASH, TokenType.STAR)) {
      val operator = previous()
      val right = unary()
      expr = Binary(expr, operator, right)
    }

    return expr
  }

  private fun unary(): Expr {
    if (match(BANG, MINUS)) {
      val operator = previous()
      val right = unary()
      return Unary(operator, right)
    }

    return primary()
  }

  private fun primary(): Expr {
    if (match(FALSE)) return Literal(false)
    if (match(TRUE)) return Literal(true)
    if (match(NIL)) return Literal(null)

    if (match(NUMBER, STRING)) {
      return Literal(previous().literal)
    }

    if (match(LEFT_PAREN)) {
      val expr = expression()
      consume(RIGHT_PAREN, "Expect ')' after expression.")
      return Grouping(expr)
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

  private fun synchronize() {
    advance()

    while (!isAtEnd()) {
      if (previous().type === SEMICOLON) return

      @Suppress("NON_EXHAUSTIVE_WHEN")
      when (peek().type) {
        CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> return
      }

      advance()
    }
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

  private fun isAtEnd() = peek().type === EOF

  private fun peek() = tokens[current]

  internal class ParseError : RuntimeException()
}